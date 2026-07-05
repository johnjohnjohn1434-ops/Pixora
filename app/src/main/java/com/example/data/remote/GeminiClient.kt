package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    private fun isKeyInvalid(key: String?): Boolean {
        return key.isNullOrEmpty() || key == "MY_GEMINI_API_KEY" || key == "GEMINI_API_KEY"
    }

    suspend fun generateCaption(promptText: String): String {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (isKeyInvalid(apiKey)) {
            return "No Gemini API key configured. Configure one in the Secrets panel."
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Generate a catchy, creative, and engaging social media caption (1-2 sentences maximum, no hashtags) based on this prompt or image context: $promptText")))),
            generationConfig = GenerationConfig(temperature = 0.8f)
        )
        return try {
            val response = service.generateContent(apiKey, request)
            val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            resultText?.trim() ?: "No response from AI model. Please try again."
        } catch (e: Exception) {
            "AI Suggestion: ${promptText.take(15)} looks amazing! #explore"
        }
    }

    suspend fun suggestHashtags(promptText: String): String {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (isKeyInvalid(apiKey)) {
            return "#pixora #moments #community #explorer"
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Suggest exactly 4-5 relevant and popular hashtags for this topic (output only space-separated hashtags, starting with #, no other text): $promptText")))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )
        return try {
            val response = service.generateContent(apiKey, request)
            val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            resultText?.trim() ?: "#pixora #explore"
        } catch (e: Exception) {
            "#pixora #moments #community"
        }
    }

    suspend fun moderatePostCaption(caption: String): Boolean {
        // Returns true if content is safe, false if offensive (violence, hate speech, severe harassment, extreme vulgarity)
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (isKeyInvalid(apiKey)) {
            return true // Fallback to safe
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Determine if this social media caption contains extreme violence, hate speech, or harassment. Output exactly 'SAFE' or 'UNSAFE' and nothing else: $caption")))),
            generationConfig = GenerationConfig(temperature = 0.1f)
        )
        return try {
            val response = service.generateContent(apiKey, request)
            val textResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()?.uppercase()
            textResult != "UNSAFE"
        } catch (e: Exception) {
            true // Fallback to safe if network fails
        }
    }
}
