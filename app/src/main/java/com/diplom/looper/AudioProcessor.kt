package com.diplom.looper

import android.media.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioProcessor {

    private const val SAMPLE_RATE = 44100
    private const val CROSSFADE_MS = 50

    // Конвертация MP3 → WAV через MediaExtractor + MediaCodec
    suspend fun convertMp3ToWav(inputPath: String, outputPath: String): Boolean =
        withContext(Dispatchers.IO) {
            var extractor: MediaExtractor? = null
            var codec: MediaCodec? = null
            var fos: FileOutputStream? = null
            try {
                extractor = MediaExtractor()
                extractor.setDataSource(inputPath)

                var audioTrackIndex = -1
                var format: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val fmt = extractor.getTrackFormat(i)
                    if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                        audioTrackIndex = i
                        format = fmt
                        break
                    }
                }
                if (audioTrackIndex < 0 || format == null) return@withContext false

                extractor.selectTrack(audioTrackIndex)
                val mime = format.getString(MediaFormat.KEY_MIME)!!
                codec = MediaCodec.createDecoderByType(mime)
                codec.configure(format, null, null, 0)
                codec.start()

                // Открываем файл и резервируем место под WAV-заголовок
                fos = FileOutputStream(outputPath)
                fos.write(ByteArray(44)) // placeholder

                val bufferInfo = MediaCodec.BufferInfo()
                var done = false
                var totalPcmBytes = 0L
                var outputChannels = 1
                var outputSampleRate = SAMPLE_RATE

                while (!done) {
                    // Подаём данные в декодер
                    val inIdx = codec.dequeueInputBuffer(10000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        buf.clear()
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                        } else {
                            codec.queueInputBuffer(
                                inIdx, 0, sampleSize, extractor.sampleTime, 0
                            )
                            extractor.advance()
                        }
                    }

                    // Читаем декодированный PCM и сразу пишем на диск
                    val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    when {
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outFmt = codec.outputFormat
                            outputChannels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            outputSampleRate = try {
                                outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            } catch (e: Exception) { SAMPLE_RATE }
                        }
                        outIdx >= 0 -> {
                            val buf = codec.getOutputBuffer(outIdx)!!
                            buf.position(bufferInfo.offset)
                            buf.limit(bufferInfo.offset + bufferInfo.size)

                            // Конвертируем стерео → моно на лету, пишем чанками
                            val shortBuf = buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val chunkOut = ByteArrayOutputStream()
                            val tmpBuf = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)

                            if (outputChannels == 2) {
                                while (shortBuf.remaining() >= 2) {
                                    val l = shortBuf.get().toInt()
                                    val r = shortBuf.get().toInt()
                                    val mono = ((l + r) / 2).toShort()
                                    tmpBuf.putShort(mono)
                                    if (!tmpBuf.hasRemaining()) {
                                        fos.write(tmpBuf.array())
                                        totalPcmBytes += tmpBuf.capacity()
                                        tmpBuf.clear()
                                    }
                                }
                            } else {
                                while (shortBuf.hasRemaining()) {
                                    tmpBuf.putShort(shortBuf.get())
                                    if (!tmpBuf.hasRemaining()) {
                                        fos.write(tmpBuf.array())
                                        totalPcmBytes += tmpBuf.capacity()
                                        tmpBuf.clear()
                                    }
                                }
                            }
                            // Записываем остаток буфера
                            if (tmpBuf.position() > 0) {
                                fos.write(tmpBuf.array(), 0, tmpBuf.position())
                                totalPcmBytes += tmpBuf.position()
                            }

                            codec.releaseOutputBuffer(outIdx, false)

                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                done = true
                            }
                        }
                    }
                }

                fos.flush()
                fos.close()
                fos = null

                // Теперь записываем правильный WAV-заголовок в начало файла
                writeWavHeaderToFile(outputPath, totalPcmBytes, outputSampleRate)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                fos?.close()
                codec?.stop()
                codec?.release()
                extractor?.release()
            }
        }

    // Дописываем заголовок в уже существующий файл
    private fun writeWavHeaderToFile(path: String, dataBytes: Long, sampleRate: Int) {
        val raf = java.io.RandomAccessFile(path, "rw")
        raf.seek(0)
        val header = ByteArray(44)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray())
        buf.putInt((dataBytes + 36).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16)
        buf.putShort(1)   // PCM
        buf.putShort(1)   // mono
        buf.putInt(sampleRate)
        buf.putInt(sampleRate * 2)
        buf.putShort(2)
        buf.putShort(16)
        buf.put("data".toByteArray())
        buf.putInt(dataBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        raf.write(header)
        raf.close()
    }

    internal fun readPcm(path: String, startMs: Long = 0, endMs: Long = 0): ShortArray {
        val file = File(path)
        val bytes = file.readBytes()
        val totalSamples = (bytes.size - 44) / 2
        val startSample = ((startMs * SAMPLE_RATE) / 1000).toInt().coerceIn(0, totalSamples)
        val endSample = if (endMs > 0)
            ((endMs * SAMPLE_RATE) / 1000).toInt().coerceIn(startSample, totalSamples)
        else totalSamples
        val count = endSample - startSample
        val result = ShortArray(count)
        val buf = ByteBuffer.wrap(bytes, 44 + startSample * 2, count * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) result[i] = buf.short
        return result
    }

    private fun applyCrossfade(pcm: ShortArray): ShortArray {
        val fadeSamples = (CROSSFADE_MS * SAMPLE_RATE / 1000).coerceAtMost(pcm.size / 4)
        val result = pcm.copyOf()
        for (i in 0 until fadeSamples) {
            val factor = i.toFloat() / fadeSamples
            result[i] = (result[i] * factor).toInt().toShort()
            val j = result.size - 1 - i
            result[j] = (result[j] * factor).toInt().toShort()
        }
        return result
    }

    fun writePcmToWav(pcm: ShortArray, outputPath: String) {
        val dataSize = pcm.size * 2
        val fos = FileOutputStream(outputPath)
        val header = ByteArray(44)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray())
        buf.putInt(dataSize + 36)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16)
        buf.putShort(1)
        buf.putShort(1)
        buf.putInt(SAMPLE_RATE)
        buf.putInt(SAMPLE_RATE * 2)
        buf.putShort(2)
        buf.putShort(16)
        buf.put("data".toByteArray())
        buf.putInt(dataSize)
        fos.write(header)
        val pcmBytes = ByteArray(dataSize)
        ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).let { b ->
            pcm.forEach { b.putShort(it) }
        }
        fos.write(pcmBytes)
        fos.flush()
        fos.close()
    }

    suspend fun processLoop(
        inputPath: String,
        outputPath: String,
        trimStartMs: Long,
        trimEndMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pcm = readPcm(inputPath, trimStartMs, trimEndMs)
            if (pcm.isEmpty()) return@withContext false
            val processed = applyCrossfade(pcm)
            writePcmToWav(processed, outputPath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun renderToM4a(
        tracks: List<LooperTrack>,
        loopCount: Int = 4,
        outputPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val activeTracks = tracks.filter { !it.isMuted && it.processedPath.isNotEmpty() }
            if (activeTracks.isEmpty()) return@withContext false

            val pcmTracks = activeTracks.map { track ->
                Pair(readPcm(track.processedPath), track.volume)
            }
            val loopLen = pcmTracks.maxOf { it.first.size }
            val totalSamples = loopLen * loopCount
            val mixed = ShortArray(totalSamples)

            for ((pcm, volume) in pcmTracks) {
                for (i in 0 until totalSamples) {
                    val sample = pcm[i % pcm.size] * volume
                    val sum = mixed[i] + sample.toInt()
                    mixed[i] = sum.coerceIn(
                        Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
                    ).toShort()
                }
            }
            encodeToAac(mixed, outputPath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun encodeToAac(pcm: ShortArray, outputPath: String) {
        val mimeType = MediaFormat.MIMETYPE_AUDIO_AAC
        val format = MediaFormat.createAudioFormat(mimeType, SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 192000)
            setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val codec = MediaCodec.createEncoderByType(mimeType)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        codec.start()

        var audioTrackIndex = -1
        var inputDone = false
        var outputDone = false
        var pcmOffset = 0
        val samplesPerFrame = 1024
        val bufferInfo = MediaCodec.BufferInfo()

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuf = codec.getInputBuffer(inputIndex)!!
                    inputBuf.clear()
                    val remaining = pcm.size - pcmOffset
                    if (remaining <= 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        val chunk = minOf(samplesPerFrame, remaining)
                        for (i in 0 until chunk) inputBuf.putShort(pcm[pcmOffset + i])
                        val pts = (pcmOffset.toLong() * 1_000_000L) / SAMPLE_RATE
                        codec.queueInputBuffer(inputIndex, 0, chunk * 2, pts, 0)
                        pcmOffset += chunk
                    }
                }
            }
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    audioTrackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                }
                outputIndex >= 0 -> {
                    val outputBuf = codec.getOutputBuffer(outputIndex)!!
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && audioTrackIndex >= 0) {
                        outputBuf.position(bufferInfo.offset)
                        outputBuf.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(audioTrackIndex, outputBuf, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }
        }
        codec.stop()
        codec.release()
        muxer.stop()
        muxer.release()
    }
}