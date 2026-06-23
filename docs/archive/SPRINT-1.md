# SPRINT 1 — Migración a VHF Res. 5/2015 + identidad MotoRFAR

> Plan de ejecución para la primera tanda de trabajo en Claude Code. Generado tras auditoría del código baseline el 2026-06-09. Ejecutar con flujo Superpowers (spec → plan → TDD → review).

## Objetivo del sprint

Al terminar, la app debe: (1) operar exclusivamente en los 3 canales VHF de Res. 5/2015, (2) tener imposibilitada por código la TX fuera de esas frecuencias, (3) rutear EMERGENCIA al canal 140.970 automáticamente, y (4) reflejar la identidad legal correcta en textos y T&C. El theme ámbar CRT es la fase final si queda tiempo (sino pasa a Sprint 2).

## Hallazgos de la auditoría (estado actual del código)

### H1 — Los límites TX actuales BLOQUEAN nuestras frecuencias ⚠️ CRÍTICO

`RadioAudioService.java` líneas 143-154: los límites TX por defecto son 144.0-148.0 MHz (banda 2m radioaficionados) y 420-450 MHz (70cm). **Nuestros 3 canales (138.510, 139.970, 140.970) están FUERA del rango permitido** — la app tal cual está no puede transmitir en M.T.T.T. La validación ocurre en `canTransmitOnFrequency()` (línea ~600).

### H2 — El preload de canales tiene mecanismo de versionado

`AppDatabase.java` líneas 62-115: `preloadArgentinaChannelsIfNeeded()` chequea el setting `argentina_channels_preloaded` contra `ArgentinaChannels.PRELOADED_VALUE` (hoy `"v2_pmr446"`). Cambiar ese valor a `"v3_mttt_vhf"` fuerza re-seed en instalaciones existentes. El mecanismo ya existe, solo hay que usarlo.

### H3 — Ya existe el mecanismo "sintonizar → TX → volver"

`RadioAudioService.java` líneas 1553-1567: el beacon APRS sintoniza `aprsBeaconFrequency`, transmite, y vuelve a `originalFrequencyStr`. **Este mismo patrón sirve para rutear EMERGENCIA a 140.970** sin escribir lógica nueva de cero.

### H4 — Lista completa de archivos con referencias PMR446/446

1. `data/ArgentinaChannels.java` — los 16 canales (núcleo del cambio)
2. `data/AppDatabase.java` — preload (solo cambia por la constante)
3. `ui/AddEditMemoryActivity.java` líneas 55-60 — lista hardcodeada de frecuencias PMR para el dropdown manual
4. `app/src/test/.../ProtocolKissTest.java` línea 713 — un test usa 446.0f como frecuencia de prueba
5. `res/layout/activity_add_edit_memory.xml` — labels del form

### H5 — AlertHelper no rutea canales

`ui/AlertHelper.java` define textos y tipos pero no decide frecuencia. El routing hay que agregarlo (con el patrón de H3).

---

## Fases de ejecución (orden TDD)

### FASE A — Datos de canales [~1h]

**A1. Test primero** — `ArgentinaChannelsTest.java` (nuevo):
- `getAll()` devuelve exactamente 3 canales
- Frecuencias exactas: "138.5100", "139.9700", "140.9700"
- Nombres: "Grupo", "Alternativo", "Emergencia"
- Grupo de memoria: "MTTT"
- `PRELOADED_VALUE` es "v3_mttt_vhf"

**A2. Implementar** — reescribir `ArgentinaChannels.java`:
```java
// Resolución 5/2015 Secretaría de Comunicaciones — M.T.T.T.
// Frecuencias de uso libre sin licencia para travesías, expediciones,
// motociclismo y actividades en zonas rurales/inhóspitas.
// 138.510 secundaria · 139.970 prioritaria · 140.970 EXCLUSIVA emergencias
```
- Canal "Grupo" = 139.970 (prioritaria, defecto)
- Canal "Alternativo" = 138.510
- Canal "Emergencia" = 140.970
- PRELOADED_VALUE = "v3_mttt_vhf"

**A3. Actualizar** `AddEditMemoryActivity.java` líneas 55-60 — reemplazar el array de 16 PMR por las 3 M.T.T.T. (el usuario puede agregar memorias manuales solo dentro de las 3).

### FASE B — Hard-limit TX (seguridad regulatoria) [~2-3h] ⚠️ LA MÁS IMPORTANTE

**B1. Test primero** — `TxWhitelistTest.java` (nuevo):
- TX permitida en 138.510, 139.970, 140.970 (con tolerancia float ±0.0001)
- TX bloqueada en: 138.500, 139.000, 144.800, 146.520, 446.000, 462.5625, cualquier otra
- TX bloqueada incluso si una "memoria" manual tiene otra frecuencia

**B2. Implementar** en `RadioAudioService.java`:
- Reemplazar la lógica de rangos min/max por whitelist explícita:
```java
private static final float[] TX_WHITELIST_MHZ = {138.510f, 139.970f, 140.970f};
private static final float TX_FREQ_TOLERANCE = 0.0005f;
```
- `canTransmitOnFrequency()` valida contra whitelist, no contra rango
- Eliminar (o dejar no-op con deprecation) `setMin2mTxFreq`/`setMax2mTxFreq`/`setMin70cmTxFreq`/`setMax70cmTxFreq`/`updateTxLimitsForBand`
- RX queda libre en todo el rango del SA818-V (134-174) — escuchar es legal

**B3. Limpiar** `MainActivity.java` `applyTxFreqLimitsSettings()` (línea ~957) — eliminar la lectura de settings de bandas ham. Verificar si `AppSetting` tiene constantes SETTING_MIN_2_M_TX_FREQ etc. y deprecarlas (no borrar la columna de DB, solo dejar de usarlas — no romper migraciones Room existentes).

**B4. Arreglar test existente** `ProtocolKissTest.java` línea 713 — cambiar `setTxFrequency(446.0f)` por `139.970f`.

### FASE C — Routing de alertas [~2h]

**C1. Test primero** — `AlertRoutingTest.java` (nuevo):
- EMERGENCY → se transmite en 140.970 y vuelve al canal previo
- STOP y REGROUP → se transmiten en el canal activo del usuario
- Si el canal activo YA es 140.970, EMERGENCY no hace switch

**C2. Implementar**:
- `AlertHelper.java`: agregar `getTargetFrequency(AlertType, String currentFreq)` que devuelve "140.9700" para EMERGENCY y `currentFreq` para el resto
- `RadioAudioService.java`: nuevo método `sendAlertOnFrequency(...)` reutilizando el patrón beacon de líneas 1553-1567 (tune → TX → restore)
- Sumar al texto de confirmación de EMERGENCY: "Se transmitirá por el canal de emergencia 140.970 MHz, monitoreado por otros grupos M.T.T.T. y entidades en la zona."

### FASE D — Identidad y textos [~1-2h]

**D1.** `strings.xml`:
- Eliminar referencias a PMR446, 144.800 APRS estándar y bandas ham
- Frecuencia de baliza por defecto → 139.970
- Nombres de canal y textos de UI según ADR-003

**D2.** `activity_terms.xml` — reescribir T&C citando exclusivamente Res. 5/2015 (usar contenido de `docs/02-MARCO-LEGAL.md` sección T&C). 8 cláusulas: marco habilitante, las 3 frecuencias, equipo experimental open source, potencia, responsabilidad del usuario, GPL-3.0, privacidad GPS local, atribución kv4p HT.

**D3.** `AndroidManifest.xml` — verificar permisos contra `.claude/claude-security-guidance.md` regla 3 (INTERNET no debería estar en v1.0; si está, documentar por qué o sacarlo).

### FASE E — Theme ámbar CRT [~3-4h, pasa a Sprint 2 si no hay tiempo]

**E1.** `colors.xml` + `themes.xml` — paleta ámbar de `docs/05-DISEÑO.md` (valores exactos ahí)
**E2.** Layout principal según mockup (`docs/assets/`): selector 3 canales arriba, display frecuencia + VU, grupo, PTT central, alertas abajo
**E3.** El toggle ámbar/verde queda para Sprint 2 — primero que el ámbar solo quede bien

---

## Definición de "hecho" del sprint

- [ ] `./gradlew test` pasa completo (tests nuevos + existentes arreglados)
- [ ] Imposible transmitir fuera de las 3 frecuencias (verificado por test)
- [ ] Re-seed de canales funciona en upgrade desde install previa (probar con APK viejo → nuevo)
- [ ] T&C cita Res. 5/2015 y no menciona PMR446
- [ ] `git log` muestra commits atómicos por fase (A, B, C, D separados)
- [ ] SESSION-LOG.md actualizado al cierre

## Notas para la sesión de Claude Code

- Usar `/brainstorm` solo si surge una decisión no cubierta por los ADRs; el grueso ya está decidido, ir directo a `/write-plan` con este documento como input
- FASE B es la crítica de seguridad: pedir review del plugin security-guidance explícitamente al terminarla
- No renombrar el package `com.vagell.kv4pht` en este sprint (es refactor mayor, ADR-006 lo deja para más adelante)
- Si `gradlew` falla por entorno (JDK, SDK path), resolver primero — es bloqueante para TDD
