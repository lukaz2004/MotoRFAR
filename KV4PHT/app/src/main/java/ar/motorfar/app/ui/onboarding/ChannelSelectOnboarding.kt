package ar.motorfar.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.EmergencyBorder
import ar.motorfar.app.ui.compose.theme.EmergencyText
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

private data class ChannelOption(val name: String, val freq: String, val desc: String)

private val ARGENTINA_CHANNELS = listOf(
    ChannelOption("GRUPO",       "139.9700", "Canal principal del grupo"),
    ChannelOption("ALTERNATIVO", "138.5100", "Canal de respaldo"),
    ChannelOption("EMERGENCIA",  "140.9700", "Solo para emergencias")
)

@Composable
fun ChannelSelectOnboarding(
    step: Int,
    onComplete: (freq: String) -> Unit
) {
    val colors    = LocalMotoRFARColors.current
    val chipShape = RoundedCornerShape(4.dp)
    var selected by remember { mutableStateOf<ChannelOption?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            "¿EN QUÉ CANAL ESTÁ TU GRUPO?",
            color = colors.textPrimary, fontFamily = ShareTechMono,
            fontSize = 14.sp, letterSpacing = 2.sp
        )
        Text(
            "Paso $step de 3",
            color = colors.textSecondary, fontFamily = ShareTechMono, fontSize = 12.sp
        )
        Spacer(Modifier.height(32.dp))

        ARGENTINA_CHANNELS.forEach { channel ->
            val isEmergency = channel.freq == "140.9700"
            val isSelected  = selected == channel
            val borderColor = when {
                isEmergency -> EmergencyBorder
                isSelected  -> colors.borderActive
                else        -> colors.borderSubtle
            }
            val textColor = when {
                isEmergency -> EmergencyText
                isSelected  -> colors.textPrimary
                else        -> colors.textSecondary
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(colors.surface, chipShape)
                    .border(if (isSelected || isEmergency) 2.dp else 1.dp, borderColor, chipShape)
                    .clickable { selected = channel }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        channel.name, color = textColor,
                        fontFamily = ShareTechMono, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        channel.desc, color = colors.textSecondary,
                        fontFamily = ShareTechMono, fontSize = 12.sp
                    )
                }
                Text(
                    "${channel.freq} MHz", color = textColor,
                    fontFamily = ShareTechMono, fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick  = { selected?.let { onComplete(it.freq) } },
            enabled  = selected != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(4.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = colors.borderActive,
                contentColor           = colors.background,
                disabledContainerColor = colors.surface,
                disabledContentColor   = colors.textSecondary
            )
        ) {
            Text("LISTO", fontFamily = ShareTechMono, letterSpacing = 3.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}
