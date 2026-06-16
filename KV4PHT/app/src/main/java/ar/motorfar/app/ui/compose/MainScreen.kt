package ar.motorfar.app.ui.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
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
import ar.motorfar.app.ui.compose.components.ModulationVisualizer
import ar.motorfar.app.ui.compose.components.PttButton
import ar.motorfar.app.ui.compose.state.MainUiAction
import ar.motorfar.app.ui.compose.state.MainUiState
import ar.motorfar.app.ui.compose.theme.AppTheme
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.MotoRFARTheme
import ar.motorfar.app.ui.compose.theme.ShareTechMono

/**
 * Pantalla principal (PTT). Se adapta a la orientación:
 *  - Vertical: stack clásico.
 *  - Horizontal (uso real en la moto): dos paneles — info a la izquierda,
 *    controles + PTT grande a la derecha — sin recortes ni superposición.
 */
@Composable
fun MainScreen(
    state: MainUiState,
    onAction: (MainUiAction) -> Unit,
    onDismissAlert: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        if (isLandscape) {
            LandscapeLayout(state, onAction, onDismissAlert, onOpenSettings)
        } else {
            PortraitLayout(state, onAction, onDismissAlert, onOpenSettings)
        }
    }
}

// ── Barra superior compartida ─────────────────────────────────────────
@Composable
private fun TopBar(
    state: MainUiState,
    onAction: (MainUiAction) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    var themeMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppStatusBar(
            isTx     = state.isTxActive,
            isRx     = state.isRxActive,
            modifier = Modifier.weight(1f)
        )

        // Selector de tema (Verde / Ámbar / Día)
        Box {
            IconButton(onClick = { themeMenuOpen = true }) {
                Icon(
                    painter            = painterResource(R.drawable.ic_contrast),
                    contentDescription = "Tema",
                    tint               = colors.textSecondary
                )
            }
            DropdownMenu(
                expanded         = themeMenuOpen,
                onDismissRequest = { themeMenuOpen = false }
            ) {
                ThemeOption("VERDE · CRT", AppTheme.GREEN, state.theme) {
                    onAction(MainUiAction.SetTheme(it)); themeMenuOpen = false
                }
                ThemeOption("AMBAR · CRT", AppTheme.AMBER, state.theme) {
                    onAction(MainUiAction.SetTheme(it)); themeMenuOpen = false
                }
                ThemeOption("DIA · SOL", AppTheme.DAY, state.theme) {
                    onAction(MainUiAction.SetTheme(it)); themeMenuOpen = false
                }
            }
        }

        IconToggleButton(
            checked         = state.isListenOnly,
            onCheckedChange = { onAction(MainUiAction.ToggleListenOnly) }
        ) {
            // Estado inequívoco: activo = círculo relleno de acento con ícono oscuro;
            // inactivo = contorno sutil. Antes solo cambiaba el tint y no se notaba.
            val active = state.isListenOnly
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .then(
                        if (active) Modifier.background(colors.accent)
                        else Modifier.border(1.dp, colors.borderSubtle, CircleShape)
                    )
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_headphones),
                    contentDescription = if (active) "Solo escucha ACTIVO" else "Modo normal (tocar para solo escucha)",
                    tint               = if (active) colors.background else colors.textSecondary,
                    modifier           = Modifier.size(18.dp)
                )
            }
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
}

@Composable
private fun ThemeOption(
    label: String,
    theme: AppTheme,
    current: AppTheme,
    onPick: (AppTheme) -> Unit
) {
    val colors = LocalMotoRFARColors.current
    val selected = theme == current
    DropdownMenuItem(
        text = {
            Text(
                text       = (if (selected) "› " else "   ") + label,
                color      = if (selected) colors.accent else colors.textPrimary,
                fontFamily = ShareTechMono,
                fontSize   = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        onClick = { onPick(theme) }
    )
}

@Composable
private fun ListenOnlyTag(modifier: Modifier = Modifier) {
    val colors = LocalMotoRFARColors.current
    Text(
        text          = "[ SOLO ESCUCHA ]",
        color         = colors.accent.copy(alpha = 0.75f),
        fontFamily    = ShareTechMono,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier      = modifier.padding(vertical = 2.dp)
    )
}

// ── Vertical ──────────────────────────────────────────────────────────
@Composable
private fun PortraitLayout(
    state: MainUiState,
    onAction: (MainUiAction) -> Unit,
    onDismissAlert: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(state, onAction, onOpenSettings)

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
        if (state.isListenOnly) ListenOnlyTag()

        Spacer(Modifier.height(8.dp))

        ChannelRow(
            channels       = state.channels,
            activeFreq     = state.activeFrequency,
            onChannelClick = { freq -> onAction(MainUiAction.ChannelSelected(freq)) },
            modifier       = Modifier.height(56.dp)
        )

        Spacer(Modifier.height(10.dp))

        ModulationVisualizer(
            isActive = state.isTxActive || state.isRxActive,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        )

        Spacer(Modifier.height(10.dp))

        AlertButtonsPanel(
            onEmergency = { onAction(MainUiAction.EmergencyAlert) },
            onStop      = { onAction(MainUiAction.StopAlert) },
            onRegroup   = { onAction(MainUiAction.RegroupAlert) }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center
        ) {
            PttButton(
                isTransmitting = state.isTxActive,
                enabled        = !state.isListenOnly,
                onPttDown      = { onAction(MainUiAction.PttPressed) },
                onPttUp        = { onAction(MainUiAction.PttReleased) }
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ── Horizontal (moto) ─────────────────────────────────────────────────
@Composable
private fun LandscapeLayout(
    state: MainUiState,
    onAction: (MainUiAction) -> Unit,
    onDismissAlert: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(state, onAction, onOpenSettings)
        AlertBanner(
            alert     = state.activeAlert,
            onDismiss = onDismissAlert,
            modifier  = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Columna 1: info — frecuencia + canales + visualizador
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FrequencyDisplayCard(
                    frequency   = state.activeFrequency,
                    channelName = state.activeChannelName,
                    sMeterLevel = state.sMeterLevel
                )
                Spacer(Modifier.height(6.dp))
                ChannelRow(
                    channels       = state.channels,
                    activeFreq     = state.activeFrequency,
                    onChannelClick = { freq -> onAction(MainUiAction.ChannelSelected(freq)) },
                    modifier       = Modifier.height(46.dp)
                )
                Spacer(Modifier.height(6.dp))
                ModulationVisualizer(
                    isActive = state.isTxActive || state.isRxActive,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
            // Columna 2: PTT — control principal, ocupa toda la altura disponible
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isListenOnly) ListenOnlyTag()
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val ptt = (minOf(maxWidth, maxHeight) - 8.dp).coerceIn(110.dp, 230.dp)
                    PttButton(
                        isTransmitting = state.isTxActive,
                        enabled        = !state.isListenOnly,
                        onPttDown      = { onAction(MainUiAction.PttPressed) },
                        onPttUp        = { onAction(MainUiAction.PttReleased) },
                        diameter       = ptt
                    )
                }
            }
            // Columna 3: alertas (centradas verticalmente)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AlertButtonsPanel(
                    onEmergency = { onAction(MainUiAction.EmergencyAlert) },
                    onStop      = { onAction(MainUiAction.StopAlert) },
                    onRegroup   = { onAction(MainUiAction.RegroupAlert) }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun MainScreenPreviewGreen() {
    MotoRFARTheme(AppTheme.GREEN) {
        MainScreen(state = MainUiState.preview(), onAction = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE7E2D6)
@Composable
private fun MainScreenPreviewDay() {
    MotoRFARTheme(AppTheme.DAY) {
        MainScreen(state = MainUiState.preview(), onAction = {})
    }
}
