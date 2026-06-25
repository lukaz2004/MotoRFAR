# MOTO HT — Contratos entre Ramas

> Estos son los **bordes del rompecabezas**. Cada rama trabaja contra estos contratos sin depender de las otras. Si una rama cambia un contrato, AVISA y se actualiza acá.
> **Algunos contratos quedan ABIERTOS hasta que F0 decida WiFi vs USB.** Marcados con ⚠️.

---

## CONTRATO A — App ↔ Firmware (el protocolo)

### Si transporte = USB (NO elegido — F0 decidió WiFi; histórico)
- Medio: USB serial CDC.
- Formato: protocolo binario kv4p existente (audio Opus + comandos KISS).
- Velocidad: la del firmware actual.
- **No tocar si se mantiene USB.**

### Transporte = WiFi ✅ DECIDIDO (F0, 2026-06-22) — detalles del protocolo a fijar en FW-3/APP-2
> Validado: latencia UDP ~10ms RTT, 0% pérdida sostenida (ver `adr/F0_decision_wifi.md`). Coexistencia RF de bajo riesgo por diseño (jaula Faraday + antenas separadas). Audio PTT viable por WiFi.
- Medio: el ESP32 es Access Point WPA2 (SSID `MotoRFAR-HT`); el teléfono se asocia a esa red.
- IP del AP: **192.168.4.1** fija (`softAPConfig`); el cliente toma DHCP en 192.168.4.x. ✅ FIJADO FW-3a.
- Puerto: **UDP 4210** ✅ FIJADO FW-3a. En FW-3a audio + comandos van por el MISMO socket UDP.
- Encuadre: **un frame KISS = un datagrama UDP**. El protocolo kv4p completo (vendor frames + KISS DATA AX.25) viaja igual vía un shim `WifiStream` (implementa Arduino `Stream` sobre UDP). La dirección del cliente se aprende del primer datagrama.
- Audio: Opus sigue, encapsulado en los vendor frames (sin cambios de formato).
- Hello: se reenvía al asociarse un cliente nuevo (no hay Hello de boot entregable sin remoto conocido).
- Failsafe TX (deadman): si el host deja de mandar datos por >400ms estando en TX, el firmware fuerza RX. Segundo pilar de "el firmware es la autoridad final de TX", junto a la whitelist.
- Roadmap FW-3b: separar comandos a **TCP confiable** (PTT/freq/estado) manteniendo audio en UDP (un PTT-off perdido no debe poder colgar el TX).
- ⚠️ Dependencias del lado APP-2: (a) **bindear el socket del cliente a la red del AP** (`Network.bindSocket`) para que Android no enrute por "red sin internet"; (b) decidir si se mantiene el windowing (`COMMAND_WINDOW_UPDATE`) sobre WiFi — en USB es flow-control; en UDP se reevalúa en FW-3b.
- **Mensajes mínimos que el contrato debe cubrir:** PTT on/off, audio TX→radio, audio RX←radio, set frecuencia (validada contra whitelist), estado/squelch, APRS.

### Regla invariante (vale para los dos)
- La **whitelist de frecuencias TX se valida en el FIRMWARE** (no confiar solo en la app). La app puede pre-filtrar, pero el firmware es la autoridad final.

---

## CONTRATO B — Firmware ↔ Hardware (pinout)

- MCU: **ESP32-WROOM-32** (NO S3 — corregido de un error previo de doc).
- Radio: **SA818-V** (VHF) / SA818-U (UHF), comunicación serial.
- Audio: Opus sobre el enlace MCU↔teléfono.
- PTT físico: señal `/PTT Button Left` (en J2, conector externo). Pin con `INPUT_PULLUP`. PTT Right sin uso.
- Auto-flash: Q1 (RTS→Reset), Q2 (DTR→GPIO0). Botones Reset/Program son respaldo.
- Pin de control del radio (PTT hacia SA818), TX/RX audio: según firmware actual.
- ⚠️ Si WiFi: el firmware usa el periférico WiFi del ESP32 (ya integrado). Verificar que no colisione con pines del radio (no debería: WiFi es interno al chip).

### Regla invariante
- El consumo en TX (radio + WiFi si aplica) exige **alimentación externa 5V** (no solo batería del teléfono).

---

## CONTRATO C — Hardware ↔ Carcasa (físico)

- Dimensiones placa: **38.1 × 82.55mm** base (CONFIRMAR si se mantiene extensión a 106.5mm según F0/HW).
- Espesor PCB: 1.6mm estándar.
- Conectores y su posición (borde):
  - USB-C (J5): borde inferior. ⚠️ Puede reubicarse según HW.
  - SMA antena VHF (J1): borde superior (zona RF).
  - J2 PTT externo: donde estaba SW1.
  - **Antena WiFi 2.4GHz: EXTERNA a la carcasa de aluminio** (la jaula de Faraday no la atenúa), separada de la SMA VHF. La **antena VHF va en pigtail, alejada de la carcasa** (aleja el radiador de 1W de la antena WiFi y de la electrónica). [Definido por diseño RF en F0.]
  - ⚠️ Si alimentación externa: conector entrada 5V — posición a definir.
- Montaje: puntos para soporte de moto (manillar). Carcasa **aluminio = jaula de Faraday** (apantalla electrónica; antenas van EXTERNAS para no atenuarlas).
- Idea de diseño: 2 antenas externas montadas en alto (techo 4x4 / moto), VHF con extensión SMA.

### Regla invariante
- La carcasa NO se finaliza (EST-2) hasta que HW-4 cierre dimensiones y posiciones de conectores.

---

## CÓMO SE ACTUALIZA ESTE ARCHIVO
Cuando una etapa resuelve un ⚠️ (ej. F0 decide WiFi, o HW fija la posición de la antena), se edita este archivo y se quita el ⚠️. Es la única fuente de verdad de las interfaces.
