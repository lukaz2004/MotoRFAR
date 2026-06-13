package ar.motorfar.app.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import ar.motorfar.app.ui.compose.state.ChatMessage
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    onSend: (String) -> Unit,
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
            fontSize      = 14.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // Lista de mensajes
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "// SIN MENSAJES //",
                    color      = colors.textSecondary.copy(alpha = 0.5f),
                    fontFamily = ShareTechMono,
                    fontSize   = 12.sp
                )
            }
        } else {
            LazyColumn(
                state    = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(msg)
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
                    Text("Mensaje...", fontFamily = ShareTechMono, fontSize = 15.sp, color = colors.textSecondary)
                },
                textStyle     = TextStyle(
                    fontFamily = ShareTechMono,
                    fontSize   = 16.sp,
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
private fun ChatBubble(msg: ChatMessage) {
    val colors = LocalMotoRFARColors.current
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    color = if (msg.isOutgoing) colors.accent.copy(alpha = 0.18f) else colors.surface,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Alias remitente (solo en mensajes entrantes)
            if (!msg.isOutgoing) {
                Text(
                    text       = msg.fromAlias,
                    color      = colors.accent,
                    fontFamily = ShareTechMono,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text       = msg.text,
                color      = colors.textPrimary,
                fontFamily = ShareTechMono,
                fontSize   = 18.sp
            )
            Text(
                text       = timeFmt.format(Date(msg.timestampMs)),
                color      = colors.textSecondary.copy(alpha = 0.6f),
                fontFamily = ShareTechMono,
                fontSize   = 11.sp,
                modifier   = Modifier.align(Alignment.End)
            )
        }
    }
}
