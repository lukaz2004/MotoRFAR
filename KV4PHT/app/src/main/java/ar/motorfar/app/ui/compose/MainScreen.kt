package ar.motorfar.app.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.motorfar.app.ui.compose.components.AlertBanner
import ar.motorfar.app.ui.compose.components.AlertButtonsPanel
import ar.motorfar.app.ui.compose.components.AppStatusBar
import ar.motorfar.app.ui.compose.components.ChannelRow
import ar.motorfar.app.ui.compose.components.FrequencyDisplayCard
import ar.motorfar.app.ui.compose.components.PttButton
import ar.motorfar.app.ui.compose.state.MainUiAction
import ar.motorfar.app.ui.compose.state.MainUiState
import ar.motorfar.app.ui.compose.theme.AppTheme
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.MotoRFARTheme

@Composable
fun MainScreen(
    state: MainUiState,
    onAction: (MainUiAction) -> Unit,
    onDismissAlert: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppStatusBar(isTx = state.isTxActive, isRx = state.isRxActive)
        FrequencyDisplayCard(
            frequency   = state.activeFrequency,
            channelName = state.activeChannelName,
            sMeterLevel = state.sMeterLevel
        )
        AlertBanner(
            alert = state.activeAlert,
            onDismiss = onDismissAlert,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(4.dp))
        ChannelRow(
            channels       = state.channels,
            activeFreq     = state.activeFrequency,
            onChannelClick = { freq -> onAction(MainUiAction.ChannelSelected(freq)) }
        )
        Spacer(Modifier.weight(1f))
        AlertButtonsPanel(
            onEmergency = { onAction(MainUiAction.EmergencyAlert) },
            onStop      = { onAction(MainUiAction.StopAlert) },
            onRegroup   = { onAction(MainUiAction.RegroupAlert) }
        )
        Spacer(Modifier.height(12.dp))
        PttButton(
            isTransmitting = state.isTxActive,
            enabled        = state.isConnected,
            onPttDown      = { onAction(MainUiAction.PttPressed) },
            onPttUp        = { onAction(MainUiAction.PttReleased) }
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun MainScreenPreviewGreen() {
    MotoRFARTheme(AppTheme.GREEN) {
        MainScreen(state = MainUiState.preview(), onAction = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun MainScreenPreviewAmber() {
    MotoRFARTheme(AppTheme.AMBER) {
        MainScreen(state = MainUiState.preview(), onAction = {})
    }
}
