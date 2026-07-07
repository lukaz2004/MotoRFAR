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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.BorderHairline
import ar.motorfar.app.ui.compose.theme.ControlShape
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

@Composable
fun AlertButtonsPanel(
    onEmergency: () -> Unit,
    onStop: () -> Unit,
    onRegroup: () -> Unit,
    isEmergencyActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // EMERGENCIA — hold 2s para confirmar (no dispara con tap corto)
        EmergencyConfirmButton(
            onConfirmed = onEmergency,
            isBlinking  = isEmergencyActive,
            modifier    = Modifier.fillMaxWidth()
        )

        // DETENCIÓN + REAGRUPAR en Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("DETENCIÓN" to onStop, "REAGRUPAR" to onRegroup).forEach { (label, action) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.surface, ControlShape)
                        .border(BorderHairline, colors.borderActive, ControlShape)
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
