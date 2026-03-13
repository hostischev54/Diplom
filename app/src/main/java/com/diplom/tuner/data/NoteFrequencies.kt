package com.diplom.tuner.models

import kotlin.math.pow

object NoteFrequencies {

    private val notes = arrayOf(
        "C", "C#", "D", "D#", "E",
        "F", "F#", "G", "G#", "A", "A#", "B"
    )
    fun findClosestString(
        detectedFreq: Double,
        tuning: Tuning,
        referenceA: Double
    ): Int? {

        val freqs = getTuningFrequencies(tuning, referenceA)

        var closestIndex: Int? = null
        var smallestDiff = Double.MAX_VALUE

        freqs.forEachIndexed { index, freq ->

            val diff = kotlin.math.abs(freq - detectedFreq)

            if (diff < smallestDiff) {
                smallestDiff = diff
                closestIndex = index
            }
        }

        return closestIndex
    }
    /**
     * Преобразует список нот строя (например, ["C2","G2",...]) в частоты
     * @param tuning - объект Tuning со списком струн
     * @param referenceA - эталон A4 (например, 440.0)
     * @return List<Double> - частоты каждой струны
     */
    fun getTuningFrequencies(tuning: Tuning, referenceA: Double): List<Double> {
        return tuning.strings.map { noteWithOct ->
            val match = Regex("([A-G]#?)(\\d)").find(noteWithOct)
            if (match != null) {
                val (note, octaveStr) = match.destructured
                val octave = octaveStr.toInt()
                val i = notes.indexOf(note)
                val midi = (octave + 1) * 12 + i
                referenceA * 2.0.pow((midi - 69) / 12.0)
            } else {
                0.0 // на случай ошибки
            }
        }
    }
}