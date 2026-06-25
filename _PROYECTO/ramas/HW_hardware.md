# RAMA HW — Hardware (PCB)

> Para arrancar: *"Trabajemos ETAPA HW-[N] del proyecto MOTO HT, leé `_PROYECTO\ramas\HW_hardware.md` (etapa HW-N) y seguimos."*
> Leé `01_CONTRATOS.md` (Contrato B y C).

**PCB:** `C:\Users\lukaz\OneDrive\Escritorio\kv4p-ht-main\kv4p-ht-main\pcb\v2.0e\kv4p-ht\kv4p-ht.kicad_pcb`
**Python KiCad:** `C:\Users\lukaz\AppData\Local\Programs\KiCad\10.0\bin\python.exe`
**kicad-cli:** `C:\Users\lukaz\AppData\Local\Programs\KiCad\10.0\bin\kicad-cli.exe`
**Backups:** `...\Temp\kv4p-ht-ORIGINAL.kicad_pcb` (limpio de GitHub) · `...\Temp\kv4p-ht-BACKUP-pre-carga.kicad_pcb`

## 🔴 REGLA DE ORO
**Nunca rutear cobre crítico (cerca de ESP32/USB/RF) por script.** Va en GUI de KiCad con vista de 4 capas. Los parches a ciegas YA causaron shorts. Hacer backup ANTES de cualquier cambio.

---

## HW-1 — Línea base (PARCIAL — mucho ya hecho)

### Estado actual ✅
- PTT externo (J2, 2 pads SMD) en `/PTT Button Left`. SW1/SW2 eliminados.
- Branding limpio. Title block = "MotoRFAR HT".
- SW3/SW4 (Reset/Program) intactos al frente.
- Validado: 0 críticos vs original.

### Pasos pendientes
1. Decidir si se MANTIENE la extensión de placa a 106.5mm (hecha) o se revierte a 82.55mm.
   - ⚠️ DEPENDE DE F0: si WiFi, NO hay circuito de carga → quizás no se necesita extensión.
2. Confirmar estado limpio con DRC.

### Verificar
- [ ] Decisión sobre extensión tomada (según F0)
- [ ] J2 conectado correctamente

---

## HW-2 — Antena dual + jaula Faraday (si F0=WiFi)

### Objetivo
Soportar 2 antenas externas (VHF por SMA + WiFi 2.4GHz), carcasa aluminio como jaula.

### Pasos
1. Definir punto de conexión de la antena WiFi (la del ESP32 es interna al módulo; para antena externa hay que ver si el WROOM-32 lo permite o usar variante con conector U.FL).
   - ⚠️ VERIFICAR: el ESP32-WROOM-32 estándar tiene antena PCB integrada. Para antena externa se necesita variante WROOM-32U (con conector U.FL) o adaptación.
2. Ubicar conector antena WiFi en borde, separado de la SMA VHF.
3. Coordinar con EST (Contrato C): ambas antenas salen externas de la carcasa de aluminio.

### Verificar 🔴
- [ ] Módulo ESP32 permite antena externa (¿WROOM-32U?) — definir en F0/HW
- [ ] Antenas físicamente separadas para no acoplar
- [ ] Posiciones pasadas a Contrato C

---

## HW-3 — Alimentación

### Si F0 = WiFi (SIMPLE)
- Solo entrada de 5V externa para alimentar el ESP32 (no hay carga de teléfono, el teléfono carga aparte).
- Conector de 2 pines (5V + GND) o USB-C de solo-power.
- Cambiar ferrite FB1 (1A) si el consumo con WiFi lo pide.

### Si F0 = USB (COMPLEJO — diseño ya hecho)
- Circuito de carga: 2× LM66100 en ORing (diseño completo en sesión previa).
- BOM: 2× LM66100 (SC-70-6), 2× 1µF, 2× 100kΩ, conector 5V, ferrite 2-3A.
- Topología: CE cruzado, selecciona fuente mayor, bloquea inversa.
- 🔴 Ruteo en GUI manual (zona densa).

### Verificar
- [ ] Arquitectura de alimentación coherente con F0
- [ ] Si carga: ruteo hecho en GUI, no script
- [ ] Ferrite dimensionado al consumo real

---

## HW-4 — Gerbers + DRC final

### Pasos
1. DRC completo en GUI KiCad CON reglas del proyecto (no kicad-cli genérico).
2. Resolver cualquier crítico real (en GUI).
3. Regenerar Gerbers (los de `production/` son viejos, con botones).
4. Generar BOM y centroid actualizados.

### Verificar 🔴
- [ ] DRC en GUI = 0 críticos
- [ ] Gerbers nuevos generados desde el archivo editado
- [ ] BOM incluye los componentes nuevos (J2, alimentación)
- [ ] **CERRAR Contrato C:** dimensiones finales y posiciones de conectores → pasar a EST-2

## TESTS DE LA RAMA
- DRC limpio con reglas reales.
- Gerbers verificados (visualizador).
- Dimensiones y conectores congelados para la carcasa.
