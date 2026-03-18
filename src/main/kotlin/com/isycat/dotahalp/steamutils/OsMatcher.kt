package com.isycat.dotahalp.steamutils

object OsMatcher {
    class StringContainsMatcher(
        private vararg val needles: String,
    ) {
        operator fun contains(x: String?): Boolean = x?.let { s -> needles.any { s.contains(it, ignoreCase = true) } } == true

        infix fun matches(x: String?): Boolean = contains(x)
    }

    val Windows = StringContainsMatcher("win")
    val Linux = StringContainsMatcher("nux", "nix")
    val Mac = StringContainsMatcher("mac", "darwin")
}
