package ar.motorfar.app.ui.compose.state

import ar.motorfar.app.ui.AlertHelper

data class ReceivedAlert(
    val type: AlertHelper.AlertType,
    val fromAlias: String,
    val receivedAtMs: Long
)
