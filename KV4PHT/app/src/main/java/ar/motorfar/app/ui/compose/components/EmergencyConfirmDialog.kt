package ar.motorfar.app.ui.compose.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ar.motorfar.app.ui.compose.theme.EmergencyBackground
import ar.motorfar.app.ui.compose.theme.EmergencyBorder
import ar.motorfar.app.ui.compose.theme.EmergencyText
import ar.motorfar.app.ui.compose.theme.ShareTechMono
import kotlinx.coroutines.delay

private const val HOLD_DURATION_MS = 2000L
private const val HOLD_STEP_MS     = 50L

@Composable
fun EmergencyConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = tween(durationMillis = HOLD_DURATION_MS.toInt()),
        label = "emergency_hold_progress"
    )

    LaunchedEffect(holding) {
        if (holding) {
            var elapsed = 0L
            while (elapsed < HOLD_DURATION_MS) {
                delay(HOLD_STEP_MS)
                elapsed += HOLD_STEP_MS
                progress = elapsed.toFloat() / HOLD_DURATION_MS.toFloat()
            }
            onConfirm()
        } else {
            progress = 0f
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EmergencyBackground, RoundedCornerShape(8.dp))
                .border(2.dp, EmergencyBorder, RoundedCornerShape(8.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠ EMERGENCIA",
                color = EmergencyText,
                fontFamily = ShareTechMono,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Mantené presionado el botón 2s\npara confirmar la alerta",
                color = EmergencyText.copy(alpha = 0.8f),
                fontFamily = ShareTechMono,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(EmergencyBorder.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .border(1.dp, EmergencyBorder, RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                holding = true
                                tryAwaitRelease()
                                holding = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    color = EmergencyBorder,
                    trackColor = Color.Transparent
                )
                Text(
                    text = if (holding) "MANTENGA..." else "PRESIONAR",
                    color = EmergencyText,
                    fontFamily = ShareTechMono,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCELAR",
                    color = EmergencyText.copy(alpha = 0.7f),
                    fontFamily = ShareTechMono
                )
            }
        }
    }
}
