# PENDIENTES — MotoRFAR MTTT

> Lista viva de cosas a no olvidar durante el rewrite. Actualizar al cerrar/abrir items.
> Última edición: 2026-07-05 (rework web comercial + registro de ruta por sesión en la app).

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
- ⬜ **Man-Down: countdown variable según fuerza G del golpe** (propuesto 2026-07-05).
  Golpe fuerte → cuenta corta (más urgente); golpe leve → cuenta larga (más
  chance de cancelar). Requiere: `FallDetectionManager` guarde el pico de
  aceleración y lo pase en el callback (`onFallDetected: (peakG: Float) -> Unit`
  en vez de `() -> Unit`), y `countdownTimeSec` deje de ser fijo. Se agrupa con
  el pendiente ya existente de calibración de Man-Down (prueba en dispositivo
  físico) — no tiene sentido calibrar umbrales dos veces por separado.
- ⬜ **Web — copy de CTCSS ("Tu grupo escucha solo a tu grupo")**: la frase "Sin
  interferencias" sobrevende, mismo problema que el ya corregido "200 motos" de
  la card de al lado — CTCSS filtra lo que escuchás, no separa el RF real (ver
  caveat ya documentado más abajo en "Feature — CTCSS/DCS por canal"). Ajustar
  cuando se retome esta sección.

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
- ⬜ **Generar keystore** (una sola vez, antes de publicar en Play Store):
  ```
  keytool -genkey -v -keystore baqueano-release.jks -alias baqueano -keyalg RSA -keysize 2048 -validity 10000
  ```
  Luego copiar `keystore.properties.template` → `keystore.properties` y completar contraseñas. ⚠️ Hacer backup del .jks — sin él no se puede actualizar la app.

## 📱 APP — ÍCONOS (2026-06-30)
- ✅ Todos los mipmap reemplazados con badge Baqueano (5 densidades × 3 archivos).
- ✅ Splash screen automático Android 12+ usa el adaptive icon nuevo.

## 🌐 WEB — GALERÍA REAL (2026-06-30)
- ✅ 6 fotos reales en `_PROYECTO/web/img/` (moto-01 a moto-06).
- ✅ Video demo con logo Baqueano en `_PROYECTO/web/baqueano-demo.mp4`.
- ✅ HTML sección "En acción" actualizado con grilla 3×2, hover/zoom, captions y video.

## 🏗️ EST-1 — CARCASA — ESPECIFICACIONES CERRADAS (2026-06-30)
- ✅ Dimensiones externas: **170 × 75 × 24mm**
- ✅ Construcción: sandwich 2 tapas Al 3mm + frame 18mm corte por agua
- ✅ Tapa: 8 tornillos M3 countersunk (3 por lado largo, 1 por lado corto)
- ✅ Frente: botón M16 Short Body High Head LED azul (esq. sup. izq., 10mm márgenes), USB-C centrado, tapa PETG 60×20×4mm antena WiFi (parte inferior)
- ✅ Borde superior: SMA VHF (15mm del borde der., 15mm del borde frontal)
- ✅ Pigtail RG316 → antena varilla inox 50cm
- ✅ Sin conector 5V separado (USB-C alimenta todo)
- ✅ Terminaciones: arenado crudo ó anodizado negro sobre arenado
- ⬜ Modelado en Fusion 360 (EST-1 arrancable — leer `ramas/EST_estetica.md`)
- ⬜ Corte por agua del frame de 18mm
- ⬜ Impresión 3D prueba antes de mecanizar aluminio

## 🌐 WEB — PUBLICADA (2026-06-24)
- ✅ Rediseño comercial completo: hero, kit, features, frecuencias, app mockup, vehículos, accesorios, specs, CTA.
- ✅ Branding renombrado a **Baqueano** — logo escudo integrado en nav y hero.
- ✅ Deploy en Netlify: https://gorgeous-taffy-a6cfde.netlify.app (pass: My-Drop-Site)
- ✅ `docs/index.html` + `docs/assets/` pusheados a main en GitHub.
- ⬜ Reclamar sitio Netlify con cuenta (60 min desde deploy — si expiró, re-deployar con `netlify deploy --dir=docs --prod --allow-anonymous` desde la carpeta del repo).
- ⬜ Renombrar URL a `baqueano.netlify.app` (desde panel Netlify tras reclamar).
- ⬜ Dominio propio (opcional, post-lanzamiento).
- ⬜ Reemplazar renders placeholder por fotos/CAD reales cuando estén disponibles.

## 🎛️ Feature — CTCSS/DCS por canal (era "subfrecuencias")
- ✅ ACLARADO: las "subfrecuencias" = CTCSS/DCS (tonos/códigos de squelch), NO frecuencias. Dejan que varios grupos compartan el mismo canal legal sin escucharse. **No mueve la frecuencia → no toca la whitelist, 100% legal (Res 5/2015 = solo 3 freqs).**
- Caveat: CTCSS filtra lo que ESCUCHÁS, no separa el RF (si dos grupos transmiten a la vez, igual colisionan). No es canal privado.
- Viable en el hardware: el SA818 soporta CTCSS (38 tonos) + DCS; el protocolo ya tiene `ctcss_tx`/`ctcss_rx` en `HostDesiredState` (el transporte WiFi ya los lleva gratis).
- PENDIENTE FW: confirmar que `reconcileDesiredState()` pase `ctcss_tx`/`ctcss_rx` al `sa818.group()` (no verificado aún). Si falta, es un add chico.
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
