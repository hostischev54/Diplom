package com.diplom.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TabEditor(
    notes: List<String>,
    onApply: (List<String>) -> Unit
) {


    val tuning = remember(notes) {
        detectTuning(notes)
    }

    val tab = remember(notes) {
        mapNotesToTabSmart(notes, tuning)
    }

    var editableTab by remember { mutableStateOf(tab) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {

        Text("Табулатура")

        Spacer(modifier = Modifier.height(8.dp))

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {

            editableTab.forEachIndexed { index, line ->

                BasicTextField(
                    value = line,
                    onValueChange = { newValue ->
                        editableTab = editableTab.toMutableList().also {
                            it[index] = newValue
                        }
                    },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        letterSpacing = 0.sp
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
            }
        }

        Button(
            onClick = { onApply(editableTab) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Применить")
        }
    }
}

data class FretPos(
    val stringIndex: Int,
    val fret: Int,
    val note: String
)

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

fun mapNotesToTabSmart(
    notes: List<String>,
    tuning: List<String>
): List<String> {

    val fretboard = buildFretboard(tuning)

    val tab = MutableList(6) { StringBuilder() }

    for (i in tuning.indices) {
        tab[i].append(tuning[i].first()).append("|")
    }

    var lastPos: FretPos? = null

    for (note in notes) {

        val candidates = fretboard.filter { it.note == note }

        val best = candidates.minByOrNull { pos ->

            val distance = if (lastPos != null)
                kotlin.math.abs(pos.fret - lastPos!!.fret)
            else 0

            val stringPenalty =
                if (lastPos != null && pos.stringIndex == lastPos!!.stringIndex)
                    5 else 0

            distance + stringPenalty
        }

        if (best != null) {

            for (i in 0 until 6) {
                if (i == best.stringIndex) {
                    tab[i].append(best.fret.toString().padEnd(2, '-'))
                } else {
                    tab[i].append("--")
                }
            }

            lastPos = best
        }
    }

    for (i in 0 until 6) {
        tab[i].append("|")
    }

    return tab.map { it.toString() }
}

fun detectTuning(notes: List<String>): List<String> {

    val allTunings = com.diplom.tuner.models.Tunings.byCategory.values.flatten()

    val lowestNote = notes.minByOrNull { noteToMidi(it) }
        ?: return allTunings.first().strings

    return allTunings.minByOrNull { tuning ->
        kotlin.math.abs(
            noteToMidi(tuning.strings.first()) - noteToMidi(lowestNote)
        )
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

