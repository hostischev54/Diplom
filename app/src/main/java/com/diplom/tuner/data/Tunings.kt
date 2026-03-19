package com.diplom.tuner.models

data class Tuning(
    val name: String,
    val strings: List<String>
)

object Tunings {

    private val noteOrderSharps = listOf(
        "C","C#","D","D#","E","F","F#","G","G#","A","A#","B"
    )

    private val noteOrderFlats = listOf(
        "C","Db","D","Eb","E","F","Gb","G","Ab","A","Bb","B"
    )

    // базовые строи
    private val standardBase = listOf("E2","A2","D3","G3","B3","E4")
    private val dropBase = listOf("D2","A2","D3","G3","B3","E4")

    // ---------------------------------------------------------
    // универсальное транспонирование ноты с сохранением октавы
    // ---------------------------------------------------------

    private fun transposeNote(
        noteWithOctave: String,
        shift: Int,
        order: List<String>
    ): String {
        // парсим ноту и октаву правильно (учитываем C#, D# и т.д.)
        val match = Regex("([A-G][#b]?)(\\d)").find(noteWithOctave) ?: return noteWithOctave
        val base = match.groupValues[1]
        var octave = match.groupValues[2].toInt()

        val index = order.indexOf(base)
        if (index < 0) return noteWithOctave

        val newIndexRaw = index + shift
        // вот здесь — октава меняется если выходим за границы 0..11
        val newIndex = ((newIndexRaw % 12) + 12) % 12
        octave += Math.floorDiv(newIndexRaw, 12)

        return order[newIndex] + octave
    }

    private fun generateStrings(
        baseStrings: List<String>,
        shift: Int,
        order: List<String>
    ): List<String> {

        return baseStrings.map { note ->
            transposeNote(note, shift, order)
        }
    }

    // ---------------------------------------------------------
    // STANDARD
    // ---------------------------------------------------------

    private fun generateStandardTunings(): List<Tuning> {

        val result = mutableListOf<Tuning>()

        var rootIndex = noteOrderSharps.indexOf("E")
        val endIndex = noteOrderSharps.indexOf("A")

        while (true) {

            val shift = rootIndex - noteOrderSharps.indexOf("E")

            val sharpStrings = generateStrings(
                standardBase,
                shift,
                noteOrderSharps
            )

            val flatStrings = generateStrings(
                standardBase,
                shift,
                noteOrderFlats
            )

            val thinSpace = "\u2009"

            val name =
                if (sharpStrings == flatStrings)
                    sharpStrings.joinToString(thinSpace)
                else
                    sharpStrings.joinToString(thinSpace) +
                            " │ " +
                            flatStrings.joinToString(thinSpace)

            result.add(Tuning(name, sharpStrings))

            if (rootIndex == endIndex) break

            rootIndex = (rootIndex - 1 + 12) % 12
        }

        return result
    }

    // ---------------------------------------------------------
    // DROP
    // ---------------------------------------------------------

    private fun generateDropTunings(): List<Tuning> {

        val result = mutableListOf<Tuning>()

        var rootIndex = noteOrderSharps.indexOf("D")
        val endIndex = noteOrderSharps.indexOf("A")

        while (true) {

            val shift = rootIndex - noteOrderSharps.indexOf("D")

            val sharpStrings = generateStrings(
                dropBase,
                shift,
                noteOrderSharps
            )

            val flatStrings = generateStrings(
                dropBase,
                shift,
                noteOrderFlats
            )

            val thinSpace = "\u2009"

            val name =
                if (sharpStrings == flatStrings)
                    sharpStrings.joinToString(thinSpace)
                else
                    sharpStrings.joinToString(thinSpace) +
                            " │ " +
                            flatStrings.joinToString(thinSpace)

            result.add(Tuning(name, sharpStrings))

            if (rootIndex == endIndex) break

            rootIndex = (rootIndex - 1 + 12) % 12
        }

        return result
    }

    // ---------------------------------------------------------
    // OPEN
    // ---------------------------------------------------------

    private val openTunings = listOf(

        Tuning(
            "D G D G B D",
            listOf("D2","G2","D3","G3","B3","D4")
        ),

        Tuning(
            "D A D F# A D │ D A D Gb A D",
            listOf("D2","A2","D3","F#3","A3","D4")
        ),

        Tuning(
            "E B E G# B E │ E B E Ab B E",
            listOf("E2","B2","E3","G#3","B3","E4")
        ),

        Tuning(
            "E A E A C# E │ E A E A Db E",
            listOf("E2","A2","E3","A3","C#4","E4")
        ),

        Tuning(
            "C G C G C E",
            listOf("C2","G2","C3","G3","C4","E4")
        )
    )

    // ---------------------------------------------------------
    // категории
    // ---------------------------------------------------------

    val byCategory: Map<String, List<Tuning>> = mapOf(

        "Standard" to generateStandardTunings(),

        "Drop" to generateDropTunings(),

        "Open" to openTunings,

        "Custom" to emptyList()
    )
}