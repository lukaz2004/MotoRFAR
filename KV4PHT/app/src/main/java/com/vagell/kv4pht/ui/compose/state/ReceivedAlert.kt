package com.vagell.kv4pht.ui.compose.state

import com.vagell.kv4pht.ui.AlertHelper

data class ReceivedAlert(
    val type: AlertHelper.AlertType,
    val fromAlias: String,
    val receivedAtMs: Long
)
