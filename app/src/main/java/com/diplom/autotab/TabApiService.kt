package com.diplom.autotab

import android.content.Context
import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit

class TabApiService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)  // Demucs долго работает
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val serverUrl = "http://YOUR_SERVER_IP:8000"

    fun ping(): Boolean {
        return try {
            val request = Request.Builder().url("$serverUrl/ping").get().build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    fun analyzeAudio(uri: Uri, tuning: String = "standard"): TabApiResult {
        // читаем файл из Uri
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Не удалось открыть файл")

        val bytes = inputStream.readBytes()
        inputStream.close()

        // определяем mime тип
        val mimeType = context.contentResolver.getType(uri) ?: "audio/mpeg"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "audio.${mimeType.substringAfter("/")}",
                RequestBody.create(mimeType.toMediaType(), bytes)
            )
            .addFormDataPart("tuning", tuning)
            .build()

        val request = Request.Builder()
            .url("$serverUrl/analyze")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Пустой ответ от сервера")

        if (!response.isSuccessful) {
            val errorJson = JSONObject(body)
            throw Exception(errorJson.optString("detail", "Ошибка сервера ${response.code}"))
        }

        return parseResponse(JSONObject(body))
    }

    private fun parseResponse(json: JSONObject): TabApiResult {
        val notesArray = json.getJSONArray("notes")
        val notes = (0 until notesArray.length()).map {
            val obj = notesArray.getJSONObject(it)
            NoteData(
                note = obj.getString("note"),
                time = obj.getDouble("time"),
                duration = obj.getDouble("duration"),
                confidence = obj.getDouble("confidence"),
                frequency = obj.getDouble("frequency")
            )
        }

        val tabObj = json.getJSONObject("tab").getJSONObject("strings")
        val strings = (1..6).associate { i ->
            val arr = tabObj.getJSONArray(i.toString())
            i to (0 until arr.length()).map { arr.getString(it) }
        }

        return TabApiResult(notes = notes, strings = strings)
    }
}

data class NoteData(
    val note: String,
    val time: Double,
    val duration: Double,
    val confidence: Double,
    val frequency: Double
)

data class TabApiResult(
    val notes: List<NoteData>,
    val strings: Map<Int, List<String>>
)