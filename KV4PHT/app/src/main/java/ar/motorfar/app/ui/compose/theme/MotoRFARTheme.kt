package ar.motorfar.app.ui.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    // El tema DÍA es claro; los CRT (verde/ámbar) son oscuros. Esto hace que
    // los componentes Material (NavigationBar, ripples, etc.) elijan defaults
    // legibles en cada caso.
    val colorScheme = if (theme == AppTheme.DAY) {
        lightColorScheme(
            background   = colors.background,
            surface      = colors.surface,
            primary      = colors.accent,
            onBackground = colors.textPrimary,
            onSurface    = colors.textPrimary,
            onPrimary    = colors.background
        )
    } else {
        darkColorScheme(
            background   = colors.background,
            surface      = colors.surface,
            primary      = colors.accent,
            onBackground = colors.textPrimary,
            onSurface    = colors.textPrimary,
            onPrimary    = colors.background
        )
    }
    CompositionLocalProvider(LocalMotoRFARColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = MotoRFARTypography,
            content     = content
        )
    }
}
