package com.isycat.dotahalp.steamutils

import com.isycat.dotahalp.steamutils.SteamInstallLocator.forAppId
import com.isycat.dotahalp.steamutils.SteamLibraryLocator.forAppId

/**
 * A convenience wrapper around [SteamInstallLocator.libraries] that provides a simpler API.
 */
object SteamLibraryLocator {
    /**
     * @see SteamInstallLocator.findSteamLibraries
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate")
    val libraries: List<SteamLibrary> get() = SteamInstallLocator.libraries

    /**
     * @see forAppId
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun forAppId(appId: String) = SteamInstallLocator.libraries.forAppId(appId)
}
