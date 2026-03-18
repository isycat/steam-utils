package com.isycat.dotahalp.steamutils

import com.isycat.dotahalp.steamutils.SteamInstallLocator.findSteamLibraries
import com.isycat.dotahalp.steamutils.SteamInstallLocator.forAppId
import com.isycat.dotahalp.steamutils.SteamLibraryLocator.forAppId
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * A convenience wrapper around [SteamInstallLocator.libraries] that provides a simpler API.
 */
object SteamLibraryLocator {
    /**
     * @see findSteamLibraries
     */
    @Suppress()
    val paths: List<File> get() = SteamInstallLocator.libraries.map { it.path }
    val libraries: List<SteamLibrary> get() = SteamInstallLocator.libraries

    /**
     * @see forAppId
     */
    @Suppress()
    fun forAppId(appId: String) = SteamInstallLocator.libraries.forAppId(appId)
}

/**
 * Steam-specific install and library discovery utilities.
 *
 * This currently only works on Windows. We should fix this.
 */
object SteamInstallLocator {
    /**
     * Locates the Steam installation directory
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun findSteamInstallDir(): File? =
        when (val os = osName) {
            in OsMatcher.Windows -> findSteamInstallDirWindows()
            in OsMatcher.Linux -> null
            in OsMatcher.Mac -> null
            else -> null
        }

    /**
     * Locates the Steam libraries on the system.
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun findSteamLibraries(): List<SteamLibrary> = findSteamInstallDir()?.let { findSteamLibrariesFromSteamRoot(it) } ?: emptyList()

    /**
     * @see findSteamLibraries
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate")
    val libraries: List<SteamLibrary> get() = findSteamLibraries()

    /**
     * Searches for the installation directory of a specific application by app ID and subdirectory
     * within a given file path.
     *
     * The method first looks for the application's installation path based on Steam library data
     * from the provided directory. If no specific Steam library match is found, the search falls back
     * to considering the provided directory itself as the potential root for the application directory.
     *
     * @param appId The unique application ID to locate within the Steam libraries. e.g. 570
     * @param appDir The subdirectory name expected within the application installation directory.
     *      e.g. "steamapps/common/dota 2 beta"
     * @param filter A predicate to filter the application installation directory based on additional criteria
     * @return The resolved application installation directory as a File if found, or null if no
     * valid directory exists that matches the criteria.
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun File.findAppInstall(
        appId: String,
        appDir: String,
        filter: (File) -> Boolean = { true },
    ): File? =
        sequenceOf(findSteamLibrariesFromSteamRoot(this).forAppId(appId)?.path, this)
            .filterNotNull()
            .map { it.resolve(appDir) }
            .firstOrNull(filter)

    /**
     * Finds the Steam library for a given app ID.
     * @param appId The Steam app ID to search for
     * @return The Steam library directory as a SteamLibrary object if found, or null if not found
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun List<SteamLibrary>.forAppId(appId: String) = this.firstOrNull { appId in it.appIds }

    private fun findSteamLibrariesFromSteamRoot(steamRoot: File): List<SteamLibrary> {
        val steamApps = File(steamRoot, "steamapps")
        val libraryFolders = File(steamApps, "libraryfolders.vdf")

        if (!libraryFolders.isFile) {
            return emptyList()
        }

        val text = libraryFolders.readText(StandardCharsets.UTF_8)
        return parseSteamLibraryFoldersVdfLibraries(text)
    }

    internal fun parseSteamLibraryFoldersVdf(vdfText: String): List<File> = parseSteamLibraryFoldersVdfLibraries(vdfText).map { it.path }

    internal fun parseSteamLibraryFoldersVdfLibraries(vdfText: String): List<SteamLibrary> {
        // Steam's VDF / KeyValues is not JSON; it is brace-based with quoted tokens. (Valve KV?)
        // We implement a small, tolerant line-based parser that captures:
        // - each library block's `path`
        // - app ids listed under its `apps` subsection
        data class LibraryBuilder(
            var path: File? = null,
            val appIds: MutableSet<String> = linkedSetOf(),
            val startDepth: Int,
        )

        val libraries = mutableListOf<SteamLibrary>()

        var depth = 0
        var pendingKey: String? = null

        var currentLibrary: LibraryBuilder? = null
        var inApps = false
        var appsStartDepth = -1

        fun normalizePath(raw: String): File? {
            val unescaped =
                raw
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

        val keyOnlyRegex = Regex("^\"([^\"]+)\"$")
        val kvRegex = Regex("^\"([^\"]+)\"\\s+\"([^\"]*)\"$")

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

    private val osName: String? by lazy { System.getProperty("os.name")?.lowercase() }
}

data class SteamLibrary(
    val path: File,
    val appIds: Set<String>,
)

@Suppress("unused", "MemberVisibilityCanBePrivate")
fun SteamLibrary.dir(appDir: String) = path.resolve(appDir)
