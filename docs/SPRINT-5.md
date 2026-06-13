# SPRINT 5 — GPS + Identidad + Sonidos

> Plan generado 2026-06-10. Input: SESSION-LOG.md (estado post Sprint 4), ROADMAP.md.

## Objetivo del sprint

Al terminar, el mapa tiene datos reales: cada moto transmite su posición periódicamente,
el grupo se ve en `MapScreen`, y la app suena como una radio táctica (click de PTT,
beeps de alerta). El alias del usuario hace que los paquetes GPS sean identificables.

## ¿Por qué este conjunto?

Las balizas GPS son el corazón del valor diferencial de MotoRFAR (saber dónde está el grupo
sin cobertura celular). Pero para transmitir una baliza hace falta:
1. **Alias** del usuario → va en el packet GPS como identificador
2. **GPS del teléfono** → lat/lon a transmitir
3. **Encode APRS** → ya existe en el código base (kv4p HT lo tiene)
4. **TX en 139.970** → ya funciona (TxWhitelist validado en Sprint 1)

Los **sonidos** son costo bajo, impacto alto en la experiencia táctica: un click de PTT
y un beep de alerta distinguen MotoRFAR de cualquier app genérica.

## Capas que NO se tocan

- `RadioAudioService.java` — excepto agregar `startGpsBeacon()` / `stopGpsBeacon()`
  siguiendo el patrón beacon APRS existente (líneas 1553-1567)
- `Protocol.java` — el encode/decode APRS ya está; solo consumimos
- `TxWhitelist.java` — intocable
- `ArgentinaChannels.java` — intocable
- Todo el Compose UI del Sprint 4 — solo agregamos pantalla de Settings

## Estado inicial conocido

| Feature | Estado |
|---|---|
| `RadioAudioService` — beacon APRS | ✅ existe (aprsBeaconFrequency, aprsBeaconInterval) |
| `Protocol.java` — encode position | ✅ existe (APRSUtils o similar en kv4p HT) |
| `ToneHelper.java` | ✅ existe pero sin implementación de sonidos nuestros |
| `AppSetting.java` — Room entity | ✅ existe (usada para settings de TX freq) |
| `SettingsActivity.java` | ✅ existe como XML Activity |
| `GroupMember.kt` | ✅ creado en Sprint 4, sin datos reales |
| `MapScreen.kt` | ✅ creado en Sprint 4, marcadores estáticos |
| `FusedLocationProvider` | ❌ no implementado |

---

## Fases de ejecución

---

### FASE A — Alias del usuario [~1-2h]

El alias viaja en cada paquete GPS como identificador de la moto en el grupo.
Máximo 6 caracteres, alfanumérico, sin espacios (restricción APRS).

**A1. Test primero** — `UserAliasTest.kt`:
- `AliasValidator.isValid("LUKAZ")` → true
- `AliasValidator.isValid("lu kaz")` → false (espacio)
- `AliasValidator.isValid("TOOLONG")` → false (>6 chars)
- `AliasValidator.isValid("")` → false
- `AliasValidator.isValid("MO77")` → true

**A2. Implementar** `AliasValidator.kt`:
```kotlin
object AliasValidator {
    private val REGEX = Regex("^[A-Z0-9]{1,6}$")
    fun isValid(alias: String) = REGEX.matches(alias.uppercase())
    fun sanitize(raw: String) = raw.uppercase().filter { it.isLetterOrDigit() }.take(6)
}
```

**A3. Persistir en AppSetting** (Room ya existente):
- Constante `SETTING_USER_ALIAS = "user_alias"`
- Default: `"MOTO"` + últimos 2 dígitos del `Build.SERIAL` hash si disponible

**A4. AliasSettingScreen.kt** — composable simple:
```
┌─────────────────────────────┐
│  TU ALIAS EN LA RED         │
│  ┌──────────────────────┐   │
│  │  LUKAZ               │   │  ← TextField, max 6, mayúsculas automáticas
│  └──────────────────────┘   │
│  "Identificás tu moto en    │
│   el mapa del grupo"        │
└─────────────────────────────┘
```
- Input auto-uppercase + auto-sanitize mientras tipea
- Validación en tiempo real: borde rojo si inválido
- Botón GUARDAR solo activo si alias válido
- Integrar en `SettingsActivity.java` como fragment Compose

**Tests Fase A:**
- `UserAliasTest.kt` (los 5 de arriba)
- `AliasSettingScreenTest.kt` — input "lu kaz" → muestra error; "LUKAZ" → botón activo

Commit: `feat(A): alias de usuario — validación + persistencia + UI`

---

### FASE B — Balizas GPS periódicas + mapa real [~3-4h]

El alias existe. Ahora cada moto transmite su posición por radio y el mapa
muestra al grupo en tiempo real.

**B1. Permiso GPS** — en `AndroidManifest.xml` verificar:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```
Solicitar en runtime desde `MainActivity.kt` antes de iniciar balizas.

**B2. GpsBeaconManager.kt** — clase nueva (no toca el servicio directamente):
```kotlin
class GpsBeaconManager(
    private val context: Context,
    private val radioService: RadioAudioService,
    private val alias: String,
    private val intervalMs: Long = 60_000L   // default 1 min
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch {
            val client = LocationServices.getFusedLocationProviderClient(context)
            while (isActive) {
                val location = client.awaitLastLocation()
                if (location != null) {
                    radioService.sendGpsBeacon(alias, location.latitude, location.longitude)
                }
                delay(intervalMs)
            }
        }
    }

    fun stop() { job?.cancel() }
}
```

**B3. `RadioAudioService.sendGpsBeacon()`** — nuevo método que reutiliza
el patrón APRS beacon existente (líneas 1553-1567):
```java
public void sendGpsBeacon(String alias, double lat, double lon) {
    // Mismo patrón que aprsBeaconFrequency: tune → TX → restore
    // Usa Protocol.encodeAprsPosition(alias, lat, lon)
    // Transmite en frecuencia activa (139.970 normalmente)
}
```

**B4. Decode de beacons recibidos → GroupMember**

Cuando el servicio recibe un paquete APRS de posición de otro usuario:
```kotlin
// En el callback onAprsMessageReceived de MainActivity.kt:
fun onAprsMessageReceived(message: APRSMessage) {
    if (message.type == APRSMessageType.POSITION) {
        val member = GroupMember(
            alias      = message.sourceCallsign,
            lat        = message.latitude,
            lon        = message.longitude,
            distanceM  = calculateDistance(myLocation, message),
            bearing    = calculateBearing(myLocation, message),
            lastSeenMs = System.currentTimeMillis()
        )
        _groupMembers.update { list -> list.upsert(member) }
    }
}
```

**B5. MapScreen conectado a datos reales**

`MapScreen.kt` ya recibe `List<GroupMember>` — solo hay que pasarle
el `StateFlow` real desde `MainActivity.kt`:
```kotlin
val groupMembers by groupMembersFlow.collectAsStateWithLifecycle()
MapScreen(groupMembers = groupMembers)
```

Marcador propio: icono distinto (color diferente o "YO" como label).
Marcadores del grupo: alias como label, desvanecen si `lastSeenMs > 5min`.

**B6. Intervalo configurable en settings** — slider 30s / 1min / 2min / 5min:
```kotlin
// En AliasSettingScreen o una pantalla BeaconSettingsScreen
Slider(
    value = beaconIntervalIndex.toFloat(),
    onValueChange = { ... },
    steps = 3,   // 30s, 1min, 2min, 5min
    valueRange = 0f..3f
)
```

**Tests Fase B:**
- `GpsBeaconManagerTest.kt` — mock del servicio, verifica que `sendGpsBeacon` se llama cada `intervalMs`
- `GroupMemberUpsertTest.kt` — recibir 2 beacons del mismo alias → lista tiene 1 miembro actualizado
- `GroupMemberDecayTest.kt` — miembro con `lastSeenMs` hace 6min → `isStale == true`

Commit: `feat(B): balizas GPS periódicas + mapa del grupo real`

---

### FASE C — Sonidos tácticos [~1-2h]

`ToneHelper.java` existe pero los sonidos de MotoRFAR no están implementados.
Todos se sintetizan con `AudioTrack` (cero assets externos, como dice docs/05-DISEÑO.md).

**C1. Implementar en `ToneHelper.java`:**

```java
// Click PTT pressed: square wave 800Hz, 30ms, fade out
public static void playPttDown(float volume) { ... }

// Click PTT released: square wave 600Hz, 30ms, fade out
public static void playPttUp(float volume) { ... }

// Alert STOP/REGROUP: double beep 1200Hz, 100ms cada uno, gap 50ms
public static void playAlertBeep(float volume) { ... }

// Alert EMERGENCY: triple beep ascendente 800/1000/1200Hz, 150ms cada uno
public static void playEmergencyBeep(float volume) { ... }

// Static burst (alguien empieza a TX): white noise 80ms, bajo volumen
public static void playStaticBurst(float volume) { ... }
```

**C2. Conectar en `MainActivity.kt`:**
- `PttButton` `onPttDown` → `ToneHelper.playPttDown(volume)`
- `PttButton` `onPttUp` → `ToneHelper.playPttUp(volume)`
- `handleAction(EmergencyAlert)` → `ToneHelper.playEmergencyBeep(volume)`
- `handleAction(StopAlert)` → `ToneHelper.playAlertBeep(volume)`
- `handleAction(RegroupAlert)` → `ToneHelper.playAlertBeep(volume)`
- callback `onRxStarted` del servicio → `ToneHelper.playStaticBurst(volume)`

**C3. Volumen desde settings** — leer `SETTING_ALERT_VOLUME` de `AppSetting`
(0-100, default 70 según docs/05-DISEÑO.md).

**Tests Fase C:**
- `ToneHelperTest.kt` — verifica que `playPttDown` no lanza excepción y retorna en < 100ms
- `SoundSettingsTest.kt` — volumen 0 → sin output (no crash)

Commit: `feat(C): sonidos tácticos — PTT click + beeps de alerta + static burst`

---

## Definición de "hecho" del Sprint 5

- [ ] `./gradlew test` pasa todos los tests (75 previos + tests nuevos)
- [ ] Alias se guarda en Room y sobrevive restart de la app
- [ ] Baliza GPS se transmite cada 60s (verificado en logcat con lat/lon reales)
- [ ] Marcador del propio usuario aparece en MapScreen
- [ ] Al recibir un beacon de otro dispositivo → marcador aparece en el mapa
- [ ] PTT hace click al presionar y soltar
- [ ] Alerta EMERGENCIA hace triple beep ascendente
- [ ] `assembleDebug` SUCCESSFUL
- [ ] SESSION-LOG.md actualizado al cierre

---

## Notas para la sesión de Claude Code

1. **GPS en emulador**: usar `Extended Controls → Location` en AVD Manager para
   inyectar coordenadas GPS. Simular dos dispositivos con coordenadas distintas
   para verificar que el mapa muestra dos marcadores.
2. **`sendGpsBeacon` sin hardware**: si el SA818-V no está conectado, el servicio
   debe fallar silenciosamente (no crashear). Verificar el manejo de `radioService == null`.
3. **`upsert` en StateFlow**: la lista de `GroupMember` necesita actualizar
   el miembro existente si el alias ya está (no duplicar). Implementar como
   `list.filter { it.alias != member.alias } + member`.
4. **AudioTrack en tests**: mockear `ToneHelper` en tests de UI para no bloquear
   el hilo de audio en el runner de tests.
5. **Permiso GPS en runtime**: `ActivityResultContracts.RequestMultiplePermissions`
   desde `MainActivity.kt`. Si el usuario niega, el botón de baliza queda deshabilitado
   con un tooltip explicativo — no crashear.
