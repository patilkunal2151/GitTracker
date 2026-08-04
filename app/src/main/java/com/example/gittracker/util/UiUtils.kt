package com.example.gittracker.util

import androidx.compose.ui.graphics.Color

object UiUtils {
    fun getLanguageColor(language: String?): Color {
        return when (language?.lowercase()) {
            "kotlin" -> Color(0xFFA97BFF)
            "java" -> Color(0xFFB07219)
            "javascript" -> Color(0xFFF1E05A)
            "typescript" -> Color(0xFF3178C6)
            "python" -> Color(0xFF3572A5)
            "go" -> Color(0xFF00ADD8)
            "rust" -> Color(0xFFDEA584)
            "swift" -> Color(0xFFF05138)
            "c++" -> Color(0xFFF34B7D)
            "c#" -> Color(0xFF178600)
            "html" -> Color(0xFFE34C26)
            "css" -> Color(0xFF563D7C)
            "ruby" -> Color(0xFF701516)
            "php" -> Color(0xFF4F5D95)
            "shell" -> Color(0xFF89E051)
            else -> Color(0xFF8B949E) // Gray
        }
    }

    fun formatCount(count: Int): String {
        return when {
            count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
            count >= 1000 -> String.format("%.1fk", count / 1000.0)
            else -> count.toString()
        }
    }
}
