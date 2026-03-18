package com.isycat.dotahalp.steamutils

import java.io.File
import java.nio.charset.StandardCharsets

fun findSteamInstallDirWindows(): File? {
    val candidates =
        listOfNotNull(
            readWindowsRegistryString(
                key = "HKCU\\Software\\Valve\\Steam",
                valueName = "SteamPath",
            ),
            readWindowsRegistryString(
                key = "HKLM\\Software\\WOW6432Node\\Valve\\Steam",
                valueName = "InstallPath",
            ),
            readWindowsRegistryString(
                key = "HKLM\\Software\\Valve\\Steam",
                valueName = "InstallPath",
            ),
        )

    return candidates
        .asSequence()
        .map { it.trim().trimEnd('\\') }
        .filter { it.isNotBlank() }
        .map { File(it) }
        .firstOrNull { it.isDirectory && File(it, "steam.exe").isFile }
}

private fun readWindowsRegistryString(
    key: String,
    valueName: String,
): String? {
    if (!isWindows()) {
        return null
    }

    return try {
        val process =
            ProcessBuilder("reg", "query", key, "/v", valueName)
                .redirectErrorStream(true)
                .start()

        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        process.waitFor()

        // Example line:
        //    SteamPath    REG_SZ    C:\\Program Files (x86)\\Steam
        val line = output.lineSequence().firstOrNull { it.contains(valueName) } ?: return null
        val parts = line.trim().split(Regex("\\s{2,}"))
        parts.lastOrNull()
    } catch (_: Exception) {
        null
    }
}

private fun isWindows(): Boolean = OsMatcher.Windows matches osName
