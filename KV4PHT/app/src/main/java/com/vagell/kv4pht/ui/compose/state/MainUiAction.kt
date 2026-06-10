package com.vagell.kv4pht.ui.compose.state

sealed class MainUiAction {
    object PttPressed                             : MainUiAction()
    object PttReleased                            : MainUiAction()
    data class ChannelSelected(val freq: String)  : MainUiAction()
    object EmergencyAlert                         : MainUiAction()
    object StopAlert                              : MainUiAction()
    object RegroupAlert                           : MainUiAction()
}
