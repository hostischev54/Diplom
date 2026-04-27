package com.diplom.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TabEditor(
    notes: List<String>,
    serverTab: Map<Int, List<String>>? = null,
    serverStringNames: Map<Int, String>? = null,
    scrollState: ScrollState? = null,
    onTabWidthMeasured: ((totalWidth: Int, columns: Int) -> Unit)? = null
) {
    val localScrollState = scrollState ?: rememberScrollState()

    val tab = remember(notes, serverTab, serverStringNames) {
        if (serverTab != null) renderTabFromServer(serverTab, serverStringNames)
        else { val tuning = detectTuning(notes); renderTab(mapNotesToTabSmart(notes, tuning)) }
    }

    Column(
        modifier = Modifier
            .wrapContentHeight()  // только по высоте контента
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {


        Column(
            modifier = Modifier
                .wrapContentHeight()
                .horizontalScroll(localScrollState)
                .onGloballyPositioned { coords ->
                    onTabWidthMeasured?.invoke(coords.size.width, tab.firstOrNull()?.length ?: 1)
                }
        ) {
            tab.forEach { line ->
                Text(
                    text = line,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        letterSpacing = 0.sp,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

data class FretPos(val stringIndex: Int, val fret: Int, val note: String)
data class TabEvent(val tick: Int, val string: Int, val fret: Int)

fun buildFretboard(tuning: List<String>): List<FretPos> {
    val result = mutableListOf<FretPos>()
    for (stringIndex in tuning.indices) {
        val openMidi = noteToMidi(tuning[stringIndex])
        for (fret in 0..24) {
            val midi = openMidi + fret
            val note = midiToNote(midi)
            result.add(FretPos(stringIndex, fret, note))
        }
    }
    return result
}

fun mapNotesToTabSmart(notes: List<String>, tuning: List<String>): List<TabEvent> {
    val fretboard = buildFretboard(tuning)
    val events = mutableListOf<TabEvent>()
    var lastPos: FretPos? = null
    var currentPosition = 0
    var sameStringCount = 0
    var tick = 0
    val boxSize = 4
    val tolerance = 2

    for (note in notes) {
        val candidates = fretboard.filter { it.note == note }
        val localLastPos = lastPos

        val best = candidates.minByOrNull { pos ->
            val fretDistance = localLastPos?.let { kotlin.math.abs(pos.fret - it.fret) } ?: 0
            val inBox = pos.fret in currentPosition..(currentPosition + boxSize)
            val nearBox = pos.fret in (currentPosition - tolerance)..(currentPosition + boxSize + tolerance)
            val stringPenalty = when {
                localLastPos == null -> 0
                pos.stringIndex == localLastPos.stringIndex -> sameStringCount * 2
                else -> 0
            }
            val stretchPenalty = if (!inBox && nearBox) 2 else if (!nearBox) 5 else 0
            val shiftPenalty = if (!inBox && !nearBox) 6 else 0
            stringPenalty + stretchPenalty + shiftPenalty + fretDistance
        }

        if (best != null) {
            if (best.fret !in currentPosition..(currentPosition + boxSize)) {
                currentPosition = best.fret.coerceAtLeast(0)
            }
            events.add(TabEvent(tick = tick, string = best.stringIndex, fret = best.fret))
            tick++
            sameStringCount = if (lastPos != null && lastPos.stringIndex == best.stringIndex) sameStringCount + 1 else 0
            lastPos = best
        }
    }
    return events
}

fun renderTab(events: List<TabEvent>): List<String> {
    val maxTick = events.maxOfOrNull { it.tick } ?: 0
    val result = MutableList(6) { StringBuilder() }
    val strings = listOf("e", "B", "G", "D", "A", "E")
    for (i in 0 until 6) result[i].append(strings[i]).append("|")
    for (t in 0..maxTick) {
        for (s in 0 until 6) {
            val event = events.find { it.tick == t && it.string == s }
            if (event != null) result[s].append(event.fret.toString().padEnd(2, '-'))
            else result[s].append("--")
        }
    }
    for (i in 0 until 6) result[i].append("|")
    return result.map { it.toString() }
}

fun renderTabFromServer(
    strings: Map<Int, List<String>>,
    stringNames: Map<Int, String>? = null
): List<String> {
    android.util.Log.d("TabEditor", "stringNames получены: $stringNames")

    val result = mutableListOf<String>()
    val maxLen = strings.values.maxOfOrNull { it.size } ?: 0
    if (maxLen == 0) return result

    // ИСПРАВЛЕНО: 1..6 вместо 6 downTo 1 (струна 1 = тонкая e, идёт сверху)
    for (s in 1..6) {
        val cells = strings[s] ?: emptyList()

        val noteName = stringNames?.get(s)?.uppercase()
            ?: mapOf(1 to "E", 2 to "B", 3 to "G", 4 to "D", 5 to "A", 6 to "E")[s]
            ?: "?"

        // Струна 1 (тонкая e) — строчная, остальные заглавные
        val displayName = if (s == 1) noteName.lowercase() else noteName.uppercase()

        val line = StringBuilder()
        line.append(displayName).append("|")
        cells.forEach { cell ->
            if (cell == "-") line.append("--")
            else line.append(cell.padEnd(2, '-'))
        }
        line.append("|")
        result.add(line.toString())
    }
    return result
}

fun detectTuning(notes: List<String>): List<String> {
    val allTunings = com.diplom.tuner.models.Tunings.byCategory.values.flatten()
    val lowestNote = notes.minByOrNull { noteToMidi(it) } ?: return allTunings.first().strings
    return allTunings.minByOrNull { tuning ->
        kotlin.math.abs(noteToMidi(tuning.strings.first()) - noteToMidi(lowestNote))
    }?.strings ?: allTunings.first().strings
}

fun midiToNote(midi: Int): String {
    val names = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
    val octave = (midi / 12) - 1
    val name = names[midi % 12]
    return name + octave
}

fun noteToMidi(note: String): Int {
    val names = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
    val name = note.dropLast(1)
    val octave = note.last().digitToInt()
    val index = names.indexOf(name)
    return (octave + 1) * 12 + index
}