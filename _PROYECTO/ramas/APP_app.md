# RAMA APP — App Android

> Para arrancar: *"Trabajemos ETAPA APP-[N] del proyecto MOTO HT, leé `_PROYECTO\ramas\APP_app.md` (etapa APP-N) y seguimos."*
> Leé `01_CONTRATOS.md` (Contrato A).

**Ubicación:** `C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\KV4PHT\`
**Build:** `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` ANTES de `.\gradlew.bat`
**Stack:** Kotlin + Jetpack Compose. Package `ar.motorfar.app`.
**Metodología:** mantener sprints (CLAUDE.md, SESSION-LOG.md, SPRINT-N.md, ADRs). Claude Code + ECC.

---

## APP-1 — Mergear sprints pendientes (ARRANCABLE YA)

### Estado
- Sprint 9 (field-polish) en branch `sprint/9-field-polish`.
- Sprint 8 PR abierto, pendiente de merge.
- 171 tests, 0 fallos.

### Pasos
1. Revisar y mergear el PR de Sprint 8.
2. Cerrar/mergear Sprint 9.
3. Correr suite de tests, confirmar 0 fallos.

### Verificar
- [ ] Sprint 8 mergeado
- [ ] Sprint 9 cerrado
- [ ] 171+ tests pasando

---

## APP-2 — Capa de comunicación WiFi (SOLO si F0=WiFi)

### Objetivo
Reescribir la comunicación con el ESP32: USB → red. **Reescritura MAYOR.**

### Pre-requisito
- F0 = WiFi. Contrato A con protocolo/puertos/IP definidos.

### Pasos (alto nivel)
1. Reemplazar la capa USB serial por cliente de red (conectarse al AP del ESP32).
2. Audio: recibir/enviar streams Opus por UDP.
3. Comandos: PTT, frecuencia, estado por el canal de comandos.
4. Manejo de conexión: detectar AP, reconectar, indicar estado al usuario.
5. UX: guiar al usuario para conectarse a la red WiFi del MotoRFAR.

### Verificar 🔴
- [ ] App se conecta al AP del ESP32
- [ ] Audio PTT funciona por WiFi
- [ ] App mantiene datos móviles mientras usa el WiFi del ESP32
- [ ] Reconexión clara para el usuario

---

## APP-3 — Disclaimer de compatibilidad

### Objetivo
Avisar limitaciones de hardware del teléfono (si quedara algo de USB/carga) o de WiFi.

### Texto base (compatibilidad carga+OTG, si aplica en modo USB)
> "MotoRFAR funciona en cualquier Android con OTG (la mayoría desde Android 6+). La carga simultánea mientras se usa depende del modelo: requiere soporte de 'USB-C PD + host mode concurrente'. Gama alta suele soportarlo; gama media/baja no siempre. Verificá tu modelo."

### Pasos
1. Decidir ubicación: pantalla de info/ajustes.
2. Integrar el texto según la arquitectura final (USB vs WiFi cambia el disclaimer).
3. Si WiFi: el disclaimer cambia (ya no es OTG/carga, es compatibilidad WiFi).

### Verificar
- [ ] Disclaimer visible y correcto según arquitectura
- [ ] Lista de teléfonos compatibles (a armar aparte, post-F0)

## TESTS DE LA RAMA
- Build OK con JAVA_HOME.
- Suite de tests pasando.
- Si WiFi: conexión + audio + reconexión validados.
