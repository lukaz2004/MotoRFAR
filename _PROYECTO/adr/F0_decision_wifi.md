# ADR F0 — Transporte teléfono↔ESP32: WiFi vs USB

**Estado:** DECIDIDO → **WiFi**. Confirmación de coexistencia RF (F0.4) pendiente en hardware armado, pero riesgo BAJO por diseño (ver abajo). No bloquea las ramas paralelas.
**Fecha:** 2026-06-22

## Contexto
Decidir si el enlace teléfono↔ESP32 se mantiene en USB serial (diseño actual) o pasa a WiFi (UDP). Si WiFi es viable, desaparece todo el circuito de carga (LM66100, extensión de placa, power-path): el teléfono deja de estar atado por USB y se carga con su propio cargador.

## Evidencia medida (F0.3 — latencia)
Hardware real: ESP32-D0WD-V3 (= WROOM-32) en modo STA, WiFi 2.4GHz, RSSI -58 dBm, echo UDP puerto 4210, modem-sleep desactivado. Cliente: PC en la misma LAN; paquetes de 120 B cada 20 ms (cadencia tipo Opus, 50 pkt/s). Escenario PESIMISTA (salto por router; el directo AP debería ser igual o mejor).

| Run | Paquetes | Pérdida | RTT mediana | RTT p95 | RTT max | Jitter |
|-----|----------|---------|-------------|---------|---------|--------|
| Corto | 300 (6 s) | 0% | 10.2 ms | 21.1 ms | 132.4 ms | 8.0 ms |
| Sostenido | 1500 (30 s) | 0% | 10.0 ms | 21.9 ms | 63.5 ms | 5.0 ms |

Presupuesto PTT (half-duplex): RTT < ~150-200 ms. **Margen ~20x.** Cero pérdida en 1800 paquetes, estable sostenido sin glitches periódicos.
→ **F0.3 PASA con amplio margen.**

## Coexistencia RF (F0.4) — riesgo BAJO por diseño
El diseño físico mitiga el riesgo en las tres vías estándar:
1. **Separación de frecuencia ~17x** (VHF ~140 MHz vs WiFi 2.4 GHz): sin interferencia co-canal.
2. **Carcasa de aluminio cerrada (jaula de Faraday)** sobre el ESP32, con **antena WiFi externa**.
3. **Antena VHF en pigtail, alejada de la carcasa**: separa el radiador de 1W de la electrónica y de la antena WiFi.

Riesgo residual: ruido de banda ancha del PA / desensibilización del front-end WiFi — bajo con shield + separación. **Se confirma empíricamente** con el SA818 transmitiendo en la unidad armada (queda como check en HW/INT, no como bloqueante).

## Pendiente (no bloqueante)
- F0.4: confirmación RF empírica (necesita SA818 + carcasa armada).
- F0.5 / Camino B: directo teléfono↔AP + que Android no corte el WiFi "sin internet". Riesgo bajo (STA-por-router ya pasó holgado).

## Decisión
**WiFi.** La latencia (gate de viabilidad) pasa holgada y la coexistencia RF —único riesgo restante— queda bien mitigada por el diseño. Se procede con WiFi en las ramas paralelas; F0.4 es confirmación en hardware, no bloqueante.

## Consecuencias
- Se ELIMINA el circuito de carga del PCB (LM66100, extensión, power-path) → HW-3 pasa a "solo 5V".
- Se activan **FW-3** (transporte WiFi) y **APP-2** (capa comunicación WiFi).
- Transporte: UDP. Puerto 4210 (provisional). Protocolo/IP/handshake se fijan en FW-3 y se vuelcan a `01_CONTRATOS.md`.

## Setup de test (reproducible)
Proyecto: `C:\Users\lukaz\f0-wifi-test\` (firmware echo + `rtt_test.py`). Build/flash: ver receta en `ramas\FW_firmware.md`.
