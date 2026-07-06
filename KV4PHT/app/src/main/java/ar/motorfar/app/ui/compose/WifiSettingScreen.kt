package ar.motorfar.app.ui.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.EmergencyBorder
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

/**
 * Cambiar la clave WPA2 del SoftAP del equipo. El firmware la aplica de
 * inmediato — el teléfono se desconecta del WiFi del equipo justo después,
 * hay que reconectarse a mano con la clave nueva desde Ajustes de Android.
 */
@Composable
fun WifiSettingScreen(
    isConnected: Boolean,
    onSavePassword: (String) -> Boolean,
    onBack: () -> Unit = {}
) {
    val colors = LocalMotoRFARColors.current
    var password by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    val isValid = password.length in 8..63

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text     = "‹ Volver",
            color    = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize = 15.sp,
            modifier = Modifier.clickable(onClick = onBack)
        )

        Text(
            text     = "WIFI DEL EQUIPO",
            color    = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 22.sp,
            letterSpacing = 2.sp
        )

        Text(
            text = "Cada equipo trae una clave única generada sola (no es la misma para todos). " +
                   "Acá podés cambiarla por una que elijas vos.",
            color = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize = 14.sp
        )

        if (!isConnected) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmergencyBorder, RoundedCornerShape(4.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Sin conexión con el equipo — conectate primero para poder cambiar la clave.",
                    color = EmergencyBorder,
                    fontFamily = ShareTechMono,
                    fontSize = 13.sp
                )
            }
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it.take(63) },
            label = { Text("Clave nueva (8-63 caracteres)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = colors.borderActive,
                unfocusedBorderColor = colors.borderActive,
                cursorColor          = colors.textPrimary,
                focusedContainerColor   = colors.surface,
                unfocusedContainerColor = colors.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.borderActive, RoundedCornerShape(4.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "⚠ Al guardar, el equipo cambia la clave YA y tu teléfono se " +
                       "desconecta del WiFi automáticamente. Tenés que reconectarte a mano " +
                       "con la clave nueva desde Ajustes de WiFi de Android.\n\n" +
                       "ANOTÁ la clave antes de guardar — si te olvidás cuál pusiste, " +
                       "no hay forma de recuperarla sin volver a flashear el equipo.",
                color = colors.textSecondary,
                fontFamily = ShareTechMono,
                fontSize = 13.sp
            )
        }

        Button(
            onClick = { showConfirm = true },
            enabled = isValid && isConnected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("GUARDAR CLAVE NUEVA", fontFamily = ShareTechMono)
        }

        resultMessage?.let {
            Text(
                text = it,
                color = colors.accent,
                fontFamily = ShareTechMono,
                fontSize = 14.sp
            )
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("¿Cambiar la clave WiFi?") },
            text = {
                Text(
                    "El equipo va a cambiar la clave YA. Tu teléfono se va a " +
                    "desconectar y vas a tener que reconectarte a mano con la clave " +
                    "nueva. ¿Ya la anotaste en algún lado?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    val ok = onSavePassword(password)
                    resultMessage = if (ok) {
                        "Clave enviada. Reconectate al WiFi del equipo con la clave nueva."
                    } else {
                        "No se pudo mandar — revisá que el equipo esté conectado."
                    }
                }) { Text("Sí, cambiar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}
