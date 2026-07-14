package com.example.gittracker.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val githubFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parseGithubDate(dateString: String): Long {
        return try {
            githubFormat.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        return outputFormat.format(date)
    }
}
