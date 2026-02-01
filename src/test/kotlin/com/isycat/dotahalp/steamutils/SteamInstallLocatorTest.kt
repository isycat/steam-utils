package com.isycat.dotahalp.steamutils

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SteamInstallLocatorTest {

    @Test
    fun `parseSteamLibraryFoldersVdf extracts paths`() {
        val vdf = """
            "libraryfolders"
            {
                "1"
                {
                    "path"      "D:\\Games\\Steam"
                    "label"     ""
                    "contentid" "123"
                }
                "2"
                {
                    "path"      "E:\\SteamLibrary"
                }
            }
        """.trimIndent()

        val paths = SteamInstallLocator.parseSteamLibraryFoldersVdf(vdf)
        assertEquals(listOf(File("D:\\Games\\Steam"), File("E:\\SteamLibrary")), paths)
    }

    @Test
    fun `parseSteamLibraryFoldersVdfLibraries captures apps per library`() {
        val vdf = """
            "libraryfolders"
            {
                "0"
                {
                    "path"      "D:\\Games\\Steam"
                    "apps"
                    {
                        "570"     "76034599075"
                        "730"     "59520259634"
                    }
                }
                "1"
                {
                    "path"      "E:\\Games\\SteamLibrary"
                    "apps"
                    {
                        "220"     "5770568474"
                    }
                }
            }
        """.trimIndent()

        val libraries = SteamInstallLocator.parseSteamLibraryFoldersVdfLibraries(vdf)
        assertEquals(2, libraries.size)
        assertEquals(File("D:\\Games\\Steam"), libraries[0].path)
        assertTrue(libraries[0].appIds.contains("570"))
        assertTrue(libraries[0].appIds.contains("730"))
        assertEquals(File("E:\\Games\\SteamLibrary"), libraries[1].path)
        assertTrue(libraries[1].appIds.contains("220"))
        assertFalse(libraries[1].appIds.contains("570"))
    }
}
