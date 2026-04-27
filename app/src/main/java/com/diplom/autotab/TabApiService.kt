package com.diplom.autotab

import android.content.Context
import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.toString

class TabApiService(private val context: Context) {

    private val pingClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val serverUrl = "http://192.168.0.103:8000"

    fun ping(): Boolean {
        return try {
            val request = Request.Builder().url("$serverUrl/ping").get().build()
            val response = pingClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    fun analyzeAudio(uri: Uri, tuning: String = "standard"): TabApiResult {

        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Не удалось открыть файл")

        val bytes = inputStream.readBytes()
        inputStream.close()

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
        android.util.Log.d("TabApi", "RAW JSON: ${json.toString()}")
        val notesArray = json.getJSONArray("notes")
        val notes = (0 until notesArray.length()).map {
            val obj = notesArray.getJSONObject(it)
            NoteData(
                note = obj.optString("note", "?"),
                time = obj.optDouble("time", 0.0),
                duration = obj.optDouble("duration", 0.0),
                confidence = obj.optDouble("confidence", 0.0),
                frequency = obj.optDouble("frequency", 0.0)
            )
        }

        val tabObj = json.getJSONObject("strings")

        val strings = (1..6).associate { i ->
            val arr = tabObj.getJSONArray(i.toString())
            i to (0 until arr.length()).map { arr.getString(it) }
        }
        val stringNames = mutableMapOf<Int, String>()
        if (json.has("string_names")) {
            val namesObj = json.getJSONObject("string_names")
            for (i in 1..6) {
                stringNames[i] = namesObj.optString(i.toString(), "")
            }
        }
        val tuning = json.optString("tuning", "")   // ← добавить перед return
        android.util.Log.d("TabApi", "Строй от сервера: $tuning")

        return TabApiResult(notes = notes, strings = strings, tuning = tuning, stringNames = stringNames)
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
    val strings: Map<Int, List<String>>,
    val tuning: String = "",
    val stringNames: Map<Int, String> = emptyMap()
)