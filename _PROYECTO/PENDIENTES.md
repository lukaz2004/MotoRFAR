# PENDIENTES — MotoRFAR MTTT

> Lista viva de cosas a no olvidar durante el rewrite. Actualizar al cerrar/abrir items.
> Última edición: 2026-07-07. Detalle completo de lo cerrado el 2026-07-06/07
> (Man-Down en background, restricción canal Emergencia, tonos CTCSS, auditoría
> de seguridad, SSID único por equipo, retoques de UI, animaciones de
> emergencia) vive en `NEXT_SESSION.md` — acá solo queda lo que sigue abierto.

## 🔓 Pendientes abiertos de la sesión 2026-07-06/07
- ✅ **Ícono de Baqueano seguía diminuto (2026-07-07)**: el PNG usado
  (`ic_launcher_moto.png`) es la capa foreground de un ícono adaptativo —
  trae ~33% de margen transparente en cada lado por convención de Android
  (zona segura del recorte de máscara), así que a tamaño fijo el escudo real
  se veía mucho más chico que la caja. Se agregó `ContentScale.Crop` (zoom
  que recorta el margen vacío) + subida de 40dp a 48dp. Confirmado
  visualmente en el emulador.
- ✅ **PTT "a veces no responde" (2026-07-07)**: causa real encontrada —
  `RadioAudioService.startPtt()` rechazaba el press en silencio (solo un
  `Log.w`, sin ningún aviso) si el módulo todavía no había vuelto a modo RX
  (ej. justo después de un timeout de runaway TX) o `isTxAllowed()` daba
  false en ese instante — exactamente el patrón intermitente reportado.
  Cambiado a `boolean` (devuelve si arrancó de verdad) y se agregó Toast
  "RADIO OCUPADA · esperá un segundo y probá de nuevo" cuando se rechaza,
  mismo patrón ya usado para modo escucha/canal Emergencia bloqueados. No
  se pudo reproducir ni verificar en el emulador (requiere el estado real
  del módulo de radio con equipo conectado) — pendiente confirmar que el
  aviso aparece en el dispositivo real cuando pasa.
- ✅ **Cerrado (2026-07-14):** `TermsScreen.kt` (onboarding) decía "MotoRFAR"
  (título + 2 menciones en el texto legal) y "139.970 MHz — GRUPO" en vez
  de "PRINCIPAL" (inconsistente con `ArgentinaChannels.java`, que ya usa
  "PRINCIPAL"). Corregido, build verde.
- ⬜ Probar en dispositivo físico: botón "ESTOY BIEN · CANCELAR" de Man-Down
  tocado a mano, y alerta de otro integrante llegando con la app cerrada.
- ✅ **`onTaskRemoved()` ya no mata el Service (2026-07-10)** — ver CIERRE
  2026-07-10 en `NEXT_SESSION.md` para el detalle completo.
- ✅ **Cerrado (2026-07-14): `channelMemories` ahora es reactiva de Room.**
  `ChannelMemoryDao.observeAll()` (nuevo `@Query` que devuelve
  `LiveData<List<ChannelMemory>>`) reemplaza el snapshot único que se
  cacheaba en un `MutableLiveData` manual en `MainViewModel.loadData()`.
  Ahora se re-emite sola en cada insert/update/delete de `channel_memories`
  vía el InvalidationTracker de Room — ya no puede desincronizarse del
  canal real. `highlightMemory()`/`isHighlighted()` no se tocaron (código
  ya muerto, sin ningún caller real, confirmado por grep). Build verde,
  sin verificar aún en dispositivo físico (mismo pendiente de siempre).
- ⬜ Animaciones de emergencia (PTT rojo, ecualizador rojo, botón parpadeando)
  verificadas por el mecanismo (mismo estado derivado, probado tocando el
  canal Emergencia a mano) pero no por el flujo automático real de 2s hold
  con radio conectada — requiere equipo físico.
- ✅ **Cerrado (2026-07-14):** `ic_launcher_moto.png` (5 densidades) renombrado
  a `ic_launcher_baqueano.png` vía `git mv` (conserva historial), referencias
  actualizadas en `ic_launcher_foreground.xml` y `MainScreen.kt`. Build verde.
- ✅ **Cerrado (2026-07-14): pantalla de historial de rutas + exportar GPX.**
  `RoutePointDao.getSessionSummaries()`/`deleteSession()` (nuevas queries,
  agrupan por `sessionId`) + `RouteHistoryScreen.kt` (lista salidas
  anteriores con fecha, cantidad de puntos y km recorridos; "VER" carga esa
  sesión en el mapa, "BORRAR" borra solo esa salida -- ya no es todo o
  nada). `GpxExporter.kt` genera GPX 1.1 y lo comparte por share intent vía
  un `FileProvider` nuevo (`res/xml/file_paths.xml`, `cacheDir/gpx/`).
  Acceso desde Ajustes → "HISTORIAL DE RUTAS →". Build verde, 171 tests
  pasan. **Sin probar con datos reales todavía** -- no hay ninguna salida
  guardada en el dispositivo de prueba (nadie activó "RUTA" con movimiento
  real); el estado vacío se verificó en el Huawei P9, el flujo completo de
  ver/exportar/borrar una sesión con datos reales queda para la primera
  salida real grabada.
- ⬜ Sin verificar en dispositivo físico: pantalla de tonos, scroll de
  Ajustes, UI de WiFi/SSID, retoques estéticos de pantalla principal, mapa.
- ⬜ **Autenticación real del protocolo UDP (token/HMAC)**: hoy solo se mitigó
  con `max_connections=1` en el SoftAP (barato, cierra el caso más común).
  La autenticación de aplicación de verdad es un rediseño de protocolo, no
  un fix de una sesión — necesita su propia sesión de diseño.
- ✅ **Confirmado (2026-07-14) sin necesitar el dashboard**: `curl -I` contra
  `baqueano.netlify.app` devuelve los headers de `_headers` aplicados
  (CSP, X-Frame-Options, HSTS, X-Content-Type-Options) — el publish
  directory está bien configurado, Netlify no los pone por defecto.
- ✅ **Confirmado (2026-07-14): el build de release SÍ define `RELEASE`.**
  `platformio.ini:42` (`env:esp32dev-release`) tiene `-DRELEASE=1`. El
  `while(true)` sin resetear WDT vive en `debug.h:161-167`
  (`_esp_error_check_failed`) adentro de `#ifndef RELEASE` -- no se
  compila en release. **Pero ojo:** el comando de build documentado en
  `CLAUDE.md` usa `-e esp32dev` (el entorno DEBUG, con el hang activo),
  no `-e esp32dev-release`. Si algún día se flashea firmware para uso
  real en campo (no solo pruebas de banco), usar `-e esp32dev-release`
  o actualizar el comando en `CLAUDE.md` antes de flashear.
- ✅ **Cerrado (2026-07-14):** `./gradlew app:dependencies` corrido, árbol
  revisado. Sin CVEs conocidos para las dependencias de nicho
  (`esp32-flash-lib`, `minimal-json:0.9.5`, `slf4j-api:1.7.36`,
  `concentus`) — búsqueda web sin resultados de vulnerabilidades. Compose/
  AndroidX/Kotlin resuelven a versiones razonablemente actuales.
- ✅ **Cerrado (2026-07-14):** `ProtocolKissTest.java:882` tenía un
  `super(null)` ambiguo entre los dos constructores de `Sender`
  (`Sender(SerialInputOutputManager)` vs `Sender(FrameWriter)`, agregados
  en el PR de WiFi transport, commit `185a757`) — rompía la compilación de
  `testDebugUnitTest` (no afectaba `assembleDebug`, solo el test suite).
  Desambiguado con cast explícito. De paso aparecieron 3 tests que todavía
  esperaban el nombre viejo "GRUPO"/versión vieja del seed en vez de
  "PRINCIPAL" (el código de producción ya se había renombrado en una sesión
  anterior, los tests nunca se actualizaron) — corregidos. `testDebugUnitTest`
  ahora pasa 171/171.

## 🔧 Hardware disponible + objetivo explícito: cerrar FIRMWARE de una vez (2026-07-10)
LuKaZ confirmó que hay un **ESP32 pelado** (sin módulo SA818/DRA818, sin
antena) **ya flasheado** con el firmware de este proyecto
(`kv4p_ht_esp32_wroom_32`), y pidió explícitamente **cerrar todo el frente
de firmware de una vez** en una sesión dedicada con este equipo, en vez de
seguir dejándolo como "pendiente de hardware" sesión tras sesión. Antes de
arrancar esa sesión, conseguir/conectar el módulo SA818/DRA818 + antena si
es posible — sin eso, la parte más importante (RF real, whitelist con RF,
PTT físico, Man-Down transmitiendo de verdad) sigue sin poder cerrarse.
Lista de lo que hoy está "🟡 pendiente de SA818 físico" en
`00_MAPA_MAESTRO.md`/`02_AUDITORIA.md`/`03_CALENDARIO.md`: FW-2 (test RF
real de whitelist), FW-3a (`--open-rx`, failsafe de WiFi en TX), FW-4 (J2
dispara TX), APP-2 (Hello/handshake + audio RX/TX real por WiFi) — todo
esto es candidato a cerrarse junto en esa sesión si el módulo está.

**Lo que SÍ se puede probar con este setup** (capa de protocolo/conexión,
sin RF real):
- Handshake HELLO por USB/WiFi, `isConnectionReady()`/`isRadioConnected()`.
- El aviso de "enlace colgado" agregado hoy (`STALE_CONNECTION_TIMEOUT_MS`,
  15s sin calibrar) — conectar, dejar el ESP32 quieto respondiendo, después
  desconectarlo/matarlo sin que la app se entere, y cronometrar si el aviso
  aparece en un tiempo razonable.
- Comandos UDP del protocolo (`COMMAND_HOST_DESIRED_STATE`,
  `COMMAND_HOST_SET_WIFI_PASSWORD`/`SET_WIFI_SSID`, etc.) y el hallazgo
  ALTO-3 de seguridad (comandos sin autenticar).
- `WifiTransport`/reconexión, SSID único por equipo, cambio de clave/SSID
  desde `WifiSettingScreen.kt`.

**Lo que NO se puede probar sin el módulo de radio** (falta SA818/DRA818 +
antena): transmisión/recepción RF real, whitelist TX con RF de verdad
(`CRÍTICO-1` de `AUDITORIA_SEGURIDAD_FIRMWARE.md` está confirmado por
trazado de código, no por RF real todavía), PTT físico del botón del
módulo, Man-Down transmitiendo de verdad por 140.970, si alguien recibe
una alerta en la práctica. Sigue siendo el mismo bloqueador de hardware de
siempre para esa parte.

## 🚨 Confiabilidad de alertas — 10 fixes cerrados (2026-07-10), detalle en NEXT_SESSION.md
Toda la cadena de Man-Down/EMERGENCIA revisada de punta a punta tras una
auditoría profunda (`AUDITORIA_CONFIABILIDAD_ALERTAS.md`,
`AUDITORIA_FALLAS_SILENCIOSAS.md`). 10 commits, build verde en cada uno,
resumen completo en `NEXT_SESSION.md` CIERRE 2026-07-10.

- ⬜ **Sin verificar en dispositivo físico, todo lo de hoy**: igual que el
  resto del proyecto, nada de esto se probó con equipo real conectado.
  Puntual: el timeout de 15s del aviso de "enlace colgado" es un punto de
  partida sin calibrar (ver `STALE_CONNECTION_TIMEOUT_MS` en
  `RadioAudioService.java`) — podría dar falsos avisos o tardar de más,
  ajustar con uso real.
- ⬜ **El problema de fondo sigue abierto a propósito**: nadie garantiza
  tener la radio en 140.970 en el momento de una EMERGENCIA real — no es
  un bug de código, es una limitación de hardware (SA818 escucha una sola
  frecuencia a la vez) + de uso. Mitigado por ahora con protocolo humano
  escrito en el propio cartel de alerta (parar, llamar por los 3 canales,
  escalar a emergencias reales si no hay respuesta).
  **Descartado explícitamente por LuKaZ (2026-07-14): "scan por defecto"
  NO se construye.** Las frecuencias fijas son una decisión de diseño a
  propósito, no un descuido — no se quiere la funcionalidad de escaneo
  del fork original (kv4p HT). El `RadioMode.SCAN` que ya existe en
  `RadioAudioService.java` es para recorrer canales/memorias guardadas a
  mano (feature separada, ya andaba), no tiene relación con esta idea
  descartada. No reabrir esto sin que LuKaZ lo pida explícitamente.

## 💡 Web/marketing — video explicando el protocolo de respuesta a EMERGENCIA (2026-07-10)
Propuesta sin construir, dicha por LuKaZ: una vez que la funcionalidad de
alertas esté resuelta y estable en la app (ver hallazgos de confiabilidad
de alertas más abajo), hacer contenido para la web/redes explicando el
protocolo de respuesta que ahora vive en el cartel de EMERGENCIA entrante
(`AlertBanner.kt`, commit 2026-07-10): parar a revisar, llamar por los 3
canales, y si no hay respuesta pedir ayuda o avisar a emergencias/
autoridades reales. Da contexto real de uso, no solo specs técnicas.
Orden explícito de LuKaZ: primero cerrar funcionalidad de la app, después
contextualizar la web — no adelantar este contenido todavía.

## 🔐 Protocolo UDP del firmware sin autenticación de aplicación (2026-07-09)
ALTO-1 + ALTO-3 de la auditoría de seguridad 2026-07-09
(`AUDITORIA_SEGURIDAD_FIRMWARE.md`): cualquier dispositivo que gane el único
slot del SoftAP (`max_connections=1`) puede mandar comandos crudos sin
autenticarse, incluyendo los nuevos `COMMAND_HOST_SET_WIFI_PASSWORD`/
`SET_WIFI_SSID` sin pedir confirmar la clave actual — puede dejar al dueño
real bloqueado afuera del equipo, recuperable solo reflasheando por USB.
Mismo protocolo, misma raíz que el hallazgo original de la auditoría del
2026-07-06. **No es un fix de una sesión — es rediseño de protocolo
(token/HMAC).** Necesita su propia sesión de diseño dedicada.

## 🗺️ Mapas offline por provincia — pipeline CORRIDO, release publicado (2026-07-09)
✅ Las 24 provincias (23 + CABA) generadas como `.map` Mapsforge y publicadas
en GitHub Release `mapas-v1` de `lukaz2004/MotoRFAR` (público, ~366MB total).
Manifest provincia→URL en `_PROYECTO/mapas_offline/provincias.json`.
El release tenía `createdAt` de dos días antes de que se lanzara el pipeline
de esta sesión — confirmado con LuKaZ que viene de otra sesión de chat previa
que ya lo había corrido (ver detalle en `NEXT_SESSION.md`, CIERRE 2026-07-09
segunda parte).
- ⬜ Sub-proyecto 2 (ventana in-app que consume el manifest) — arrancable
  ahora que hay URLs reales, todavía sin construir.

## 🧭 Navegación turn-by-turn — MVP hecho y probado (2026-07-14), fase 2 pendiente
Diseño completo en `NAV_TURN_BY_TURN_DISENO.md`. **MVP construido y probado en
el Huawei P9 real** (ver CIERRE 2026-07-14 (3) en `NEXT_SESSION.md`): calcula
y dibuja una ruta real con BRouter vendorizado a un punto tocado en el mapa,
sin voz ni recálculo automático.
- ✅ **Buscador de direcciones (hecho y probado 2026-07-14)**: campo de texto
  junto a "IR A" que geocodifica con Nominatim (`GeocodingRepository.kt`,
  `countrycodes=ar` para no matchear un lugar homónimo en otro país) y dispara
  la misma ruta que tocar el mapa. Requiere internet en el momento de buscar.
- ✅ **Señalización visual de giros (hecho y probado 2026-07-14)**: cartel en
  el HUD con flecha + distancia ("◀ Girá a la izquierda, en 120 m") usando el
  `VoiceHintList` que BRouter ya calculaba internamente (había que ampliar la
  visibilidad de esos campos en el vendorizado -- eran package-private -- y
  setear `rc.turnInstructionMode = 1` DESPUÉS de `parseProfile()`, porque el
  perfil lo resetea a 0 si no se pisa después). Sin voz.
- ✅ **Voz + ducking (hecho 2026-07-14)**: `TurnAnnouncer.kt` (TTS nativo,
  offline, es-AR con fallback a es genérico) anuncia el próximo giro una vez
  cuando entra a 200m, pidiendo foco `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.
  `RadioAudioService.java` ahora tiene `onRxAudioFocusChange()`: se agacha de
  volumen (25%) en vez de competir, y `handleRxAudio()` dejó de re-pedir
  `AUDIOFOCUS_GAIN` en cada paquete RX (antes le arrebataba el foco de vuelta
  a mitad de frase). Verificado: build limpio, la app no crashea, el motor
  TTS se conecta de verdad al servicio de Google TTS (`dumpsys activity
  processes`), y la radio sigue andando. **No verificado con oído real** --
  ni el audio hablado ni el ducking en vivo con tráfico de radio real; eso
  necesita prueba manual con el equipo.
- ✅ **Recálculo automático por desvío de ruta (hecho 2026-07-14)**: si te
  alejás de la ruta trazada más de 70m (point-to-segment, no solo al vértice
  más cercano -- evita falsos positivos en tramos rurales largos y rectos),
  se recalcula sola desde donde estás al mismo destino. Chequeo cada 8s.
  Verificado por build + revisión de lógica; no se probó en campo con un
  desvío real (no hay forma práctica de simular GPS mock en este dispositivo).
- ✅ **Descarga de tiles para todo el país (hecho y probado 2026-07-14)**:
  botón "DESCARGAR RUTAS DE TODO EL PAÍS" en Ajustes (`CountryTileRepository.kt`)
  -- 40 celdas de 5°×5° cubriendo Argentina (bounding box rectangular con
  margen, no el contorno real), descarga secuencial, cada celda fallida
  (océano sin datos) se salta sin abortar el lote, cancelable a mitad de
  camino. Probado en el Huawei P9 real por WiFi: bajó 7 tiles reales
  (160 KB a 14 MB cada uno), saltó 1 celda oceánica sin datos, y Cancelar
  cortó el lote correctamente.
- ✅ **Mapa sigue la posición (hecho y probado 2026-07-14)**: botón "MI GPS"
  pasó a ser un toggle ("SIGUIENDO" cuando está activo) que usa el
  follow-location nativo de OSMDroid (`MyLocationNewOverlay.enableFollowLocation()`)
  -- se re-centra solo en cada fix de GPS, sin polling propio. No se
  auto-desactiva si arrastrás el mapa (toggle explícito, más predecible que
  tratar de distinguir un scroll manual de uno programático). Probado en el
  Huawei P9 real: centra en la posición real al activarse, vuelve a "MI GPS"
  sin romper nada al desactivarlo.
- ⬜ Los 4 puntos "abierto para pulir" del diseño original (tile inicial sin
  GPS fix, UI del cartel de advertencia, destino sobre miembro sin fix
  reciente) siguen abiertos, ninguno bloquea el MVP actual.

## ✅ Confirmado con código (no había que arreglar nada) — 2026-07-05
- El PTT físico (botón del equipo o externo por jack) funciona con cualquier
  otra app abierta en pantalla (Google Maps, etc.) — `RadioAudioService` corre
  como foreground service y procesa `isPhysPttDown()` fuera del ciclo de vida
  de la Activity. Ya sumado como diferencial en la web (beneficios).
- "IR A UBICACIÓN" ya abre Google Maps real vía intent `geo:`, no el mapa
  interno — confirmado en `APRSAdapter.java`.

## 🗺️ Registro de ruta — arreglado 2026-07-05
- ✅ Antes mezclaba todo el historial de un alias en una sola línea sin fin, sin
  forma de borrarlo desde la UI. Ahora: `RoutePoint.sessionId` agrupa por salida
  (migración Room 7→8), se carga solo la última sesión al abrir la app, y hay
  botón de borrar (con confirmación) en la barra superior.
- ✅ **Hecho (2026-07-14)**: exportar a GPX / compartir el track — ver
  CIERRE de "pantalla de historial de rutas + exportar GPX" más arriba.
- 💡 **Propuesta sin construir**: marcar POIs propios (ej. "buen lugar para
  acampar", "cruce peligroso") y compartirlos al grupo — reusaría la misma
  infraestructura del botón WAYPOINT existente (`MainUiAction.SendWaypoint`,
  ya transmite posición por VHF) en vez de ser algo nuevo desde cero.
- ✅ **Man-Down: countdown variable según fuerza G del golpe** (hecho 2026-07-05).
  `FallDetectionManager` pasa el pico de aceleración al callback; `countdownSecondsFor()`
  en `MainActivity.kt` mapea: >60 m/s² (~6G) → 8s, 40-60 (~4-6G) → 15s, 25-40
  (~2.5-4G, original) → 30s. Build verificado. Umbrales de corte (60/40) son
  punto de partida — se calibran junto con el resto de Man-Down en dispositivo
  físico (mismo pendiente de siempre, no se probó en moto real todavía).
  ✅ Copy de la web corregido (2026-07-05): las 3 menciones de "30 segundos"
  fijo (beneficios, card Man-Down, FAQ) ahora dicen "cuenta regresiva" sin
  número fijo, coherente con el countdown variable por fuerza G.
- ✅ **Web — copy de CTCSS corregido (2026-07-05)**: "Sin interferencias, sin
  confusión, sin escuchar a desconocidos" (sobrevendía) → "No escuchás a
  desconocidos que compartan el canal, aunque transmitan al mismo tiempo que
  tu grupo" — ya no insinúa que separa el RF real, solo lo que se escucha.

## 🔧 HW-1 — rework real 2026-07-04 (rescatado, no se había anotado en su momento)
- ✅ Stub RF viejo de J1 borrado + traza C5→J1 re-ruteada (se rompió al borrar el stub).
- ✅ C9→R2 conectado — **bug heredado real** (filtro mic audio nunca se había completado,
  no eran restos de cobre para borrar como se pensó al principio).
- ✅ J2 corregido en el esquemático (Pin1/Pin2 estaban invertidos).
- ✅ Texto silkscreen 'Baqueano V 2.0.1' movido de F.Cu a F.Silkscreen.
- ✅ DRC final: 90 violaciones, todas heredadas y documentadas (ver `HW1_CIERRE.md`).
- ⬜ **C15/C32** — pin2 debería ir a GND (hoy comparte nodo con R12/J2 en `/PTT Button Left`).
  Cosmético, no bloquea fabricación, pero requiere reubicar el símbolo en el esquemático
  con supervisión en vivo — no editar a ciegas.
- Evidencia: `HW1_CIERRE.md`, `drc_FINAL_night.json`, `erc_j2_redo.json`, `netlist_after_j2.net`.

## 🚑 Man-Down — countdown cerrado y probado (2026-07-04)
- ✅ Toggle en Ajustes (Compose `AliasSettingScreen.kt`) — estaba wireado pero inalcanzable desde
  que la navegación pasó a Compose (vivía en `SettingsActivity` legacy). Opt-in, default OFF.
- ✅ Fix leak de audio focus al cancelar countdown ("ESTOY BIEN").
- ✅ Fix crash `NetworkOnMainThreadException` en `WifiTransport` al mandar la alerta de emergencia
  (afectaba también baliza y chat, no solo Man-Down).
- ✅ Build roto arreglado de paso: XML legacy huérfano (`layout-land/activity_main.xml`) rompía
  AAPT2 — probablemente lo que colgó la sesión anterior.
- ✅ Probado end-to-end en emulador con caída simulada (acelerómetro inyectado).
- ✅ Commits: `aad9a5e` (fix build) + `9858d8c` (feature + fix crash).
- ⬜ Probar en dispositivo físico (moto real, no emulador).
- ⬜ Calibrar umbrales de `FallDetectionManager.kt` (impacto 25 m/s², quietud 1.5, countdown 30s)
  con uso real — hoy son valores de arranque.

## 🟢 APP-1 — CERRADO (2026-06-24)
- ✅ Sprints 1-9 mergeados en main.
- ✅ Tiles offline OSMDroid: botón en MapScreen.kt (área visible, zoom 10-16) + OfflineTilesDialog.kt.
- ✅ Privacy Policy: `docs/PRIVACY-POLICY.md` + `PrivacyPolicyActivity.kt` + acceso desde `AliasSettingScreen` (Compose Settings).
- ✅ GitHub Release v1.0-beta1 publicado con APK debug: https://github.com/lukaz2004/MotoRFAR/releases/tag/v1.0-beta1
- ✅ Fix ProGuard/R8 — BUILD SUCCESSFUL. Los warnings de Lombok son benignos, no bloquean. (2026-06-24)
- ✅ Signing pipeline configurado: `build.gradle` lee `keystore.properties` local (no commiteado). Template en `KV4PHT/keystore.properties.template`. (2026-06-24)
- ✅ **Keystore generado (2026-07-05)**: `KV4PHT/baqueano-release.jks` (RSA 2048,
  alias `baqueano`, válido hasta 2053), `KV4PHT/keystore.properties` completo con
  contraseñas fuertes generadas al azar (gitignorado, no en el repo). Verificado
  con `keytool -list`. Había un `keystore.properties` viejo a medio armar con
  contraseña débil y ruta rota (`storeFile` sin el `../` que pide `build.gradle`,
  apuntando a un .jks que nunca existió) — reemplazado.
  ⚠️ **Hacer backup de `baqueano-release.jks` YA** (password manager o backup
  externo) — sin él no se puede volver a firmar/actualizar la app en Play Store.
  Las contraseñas están solo en `keystore.properties` en este disco.

## 📱 APP — ÍCONOS (2026-06-30)
- ✅ Todos los mipmap reemplazados con badge Baqueano (5 densidades × 3 archivos).
- ✅ Splash screen automático Android 12+ usa el adaptive icon nuevo.

## 🌐 WEB — GALERÍA REAL (2026-06-30)
- ✅ 6 fotos reales en `_PROYECTO/web/img/` (moto-01 a moto-06).
- ✅ Video demo con logo Baqueano en `_PROYECTO/web/baqueano-demo.mp4`.
- ✅ HTML sección "En acción" actualizado con grilla 3×2, hover/zoom, captions y video.

## 🟢 EST-1 — CARCASA — CERRADO (resuelto fuera de este tracking, confirmado 2026-07-05)
- ✅ Especificaciones (dimensiones 170×75×24mm, sandwich Al 3mm + frame 18mm, tornillería, SMA/USB-C, terminaciones) — cerradas 2026-06-30.
- ✅ Resto del proceso (modelado, corte, impresión de prueba) resuelto por LuKaZ fuera de este chat — no queda nada pendiente de EST-1.

## 🌐 WEB — PUBLICADA (2026-06-24, URL confirmada online 2026-07-05)
- ✅ Rediseño comercial completo: hero, kit, features, frecuencias, app mockup, vehículos, accesorios, specs, CTA.
- ✅ Branding renombrado a **Baqueano** — logo escudo integrado en nav y hero.
- ✅ Cuenta Netlify reclamada, sitio **online en `baqueano.netlify.app`** (verificado 2026-07-05: título "Baqueano — Comunicación grupal VHF donde no hay señal", beta/waitlist funcionando).
- ✅ `docs/index.html` + `docs/assets/` pusheados a main en GitHub.
- ⬜ Dominio propio (opcional, post-lanzamiento).
- ⬜ Reemplazar renders placeholder por fotos/CAD reales cuando estén disponibles.

## 🎛️ Feature — CTCSS/DCS por canal (era "subfrecuencias")
- ✅ ACLARADO: las "subfrecuencias" = CTCSS/DCS (tonos/códigos de squelch), NO frecuencias. Dejan que varios grupos compartan el mismo canal legal sin escucharse. **No mueve la frecuencia → no toca la whitelist, 100% legal (Res 5/2015 = solo 3 freqs).**
- Caveat: CTCSS filtra lo que ESCUCHÁS, no separa el RF (si dos grupos transmiten a la vez, igual colisionan). No es canal privado.
- Viable en el hardware: el SA818 soporta CTCSS (38 tonos) + DCS; el protocolo ya tiene `ctcss_tx`/`ctcss_rx` en `HostDesiredState` (el transporte WiFi ya los lleva gratis).
- ✅ Verificado (2026-07-05): `reconcileDesiredState()` ya pasa `ctcss_tx`/`ctcss_rx`
  a `sa818.group(...)` — línea 312 de `kv4p_ht_esp32_wroom_32.ino`. Ya andaba, no
  hacía falta cambiar código.
- PENDIENTE APP-2: UI para elegir tono/código por canal/grupo.
- Orden: feature aparte, DESPUÉS de validar el transporte FW-3a.

## ✅ FW-3a — COMPLETADO (2026-06-23)
- ✅ Flasheado (maniobra BOOT, partitions.bin re-flasheado).
- ✅ Smoke test básico: `fw3a_smoke.py` → Hello llegó por WiFi (transporte OK).
- ⬜ `fw3a_smoke.py --open-rx` con SA818 físico conectado → ver frames RX_AUDIO por WiFi.
- ⬜ Failsafe: cortar WiFi en plena TX → confirmar que vuelve a RX solo.
  (estos 2 últimos requieren SA818 físico — pendiente de hardware)

## 🟢 Decididos en FW-3a (con nota de revisión a futuro)
- **Clave WPA2 del AP**: hoy `motorfar1234` (PRUEBA, no definitiva). → Hacerla **seteable desde APP-2** (runtime/NVS).
- **Deadman PTT**: corte **instantáneo** por desconexión limpia (`softAPgetStationNum()==0`) + respaldo de 400 ms para fuera-de-rango/app-colgada. → Tunable; revisar el valor tras test real en la moto.
- **Windowing (`COMMAND_WINDOW_UPDATE`)**: se mantiene en FW-3a (queda inerte). → **Quitar/rediseñar en FW-3b + APP-2.**

## 🟢 HW-1 — CERRADO (2026-06-29)
- ✅ SW1/SW2 (botones físicos PTT) marcados DNP en esquemático.
- ✅ J2 conector jack 3.5mm TS PTT externo agregado al esquemático.
- ✅ Logo BAQUEANO en B.SilkS del PCB.
- ✅ **J3 header 2-pin PTT Right** colocado en PCB (x=163.5 y=99.0mm). pad1=/PTT Button Right, pad2=GND. (2026-06-24)
- ✅ **Edge.Cuts revertida a 82.55mm** (38.2 × 82.6mm). LOGO1 movido adentro de la placa. (2026-06-24)
- ✅ **J1 VHF antenna: SMA horizontal → U.FL Hirose** (x=155.575 y=68.326mm, mismo punto, red RF conservada). Pigtail U.FL→SMA bulkhead se rosca en la carcasa. Sin palancas sobre el PCB. BOM: C11519. (2026-06-24)
- ✅ BOM formato JLCPCB (`BOM_JLCPCB.csv`, 38 componentes con LCSC#).
- ✅ CPL formato JLCPCB (`CPL_JLCPCB.csv`, 89 componentes posicionados).
- ⚠️ SA818-V y jack 3.5mm PJ302M NO van en el PCBA — comprar por separado (AliExpress).
- ✅ GUI KiCad — 4 tareas completadas (2026-06-29)
- ✅ Esquemático actualizado: J3 + U5 WROOM-32U-N4
- ✅ Gerbers + BOM + ZIP regenerados
- ⬜ **Pedir a JLCPCB** (próximo paso de hardware)
- ✅ Backup pre-cambios: `AppData\Local\Temp\kv4p-ht-BACKUP-pre-82mm.kicad_pcb`

## 🔵 FW-3b — roadmap (tras validar 3a)
- Split **TCP** (comandos confiables: PTT/freq/estado) / **UDP** (audio). Keepalive durante PTT + reconexión automática.
- Resolver el windowing (redundante sobre TCP).

## 🟡 APP-2 — transporte WiFi (PR #8, 2026-06-27)
- ✅ `WifiTransport.java`: `ConnectivityManager.requestNetwork` + `network.bindSocket()` + loop UDP recv en thread daemon.
- ✅ `Protocol.FrameWriter` (@FunctionalInterface): `Sender` acepta USB o WiFi. WiFi: flowControl=MAX (sin backpressure).
- ✅ `RadioAudioService`: `connectionController` → `attemptWifiConnect()`. `isConnectionReady()` acepta ambos transports. USB solo para FLASHING.
- ✅ Permisos: `CHANGE_NETWORK_STATE` + `ACCESS_NETWORK_STATE`.
- ✅ Build: `assembleDebug` OK.
- ⬜ **Requiere SA818 + ESP32 con FW-3a**: verificar Hello/handshake por WiFi, audio RX/TX real.
- ✅ UI "Conectate a MotoRFAR-HT": hecho hace rato (`WifiConnectBanner.kt`, PR #9) — este bullet había quedado desactualizado.
- ⬜ Config credenciales AP desde app: firmware ya tiene el comando (`COMMAND_HOST_SET_WIFI_PASSWORD`, 2026-07-06), falta el lado app (ver sección de auditoría de seguridad más arriba).
- ✅ UI CTCSS/DCS por canal: hecho 2026-07-06 (`TonesSettingScreen.kt`) — este bullet había quedado desactualizado.
- ⬜ Windowing: quitar/rediseñar en FW-3b (hoy inerte).

## ⚖️ Licencia / Venta — checklist pre-venta
- ⬜ **Homologación ENACOM del equipo** (2026-07-05, real, no opcional). Distinto
  de que el canal sea libre para el usuario (Res. 5/2015, eso ya está): vender
  hardware que usa espectro sin homologar es infracción a la Res. 729/80,
  sancionable bajo Ley 24.240 (multas, decomiso, hasta clausura). Bloqueante
  real antes de cualquier venta comercial (la lista de espera actual no vende,
  no es urgente todavía, pero hay que arrancar el trámite antes de facturar).
  Fuente: enacom.gob.ar/homologacion-de-equipos_p347

- ✅ Decidido: vender es compatible con GPL. Firmware y app siguen siendo forks GPL-3.0; se publica el fuente.
- ✅ Repo con fuente publicado (Release v1.0-beta1).
- ✅ Pantalla "Acerca de / Licencias" — completa. Email removido de Créditos. Solo atribuciones exigidas por licencia. (2026-06-24)
- ✅ Web footer: crédito a kv4p HT / Vance Vagell (KV4P) agregado. (2026-06-24)
- ✅ PCB production-vhf/README.md: atribución CC-BY-SA 4.0 KiCad Libraries + GPL-3.0 upstream. (2026-06-24)
- ⬜ Equipo flasheable por USB, sin secure-boot bloqueado.
- ✅ Fix ProGuard para APK release firmado. (2026-06-24)
- ⬜ Revisión legal de IP antes de vender en serio.
