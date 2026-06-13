package ar.motorfar.app.ui

import ar.motorfar.app.ui.compose.AliasValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAliasTest {

    @Test
    fun alias_valid_uppercase() {
        assertTrue(AliasValidator.isValid("LUKAZ"))
    }

    @Test
    fun alias_invalid_has_space() {
        assertFalse(AliasValidator.isValid("lu kaz"))
    }

    @Test
    fun alias_invalid_too_long() {
        assertFalse(AliasValidator.isValid("TOOLONG"))
    }

    @Test
    fun alias_invalid_empty() {
        assertFalse(AliasValidator.isValid(""))
    }

    @Test
    fun alias_valid_alphanumeric() {
        assertTrue(AliasValidator.isValid("MO77"))
    }

    @Test
    fun sanitize_lowercases_to_upper_and_strips_spaces() {
        assertEquals("LUKAZ", AliasValidator.sanitize("lu kaz"))
    }

    @Test
    fun sanitize_truncates_to_6_chars() {
        assertEquals("TOOLON", AliasValidator.sanitize("TOOLONG"))
    }
}
