package com.isycat.dotahalp.steamutils

import java.io.File

data class SteamLibrary(
    val path: File,
    val appIds: Set<String>,
)

@Suppress("unused", "MemberVisibilityCanBePrivate")
fun SteamLibrary.dir(appDir: String) = path.resolve(appDir)
