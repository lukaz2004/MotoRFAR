package com.vagell.kv4pht.ui.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalMotoRFARColors = staticCompositionLocalOf { appColors(AppTheme.GREEN) }

@Composable
fun MotoRFARTheme(
    theme: AppTheme = AppTheme.GREEN,
    content: @Composable () -> Unit
) {
    val colors = appColors(theme)
    CompositionLocalProvider(LocalMotoRFARColors provides colors) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = colors.background,
                surface    = colors.surface,
                primary    = colors.accent
            ),
            typography = MotoRFARTypography,
            content    = content
        )
    }
}
