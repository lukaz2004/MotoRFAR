package ar.motorfar.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.AliasValidator
import ar.motorfar.app.ui.compose.theme.EmergencyBorder
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

@Composable
fun AliasSetupOnboarding(
    step: Int,
    onSave: (alias: String) -> Unit
) {
    val colors = LocalMotoRFARColors.current
    var aliasInput by remember { mutableStateOf("") }
    val isValid     = AliasValidator.isValid(aliasInput)
    val borderColor = if (isValid || aliasInput.isEmpty()) colors.borderActive else EmergencyBorder

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            "TU ALIAS EN LA RED",
            color = colors.textPrimary, fontFamily = ShareTechMono,
            fontSize = 14.sp, letterSpacing = 2.sp
        )
        Text(
            "Paso $step de 3",
            color = colors.textSecondary, fontFamily = ShareTechMono, fontSize = 12.sp
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Tu alias identifica tu moto en el mapa del grupo.\n1-6 caracteres, A-Z y 0-9.",
            color = colors.textSecondary, fontFamily = ShareTechMono, fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value         = aliasInput,
            onValueChange = { raw -> aliasInput = AliasValidator.sanitize(raw).take(6) },
            modifier      = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(4.dp)),
            singleLine    = true,
            textStyle     = TextStyle(
                color      = colors.textPrimary,
                fontFamily = ShareTechMono,
                fontSize   = 32.sp
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType   = KeyboardType.Ascii
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = borderColor,
                unfocusedBorderColor    = borderColor,
                cursorColor             = colors.textPrimary,
                focusedContainerColor   = colors.surface,
                unfocusedContainerColor = colors.surface
            )
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick  = { onSave(aliasInput) },
            enabled  = isValid,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(4.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = colors.borderActive,
                contentColor           = colors.background,
                disabledContainerColor = colors.surface,
                disabledContentColor   = colors.textSecondary
            )
        ) {
            Text("GUARDAR", fontFamily = ShareTechMono, letterSpacing = 3.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}
