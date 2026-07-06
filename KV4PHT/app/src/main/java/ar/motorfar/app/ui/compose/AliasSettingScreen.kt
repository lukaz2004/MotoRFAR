package ar.motorfar.app.ui.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.EmergencyBorder
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.MotoRFARTheme
import ar.motorfar.app.ui.compose.theme.ShareTechMono

// Intervalo de baliza en modo manual (fallback). Con SmartBeaconing activo,
// el intervalo se calcula dinámicamente según la velocidad.
private val BEACON_INTERVAL_OPTIONS = listOf(30, 60, 120, 300)
private val BEACON_INTERVAL_LABELS  = listOf("30s", "1min", "2min", "5min")

@Composable
fun AliasSettingScreen(
    currentAlias: String = "",
    currentBeaconIntervalSec: Int = 60,
    currentVolume: Int = 70,
    currentSmartBeacon: Boolean = true,
    currentManDown: Boolean = false,
    onSave: (alias: String, beaconIntervalSec: Int, volume: Int) -> Unit = { _, _, _ -> },
    onToggleSmartBeacon: (Boolean) -> Unit = {},
    onToggleManDown: (Boolean) -> Unit = {},
    onDownloadMaps: () -> Unit = {},
    onConfigureTones: () -> Unit = {},
    onConfigureWifi: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onAbout: () -> Unit = {}
) {
    val colors = LocalMotoRFARColors.current

    var aliasInput   by remember { mutableStateOf(currentAlias) }
    var smartBeacon  by remember { mutableStateOf(currentSmartBeacon) }
    var manDown      by remember { mutableStateOf(currentManDown) }
    var intervalIndex by remember {
        mutableFloatStateOf(
            BEACON_INTERVAL_OPTIONS.indexOf(currentBeaconIntervalSec).coerceAtLeast(0).toFloat()
        )
    }
    var volume by remember { mutableFloatStateOf(currentVolume.toFloat()) }

    val isAliasValid = AliasValidator.isValid(aliasInput)
    val borderColor  = if (isAliasValid || aliasInput.isEmpty()) colors.borderActive else EmergencyBorder

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text     = "TU ALIAS EN LA RED",
            color    = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 19.sp,
            letterSpacing = 2.sp
        )

        OutlinedTextField(
            value         = aliasInput,
            onValueChange = { raw ->
                aliasInput = AliasValidator.sanitize(raw).take(6)
            },
            modifier      = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(4.dp)),
            singleLine    = true,
            textStyle     = androidx.compose.ui.text.TextStyle(
                color      = colors.textPrimary,
                fontFamily = ShareTechMono,
                fontSize   = 28.sp
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType   = KeyboardType.Ascii
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = borderColor,
                unfocusedBorderColor = borderColor,
                cursorColor          = colors.textPrimary,
                focusedContainerColor   = colors.surface,
                unfocusedContainerColor = colors.surface
            ),
            supportingText = {
                if (!isAliasValid && aliasInput.isNotEmpty()) {
                    Text(
                        "1-6 caracteres A-Z 0-9, sin espacios",
                        color = EmergencyBorder,
                        fontFamily = ShareTechMono,
                        fontSize = 15.sp
                    )
                } else {
                    Text(
                        "Te identifica a vos en el mapa del grupo",
                        color = colors.textSecondary,
                        fontFamily = ShareTechMono,
                        fontSize = 15.sp
                    )
                }
            }
        )

        // ── Balizado ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = "BALIZA INTELIGENTE",
                    color    = colors.textPrimary,
                    fontFamily = ShareTechMono,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text     = "Ajusta el envío de posición según tu velocidad. Ahorra batería parado y es preciso en ruta.",
                    color    = colors.textSecondary,
                    fontFamily = ShareTechMono,
                    fontSize = 15.sp
                )
            }
            androidx.compose.material3.Switch(
                checked = smartBeacon,
                onCheckedChange = {
                    smartBeacon = it
                    onToggleSmartBeacon(it)
                },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor   = colors.accent,
                    checkedTrackColor   = colors.accent.copy(alpha = 0.4f),
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.surface
                )
            )
        }

        // Slider de intervalo manual — solo visible si la baliza inteligente está OFF
        if (!smartBeacon) {
            Text(
                text     = "INTERVALO FIJO",
                color    = colors.textSecondary,
                fontFamily = ShareTechMono,
                fontSize = 17.sp,
                letterSpacing = 1.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BEACON_INTERVAL_LABELS.forEach { label ->
                    Text(label, color = colors.textSecondary, fontFamily = ShareTechMono, fontSize = 17.sp)
                }
            }
            Slider(
                value         = intervalIndex,
                onValueChange = { intervalIndex = it },
                valueRange    = 0f..3f,
                steps         = 2,
                colors        = SliderDefaults.colors(
                    thumbColor       = colors.borderActive,
                    activeTrackColor = colors.borderActive
                )
            )
        }

        Spacer(Modifier.height(4.dp))

        // ── Man-Down ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = "DETECCIÓN DE CAÍDAS (MAN-DOWN)",
                    color    = colors.textPrimary,
                    fontFamily = ShareTechMono,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text     = "Si detecta un impacto fuerte seguido de quietud, asume que puede ser una caída o accidente: cuenta unos segundos (menos cuanto más fuerte el golpe) y transmite tu posición sin cifrar por VHF — la puede escuchar cualquiera en el canal, no solo tu grupo. Podés cancelarlo. Puede dispararse por golpes fuertes o terreno irregular (baches, pozos, caídas) sin que haya pasado nada en realidad.",
                    color    = colors.textSecondary,
                    fontFamily = ShareTechMono,
                    fontSize = 15.sp
                )
            }
            androidx.compose.material3.Switch(
                checked = manDown,
                onCheckedChange = {
                    manDown = it
                    onToggleManDown(it)
                },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor   = colors.accent,
                    checkedTrackColor   = colors.accent.copy(alpha = 0.4f),
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.surface
                )
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text     = "VOLUMEN ALERTAS  ${volume.toInt()}%",
            color    = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 18.sp,
            letterSpacing = 2.sp
        )
        Slider(
            value         = volume,
            onValueChange = { volume = it },
            valueRange    = 0f..100f,
            colors        = SliderDefaults.colors(
                thumbColor       = colors.borderActive,
                activeTrackColor = colors.borderActive
            )
        )

        Spacer(Modifier.height(4.dp))

        // ── Tonos CTCSS ───────────────────────────────────────────────
        Text(
            text     = "TONOS CTCSS",
            color    = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 18.sp,
            letterSpacing = 2.sp
        )
        Text(
            text     = "Elegí el tono por canal (Grupo/Alternativo) para no escuchar a otros grupos en el mismo canal.",
            color    = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize = 15.sp
        )
        OutlinedButton(
            onClick  = onConfigureTones,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(4.dp),
            border   = androidx.compose.foundation.BorderStroke(1.dp, colors.borderActive)
        ) {
            Text(
                text     = "CONFIGURAR TONOS →",
                color    = colors.textPrimary,
                fontFamily = ShareTechMono,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        // ── WiFi del equipo ───────────────────────────────────────────
        Text(
            text     = "WIFI DEL EQUIPO",
            color    = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 18.sp,
            letterSpacing = 2.sp
        )
        Text(
            text     = "Cambiá la clave WiFi del equipo por una que elijas vos.",
            color    = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize = 15.sp
        )
        OutlinedButton(
            onClick  = onConfigureWifi,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(4.dp),
            border   = androidx.compose.foundation.BorderStroke(1.dp, colors.borderActive)
        ) {
            Text(
                text     = "CONFIGURAR WIFI →",
                color    = colors.textPrimary,
                fontFamily = ShareTechMono,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        // ── Mapas offline ─────────────────────────────────────────────
        Text(
            text     = "MAPAS OFFLINE",
            color    = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 18.sp,
            letterSpacing = 2.sp
        )
        Text(
            text     = "Descargá el mapa de Argentina para usar sin señal en ruta.",
            color    = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize = 15.sp
        )
        OutlinedButton(
            onClick  = onDownloadMaps,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(4.dp),
            border   = androidx.compose.foundation.BorderStroke(1.dp, colors.borderActive),
            colors   = ButtonDefaults.outlinedButtonColors(
                contentColor = colors.accent
            )
        ) {
            Text("DESCARGAR MAPA DE ARGENTINA", fontFamily = ShareTechMono, fontSize = 17.sp, letterSpacing = 1.sp)
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text       = "Política de privacidad ›",
                color      = colors.accent,
                fontFamily = ShareTechMono,
                fontSize   = 14.sp,
                modifier   = Modifier
                    .clickable(onClick = onPrivacyPolicy)
                    .padding(vertical = 8.dp)
            )
            Text(
                text       = "Acerca de / Licencias ›",
                color      = colors.accent,
                fontFamily = ShareTechMono,
                fontSize   = 14.sp,
                modifier   = Modifier
                    .clickable(onClick = onAbout)
                    .padding(vertical = 8.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick  = {
                onSave(
                    aliasInput,
                    BEACON_INTERVAL_OPTIONS[intervalIndex.toInt().coerceIn(0, 3)],
                    volume.toInt()
                )
            },
            enabled  = isAliasValid,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(4.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = colors.borderActive,
                contentColor           = colors.background,
                disabledContainerColor = Color(0xFF2A2A2A),
                disabledContentColor   = Color(0xFF555555)
            )
        ) {
            Text("GUARDAR", fontFamily = ShareTechMono, letterSpacing = 3.sp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun AliasSettingScreenPreview() {
    MotoRFARTheme {
        AliasSettingScreen(currentAlias = "LUKAZ")
    }
}
