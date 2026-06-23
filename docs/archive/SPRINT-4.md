# SPRINT 4 — Migración UI a Jetpack Compose

> Plan generado 2026-06-10. Input: CLAUDE.md, SESSION-LOG.md, docs/04-ROADMAP.md, docs/05-DISEÑO.md, auditoría de build.gradle y activity_main.xml.

## Objetivo del sprint

Eliminar de raíz el conflicto visual entre Material3 y nuestros custom drawables reemplazando **toda la capa de UI** (activity_main.xml + sus layouts hijos) por Jetpack Compose. Compose da control total de Canvas: las barras, gradientes y efectos CRT quedan en código Kotlin puro, sin que ningún theme del sistema los sobreescriba.

Al terminar el sprint la app debe: (1) compilar con Compose habilitado, (2) mostrar la pantalla principal fiel al mockup CRT verde/ámbar, (3) tener el PTT y las alertas conectados al RadioAudioService existente, y (4) mostrar el mapa OSMDroid del grupo en la pestaña de mapa.

## Capas que NO se tocan

| Archivo | Motivo |
|---|---|
| `radio/RadioAudioService.java` | Capa de audio/radio — intocable en este sprint |
| `radio/Protocol.java` | Protocolo serie con el ESP32 — intocable |
| `radio/TxWhitelist.java` | Seguridad regulatoria — intocable |
| `data/ArgentinaChannels.java` | Datos de canales validados en Sprint 1 — no regresión |
| `ui/MainViewModel.java` | Ya funciona; lo observamos con `observeAsState()` |
| `ui/MemoriesAdapter.java` | Queda vivo hasta Fase B; se lo descarta cuando el ChannelRow Compose lo reemplaza |

## ADR asociado

ADR-008 en `docs/03-DECISIONES.md` — documenta la decisión de Compose y sus consecuencias.

## Branch

```
git checkout -b sprint/4-compose-ui
```

---

## Estado inicial conocido (hallazgos de auditoría)

### H1 — No hay plugin Kotlin en el proyecto

`KV4PHT/build.gradle` (root) solo tiene `com.android.application` y `sonarqube`. No hay `org.jetbrains.kotlin.android`. La línea `implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.22"))` en `app/build.gradle` es solo un constraint de versiones, **no activa la compilación Kotlin**.

Acción requerida: agregar el plugin en ambos build.gradle.

### H2 — Compose no está habilitado en buildFeatures

`buildFeatures { viewBinding true; dataBinding true; buildConfig true }` — falta `compose true` y `composeOptions {}`.

### H3 — Kotlin 1.8.22 es demasiado viejo para Compose moderno

El Compose Compiler 1.5.10 requiere Kotlin 1.9.22. Hay que subir el BOM.

### H4 — activity_main.xml tiene ~463 líneas

El layout es complejo pero está bien estructurado. El mapeo a componentes Compose es directo:

| XML | Componente Compose |
|---|---|
| `frequencyContainer` (LinearLayout) | `FrequencyDisplayCard` |
| `sMeter` (LinearLayout con 9 Views) | `SMeter` (Canvas, barras proporcionales) |
| `memoriesList` (RecyclerView) | `ChannelRow` (Row de 3 chips) |
| `voiceModeLineHolder` | `GroupSelectorBar` |
| `emergencyAlertButton` (full width rojo) | parte de `AlertButtonsPanel` |
| `stopAlertButton` + `regroupAlertButton` | parte de `AlertButtonsPanel` |
| `pttButton` (circle 90dp) | `PttButton` (Canvas radial) |
| `textModeContainer` (APRS chat) | `ChatPanel` (Fase B, AndroidView fallback) |
| `bottomNavigationView` | `NavigationBar` (Compose) |

### H5 — OSMDroid no está en dependencies

`org.osmdroid:osmdroid-android` no está en `app/build.gradle`. Se agrega en Fase A junto con todo lo demás de una sola pasada.

---

## Fases de ejecución

---

### FASE A — Infraestructura Gradle + UI estática [~3-4h]

Objetivo: app compila con Compose, `MainActivity.kt` setea el contenido con estados hardcodeados (sin binding al servicio), preview funcional en Android Studio.

#### A0 · Smoke test antes de empezar

```bash
cd KV4PHT && ./gradlew test
```

Si alguno de los 59 tests falla antes de tocar nada → resolver primero. No arrancar sobre rojo.

#### A1 · Renombrar MainActivity.java → MainActivityLegacy.java

```bash
git mv app/src/main/java/com/vagell/kv4pht/ui/MainActivity.java \
        app/src/main/java/com/vagell/kv4pht/ui/MainActivityLegacy.java
```

Cambiar la declaración de clase: `public class MainActivityLegacy extends ...`

Actualizar `AndroidManifest.xml`:
```xml
<!-- temporalmente apunta a Legacy mientras construimos la nueva -->
<activity android:name=".ui.MainActivityLegacy" ...>
```

Commit: `refactor: rename MainActivity → MainActivityLegacy (ref sprint/4)`

#### A2 · Gradle: activar Kotlin + Compose + OSMDroid

**`KV4PHT/build.gradle` (root) — agregar Kotlin plugin:**
```groovy
plugins {
    id 'com.android.application' version '8.13.2' apply false
    id 'com.android.library'     version '8.13.2' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.22' apply false  // ← NUEVO
    id "org.sonarqube" version "5.1.0.4882"
}
```

**`KV4PHT/app/build.gradle` — diff completo relevante:**
```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'              // ← NUEVO
}

android {
    // ... existente sin cambios hasta buildTypes ...

    kotlinOptions {                                 // ← NUEVO bloque
        jvmTarget = '1.8'
    }

    buildFeatures {
        viewBinding  true
        dataBinding  true
        buildConfig  true
        compose      true                          // ← NUEVO
    }

    composeOptions {                               // ← NUEVO bloque
        kotlinCompilerExtensionVersion '1.5.10'
    }
}

dependencies {
    // ... deps existentes sin tocar ...

    // Actualizar kotlin-bom de 1.8.22 a 1.9.22
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.22"))

    // ── Compose BOM (nuevo) ──
    def composeBom = platform('androidx.compose:compose-bom:2024.02.02')
    implementation composeBom
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-graphics'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.activity:activity-compose:1.8.2'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-compose:2.7.0'
    debugImplementation 'androidx.compose.ui:ui-tooling'

    // ── OSMDroid (nuevo, Fase C) ──
    implementation 'org.osmdroid:osmdroid-android:6.1.18'
}
```

Test de smoke tras el cambio:
```bash
./gradlew assembleDebug
```

Si hay conflictos de versión entre `activity:activity:1.10.1` y `activity-compose:1.8.2`, usar el más nuevo explícitamente.

Commit: `build: Kotlin plugin + Compose BOM + OSMDroid deps`

#### A3 · Sistema de theme

Crear `ui/compose/theme/`:

**`Color.kt`** — paleta exacta de docs/05-DISEÑO.md:
```kotlin
// Theme ÁMBAR (defecto)
val AmberBackground     = Color(0xFF050505)
val AmberDisplay        = Color(0xFF0E0904)
val AmberSurface        = Color(0xFF1A1208)
val AmberBorderSubtle   = Color(0xFF3A2807)
val AmberBorderActive   = Color(0xFFEF9F27)
val AmberTextPrimary    = Color(0xFFFAC775)
val AmberTextSecondary  = Color(0xFFBA7517)
val AmberTextDisabled   = Color(0xFF854F0B)
val AmberTextGhost      = Color(0xFF633806)
val AmberAccent         = Color(0xFFEF9F27)

// Theme VERDE FÓSFORO
val GreenBackground    = Color(0xFF050505)
val GreenDisplay       = Color(0xFF040C04)
val GreenSurface       = Color(0xFF0C1408)
val GreenBorderSubtle  = Color(0xFF1F3A1F)
val GreenBorderActive  = Color(0xFF4FBD3B)
val GreenTextPrimary   = Color(0xFF8FE875)
val GreenTextSecondary = Color(0xFF4FBD3B)
val GreenTextDisabled  = Color(0xFF1F5511)
val GreenAccent        = Color(0xFF4FBD3B)

// Emergencia — igual en ambos themes
val EmergencyBackground = Color(0xFF501313)
val EmergencyBorder     = Color(0xFFE24B4A)
val EmergencyText       = Color(0xFFF09595)
```

**`Type.kt`** — Share Tech Mono + fallback system sans:
```kotlin
val ShareTechMono = FontFamily(
    Font(R.font.share_tech_mono, FontWeight.Normal)
)

val MotoRFARTypography = Typography(
    displayLarge = TextStyle(fontFamily = ShareTechMono, fontSize = 56.sp, fontWeight = FontWeight.Bold),
    titleMedium  = TextStyle(fontFamily = ShareTechMono, fontSize = 14.sp, letterSpacing = 0.2.sp),
    bodySmall    = TextStyle(fontFamily = ShareTechMono, fontSize = 9.sp,  letterSpacing = 0.15.sp),
    // ... resto de estilos
)
```

**`AppTheme.kt`** — enum + data class de colores:
```kotlin
enum class AppTheme { AMBER, GREEN }

data class MotoRFARColors(
    val background: Color,
    val display: Color,
    val surface: Color,
    val borderSubtle: Color,
    val borderActive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val accent: Color
)

fun appColors(theme: AppTheme): MotoRFARColors = when (theme) {
    AppTheme.AMBER -> MotoRFARColors(AmberBackground, AmberDisplay, ...)
    AppTheme.GREEN -> MotoRFARColors(GreenBackground, GreenDisplay, ...)
}
```

**`MotoRFARTheme.kt`** — provider con CompositionLocal:
```kotlin
val LocalMotoRFARColors = staticCompositionLocalOf { appColors(AppTheme.GREEN) }

@Composable
fun MotoRFARTheme(
    theme: AppTheme = AppTheme.GREEN,
    content: @Composable () -> Unit
) {
    val colors = appColors(theme)
    CompositionLocalProvider(LocalMotoRFARColors provides colors) {
        // MaterialTheme con colores mapeados para Scaffold/dialogs,
        // pero nuestros componentes leen de LocalMotoRFARColors directamente
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = colors.background,
                surface = colors.surface,
                primary = colors.accent
            ),
            typography = MotoRFARTypography,
            content = content
        )
    }
}
```

#### A4 · State y actions (contratos del UI)

Crear `ui/compose/state/`:

**`MainUiState.kt`:**
```kotlin
data class MainUiState(
    val activeFrequency: String      = "139.970",
    val activeChannelName: String    = "PRINCIPAL",
    val channels: List<ChannelMemory> = emptyList(),
    val sMeterLevel: Int             = 0,       // 0–9
    val isTxActive: Boolean          = false,
    val isRxActive: Boolean          = false,
    val isConnected: Boolean         = false,
    val theme: AppTheme              = AppTheme.GREEN
) {
    companion object {
        /** Estado de preview — sin datos reales */
        fun preview() = MainUiState(
            activeFrequency   = "139.970",
            activeChannelName = "PRINCIPAL",
            sMeterLevel       = 4,
            isConnected       = false
        )
    }
}
```

**`MainUiAction.kt`:**
```kotlin
sealed class MainUiAction {
    object PttPressed                                : MainUiAction()
    object PttReleased                               : MainUiAction()
    data class ChannelSelected(val freq: String)     : MainUiAction()
    object EmergencyAlert                            : MainUiAction()
    object StopAlert                                 : MainUiAction()
    object RegroupAlert                              : MainUiAction()
}
```

#### A5 · Componentes Compose

Crear `ui/compose/components/`:

**`FrequencyDisplayCard.kt`** — panel superior con frecuencia + S-meter:
- Container con `border(1.dp, colors.borderSubtle)` y fondo `colors.display`
- Fila superior: nombre canal en 9sp
- Centro: frecuencia en 56sp Share Tech Mono con `color = colors.textPrimary`
- Subtítulo: "MHz · FM · SIMPLEX" en 9sp ghost
- `SMeter` (sub-componente): Canvas con 9 barras en escalonado, color `colors.accent`,
  `alpha` 0.3f para las inactivas, 1.0f para las activas según `sMeterLevel`

**`ChannelRow.kt`** — selector de los 3 canales:
```kotlin
// Row de 3 chips iguales (reemplaza RecyclerView + MemoriesAdapter)
@Composable
fun ChannelRow(
    channels: List<ChannelMemory>,
    activeFreq: String,
    onChannelClick: (String) -> Unit
)
```
Cada chip: borde `borderActive` si activo, `borderSubtle` si no. Fondo
`colors.surface`. Nombre en ShareTechMono 13sp. Borde rojo
`EmergencyBorder` si el canal es EMERGENCIA (140.970).

**`PttButton.kt`** — círculo PTT con Canvas:
```kotlin
@Composable
fun PttButton(
    isTransmitting: Boolean,
    enabled: Boolean,
    onPttDown: () -> Unit,
    onPttUp: () -> Unit
)
```
Canvas drawCircle con radial gradient: centro `colors.accent.copy(alpha=0.9f)`,
borde exterior oscuro. Icono micrófono (Tabler Icons via vector) centrado.
`pointerInput` detecta `onPress` / `tryAwaitRelease` — NO un onClick normal
(PTT es hold).

**`AlertButtonsPanel.kt`:**
```kotlin
// EMERGENCIA full-width arriba (rojo), DETENCIÓN + REAGRUPAR en Row abajo
@Composable
fun AlertButtonsPanel(
    onEmergency: () -> Unit,
    onStop: () -> Unit,
    onRegroup: () -> Unit
)
```
EMERGENCIA: `background = EmergencyBackground`, `border = EmergencyBorder`,
texto blanco bold 14sp. Los otros dos: amber/green según theme.

**`AppStatusBar.kt`** — barra superior mínima:
- Hora (HH:mm) a izquierda
- "VHF · SIMPLEX" a derecha
- LED circular: verde pulsante si RX, rojo si TX, gris si idle
- 12sp ShareTechMono, todo en `colors.textSecondary`

#### A6 · MainScreen.kt

```kotlin
@Composable
fun MainScreen(
    state: MainUiState,
    onAction: (MainUiAction) -> Unit
) {
    val colors = LocalMotoRFARColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        AppStatusBar(isTx = state.isTxActive, isRx = state.isRxActive)
        FrequencyDisplayCard(
            frequency     = state.activeFrequency,
            channelName   = state.activeChannelName,
            sMeterLevel   = state.sMeterLevel
        )
        Spacer(Modifier.height(8.dp))
        GroupSelectorBar()  // simple row estática por ahora
        Spacer(Modifier.height(4.dp))
        ChannelRow(
            channels    = state.channels,
            activeFreq  = state.activeFrequency,
            onChannelClick = { freq -> onAction(MainUiAction.ChannelSelected(freq)) }
        )
        Spacer(Modifier.weight(1f))
        AlertButtonsPanel(
            onEmergency = { onAction(MainUiAction.EmergencyAlert) },
            onStop      = { onAction(MainUiAction.StopAlert) },
            onRegroup   = { onAction(MainUiAction.RegroupAlert) }
        )
        Spacer(Modifier.height(12.dp))
        PttButton(
            isTransmitting = state.isTxActive,
            enabled        = state.isConnected,
            onPttDown      = { onAction(MainUiAction.PttPressed) },
            onPttUp        = { onAction(MainUiAction.PttReleased) }
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreviewGreen() {
    MotoRFARTheme(AppTheme.GREEN) {
        MainScreen(MainUiState.preview()) {}
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreviewAmber() {
    MotoRFARTheme(AppTheme.AMBER) {
        MainScreen(MainUiState.preview()) {}
    }
}
```

#### A7 · MainActivity.kt (entry point nuevo)

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Fase A: estado hardcodeado para verificar look
            MotoRFARTheme(AppTheme.GREEN) {
                MainScreen(
                    state    = MainUiState.preview(),
                    onAction = { /* Fase B conecta aquí */ }
                )
            }
        }
    }
}
```

Actualizar `AndroidManifest.xml` para que el launcher apunte a `MainActivity` (Kotlin).

#### Tests Fase A

- `MainScreenTest.kt` — `composeTestRule.setContent { MainScreen(...) }` y verifica que los textos clave se muestran ("PRINCIPAL", "139.970", "PTT", "EMERGENCIA")
- `ColorContrastTest.kt` — verifica ratio de contraste entre `textPrimary` y `background` >= 4.5:1 para WCAG AA (necesario para legibilidad con sol)

Commit: `feat(A): Compose UI shell — static MainScreen + theme system`

---

### FASE B — Conectar al RadioAudioService [~3-4h]

Objetivo: la pantalla Compose refleja el estado real del radio. PTT transmite, canales cambian, S-meter se mueve, alertas se envían.

#### B1 · Observar MainViewModel desde MainActivity.kt

`MainViewModel.java` tiene `MutableLiveData<>`. En Kotlin:

```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var radioService: RadioAudioService? = null

    override fun onCreate(...) {
        super.onCreate(savedInstanceState)
        setContent {
            val channelMemories by viewModel.channelMemories.observeAsState(emptyList())
            val uiState = buildUiState(channelMemories, radioService)
            MotoRFARTheme(AppTheme.GREEN) {
                MainScreen(state = uiState, onAction = ::handleAction)
            }
        }
    }
}
```

#### B2 · ServiceConnection en Kotlin

Replicar el binding de `MainActivityLegacy.java`:
```kotlin
private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        radioService = (binder as RadioAudioService.LocalBinder).service
        radioService?.setCallbacks(serviceCallbacks)
    }
    override fun onServiceDisconnected(name: ComponentName?) {
        radioService = null
    }
}

override fun onStart() {
    super.onStart()
    bindService(Intent(this, RadioAudioService::class.java),
        serviceConnection, Context.BIND_AUTO_CREATE)
}

override fun onStop() {
    super.onStop()
    unbindService(serviceConnection)
}
```

#### B3 · Callbacks del servicio → StateFlow

Crear `radioUiState: MutableStateFlow<MainUiState>` en MainActivity.
Los callbacks del servicio actualizan el flow:

```kotlin
private val serviceCallbacks = object : RadioAudioService.Callbacks {
    override fun onSMeterUpdate(level: Int) {
        _radioUiState.update { it.copy(sMeterLevel = level) }
    }
    override fun onTxStateChanged(active: Boolean) {
        _radioUiState.update { it.copy(isTxActive = active) }
    }
    override fun onRxStateChanged(active: Boolean) {
        _radioUiState.update { it.copy(isRxActive = active) }
    }
}
```

En `setContent {}` usar `collectAsStateWithLifecycle()` para observar el flow.

#### B4 · handleAction — despacho de eventos UI

```kotlin
private fun handleAction(action: MainUiAction) {
    when (action) {
        MainUiAction.PttPressed     -> radioService?.startPtt()
        MainUiAction.PttReleased    -> radioService?.stopPtt()
        is MainUiAction.ChannelSelected -> tuneToChannel(action.freq)
        MainUiAction.EmergencyAlert -> showEmergencyConfirmDialog()
        MainUiAction.StopAlert      -> radioService?.sendAlert(AlertType.STOP, ...)
        MainUiAction.RegroupAlert   -> radioService?.sendAlert(AlertType.REGROUP, ...)
    }
}
```

El diálogo de confirmación de EMERGENCIA es un `AlertDialog` Compose dentro del setContent:
```kotlin
if (showEmergencyDialog) {
    AlertDialog(
        onDismissRequest = { showEmergencyDialog = false },
        title = { Text("⚠ EMERGENCIA", color = EmergencyText) },
        text  = { Text("Se transmitirá en 140.970 MHz. ¿Confirmar?") },
        confirmButton = {
            TextButton(onClick = {
                radioService?.sendAlert(AlertType.EMERGENCY, ...)
                showEmergencyDialog = false
            }) { Text("TRANSMITIR", color = EmergencyText) }
        },
        dismissButton = {
            TextButton(onClick = { showEmergencyDialog = false }) { Text("CANCELAR") }
        }
    )
}
```

#### B5 · Canal switching con sincronización al Service

```kotlin
private fun tuneToChannel(freq: String) {
    radioService?.tuneToFrequency(freq)
    _radioUiState.update { it.copy(activeFrequency = freq) }
}
```

#### B6 · Reemplazar MemoriesAdapter

Una vez que `ChannelRow` en Compose recibe la lista real de canales del ViewModel
y llama `handleAction(ChannelSelected(...))`, el `MemoriesAdapter` ya no se usa
en la pantalla principal. Dejar el archivo (`MemoriesAdapter.java`) intacto por ahora;
se depreca en el commit de cierre de Fase B.

#### Tests Fase B

- `ServiceBindingTest.kt` — usando `ServiceTestRule`, verifica que el bind llama a `setCallbacks`
- `PttActionTest.kt` — mock del service, press PTT → verifica `startPtt()` llamado
- `ChannelSwitchTest.kt` — click en chip "Alternativo" → verifica `tuneToFrequency("138.510")` llamado
- `SMeterUpdateTest.kt` — `onSMeterUpdate(7)` → verifica que `sMeterLevel == 7` en el state

Commit: `feat(B): Compose UI binding completo — PTT + alertas + canal switch`

---

### FASE C — OSMDroid mapa del grupo [~2-3h]

Objetivo: pestaña de mapa funcional con marcadores del grupo.

#### C1 · MapScreen.kt

```kotlin
@Composable
fun MapScreen(groupMembers: List<GroupMember>) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            Configuration.getInstance().load(ctx,
                PreferenceManager.getDefaultSharedPreferences(ctx))
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(14.0)
                setMultiTouchControls(true)
                setBuiltInZoomControls(false)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            groupMembers.forEach { member ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(member.lat, member.lon)
                    title    = member.alias
                    // Icono custom: dot verde/ámbar según theme
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    )
}
```

#### C2 · Permiso de almacenamiento para tiles offline

En `AndroidManifest.xml` verificar que `WRITE_EXTERNAL_STORAGE` (API < 29) esté presente
o que para API 29+ se use `osmdroid` con scoped storage correctamente configurado.

Carpeta de cache tiles:
```kotlin
Configuration.getInstance().osmdroidTileCache =
    File(context.cacheDir, "osm_tiles")
```

#### C3 · Navegación bottom bar

Reemplazar `BottomNavigationView` XML por `NavigationBar` Compose con 2 destinos:
- **PTT** (icono micrófono) → `MainScreen`
- **MAPA** (icono map-pin) → `MapScreen`

Usar Compose Navigation (`NavHost`) con 2 rutas:
```kotlin
NavHost(navController, startDestination = "main") {
    composable("main") {
        MainScreen(state = uiState, onAction = ::handleAction)
    }
    composable("map") {
        MapScreen(groupMembers = groupMembers)
    }
}
```

#### C4 · GroupMember data class

```kotlin
data class GroupMember(
    val alias: String,
    val lat: Double,
    val lon: Double,
    val distanceM: Int,
    val bearing: Float,
    val lastSeenMs: Long
)
```

Los miembros del grupo se pueblan desde los beacons GPS que llegan por radio
(evento `onGpsBeaconReceived` del servicio, Fase B ya lo captura).

#### Tests Fase C

- `MapScreenTest.kt` — `AndroidView` se crea sin crash, `MapView` instanciado correctamente
- `GroupMemberMarkerTest.kt` — lista de 2 miembros → 2 overlays en el MapView
- `NavTest.kt` — click en tab MAPA → navega a MapScreen

Commit: `feat(C): OSMDroid mapa del grupo con Compose Navigation`

---

## Definición de "hecho" del Sprint 4

- [ ] `./gradlew test` pasa todos los tests (59 previos + tests nuevos de este sprint)
- [ ] `./gradlew assembleDebug` sin warnings de deprecación Compose
- [ ] App corre en emulador API 33: pantalla principal fiel al mockup CRT
  - S-meter en verde #4FBD3B (sin override de Material3)
  - Canal activo con borde verde visible al iniciar
  - Canal EMERGENCIA con borde rojo
  - PTT grande centrado con gradiente radial
  - Botones de alerta con estilos diferenciados
- [ ] PTT funciona: hold → TX al servicio, release → RX
- [ ] Cambio de canal funciona y sintoniza la frecuencia correcta
- [ ] EMERGENCIA muestra diálogo de confirmación antes de transmitir
- [ ] Pestaña MAPA muestra OSMDroid con tiles de internet
- [ ] Bottom nav navega entre PTT y MAPA
- [ ] `MainActivityLegacy.java` marcado `@Deprecated` (eliminación en Sprint 5)
- [ ] ADR-008 escrito y commiteado
- [ ] `docs/SESSION-LOG.md` actualizado al cierre

---

## Notas para la sesión de Claude Code

1. **Arrancar siempre por A0** — no tocar Gradle hasta confirmar que los 59 tests pasan.
2. **Fase A es la más arriesgada** — Gradle puede tener conflictos de versiones entre AGP 8.13.2 y el Kotlin plugin. Si `assembleDebug` no pasa, revisar compatibilidad en el [mapa oficial](https://developer.android.com/jetpack/androidx/releases/compose-kotlin).
3. **No usar `@Stable` ni optimizaciones prematuras de Compose** — el recomposición es el default, optimizar después.
4. **Canvas radial en PttButton**: usar `Brush.radialGradient` de Compose, no un drawable XML. Ese es exactamente el punto de la migración.
5. **Scan lines CRT**: implementar con `Canvas.drawRect` en loop con `alpha=0.05f` — efecto visual que en XML era imposible de hacer sin pelear con Material3.
6. **Si el emulador no tiene GPU**: ejecutar en dispositivo físico o activar SwiftShader en AVD Manager.
7. **OSMDroid + Compose**: el `AndroidView` es la única manera — OSMDroid no tiene binding Compose propio. El `update` lambda se llama en cada recomposición que cambie `groupMembers`.
