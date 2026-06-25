# RAMA FW — Firmware (ESP32)

> Para arrancar en chat nuevo: *"Trabajemos ETAPA FW-[N] del proyecto MOTO HT, leé `_PROYECTO\ramas\FW_firmware.md` (etapa FW-N) y seguimos."*
> Leé también `01_CONTRATOS.md` (Contrato A y B).

**Ubicación firmware:** `C:\Users\lukaz\OneDrive\Escritorio\kv4p-ht-main\kv4p-ht-main\microcontroller-src\kv4p_ht_esp32_wroom_32\`
**Build:** PlatformIO (`platformio.ini` en `microcontroller-src\`). Verificar instalación.

---

## FW-1 — Línea base (ARRANCABLE YA)

### Objetivo
Compilar el firmware actual, entender su estructura, dejar base limpia.

### Pasos
1. Verificar PlatformIO instalado (`pio --version`). Si no, instalar.
2. Compilar el firmware tal cual (`pio run`).
3. Mapear archivos clave: `kv4p_ht_esp32_wroom_32.ino` (main), `buttons.h`, `globals.h`, `protocol.h`, `txAudio.h`, `rxAudio.h`, `board.h`.
4. Documentar: dónde se define el transporte (USB serial), dónde el protocolo, dónde el control del radio.

### Verificar
- [ ] Compila sin errores
- [ ] Identificado el punto donde se podría cambiar transporte (para FW-3)

---

## FW-2 — Whitelist TX (ARRANCABLE YA — MÁXIMA PRIORIDAD SEGURIDAD)

### Objetivo
Hard-limit en firmware: solo permitir TX en las 3 frecuencias Res 5/2015. La app puede pre-filtrar, pero **el firmware es la autoridad final**.

### Frecuencias permitidas
- 139.970 MHz (Principal) · 138.510 MHz (Alternativo) · 140.970 MHz (Emergencia)

### Pasos
1. Localizar dónde el firmware setea la frecuencia del SA818.
2. Agregar validación: si la frecuencia pedida NO está en la whitelist, RECHAZAR TX (no transmitir).
3. Asegurar que el rechazo sea a nivel de comando al radio (no solo UI).

### Verificar 🔴
- [ ] Test real: pedir TX en una frecuencia fuera de lista → NO transmite
- [ ] Las 3 frecuencias válidas SÍ transmiten
- [ ] El bypass por la app no es posible (firmware bloquea aunque la app mande otra)

---

## FW-3 — Transporte WiFi (SOLO si F0=WiFi)

### Objetivo
Reescribir la capa de transporte: USB serial → red. Resultó **quirúrgico, no "reescritura mayor"**: el protocolo (`protocol.h`) ya estaba abstraído detrás de Arduino `Stream`, y la whitelist TX gatea el MODO del radio (no el enlace), así que sigue activa sin tocarla.

### Pre-requisito
- F0 decidida = WiFi ✅. FW-2 (whitelist) ya implementada en el firmware ✅. Contrato A actualizado con IP/puerto/encuadre ✅.

### FW-3a — UDP único (Stream shim) — ✅ IMPLEMENTADO, COMPILA (2026-06-23)
Enfoque: un socket UDP, un frame KISS por datagrama, todo el protocolo kv4p por ahí.
Archivos:
- NUEVO `wifi_credentials.h` (SSID/clave WPA2, gitignored) + `wifi_credentials.h.example`.
- NUEVO `wifiTransport.h`: clase `WifiStream : public Stream` sobre `WiFiUDP` + instancias globales `wifiUdp`/`wifiLink` + config (IP 192.168.4.1, UDP 4210, deadman 400ms).
- `protocol.h`: `#include "wifiTransport.h"`; los 3 overloads inline y el `parser` reapuntados de `Serial` → `wifiLink`; `_out.flush()` agregado a `KissBufferedWriter::end()` (señal "frame completo" → emite el datagrama).
- `kv4p_ht_esp32_wroom_32.ino`: `wifiSetup()` (SoftAP WPA2 + `setSleep(false)`), `announceHello()` (Hello al conectar cliente), `wifiServiceLoop()` (failsafe TX: corte instantáneo por desconexión + respaldo 400ms). `Serial` queda SOLO para banner/logs USB. Clave de prueba `motorfar1234`. **Pendientes vivos en `PENDIENTES.md`.**
- `platformio.ini`: **`board_build.partitions = min_spiffs.csv`** — OBLIGATORIO: la pila WiFi empujó el binario sobre la partición default (1.31MB). Con min_spiffs (app ~1.9MB, 2 slots OTA): flash 67.8% (1.33MB/1.97MB), RAM 21.9%.

Build OK con la receta de abajo. `firmware.bin` en `C:\Users\lukaz\pio-build\esp32dev\`.

### FW-3a — PENDIENTE (necesita el equipo + manos)
1. Flashear (maniobra BOOT — ver receta). Re-flashea también `partitions.bin` (cambió el esquema).
2. Unir la PC a la WiFi `MotoRFAR-HT` y correr `_PROYECTO\fw3a_smoke.py` → debe llegar el Hello.
3. `fw3a_smoke.py --open-rx` (con SA818 conectado) → ver frames RX_AUDIO por WiFi.

### Verificar 🔴
- [ ] Llega el Hello por WiFi (transporte kv4p OK sobre UDP) — `fw3a_smoke.py`
- [ ] RX audio fluye por WiFi (`--open-rx`)
- [ ] Audio PTT funciona por WiFi con latencia aceptable (criterio de F0) — necesita app o script TX
- [ ] Whitelist TX (FW-2) sigue activa sobre el nuevo transporte (por diseño: gatea el modo, no el enlace)
- [ ] Deadman: cortar el WiFi en plena TX → el firmware vuelve a RX solo

### FW-3b — split TCP/UDP (roadmap, tras validar 3a)
- Comandos (HostDesiredState/Hello/DeviceState) por **TCP confiable**; audio por UDP.
- Keepalive durante PTT + reconexión automática (cierra el item de reconexión).
- Reevaluar el windowing (`COMMAND_WINDOW_UPDATE`) — redundante sobre TCP.

---

## FW-4 — PTT + validación (PARCIAL — ya auditado)

### Estado
- Ya confirmado en `buttons.h`: 2 PTT en OR lógico, ambos `INPUT_PULLUP`. PTT Right flotante = seguro.

### Pasos
1. Confirmar perfil de hardware con `hasPhysPTT = true` para la variante MotoRFAR al flashear.
2. Validar que el J2 (PTT externo) dispara TX correctamente.
3. Probar PTT en transmisión real.

### Verificar
- [ ] `hasPhysPTT` correcto en la config de build
- [ ] J2 dispara TX
- [ ] Sonidos TX/listen-only OK (si aplican desde la app)

---

## TESTS DE LA RAMA (antes de INT)
- Compila y flashea sin error.
- Whitelist TX bloquea fuera de las 3 frecuencias (test real con equipo).
- PTT físico (J2) transmite.
- Si WiFi: enlace estable, audio OK, reconexión OK.


---

## 🛠️ RECETA DE BUILD + FLASH (toolchain que FUNCIONA) — verificado 2026-06-22

> ⚠️ El instalador normal de Python FALLA en esta máquina (Windows Installer roto: error 0x80070003 / MSI 2203, aunque las carpetas existan). NO perder tiempo reinstalando Python para esto — usar el venv ya armado abajo.

**PlatformIO** corre en un venv aislado creado con el Python de KiCad (3.11.5):
- `pio` → `C:\Users\lukaz\pio-venv\Scripts\pio.exe`
- python del venv → `C:\Users\lukaz\pio-venv\Scripts\python.exe`
- Si hay que recrearlo: `& 'C:\Users\lukaz\AppData\Local\Programs\KiCad\10.0\bin\python.exe' -m venv 'C:\Users\lukaz\pio-venv'` y después `python -m pip install platformio`.

**Proyecto firmware:** `C:\Users\lukaz\OneDrive\Escritorio\kv4p-ht-main\kv4p-ht-main\microcontroller-src`
- Entornos: `esp32dev` (debug, el que se flashea), `esp32dev-release`, `native-tests`.
- Build dir redirigido FUERA de OneDrive con env var `PLATFORMIO_BUILD_DIR=C:\Users\lukaz\pio-build` (evita que OneDrive trabe archivos a mitad de build).

**Compilar (solo esp32dev):**
```
[Environment]::SetEnvironmentVariable('PLATFORMIO_BUILD_DIR','C:\Users\lukaz\pio-build','Process'); & 'C:\Users\lukaz\pio-venv\Scripts\pio.exe' run -e esp32dev -d 'C:\Users\lukaz\OneDrive\Escritorio\kv4p-ht-main\kv4p-ht-main\microcontroller-src'
```

**Hardware de prueba:** ESP32-D0WD-V3 rev v3.1, en **COM9**, MAC b4:bf:e9:02:06:c8. Driver Silicon Labs CP210x ya instalado.

**⚠️ FLASHEAR — esta placa NO auto-resetea.** Hay que MANTENER apretado **BOOT** mientras esptool conecta (el reset por RTS anda, pero DTR no baja GPIO0). Maniobra: sostener BOOT → correr el flash → soltar cuando empieza a escribir. Comando (esptool directo, con `--before default_reset`):
```
& 'C:\Users\lukaz\pio-venv\Scripts\python.exe' 'C:\Users\lukaz\.platformio\packages\tool-esptoolpy\esptool.py' --chip esp32 --port COM9 --baud 460800 --before default_reset --after hard_reset write_flash -z --flash_mode dio --flash_freq 40m --flash_size 4MB 0x1000 'C:\Users\lukaz\pio-build\esp32dev\bootloader.bin' 0x8000 'C:\Users\lukaz\pio-build\esp32dev\partitions.bin' 0xe000 'C:\Users\lukaz\.platformio\packages\framework-arduinoespressif32\tools\partitions\boot_app0.bin' 0x10000 'C:\Users\lukaz\pio-build\esp32dev\firmware.bin'
```
> Tip: redirigir la salida a archivo con `*> 'C:\Users\lukaz\flash.log'` y leer el archivo — esptool escribe directo a consola y si no se pierde el error.
