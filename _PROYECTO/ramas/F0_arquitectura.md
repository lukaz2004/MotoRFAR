# RAMA F0 — Decisión de Arquitectura (WiFi vs USB)

> **LA LLAVE MAESTRA.** Define el 40% del resto del proyecto. Hacer ANTES que hardware/transporte.
> Para arrancar en chat nuevo: *"Trabajemos ETAPA F0 del proyecto MOTO HT, leé `_PROYECTO\ramas\F0_arquitectura.md` y seguimos."*

---

## OBJETIVO
Decidir, con evidencia, si el enlace teléfono↔ESP32 se mantiene en **USB** o pasa a **WiFi**. Escribir la decisión en `adr/F0_decision_wifi.md`.

## POR QUÉ IMPORTA
- **Si WiFi:** el teléfono deja de estar atado por USB → se carga con su propio cargador → desaparece TODO el circuito de carga (LM66100, extensión de placa, power-path). El MotoRFAR solo necesita alimentación. Resuelve el problema de fondo (cable + montaje + carga).
- **Si USB:** se sigue el diseño actual, con la complejidad de carga ya analizada.

## DATO DE INVESTIGACIÓN (ya hecho)
- **Nadie hizo kv4p sobre WiFi.** Todas las versiones (v1.6 a v2.x) son USB serial.
- El patrón "ESP32 ↔ red ↔ otro dispositivo, emulando un periférico" está PROBADO en otros proyectos (ej. KVM-over-WiFi por UDP a 125Hz). Viable técnicamente.
- ESP32-WROOM-32 ya tiene WiFi/BT — cero hardware nuevo.
- Un usuario de la comunidad ya planteó controlar el ESP32 sin el teléfono vía el puerto — hay apetito por desacoplar.

---

## PASO A PASO

### F0.1 — Conseguir un ESP32 de prueba físico
- Un devkit ESP32-WROOM-32 suelto (~US$5-10). **NO usar la placa buena para esto.**
- ⚠️ VERIFICAR: que sea WROOM-32 (no S3), para igualar el hardware real.

### F0.2 — Prototipo mínimo de transporte WiFi
- Firmware mínimo: ESP32 como Access Point + echo de audio por UDP.
- App mínima (o script): conectarse al AP y mandar/recibir un stream de audio Opus.
- Objetivo: NO es el producto, es medir si el enlace aguanta audio PTT.

### F0.3 — Medir latencia y estabilidad (EL TEST CLAVE)
- Medir latencia ida y vuelta del audio por WiFi local.
- **Criterio de aceptación:** para PTT (half-duplex), latencia < ~150-200ms es usable. Jitter estable, sin cortes.
- 🔴 VERIFICAR: que el audio no se entrecorte con el teléfono a distancia de bolsillo (1-2m).

### F0.4 — Coexistencia RF
- Probar el WiFi del ESP32 mientras el SA818 transmite VHF a 1W.
- 🔴 VERIFICAR: que el WiFi no se caiga ni degrade con el PA transmitiendo cerca.
- Mitigación de diseño ya prevista: antenas separadas + carcasa aluminio (jaula Faraday) con antenas externas.

### F0.5 — Manejo de red en la moto
- Confirmar: el teléfono se conecta al AP del ESP32 (sin internet) mientras mantiene datos móviles 4G/5G.
- ⚠️ VERIFICAR: que Android no corte el WiFi "sin internet" (algunos lo hacen; hay flags para forzar).

### F0.6 — Decisión y ADR
- Con los datos de F0.3 y F0.4, decidir WiFi o USB.
- Escribir `adr/F0_decision_wifi.md` con: decisión, evidencia (latencia medida, RF), consecuencias para cada rama.
- **ACTUALIZAR `01_CONTRATOS.md`:** quitar los ⚠️ del transporte, fijar protocolo/puertos/IP.

---

## PUNTOS RELEVANTES A VERIFICAR (resumen)
- [ ] ESP32 de prueba es WROOM-32 (no S3)
- [ ] Latencia audio PTT < ~150-200ms, sin cortes
- [ ] WiFi estable con PA VHF transmitiendo
- [ ] Android mantiene datos móviles + WiFi sin internet
- [ ] Decisión escrita en ADR y contratos actualizados

## SALIDA DE LA ETAPA
- `adr/F0_decision_wifi.md` completo.
- `01_CONTRATOS.md` actualizado (transporte definido).
- Mapa maestro: marcar F0 como ✅ y desbloquear olas 2.

## SI NO QUERÉS/ PODÉS PROTOTIPAR AHORA
Alternativa: arrancar las olas 1 NO bloqueadas (FW-2 whitelist, APP-1 sprints, EST-1 carcasa) y dejar F0 para cuando tengas el ESP32 de prueba. El proyecto avanza igual en esas ramas.
