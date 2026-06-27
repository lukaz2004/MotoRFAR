# PENDIENTES — MotoRFAR MTTT

> Lista viva de cosas a no olvidar durante el rewrite. Actualizar al cerrar/abrir items.
> Última edición: 2026-06-24 (Privacy Policy OK · web borrador listo · Release v1.0-beta1 creado · ProGuard pendiente).

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

## 🟡 HW-1 — avanzado (2026-06-24)
- ✅ SW1/SW2 (botones físicos PTT) marcados DNP en esquemático.
- ✅ J2 conector jack 3.5mm TS PTT externo agregado al esquemático.
- ✅ Logo BAQUEANO en B.SilkS del PCB.
- ✅ **J3 header 2-pin PTT Right** colocado en PCB (x=163.5 y=99.0mm). pad1=/PTT Button Right, pad2=GND. (2026-06-24)
- ✅ **Edge.Cuts revertida a 82.55mm** (38.2 × 82.6mm). LOGO1 movido adentro de la placa. (2026-06-24)
- ✅ **J1 VHF antenna: SMA horizontal → U.FL Hirose** (x=155.575 y=68.326mm, mismo punto, red RF conservada). Pigtail U.FL→SMA bulkhead se rosca en la carcasa. Sin palancas sobre el PCB. BOM: C11519. (2026-06-24)
- ✅ BOM formato JLCPCB (`BOM_JLCPCB.csv`, 38 componentes con LCSC#).
- ✅ CPL formato JLCPCB (`CPL_JLCPCB.csv`, 89 componentes posicionados).
- ⚠️ SA818-V y jack 3.5mm PJ302M NO van en el PCBA — comprar por separado (AliExpress).
- ⬜ **GUI KiCad — 4 tareas pendientes antes de ordenar:**
  1. Rutear 3 pistas cortas /PTT Button Right: R14-pad1 → C33-pad1 → U5-pad29 → J3-pad1
  2. Verificar y ajustar traza RF de J1: el U.FL tiene pad central más pequeño que el SMA — verificar que la pista existente conecte bien. Extender si hace falta (VHF 138MHz → 2m λ, unos mm extra = inocuos).
  3. Verificar posición LOGO1 (y=140) y J3 (x=163.5, ~1.6mm del borde derecho) — pueden necesitar ajuste
  4. DRC completo (diálogo queda off-screen: abrir con Win+Shift+Flecha)
- ⬜ Actualizar esquemático: agregar J3 header 2-pin + cambiar U5 a WROOM-32U-N4 para mantener sync PCB↔SCH.
- ⬜ Regenerar Gerbers + BOM + ZIP tras las correcciones GUI.
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
- ✅ Decidido: vender es compatible con GPL. Firmware y app siguen siendo forks GPL-3.0; se publica el fuente.
- ✅ Repo con fuente publicado (Release v1.0-beta1).
- ✅ Pantalla "Acerca de / Licencias" — completa. Email removido de Créditos. Solo atribuciones exigidas por licencia. (2026-06-24)
- ✅ Web footer: crédito a kv4p HT / Vance Vagell (KV4P) agregado. (2026-06-24)
- ✅ PCB production-vhf/README.md: atribución CC-BY-SA 4.0 KiCad Libraries + GPL-3.0 upstream. (2026-06-24)
- ⬜ Equipo flasheable por USB, sin secure-boot bloqueado.
- ✅ Fix ProGuard para APK release firmado. (2026-06-24)
- ⬜ Revisión legal de IP antes de vender en serio.
