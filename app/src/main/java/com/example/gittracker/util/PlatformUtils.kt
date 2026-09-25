package com.example.gittracker.util

import com.example.gittracker.data.model.ReleaseAsset

enum class Platform(val label: String, val symbol: String) {
    ANDROID("Android", "🤖"),
    WINDOWS("Windows", "🪟"),
    MACOS("macOS", "🍎"),
    LINUX("Linux", "🐧"),
    IOS("iOS", "📱")
}

object PlatformUtils {

    private val androidRegex = Regex("\\b(android|apk|aab|google play|play store)\\b", RegexOption.IGNORE_CASE)
    private val windowsRegex = Regex("\\b(windows|win10|win11|win32|win64|win-x64|win-x86|msi|exe)\\b", RegexOption.IGNORE_CASE)
    private val macosRegex = Regex("\\b(macos|mac os|osx|darwin|apple silicon|dmg|pkg)\\b", RegexOption.IGNORE_CASE)
    private val linuxRegex = Regex("\\b(linux|ubuntu|debian|fedora|arch|appimage|flatpak|snapcraft|deb|rpm)\\b", RegexOption.IGNORE_CASE)
    private val iosRegex = Regex("\\b(ios|iphone|ipad|testflight|app store|ipa)\\b", RegexOption.IGNORE_CASE)

    fun detectPlatforms(
        assets: List<ReleaseAsset> = emptyList(),
        topics: List<String> = emptyList(),
        readme: String? = null,
        description: String? = null
    ): List<Platform> {
        val detected = mutableSetOf<Platform>()

        // 1. Analyze Release Assets (Highest Confidence - Concrete Binaries)
        for (asset in assets) {
            val name = asset.name.lowercase()
            when {
                name.endsWith(".apk") || name.endsWith(".aab") -> detected.add(Platform.ANDROID)
                name.endsWith(".exe") || name.endsWith(".msi") || name.contains("-win") || name.contains("-windows") || name.contains(".win.") -> detected.add(Platform.WINDOWS)
                name.endsWith(".dmg") || name.endsWith(".pkg") || name.contains("-mac") || name.contains("-darwin") || name.contains("-macos") || name.contains(".app.zip") -> detected.add(Platform.MACOS)
                name.endsWith(".appimage") || name.endsWith(".deb") || name.endsWith(".rpm") || name.endsWith(".flatpak") || name.endsWith(".snap") || name.endsWith(".tar.xz") || name.contains("-linux") -> detected.add(Platform.LINUX)
                name.endsWith(".ipa") -> detected.add(Platform.IOS)
            }
        }

        // If official binary assets were detected, trust them exclusively
        if (detected.isNotEmpty()) {
            return Platform.entries.filter { it in detected }
        }

        // 2. Check Repository Topics / Tags if no binary assets were found
        for (topic in topics) {
            val t = topic.lowercase().trim()
            when {
                t in listOf("android", "apk", "android-app") -> detected.add(Platform.ANDROID)
                t in listOf("windows", "win32", "win64", "windows-app") -> detected.add(Platform.WINDOWS)
                t in listOf("macos", "mac", "osx", "darwin", "macos-app") -> detected.add(Platform.MACOS)
                t in listOf("linux", "ubuntu", "debian", "flatpak", "appimage", "snap", "linux-app") -> detected.add(Platform.LINUX)
                t in listOf("ios", "iphone", "ipad", "swiftui-ios") -> detected.add(Platform.IOS)
            }
        }

        if (detected.isNotEmpty()) {
            return Platform.entries.filter { it in detected }
        }

        // 3. Fallback: Scan README & Description with Word-Boundary Regex
        val textToScan = buildString {
            if (!description.isNullOrBlank()) append(" ").append(description)
            if (!readme.isNullOrBlank()) append(" ").append(readme)
        }

        if (textToScan.isNotBlank()) {
            if (Platform.ANDROID !in detected && androidRegex.containsMatchIn(textToScan)) {
                detected.add(Platform.ANDROID)
            }
            if (Platform.WINDOWS !in detected && windowsRegex.containsMatchIn(textToScan)) {
                detected.add(Platform.WINDOWS)
            }
            if (Platform.MACOS !in detected && macosRegex.containsMatchIn(textToScan)) {
                detected.add(Platform.MACOS)
            }
            if (Platform.LINUX !in detected && linuxRegex.containsMatchIn(textToScan)) {
                detected.add(Platform.LINUX)
            }
            if (Platform.IOS !in detected && iosRegex.containsMatchIn(textToScan)) {
                detected.add(Platform.IOS)
            }
        }

        return Platform.entries.filter { it in detected }
    }
}
