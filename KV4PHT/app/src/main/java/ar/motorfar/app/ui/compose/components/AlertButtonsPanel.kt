package ar.motorfar.app.ui.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.EmergencyBackground
import ar.motorfar.app.ui.compose.theme.EmergencyBorder
import ar.motorfar.app.ui.compose.theme.EmergencyText
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

private val btnShape = RoundedCornerShape(4.dp)

@Composable
fun AlertButtonsPanel(
    onEmergency: () -> Unit,
    onStop: () -> Unit,
    onRegroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // EMERGENCIA — full width, rojo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(EmergencyBackground, btnShape)
                .border(2.dp, EmergencyBorder, btnShape)
                .clickable(onClick = onEmergency)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "EMERGENCIA",
                color      = Color.White,
                fontFamily = ShareTechMono,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // DETENCIÓN + REAGRUPAR en Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("DETENCIÓN" to onStop, "REAGRUPAR" to onRegroup).forEach { (label, action) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.surface, btnShape)
                        .border(1.dp, colors.borderActive, btnShape)
                        .clickable(onClick = action)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = label,
                        color      = colors.textPrimary,
                        fontFamily = ShareTechMono,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
