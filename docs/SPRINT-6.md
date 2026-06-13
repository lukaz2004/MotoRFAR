# SPRINT 6 — Canales nombrados + Alertas grupales

> Plan generado 2026-06-10. Input: SESSION-LOG.md (estado post Sprint 5), ROADMAP.md.

## Objetivo del sprint

Al terminar, la app es operable en una salida real: los tres canales VHF tienen nombres
(Grupo / Alternativo / Emergencia), el grupo puede enviarse alertas táctiles con GPS
adjunto (STOP, REGROUP, EMERGENCY), y la deuda técnica del Sprint 5 queda cerrada
(beaconInterval hot-reload + tests instrumented).

## ¿Por qué este conjunto?

Los canales nombrados son el requisito más básico de UX táctica: un motociclista
no puede buscar "139.970" mientras maneja. Necesita "GRUPO". Las alertas son el
segundo pilar del valor diferencial de MotoRFAR: comunicar una detención de emergencia
sin hablar. Ambas features son funcionalidad pura sin estética.

La deuda de beaconInterval es un bug silencioso: el usuario cambia el intervalo
en settings y no pasa nada hasta reiniciar la app.

## Capas que NO se tocan

- `Protocol.java` — el encode/decode APRS ya está; solo consumimos
- `TxWhitelist.java` — intocable
- `ToneHelper.java` — ya funciona del Sprint 5, no agregar ni cambiar sonidos
- `GpsBeaconManager.kt` — solo el fix de hot-reload, no refactor
- Todo el Compose UI del Sprint 5 — solo agregamos/modificamos lo necesario

## Estado inicial conocido (post Sprint 5)

| Feature | Estado |
|---|---|
| `ArgentinaChannels.java` — 3 frecuencias VHF | ✅ existe, sin nombres tácticos |
| `GpsBeaconManager.kt` — beaconIntervalSec fijo al init | ⚠️ bug: no reacciona a cambios en settings |
| `ToneHelper.java` — sonidos tácticos | ✅ completo (Sprint 5) |
| `GroupMember.kt` + `MapScreen.kt` | ✅ conectados a datos reales (Sprint 5) |
| Alertas EMERGENCY/STOP/REGROUP | ❌ no implementadas |
| Tests instrumented en emulador | ❌ pendiente (MainScreenTest.kt) |
| `RadioServiceAccessor.java` | ✅ bridge Kotlin↔Lombok completo |


---

## Fases de ejecución

---

### FASE A — Deuda técnica Sprint 5 [~1-2h]

**A1. Fix beaconInterval hot-reload**

El problema: `GpsBeaconManager` lee `beaconIntervalSec` al hacer `start()`.
Si el usuario cambia el valor en settings, el cambio no aplica hasta reiniciar.

Solución: pasar un `Flow<Long>` del intervalo al manager y reiniciar el job
interno cuando cambia el valor.

```kotlin
class GpsBeaconManager(
    ...
    private val intervalFlow: Flow<Long>   // reemplaza intervalMs: Long
) {
    fun start(scope: CoroutineScope) {
        job = scope.launch {
            intervalFlow.collectLatest { intervalMs ->
                while (isActive) {
                    val location = client.awaitLastLocation()
                    if (location != null) radioService.sendPositionBeacon(alias, location)
                    delay(intervalMs)
                }
            }
        }
    }
}
```

En `MainActivity.kt` construir el flow desde `AppSetting`:
```kotlin
val intervalFlow = settingsRepo
    .observe(SETTING_BEACON_INTERVAL)
    .map { it.toLongOrNull() ?: 60_000L }
    .distinctUntilChanged()
```

**Tests Fase A1:**
- `GpsBeaconManagerHotReloadTest.kt` — cambiar el flow de 60s a 30s → job se reinicia y usa 30s

**A2. Tests instrumented — `MainScreenTest.kt`**

Usando `composeTestRule` verificar que:
- Botón PTT existe y tiene contentDescription "PTT"
- Tab MAP existe y navega a MapScreen
- Tab CONFIG existe y navega a AliasSettingScreen

```kotlin
@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test fun pttButton_exists() {
        composeTestRule.onNodeWithContentDescription("PTT").assertIsDisplayed()
    }

    @Test fun navigation_to_map() {
        composeTestRule.onNodeWithText("MAPA").performClick()
        composeTestRule.onNodeWithTag("map_screen").assertIsDisplayed()
    }
}
```

Commit: `fix(A): beaconInterval hot-reload + tests instrumented MainScreen`

---

### FASE B — Canales nombrados [~1-2h]

Los tres canales VHF necesitan identidad táctica en la UI.

**B1. Actualizar `ArgentinaChannels.java`**

```java
public enum ArgentinaChannels {
    GRUPO      (1, "GRUPO",       138_510_000, "Canal principal del grupo"),
    ALTERNATIVO(2, "ALTERNATIVO", 139_970_000, "Canal de respaldo"),
    EMERGENCIA (3, "EMERGENCIA",  140_970_000, "Solo para emergencias críticas");

    public final int    number;
    public final String name;
    public final int    frequencyHz;
    public final String description;
}
```

**B2. UI de selección de canal**

En `MainScreen.kt` reemplazar el selector genérico por tres botones nombrados:

```
┌──────────┬──────────────┬────────────┐
│  GRUPO   │ ALTERNATIVO  │ EMERGENCIA │
│ 138.510  │   139.970    │  140.970   │
└──────────┴──────────────┴────────────┘
```
- Canal activo: borde ámbar resaltado
- Canal EMERGENCIA: texto en color de alerta (rojo ámbar) para diferenciarlo visualmente
- Al cambiar canal → llama `handleAction(ChangeChannel(channel))` → actualiza servicio

**Tests Fase B:**
- `ArgentinaChannelsTest.kt` — verificar que los 3 canales tienen frecuencia y nombre no nulos
- `ChannelSelectorTest.kt` — seleccionar ALTERNATIVO → canal activo cambia a 139.970

Commit: `feat(B): canales VHF nombrados — Grupo / Alternativo / Emergencia`


---

### FASE C — Alertas STOP y REGROUP [~2-3h]

Botones visibles en la pantalla principal. Al presionar transmiten un paquete APRS
con tipo de alerta + GPS adjunto. Al recibir una alerta, la app muestra un banner
prominente y suena.

**C1. Modelo `AlertType.kt`**

```kotlin
enum class AlertType(val label: String, val aprsSymbol: String) {
    STOP    ("STOP",    "!STOP"),
    REGROUP ("REAGRUPAR", "!RGP"),
    EMERGENCY("EMERGENCIA","!SOS")
}
```

**C2. Transmisión de alerta**

En `RadioServiceAccessor.java` agregar:
```java
public void sendAlert(AlertType type, double lat, double lon) {
    // Encode: "MOTORFAR:!SOS LAT/LON"
    // Mismo patrón que sendPositionBeacon pero con symbol de alerta
    String payload = Protocol.encodeAlert(type.getAprsSymbol(), lat, lon);
    transmitAprs(payload);
}
```

**C3. Recepción y banner**

En `MainUiState.kt`:
```kotlin
data class MainUiState(
    ...
    val activeAlert: ReceivedAlert? = null  // null = sin alerta activa
)

data class ReceivedAlert(
    val type: AlertType,
    val fromAlias: String,
    val lat: Double,
    val lon: Double,
    val receivedAt: Long = System.currentTimeMillis()
)
```

Banner en `MainScreen.kt` — aparece sobre el contenido mientras `activeAlert != null`:
```
┌─────────────────────────────────────────┐
│  ⚠ STOP — LUKAZ · hace 5s             │
│  [VER EN MAPA]              [CERRAR]   │
└─────────────────────────────────────────┘
```
Auto-dismiss a los 30 segundos.

**C4. Botones en UI principal**

Dos botones secundarios debajo del PTT:
```
   [STOP]     [REAGRUPAR]
```
Tamaño mediano, no compiten con el PTT grande central.

**Tests Fase C:**
- `AlertTypeTest.kt` — los 3 tipos tienen label y symbol no vacíos
- `AlertBannerTest.kt` — `activeAlert != null` → banner visible; `null` → banner oculto
- `AlertTransmitTest.kt` — `sendAlert(STOP, lat, lon)` → payload contiene "!STOP" y coordenadas

Commit: `feat(C): alertas STOP + REGROUP con GPS — transmisión + banner`

---

### FASE D — EMERGENCIA con confirmación [~1-2h]

La alerta de emergencia tiene consecuencias reales: activa a todo el grupo.
Necesita confirmación para prevenir activaciones accidentales.

**D1. Botón EMERGENCIA separado**

Botón rojo/ámbar destacado, separado de STOP/REGROUP.
Al presionar → abre `EmergencyConfirmDialog`.

**D2. `EmergencyConfirmDialog.kt`**

Dos opciones de UX (elegir la que Claude Code implemente más limpio):

**Opción A — Hold 2s:**
```
┌──────────────────────────────────┐
│   ⚠ EMERGENCIA                  │
│   Mantené presionado 2s         │
│   para confirmar                │
│                                  │
│   [████░░░░░░] 40%               │  ← ProgressBar animado
│                                  │
│              [CANCELAR]          │
└──────────────────────────────────┘
```

**Opción B — Slide to confirm:**
```
┌──────────────────────────────────┐
│   ⚠ EMERGENCIA                  │
│   ┌──────────────────────────┐  │
│   │ >>>  DESLIZÁ PARA        │  │  ← Slider que no vuelve
│   │      CONFIRMAR           │  │
│   └──────────────────────────┘  │
│              [CANCELAR]          │
└──────────────────────────────────┘
```

Al confirmar → `handleAction(SendAlert(AlertType.EMERGENCY))` → `sendAlert()` + `playEmergencyBeep()`

**Tests Fase D:**
- `EmergencyDialogTest.kt` — cancelar → no se llama `sendAlert`
- `EmergencyDialogTest.kt` — confirmar (hold 2s completo o slide 100%) → se llama `sendAlert(EMERGENCY)`

Commit: `feat(D): EMERGENCIA con confirmación — hold 2s o slide-to-confirm`


---

## Definición de "hecho" del Sprint 6

- [ ] `./gradlew test` pasa todos los tests (99 previos + tests nuevos)
- [ ] Cambiar beaconInterval en settings → GpsBeaconManager usa el nuevo valor sin reiniciar
- [ ] Tests instrumented MainScreenTest pasan en emulador
- [ ] UI muestra 3 canales: GRUPO / ALTERNATIVO / EMERGENCIA con sus frecuencias
- [ ] Cambiar canal activo en UI → servicio cambia frecuencia
- [ ] Botones STOP y REAGRUPAR visibles en pantalla principal
- [ ] Presionar STOP → transmite paquete APRS con GPS
- [ ] Recibir alerta → banner aparece con alias + tipo + tiempo
- [ ] Botón EMERGENCIA requiere confirmación antes de transmitir
- [ ] `assembleDebug` SUCCESSFUL
- [ ] SESSION-LOG.md actualizado al cierre

---

## Notas para la sesión de Claude Code

1. **`ArgentinaChannels.java` → enum**: actualmente puede ser una clase o lista.
   Convertir a enum es seguro si se actualiza el único punto de uso (`TxWhitelist.java`
   o donde se instancie el canal al iniciar el servicio). Verificar antes de refactorizar.

2. **GPS en alertas**: si el GPS no está disponible al enviar la alerta, transmitir
   igual sin coordenadas (el tipo de alerta sigue siendo válido). No bloquear.

3. **Banner de alerta**: usar `AnimatedVisibility` de Compose para entrada/salida suave.
   Z-order: sobre el contenido principal, bajo la TopBar.

4. **Hold 2s vs slide**: elegir según lo que quede más limpio en Compose.
   El hold 2s es más simple (`LaunchedEffect` + `delay`). El slide es más táctil
   para guantes de moto pero más código. Claude Code decide.

5. **`Protocol.encodeAlert`**: verificar si ya existe algún método de encode
   en `Protocol.java` que se pueda reutilizar antes de crear uno nuevo.
   Seguir el patrón ADR-010 (RadioServiceAccessor como bridge).

6. **Branch**: crear `sprint/6-channels-alerts` desde `main` al arrancar.
