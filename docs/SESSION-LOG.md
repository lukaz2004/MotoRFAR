# Session log â€” MotoRFAR

---

## 2026-06-09 Â· SesiÃ³n 0 â€” Setup e investigaciÃ³n inicial

- Stack instalado (PowerShell 7, Git, uv/uvx, plugins, MCPs)
- InvestigaciÃ³n legal: Res. 5/2015 adoptada como marco, PMR446/FRS descartados
- Decisiones: SA818-V, VHF, 3 canales, modo Ãºnico sin licencia, OSMDroid
- Scaffold completo: CLAUDE.md, 6 docs, gitignore, security guidance
- Git inicializado, 222 archivos en commit inicial

---

## 2026-06-09 Â· SesiÃ³n 1 â€” Sprint 1 completo (Fases A-D)

- A: ArgentinaChannels â†’ 3 canales VHF (6/6 tests)
- B: TX whitelist hard-limit â€” imposible salir de las 3 frecuencias (12/12 tests)
- C: EMERGENCIA â†’ 140.970 automÃ¡tico (8/8 tests)
- D: T&C v2.0 + strings sin PMR446
- PATH Windows corregido, GitHub configurado

---

## 2026-06-09 / 2026-06-10 Â· SesiÃ³n 2 â€” Sprint 2 visual + Sprint 3 (incompleto)

**Lo que se hizo:**
- Paleta verde tÃ¡ctico CRT completa (colors.xml reescrito)
- Share Tech Mono descargada y aplicada (res/font/share_tech_mono.ttf)
- Botones AppCompatButton con drawables custom (btn_emergency, btn_stop, btn_regroup)
- PTT con gradiente radial oval (ptt_button.xml como layer-list)
- rounded_corners con gradiente oscuro y borde verde
- memory_channel_bg/selected con layer-list y gradiente
- memory_channel_bg_emergency con borde rojo
- MemoriesAdapter.setHighlighted() â†’ usa setBackground() con drawables (no mÃ¡s setBackgroundColor que ignora XML)
- Canal EMERGENCIA detectado por frecuencia y borde rojo aplicado
- S-meter hardcodeado a #4FBD3B para evitar override de Material3
- Canales renombrados: Grupo â†’ PRINCIPAL
- Orden botones: EMERGENCIA arriba, DETENCIÃ“N+REAGRUPAR abajo
- Bottom nav: PTT + Mensajes con Ã­cono micrÃ³fono
- Version bump v4_principal_vhf (re-seed forzado)
- RecyclerView: wrap_content (eliminado space vacÃ­o)
- GitHub actualizado

**Estado visual al cierre:**
- Build exitoso, 59/59 tests pasando
- La app corre en emulador, la estructura es correcta
- La brecha visual con el mockup verde todavÃ­a existe:
  - S-meter puede mostrar Ã¡mbar en algunas builds (override programÃ¡tico en MainActivity)
  - El canal PRINCIPAL no siempre muestra el borde verde (depende del estado al cargar)
  - EMERGENCIA channel card puede no mostrar borde rojo hasta que el adapter hace el first bind

**Causa raÃ­z del bloqueo:**
Esta sesiÃ³n acumulÃ³ demasiado contexto (~200K tokens). Las Ãºltimas horas trabajÃ© con ventana saturada, generando mÃ¡s errores que soluciones en el visual.

---

## Estado actual del repo

Commits recientes:
- fix: botones AppCompat + S-meter verde + canal EMERGENCIA rojo
- fix: canal activo con borde verde real + PTT 90dp
- feat(Sprint3): visual CRT completo â€” fuente, botones 3D, PTT radial
- fix: version bump v4_principal_vhf

Tests: 59/59 PASS
Build: SUCCESSFUL
GitHub: https://github.com/lukaz2004/MotoRFAR

---

## 2026-06-10 Â· SesiÃ³n 3 â€” Plan Sprint 4 (write-plan)

**Lo que se hizo:**
- Lectura de CLAUDE.md, SESSION-LOG.md, docs/04-ROADMAP.md, docs/05-DISEÃ‘O.md, 03-DECISIONES.md
- AuditorÃ­a de build.gradle: detectado que Kotlin plugin NO estaba en el proyecto (solo kotlin-bom como impl dep), Compose no habilitado, kotlin 1.8.22 incompatible con Compose Compiler 1.5.x
- AuditorÃ­a de activity_main.xml (463 lÃ­neas) â†’ mapeados todos los componentes XML a equivalentes Compose
- Escrito docs/SPRINT-4.md con plan completo de 3 fases (A: infra, B: binding service, C: OSMDroid)
- Escrito ADR-008 en docs/03-DECISIONES.md: documenta la decisiÃ³n de migrar a Compose y por quÃ©

**Decisiones tomadas:**
- Kotlin 1.9.22 + Compose Compiler 1.5.10 + Compose BOM 2024.02.02 (tabla oficial de compatibilidad)
- MainActivity.java â†’ MainActivityLegacy.java (referencia), MainActivity.kt es el nuevo entry point
- MemoriesAdapter.java reemplazado por ChannelRow composable (3 canales no necesitan RecyclerView)
- Colors CRT como parÃ¡metros de funciÃ³n (Brush.radialGradient, Canvas) â€” no herencia de tema
- OSMDroid via AndroidView wrapper dentro de MapScreen composable

**QuÃ© queda pendiente:**
- EJECUCIÃ“N completa del plan (Fases A â†’ B â†’ C en Claude Code)
- Crear branch sprint/4-compose-ui
- A0: smoke test 59 tests antes de tocar nada
- A1: rename MainActivity.java â†’ MainActivityLegacy.java

---

## Para la prÃ³xima sesiÃ³n â€” Sprint 4 (Compose migration)

Arrancar Claude Code en MotoRFAR-MTTT con este prompt:

```
LeÃ© CLAUDE.md, docs/SESSION-LOG.md y docs/SPRINT-4.md.

El plan de Sprint 4 ya estÃ¡ escrito y auditado. Ir directo a ejecuciÃ³n:

1. Crear branch sprint/4-compose-ui
2. Ejecutar Fase A â†’ B â†’ C en orden, con TDD
3. Commit atÃ³mico al cerrar cada fase

No hacer brainstorming â€” las decisiones estÃ¡n en ADR-008 (docs/03-DECISIONES.md).
La primera acciÃ³n es A0: ./gradlew test para confirmar que los 59 tests pasan antes de tocar nada.
```

---

## 2026-06-10 Â· SesiÃ³n 4 â€” Sprint 4 ejecutado (Compose UI completo)

**Lo que se hizo:**

- A0: confirmados 59 tests pre-existentes antes de tocar nada
- Branch `sprint/4-compose-ui` creado
- A1: `git mv MainActivity.java â†’ MainActivityLegacy.java`, manifest y refs actualizadas
- A2: Gradle â€” Kotlin 2.2.0 + Compose Compiler bundled + Compose BOM 2024.02.02 + OSMDroid 6.1.18
- A3-A7: theme CRT (Color.kt, Type.kt, AppTheme.kt, MotoRFARTheme.kt), estado (MainUiState, MainUiAction), 5 componentes Compose, MainScreen.kt
- Fase A: 16 tests nuevos, commit `feat(A)`
- Fase B: MainActivity.kt Kotlin â€” ServiceConnection con RadioBinder, MutableStateFlow, callbacks, handleAction, alert flow con GPS permission â€” commit `feat(B)`
- Fase C: MapScreen.kt (OSMDroid via AndroidView), GroupMember.kt, NavHost + NavigationBar â€” commit `feat(C)`

**Tests al cierre:** 75/75 PASS (unit). `assembleDebug` SUCCESSFUL.

**Decisiones nuevas:** ADR-009 (Kotlin 2.2.0 bundled Compose Compiler), ADR-010 (RadioServiceAccessor bridge Kotlinâ†”Lombok).

**Pendiente:** push + PR, tests instrumented en dispositivo, GroupMember conectado a GPS real (Sprint 5).

---

## 2026-06-10 Â· SesiÃ³n 5 â€” Sprint 5 ejecutado (GPS + Alias + Sonidos)

**Lo que se hizo:**

- Branch `sprint/5-gps-alias-sounds` creado desde `main` (post Sprint 4)
- A: `AliasValidator.kt` + `SETTING_USER_ALIAS/BEACON_INTERVAL/ALERT_VOLUME` + `AliasSettingScreen.kt` (3er tab CONFIG) â€” 7 tests nuevos â€” commit `feat(A)`
- B: `GpsBeaconManager.kt` (coroutines, llama `sendPositionBeacon()` cada N seg), `GroupMember.isStale()`, decode `packetReceived` â†’ `GroupMember`, `MapScreen` conectado a `_groupMembers` StateFlow real â€” 10 tests nuevos â€” commit `feat(B)`  
- C: `ToneHelper.java` con `playPttDown/Up/AlertBeep/EmergencyBeep/StaticBurst` via `AudioTrack`, conectado en `handleAction` y `moduleTxStateChanged` â€” 7 tests nuevos â€” commit `feat(C)`
- `RadioServiceAccessor.java` extendido con `getAprsPayload()` / `getAprsSourceCall()` (bridge APRSPacket Kotlinâ†”Lombok)
- `kotlinx-coroutines-test:1.7.3` agregado para tests de GpsBeaconManager con tiempo virtual

**Tests al cierre:** 99/99 PASS. `assembleDebug` SUCCESSFUL.

**Decisiones:** RadioServiceAccessor bridge extendido para APRSPacket (mismo patrÃ³n ADR-010). Alias como callsign APRS â€” se pasa al servicio en `startAndBindService()`.

**Pendiente para Sprint 6:**
- Push + PR de este branch
- Tests instrumented en emulador (MainScreenTest.kt)
- Verificar transmisiÃ³n GPS real con SA818-V conectado
- Intervalo de baliza: `GpsBeaconManager` actualmente usa `beaconIntervalSec` fijo al iniciar â€” si cambia en settings, no recrea el manager hasta reinicio

---

## 2026-06-10 Â· SesiÃ³n 6 â€” Sprint 6 ejecutado (Canales nombrados + Alertas grupales)

**Lo que se hizo:**

- Branch `sprint/6-channels-alerts` ejecutado completo (Fases A â†’ B â†’ C â†’ D)
- A: `GpsBeaconManager.kt` â€” `intervalMs: Long` reemplazado por `intervalFlow: Flow<Long>` + `collectLatest`; `_beaconIntervalFlow` en MainActivity actualizado por `loadSettings()` y `saveAliasSettings()`. `GpsBeaconManagerHotReloadTest.kt` â€” 3 tests nuevos. 102 tests en total.
- B: `ArgentinaChannels.java` â€” canales renombrados GRUPO/ALTERNATIVO/EMERGENCIA, `PRELOADED_VALUE` bumpeado a `v6_channels_tactical` (fuerza re-seed en DB). `ChannelSelectorTest.kt` nuevo. 106 tests.
- C: `ReceivedAlert.kt` + `activeAlert: ReceivedAlert?` en `MainUiState`. `AlertBanner.kt` con `AnimatedVisibility` (slide+fade). DetecciÃ³n de paquetes APRS entrantes con keywords ALERTA/DETENCION/REAGRUPAMIENTO â†’ banner + `playAlertBeep` + auto-dismiss 30s. 124 tests.
- D: `EmergencyConfirmDialog.kt` â€” hold-2s con `LinearProgressIndicator` animado via `animateFloatAsState`, `detectTapGestures.onPress` + `tryAwaitRelease`. Reemplaza `AlertDialog` legacy en MainActivity. 130 tests.
- `assembleDebug` SUCCESSFUL al cierre.

**Decisiones nuevas:** Ninguna â€” siguiÃ³ patrones ADR-010 (RadioServiceAccessor bridge) y ADR-009 (Kotlin Compose).

**Pendiente para Sprint 7:**
- Push + PR de este branch
- Tests instrumented en emulador (MainScreenTest.kt actualizado para GRUPO)
- Verificar recepciÃ³n real de alertas con SA818-V + dos dispositivos
- Verificar hot-reload real del beacon interval en campo

---

## 2026-06-11 Â· SesiÃ³n 7 â€” Sprint 7 ejecutado (Onboarding package)

**Lo que se hizo:**

- Branch `sprint/7-onboarding-package` ejecutado completo (Fases A â†’ B â†’ C â†’ D) con flujo subagent-driven-development
- A: `AlertBannerInstrumentedTest.kt` (testTag + 2 tests instrumentados) + `channelSelector_shows_three_channels` en `MainScreenTest.kt`
- B: rename package `com.vagell.kv4pht` â†’ `ar.motorfar.app` en 121 archivos vÃ­a PowerShell bulk. `applicationId "ar.motorfar.app"`. Listo para Play Store.
- C: `TermsScreen.kt` â€” pantalla Compose completa con 6 secciones, cita explÃ­cita ResoluciÃ³n 5/2015, las 3 frecuencias legales, botÃ³n ACEPTAR. `TermsScreenTest.kt` (2 tests).
- D: `OnboardingHelper.kt` (objeto puro, unit-testeable), `AliasSetupOnboarding.kt`, `ChannelSelectOnboarding.kt`, `OnboardingActivity.kt` con NavHost `termsâ†’aliasâ†’channel`. Gate en `MainActivity.onCreate` â€” redirige al onboarding si `onboarding_complete != "true"`.
- Post code-review: 4 fixes â€” `executor.shutdown()`, `noHistory=true`, `stopService` antes de redirigir, EMERGENCIA no seleccionable como canal default.
- Unit tests al cierre: BUILD SUCCESSFUL (suite completa).
- PR abierto: `sprint/7-onboarding-package` â†’ `main` (gh CLI no instalado; PR creado manualmente).

**Decisiones nuevas:** Ninguna estructura nueva â€” sigue ADR-009 (Compose), ADR-010 (RadioServiceAccessor). `TermsActivity.java` se mantiene (sus constantes las reutiliza `OnboardingActivity`).

**Trampas:** GateGuard activo bloqueÃ³ cada Edit/Write hasta presentar 4 facts. Costo de sesiÃ³n elevado (~$9.67) por cantidad de subagentes + rename masivo.

**Pendiente para Sprint 8:**
- Verificar onboarding en emulador fÃ­sico (tests instrumentados requieren dispositivo/emulador)
- Instalar `gh` CLI para automatizar creaciÃ³n de PRs (`winget install GitHub.cli`)
- `TermsActivity.java` â€” evaluar si extraer sus constantes a `AppSetting` (Finding 5 MEDIUM pendiente)

---

## 2026-06-13 Â· SesiÃ³n 8 â€” Sprint 8 ejecutado (ConfirmaciÃ³n EMERGENCIA + AnimaciÃ³n PTT + Modo Escucha)

**Lo que se hizo:**

- **PRE-SPRINT:** `gh` CLI 2.94.0 instalado vÃ­a winget + autenticado (token, scopes repo/workflow/admin:org). PR #4 (sprint/7 â†’ main) creado y mergeado. Cierre de artefactos S7 commiteado primero: docs sprints 4-7, schema Room `ar.motorfar.app.data.AppDatabase/6.json`, eliminaciÃ³n de schemas viejos `com.vagell.*` del tracking, `.gitignore` ampliado (.claude/, docs/superpowers/). Branch `sprint/8-ux-tactical-polish` creado desde main actualizado.
- **A â€” ConfirmaciÃ³n EMERGENCIA:** `EmergencyConfirmButton.kt` nuevo. Hold 2s con fill de progreso Canvas (izqâ†’der), borde que brilla al sostener. Tap corto NO dispara. Reemplaza el Box clickable en `AlertButtonsPanel.kt`. `EmergencyConfirmDialog` (S6) removido del flujo de MainActivity â€” `EmergencyAlert` ahora va directo a `requestLocationAndTransmit`. 8 tests del hold.
- **B â€” AnimaciÃ³n PTT:** `PttButton.kt` con 3 anillos radiales expansivos vÃ­a `InfiniteTransition`. Fases escalonadas (1/3 desfase), alpha 0.55â†’0, ciclo 1200ms. Solo durante `isTransmitting`.
- **C â€” Modo Solo Escucha:** `SETTING_LISTEN_ONLY` en AppSetting. `MainUiState.isListenOnly` + `MainUiAction.ToggleListenOnly`. Toggle en TopBar (`ic_headphones.xml` nuevo) + indicador `[ SOLO ESCUCHA ]`. Guards en `handleAction`: PTT bloqueado, STOP/REGROUP bloqueadas, **EMERGENCIA siempre disponible**, beacon GPS suprimido (RX-only real vÃ­a lambda condicionada). Persistencia Room, default OFF. 10 tests.
- **D â€” Cierre:** compileDebugKotlin + testDebugUnitTest â†’ BUILD SUCCESSFUL. **151 tests, 0 fallos, 24 suites.**

**Decisiones nuevas:**
- Modo escucha como RX-only **real**: no solo deshabilita UI, tambiÃ©n suprime el beacon GPS automÃ¡tico (que es un TX). EMERGENCIA es la Ãºnica excepciÃ³n que transmite en modo escucha â€” decisiÃ³n de seguridad explÃ­cita.
- `EmergencyConfirmDialog` (S6) deprecado en favor del botÃ³n inline con hold. El archivo queda en el repo pero ya no se referencia (limpieza opcional para S9).
- JDK 21 de Android Studio (`C:\Program Files\Android\Android Studio\jbr`) usado para builds CLI â€” el Java del sistema es 8, incompatible con AGP.

**Trampas:**
- Flujo de auth gh: varios tokens se expusieron en el chat durante el setup (en lÃ­nea de comando, como nombre de archivo). Resuelto con `Read-Host -AsSecureString` en una sola lÃ­nea. **Todos los tokens previos deben considerarse comprometidos.**
- El trabajo de S8 (8 archivos) ya estaba escrito sin commitear cuando se decidiÃ³ mergear S7 primero â€” se preservÃ³ en stash, se mergeÃ³ S7, y se recuperÃ³ con stash pop sobre el branch S8.

**Pendiente para Sprint 9 (v1.1):**
- Revocar definitivamente los tokens gh expuestos en sesiÃ³n + regenerar uno limpio si se sigue usando gh
- Mergear PR de Sprint 8 (queda abierto para review)
- Verificar las 3 features en emulador/dispositivo fÃ­sico
- PTT externo vÃ­a jack 3.5mm (audio focus + hardware)
- OSMDroid tiles offline
- Eliminar `EmergencyConfirmDialog.kt` (deprecado, sin referencias)
- `TermsActivity.java` â€” extraer constantes a AppSetting (Finding 5 MEDIUM, arrastrado de S7)
- VerificaciÃ³n con SA818-V hardware real

---

## 2026-06-15 Â· SesiÃ³n 9 â€” Testing de campo + handoff (sin cÃ³digo nuevo)

**Lo que se hizo:**

- Testing de campo en emulador (Pixel 10 Pro XL, landscape). 5 observaciones del usuario:
  (1) el rail lateral pisa la isla de la cÃ¡mara, (2) "RUTA ARRIBA" sin explicar,
  (3) falta PTT / volver atrÃ¡s en el mapa, (4) sin sonido de radio al emitir/terminar TX,
  (5) modo solo-escucha sin feedback visual ni aviso al bloquear funciones.
- **Hallazgo central:** los 5 Ã­tems YA estÃ¡n implementados en el working tree, como
  cambios **sin commitear** en 4 archivos: `MainActivity.kt`, `ToneHelper.java`,
  `MainScreen.kt`, `MapScreen.kt`. El emulador corrÃ­a un APK viejo â†’ de ahÃ­ la discrepancia
  entre las capturas y el cÃ³digo.
  - #1 â†’ `.displayCutoutPadding()` en el Row raÃ­z. #2 â†’ toggle heading-up + indicador.
  - #3 â†’ `MapPttButton` sobre el zoom + `NavigationRail` en landscape (todas las pantallas).
  - #4 â†’ `playPttDown` + `playRogerBeep` (fin TX, estilo VHF). #5 â†’ toggle con estado relleno
    + `notifyListenOnlyBlocked()` Toast + Toast de cambio de modo.
- Verificado que el cÃ³digo es auto-consistente (`MainActivity` llama `ToneHelper.playRogerBeep`,
  que existe en el archivo).
- **No se commiteÃ³ ni compilÃ³ nada** (decisiÃ³n: verificar build antes de commitear, y en branch,
  no en `main`).
- Creado `docs/SPRINT-9.md` con el handoff "CONTINUAR ACÃ" + checklist de re-test.

**Estado git al cierre:** branch `main`; 4 archivos `M` sin commitear; sin stash.

**Pendiente (maÃ±ana / Sprint 9):** compilar (`assembleDebug`, JAVA_HOME = jbr de Android Studio)
â†’ commit en `sprint/9-field-polish` â†’ `installDebug` â†’ re-test de los 5 Ã­tems â†’ tests â†’ PR.
Arrastrado de S8: mergear el PR de Sprint 8, revocar los tokens gh expuestos, borrar
`EmergencyConfirmDialog.kt`.

---

## 2026-06-23 Â· SesiÃ³n 10 â€” Hardware: modificaciÃ³n esquemÃ¡tico PCB v2.0e (DÃ­a 3)

**Lo que se hizo:**

- EsquemÃ¡tico `pcb/v2.0e/kv4p-ht/kv4p-ht.kicad_sch` modificado para MotoRFAR (sin botones fÃ­sicos, PTT externo).
- SW1 y SW2 marcados DNP (`(dnp yes)` + `(in_bom no)`) â€” no se pueblan en el PCB.
- R14 se mantiene: mantiene GPIO35 en HIGH permanente (PTT Right = nunca presionado = seguro).
- Agregado `Connector_Generic:Conn_01x02` a `lib_symbols` (definiciÃ³n embebida en el .kicad_sch).
- Agregado J2 "PTT_EXT_3.5mm" (jack 3.5mm TS mono, footprint PJ302M_Vertical):
  - Pin 1 (Tip/PTT) â†’ net "PTT Button Left" (vÃ­a wires hasta la junction existente en (148.59, 146.685))
  - Pin 2 (Sleeve/GND) â†’ sÃ­mbolo GND
- BOM `production-vhf/bom.csv` actualizado: SW1/SW2 removidos, J2 agregado (LCSC TODO â€” verificar C2899878 o equivalente PJ302M).
- Errores de sintaxis KiCad resueltos en el proceso:
  - `pin_background` â†’ `background` (fill type invÃ¡lido en KiCad 10.0)
  - `(at X Y)` â†’ `(at X Y 0)` en definiciÃ³n de pines (faltaba el Ã¡ngulo)
  - Backtick-n literal en reemplazo regex con single-quotes â†’ corregido con `.Replace()` directo
- EsquemÃ¡tico abre y guarda sin errores en KiCad 10.0.3.

**Decisiones:**
- Sin botones PTT fÃ­sicos en la PCB de MotoRFAR. Un solo conector 3.5mm TS para PTT externo por manillar.
- R14 se mantiene (sin ADR nuevo â€” es consecuencia directa de SW2 DNP).

**Pendiente:**
- Verificar LCSC part number del jack 3.5mm TS (PJ302M o equivalente) antes de ordenar.
- ERC del esquemÃ¡tico (puede haber warning de pin desconectado â€” es aceptable).
- Actualizar `docs/06-HARDWARE.md` para reflejar el conector J2 PTT externo.

---

## 2026-06-23 Â· SesiÃ³n 11 â€” Hardware: archivos de producciÃ³n PCB listos para JLCPCB

**Lo que se hizo:**

- Confirmado: SW1/SW2 no estÃ¡n en el layout del PCB (solo en el esquemÃ¡tico) â€” no hay nada que sincronizar.
- Logo BAQUEANO V2.0 convertido a footprint KiCad (`kv4p-ht.pretty/Logo_Baqueano.kicad_mod`, 98 polÃ­gonos en B.SilkS, 28Ã—19.4mm). Instancia agregada al PCB en posiciÃ³n (146.05, 158.0) cara trasera.
- Gerbers generados con `kicad-cli pcb export gerbers` (22 archivos, 4 capas de cobre, silkscreen, mask, Edge.Cuts, drill).
- CPL convertido a formato JLCPCB (`CPL_JLCPCB.csv`, 89 componentes con columnas Designator / Mid X / Mid Y / Layer / Rotation).
- BOM limpiado a formato JLCPCB (`BOM_JLCPCB.csv`, 38 componentes con LCSC#). Excluidos de PCBA: SA818-V (no en LCSC) y J2 jack 3.5mm PJ302M (through-hole, no Economic PCBA) â€” se compran por separado y se sueldan a mano.
- ZIP final `production-vhf/MotoRFAR_JLCPCB_READY.zip` (536 KB) listo para subir a jlcpcb.com/quote.
- Color PCB: negro o azul ($0 extra). Estimado total: ~USD 100â€“130 para 5 placas ensambladas + envÃ­o Global Standard.

**Pendiente hardware (antes de ordenar):**
- Verificar LCSC# del PJ302M (placeholder C2899878) en lcsc.com.
- Verificar visualmente posiciÃ³n del logo en KiCad PCB editor.
- SA818-V + PJ302M: comprar por separado en AliExpress.

**Pendiente app:** Sprint 9 â€” compilar working tree, re-test 5 hallazgos campo, tests, PR.

---

## 2026-06-23 · Sesión 12 — Cierre Sprint 9 + setup para sesiones largas

**Lo que se hizo:**

- Sprint 9 compilado (build + tests exit 0, 8 archivos), commiteado, PR #6 creado y mergeado. Main limpio.
- CLAUDE.md actualizado: apunta a SPRINT-10, instrucciones para modalidad celular→PC (JAVA_HOME, comandos de build), estado real del proyecto.
- SPRINT-10.md creado: 4 fases autónomas (A: cleanup, B: tiles offline, C: README+Privacy, D: release). Diseñado para ejecución sin preguntas intermedias.
- Archivos hardware pendientes de trackear (_PROYECTO/, STL, f3d, PSD) identificados — se hacen en Fase A de Sprint 10.

**Decisiones:**
- Sesiones largas desde celular → Claude ejecuta fase completa sin input del usuario salvo bloqueantes reales.

**Próximo:** git checkout -b sprint/10-offline-docs y ejecutar Fase A de SPRINT-10.md.

---