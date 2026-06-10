package com.vagell.kv4pht.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica WCAG AA (ratio >= 4.5:1) entre los colores de texto primario y fondo.
 * Necesario para legibilidad con luz solar directa.
 */
class ColorContrastTest {

    private fun relativeLuminance(r: Int, g: Int, b: Int): Double {
        fun channel(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    private fun contrastRatio(l1: Double, l2: Double): Double {
        val lighter = maxOf(l1, l2)
        val darker  = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun greenTheme_textPrimary_vs_background_meetsWCAG_AA() {
        // GreenTextPrimary  = 0xFF8FE875  → RGB(143, 232, 117)
        // GreenBackground   = 0xFF050505  → RGB(5, 5, 5)
        val textLum = relativeLuminance(143, 232, 117)
        val bgLum   = relativeLuminance(5, 5, 5)
        val ratio   = contrastRatio(textLum, bgLum)
        assertTrue("Contraste verde textPrimary/background = $ratio (debe >= 4.5)", ratio >= 4.5)
    }

    @Test
    fun amberTheme_textPrimary_vs_background_meetsWCAG_AA() {
        // AmberTextPrimary  = 0xFFFAC775  → RGB(250, 199, 117)
        // AmberBackground   = 0xFF050505  → RGB(5, 5, 5)
        val textLum = relativeLuminance(250, 199, 117)
        val bgLum   = relativeLuminance(5, 5, 5)
        val ratio   = contrastRatio(textLum, bgLum)
        assertTrue("Contraste ámbar textPrimary/background = $ratio (debe >= 4.5)", ratio >= 4.5)
    }

    @Test
    fun emergencyText_vs_emergencyBackground_meetsWCAG_AA() {
        // EmergencyText       = 0xFFF09595  → RGB(240, 149, 149)
        // EmergencyBackground = 0xFF501313  → RGB(80, 19, 19)
        val textLum = relativeLuminance(240, 149, 149)
        val bgLum   = relativeLuminance(80, 19, 19)
        val ratio   = contrastRatio(textLum, bgLum)
        assertTrue("Contraste emergencia texto/fondo = $ratio (debe >= 4.5)", ratio >= 4.5)
    }
}
