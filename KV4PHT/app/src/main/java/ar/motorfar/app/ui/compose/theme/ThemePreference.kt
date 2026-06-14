package ar.motorfar.app.ui.compose.theme

import android.content.Context

/**
 * Persistencia del tema de UI en SharedPreferences (separado de la DB de radio).
 *
 * Se lee de forma síncrona en MainActivity.onCreate ANTES de componer, así el
 * tema elegido se aplica al instante en el arranque, sin "flash" del default.
 */
object ThemePreference {
    private const val PREFS     = "motorfar_ui_prefs"
    private const val KEY_THEME = "ui_theme"

    fun get(context: Context): AppTheme {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, AppTheme.GREEN.name) ?: AppTheme.GREEN.name
        return runCatching { AppTheme.valueOf(name) }.getOrDefault(AppTheme.GREEN)
    }

    fun set(context: Context, theme: AppTheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme.name)
            .apply()
    }
}
