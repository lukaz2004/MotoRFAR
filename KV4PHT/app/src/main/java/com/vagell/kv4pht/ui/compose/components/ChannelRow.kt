package com.vagell.kv4pht.ui.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vagell.kv4pht.data.ChannelMemory
import com.vagell.kv4pht.ui.compose.theme.EmergencyBorder
import com.vagell.kv4pht.ui.compose.theme.LocalMotoRFARColors
import com.vagell.kv4pht.ui.compose.theme.ShareTechMono

private const val EMERGENCY_FREQ = "140.970"
private val chipShape = RoundedCornerShape(4.dp)

@Composable
fun ChannelRow(
    channels: List<ChannelMemory>,
    activeFreq: String,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        channels.forEach { channel ->
            val freq = channel.frequency ?: ""
            val isActive    = freq == activeFreq
            val isEmergency = freq == EMERGENCY_FREQ
            val borderColor = when {
                isEmergency -> EmergencyBorder
                isActive    -> colors.borderActive
                else        -> colors.borderSubtle
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(colors.surface, chipShape)
                    .border(if (isActive || isEmergency) 2.dp else 1.dp, borderColor, chipShape)
                    .clickable { onChannelClick(freq) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = channel.name ?: freq,
                    color     = if (isEmergency) com.vagell.kv4pht.ui.compose.theme.EmergencyText
                                else if (isActive) colors.textPrimary
                                else colors.textSecondary,
                    fontFamily = ShareTechMono,
                    fontSize  = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines  = 1
                )
            }
        }
    }
}
