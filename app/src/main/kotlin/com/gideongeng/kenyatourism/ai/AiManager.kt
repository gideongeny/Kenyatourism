package com.gideongeng.kenyatourism.ai

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object AiManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Using a stable OpenAI-compatible free provider endpoint
    private const val API_URL = "https://api.openai.com/v1/chat/completions" // Placeholder to be replaced with a real free provider if needed
    private var apiKey: String = ""

    fun initialize(key: String = "") {
        this.apiKey = key
    }

    suspend fun getResponse(prompt: String): String = suspendCancellableCoroutine { continuation ->
        val json = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are 'Jambo', an expert Safari Guide for Kenya Tourism. Provide helpful, inspiring, and accurate information about Kenyan destinations, wildlife, and culture. Be polite, use Swahili words like 'Karibu' or 'Asante', and keep the tone premium.")
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
            .post(body)
            .apply {
                if (apiKey.isNotEmpty()) {
                    addHeader("Authorization", "Bearer $apiKey")
                }
            }
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resume("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { 
                    if (!response.isSuccessful) {
                        if (continuation.isActive) continuation.resume("Jambo! I'm currently taking a short break to watch the sunrise over the Savannah. Please try again in a moment!")
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
                            if (continuation.isActive) continuation.resume("I'm not sure how to answer that right now, Karibu!")
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resume("Error: ${e.message}")
                    }
                }
            }
        })
    }
}
