# BAQUEANO — Prompt de arranque de sesión
> Copiá y pegá esto al inicio de cada chat. Claude lee este archivo + `05_VISION.md` y arranca.

## ⚡ CIERRE 2026-07-09 — mapas offline (diseño pipeline) + testing real Huawei P9 commiteado
- **Mapas offline por provincia:** diseño del pipeline de generación aprobado
  (`docs/superpowers/specs/2026-07-08-mapas-offline-por-provincia-design.md`
  — GitHub Releases, Mapsforge `.map` por provincia vía osmium-tool + Osmosis,
  sin costo de hosteo). **Sin código todavía** — requiere instalar herramientas
  (no están en esta máquina) y correr el pipeline como sesión larga/desatendida
  aparte. Sub-proyecto 2 (ventana in-app con links) espera las URLs reales que
  produzca ese pipeline.
- **Working tree de varias sesiones (probado en Huawei P9 real, nunca
  commiteado) — ahora en 3 commits:**
  1. Fix crash real: "Descargar mapa de Argentina" tiraba `TileSourcePolicyException`
     sin capturar (Mapnik bloquea descarga masiva, `FLAG_NO_BULK`) — ahora avisa
     con Toast. + PTT del Mapa unificado visualmente con el de la pantalla
     principal (`PttVisual` compartido).
  2. **minSdk bajado de 26 a 24** para soportar Android 7 (probado en Huawei P9
     real) — encontró varios crashes reales que el emulador nunca mostró
     (`NoClassDefFoundError`/`AbstractMethodError` en APIs 26+/30+, guardados
     detrás de `SDK_INT` checks). De paso: Man-Down usaba
     `TYPE_ACCELEROMETER` (incluye gravedad, el umbral de quietud nunca se
     cumplía con el teléfono quieto) → `TYPE_LINEAR_ACCELERATION`; umbral de
     impacto 25→50 (disparaba con vibración de motor); WakeLock propio para
     la cuenta regresiva; GPS a veces devolvía (0,0) cacheado sin fix real;
     `isConnected` se marcaba true antes de terminar el handshake real
     (PTT se rechazaba en silencio en esa ventana); race entre reseed de
     canales y carga inicial (`MainViewModel.loadData()`) que podía cachear
     la tabla vacía para siempre.
  3. UI adaptada a pantalla chica real (360×640dp): PTT y DETENCIÓN/REAGRUPAR
     quedaban tapados por la barra de navegación, texto de canales/alertas
     cortado en horizontal. Cartel "Sin conexión con el equipo" movido de la
     pantalla principal a Ajustes > WiFi.
- ⬜ **Nada de esto se probó en moto real** — todo lo de arriba fue verificado
  en el Huawei P9 físico pero en mano, no andando. Sigue pendiente lo de
  siempre (Man-Down en uso real, umbrales de calibración).
- ⬜ Ver si el Huawei P9 fue el único dispositivo de prueba — si hay más
  equipos viejos dando vueltas, valdría la pena repetir el smoke test ahí.

## ⚡ CIERRE 2026-07-06/07 — background fix Man-Down, restricción Emergencia, seguridad, SSID único, retoques UI, animaciones de emergencia
Sesión larga con muchas rondas de feedback en vivo probando el APK. Resumen
por tema (detalle histórico completo estaba en `PENDIENTES.md`, recortado
de ahí tras este cierre):

- **Man-Down dejaba de funcionar en segundo plano — bug real corregido:**
  `FallDetectionManager` y el callback de alertas vivían atados al ciclo de
  vida de `MainActivity`, no al `RadioAudioService` (foreground service).
  Con la app minimizada, `onStop()` apagaba el acelerómetro — exactamente el
  escenario real de uso (teléfono guardado, pantalla apagada). Movido todo
  al Service: countdown, disparo de alerta, notificación de alta prioridad
  con "ESTOY BIEN · CANCELAR", alertas de otros integrantes ahora generan
  notificación propia (antes se perdían en silencio si la app no estaba
  bindeada). Sumado: pantalla propia vía full-screen intent (bypassa
  pantalla bloqueada), atajo de volumen físico (3x VOLUMEN- cancela, pensado
  para pantalla rota tras una caída), countdown escalado según fuerza G del
  golpe, copy en modo potencial ("posible caída", no "caída detectada").
  Verificado en emulador con inyección de sensor real (impacto + quietud).
  ⬜ **Falta:** probar en dispositivo físico — botón "ESTOY BIEN" tocado a
  mano, alerta de otro integrante con la app cerrada (no simulable en
  emulador sin un segundo radio/peer). `onTaskRemoved()` sigue matando el
  Service si el usuario desliza la app fuera de "recientes" — decisión de
  producto pendiente (¿debería sobrevivir a eso, tipo Life360?).

- **Canal Emergencia restringido a uso real:** chat libre, STOP/Reagrupamiento
  y el balizado de rutina ahora se bloquean si el canal activo es 140.970
  (antes nada lo impedía). Tonos CTCSS por defecto para Principal/Alternativo
  (Emergencia sin tono a propósito). Pantalla de selección de tonos nueva
  (`TonesSettingScreen.kt`) con explicación en criollo de cómo funciona el
  tono y sus límites reales.

- **Auditoría de seguridad aplicada:** whitelist TX también en `sa818.group()`
  (defensa en profundidad), clave WPA2 única por equipo (ya no hardcodeada),
  deadman de PTT desacoplado del tráfico UDP genérico, validación de origen
  UDP, fix parsing APRS, copy de privacidad Man-Down, headers de seguridad
  en la web. **SSID único por equipo** (`Baqueano-XXXX`, derivado del efuse
  MAC) para que dos equipos cercanos no se confundan de red — motivado por
  pregunta real de seguridad del usuario (dos motos a 20m con el mismo SSID
  fijo). Configurable desde la app (Ajustes > WiFi) junto con la clave.

- **Retoques de UI pedidos en vivo probando el APK real** (varias rondas de
  feedback, cada una encontró algo real):
  - Descarga de mapas offline unificada (había dos entradas, una placeholder).
  - Canal "GRUPO" renombrado a "PRINCIPAL" (juega con "ALTERNATIVO").
  - Mapa: sacados los botones +/- de zoom (redundantes, el pellizco ya
    hacía zoom); RUMBO/MI GPS movidos junto al HUD de coordenadas arriba
    (antes competían con el PTT, riesgo de toque accidental manejando);
    WAYPOINT movido ahí también (estaba fuera de lugar en pantalla principal).
  - "Borrar ruta guardada" movido de la barra superior a Ajustes.
  - Emergencia activa ahora se nota de verdad: botón de canal se rellena de
    rojo sólido, cartel de frecuencia se pone rojo, botón PTT y sus anillos
    se ponen rojos, ecualizador se pone rojo, botón de confirmación
    ("⚠ EMERGENCIA") parpadea — todo sincronizado a un solo estado derivado
    (`MainUiState.isEmergencyActive`). Esto reveló un bug real: el flujo
    automático de alerta (mantener 2s) solo retunaba el hardware, nunca
    avisaba a la UI — corregido en `transmitGroupAlert()`.
  - Botón PTT: "PTT"/"TX" → "PUSH TO TALK"/"TRANSMITIENDO", con borde blanco
    detrás del texto (se perdía contra el degradé de fondo). Mismo
    tratamiento en el PTT compacto del Mapa.
  - Ícono de Baqueano agregado a la barra superior (mipmap real, no el XML
    de ícono adaptativo — eso crasheaba `painterResource`).
  - Tono CTCSS visible en pantalla principal junto a "MHz · FM · SIMPLEX".
  - **Bug real encontrado en producción (probado en celular):** el reseed de
    canales (`preloadArgentinaChannelsIfNeeded`) hacía cada `delete()` y el
    `insertAll()` como operaciones sueltas sin transacción — la LiveData de
    `getAll()` se invalidaba después de cada una, y la pantalla principal
    podía agarrar la tabla vacía a mitad del reseed (nombre de canal caía a
    "SIMPLEX", tono sin canal para mostrar). Corregido con
    `db.runInTransaction(...)`.
  - Build verificado (`gradlew assembleDebug`/`assembleRelease`) y
    confirmado visualmente en emulador en cada ronda.

- ⬜ **Hallazgo nuevo, no resuelto:** `MainViewModel.channelMemories` es un
  snapshot cargado una sola vez (`loadData()`), no una LiveData reactiva de
  Room — queda desincronizado del canal real que usa `ChannelRow` (que sí
  es reactivo). Esto hace que `tuneToChannel()` (al tocar un canal a mano)
  a veces muestre "SIMPLEX" en vez del nombre real. No afecta el flujo
  automático de Emergencia (usa un string literal, no este lookup), pero
  conviene unificarlo a la fuente reactiva en una próxima sesión.
- ⬜ **Animaciones de emergencia sin probar con radio real conectada:** el
  flujo automático (mantener 2s el botón "⚠ EMERGENCIA") requiere
  `uiState.isConnected == true`, que el emulador no tiene sin hardware real
  — se verificó el mecanismo (mismo estado derivado) tocando manualmente el
  canal Emergencia, que sí disparó todo correctamente, pero falta confirmar
  el flujo automático de punta a punta en dispositivo físico con equipo.
- ⬜ Renombrar `ic_launcher_moto.png` → el archivo dice "moto" pero la imagen
  ya es el escudo Baqueano (rebrand viejo, solo el nombre del archivo quedó
  desactualizado). Cosmético, no bloquea nada.
- ⬜ No existe pantalla de historial de rutas — solo se guarda/muestra la
  última sesión por alias; "Borrar ruta guardada" borra todo, no por viaje.
  Sería feature nueva si se pide.

## ⚡ CIERRE 2026-07-05 (tercera parte) — diseño navegación + limpieza de pendientes + keystore + push
- **Navegación turn-by-turn propia — diseño aprobado, sin código todavía:**
  spec completo en `_PROYECTO/NAV_TURN_BY_TURN_DISENO.md`. Motor elegido:
  **BRouter** (Java puro, sin NDK, GPL-3.0 — mismo esquema de licencia que ya
  usa Baqueano; datos de ruteo pre-procesados por el proyecto, no hace falta
  pipeline OSM propio). Tamaño real verificado: ~400-600MB para cubrir toda
  Argentina (contra tamaños reales de tiles `.rd5`, extracto Geofabrik y
  paquete OsmAnd). Estrategia de descarga en 2 etapas para no bloquear nunca
  la función crítica (radio/grupo): tile local obligatorio al primer inicio +
  resto del país en background. Voz (TTS) + flecha visual, con **ducking** de
  audio sobre el radio VHF en vez de cortarlo. **El propio spec deja 4 puntos
  para pulir antes de armar el plan de implementación** (ver sección "Abierto
  para pulir" del archivo) — no dar por definitivo sin repasarlos.
- **Correcciones de estado real, quedaba desactualizado:**
  - EST-1 carcasa: estaba anotada con tareas pendientes (modelado, corte,
    impresión) que LuKaZ ya había resuelto fuera de las sesiones de chat —
    marcada CERRADA.
  - Web Netlify: estaba anotada con "reclamar cuenta + renombrar" pendiente;
    verificado por fetch real que `baqueano.netlify.app` ya está online con la
    cuenta reclamada — corregido en `PENDIENTES.md` y `NEXT_SESSION.md`.
- **Copy de la web corregido:** las 3 menciones de "30 segundos" fijo en
  Man-Down (ya no es así desde el countdown variable por fuerza G) → "cuenta
  regresiva" sin número fijo. Copy de CTCSS ("Sin interferencias, sin
  confusión...") sobrevendía → corregido a lo que realmente hace (filtra lo
  que escuchás, no separa el RF real).
- **CTCSS/DCS por canal — verificado, no hacía falta tocar código:**
  `reconcileDesiredState()` ya pasa `ctcss_tx`/`ctcss_rx` a `sa818.group(...)`
  (línea 312 de `kv4p_ht_esp32_wroom_32.ino`). Quedaba anotado como "no
  verificado" desde hace rato — ya funciona.
- **Keystore de release generado:** `KV4PHT/baqueano-release.jks` (RSA 2048,
  alias `baqueano`, válido hasta 2053) + `KV4PHT/keystore.properties` con
  contraseña fuerte generada al azar (gitignorado). Encontrado y reemplazado
  un `keystore.properties` viejo a medio armar (contraseña débil, ruta rota,
  apuntaba a un `.jks` que nunca existió). Primer intento del certificado puso
  `OU=MotoRFAR` por error de Claude — corregido y regenerado con todo en
  `Baqueano` antes de que se usara para firmar nada. Guía de backup (Bitwarden
  como Nota Segura, no Login) dada a LuKaZ — **el backup físico del `.jks` en
  un segundo lugar (fuera de esta PC) queda como acción suya, no está hecho
  todavía.**
- **Push a GitHub:** los ~30 commits locales de la sesión larga anterior +
  los de hoy quedaron subidos a `origin/main` (`ce76180..eee7010`). Ya no hay
  nada pendiente de pushear.
- **Sesión termina acá.** LuKaZ puede retomar mañana a la mañana si tiene
  tiempo — punto de partida sugerido: repasar los 4 puntos de "Abierto para
  pulir" del spec de navegación y, si cierran, pasar a armar el plan de
  implementación (`writing-plans`).

## ⚡ CIERRE 2026-07-05 (segunda parte) — web comercial + registro de ruta
- **Web reconstruida de punta a punta** sobre `docs/index.html` (la que ya estaba
  en vivo, más madura de lo que parecía — ver auditoría en
  `AUDITORIA_MARKETING_WEB.md`): íconos SVG en vez de emoji, animaciones de
  hover/mouse, sección Comparativa nueva (vs. handy VHF e intercom Bluetooth,
  con gancho creativo vs. armar un grupo de Discord/Zello por LTE), "Cómo
  funciona" movido arriba del todo (justo después del hero), copy menos técnico
  (sin nombrar módulo/chip en el flujo principal), alimentación corregida a
  USB-C en todos lados (ya no dice 12V, esa idea se había descartado), PTT
  aclarado como término (Push To Talk) y corregido para no asumir manillar en
  pasos genéricos, ejemplo de "200 motos" en CTCSS corregido (sobrevendía).
  `_PROYECTO/web/index.html` y `docs/index.html` quedan sincronizados — ver
  `_PROYECTO/web/README.md` para el flujo (se edita ahí, se copia a docs/).
- **Registro de ruta arreglado en la app** (no solo copy): antes mezclaba todo
  el historial en una sola línea infinita sin forma de borrarla. Ahora agrupa
  por sesión (`RoutePoint.sessionId`, migración Room 7→8) y hay botón de borrar
  con confirmación. Build verificado (`assembleDebug` OK).
- **Hallazgo legal real, no cosmético:** homologación ENACOM del equipo es un
  trámite obligatorio y separado de que el canal sea libre para el usuario —
  vender sin homologar es infracción sancionable (Res. 729/80, Ley 24.240).
  El copy "Legal por diseño" sobrevendía esto, corregido. Homologación queda
  como bloqueante real antes de vender de verdad (no urgente mientras solo haya
  lista de espera) — anotado en `PENDIENTES.md`.
- **Pendientes anotados, sin construir:** Man-Down con countdown variable según
  fuerza G del golpe; marcar y compartir POIs propios (reusaría el botón
  WAYPOINT existente); navegación turn-by-turn propia (feature grande, no se
  puede inyectar en la navegación de Google Maps desde afuera); exportar ruta a
  GPX; adaptar frecuencias a otros países (técnicamente trivial en el firmware,
  el problema es la investigación legal país por país, no el código).
- **Sin pushear a GitHub** — todos los commits de web quedaron locales en
  `main`, a la espera de que LuKaZ revise antes de subir.
- **Sesión larga, costo alto** (~$120) por la cantidad de idas y vueltas de
  diseño/copy en vivo. Para la próxima: si se retoma la web, arrancar leyendo
  este cierre + `AUDITORIA_MARKETING_WEB.md` antes de tocar nada.

## ⚡ CIERRE 2026-07-05 (primera parte) — sesión de orden (nada de código nuevo, solo prolijidad)
- **Los 3 frentes sueltos del cierre anterior, commiteados:** rediseño web (`c942c00`),
  rebrand de íconos (`1461aaa`), limpieza Compose + rename MotoRFAR→Baqueano (`1905c96`).
  Working tree limpio salvo `imegenes baqueano edit/` (16MB, material fuente de video, no
  se toca).
- **Rescatado un cierre real de HW-1 del 2026-07-04 que nunca quedó anotado** (pasó el
  mismo día que Man-Down y se perdió en el cierre de esa sesión): stub RF J1 borrado,
  C9→R2 conectado (bug heredado real), J2 corregido en esquemático, DRC final en 90
  violaciones heredadas. Commiteado en `2d1555f` junto con `05_VISION.md` (aprobado el
  02/07, tampoco se había commiteado) y el schema Room v7. Detalle completo en
  `HW1_CIERRE.md`. Único pendiente real: C15/C32 (pin2 a GND), requiere sesión en vivo.
- **Limpieza `_PROYECTO/`:** borrados 15 archivos de iteración de DRC/ERC del 07-04
  (`drc_verify_2026-07-04*.json` a-j, `drc_final_2026-07-02.json`, `erc_after_j2.json`,
  `erc_verify_now.rpt`, `netlist_check.net`) — todos superados por los reportes finales
  ya commiteados arriba. También `tools_cierre_hw1.py` (script de un solo uso, ya
  ejecutado) y `ESTADO_SESION.txt` (resumen viejo del 28/06, redundante con este archivo).
- **Man-Down — sigue igual que el 04/07:** falta prueba en dispositivo físico y calibrar
  umbrales de `FallDetectionManager.kt`. Bloqueado en hardware, no hay nada más para
  hacer sin la moto real — no reabrir sin datos nuevos.

## ⚡ CIERRE 2026-07-04 — estado más reciente (si contradice algo de abajo, MANDA ESTO)
- **Sesión anterior colgada:** un `layout-land/activity_main.xml` legacy (huérfano de antes de la
  migración a Compose, nadie lo referenciaba) rompía el merge de recursos de AAPT2 con un error
  críptico ("layout file should've changed") y probablemente eso colgó el chat previo. Borrado junto
  con `MainActivityLegacy.java` y `layout/activity_main.xml` (mismo legacy, también muerto).
  Commit `aad9a5e`.
- **Man-Down — countdown completo y probado (commit `9858d8c`):** el toggle vivía en el
  `SettingsActivity` legacy, que ya no es alcanzable desde la navegación Compose actual → la
  función estaba wireada (countdown, overlay, tono, DB) pero **inaccesible para el usuario**.
  Agregado el toggle en `AliasSettingScreen.kt` (opt-in, default OFF). De paso, dos bugs reales
  encontrados probando en emulador:
  - Fuga de audio focus al cancelar ("ESTOY BIEN"): liberaba con una API vieja que no hacía nada;
    el foco exclusivo (silencia música/GPS) quedaba trabado para siempre.
  - **Crash real:** `WifiTransport` mandaba el UDP en el mismo thread que lo llamaba →
    `NetworkOnMainThreadException` justo cuando el countdown llega a 0 y dispara
    `transmitGroupAlert(EMERGENCY)` — la app se caía exactamente al mandar la alerta de
    emergencia. Ahora corre en un executor propio (aplica también a baliza y chat).
  - Probado end-to-end en emulador (Pixel_10_Pro_XL, caída simulada por acelerómetro): toggle,
    countdown, cancelar, y countdown completo → alerta se manda sin crashear.
  - ⬜ Falta: probar en dispositivo físico (el emulador valida la lógica, no el umbral de impacto
    real en moto). Umbrales de `FallDetectionManager` (30s countdown, impacto 25 m/s², quietud
    1.5) son valores de arranque sin calibrar con uso real.
- **⚠️ Working tree con 3 frentes sueltos sin commitear** (no tocados esta sesión, dejados
  adrede — cada uno es su propia etapa):
  1. Rediseño visual completo de `_PROYECTO/web/index.html` (tipografía Rajdhani, paleta
     verde retocada, título ya dice Baqueano).
  2. Rebrand de íconos: los 5 mipmaps (`ic_launcher*`) recomprimidos/actualizados.
  3. Limpieza menor Compose: `MainViewModel.java` (executor pool), `MapScreen.kt` (Polyline de
     ruta), `activity_terms.xml` (texto MotoRFAR→Baqueano).
  Correr `git status` antes de asumir árbol limpio en el próximo chat.
- **Emulador:** quedó corriendo y se cerró solo (o se lo mató) durante la sesión — no hay AVD
  activo al cierre.

## ⚡ CIERRE 2026-07-02
- **HW-1:** DRC completo corrido (CLI + GUI de LuKaZ, misma foto). **Ruteo PTT de LuKaZ: LIMPIO, cero errores.**
  Quedan 6 retoques de GUI (~25 min) con coordenadas exactas en `HW1_CIERRE.md` → hacerlos, re-correr
  DRC, y HW-1 pasa a CERRADO definitivo con evidencia. Las 169 marcas heredadas están clasificadas
  como no bloqueantes en ese archivo: NO se trabajan, NO se re-auditan.
- **05_VISION.md:** creado y aprobado por LuKaZ. Se lee en cada arranque junto a este archivo.
- **Esquemático:** BOM UTF-8 (3 bytes inválidos) eliminado; backup `kv4p-ht.kicad_sch.bak-BOM-2026-07-02`.
- **Conectores MCP:** Spotify / Shopify / Vercel / GDrive DESCONECTADOS (2026-07-02). Ignorar menciones viejas.
- **Python del sistema: FUNCIONA** (3.14.6 + pip 26.1.2, verificado 2026-07-02). Regla "Python roto" obsoleta.
  PlatformIO sigue en pio-venv (el build del firmware no se migra).
- **PRÓXIMA ETAPA — REBRAND total → Baqueano (chat propio, 1 chat = 1 etapa):** renombrar carpetas,
  app, SSID (MotoRFAR-HT → Baqueano-HT), web, docs. MotoRFAR muere como nombre: el producto ya no es
  solo para motos. ⚠️ Límite GPL: los headers de copyright kv4p/Vance Vagell dentro de los archivos
  fuente y el texto de la licencia NO se tocan (atribución obligatoria); todo lo demás se renombra.
- **Pendiente de organización:** crear `CERRADO.md` / `HISTORIAL.md` / `IDEAS.md` + adelgazar este
  archivo (mover historia vieja a HISTORIAL). Primera tarea del próximo chat de organización.
- **Alerta web:** SIGUE VIGENTE (ver abajo), sin cambios hoy.

---

## CONTEXTO (no leer más archivos salvo que la tarea lo pida)

**Proyecto:** MotoRFAR HT (fork kv4p, GPL-3.0) — radio VHF/UHF que convierte un Android en transceptor ham, montado en moto/4x4. Nombre comercial: **Baqueano**.

**Rutas clave:**
- Proyecto: `C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\_PROYECTO\`
- Firmware: `C:\Users\lukaz\OneDrive\Escritorio\kv4p-ht-main\kv4p-ht-main\microcontroller-src\`
- App Android: `C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\KV4PHT\`
- PCB: `...\kv4p-ht-main\pcb\v2.0e\kv4p-ht\kv4p-ht.kicad_pcb`
- Web: `C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\docs\` → publicada en Netlify

**Frecuencias TX permitidas (Res 5/2015 MTTT):** 139.970 · 138.510 · 140.970 MHz — whitelist en firmware ES LA AUTORIDAD FINAL.

---

## ESTADO AL 2026-07-02

### 🔴 ALERTA — WEB en estado divergente, sesión de auditoría no persistida a disco
- `docs\index.html` (deployado en Netlify) sin cambios desde 29/06 01:18 — 71K, 1238 líneas.
- `_PROYECTO\web\index.html` (fuente/staging) sin cambios desde 30/06 01:02 — 32K, 832 líneas.
- Chat "Subir fotos al proyecto web" (activo hasta 01/07 tarde-noche) hizo fetch a baqueano.netlify.app, auditoría completa de diseño/contenido, y reconstruyó la página — pero trabajó en el filesystem efímero del contenedor (`/home/claude/baqueano-web-assets/`) y nunca copió el resultado final a estos paths vía Desktop Commander. Ese trabajo puede estar accesible solo reabriendo ese chat puntual.
- **Antes de tocar la web de nuevo:** buscar ese chat con `conversation_search` ("auditoria diseño landing baqueano") y confirmar si el HTML final es recuperable. Si no, rehacer la auditoría — esta vez cerrando con copia a disco.
- Hasta resolver esto, los dos `index.html` siguen siendo versiones distintas — no asumir cuál es la vigente.

### 🟡 Conectores MCP — estado real (verificado 2026-07-02)
- **Vercel**: registrado pero no autorizado en esta sesión (`No approval received` al listar teams). Si se quiere usar para deploy/debug de la web, hay que reautorizarlo desde la configuración de Claude primero.
- **Google Drive**: mismo estado — registrado, no autorizado.
- **Shopify**: conectado y con herramientas activas, sin uso todavía en este proyecto. Relevante recién en la etapa de venta comercial.
- **Spotify**: conectado, sin relevancia para este proyecto.

---

## ESTADO AL 2026-06-30

### Cambios de la sesión 2026-06-30
- ✅ **App ícono**: Todos los mipmap (mdpi→xxxhdpi) reemplazados con badge Baqueano — `ic_launcher.png`, `ic_launcher_moto.png`, `ic_launcher_round.png` en las 5 densidades. Splash screen automático en Android 12+.
- ✅ **Web galería real**: 6 fotos reales en `_PROYECTO/web/img/` (moto-01 a moto-06, ~50-200KB c/u). Video demo con logo Baqueano en `_PROYECTO/web/baqueano-demo.mp4` (1.6MB). HTML actualizado con grilla 3×2 real, hover/zoom y captions.
- ✅ **Videos procesados**: Watermark Gemini tapado (blur quirúrgico + logo Baqueano overlay) en los 2 videos originales (`imegenes baqueano edit/mp4.mp4` y `mp4 (1).mp4`).
- ✅ **Diseño carcasa EST-1**: Especificaciones completas definidas:
  - Construcción sandwich: 2 tapas aluminio 3mm + frame 18mm corte por agua
  - Dimensiones externas: **170 × 75 × 24mm**
  - 8 tornillos M3 en la tapa (3 por lado largo, 1 por lado corto)
  - Frente: botón M16 Short Body High Head con LED azul (esquina superior izquierda, 10mm de margen), USB-C centrado, tapa PETG 60×20×4mm antena WiFi (parte inferior)
  - Borde superior: SMA VHF (15mm del borde derecho, 15mm del borde frontal) con pigtail RG316 → antena varilla inox 50cm
  - Sin conector 5V separado (alimenta por USB-C)
  - Terminaciones: aluminio arenado crudo ó anodizado negro sobre arenado
- ✅ **Prompt render prototipo**: Desarrollado prompt iterativo para Gemini Image — estrategia por capas (caja+tornillos+USB-C primero, luego botón, luego antenas)

---

## ESTADO AL 2026-06-28

### Cambios de la sesión 2026-06-28
- ✅ **Web:** baqueano.netlify.app EN VIVO — hero animado, capturas reales de la app, logo
- ✅ **App (Gemini fixes, commit 6082bfc):** 3 bugs corregidos:
  - `RadioAudioService.java` línea 209: carácter `b` suelto → eliminado
  - `RadioAudioService.java`: `MainActivityLegacy` (borrada) → `MainActivity`
  - `FallDetectionManager.kt`: `SENSOR_DELAY_NORMAL` → `SENSOR_DELAY_GAME` (20ms)
  - TxWhitelist verificado ✅
- ✅ **App (commit dd06575):** Toggle Man-Down en Ajustes — desactivado por defecto (evita falsas alarmas en baches)
- ⬜ **Email profesional:** info@baqueano.com → ver `_PROYECTO/GUIA_EMAIL_CHATBOT.md`
- ⬜ **WhatsApp chatbot:** ManyChat → ver `_PROYECTO/GUIA_EMAIL_CHATBOT.md`

---

## ESTADO AL 2026-06-27

| Etapa | Estado | Bloqueante |
|-------|--------|------------|
| F0 WiFi | ✅ SoftAP `MotoRFAR-HT`, UDP 4210, IP 192.168.4.1 | — |
| FW-1 línea base | ✅ compila + flashea | — |
| FW-2 whitelist TX | 🟡 código+flash OK | test RF real con SA818 |
| FW-3a WiFi transport | ✅ flasheado + Hello OK | `--open-rx` + failsafe (SA818) |
| FW-3b TCP/UDP split | ⬜ roadmap | tras FW-3a validado completo |
| FW-4 PTT J2 | 🟡 auditado | test físico con SA818 |
| HW-1 PCB | ✅ CERRADO (2026-06-29) | — |
| HW-2/3/4 | ⬜ | tras HW-1 |
| APP-1 | ✅ CERRADO | — |
| APP-2 WiFi client | 🟡 transport + UI guide hechos (PR #8+#9) | test real con SA818+ESP32 |
| APP-3 disclaimer | ⬜ | tras APP-2 |
| EST-1 carcasa | ⬜ arrancable | — |
| WEB | ✅ publicada Netlify | reclamar cuenta + renombrar a baqueano.netlify.app |
| INT | ⬜ | todo lo anterior |

---

## PENDIENTES CONCRETOS (sin hardware)

**HW-1 — 4 tareas GUI KiCad antes de ordenar JLCPCB:**
1. Rutear 3 pistas cortas PTT Right: R14-pad1 → C33-pad1 → U5-pad29 → J3-pad1
2. Verificar traza RF J1 (U.FL Hirose — pad central más chico que SMA anterior)
3. Verificar posición LOGO1 (y=140) y J3 (x=163.5, ~1.6mm borde derecho)
4. DRC completo (diálogo se abre off-screen: Win+Shift+Flecha)
5. Actualizar esquemático: agregar J3 + cambiar U5 a WROOM-32U-N4
6. Regenerar Gerbers + BOM + ZIP

**APP — pendientes sin hardware:**
- ✅ Fix ProGuard/R8 → APK release firmado (2026-06-24)
- ✅ Pantalla "Acerca de / Licencias" (2026-06-24)
- ✅ WifiTransport UDP + Protocol.FrameWriter (PR #8, 2026-06-27)
- ✅ WifiConnectBanner — guía al usuario a la red MotoRFAR-HT (PR #9, 2026-06-27)
- ⬜ Verificar Hello/handshake + audio RX/TX por WiFi (requiere SA818 físico)

**WEB:**
- Reclamar cuenta Netlify (si expiró: `netlify deploy --dir=docs --prod --allow-anonymous`)
- Renombrar URL a `baqueano.netlify.app`

**Con SA818 físico (desbloqueador hardware):**
- FW-3a `--open-rx` → ver frames RX_AUDIO por WiFi
- FW-3a failsafe → cortar WiFi en TX, confirmar vuelta a RX
- FW-2 test RF → TX fuera de whitelist = bloqueado en hardware
- FW-4 → J2 dispara TX

---

## TOOLCHAIN (para no perder tiempo)

**PlatformIO (ESP32):**
```
[Environment]::SetEnvironmentVariable('PLATFORMIO_BUILD_DIR','C:\Users\lukaz\pio-build','Process')
& 'C:\Users\lukaz\pio-venv\Scripts\pio.exe' run -e esp32dev -d 'C:\Users\lukaz\OneDrive\Escritorio\kv4p-ht-main\kv4p-ht-main\microcontroller-src'
```
⚠️ Python system installer ROTO — usar SIEMPRE `C:\Users\lukaz\pio-venv\Scripts\pio.exe`

**Flash ESP32 (COM9):** maniobra BOOT manual — sostener BOOT mientras esptool conecta (DTR no baja GPIO0 solo). Flash completo (bootloader + partitions + firmware).

**Android build:**
```
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
cd MotoRFAR-MTTT\KV4PHT && .\gradlew assembleDebug
```
Emulador: `C:\Users\lukaz\AppData\Local\Android\Sdk\emulator\emulator.exe -avd Pixel_10_Pro_XL`

**KiCad CLI:** `C:\Users\lukaz\AppData\Local\Programs\KiCad\10.0\bin\kicad-cli.exe`
⚠️ NUNCA rutear cobre crítico PCB por script — siempre GUI KiCad.

---

## LINKS RÁPIDOS
- Release v1.0-beta1: https://github.com/lukaz2004/MotoRFAR/releases/tag/v1.0-beta1
- Web Netlify: https://baqueano.netlify.app (cuenta reclamada, URL final — verificado online 2026-07-05)
- PCB backup pre-cambios: `AppData\Local\Temp\kv4p-ht-BACKUP-pre-82mm.kicad_pcb`
- PCB backup original: `AppData\Local\Temp\kv4p-ht-ORIGINAL.kicad_pcb`

---

## CÓMO ARRANCAR CADA ETAPA (si querés el detalle completo)

| Tarea | Decile a Claude |
|-------|----------------|
| HW-1 GUI KiCad | "Leé `_PROYECTO\ramas\HW_hardware.md` y cerramos HW-1" |
| Fix ProGuard | "Leé `_PROYECTO\ramas\APP_app.md` y arreglamos ProGuard/R8" |
| Pantalla licencias | "Leé `_PROYECTO\ramas\APP_app.md` y agregamos pantalla Acerca de/Licencias" |
| FW-3a validación SA818 | "Leé `_PROYECTO\ramas\FW_firmware.md` (FW-3a) y hacemos validación con SA818" |
| APP-2 WiFi client | "Leé `_PROYECTO\ramas\APP_app.md` (APP-2) y `_PROYECTO\01_CONTRATOS.md`" |
| EST-1 carcasa | "Leé `_PROYECTO\ramas\EST_estetica.md` y arrancamos EST-1" |
| Web Netlify | "Leé `_PROYECTO\web\README.md` y reclamamos/renombramos el sitio" |

---

## REGLAS QUE NO SE NEGOCIAN
- 🔴 Nunca script para rutear cobre crítico en KiCad → GUI siempre.
- 🔴 Whitelist TX: firmware es la autoridad final (no confiar solo en la app).
- 🔴 Todo trabajo hecho en filesystem efímero de una sesión (bash_tool, `/home/claude/...`) se copia al disco real vía Desktop Commander ANTES de cerrar el chat. Si no se copia, se pierde — ya pasó con el rebuild de la web (ver ESTADO AL 2026-07-02).
- 🟢 Backup del PCB ANTES de cualquier cambio.
- 🟢 Cada chat = 1 etapa. Los contratos (`01_CONTRATOS.md`) son la memoria compartida entre ramas.
- 🟢 Python system installer roto en esta máquina — siempre pio-venv.
