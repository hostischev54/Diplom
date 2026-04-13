package com.diplom.autotab

import android.content.Context
import android.media.*
import android.net.Uri

class AudioDecoder {

    fun decodeToPCM(
        context: Context,
        uri: Uri,
        onChunk: (ShortArray, Int) -> Unit
    ) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        var trackIndex = -1

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)

            if (mime != null && mime.startsWith("audio/")) {
                trackIndex = i
                break
            }
        }

        if (trackIndex == -1) {
            return
        }

        val format = extractor.getTrackFormat(trackIndex)
        extractor.selectTrack(trackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME) ?: return

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var isEOS = false

        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

        while (true) {

            if (!isEOS) {
                val inputIndex = codec.dequeueInputBuffer(10000)

                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)

                    val sampleSize = inputBuffer?.let {
                        extractor.readSampleData(it, 0)
                    } ?: -1

                    if (sampleSize < 0) {

                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        isEOS = true

                    } else {
                        val presentationTime = extractor.sampleTime

                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            sampleSize,
                            presentationTime,
                            0
                        )

                        extractor.advance()
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)

            if (outputIndex >= 0) {

                val outputBuffer = codec.getOutputBuffer(outputIndex)

                if (outputBuffer != null && bufferInfo.size > 0) {

                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.get(chunk)
                    outputBuffer.clear()

                    if (chunk.size < 2) {
                        codec.releaseOutputBuffer(outputIndex, false)
                        continue
                    }

                    val shortArray = ShortArray(chunk.size / 4)

                    for (i in shortArray.indices) {

                        val leftLow = chunk[i * 4].toInt() and 0xFF
                        val leftHigh = chunk[i * 4 + 1].toInt() shl 8
                        val left = (leftLow or leftHigh).toShort()

                        val rightLow = chunk[i * 4 + 2].toInt() and 0xFF
                        val rightHigh = chunk[i * 4 + 3].toInt() shl 8
                        val right = (rightLow or rightHigh).toShort()

                        shortArray[i] = ((left + right) / 2).toShort()
                    }

                    onChunk(shortArray, sampleRate)
                }

                codec.releaseOutputBuffer(outputIndex, false)
            }

            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                break
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

    }
}