package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.Categorizer
import com.example.data.api.ApiService
import com.example.data.database.AppDatabase
import com.example.data.model.CategoryRule
import com.example.data.model.LinkRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class LinkRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val linkDao = database.linkDao()
    private val categoryRuleDao = database.categoryRuleDao()

    val allLinks: Flow<List<LinkRecord>> = linkDao.getAllLinksFlow()
    val allRules: Flow<List<CategoryRule>> = categoryRuleDao.getAllRulesFlow()

    // Optimistic Save
    suspend fun saveLinkOptimistic(url: String): LinkRecord = withContext(Dispatchers.IO) {
        val cleanedUrl = url.trim()
        val rules = categoryRuleDao.getAllRules()
        val info = Categorizer.categorize(cleanedUrl, rules)

        val id = UUID.randomUUID().toString()
        val searchBlob = createSearchBlob(
            name = info.name,
            source = info.source,
            url = cleanedUrl,
            category = info.category,
            tags = "",
            summary = ""
        )

        val link = LinkRecord(
            id = id,
            url = cleanedUrl,
            name = info.name,
            category = info.category,
            source = info.source,
            status = "not_applied",
            readState = "unread",
            pinned = false,
            tags = "",
            summary = null,
            thumbnail = null,
            createdAt = System.currentTimeMillis(),
            searchBlob = searchBlob
        )

        linkDao.insertLink(link)
        link
    }

    // Secondary Enrichment Step
    suspend fun enrichLink(link: LinkRecord): Unit = withContext(Dispatchers.IO) {
        try {
            // 0. Try direct HTML scraping (fantastic for Pinterest & redirected or active links)
            val scrapedTitle = ApiService.scrapeTitle(link.url)

            // 1. Try oEmbed if YouTube/Vimeo
            val oEmbed = ApiService.fetchOEmbed(link.url)
            val oEmbedTitle = oEmbed?.title
            val oEmbedThumbnail = oEmbed?.thumbnailUrl

            // 2. Try Gemini API
            var aiTitle: String? = null
            var aiSummary: String? = null
            var aiTags: List<String>? = null
            var aiCategory: String? = null

            if (ApiService.isApiConfigured()) {
                val geminiResult = ApiService.enrichUrlWithAI(link.url)
                if (geminiResult != null) {
                    aiTitle = geminiResult.title
                    aiSummary = geminiResult.summary
                    aiTags = geminiResult.tags
                    aiCategory = geminiResult.category
                }
            }

            // Derive final merged values
            val finalTitle = scrapedTitle ?: oEmbedTitle ?: aiTitle ?: link.name
            val finalThumbnail = oEmbedThumbnail ?: link.thumbnail
            val finalSummary = aiSummary ?: link.summary
            val finalTags = if (aiTags != null) aiTags.joinToString(",") else link.tags
            
            // Refine category if optimistic was uncategorized and AI suggested one
            val finalCategory = if (link.category == "uncategorized" && aiCategory != null) {
                val validCategories = setOf("jobs", "socials", "videos", "articles")
                if (validCategories.contains(aiCategory.lowercase())) aiCategory.lowercase() else link.category
            } else {
                link.category
            }

            // Recompute search blob with enriched metadata
            val finalSearchBlob = createSearchBlob(
                name = finalTitle,
                source = link.source,
                url = link.url,
                category = finalCategory,
                tags = finalTags,
                summary = finalSummary
            )

            val updatedLink = link.copy(
                name = finalTitle,
                category = finalCategory,
                tags = finalTags,
                summary = finalSummary,
                thumbnail = finalThumbnail,
                searchBlob = finalSearchBlob
            )

            linkDao.updateLink(updatedLink)
        } catch (e: Exception) {
            Log.e("LinkRepository", "Enrichment failed for link ${link.url}", e)
        }
    }

    // Update Status, Pinned or ReadState
    suspend fun updateLink(link: LinkRecord) = withContext(Dispatchers.IO) {
        val updatedBlob = createSearchBlob(
            name = link.name,
            source = link.source,
            url = link.url,
            category = link.category,
            tags = link.tags,
            summary = link.summary
        )
        linkDao.updateLink(link.copy(searchBlob = updatedBlob))
    }

    suspend fun deleteLink(id: String) = withContext(Dispatchers.IO) {
        linkDao.deleteLinkById(id)
    }

    // Add / edit categorization rules
    suspend fun insertRule(domain: String, category: String) = withContext(Dispatchers.IO) {
        categoryRuleDao.insertRule(CategoryRule(domain.lowercase().trim(), category))
    }

    suspend fun deleteRuleByDomain(domain: String) = withContext(Dispatchers.IO) {
        categoryRuleDao.deleteRuleByDomain(domain)
    }

    private fun createSearchBlob(
        name: String,
        source: String,
        url: String,
        category: String,
        tags: String,
        summary: String?
    ): String {
        return "$name $source $url $category $tags ${summary ?: ""}".lowercase()
    }
}
