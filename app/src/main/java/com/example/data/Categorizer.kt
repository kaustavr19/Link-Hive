package com.example.data

import com.example.data.model.CategoryRule
import java.util.Locale

data class CategorizedInfo(
    val category: String,
    val source: String,
    val name: String
)

object Categorizer {

    private val stopwords = setOf(
        "of", "the", "in", "for", "and", "a", "an", "to", "at", "by", "on", "with", 
        "from", "but", "or", "nor", "as", "is", "it", "that", "this", "to"
    )

    private val acronyms = setOf(
        "ui", "ux", "api", "css", "ai", "ios", "html", "pwa", "url", "ip", "db", "sdk", "rest", "json", "http"
    )

    fun cleanUrl(url: String): String {
        var cleaned = url.trim()
        if (cleaned.startsWith("https://")) {
            cleaned = cleaned.substring(8)
        } else if (cleaned.startsWith("http://")) {
            cleaned = cleaned.substring(7)
        }
        if (cleaned.startsWith("www.")) {
            cleaned = cleaned.substring(4)
        }
        return cleaned
    }

    fun isOpaqueOrNumeric(s: String): Boolean {
        if (s.isEmpty()) return true
        if (s.all { it.isDigit() }) return true
        // Check if it's a long hex string/guid/id (e.g. length >= 12 with mixed letters/numbers and no normal vowels)
        if (s.length >= 12 && s.matches(Regex("^[a-fA-F0-9_\\-]+$"))) {
            // If it has lots of digits relative to letters, or has hex characters
            val digitsCount = s.count { it.isDigit() }
            if (digitsCount > s.length * 0.3) return true
        }
        return false
    }

    fun deriveSource(url: String, category: String): String {
        val cleaned = cleanUrl(url)
        val segments = cleaned.split("/").filter { it.isNotEmpty() }
        if (segments.isEmpty()) return "unknown"
        val domain = segments[0]
        val bareDomain = domain.split(":")[0] // strip port

        if (category == "socials" && segments.size > 1) {
            val reserved = setOf(
                "explore", "p", "reels", "status", "hashtag", "search", "home", 
                "photos", "share", "profile", "jobs", "feed", "stories", "reels"
            )
            for (i in 1 until segments.size) {
                val segment = segments[i].lowercase(Locale.ROOT)
                if (segment.isNotEmpty() && !reserved.contains(segment) && !segment.all { it.isDigit() }) {
                    // Strip query params or fragments if any
                    val cleanSegment = segment.substringBefore("?").substringBefore("#")
                    if (cleanSegment.isNotEmpty()) {
                        return "@$cleanSegment"
                    }
                }
            }
        }
        return bareDomain
    }

    fun deriveName(url: String): String {
        // Strip query parameters and anchors
        var pathPart = url
        if (pathPart.contains("?")) {
            pathPart = pathPart.substringBefore("?")
        }
        if (pathPart.contains("#")) {
            pathPart = pathPart.substringBefore("#")
        }
        val cleanedPath = cleanUrl(pathPart)
        val segments = cleanedPath.split("/").filter { it.isNotEmpty() }
        if (segments.size <= 1) {
            // Fallback to domain label
            val domain = if (segments.isNotEmpty()) segments[0] else "Link"
            val label = domain.split(".")[0]
            return label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        // Drop the domain part
        val candidates = segments.drop(1).filter { segment ->
            val cleanSeg = segment.substringBefore("?").substringBefore("#")
            !isOpaqueOrNumeric(cleanSeg)
        }

        val bestSegment = if (candidates.isNotEmpty()) {
            candidates.maxByOrNull { s ->
                val cleanS = s.substringBefore("?").substringBefore("#")
                val wordCount = cleanS.split('-', '_', '+').filter { it.length > 1 }.size
                wordCount * 10 + cleanS.length
            } ?: candidates.first()
        } else {
            segments[1] // Default fallback segment
        }

        val deSlugged = bestSegment.substringBefore("?").substringBefore("#")
            .replace('-', ' ')
            .replace('_', ' ')
            .replace('+', ' ')
            .trim()

        val words = deSlugged.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) {
            return "Link"
        }

        var finalWords = words
        // Strip trailing opaque word if any
        if (finalWords.size > 1 && isOpaqueOrNumeric(finalWords.last())) {
            finalWords = finalWords.dropLast(1)
        }

        val capitalized = finalWords.mapIndexed { index, word ->
            val lower = word.lowercase(Locale.ROOT)
            val isFirstOrLast = index == 0 || index == finalWords.size - 1

            if (acronyms.contains(lower)) {
                if (lower == "ios") "iOS" else lower.uppercase(Locale.ROOT)
            } else if (stopwords.contains(lower) && !isFirstOrLast) {
                lower
            } else {
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }.joinToString(" ")

        return capitalized
    }

    fun categorize(url: String, rules: List<CategoryRule>): CategorizedInfo {
        val cleaned = cleanUrl(url)
        val matchedRule = rules.sortedByDescending { it.domain.length }.firstOrNull { rule ->
            cleaned.contains(rule.domain, ignoreCase = true)
        }
        val category = matchedRule?.category ?: "uncategorized"
        val source = deriveSource(url, category)
        val name = deriveName(url)
        return CategorizedInfo(category, source, name)
    }

    fun getDefaultRules(): List<CategoryRule> {
        return listOf(
            CategoryRule("youtube.com", "videos"),
            CategoryRule("youtu.be", "videos"),
            CategoryRule("vimeo.com", "videos"),
            CategoryRule("linkedin.com/jobs", "jobs"),
            CategoryRule("indeed.com", "jobs"),
            CategoryRule("greenhouse.io", "jobs"),
            CategoryRule("lever.co", "jobs"),
            CategoryRule("ashbyhq.com", "jobs"),
            CategoryRule("workable.com", "jobs"),
            CategoryRule("instagram.com", "socials"),
            CategoryRule("x.com", "socials"),
            CategoryRule("twitter.com", "socials"),
            CategoryRule("tiktok.com", "socials"),
            CategoryRule("facebook.com", "socials"),
            CategoryRule("threads.net", "socials"),
            CategoryRule("medium.com", "articles"),
            CategoryRule("substack.com", "articles"),
            CategoryRule("dev.to", "articles"),
            CategoryRule("hashnode.dev", "articles")
        )
    }
}
