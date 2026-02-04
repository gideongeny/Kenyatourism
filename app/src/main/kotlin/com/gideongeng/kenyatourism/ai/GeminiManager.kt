package com.gideongeng.kenyatourism.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object GeminiManager {
    private var model: GenerativeModel? = null
    
    // Fallback if API key is not provided or fails
    private val isMockMode = true 

    fun initialize(apiKey: String) {
        model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
    }

    suspend fun getResponse(prompt: String): String {
        val generativeModel = model ?: return "Gemini AI is not initialized. Please provide an API Key."
        
        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "I'm not sure how to answer that."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }
    
    // System prompt to give Gemini the "Safari Guide" persona
    fun getSafariPrompt(query: String): String {
        return """
            You are 'Jambo', an expert Safari Guide for Kenya Tourism. 
            Your goal is to provide helpful, inspiring, and accurate information about Kenyan destinations, wildlife, and culture.
            Be polite, use some Swahili words like 'Karibu' or 'Asante', and keep the tone premium and professional.
            
            User Question: $query
        """.trimIndent()
    }
}
