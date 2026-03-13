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

        val note = noteWithOctave.dropLastWhile { it.isDigit() }
        val octave = noteWithOctave.drop(note.length).toInt()

        val index = order.indexOf(note)

        val absoluteIndex = octave * 12 + index
        val newAbsoluteIndex = absoluteIndex + shift

        val newOctave = newAbsoluteIndex / 12
        val newIndex = ((newAbsoluteIndex % 12) + 12) % 12

        return order[newIndex] + newOctave
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
        val baseIndex = noteOrderSharps.indexOf("E")

        while (true) {

            var shift = rootIndex - baseIndex
            if (shift > 0) shift -= 12

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
        val baseIndex = noteOrderSharps.indexOf("D")

        while (true) {

            var shift = rootIndex - baseIndex
            if (shift > 0) shift -= 12

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

        // Open G — D G D G B D
        Tuning(
            "D G D G B D",
            listOf("D2","G2","D3","G3","B3","D4")
        ),

        // Open D — D A D F# A D
        Tuning(
            "D A D F# A D │ D A D Gb A D",
            listOf("D2","A2","D3","F#3","A3","D4")
        ),

        // Open E — E B E G# B E
        Tuning(
            "E B E G# B E │ E B E Ab B E",
            listOf("E2","B2","E3","G#3","B3","E4")
        ),

        // Open A — E A E A C# E
        Tuning(
            "E A E A C# E │ E A E A Db E",
            listOf("E2","A2","E3","A3","C#3","E4")
        ),

        // Open C — C G C G C E
        Tuning(
            "C G C G C E",
            listOf("C2","G2","C3","G3","C3","E3")
        )
    )
    private val customTunings = listOf(
        // DADGAD — популярный фолк-строй
        Tuning(
            "D A D G A D",
            listOf("D2","A2","D3","G3","A3","D4")
        ),

        // DADDAD — «Papa-Papa», фолк и альтернативная музыка
        Tuning(
            "D A D D A D",
            listOf("D2","A2","D3","D3","A3","D4")
        ),

        // Cross A — «Sitar A», индийское звучание
        Tuning(
            "E A E A E A",
            listOf("E2","A2","E3","A3","E4","A4")
        ),

        // Open D variant — «John Mayer special»
        Tuning(
            "B D D D D D",
            listOf("B1","D2","D3","D3","D3","D4")
        )
    )


    // ---------------------------------------------------------
    // категории
    // ---------------------------------------------------------

    val byCategory: Map<String, List<Tuning>> = mapOf(

        "Standard" to generateStandardTunings(),

        "Drop" to generateDropTunings(),

        "Open" to openTunings,

        "Custom" to customTunings
    )
}