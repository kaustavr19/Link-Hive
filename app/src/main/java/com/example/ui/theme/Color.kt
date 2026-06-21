package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Colors
val LightCanvas = Color(0xFFF9F9F9)
val LightSurface = Color(0xFFFFFFFF)
val LightSidebar = Color(0xFFF0F0F0)
val LightInk = Color(0xFF1A1C1C)
val LightMuted = Color(0xFF444748)
val LightHairline = Color(0xFFE2E2E2)
val LightPrimary = Color(0xFF0D0D0D)
val LightOnPrimary = Color(0xFFFFFFFF)

// Dark Colors
val DarkCanvas = Color(0xFF0D0D0D)
val DarkSurface = Color(0xFF1A1A1A)
val DarkSidebar = Color(0xFF141414)
val DarkInk = Color(0xFFF1F1F1)
val DarkMuted = Color(0xFFA1A1A1)
val DarkHairline = Color(0xFF2A2A2A)
val DarkPrimary = Color(0xFFF5F5F5)
val DarkOnPrimary = Color(0xFF0D0D0D)

// Category Accents (Heuristic light / dark)
object CategoryColors {
    private val customColors = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val customEmojis = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun registerCustomCategory(category: String, hexColor: String?, emoji: String?) {
        val key = category.lowercase().trim()
        if (hexColor != null) {
            customColors[key] = hexColor
        }
        if (emoji != null) {
            customEmojis[key] = emoji
        }
    }

    fun getAccent(category: String, isDark: Boolean): Color {
        val key = category.lowercase().trim()
        val customHex = customColors[key]
        if (customHex != null) {
            return try {
                Color(android.graphics.Color.parseColor(customHex))
            } catch (e: Exception) {
                if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
            }
        }
        return when (key) {
            "jobs" -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
            "socials" -> if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)
            "videos" -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
            "articles" -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
            else -> if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
        }
    }

    fun getEmoji(category: String): String {
        val key = category.lowercase().trim()
        val customEmoji = customEmojis[key]
        if (customEmoji != null) return customEmoji
        return when (key) {
            "jobs" -> "💼"
            "socials" -> "💬"
            "videos" -> "🎥"
            "articles" -> "📝"
            "uncategorized" -> "📂"
            else -> "📂"
        }
    }

    private fun blendWithBackground(color: Color, background: Color, ratio: Float): Color {
        return Color(
            red = color.red * ratio + background.red * (1 - ratio),
            green = color.green * ratio + background.green * (1 - ratio),
            blue = color.blue * ratio + background.blue * (1 - ratio),
            alpha = 1.0f
        )
    }

    fun getCardBackground(category: String, isDark: Boolean): Color {
        val key = category.lowercase().trim()
        val customHex = customColors[key]
        if (customHex != null) {
            val baseColor = try {
                Color(android.graphics.Color.parseColor(customHex))
            } catch (e: Exception) {
                if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
            }
            return if (isDark) {
                blendWithBackground(baseColor, Color(0xFF1E1E20), 0.12f)
            } else {
                blendWithBackground(baseColor, Color(0xFFFFFFFF), 0.08f)
            }
        }
        return if (isDark) {
            when (key) {
                "jobs" -> Color(0xFF0F172A)     // Deep slate blue
                "socials" -> Color(0xFF330F20)  // Deep rich rose
                "videos" -> Color(0xFF3B0F0F)   // Deep solid dark red
                "articles" -> Color(0xFF3B250F) // Deep solid amber
                else -> Color(0xFF1E1E20)       // Dark card background
            }
        } else {
            when (key) {
                "jobs" -> Color(0xFFEFF6FF)     // Soft light pastel blue
                "socials" -> Color(0xFFFDF2F8)  // Soft light pastel pink
                "videos" -> Color(0xFFFEF2F2)   // Soft light pastel red
                "articles" -> Color(0xFFFFFBEB) // Soft light pastel yellow
                else -> Color(0xFFFFFFFF)       // Clean white card background
            }
        }
    }

    fun getBackgroundTint(category: String, isDark: Boolean): Color {
        return getCardBackground(category, isDark)
    }
}

// Job Status Accents
object JobStatusColors {
    val Applied = Color(0xFF16A34A)
    val NotApplied = Color(0xFFD97706)
    val Rejected = Color(0xFFDC2626)
}
