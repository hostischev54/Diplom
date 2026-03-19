package com.diplom.tuner.models

import kotlin.math.pow
import kotlin.math.abs
import kotlin.math.log2

object NoteFrequencies {

    private val notes = arrayOf(
        "C", "C#", "D", "D#", "E",
        "F", "F#", "G", "G#", "A", "A#", "B"
    )
    fun findClosestString(
        detectedFreq: Double,
        tuning: Tuning,
        referenceA: Double,
    ): Int? {
        val frequencies = getTuningFrequencies(tuning, referenceA)
        if (frequencies.isEmpty() || detectedFreq <= 0) return null

        var closestIndex = 0
        var minDiff = Double.MAX_VALUE

        frequencies.forEachIndexed { index, freq ->
            if (freq <= 0) return@forEachIndexed
            val diff = abs(12 * log2(detectedFreq / freq))
            if (diff < minDiff) {
                minDiff = diff
                closestIndex = index
            }
        }

        return if (minDiff < 12.0) closestIndex else null
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