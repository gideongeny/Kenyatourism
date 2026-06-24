package com.gideongeng.kenyatourism.ai

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object AiManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"
    private var apiKey: String = "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"

    fun initialize(key: String = "") {
        if (key.isNotEmpty()) this.apiKey = key
    }

    suspend fun getResponse(prompt: String): String = suspendCancellableCoroutine { continuation ->
        val json = JSONObject().apply {
            put("model", "google/gemini-2.5-flash:free")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are Jambo, a friendly and expert Kenya Tourism Guide. Your goal is to help users explore the beauty of Kenya, from the Maasai Mara to the shores of Diani. Be enthusiastic, educational, and helpful.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://gideongeng.com/kenyatourism")
            .addHeader("X-Title", "Kenya Tourism App")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resume("Jambo! I'm currently taking a short break to recharge. Please try again in a moment!")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        if (continuation.isActive) continuation.resume("Jambo! I'm having a bit of trouble connecting to my travel guides. Let me check my map and try again!")
                        return
                    }

                    val responseBody = response.body?.string() ?: ""
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val choices = jsonResponse.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
                            if (continuation.isActive) continuation.resume(content)
                        } else {
                            if (continuation.isActive) continuation.resume("Jambo! That's a great question, but I'm not sure I understood. Could you tell me more?")
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resume("Jambo! My safari radio is a bit fuzzy. Mind asking that again?")
                    }
                }
            }
        })
    }
}
