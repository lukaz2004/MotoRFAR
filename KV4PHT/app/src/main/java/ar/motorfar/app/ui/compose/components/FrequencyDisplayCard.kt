package ar.motorfar.app.ui.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

@Composable
fun FrequencyDisplayCard(
    frequency: String,
    channelName: String,
    sMeterLevel: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.display)
            .border(1.dp, colors.borderSubtle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text  = channelName,
            color = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize = 9.sp,
            letterSpacing = 0.15.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text  = frequency,
            color = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 48.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Text(
            text  = "MHz · FM · SIMPLEX",
            color = colors.textGhost,
            fontFamily = ShareTechMono,
            fontSize = 9.sp
        )
        Spacer(Modifier.height(8.dp))
        SMeter(level = sMeterLevel)
    }
}

@Composable
fun SMeter(level: Int, modifier: Modifier = Modifier) {
    val colors = LocalMotoRFARColors.current
    val barCount = 9
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .padding(horizontal = 4.dp)
    ) {
        val barWidth  = (size.width - (barCount - 1) * 3f) / barCount
        for (i in 0 until barCount) {
            val barHeight = size.height * (0.4f + 0.6f * (i + 1) / barCount)
            val x = i * (barWidth + 3f)
            val alpha = if (i < level) 1f else 0.2f
            drawRect(
                color    = colors.accent.copy(alpha = alpha),
                topLeft  = Offset(x, size.height - barHeight),
                size     = Size(barWidth, barHeight)
            )
        }
    }
}
