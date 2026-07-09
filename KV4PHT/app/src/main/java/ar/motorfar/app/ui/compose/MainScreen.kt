package ar.motorfar.app.ui.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import ar.motorfar.app.ui.compose.components.EmergencyConfirmButton
import ar.motorfar.app.ui.compose.components.FrequencyDisplayCard
import ar.motorfar.app.ui.compose.components.ModulationVisualizer
import ar.motorfar.app.ui.compose.components.PttButton
import ar.motorfar.app.ui.compose.state.MainUiAction
import ar.motorfar.app.ui.compose.state.MainUiState
import ar.motorfar.app.ui.compose.state.activeToneLabel
import ar.motorfar.app.ui.compose.state.isEmergencyActive
import ar.motorfar.app.ui.compose.theme.AppTheme
import ar.motorfar.app.ui.compose.theme.BorderHairline
import ar.motorfar.app.ui.compose.theme.ControlShape
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
    onOpenWifiSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            // ponytail: sin systemBarsPadding acá -- el Scaffold en MainActivity
            // ya aplica el inset de status bar via innerPadding; duplicarlo
            // dejaba una franja vacía extra arriba de toda la pantalla.
    ) {
        if (state.isRouteActive) {
            RouteActiveLayout(state, onAction)
        } else if (isLandscape) {
            LandscapeLayout(state, onAction, onDismissAlert, onOpenSettings, onOpenWifiSettings)
        } else {
            PortraitLayout(state, onAction, onDismissAlert, onOpenSettings, onOpenWifiSettings)
        }
        // 2026-07-07: el overlay de Man-Down se movió a MainActivity (afuera del
        // Scaffold) para que se vea en Mapa/Chat/Ajustes también, no solo acá.
    }
}

// ── Barra superior compartida ─────────────────────────────────────────
@Composable
private fun TopBar(
    state: MainUiState,
    onAction: (MainUiAction) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    var themeMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(
            painter            = androidx.compose.ui.res.painterResource(R.mipmap.ic_launcher_moto),
            contentDescription = "Baqueano",
            // 2026-07-07: seguía viéndose diminuto -- el PNG es la capa
            // "foreground" de un ícono adaptativo, que por convención de
            // Android trae ~33% de margen transparente en cada lado (zona
            // segura del recorte de máscara). Con tamaño fijo se veía la
            // insignia real mucho más chica que la caja. ContentScale.Crop
            // hace zoom hasta recortar ese margen vacío y llenar la caja.
            contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
            modifier           = Modifier
                .padding(end = 8.dp)
                .size(48.dp)
        )

        AppStatusBar(
            isTx               = state.isTxActive,
            isRx               = state.isRxActive,
            isConnected        = state.isConnected,
            onOpenWifiSettings = onOpenWifiSettings,
            modifier           = Modifier.weight(1f)
        )

        // RUTA: modo pantalla siempre-on con UI simplificada
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .clip(ControlShape)
                .background(if (state.isRouteActive) colors.accent else colors.surface)
                .border(BorderHairline, if (state.isRouteActive) colors.accent else colors.borderSubtle, ControlShape)
                .clickable { onAction(MainUiAction.ToggleRouteActive) }
                .padding(horizontal = 8.dp, vertical = 5.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text          = "RUTA",
                color         = if (state.isRouteActive) colors.background else colors.textSecondary,
                fontFamily    = ShareTechMono,
                fontSize      = 14.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // 2026-07-06: WAYPOINT se movió a la pantalla de Mapa -- el usuario
        // lo marcó dos veces como fuera de lugar acá ("es algo del mapa").

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
                fontSize   = 18.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        onClick = { onPick(theme) }
    )
}

// ── Modo Ruta Activa — UI minimalista para moto en movimiento ────────────
@Composable
private fun RouteActiveLayout(
    state: MainUiState,
    onAction: (MainUiAction) -> Unit
) {
    val colors = LocalMotoRFARColors.current
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        // Barra superior con estado TX/RX y salida de modo ruta
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            AppStatusBar(
                isTx        = state.isTxActive,
                isRx        = state.isRxActive,
                isConnected = state.isConnected,
                modifier    = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(ControlShape)
                    .background(colors.surface)
                    .border(BorderHairline, colors.accent, ControlShape)
                    .clickable { onAction(MainUiAction.ToggleRouteActive) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text          = "SALIR RUTA",
                    color         = colors.accent,
                    fontFamily    = ShareTechMono,
                    fontSize      = 15.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Frecuencia activa — máximo tamaño para lectura a alta velocidad
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(
                text          = state.activeChannelName,
                color         = if (state.isEmergencyActive) ar.motorfar.app.ui.compose.theme.EmergencyBorder else colors.textSecondary,
                fontFamily    = ShareTechMono,
                fontSize      = 18.sp,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = state.activeFrequency,
                color      = if (state.isEmergencyActive) ar.motorfar.app.ui.compose.theme.EmergencyBorder else colors.textPrimary,
                fontFamily = ShareTechMono,
                fontSize   = 72.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text          = "MHz" + (state.activeToneLabel?.let { " · $it" } ?: ""),
                color         = if (state.isEmergencyActive) ar.motorfar.app.ui.compose.theme.EmergencyBorder else colors.textSecondary,
                fontFamily    = ShareTechMono,
                fontSize      = 22.sp,
                letterSpacing = 2.sp
            )
        }

        // PTT grande central
        BoxWithConstraints(
            modifier         = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            val ptt = (minOf(maxWidth, maxHeight) - 32.dp).coerceIn(120.dp, 200.dp)
            PttButton(
                isTransmitting = state.isTxActive,
                enabled        = !state.isListenOnly,
                onPttDown      = { onAction(MainUiAction.PttPressed) },
                onPttUp        = { onAction(MainUiAction.PttReleased) },
                diameter       = ptt,
                isEmergency    = state.isEmergencyActive
            )
        }

        // Solo EMERGENCIA visible — modo ruta prioriza seguridad
        EmergencyConfirmButton(
            onConfirmed = { onAction(MainUiAction.EmergencyAlert) },
            isBlinking  = state.isEmergencyActive,
            modifier    = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        // Indicador de estado GPS auto-off
        Text(
            text          = "● GPS MONITOR · auto-off al detenerte",
            color         = colors.accent.copy(alpha = 0.6f),
            fontFamily    = ShareTechMono,
            fontSize      = 14.sp,
            letterSpacing = 1.sp,
            modifier      = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun ListenOnlyTag(modifier: Modifier = Modifier) {
    val colors = LocalMotoRFARColors.current
    Text(
        text          = "[ SOLO ESCUCHA ]",
        color         = colors.accent.copy(alpha = 0.75f),
        fontFamily    = ShareTechMono,
        fontSize      = 15.sp,
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
    onOpenSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // 2026-07-07: en pantallas chicas (Huawei P9, 640dp de alto) el
            // contenido fijo no entraba entero -- PTT y DETENCION/REAGRUPAR
            // quedaban invisibles, tapados por la barra de navegación (confirmado
            // con captura real). Scroll como red de seguridad: en pantallas
            // normales/grandes no se nota (todo entra sin gestos), en las chicas
            // permite bajar un toque en vez de perder acceso al PTT.
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(state, onAction, onOpenSettings, onOpenWifiSettings)

        // 2026-07-07: el cartel largo "SIN RADIO" se movió a Ajustes > WiFi del
        // equipo (WifiSettingScreen) -- ahí hay lugar para más info y scroll.
        // Acá solo queda el indicador compacto del TopBar (rojo titilante,
        // clickeable) para no ocupar tanto espacio fijo en la pantalla principal.

        FrequencyDisplayCard(
            frequency         = state.activeFrequency,
            sMeterLevel       = state.sMeterLevel,
            tone              = state.activeToneLabel,
            isEmergencyActive = state.isEmergencyActive
        )
        AlertBanner(
            alert     = state.activeAlert,
            onDismiss = onDismissAlert,
            modifier  = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        if (state.isListenOnly) ListenOnlyTag()

        Spacer(Modifier.height(4.dp))

        ChannelRow(
            channels       = state.channels,
            activeFreq     = state.activeFrequency,
            onChannelClick = { freq -> onAction(MainUiAction.ChannelSelected(freq)) }
        )

        Spacer(Modifier.height(6.dp))

        // 2026-07-07: 150dp fijo se veía perfecto en el emulador (pantalla enorme)
        // pero en el Huawei P9 (640dp de alto total) hacía que el resto de la
        // columna -- PTT, DETENCION/REAGRUPAR -- quedara totalmente fuera de
        // pantalla, tapado por la barra de navegación (confirmado con captura
        // real). Altura proporcional al alto real del dispositivo, con piso
        // (nunca invisible) y techo (nunca exagerado en pantallas gigantes).
        val visualizerHeight = (LocalConfiguration.current.screenHeightDp * 0.12f)
            .dp.coerceIn(48.dp, 150.dp)
        ModulationVisualizer(
            isActive = state.isTxActive || state.isRxActive,
            barColor = if (state.isEmergencyActive) ar.motorfar.app.ui.compose.theme.EmergencyBorder else null,
            modifier = Modifier
                .height(visualizerHeight)
                .padding(vertical = 2.dp)
        )

        Spacer(Modifier.height(6.dp))

        AlertButtonsPanel(
            onEmergency       = { onAction(MainUiAction.EmergencyAlert) },
            onStop            = { onAction(MainUiAction.StopAlert) },
            onRegroup         = { onAction(MainUiAction.RegroupAlert) },
            isEmergencyActive = state.isEmergencyActive
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 2026-07-07: weight(1f) necesita una Column de alto acotado --
                // no es compatible con el scroll que se agregó arriba (crashea:
                // "Vertically scrollable component was measured with an infinity
                // maximum height"). Alto fijo en su lugar; en pantallas grandes
                // sigue viéndose centrado con aire alrededor.
                .height(196.dp),
            contentAlignment = Alignment.Center
        ) {
            PttButton(
                isTransmitting = state.isTxActive,
                enabled        = !state.isListenOnly,
                onPttDown      = { onAction(MainUiAction.PttPressed) },
                onPttUp        = { onAction(MainUiAction.PttReleased) },
                isEmergency    = state.isEmergencyActive
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ── Horizontal (moto) ─────────────────────────────────────────────────
@Composable
private fun LandscapeLayout(
    state: MainUiState,
    onAction: (MainUiAction) -> Unit,
    onDismissAlert: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(state, onAction, onOpenSettings, onOpenWifiSettings)
        // 2026-07-07: el cartel "SIN RADIO" se movió a Ajustes > WiFi -- el
        // indicador compacto del TopBar (rojo titilante, clickeable) alcanza acá.
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
                    frequency         = state.activeFrequency,
                    sMeterLevel       = state.sMeterLevel,
                    tone              = state.activeToneLabel,
                    isEmergencyActive = state.isEmergencyActive,
                    compact           = true
                )
                Spacer(Modifier.height(4.dp))
                ChannelRow(
                    channels       = state.channels,
                    activeFreq     = state.activeFrequency,
                    onChannelClick = { freq -> onAction(MainUiAction.ChannelSelected(freq)) },
                    compact        = true
                )
                Spacer(Modifier.height(4.dp))
                ModulationVisualizer(
                    isActive = state.isTxActive || state.isRxActive,
                    barColor = if (state.isEmergencyActive) ar.motorfar.app.ui.compose.theme.EmergencyBorder else null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
            // Espacio lateral -- antes acá vivía la columna del PTT solo; ahora
            // el PTT se movió abajo de las alertas, este aire queda como
            // separación entre la info y los controles.
            Spacer(Modifier.weight(0.5f))
            // Columna 2: alertas arriba, PTT abajo, todo apilado
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isListenOnly) ListenOnlyTag()
                AlertButtonsPanel(
                    onEmergency       = { onAction(MainUiAction.EmergencyAlert) },
                    onStop            = { onAction(MainUiAction.StopAlert) },
                    onRegroup         = { onAction(MainUiAction.RegroupAlert) },
                    isEmergencyActive = state.isEmergencyActive
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val ptt = (minOf(maxWidth, maxHeight) - 8.dp).coerceIn(90.dp, 190.dp)
                    PttButton(
                        isTransmitting = state.isTxActive,
                        enabled        = !state.isListenOnly,
                        onPttDown      = { onAction(MainUiAction.PttPressed) },
                        onPttUp        = { onAction(MainUiAction.PttReleased) },
                        diameter       = ptt,
                        isEmergency    = state.isEmergencyActive
                    )
                }
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
