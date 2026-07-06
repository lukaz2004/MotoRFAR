# PENDIENTES — MotoRFAR MTTT

> Lista viva de cosas a no olvidar durante el rewrite. Actualizar al cerrar/abrir items.
> Última edición: 2026-07-06 (Man-Down y alertas movidas al Service — bug de
> segundo plano corregido, ver sección de arriba).

## 🚨 Man-Down y alertas dejaban de funcionar en segundo plano — CORREGIDO (2026-07-06)
Pregunta del usuario: "si la app está en segundo plano y alguien manda una
alerta, ¿hay algún aviso?" Investigando esto encontré un bug más grave: **el
acelerómetro de Man-Down se apagaba solo** cuando la app dejaba de estar en
pantalla.

**Causa raíz:** `FallDetectionManager` (el acelerómetro) y el callback de
alertas recibidas vivían atados al ciclo de vida de `MainActivity`
(`onStart()`/`onStop()`), no al `RadioAudioService` (el foreground service que
sigue corriendo con la app en segundo plano). `onStop()` literalmente hacía
`fallDetectionManager?.stop()` y desvinculaba el callback (`unbindService`) —
exactamente el escenario real de uso (teléfono en el bolsillo/soporte, pantalla
apagada) desactivaba la función que más importa ahí.

**Corregido, movido todo a `RadioAudioService.java`:**
- ✅ `FallDetectionManager` vive y corre en el Service (`setManDownEnabled()`),
  ya no se detiene al salir de la app.
- ✅ Cuenta regresiva + disparo de la alerta real (`transmitEmergencyAlert()`)
  corren en el Service, independiente de si `MainActivity` está abierta.
- ✅ Notificación de alta prioridad (canal `IMPORTANCE_HIGH`, sonido+vibración
  por default) durante la cuenta regresiva, con botón "ESTOY BIEN · CANCELAR"
  que funciona desde la bandeja de notificaciones sin abrir la app.
- ✅ **Alertas de OTROS integrantes (EMERGENCIA/DETENCIÓN/REAGRUPAMIENTO)
  ahora generan notificación independiente** — antes solo llegaban vía un
  callback que se volvía no-op si la app no estaba bindeada (silencio total
  en segundo plano).
- ✅ `MainActivity` sigue mostrando el overlay visual de cuenta regresiva
  cuando está abierta (vía nuevo callback `manDownCountdownTick`), pero ya no
  es la autoridad — es solo un espejo de UI.
- Build verificado: `gradlew assembleDebug` → BUILD SUCCESSFUL.
- ✅ **Verificado en emulador (2026-07-06, Pixel_10_Pro_XL):** caída simulada
  con `adb emu sensor set acceleration` (impacto + quietud real, no el valor
  de gravedad ~9.8 que usé al principio y que el algoritmo NO cuenta como
  "quieto") con la app en segundo plano. Confirmado por `dumpsys sensorservice`
  que el listener sigue registrado tras backgroundear. Notificación de alta
  prioridad aparece con el texto y countdown correctos. Sin cancelar, la
  alerta se dispara sola: logs confirman envío de paquete AX25 + "Beaconing
  position via APRS" + segundo paquete, dos corridas independientes, sin
  crash (proceso vivo en ambas). No pude tapear el botón "ESTOY BIEN" con
  precisión vía `adb` (el peek de la notificación colapsa rápido) — es
  `NotificationCompat.addAction()` estándar, no código custom, pero falta
  confirmarlo con el dedo en un dispositivo real.
- ⬜ **Pendiente real:** probar en dispositivo físico Android (no emulador) —
  el botón "ESTOY BIEN · CANCELAR" tocado a mano, y una alerta de otro
  integrante llegando con la app cerrada (esto último no se pudo simular en
  el emulador por falta de un segundo radio/peer).
- ⬜ **Relacionado, no resuelto:** `onTaskRemoved()` sigue haciendo
  `stopSelf()` — si el usuario desliza la app fuera de "apps recientes"
  (gesto distinto a solo minimizarla), el Service entero se mata y Man-Down
  se apaga igual. No se tocó en esta pasada; es una decisión de producto
  aparte (¿Man-Down debería sobrevivir incluso a eso, como apps tipo Life360?).

## ⚖️ Canal Emergencia — uso restringido a emergencias reales (2026-07-06)
Pregunta real de un radioaficionado: ¿el balizado/alertas de la app usan el
canal Emergencia (140.970) o los canales Principal/Alternativo? Respuesta:
Man-Down y la alerta manual de emergencia SIEMPRE usan 140.970 (correcto, es
una emergencia real). Pero STOP/Reagrupamiento, el chat libre y el balizado
de rutina podían salir por 140.970 si el usuario dejaba el radio sintonizado
ahí — nada lo impedía. Corregido:
- ✅ Chat libre y STOP/Reagrupamiento bloqueados si el canal activo es
  Emergencia (`MainActivity.kt`, `notifyEmergencyChannelBlocked()`).
- ✅ Balizado APRS de rutina (el automático cada 5 min) se salta si el canal
  resuelto es Emergencia (`RadioAudioService.java`, scheduler).
- ✅ Nuevo método `TxWhitelist.isEmergencyFreq()` centraliza el chequeo.
- ✅ Cartel de recordatorio al abrir la app (no al volver de segundo plano):
  explica que 140.970 es uso exclusivo M.T.T.T./ENACOM. `AlertDialog` con
  `remember` (no Saveable) — se resetea solo si la Activity se crea de cero.
- ✅ **Diálogo de confirmación conectado (2026-07-06)**: `getConfirmationTitle()`/
  `getConfirmationText()` ahora se usan de verdad, pero solo para STOP/REGROUP
  (un tap simple sin protección podía mandar una falsa alarma al grupo).
  EMERGENCY queda sin tocar — ya tiene su propia confirmación deliberada
  (`EmergencyConfirmButton`, hold de 2 segundos), un diálogo ahí sería
  fricción redundante en una emergencia real, no una mejora de seguridad.
- ✅ **Tonos CTCSS por canal (2026-07-06)**: `ArgentinaChannels.java` ya
  asignaba `ChannelMemory.txTone`/`rxTone` (existentes, usados hoy para
  repetidoras — no hizo falta migración de Room). Grupo=100.0Hz,
  Alternativo=123.0Hz. Emergencia queda SIN tono a propósito (debe ser
  audible para cualquiera). Marcador de seed subido a `v7_channels_ctcss`
  para que instalaciones existentes se re-seedeen solas. Sin UI para que el
  usuario elija otro tono — queda como pendiente si hace falta más adelante.
Build verificado: `gradlew assembleDebug` → BUILD SUCCESSFUL.

## 🔒 Auditoría de seguridad — correcciones aplicadas (2026-07-06)
Reportes completos: `AUDITORIA_SEGURIDAD_APP.md`, `AUDITORIA_SEGURIDAD_FIRMWARE.md`,
`AUDITORIA_SEGURIDAD_WEB.md` (cada uno con su sección "Estado de correcciones").
Resuelto: whitelist TX también en `sa818.group()` (defensa en profundidad),
clave WPA2 única por equipo (ya no hardcodeada/compartida), deadman de PTT
desacoplado del tráfico UDP genérico, validación de origen UDP en la app,
fix de parsing APRS, copy de privacidad de Man-Down, headers de seguridad en
la web, `.netlify/` fuera de git. Build verificado en firmware (`pio run`) y
app (`assembleDebug`).
- ⬜ **UI en la app para ver/cambiar la clave WiFi del equipo**: el firmware
  ya tiene el comando (`COMMAND_HOST_SET_WIFI_PASSWORD`) y persiste en NVS,
  pero la app todavía no lo manda — falta agregar `Protocol.java` + pantalla
  de ajustes. Es la mitad que falta del pendiente viejo "Config credenciales
  AP desde app".
- ⬜ **Autenticación real del protocolo UDP (token/HMAC)**: hoy solo se mitigó
  con `max_connections=1` en el SoftAP (barato, cierra el caso más común).
  La autenticación de aplicación de verdad es un rediseño de protocolo, no
  un fix de una sesión — necesita su propia sesión de diseño.
- ⬜ Confirmar en el dashboard de Netlify el `publish directory` real (para
  validar que `_headers` quedó en la carpeta correcta).
- ⬜ Confirmar que el build de release del firmware define `RELEASE` (evita
  que quede un `while(true)` sin resetear WDT en el binario de producción).
- ⬜ Correr `./gradlew app:dependencies` + CVE scanning cuando haya
  conectividad (dependencias de nicho: `esp32-flash-lib`, `concentus`).

## 🧭 Navegación turn-by-turn propia — diseño aprobado (2026-07-05)
Diseño completo en `NAV_TURN_BY_TURN_DISENO.md`: motor **BRouter** (Java puro,
GPL-3.0, sin pipeline OSM propio), datos de ruteo ~400-600MB para toda
Argentina (verificado contra tamaños reales de BRouter/Geofabrik/OsmAnd),
descarga en 2 etapas (tile local obligatorio al primer inicio + resto del país
en background, nunca bloquea el radio/grupo), voz + flecha visual con ducking
de audio sobre el radio VHF. Aprobado para pasar a plan de implementación, pero
**el propio spec marca puntos a pulir antes de arrancar a codear** (ver sección
"Abierto para pulir" del archivo): tile inicial sin GPS fix, UI del cartel de
advertencia, integración fina del ducking, destino sobre miembro sin fix
reciente. Sin código escrito todavía — feature grande (subsistema nuevo, no un
ajuste chico).

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
- ⬜ **Exportar a GPX / compartir el track**: no se hizo (más superficie — archivo
  + share intent). Evaluar si hace falta antes de construirlo.
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
- ⬜ UI "Conectate a MotoRFAR-HT": guiar al usuario a conectar el WiFi del teléfono a la red del AP.
- ⬜ Config credenciales AP desde app (clave WPA2 hoy hardcodeada en firmware como `motorfar1234`).
- ⬜ UI CTCSS/DCS por canal.
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
