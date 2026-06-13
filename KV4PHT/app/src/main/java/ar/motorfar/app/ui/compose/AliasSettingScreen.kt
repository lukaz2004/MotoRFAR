package ar.motorfar.app.ui.compose

import androidx.compose.foundation.border
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

private val BEACON_INTERVAL_OPTIONS = listOf(30, 60, 120, 300)
private val BEACON_INTERVAL_LABELS  = listOf("30s", "1min", "2min", "5min")

@Composable
fun AliasSettingScreen(
    currentAlias: String = "",
    currentBeaconIntervalSec: Int = 60,
    currentVolume: Int = 70,
    onSave: (alias: String, beaconIntervalSec: Int, volume: Int) -> Unit = { _, _, _ -> }
) {
    val colors = LocalMotoRFARColors.current

    var aliasInput   by remember { mutableStateOf(currentAlias) }
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
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text     = "TU ALIAS EN LA RED",
            color    = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 14.sp,
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
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        "Identificás tu moto en el mapa del grupo",
                        color = colors.textSecondary,
                        fontFamily = ShareTechMono,
                        fontSize = 11.sp
                    )
                }
            }
        )

        Text(
            text     = "INTERVALO DE BALIZA",
            color    = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 13.sp,
            letterSpacing = 2.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BEACON_INTERVAL_LABELS.forEach { label ->
                Text(label, color = colors.textSecondary, fontFamily = ShareTechMono, fontSize = 12.sp)
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

        Spacer(Modifier.height(4.dp))

        Text(
            text     = "VOLUMEN ALERTAS  ${volume.toInt()}%",
            color    = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 13.sp,
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

        Spacer(Modifier.weight(1f))

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
