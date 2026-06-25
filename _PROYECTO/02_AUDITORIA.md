# MOTO HT — Auditoría del Proyecto

> Última actualización: 2026-06-23 (recalculada desde archivos locales reales).

---

## RESUMEN EJECUTIVO

| Rama | Estado real | Bloqueante |
|------|-------------|------------|
| F0 Arquitectura | ✅ Decidido: WiFi | — |
| FW-1 Línea base | ✅ Compila + flashea | — |
| FW-2 Whitelist TX | 🟡 Código+flash OK | Test RF real con SA818 |
| FW-3a WiFi transport | ✅ Flasheado + Hello OK | Test --open-rx + failsafe (SA818) |
| FW-3b TCP/UDP split | ⬜ Roadmap | Espera validar FW-3a completo |
| FW-4 PTT | 🟡 Auditado | Test real J2 + SA818 |
| HW-1 PCB línea base | 🟡 Gerbers/BOM/CPL/ZIP OK | DRC GUI + decisión extensión placa |
| HW-2/3/4 | ⬜ | Espera HW-1 + F0 |
| APP-1 Sprints | 🟡 Sprints 1-9 mergeados | Tiles offline, Privacy Policy, Release |
| APP-2 WiFi client | ⬜ | Espera FW-3a validado + APP-1 |
| APP-3 Disclaimer | ⬜ | Espera APP-1 + decisión WiFi |
| EST-1 Carcasa base | ⬜ | — (arrancable ya) |
| EST-2/3 | ⬜ | Espera HW-4 |
| INT | ⬜ | Todo lo anterior |
| WEB | ⬜ nuevo | Decisiones: stack, dominio, diseño |

**Lo más sólido:** transporte WiFi funcionando (FW-3a Hello OK), PCB lista para cotizar, app con 9 sprints mergeados.
**Bloqueante principal de hardware:** SA818 físico (para FW-2/FW-3a/FW-4 test RF).
**Próxima pieza de software de alto impacto:** APP-2 (reemplazar USB → WiFi en la app).

---

## 1. FIRMWARE (ESP32)

### Activos en disco (verificado)
- `kv4p_ht_esp32_wroom_32/` contiene: `.ino` (main), `buttons.h`, `globals.h`, `protocol.h`, `txAudio.h`, `rxAudio.h`, `board.h`, `led.h`, `debug.h`, `utils.h`.
- **Nuevos para WiFi:** `wifiTransport.h` ✅, `wifi_credentials.h` ✅, `wifi_credentials.h.example` ✅.
- `platformio.ini` con `board_build.partitions = min_spiffs.csv` ✅ (obligatorio para FW-3a).
- Build dir en `C:\Users\lukaz\pio-build\` (fuera de OneDrive).
- Binarios compilados: `firmware.bin`, `partitions.bin`, `bootloader.bin` en `pio-build\esp32dev\`.
- Hardware: ESP32-D0WD-V3 rev 3.1, COM9, MAC b4:bf:e9:02:06:c8.

### Estado por etapa
- **FW-1** ✅ Compila, flashea, base documentada.
- **FW-2** 🟡 `TxWhitelist.java` existe en la app (app-side). En firmware: `txAllowedByHost()` implementado. Flasheado. Falta: test RF real que TX fuera de whitelist sea rechazado en hardware.
- **FW-3a** ✅ Implementado quirúrgicamente (shim `WifiStream` sobre `Stream`). SoftAP `MotoRFAR-HT`, UDP 4210, deadman 400ms+instantáneo. Flasheado + Hello OK por WiFi. Pendiente (requiere SA818): `--open-rx` frames RX_AUDIO + failsafe corte WiFi en TX.
- **FW-4** 🟡 OR lógico PTT auditado, `INPUT_PULLUP` confirmado. Pendiente: test J2 físico con SA818.

### Toolchain (verificado funcional)
- PlatformIO en venv KiCad: `C:\Users\lukaz\pio-venv\Scripts\pio.exe`
- ⚠️ Python system installer roto en esta máquina — usar SIEMPRE el venv.
- Flash: maniobra BOOT manual (RTS anda, DTR no baja GPIO0 solo).

### Riesgos
- WiFi + VHF coexistencia RF: mitigado por diseño (antenas separadas + jaula aluminio) — validación empírica pendiente en INT.
- Clave WPA2 actual `motorfar1234` es de prueba — seteable desde APP-2 (NVS).

---

## 2. HARDWARE (PCB)

### Activos en disco (verificado)
- PCB editada: `pcb/v2.0e/kv4p-ht/kv4p-ht.kicad_pcb` ✅
- Esquemático: `kv4p-ht.kicad_sch` ✅
- `production-vhf/`: `gerbers/` ✅, `BOM_JLCPCB.csv` ✅, `CPL_JLCPCB.csv` ✅, `MotoRFAR_JLCPCB_READY.zip` ✅
- Backups: original limpio en `AppData\Local\Temp\kv4p-ht-ORIGINAL.kicad_pcb`.

### Estado
- ✅ SW1/SW2 DNP, J2 PTT externo 3.5mm TS agregado.
- ✅ Logo BAQUEANO en B.SilkS.
- ✅ Gerbers + drill generados con kicad-cli.
- ✅ BOM y CPL para JLCPCB listos (38 componentes con LCSC#).
- ✅ ZIP para subir a JLCPCB listo (~USD 100-130 para 5 placas PCBA).
- ⬜ DRC completo en GUI KiCad (kicad-cli da 528 falsos positivos, no sirve para validar).
- ⬜ Decisión extensión: 82.55mm (actual, sin alimentación externa) vs 106.5mm (con power-path). Con WiFi, el teléfono se desconecta del USB → NO hace falta circuito de carga → placa se queda en **82.55mm** (recomendado cerrar esto ya).

### ⚠️ Comprar por separado (no van en PCBA JLCPCB)
- SA818-V (módulo radio VHF) — AliExpress.
- Jack 3.5mm PJ302M — AliExpress.

### Regla de oro
- **Nunca rutear cobre crítico por script.** Todo cambio cerca del ESP32/USB/RF → GUI KiCad manual.

---

## 3. APP (Android)

### Activos en disco (verificado)
- Proyecto: `MotoRFAR-MTTT/KV4PHT/`
- Package: `ar.motorfar.app`, versionName `1.9.9.2`, versionCode `51`, compileSdk `35`, minSdk `26`.
- Stack: Kotlin + Compose + Java híbrido. Room DB, OSMDroid 6.1.18, Concentus (Opus), usbSerialForAndroid.
- Archivos clave: `RadioAudioService.java`, `Protocol.java`, `ConnectionController.java`, `TxWhitelist.java`, `ArgentinaChannels.java`, `MainActivity.kt`.
- Tests en `test/java/ar/motorfar/app/{data,radio,ui}`.

### Estado
- ✅ Sprints 1-9 mergeados en main. App funcional.
- ✅ Package rename, T&C español Res 5/2015, onboarding completo.
- ✅ OSMDroid incluido — mapa funciona, tiles online. Falta: descarga offline.
- ✅ `TxWhitelist.java` existe (lado app — complementa la whitelist del firmware).
- ⬜ **Tiles offline OSMDroid** — botón de descarga + `OfflineDownloadManager` + dialog de progreso.
- ⬜ **Privacy Policy** — `docs/PRIVACY-POLICY.md` (texto) + pantalla en app (Settings o Acerca de).
- ⬜ **GitHub Release v1.0-beta1** — tag, APK release, changelog.
- ⬜ **APP-2 (mayor):** reemplazar `usbSerialForAndroid` por cliente WiFi/UDP. Bindear socket a red del AP, KISS framing, Hello/DeviceState, keepalive PTT, reconexión. Decisión windowing.
- ⬜ **APP-3:** disclaimer compatibilidad (ahora es WiFi, no OTG).
- ⬜ **UI/UX rediseño** (el usuario quiere otro formato) — es capa presentación, separable de lógica. Post APP-2.
- ⬜ **CTCSS/DCS por canal** — UI para elegir tono/código. Post APP-2.

### Build
```
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
cd MotoRFAR-MTTT\KV4PHT
.\gradlew assembleDebug
```

---

## 4. ESTÉTICA (Carcasa)

### Activos en disco
- `MotoRFAR-MTTT/carcaza v1.f3d` (Fusion 360, referencia KB9VQQ Thin).
- `box v1.stl`, `box v2.stl`.
- Una carcasa ya impresa en físico.

### Estado
- ✅ Referencia dimensional real: 38.1 × 82.55mm (o 106.5mm si se extiende).
- ⬜ EST-1: reconstrucción paramétrica limpia como sólido (la malla STL no es editable para CNC).
- ⬜ EST-2: ajuste a placa final + posición 2 antenas externas. BLOQUEADA hasta HW-4.
- ⬜ EST-3: print 3D → CNC aluminio.

### Diseño objetivo
- Carcasa aluminio = jaula de Faraday (electrónica adentro).
- 2 antenas externas: SMA VHF (pigtail, alejada 1W PA) + WiFi 2.4GHz (fuera de aluminio).
- Sin relieve PTT lateral (va conector J2 externo).
- Ventana USB-C a medida de J5.

---

## 5. WEB (NUEVO — anotado en PENDIENTES)

- ⬜ Sitio de producto MotoRFAR HT.
- A definir: stack (GitHub Pages estático recomendado), dominio, diseño.
- Contenido mínimo: qué es, canales Res 5/2015, cómo conseguirlo, GPL.
- Orden: en paralelo con APP-2/HW-2, antes de venta real.

---

## 6. LICENCIAS (auditado, sin cambios)

| Componente | Licencia |
|---|---|
| Firmware kv4p + app Android | GPL-3.0 |
| arduino-audio-tools | GPL-3.0 |
| esp32-afsk (APRS) | GPL-3.0-only |
| DRA818 | LGPL-3.0 |
| arduino-libopus / Concentus | BSD-3-Clause |
| Core Arduino-ESP32 | LGPL-2.1+/Apache-2.0 |

- Vender es compatible con GPL. Se publica el fuente. Ver `adr/L0_decision_licencia_venta.md`.
- Checklist pre-venta: repo público fuente firmware+app, pantalla Acerca de/Licencias, equipo flasheable por USB (sin secure-boot), revisión legal recomendada.
