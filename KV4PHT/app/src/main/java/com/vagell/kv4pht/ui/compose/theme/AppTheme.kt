package com.vagell.kv4pht.ui.compose.theme

import androidx.compose.ui.graphics.Color

enum class AppTheme { AMBER, GREEN }

data class MotoRFARColors(
    val background: Color,
    val display: Color,
    val surface: Color,
    val borderSubtle: Color,
    val borderActive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val textGhost: Color,
    val accent: Color
)

fun appColors(theme: AppTheme): MotoRFARColors = when (theme) {
    AppTheme.AMBER -> MotoRFARColors(
        background    = AmberBackground,
        display       = AmberDisplay,
        surface       = AmberSurface,
        borderSubtle  = AmberBorderSubtle,
        borderActive  = AmberBorderActive,
        textPrimary   = AmberTextPrimary,
        textSecondary = AmberTextSecondary,
        textDisabled  = AmberTextDisabled,
        textGhost     = AmberTextGhost,
        accent        = AmberAccent
    )
    AppTheme.GREEN -> MotoRFARColors(
        background    = GreenBackground,
        display       = GreenDisplay,
        surface       = GreenSurface,
        borderSubtle  = GreenBorderSubtle,
        borderActive  = GreenBorderActive,
        textPrimary   = GreenTextPrimary,
        textSecondary = GreenTextSecondary,
        textDisabled  = GreenTextDisabled,
        textGhost     = GreenTextDisabled,
        accent        = GreenAccent
    )
}
