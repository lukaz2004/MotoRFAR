package ar.motorfar.app.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.R
import ar.motorfar.app.ui.AlertHelper
import ar.motorfar.app.ui.compose.state.ChatMessage
import ar.motorfar.app.ui.compose.state.ReceivedAlert
import ar.motorfar.app.ui.compose.theme.EmergencyBackground
import ar.motorfar.app.ui.compose.theme.EmergencyBorder
import ar.motorfar.app.ui.compose.theme.EmergencyText
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.RegroupBackground
import ar.motorfar.app.ui.compose.theme.RegroupBorder
import ar.motorfar.app.ui.compose.theme.RegroupText
import ar.motorfar.app.ui.compose.theme.ShareTechMono
import ar.motorfar.app.ui.compose.theme.StopBackground
import ar.motorfar.app.ui.compose.theme.StopBorder
import ar.motorfar.app.ui.compose.theme.StopText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    alertHistory: List<ReceivedAlert> = emptyList(),
    onSend: (String) -> Unit,
    onGoToLocation: (Double, Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll al último mensaje
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        // Encabezado
        Text(
            text          = "CHAT VHF",
            color         = colors.accent,
            fontFamily    = ShareTechMono,
            fontSize      = 19.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // Historial de alertas recibidas (últimas 5)
        if (alertHistory.isNotEmpty()) {
            AlertHistoryPanel(alertHistory)
        }

        // Lista de mensajes
        if (messages.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text          = "// CANAL EN SILENCIO //",
                    color         = colors.textSecondary.copy(alpha = 0.7f),
                    fontFamily    = ShareTechMono,
                    fontSize      = 18.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "Mantené PTT para hablar, o escribí abajo para enviar texto al grupo.",
                    color      = colors.textGhost,
                    fontFamily = ShareTechMono,
                    fontSize   = 15.sp,
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        } else {
            LazyColumn(
                state    = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(msg, onGoToLocation)
                }
            }
        }

        // Barra de entrada
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = draft,
                onValueChange = { if (it.length <= 67) draft = it }, // límite APRS típico
                modifier      = Modifier.weight(1f),
                placeholder   = {
                    Text("Mensaje...", fontFamily = ShareTechMono, fontSize = 20.sp, color = colors.textSecondary)
                },
                textStyle     = TextStyle(
                    fontFamily = ShareTechMono,
                    fontSize   = 22.sp,
                    color      = colors.textPrimary
                ),
                singleLine    = true,
                shape         = RoundedCornerShape(6.dp),
                colors        = TextFieldDefaults.colors(
                    focusedContainerColor   = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedIndicatorColor   = colors.accent,
                    unfocusedIndicatorColor = colors.borderActive,
                    cursorColor             = colors.accent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    val text = draft.trim()
                    if (text.isNotEmpty()) { onSend(text); draft = "" }
                })
            )
            IconButton(
                onClick = {
                    val text = draft.trim()
                    if (text.isNotEmpty()) { onSend(text); draft = "" }
                }
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_send),
                    contentDescription = "Enviar",
                    tint               = colors.accent
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    msg: ChatMessage,
    onGoToLocation: (Double, Double) -> Unit = { _, _ -> }
) {
    val colors = LocalMotoRFARColors.current
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Colores según tipo de alerta (o burbuja normal)
    val isAlert = msg.alertType != null
    val bg = when (msg.alertType) {
        ChatMessage.AlertKind.EMERGENCY -> EmergencyBackground
        ChatMessage.AlertKind.STOP      -> StopBackground
        ChatMessage.AlertKind.REGROUP   -> RegroupBackground
        null -> if (msg.isOutgoing) colors.accent.copy(alpha = 0.18f) else colors.surface
    }
    val borderColor = when (msg.alertType) {
        ChatMessage.AlertKind.EMERGENCY -> EmergencyBorder
        ChatMessage.AlertKind.STOP      -> StopBorder
        ChatMessage.AlertKind.REGROUP   -> RegroupBorder
        null -> null
    }
    val txtColor = when (msg.alertType) {
        ChatMessage.AlertKind.EMERGENCY -> EmergencyText
        ChatMessage.AlertKind.STOP      -> StopText
        ChatMessage.AlertKind.REGROUP   -> RegroupText
        null -> colors.textPrimary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = when {
            isAlert         -> Arrangement.Center      // alertas centradas, ocupan ancho
            msg.isOutgoing  -> Arrangement.End
            else            -> Arrangement.Start
        }
    ) {
        Column(
            modifier = Modifier
                .then(if (isAlert) Modifier.fillMaxWidth() else Modifier.widthIn(max = 320.dp))
                .background(color = bg, shape = RoundedCornerShape(8.dp))
                .then(
                    if (borderColor != null)
                        Modifier.border(2.dp, borderColor, RoundedCornerShape(8.dp))
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Encabezado de alerta con ícono
            if (isAlert) {
                val header = when (msg.alertType) {
                    ChatMessage.AlertKind.EMERGENCY -> "⚠ EMERGENCIA"
                    ChatMessage.AlertKind.STOP      -> "■ DETENCIÓN"
                    ChatMessage.AlertKind.REGROUP   -> "● REAGRUPAMIENTO"
                    null -> ""
                }
                Text(
                    text          = "$header · ${msg.fromAlias}",
                    color         = txtColor,
                    fontFamily    = ShareTechMono,
                    fontSize      = 20.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
            } else if (!msg.isOutgoing) {
                // Alias remitente (mensajes normales entrantes)
                Text(
                    text       = msg.fromAlias,
                    color      = colors.accent,
                    fontFamily = ShareTechMono,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Cuerpo del mensaje — 22sp para lectura desde lejos
            Text(
                text       = msg.text,
                color      = txtColor,
                fontFamily = ShareTechMono,
                fontSize   = 22.sp,
                fontWeight = if (isAlert) FontWeight.Bold else FontWeight.Normal
            )

            // Posición GPS + botón "ir a la ubicación" (solo alertas con coordenadas)
            if (isAlert && msg.lat != null && msg.lon != null) {
                val lat = msg.lat
                val lon = msg.lon
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = "POS: %.5f, %.5f".format(lat, lon),
                    color      = txtColor.copy(alpha = 0.85f),
                    fontFamily = ShareTechMono,
                    fontSize   = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, txtColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .clickable { onGoToLocation(lat, lon) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_pin),
                        contentDescription = null,
                        tint               = txtColor,
                        modifier           = Modifier.size(16.dp)
                    )
                    Text(
                        text          = "IR A UBICACIÓN",
                        color         = txtColor,
                        fontFamily    = ShareTechMono,
                        fontSize      = 17.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier      = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text       = timeFmt.format(Date(msg.timestampMs)),
                color      = (if (isAlert) txtColor else colors.textSecondary).copy(alpha = 0.6f),
                fontFamily = ShareTechMono,
                fontSize   = 15.sp,
                modifier   = Modifier.align(Alignment.End)
            )
        }
    }
}

// ── Historial de alertas recibidas (últimas 5) ────────────────────────────
@Composable
private fun AlertHistoryPanel(alerts: List<ReceivedAlert>) {
    val colors = LocalMotoRFARColors.current
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text          = "ALERTAS RECIENTES",
            color         = colors.textSecondary,
            fontFamily    = ShareTechMono,
            fontSize      = 14.sp,
            letterSpacing = 2.sp
        )
        alerts.forEach { alert ->
            val (label, tint) = when (alert.type) {
                AlertHelper.AlertType.EMERGENCY -> "⚠ EMERGENCIA" to EmergencyText
                AlertHelper.AlertType.STOP      -> "■ DETENCIÓN"  to StopText
                AlertHelper.AlertType.REGROUP   -> "● REAGRUPAR"  to RegroupText
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = "$label · ${alert.fromAlias}",
                    color      = tint,
                    fontFamily = ShareTechMono,
                    fontSize   = 17.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text       = timeFmt.format(Date(alert.receivedAtMs)),
                    color      = colors.textSecondary,
                    fontFamily = ShareTechMono,
                    fontSize   = 15.sp
                )
            }
        }
    }
}
