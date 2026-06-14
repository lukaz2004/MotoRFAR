package ar.motorfar.app.ui.compose.state

import ar.motorfar.app.ui.compose.theme.AppTheme

sealed class MainUiAction {
    object PttPressed                             : MainUiAction()
    object PttReleased                            : MainUiAction()
    data class ChannelSelected(val freq: String)  : MainUiAction()
    object EmergencyAlert                         : MainUiAction()
    object StopAlert                              : MainUiAction()
    object RegroupAlert                           : MainUiAction()
    object ToggleListenOnly                       : MainUiAction()
    data class SetTheme(val theme: AppTheme)      : MainUiAction()
}
