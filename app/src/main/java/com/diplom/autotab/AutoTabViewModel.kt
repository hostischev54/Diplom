package com.diplom.autotab

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

class AutoTabViewModel {

    private val referenceA = 440.0

    private val notes = generateNotes()

    private fun generateNotes(): List<Pair<String, Double>> {
        val names = arrayOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")

        val list = mutableListOf<Pair<String, Double>>()

        for (oct in 2..5) {
            for ((i, n) in names.withIndex()) {
                val midi = (oct + 1) * 12 + i
                val freq = referenceA * Math.pow(2.0, (midi - 69) / 12.0)
                list.add(n + oct to freq)
            }
        }

        return list
    }

    fun mapFrequencyToNote(freq: Double): String {

        return notes.minByOrNull {
            kotlin.math.abs(1200 * kotlin.math.log2(freq / it.second))
        }?.first ?: "--"
    }
}