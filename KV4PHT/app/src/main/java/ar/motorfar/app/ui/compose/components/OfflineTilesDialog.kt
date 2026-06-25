package ar.motorfar.app.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

@Composable
fun OfflineTilesDialog(
    progress: Float,
    tilesDone: Int,
    tilesTotal: Int,
    onCancel: () -> Unit
) {
    val colors = LocalMotoRFARColors.current
    Dialog(onDismissRequest = {}) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background, RoundedCornerShape(8.dp))
                .border(2.dp, colors.accent, RoundedCornerShape(8.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DESCARGANDO MAPA",
                color = colors.accent,
                fontFamily = ShareTechMono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Zona visible · zoom 10–16",
                color = colors.textSecondary,
                fontFamily = ShareTechMono,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = colors.accent,
                trackColor = colors.surface
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "$tilesDone / $tilesTotal tiles",
                color = colors.textSecondary,
                fontFamily = ShareTechMono,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                color = colors.accent,
                fontFamily = ShareTechMono,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onCancel) {
                Text(
                    text = "CANCELAR",
                    color = colors.textSecondary,
                    fontFamily = ShareTechMono
                )
            }
        }
    }
}
