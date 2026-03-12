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

        val base = noteWithOctave.dropLast(1)
        val octave = noteWithOctave.last()

        val index = order.indexOf(base)

        val newIndex = (index + shift + 12) % 12

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
            listOf("D3","G3","D4","G4","B4","D5")
        ),

        Tuning(
            "D A D F# A D │ D A D Gb A D",
            listOf("D3","A3","D4","F#4","A4","D5")
        ),

        Tuning(
            "E B E G# B E │ E B E Ab B E",
            listOf("E3","B3","E4","G#4","B4","E5")
        ),

        Tuning(
            "E A E A C# E │ E A E A Db E",
            listOf("E3","A3","E4","A4","C#5","E5")
        ),

        Tuning(
            "C G C G C E",
            listOf("C3","G3","C4","G4","C5","E5")
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