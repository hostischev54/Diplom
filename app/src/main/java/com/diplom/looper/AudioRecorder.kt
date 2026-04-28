package com.diplom.looper

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var outputFile: File? = null

    fun getOutputDir(): File {
        val dir = File(context.filesDir, "looper_tracks")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun startRecording(trackId: Int): String = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 4
        )

        val file = File(getOutputDir(), "track_${trackId}_raw.wav")
        outputFile = file

        audioRecord!!.startRecording()

        val dataOutputStream = FileOutputStream(file)
        // WAV header placeholder — заполним после записи
        dataOutputStream.write(ByteArray(44))

        val buffer = ByteArray(bufferSize)
        var totalBytes = 0

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val read = audioRecord!!.read(buffer, 0, bufferSize)
                if (read > 0) {
                    dataOutputStream.write(buffer, 0, read)
                    totalBytes += read
                }
            }
        }

        recordingJob!!.join()
        dataOutputStream.flush()
        dataOutputStream.close()

        // Пишем WAV заголовок
        writeWavHeader(file, totalBytes)

        file.absolutePath
    }

    fun stopRecording() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun writeWavHeader(file: File, dataSize: Int) {
        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)
        val header = ByteArray(44)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray())
        buf.putInt(dataSize + 36)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16)
        buf.putShort(1)  // PCM
        buf.putShort(1)  // mono
        buf.putInt(SAMPLE_RATE)
        buf.putInt(SAMPLE_RATE * 2)
        buf.putShort(2)
        buf.putShort(16)
        buf.put("data".toByteArray())
        buf.putInt(dataSize)
        raf.write(header)
        raf.close()
    }

    fun isHeadphonesConnected(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        return audioManager.isWiredHeadsetOn ||
                audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                    .any { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES }
    }

    fun registerHeadphoneCallback(
        context: Context,
        onChanged: (Boolean) -> Unit
    ): android.media.AudioDeviceCallback {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val cb = object : android.media.AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<android.media.AudioDeviceInfo>) {
                onChanged(isHeadphonesConnected(context))
            }
            override fun onAudioDevicesRemoved(removed: Array<android.media.AudioDeviceInfo>) {
                onChanged(isHeadphonesConnected(context))
            }
        }
        am.registerAudioDeviceCallback(cb, null)
        return cb
    }

    fun unregisterHeadphoneCallback(
        context: Context,
        cb: android.media.AudioDeviceCallback
    ) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        am.unregisterAudioDeviceCallback(cb)
    }
}