package ar.motorfar.app.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.R
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
import ar.motorfar.app.ui.compose.theme.ShareTechMono

@Composable
fun MainScreen(
    state: MainUiState,
    onAction: (MainUiAction) -> Unit,
    onDismissAlert: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
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
        // Barra superior: status + toggle escucha + acceso a config
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppStatusBar(
                isTx     = state.isTxActive,
                isRx     = state.isRxActive,
                modifier = Modifier.weight(1f)
            )
            IconToggleButton(
                checked         = state.isListenOnly,
                onCheckedChange = { onAction(MainUiAction.ToggleListenOnly) }
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_headphones),
                    contentDescription = if (state.isListenOnly) "Solo escucha activo" else "Modo normal",
                    tint               = if (state.isListenOnly) colors.accent else colors.textSecondary
                )
            }
            IconButton(
                onClick  = onOpenSettings,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_settings),
                    contentDescription = "Configuración",
                    tint               = colors.textSecondary
                )
            }
        }

        FrequencyDisplayCard(
            frequency   = state.activeFrequency,
            channelName = state.activeChannelName,
            sMeterLevel = state.sMeterLevel
        )
        AlertBanner(
            alert     = state.activeAlert,
            onDismiss = onDismissAlert,
            modifier  = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Indicador "SOLO ESCUCHA" cuando está activo
        if (state.isListenOnly) {
            Text(
                text          = "[ SOLO ESCUCHA ]",
                color         = colors.accent.copy(alpha = 0.75f),
                fontFamily    = ShareTechMono,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier      = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
        }

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
            // PTT habilitado salvo en modo escucha. Sin radio conectada funciona
            // en modo simulación (sonido + animación, sin TX real).
            enabled        = !state.isListenOnly,
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
private fun MainScreenPreviewListenOnly() {
    MotoRFARTheme(AppTheme.AMBER) {
        MainScreen(state = MainUiState.preview().copy(isListenOnly = true), onAction = {})
    }
}
