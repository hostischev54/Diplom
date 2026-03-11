package com.diplom.tuner.models

data class Tuning(
    val name: String,          // Подпись для кнопки (Sharp/Flat)
    val strings: List<String>  // Ноты для каждой струны без октав
)

object Tunings {

    private val noteOrderSharps = listOf(
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    )

    private val noteOrderFlats = listOf(
        "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"
    )

    // --- Стартовые строи ---
    private val standardBase = listOf("E", "A", "D", "G", "B", "E")
    private val dropBase = listOf("D", "A", "D", "G", "B", "E")

    // --- Генерация стандартных строев от E до A ---
    private fun generateStandardTunings(): List<Tuning> {
        val result = mutableListOf<Tuning>()
        var currentRootIndex = noteOrderSharps.indexOf("E")
        val endIndex = noteOrderSharps.indexOf("A")

        while (true) {
            val sharpStrings = standardBase.map { note ->
                val idx = (noteOrderSharps.indexOf(note) + currentRootIndex - noteOrderSharps.indexOf("E") + 12) % 12
                noteOrderSharps[idx]
            }
            val flatStrings = standardBase.map { note ->
                val idx = (noteOrderFlats.indexOf(note) + currentRootIndex - noteOrderSharps.indexOf("E") + 12) % 12
                noteOrderFlats[idx]
            }

            val thinSpace = "\u2009"

            val name = if (sharpStrings == flatStrings) {
                sharpStrings.joinToString(thinSpace)
            } else {
                sharpStrings.joinToString(thinSpace) +
                        " │ " +
                        flatStrings.joinToString(thinSpace)
            }

            result.add(Tuning(name = name, strings = sharpStrings))

            if (currentRootIndex == endIndex) break
            currentRootIndex = (currentRootIndex - 1 + 12) % 12
        }

        return result
    }

    // --- Генерация дроп-тюнингов от Drop D до Drop A ---
    private fun generateDropTunings(): List<Tuning> {
        val result = mutableListOf<Tuning>()
        var currentRootIndex = noteOrderSharps.indexOf("D")
        val endIndex = noteOrderSharps.indexOf("A") // минимальный Drop A

        while (true) {
            val sharpStrings = dropBase.map { note ->
                val idx = (noteOrderSharps.indexOf(note) + currentRootIndex - noteOrderSharps.indexOf("D") + 12) % 12
                noteOrderSharps[idx]
            }
            val flatStrings = dropBase.map { note ->
                val idx = (noteOrderFlats.indexOf(note) + currentRootIndex - noteOrderSharps.indexOf("D") + 12) % 12
                noteOrderFlats[idx]
            }

            val thinSpace = "\u2009"

            val name = if (sharpStrings == flatStrings) {
                sharpStrings.joinToString(thinSpace)
            } else {
                sharpStrings.joinToString(thinSpace) +
                        " │ " +
                        flatStrings.joinToString(thinSpace)
            }

            result.add(Tuning(name = name, strings = sharpStrings))

            if (currentRootIndex == endIndex) break
            currentRootIndex = (currentRootIndex - 1 + 12) % 12
        }

        return result
    }
    // --- Open tunings ---
    // --- Open tunings с буквенными обозначениями струн ---
    private val openTunings = listOf(
        Tuning("D G D G B D", listOf("D", "G", "D", "G", "B", "D")),  // Open G
        Tuning("D A D F# A D │ D A D Gb A D", listOf("D", "A", "D", "F#", "A", "D")), // Open D
        Tuning("E B E G# B E │ E B E Ab B E", listOf("E", "B", "E", "G#", "B", "E")), // Open E
        Tuning("E A E A C# E │ E A E A Db E", listOf("E", "A", "E", "A", "C#", "E")), // Open A
        Tuning("C G C G C E", listOf("C", "G", "C", "G", "C", "E"))    // Open C
    )


    // --- Категории строев ---
    val byCategory: Map<String, List<Tuning>> = mapOf(
        "Standard" to generateStandardTunings(),
        "Drop" to generateDropTunings(),
        "Open" to openTunings,
        "Custom" to emptyList()
    )
}