package com.vagell.kv4pht.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vagell.kv4pht.ui.AlertHelper
import com.vagell.kv4pht.ui.compose.state.ReceivedAlert
import com.vagell.kv4pht.ui.compose.theme.EmergencyBackground
import com.vagell.kv4pht.ui.compose.theme.EmergencyBorder
import com.vagell.kv4pht.ui.compose.theme.EmergencyText
import com.vagell.kv4pht.ui.compose.theme.LocalMotoRFARColors
import com.vagell.kv4pht.ui.compose.theme.ShareTechMono

private val bannerShape = RoundedCornerShape(4.dp)

@Composable
fun AlertBanner(
    alert: ReceivedAlert?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = alert != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit  = slideOutVertically(targetOffsetY  = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        alert ?: return@AnimatedVisibility
        val colors = LocalMotoRFARColors.current
        val isEmergency = alert.type == AlertHelper.AlertType.EMERGENCY
        val bgColor     = if (isEmergency) EmergencyBackground else colors.surface
        val borderColor = if (isEmergency) EmergencyBorder     else colors.borderActive
        val textColor   = if (isEmergency) EmergencyText       else colors.textPrimary
        val label = when (alert.type) {
            AlertHelper.AlertType.EMERGENCY -> "⚠ EMERGENCIA"
            AlertHelper.AlertType.STOP      -> "⚠ DETENCIÓN"
            AlertHelper.AlertType.REGROUP   -> "⚠ REAGRUPAR"
        }

        Row(
            modifier = Modifier
                .testTag("alert_banner")
                .fillMaxWidth()
                .background(bgColor, bannerShape)
                .border(1.dp, borderColor, bannerShape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label — ${alert.fromAlias}",
                color = textColor,
                fontFamily = ShareTechMono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "×",
                color = textColor,
                fontFamily = ShareTechMono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(onClick = onDismiss)
            )
        }
    }
}
