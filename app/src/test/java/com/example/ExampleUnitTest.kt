package com.example

import com.example.data.Categorizer
import com.example.data.model.CategoryRule
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    private val sampleRules = listOf(
        CategoryRule("youtube.com", "videos"),
        CategoryRule("youtu.be", "videos"),
        CategoryRule("linkedin.com/jobs", "jobs"),
        CategoryRule("indeed.com", "jobs"),
        CategoryRule("instagram.com", "socials"),
        CategoryRule("x.com", "socials"),
        CategoryRule("medium.com", "articles"),
        CategoryRule("dev.to", "articles")
    )

    @Test
    fun testCategoryMapping() {
        // Videos mapped
        val youtube = Categorizer.categorize("https://www.youtube.com/watch?v=123", sampleRules)
        assertEquals("videos", youtube.category)

        // Jobs mapped
        val linkedinJobs = Categorizer.categorize("https://linkedin.com/jobs/view/999", sampleRules)
        assertEquals("jobs", linkedinJobs.category)

        // Socials mapped
        val twitter = Categorizer.categorize("https://x.com/leomessi/status/456", sampleRules)
        assertEquals("socials", twitter.category)

        // Articles mapped
        val medium = Categorizer.categorize("https://medium.com/@author/my-first-post", sampleRules)
        assertEquals("articles", medium.category)

        // Uncategorized
        val github = Categorizer.categorize("https://github.com/android/architecture", sampleRules)
        assertEquals("uncategorized", github.category)
    }

    @Test
    fun testSocialHandleSourceExtraction() {
        // Extract handle for Instagram profile
        val handle1 = Categorizer.deriveSource("https://instagram.com/leomessi", "socials")
        assertEquals("@leomessi", handle1)

        // Skip explore, reels and get the handle
        val handle2 = Categorizer.deriveSource("https://instagram.com/reels/leomessi/123", "socials")
        assertEquals("@leomessi", handle2)

        // Non-social falls back to bare domain
        val sourceYoutube = Categorizer.deriveSource("https://www.youtube.com/watch?v=123", "videos")
        assertEquals("youtube.com", sourceYoutube)
    }

    @Test
    fun testNameDeSluggingAndAcronymCasing() {
        // Simple slug
        val name1 = Categorizer.deriveName("https://medium.com/offline-first-pwa-article")
        assertEquals("Offline First PWA Article", name1)

        // Acronyms in slugs: UI, UX, API, CSS
        val name2 = Categorizer.deriveName("https://dev.to/the-ultimate-guide-to-rest-api")
        assertEquals("The Ultimate Guide to REST API", name2)

        // Stopwords formatting (lowercase in middle, capitalized at edges)
        val name3 = Categorizer.deriveName("https://dev.to/by-the-flow-of-the-river")
        assertEquals("By the Flow of the River", name3)

        // Trailing Opaque ID removal
        val name4 = Categorizer.deriveName("https://medium.com/designing-ui-ux-interfaces-fdf928a3f89e")
        assertEquals("Designing UI UX Interfaces", name4)

        // Fallback to domain label
        val name5 = Categorizer.deriveName("https://google.com")
        assertEquals("Google", name5)
    }

    @Test
    fun testMalformedUrlRobustness() {
        // Completely blank
        val blank = Categorizer.categorize("", sampleRules)
        assertEquals("uncategorized", blank.category)
        assertEquals("unknown", blank.source)
        assertEquals("Link", blank.name)

        // Opaque string without protocol
        val opaque = Categorizer.categorize("somesite", sampleRules)
        assertEquals("uncategorized", opaque.category)
        assertEquals("somesite", opaque.source)
        assertEquals("Somesite", opaque.name)
    }
}
