package com.isycat.dotahalp.steamutils

import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Steam-specific install and library discovery utilities.
 *
 * This currently only works on Windows. We should fix this.
 */
object SteamInstallLocator {

    data class SteamLibrary(
        val path: File,
        val appIds: Set<String>
    )

    fun findSteamInstallDir(): File? {
        if (!isWindows()) {
            return null
        }

        return findSteamInstallDirWindows()
    }

    fun findSteamLibrariesFromSteamRoot(steamRoot: File): List<SteamLibrary> {
        val steamApps = File(steamRoot, "steamapps")
        val libraryFolders = File(steamApps, "libraryfolders.vdf")

        if (!libraryFolders.isFile) {
            return emptyList()
        }

        val text = libraryFolders.readText(StandardCharsets.UTF_8)
        return parseSteamLibraryFoldersVdfLibraries(text)
    }

    internal fun parseSteamLibraryFoldersVdf(vdfText: String): List<File> {
        return parseSteamLibraryFoldersVdfLibraries(vdfText).map { it.path }
    }

    internal fun parseSteamLibraryFoldersVdfLibraries(vdfText: String): List<SteamLibrary> {
        // Steam's VDF / KeyValues is not strict JSON; it is brace-based with quoted tokens.
        // We implement a small, tolerant line-based parser that captures:
        // - each library block's `path`
        // - app ids listed under its `apps` subsection
        data class LibraryBuilder(
            var path: File? = null,
            val appIds: MutableSet<String> = linkedSetOf(),
            val startDepth: Int
        )

        val libraries = mutableListOf<SteamLibrary>()

        var depth = 0
        var pendingKey: String? = null

        var currentLibrary: LibraryBuilder? = null
        var inApps = false
        var appsStartDepth = -1

        fun normalizePath(raw: String): File? {
            val unescaped = raw
                .replace("\\\\", "\\")
                .replace("/", "\\")
                .trim()
            return if (unescaped.isBlank()) null else File(unescaped)
        }

        fun flushLibraryIfComplete() {
            val lib = currentLibrary ?: return
            val path = lib.path
            if (path != null) {
                libraries.add(SteamLibrary(path = path, appIds = lib.appIds.toSet()))
            }
        }

        val keyOnlyRegex = Regex("^\\\"([^\\\"]+)\\\"$")
        val kvRegex = Regex("^\\\"([^\\\"]+)\\\"\\s+\\\"([^\\\"]*)\\\"$")

        for (rawLine in vdfText.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                continue
            }

            if (line == "{") {
                val key = pendingKey
                pendingKey = null

                // `libraryfolders` root is typically depth 0 -> 1
                // Library blocks are typically numeric keys at depth 1.
                if (key != null && key.all { it.isDigit() } && depth == 1) {
                    currentLibrary?.let {
                        flushLibraryIfComplete()
                    }
                    currentLibrary = LibraryBuilder(startDepth = depth + 1)
                    inApps = false
                    appsStartDepth = -1
                } else if (key == "apps" && currentLibrary != null) {
                    inApps = true
                    appsStartDepth = depth + 1
                }

                depth += 1
                continue
            }

            if (line == "}") {
                depth -= 1
                if (inApps && depth < appsStartDepth) {
                    inApps = false
                    appsStartDepth = -1
                }
                val lib = currentLibrary
                if (lib != null && depth < lib.startDepth) {
                    flushLibraryIfComplete()
                    currentLibrary = null
                }
                continue
            }

            val kvMatch = kvRegex.matchEntire(line)
            if (kvMatch != null) {
                val key = kvMatch.groupValues[1]
                val value = kvMatch.groupValues[2]

                val lib = currentLibrary
                if (lib != null) {
                    if (inApps) {
                        // Under `apps` subsection the *keys* are the app IDs.
                        lib.appIds.add(key)
                    } else if (key == "path") {
                        lib.path = normalizePath(value)
                    }
                }
                continue
            }

            val keyOnlyMatch = keyOnlyRegex.matchEntire(line)
            if (keyOnlyMatch != null) {
                pendingKey = keyOnlyMatch.groupValues[1]
                continue
            }
        }

        // In case the file ended without the final closing braces.
        currentLibrary?.let {
            flushLibraryIfComplete()
        }

        return libraries
    }

    private fun findSteamInstallDirWindows(): File? {
        val candidates = listOfNotNull(
            readWindowsRegistryString(
                key = "HKCU\\Software\\Valve\\Steam",
                valueName = "SteamPath"
            ),
            readWindowsRegistryString(
                key = "HKLM\\Software\\WOW6432Node\\Valve\\Steam",
                valueName = "InstallPath"
            ),
            readWindowsRegistryString(
                key = "HKLM\\Software\\Valve\\Steam",
                valueName = "InstallPath"
            )
        )

        return candidates
            .asSequence()
            .map { it.trim().trimEnd('\\') }
            .filter { it.isNotBlank() }
            .map { File(it) }
            .firstOrNull { it.isDirectory && File(it, "steam.exe").isFile }
    }

    private fun readWindowsRegistryString(key: String, valueName: String): String? {
        if (!isWindows()) {
            return null
        }

        return try {
            val process = ProcessBuilder("reg", "query", key, "/v", valueName)
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

    private fun isWindows(): Boolean {
        val os = System.getProperty("os.name")?.lowercase() ?: return false
        return os.contains("win")
    }
}
