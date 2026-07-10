package ar.motorfar.app.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.MotoRFARTheme
import ar.motorfar.app.ui.compose.theme.ShareTechMono
import ar.motorfar.app.ui.compose.theme.ThemePreference

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val theme = ThemePreference.get(this)
        setContent {
            MotoRFARTheme(theme = theme) {
                PrivacyPolicyScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val colors = LocalMotoRFARColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "POLITICA DE PRIVACIDAD",
            color = colors.accent,
            fontFamily = ShareTechMono,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 48.dp, bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Section("MotoRFAR HT")
            Body("Aplicación de radio VHF para motociclistas y 4x4 en Argentina. Opera en frecuencias libres de la Resolución 5/2015 (M.T.T.T.): 138.510, 139.970 y 140.970 MHz.")

            Section("DATOS QUE USA LA APP")
            Bullet("Ubicación GPS — mostrar tu posición en el mapa y transmitirla sin cifrar por VHF. No se envía a internet, pero cualquiera con un equipo sintonizado al mismo canal puede escucharla y decodificarla, no solo tu grupo.")
            Bullet("Audio del micrófono — transmitir voz por PTT a través del módulo de radio. No se graba ni almacena.")
            Bullet("Frecuencia de operación — sintonizar el transceiver en los canales permitidos.")
            Bullet("Alias / indicativo — identificarte dentro del grupo.")
            Bullet("Tiles de mapa — se descargan de OpenStreetMap y se guardan en caché local para uso offline.")

            Section("QUÉ NO HACEMOS")
            Bullet("No recopilamos datos personales para publicidad, analítica ni perfilamiento.")
            Bullet("No enviamos datos a servidores propios ni de terceros. Toda la comunicación ocurre por radio VHF.")
            Bullet("No almacenamos grabaciones de audio. El audio PTT se transmite en tiempo real.")
            Bullet("No rastreamos tu ubicación en segundo plano. El GPS solo se usa con la app abierta.")

            Section("ALMACENAMIENTO LOCAL")
            Body("La app guarda en tu dispositivo: configuración de radio, caché de tiles del mapa, y tu alias. Estos datos se borran al desinstalar la app.")

            Section("HARDWARE")
            Body("MotoRFAR HT requiere un módulo de radio externo (ESP32 + SA868S) conectado por USB. El hardware es propiedad del usuario.")

            Section("PERMISOS")
            Bullet("Ubicación (GPS) — mostrar tu posición en el mapa.")
            Bullet("Micrófono — transmitir audio por PTT.")
            Bullet("USB — comunicarse con el módulo de radio.")
            Bullet("Internet — descargar tiles del mapa.")

            Section("CÓDIGO ABIERTO")
            Body("MotoRFAR HT es software libre bajo licencia GNU GPL v3.0. El código fuente está disponible para su auditoría.")

            Section("CONTACTO")
            Body("lukaz1979@gmail.com")

            Spacer(Modifier.height(32.dp))
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "VOLVER",
                color = colors.accent,
                fontFamily = ShareTechMono,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun Section(title: String) {
    val colors = LocalMotoRFARColors.current
    Spacer(Modifier.height(20.dp))
    Text(
        text = title,
        color = colors.accent,
        fontFamily = ShareTechMono,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Body(text: String) {
    val colors = LocalMotoRFARColors.current
    Text(
        text = text,
        color = colors.textPrimary,
        fontFamily = ShareTechMono,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )
}

@Composable
private fun Bullet(text: String) {
    val colors = LocalMotoRFARColors.current
    Text(
        text = "· $text",
        color = colors.textPrimary,
        fontFamily = ShareTechMono,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}
