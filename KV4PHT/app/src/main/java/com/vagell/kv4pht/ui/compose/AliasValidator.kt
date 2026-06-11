package com.vagell.kv4pht.ui.compose

object AliasValidator {
    private val REGEX = Regex("^[A-Z0-9]{1,6}$")

    fun isValid(alias: String): Boolean = REGEX.matches(alias.uppercase())

    fun sanitize(raw: String): String =
        raw.uppercase().filter { it.isLetterOrDigit() }.take(6)
}
