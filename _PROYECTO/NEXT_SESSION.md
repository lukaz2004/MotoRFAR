# BAQUEANO — Prompt de arranque de sesión
> Copiá y pegá esto al inicio de cada chat. Claude lee SOLO este archivo y arranca.

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

## ESTADO AL 2026-06-27

| Etapa | Estado | Bloqueante |
|-------|--------|------------|
| F0 WiFi | ✅ SoftAP `MotoRFAR-HT`, UDP 4210, IP 192.168.4.1 | — |
| FW-1 línea base | ✅ compila + flashea | — |
| FW-2 whitelist TX | 🟡 código+flash OK | test RF real con SA818 |
| FW-3a WiFi transport | ✅ flasheado + Hello OK | `--open-rx` + failsafe (SA818) |
| FW-3b TCP/UDP split | ⬜ roadmap | tras FW-3a validado completo |
| FW-4 PTT J2 | 🟡 auditado | test físico con SA818 |
| HW-1 PCB | 🟡 Gerbers/BOM/CPL/ZIP listos | 4 tareas GUI KiCad (ver abajo) |
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
- Web Netlify: https://gorgeous-taffy-a6cfde.netlify.app (pass: My-Drop-Site)
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
- 🟢 Backup del PCB ANTES de cualquier cambio.
- 🟢 Cada chat = 1 etapa. Los contratos (`01_CONTRATOS.md`) son la memoria compartida entre ramas.
- 🟢 Python system installer roto en esta máquina — siempre pio-venv.
