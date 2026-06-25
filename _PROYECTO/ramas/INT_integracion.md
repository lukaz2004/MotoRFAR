# RAMA INT — Integración y Ensamblaje Final

> Para arrancar: *"Trabajemos ETAPA INT del proyecto MOTO HT, leé `_PROYECTO\ramas\INT_integracion.md` y seguimos."*
> **Pre-requisito: TODAS las ramas con sus tests propios pasados.**

---

## OBJETIVO
Juntar las piezas del rompecabezas y validar el sistema completo funcionando.

## PRE-REQUISITOS (gate de entrada)
- [ ] F0: arquitectura decidida y en ADR
- [ ] HW-4: PCB con Gerbers nuevos, DRC limpio, placa fabricada
- [ ] FW: firmware compilado, whitelist TX validada, PTT OK (+ WiFi si aplica)
- [ ] APP: sprints mergeados, tests OK (+ comunicación WiFi si aplica)
- [ ] EST: carcasa impresa/fresada que aloja la placa real

---

## PASO A PASO

### INT.1 — Ensamblaje físico
1. Montar placa fabricada en la carcasa.
2. Conectar antenas (VHF + WiFi si aplica), conector PTT externo, alimentación.
3. Verificar ajuste mecánico, que todo entre y los conectores alineen.

### INT.2 — Bring-up eléctrico
1. Alimentar desde fuente externa 5V (moto/powerbank).
2. Flashear firmware (perfil con `hasPhysPTT`).
3. Verificar que el ESP32 arranca y el radio responde.

### INT.3 — Enlace teléfono
- **Si USB:** conectar teléfono, app reconoce el dispositivo.
- **Si WiFi:** teléfono se conecta al AP del ESP32, app establece enlace.

### INT.4 — Test funcional completo
1. RX: escuchar una repetidora/simplex local.
2. TX: transmitir en una de las 3 frecuencias válidas (con licencia/equipo de prueba).
3. PTT externo (J2) dispara TX.
4. Whitelist: confirmar que NO transmite fuera de las 3 frecuencias.
5. APRS/texto si aplica.

### INT.5 — Test en condiciones reales (moto)
1. Montaje en manillar, antenas en alto.
2. Alimentación desde la moto.
3. Coexistencia: WiFi (si aplica) estable con VHF transmitiendo.
4. Vibración/pozos: que nada se suelte.
5. Autonomía: el teléfono aguanta la sesión (con su carga aparte).

---

## PUNTOS RELEVANTES A VERIFICAR 🔴
- [ ] TX SOLO en las 3 frecuencias Res 5/2015 (seguridad legal)
- [ ] PTT externo funciona
- [ ] RX y TX reales OK
- [ ] Si WiFi: enlace estable en marcha, sin cortes de audio
- [ ] Alimentación desde moto estable (sin reset por picos)
- [ ] Mecánica firme ante vibración

## SALIDA
- Sistema MOTO HT funcionando end-to-end.
- Documentar resultados, fotos, mediciones.
- Lista de mejoras para v2.

---

## NOTA LEGAL/IP
- Proyecto GPL-3.0 (deriva de kv4p HT, Vance Vagell KV4P). Mantener atribución en README del repo.
- TX limitada a frecuencias autorizadas M.T.T.T. Res 5/2015.
