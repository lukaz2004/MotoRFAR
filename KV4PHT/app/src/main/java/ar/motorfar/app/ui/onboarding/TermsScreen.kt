package ar.motorfar.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

@Composable
fun TermsScreen(
    step: Int,
    onAccept: () -> Unit
) {
    val colors = LocalMotoRFARColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text       = "Baqueano",
            color      = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text       = "Términos de uso — $step de 3",
            color      = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize   = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            TermsSection(
                "1. Marco legal y uso del espectro",
                "Baqueano opera bajo la Resolución 5/2015 del Ministerio de Transporte " +
                "de la Nación Argentina, que habilita el uso de estas frecuencias VHF " +
                "sin licencia individual para comunicaciones de grupos en tránsito:\n\n" +
                "  139.970 MHz — PRINCIPAL (prioritaria)\n" +
                "  138.510 MHz — ALTERNATIVO (secundaria)\n" +
                "  140.970 MHz — EMERGENCIA (uso exclusivo)\n\n" +
                "Cualquier transmisión fuera de estas frecuencias es responsabilidad exclusiva del usuario."
            )
            TermsSection(
                "2. Uso permitido",
                "• Comunicación grupal entre vehículos en tránsito.\n" +
                "• Transmisión de posición GPS en las frecuencias habilitadas.\n" +
                "• Alertas de emergencia entre miembros del grupo.\n\n" +
                "No usar para comunicaciones fuera del grupo en tránsito. No usar fuera del " +
                "territorio argentino sin verificar la normativa local."
            )
            TermsSection(
                "3. Uso responsable de alertas",
                "Las alertas (Emergencia, Detención, Reagrupamiento) transmiten tu posición GPS. " +
                "Usarlas sólo cuando correspondan a la situación real. El envío de alertas falsas " +
                "puede constituir una infracción a la Ley N° 19.798."
            )
            TermsSection(
                "4. Privacidad",
                "Esta aplicación no recopila ni transmite datos personales a servidores externos. " +
                "Toda la información se almacena exclusivamente en el dispositivo."
            )
            TermsSection(
                "5. Exención de responsabilidad",
                "Baqueano se provee tal como está (as-is), sin garantías de funcionamiento, " +
                "cobertura ni recepción. El usuario acepta usarla bajo su exclusiva responsabilidad."
            )
            TermsSection(
                "6. Código abierto",
                "Basado en kv4p HT por Vance Vagell (KV4P), distribuido bajo GPL-3.0.\n" +
                "github.com/VanceVagell/kv4p-ht"
            )
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick  = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape  = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.borderActive,
                contentColor   = colors.background
            )
        ) {
            Text("ACEPTAR", fontFamily = ShareTechMono, letterSpacing = 3.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TermsSection(title: String, body: String) {
    val colors = LocalMotoRFARColors.current
    Text(
        text       = title,
        color      = colors.textPrimary,
        fontFamily = ShareTechMono,
        fontSize   = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier   = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
    Text(
        text       = body,
        color      = colors.textSecondary,
        fontFamily = ShareTechMono,
        fontSize   = 13.sp,
        lineHeight = 20.sp
    )
}
