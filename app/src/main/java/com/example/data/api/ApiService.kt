package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.Categorizer
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

// --- Gemini API Moshi Models ---

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    val type: String,
    val properties: Map<String, SchemaProperty>? = null,
    val required: List<String>? = null,
    val items: SchemaProperty? = null
)

@JsonClass(generateAdapter = true)
data class SchemaProperty(
    val type: String,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    val mimeType: String,
    val schema: ResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPartResponse(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContentResponse(
    val parts: List<GeminiPartResponse>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContentResponse? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null
)

// --- oEmbed Model ---

@JsonClass(generateAdapter = true)
data class OEmbedResponse(
    val title: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null
)

// --- Helper Payload Models ---

@JsonClass(generateAdapter = true)
data class GeminiEnrichResult(
    val title: String,
    val summary: String,
    val tags: List<String>,
    val category: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSearchResultItem(
    val id: String,
    val reason: String
)

// --- Retrofit Service Interfaces ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

interface GenericHttpService {
    @GET
    suspend fun getOEmbed(@Url url: String): OEmbedResponse
}

// --- API Service Client ---

object ApiService {
    private const val TAG = "ApiService"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val genericService: GenericHttpService by lazy {
        Retrofit.Builder()
            .baseUrl("https://localhost/") // placeholder base URL
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GenericHttpService::class.java)
    }

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
        } catch (e: Exception) {
            ""
        }
    }

    fun isApiConfigured(): Boolean {
        return getApiKey().isNotEmpty()
    }

    // Call oEmbed endpoints for YouTube/Vimeo
    suspend fun fetchOEmbed(url: String): OEmbedResponse? {
        val clean = url.trim()
        val encodedUrl = URLEncoder.encode(clean, "UTF-8")
        return try {
            if (clean.contains("youtube.com", ignoreCase = true) || clean.contains("youtu.be", ignoreCase = true)) {
                val oEmbedUrl = "https://www.youtube.com/oembed?url=$encodedUrl&format=json"
                genericService.getOEmbed(oEmbedUrl)
            } else if (clean.contains("vimeo.com", ignoreCase = true)) {
                val oEmbedUrl = "https://vimeo.com/api/oembed.json?url=$encodedUrl"
                genericService.getOEmbed(oEmbedUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling oEmbed for $url", e)
            null
        }
    }

    // Call Gemini API to parse and enrich any URL
    suspend fun enrichUrlWithAI(url: String): GeminiEnrichResult? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return null

        val promptJsonSchema = ResponseSchema(
            type = "OBJECT",
            properties = mapOf(
                "title" to SchemaProperty("STRING", "Correct webpage title name"),
                "summary" to SchemaProperty("STRING", "Concise 1 to 2 sentence page content summary"),
                "tags" to SchemaProperty("ARRAY", "2 to 4 topical tags"),
                "category" to SchemaProperty("STRING", "Output one best category of the page from: jobs, socials, videos, articles")
            ),
            required = listOf("title", "summary", "tags", "category")
        )

        val systemInstructionText = """
            You are LinkHive's URL parser. You extract or infer valid title names, summaries, tags and correct semantic category.
            You must reply ONLY with a valid JSON representation matching the requested response schema format.
        """.trimIndent()

        val prompt = "Analyze the contents/nature of the following URL: $url to suggest appropriate metadata. Return correct title, a concise 1-2 sentence description, 2-4 tags, and pick the best category among (jobs, socials, videos, articles)."

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(
                        mimeType = "application/json",
                        schema = promptJsonSchema
                    )
                ),
                temperature = 0.2f
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
        )

        return try {
            val response = geminiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d(TAG, "Gemini Response json: $jsonText")
                val adapter = moshi.adapter(GeminiEnrichResult::class.java)
                adapter.fromJson(jsonText)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini enrich API for $url", e)
            null
        }
    }

    // Reason over links via Natural Language Search
    suspend fun searchWithAI(query: String, compactListText: String): List<GeminiSearchResultItem> {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return emptyList()

        val searchItemSchema = ResponseSchema(
            type = "ARRAY",
            items = SchemaProperty("OBJECT", "Matches")
        )

        val systemInstructionText = """
            You are LinkHive's smart search assistant.
            The user wants to find links in their saved list using semantic natural language query.
            You analyze the user's saved list, select matching IDs, and write a helpful 1-sentence matching explanation.
            You must reply ONLY with a JSON array where each object has "id" and "reason" keys.
            Example: [ { "id": "uuid-1", "reason": "Mentions offline capabilities which matches offline-first" } ]
            If none match, return empty array []. No formatting or codeblocks.
        """.trimIndent()

        val prompt = """
            Query: $query
            
            Saved Links List:
            $compactListText
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(
                        mimeType = "application/json",
                        schema = ResponseSchema(
                            type = "ARRAY",
                            items = SchemaProperty("OBJECT", "Matches with keys 'id' and 'reason'")
                        )
                    )
                ),
                temperature = 0.2f
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
        )

        try {
            val response = geminiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d(TAG, "Gemini search results: $jsonText")
                val listType = Types.newParameterizedType(List::class.java, GeminiSearchResultItem::class.java)
                val adapter = moshi.adapter<List<GeminiSearchResultItem>>(listType)
                return adapter.fromJson(jsonText) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini search API", e)
        }
        return emptyList()
    }
}
