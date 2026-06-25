# MOTO HT — Mapa Maestro del Proyecto

> **Cómo usar este proyecto:** Cada etapa es una pieza independiente. En un chat NUEVO de Claude, decí:
> *"Trabajemos la ETAPA [código] del proyecto MOTO HT. Leé el archivo correspondiente en C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\_PROYECTO\ y seguimos desde ahí."*
> Claude lee SOLO el archivo de esa etapa (no todo el contexto) y ejecuta el paso a paso.

---

## Qué es MOTO HT

Evolución de MotoRFAR-MTTT (fork de kv4p HT, GPL-3.0). Radio VHF/UHF que convierte un Android en transceptor ham, montado en moto/4x4.
**Cambio estratégico:** evaluar reemplazar el enlace USB (teléfono↔ESP32) por **WiFi**, lo que resolvería de raíz el problema del cable y la carga.

## Frecuencias M.T.T.T. Res 5/2015 (whitelist TX obligatoria)
- 139.970 MHz Principal · 138.510 MHz Alternativo · 140.970 MHz Emergencia

---

## ESTRUCTURA EN RAMAS Y ETAPAS

| Código | Rama | Etapa | Depende de | Estado |
|--------|------|-------|------------|--------|
| **F0** | Arquitectura | Decisión WiFi vs USB | — | ✅ DECIDIDO: **WiFi** (latencia 10ms/0% loss; RF bajo riesgo por diseño) — confirmación RF empírica en HW |
| **HW-1** | Hardware | Línea base placa actual | F0 | 🟡 Gerbers+BOM+CPL listos · DRC GUI pendiente |
| **HW-2** | Hardware | Antena dual + jaula Faraday | F0 | ⬜ |
| **HW-3** | Hardware | Alimentación (solo 5V o +carga) | F0, HW-1 | ⬜ |
| **HW-4** | Hardware | Gerbers + DRC final | HW-1..3 | ⬜ |
| **FW-1** | Firmware | Línea base + compilar | — | ✅ Compila OK (esp32dev) + flashea a ESP32 real |
| **FW-2** | Firmware | Whitelist TX (seguridad) | FW-1 | 🟡 Código+build+flash OK en ESP32 real (hash verificado) — falta test RF de TX con SA818 |
| **FW-3** | Firmware | Transporte WiFi (si F0=WiFi) | F0, FW-1 | 🟡 FW-3a ✅ (flash+Hello OK), FW-3b ⬜ |
| **FW-4** | Firmware | PTT + validación | FW-1 | 🟡 Parcial |
| **APP-1** | App | Mergear sprints pendientes | — | 🟡 Parcial |
| **APP-2** | App | Capa comunicación WiFi (si F0=WiFi) | F0, APP-1 | ⬜ |
| **APP-3** | App | Disclaimer compatibilidad | APP-1 | ⬜ |
| **EST-1** | Estética | Reconstrucción carcasa paramétrica | — | ⬜ |
| **EST-2** | Estética | Ajuste a placa final + antenas | HW-4 | ⬜ |
| **EST-3** | Estética | Print 3D → CNC aluminio | EST-1, EST-2 | ⬜ |
| **INT** | Integración | Ensamblaje + test sistema completo | TODAS | ⬜ |

**Leyenda:** ⬜ no iniciada · 🟡 parcial (hay trabajo previo) · ✅ completa

---

## ORDEN RECOMENDADO

1. **F0 primero** (desbloquea hardware y firmware-transporte).
2. En paralelo, SIN esperar F0: **FW-2** (whitelist), **APP-1** (sprints), **EST-1** (carcasa). No dependen de la decisión WiFi.
3. Post-F0: si WiFi → FW-3, APP-2, HW-2/3 con esa premisa. Si USB → seguir diseño actual.
4. **INT al final.**

---

## LOS 3 CONTRATOS (bordes del rompecabezas — definidos en 01_CONTRATOS.md)

- **App ↔ Firmware:** protocolo de mensajes (USB serial o red).
- **Firmware ↔ Hardware:** pinout del ESP32, qué controla.
- **Hardware ↔ Carcasa:** dimensiones placa, posición conectores/antenas, montaje.

Cada rama trabaja contra estos contratos sin depender de las otras.

---

## ÍNDICE DE ARCHIVOS

- `00_MAPA_MAESTRO.md` — este archivo
- `01_CONTRATOS.md` — interfaces entre ramas
- `02_AUDITORIA.md` — estado real de cada activo (qué tenemos / qué falta)
- `03_CALENDARIO.md` — secuencia y dependencias
- `04_HERRAMIENTAS.md` — agentes/plugins/extensiones necesarios
- `adr/F0_decision_wifi.md` — a completar en Etapa F0
- `ramas/F0_arquitectura.md`
- `ramas/HW_hardware.md`
- `ramas/FW_firmware.md`
- `ramas/APP_app.md`
- `ramas/EST_estetica.md`
- `ramas/INT_integracion.md`

---

## UBICACIÓN DE LA INFO ACTUAL (planos y datos usables)

| Qué | Dónde |
|-----|-------|
| Proyecto raíz | `C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\` |
| App Android | `...\MotoRFAR-MTTT\KV4PHT\` |
| Firmware ESP32 | `...\kv4p-ht-main\kv4p-ht-main\microcontroller-src\kv4p_ht_esp32_wroom_32\` |
| PCB editado (PTT ✓, branding limpio ✓) | `...\kv4p-ht-main\kv4p-ht-main\pcb\v2.0e\kv4p-ht\kv4p-ht.kicad_pcb` |
| Esquemático | `...\pcb\v2.0e\kv4p-ht\kv4p-ht.kicad_sch` |
| PCB original limpio (backup GitHub) | `C:\Users\lukaz\AppData\Local\Temp\kv4p-ht-ORIGINAL.kicad_pcb` |
| Backup pre-carga | `C:\Users\lukaz\AppData\Local\Temp\kv4p-ht-BACKUP-pre-carga.kicad_pcb` |
| Carcasa Fusion (referencia KB9VQQ Thin) | en sesión Fusion / `...\MotoRFAR-MTTT\carcaza v1.f3d` |
| STLs carcasa | `...\MotoRFAR-MTTT\box v1.stl`, `box v2.stl` |
| KiCad Python | `C:\Users\lukaz\AppData\Local\Programs\KiCad\10.0\bin\python.exe` |
| kicad-cli | `C:\Users\lukaz\AppData\Local\Programs\KiCad\10.0\bin\kicad-cli.exe` |
| Doc metodología existente | `...\MotoRFAR-MTTT\CLAUDE.md` |
