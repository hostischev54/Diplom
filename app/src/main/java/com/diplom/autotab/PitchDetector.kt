package com.diplom.autotab

object PitchDetector {

    fun detectPitch(
        buffer: ShortArray,
        sampleRate: Int
    ): Double {

        val size = buffer.size

        var bestLag = 0
        var maxCorr = 0.0

        val minLag = sampleRate / 1200   // верхняя нота
        val maxLag = sampleRate / 70     // нижняя нота (гитара)

        for (lag in minLag..maxLag) {

            var corr = 0.0

            for (i in 0 until size - lag) {
                corr += buffer[i] * buffer[i + lag]
            }

            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }

        return if (bestLag > 0)
            sampleRate.toDouble() / bestLag
        else
            -1.0
    }
}