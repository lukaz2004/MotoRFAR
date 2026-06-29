package ar.motorfar.app.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

@Composable
fun FallCountdownOverlay(
    secondsLeft: Int,
    onCancel: () -> Unit
) {
    val colors = LocalMotoRFARColors.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "¡CAÍDA DETECTADA!",
                color = Color.Red,
                fontFamily = ShareTechMono,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Enviando alerta de emergencia en:",
                color = Color.White,
                fontFamily = ShareTechMono,
                fontSize = 16.sp
            )
            
            Spacer(Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.Red, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = secondsLeft.toString(),
                    color = Color.White,
                    fontFamily = ShareTechMono,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(40.dp))
            
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable { onCancel() }
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "ESTOY BIEN · CANCELAR",
                    color = colors.background,
                    fontFamily = ShareTechMono,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
