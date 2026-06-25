package ar.motorfar.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.BuildConfig
import ar.motorfar.app.ui.compose.theme.MotoRFARTheme
import ar.motorfar.app.ui.compose.theme.ShareTechMono

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MotoRFARTheme {
                AboutScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit = {}) {
    val bg      = Color(0xFF050505)
    val accent  = Color(0xFF00CC44)
    val textPri = Color(0xFFE0E0E0)
    val textSec = Color(0xFF888888)
    val divider = Color(0xFF1A1A1A)

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ACERCA DE",
                        fontFamily = ShareTechMono,
                        fontSize   = 16.sp,
                        letterSpacing = 3.sp,
                        color      = textPri
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = bg,
                    titleContentColor      = textPri,
                    navigationIconContentColor = accent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Encabezado ────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "BAQUEANO",
                    fontFamily    = ShareTechMono,
                    fontSize      = 28.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color         = accent
                )
                Text(
                    "v${BuildConfig.VERSION_NAME}  ·  VHF para grupos moto y 4×4",
                    fontFamily = ShareTechMono,
                    fontSize   = 13.sp,
                    color      = textSec
                )
                Text(
                    "Argentina · Resolución ENACOM 5/2015",
                    fontFamily = ShareTechMono,
                    fontSize   = 13.sp,
                    color      = textSec
                )
            }
            HorizontalDivider(color = divider)

            // ── Origen del proyecto ───────────────────────────────────────
            AboutSection(
                title   = "BASADO EN",
                accent  = accent,
                textPri = textPri,
                textSec = textSec,
                body    = """
kv4p HT — by Vance Vagell (KV4P)
https://github.com/VanceVagell/kv4p-ht

Baqueano es un fork de kv4p HT adaptado para Argentina:
frecuencias habilitadas por Resolución 5/2015 del ENACOM,
integración con el hardware SA818-V y la app rediseñada
para grupos vehiculares.

Gracias, Vance. El proyecto no existiría sin tu trabajo.
                """.trimIndent()
            )
            HorizontalDivider(color = divider)

            // ── Licencia ──────────────────────────────────────────────────
            AboutSection(
                title   = "LICENCIA",
                accent  = accent,
                textPri = textPri,
                textSec = textSec,
                body    = """
GNU General Public License v3.0 (GPL-3.0)

Este software es libre: podés redistribuirlo y/o
modificarlo bajo los términos de la GPL-3.0 publicada
por la Free Software Foundation.

El código fuente está disponible en:
https://github.com/lukaz2004/MotoRFAR

Cualquier distribución del binario debe incluir
acceso al código fuente completo.
                """.trimIndent()
            )
            HorizontalDivider(color = divider)

            // ── Dependencias open source ──────────────────────────────────
            AboutSection(
                title   = "DEPENDENCIAS DE CÓDIGO ABIERTO",
                accent  = accent,
                textPri = textPri,
                textSec = textSec,
                body    = """
OSMDroid 6.1.18 — Apache 2.0
  Mapas offline OpenStreetMap.

Concentus (jaredmdobson) — BSD
  Codec Opus puro Java para audio de voz.

esp32-flash-lib (dkaukov) — Apache 2.0
  Flash de firmware ESP32 por USB desde Android.

Project Lombok 1.18.30 — MIT
  Reducción de boilerplate en código Java.

slf4android (brightinventions) — MIT
  Bridge SLF4J → Android Logcat.

Apache Commons Math3 + Lang3 — Apache 2.0
  Utilidades matemáticas y de strings.

Google ZXing 3.4.1 — Apache 2.0
  Lectura y generación de códigos QR.

usbSerialForAndroid — LGPL-2.1
  Comunicación USB-serial con el ESP32.

Jetpack Compose / AndroidX — Apache 2.0
  Framework de UI declarativa Android (Google).

Google Play Services Location 21.3 — Propietario
  Acceso a GPS del dispositivo.
                """.trimIndent()
            )
            HorizontalDivider(color = divider)

            // ── Créditos ──────────────────────────────────────────────────
            AboutSection(
                title   = "CRÉDITOS",
                accent  = accent,
                textPri = textPri,
                textSec = textSec,
                body    = """
Desarrollo Baqueano: LuKaZ
Proyecto original kv4p HT: Vance Vagell, KV4P
Comunidad de radioaficionados y grupos 4x4 de Argentina
                """.trimIndent()
            )

            Spacer(Modifier.height(32.dp))

            Text(
                "\"Baqueano\" — el que conoce el camino.",
                fontFamily = ShareTechMono,
                fontSize   = 12.sp,
                color      = Color(0xFF444444),
                modifier   = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun AboutSection(
    title:   String,
    accent:  Color,
    textPri: Color,
    textSec: Color,
    body:    String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            fontFamily    = ShareTechMono,
            fontSize      = 12.sp,
            letterSpacing = 2.sp,
            color         = accent
        )
        Text(
            body,
            fontFamily = ShareTechMono,
            fontSize   = 14.sp,
            lineHeight = 22.sp,
            color      = textSec
        )
    }
}
