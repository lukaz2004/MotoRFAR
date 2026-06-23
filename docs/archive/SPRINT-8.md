# SPRINT 8 — Confirmación EMERGENCIA + Animación PTT + Modo Escucha

> Plan generado 2026-06-11. Input: SESSION-LOG.md (estado post Sprint 7), ROADMAP.md.

## Objetivo del sprint

Al terminar, MotoRFAR está en **feature freeze para v1.0**:
- La EMERGENCIA requiere confirmación intencional (no se dispara por accidente)
- El PTT tiene feedback visual durante TX (anillos radiales Canvas)
- Existe un modo "Solo escucha" para riders que no necesitan transmitir
- El PR #7 mergeado y el flujo de gh CLI automatizado para futuros sprints

## ¿Por qué este conjunto?

La confirmación de EMERGENCIA es **safety-critical**: un falso positivo en ruta genera
pánico en el grupo. Sin esto, v1.0 no puede distribuirse responsablemente.
La animación PTT y el modo escucha son las últimas features de UX del ROADMAP v1.0.
Todo lo demás (PTT externo jack, OSMDroid tiles offline) queda para v1.1.

## Capas que NO se tocan

- `OnboardingHelper.kt`, `OnboardingActivity.kt`, `TermsScreen.kt` — Sprint 7, intocables
- `ArgentinaChannels.java` / `TxWhitelist.java` — intocables
- `Protocol.java` — intocable
- `GpsBeaconManager.kt` — intocable
- `RadioAudioService` / `RadioServiceAccessor` — intocable

## Estado inicial conocido (post Sprint 7)

| Feature                            | Estado                          |
|------------------------------------|---------------------------------|
| Onboarding T&C → alias → canal     | ✅ Sprint 7                     |
| Rename ar.motorfar.app             | ✅ Sprint 7                     |
| Canales GRUPO / ALT / EMERGENCIA   | ✅ Sprint 6                     |
| Alertas STOP / REGROUP / EMERGENCY | ✅ Sprint 6                     |
| beaconInterval hot-reload          | ✅ Sprint 6                     |
| Compose UI (CRT amber)             | ✅ Sprint 4                     |
| EMERGENCIA con confirmación        | ❌ pendiente (safety-critical)  |
| Animación PTT durante TX           | ❌ pendiente                    |
| Modo "Solo escucha"                | ❌ pendiente                    |
| gh CLI instalado                   | ❌ pendiente (winget)           |

---

## Fases de ejecución

---

### PRE-SPRINT — Setup gh CLI y merge PR [~10 min]

**P1. Instalar gh CLI:**
```powershell
winget install GitHub.cli
# Reiniciar terminal, luego:
gh auth login
# Elegir: GitHub.com → HTTPS → Login with a web browser
```

**P2. Merge del PR #7 (sprint/7-onboarding-package → main):**
```powershell
cd C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT
gh pr merge sprint/7-onboarding-package --squash --delete-branch
# Si el PR fue creado manualmente en GitHub, usar el número:
# gh pr merge 7 --squash
```

**P3. Crear branch Sprint 8:**
```powershell
git checkout main
git pull
git checkout -b sprint/8-confirmation-ptt-listen
```

**P4. Verificar tests base:**
```powershell
./gradlew test
# Debe pasar la suite completa (BUILD SUCCESSFUL)
```

---

### FASE A — Confirmación EMERGENCIA [~1.5h]

EMERGENCIA es el canal de último recurso. Debe ser imposible activar por error.
Implementar mecanismo de **hold 2 segundos** con feedback visual de progreso.

**A1. `EmergencyConfirmButton.kt` — nuevo composable:**
```kotlin
// app/src/main/java/ar/motorfar/app/ui/components/EmergencyConfirmButton.kt
@Composable
fun EmergencyConfirmButton(
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Arco de progreso Canvas — rojo → se completa en 2s
    // Al soltar antes de completar: cancela y resetea
    // Al completar: llama onConfirmed()
}
```

**A2. Integrar en `MainScreen.kt`:**
- Reemplazar el botón EMERGENCIA actual por `EmergencyConfirmButton`
- El `onConfirmed` dispara el flow existente de GPS beacon + alert

**A3. Tests:**
```kotlin
// EmergencyConfirmButtonTest.kt
@Test fun emergency_button_cancels_on_early_release()
@Test fun emergency_button_fires_after_hold()
```

---

### FASE B — Animación PTT (anillos radiales) [~1.5h]

Durante TX, el usuario debe saber visualmente que está transmitiendo.
Implementar anillos radiales expansivos en Canvas sobre el botón PTT existente.

**B1. `PttButton.kt` — agregar overlay de animación:**
```kotlin
// Usar InfiniteTransition con animateFloat
// Tres anillos que se expanden con alpha decreciente
// Color: CRT amber (#E07B39) con alpha 0.6 → 0.0
// Solo visible cuando isTransmitting == true
// Se detiene limpiamente cuando TX termina
```

**B2. Conectar `isTransmitting` al estado de `MainUiState`:**
- `MainUiState` ya tiene estado de TX vía `RadioServiceAccessor`
- Pasar el flag al composable `PttButton`

**B3. Tests:**
```kotlin
// MainScreenTest.kt — agregar:
@Test fun ptt_animation_visible_during_tx()
@Test fun ptt_animation_hidden_when_idle()
```

---

### FASE C — Modo "Solo escucha" [~1h]

Permite a riders que no necesitan transmitir (ej: pasajeros en auto de apoyo)
poner la radio en modo RX-only, deshabilitando el PTT y reduciendo batería.

**C1. `ListenOnlyMode.kt` — toggle en settings o UI principal:**
```kotlin
// Toggle en el TopBar o Settings
// Cuando activo:
//   - PTT queda disabled (grisado con contentDescription "PTT deshabilitado")  
//   - EMERGENCIA sigue activa (excepción de seguridad)
//   - RadioAudioService: no responde a comandos PTT
// Se persiste en SharedPreferences: "listen_only_mode"
```

**C2. Integrar en `MainUiState` + `MainUiAction`:**
```kotlin
data class MainUiState(..., val isListenOnly: Boolean = false)
sealed class MainUiAction { object ToggleListenOnly : MainUiAction() }
```

**C3. Tests:**
```kotlin
@Test fun ptt_disabled_in_listen_only_mode()
@Test fun emergency_enabled_in_listen_only_mode()
@Test fun listen_only_persists_across_restart()
```

---

### FASE D — Cierre y PR [~30 min]

**D1. Suite completa:**
```powershell
./gradlew test
# Todos los tests deben pasar (BUILD SUCCESSFUL)
```

**D2. SESSION-LOG.md — agregar entrada Sprint 8**

**D3. Commit y PR con gh CLI:**
```powershell
git add -A
git commit -m "feat(sprint-8): emergency hold confirm + PTT rings animation + listen-only mode"
gh pr create \
  --base main \
  --head sprint/8-confirmation-ptt-listen \
  --title "Sprint 8: Confirmación EMERGENCIA + Animación PTT + Modo Escucha" \
  --body-file docs/SPRINT-8.md
```

---

## Criterios de éxito

- [ ] PR #7 mergeado limpiamente antes de arrancar
- [ ] `EmergencyConfirmButton` requiere hold de 2s — no dispara con tap corto
- [ ] Anillos PTT visibles durante TX, ausentes en idle
- [ ] Modo escucha deshabilita PTT pero mantiene EMERGENCIA activa
- [ ] `./gradlew test` → BUILD SUCCESSFUL (suite completa)
- [ ] PR abierto con `gh pr create` (sin paso manual)

## Pendiente para Sprint 9 (v1.1)

- PTT externo vía jack 3.5mm (requiere audio focus + hardware test)
- OSMDroid tiles offline (descarga por zona)
- `TermsActivity.java` → extraer constantes a `AppSetting` (Finding 5 MEDIUM)
- Verificación con SA818-V hardware real conectado

---

## ✅ RESULTADO DE EJECUCIÓN (2026-06-13)

**Sprint 8 COMPLETADO.** Todas las fases ejecutadas y verificadas.

### PRE-SPRINT
- ✅ `gh` CLI 2.94.0 instalado vía winget
- ✅ Autenticación gh con token (scopes: repo, workflow, admin:org)
- ✅ PR #4 (sprint/7-onboarding-package → main) creado y **MERGEADO**
  - Incluyó cierre de artefactos S7: docs sprints 4-7, schema Room `ar.motorfar.app.data.AppDatabase/6.json`, limpieza de schemas viejos `com.vagell.*`, `.gitignore` para tooling local
- ✅ Branch `sprint/8-ux-tactical-polish` creado desde main actualizado

### FASE A — Confirmación EMERGENCIA
- ✅ `EmergencyConfirmButton.kt` — composable nuevo, hold 2s con fill de progreso Canvas (izq→der)
- ✅ Tap corto NO dispara; solo hold completo de 2s llama `onConfirmed`
- ✅ Integrado en `AlertButtonsPanel.kt` (reemplazó el Box clickable)
- ✅ `MainActivity`: `EmergencyAlert` va directo a transmit, dialog viejo (`EmergencyConfirmDialog`) removido del flujo
- ✅ Tests: `EmergencyConfirmButtonTest.kt` (8 tests del comportamiento del hold)

### FASE B — Animación PTT
- ✅ `PttButton.kt` — 3 anillos radiales expansivos con `InfiniteTransition`
- ✅ Fases escalonadas (1/3 de desfase), alpha decreciente 0.55→0, ciclo 1200ms
- ✅ Solo activos durante `isTransmitting == true`, se detienen limpio al terminar TX

### FASE C — Modo "Solo escucha"
- ✅ `SETTING_LISTEN_ONLY` en `AppSetting.java`
- ✅ `MainUiState.isListenOnly` + `MainUiAction.ToggleListenOnly`
- ✅ Toggle en TopBar (ícono `ic_headphones.xml` nuevo) + indicador `[ SOLO ESCUCHA ]`
- ✅ Guards de seguridad en `MainActivity.handleAction`:
  - PTT manual → bloqueado
  - Alertas STOP/REGROUP → bloqueadas
  - **EMERGENCIA → SIEMPRE disponible** (excepción de seguridad)
  - Beacon GPS automático → suprimido (RX-only real)
- ✅ Persistencia en Room, carga en `loadSettings`, default OFF
- ✅ Tests: `ListenOnlyModeTest.kt` (10 tests de guards + persistencia)

### FASE D — Cierre
- ✅ `./gradlew compileDebugKotlin` → **BUILD SUCCESSFUL**
- ✅ `./gradlew testDebugUnitTest` → **BUILD SUCCESSFUL**
- ✅ **151 tests, 0 fallos, 24 suites**

### Criterios de éxito — TODOS cumplidos
- [x] PR S7 mergeado limpiamente antes de arrancar
- [x] `EmergencyConfirmButton` requiere hold de 2s — no dispara con tap corto
- [x] Anillos PTT visibles durante TX, ausentes en idle
- [x] Modo escucha deshabilita PTT pero mantiene EMERGENCIA activa
- [x] `./gradlew test` → BUILD SUCCESSFUL (suite completa)
- [x] PR abierto con `gh pr create`

### Archivos tocados en S8
```
Nuevos:
  app/.../ui/compose/components/EmergencyConfirmButton.kt
  app/.../res/drawable/ic_headphones.xml
  app/src/test/.../ui/EmergencyConfirmButtonTest.kt
  app/src/test/.../ui/ListenOnlyModeTest.kt

Modificados:
  app/.../data/AppSetting.java                       (+ SETTING_LISTEN_ONLY)
  app/.../ui/MainActivity.kt                         (guards listen-only, beacon suppress, dialog removido)
  app/.../ui/compose/MainScreen.kt                   (toggle escucha + indicador + PTT guard)
  app/.../ui/compose/components/AlertButtonsPanel.kt (usa EmergencyConfirmButton)
  app/.../ui/compose/components/PttButton.kt         (anillos radiales TX)
  app/.../ui/compose/state/MainUiState.kt            (+ isListenOnly)
  app/.../ui/compose/state/MainUiAction.kt           (+ ToggleListenOnly)
```

**MotoRFAR en feature freeze para v1.0.** ✅
