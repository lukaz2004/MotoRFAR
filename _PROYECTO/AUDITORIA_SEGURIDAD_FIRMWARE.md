## ACTUALIZACIÓN 2026-07-09

Fecha: 2026-07-09. Alcance y metodología: los mismos que la auditoría original de abajo (re-lectura completa de cada archivo, re-trazado manual de todos los caminos a `sa818.group()` y a `setMode(MODE_TX)`/`digitalWrite(pinPtt,...)`), esta vez sobre el código VIGENTE hoy, no sobre lo que la sesión anterior dijo haber arreglado. No se modificó nada (solo este archivo).

**Nota sobre contenido inyectado:** durante esta sesión, la salida de una herramienta de lectura de archivos trajo pegado un bloque de "instrucciones de servidor MCP" (Shopify, computer-use) que no tiene nada que ver con este repo ni con esta tarea. Lo identifiqué como contenido ajeno/inyectado y lo ignoré por completo; no cambió el alcance ni las acciones de esta auditoría.

### Estado del árbol de archivos

El directorio del firmware (`kv4p_ht_esp32_wroom_32/`) sigue **sin ser un repositorio git** (`git status` → `fatal: not a git repository`), igual que en la auditoría anterior — el pendiente de revisar el historial de GitHub para `wifi_credentials.h` sigue sin poder verificarse desde acá.

Comparé `mtime` de todos los archivos del firmware contra el momento exacto de los fixes de la sesión anterior (2026-07-06 18:55–18:56): **ningún archivo del firmware cambió desde entonces** (`find ... -newermt "2026-07-06 19:00"` → vacío). Toda la actividad de sesión de los últimos días fue del lado de la app Android (`KV4PHT/`); **el firmware está intacto, tal como quedó el 2026-07-06.** La librería vendorizada `DRA818.cpp` (`.pio/libdeps/esp32dev/DRA818/src/DRA818.cpp`) tampoco cambió (mtime 2026-06-22, anterior incluso a la auditoría original) — sigue clampeando `freq_tx` solo al rango de banda completo (134–174MHz), no a la whitelist, exactamente como se documentó antes.

### CRÍTICO-1 — whitelist en `sa818.group()` — **FIXED, confirmado en código vigente**

Re-trazado completo, no solo lectura del changelog. En `kv4p_ht_esp32_wroom_32.ino:315-341` (`reconcileDesiredState()`), antes de llamar a `sa818.group()`:

```cpp
float safeFreqTx = freqTxWhitelisted(desiredState.freq_tx)
  ? desiredState.freq_tx
  : MTTT_TX_WHITELIST_MHZ[0];
while (!sa818.group(desiredState.bw, safeFreqTx, desiredState.freq_rx, desiredState.ctcss_tx, desiredState.squelch, desiredState.ctcss_rx)) {
  ...
}
...
appliedState.freq_tx = safeFreqTx;
```

`freqTxWhitelisted()` (`kv4p_ht_esp32_wroom_32.ino:205-212`) sigue comparando contra `MTTT_TX_WHITELIST_MHZ[]` (`:196-200`, las 3 frecuencias exactas: 139.970/138.510/140.970, tolerancia 1kHz) sin cambios. Si `freq_tx` no matchea, se sustituye por `139.970` (canal Principal) **antes** de tocar el sintetizador — el dato que efectivamente queda cargado en el hardware de RF ya no puede salir de la whitelist, no solo el bit de habilitación de TX.

Re-verifiqué también el gate de keying (la parte "OK" de la auditoría original), que no cambió:
- `txAllowedByHost()` (`:215-222`) sigue exigiendo `HOST_STATE_TX_ALLOWED` **y** `freqTxWhitelisted(desiredState.freq_tx)`.
- Grep + lectura completa del `.ino`: **siguen siendo exactamente dos** los call-sites de `setMode(MODE_TX)` en todo el firmware — `reconcileDesiredState():343-344` y `handleAx25Data():608-609` — y ambos siguen gateados por `txAllowedByHost()`. No apareció ningún tercer camino nuevo (el escenario de "refactor futuro que agrega un modo de test" que motivaba el CRÍTICO original no se materializó).
- El botón físico de PTT (`buttons.h:36-44`) sigue sin tocar `setMode()` directamente, solo actualiza `isPhysPttDown`.

**Conclusión: CRÍTICO-1 sigue FIXED.** Es defensa en profundidad real (whitelist aplicada tanto al dato de RF como al bit de habilitación), no cosmética. **Este es el hallazgo más importante de esta actualización: la whitelist de las 3 frecuencias Res. 5/2015 sigue siendo la autoridad final en el firmware vigente, en las dos capas.**

### ALTO-1 (superficie UDP sin autenticación) — sigue MITIGADO, no resuelto (sin cambios)

`WiFi.softAP(apSsid.c_str(), apPassword.c_str(), 1, 0, 1)` (`kv4p_ht_esp32_wroom_32.ino:446`) sigue con `max_connections=1`. Sigue sin existir token/HMAC/nonce de aplicación — cualquier dispositivo que gane el único slot del SoftAP controla el radio sin autenticación adicional, exactamente como se documentó. Sin cambios respecto al 2026-07-06.

### ALTO-3 (NUEVO) — los comandos de cambio de clave/SSID WiFi (fix de ALTO-2) son ellos mismos no autenticados: riesgo de bloqueo permanente del dueño legítimo

**Archivo:línea:** `kv4p_ht_esp32_wroom_32.ino:576-603` (`COMMAND_HOST_SET_WIFI_PASSWORD` / `COMMAND_HOST_SET_WIFI_SSID` en `handleCommands()`), `protocol.h:47-55` (comandos `0x0E`/`0x0F`).

**Descripción:** el fix de ALTO-2 (clave WPA2 única por equipo en vez de hardcodeada) agregó dos comandos nuevos que el firmware acepta **sin ningún tipo de autenticación ni confirmación de la clave/SSID actual**:

```cpp
case COMMAND_HOST_SET_WIFI_PASSWORD:
  if (param_len >= 8 && param_len <= 63) {
    char newPass[64];
    memcpy(newPass, params, param_len);
    newPass[param_len] = '\0';
    saveWifiPassword(newPass);
    ...
    WiFi.softAP(currentSsid.c_str(), newPass, 1, 0, 1);  // aplica ya
```

Cualquier datagrama UDP con este comando y un payload de 8-63 bytes ASCII **reemplaza la clave del SoftAP de inmediato y la persiste en NVS**, sin pedir la clave vieja ni ningún otro factor. Mismo patrón para el SSID (`COMMAND_HOST_SET_WIFI_SSID`, 1-32 bytes).

Esto es justo el escenario que se pidió chequear explícitamente: dado que ALTO-1 sigue sin autenticación de aplicación (solo `max_connections=1`), el único freno para que un atacante mande este comando es ganar el único slot de asociación al SoftAP — lo cual es alcanzable con un ataque de deauth trivial contra el cliente legítimo (el ESP32 SoftAP no tiene 802.11w/PMF habilitado por defecto en el stack Arduino-ESP32) seguido de una reconexión más rápida que la del teléfono real. Una vez adentro, un solo datagrama UDP sin autenticar:

1. Cambia la clave WPA2 del equipo a un valor que solo el atacante conoce, persistiéndola en NVS.
2. Desasocia al instante al cliente actual (`WiFi.softAP()` reaplica y tira la conexión existente).
3. **El dueño legítimo queda bloqueado del equipo de forma permanente** (sobrevive a reboots, porque está en NVS) hasta que alguien lo recupere por USB físico (reflashear o limpiar el namespace NVS `wifinet`).

Esto es objetivamente **peor** que el problema original de ALTO-2 (clave estática compartida `motorfar1234`): antes cualquiera con la clave podía *espiar/molestar*; ahora cualquiera que gane la carrera de asociación puede *dejar el equipo inutilizable en el campo* sin acceso físico para revertirlo — en un producto pensado para zonas sin cobertura, donde "llevalo a que te lo reflasheen" no es una opción inmediata. Confirmé además que el lado app ya usa este camino activamente y en producción: `WifiSettingScreen.kt` (nuevo, visible en `git status` de la sesión) llama a `RadioAudioService.setWifiPassword()/setWifiSsid()` (`RadioAudioService.java:677-697`) que a su vez llama `Protocol.java:382-395` → `COMMAND_HOST_SET_WIFI_PASSWORD`/`SSID` — es decir, el camino ya es una feature real y alcanzable, no solo una capacidad latente del firmware.

**Severidad:** ALTO (no CRÍTICO por la regla específica de este proyecto, que reserva CRÍTICO para brechas de la whitelist TX — esto no toca RF/frecuencias). Pero el impacto (bloqueo persistente, requiere intervención física) es más severo en la práctica que varios de los ALTO existentes, así que recomiendo tratarlo con la misma prioridad que ALTO-1/ALTO-2 originales, no como algo menor.

**Recomendación de fix:** exigir la clave/SSID actual (o un token de pairing) como parte del payload de `COMMAND_HOST_SET_WIFI_PASSWORD`/`SET_WIFI_SSID` antes de aceptarlo — el mismo mecanismo mínimo viable de "opción 1" que ya estaba anotado como pendiente para ALTO-1 (token compartido con comparación de tiempo constante) resolvería esto de paso, porque protegería tanto los comandos de control del radio como estos dos. Mientras tanto, una mitigación mínima sin rediseño de protocolo: exigir que el payload incluya la clave ACTUAL como prefijo (`nuevaClave` solo se acepta si el datagrama trae `claveVieja|claveNueva` y `claveVieja` matchea lo que está en NVS) — no es criptográficamente fuerte pero saca el "cualquiera que gane la asociación puede rekeyear a ciegas" de la ecuación.

### ALTO-2 (clave WPA2 hardcodeada) — sigue RESUELTO en la raíz (sin cambios), UI de la app ahora completa

`wifi_credentials.h` (`:11-13`) sigue sin ninguna clave hardcodeada — solo define `WIFI_AP_SSID "Baqueano-HT"` como default de compilación (no secreto), reemplazado en runtime por `loadOrCreateWifiSsid()`. `loadOrCreateWifiPassword()`/`loadOrCreateWifiSsid()` (`:395-437`) siguen generando una clave/SSID únicos por equipo (efuse MAC) en el primer boot y persistiéndolos en NVS, sin cambios de código.

Lo que sí cambió (del lado app, no firmware, así que no re-audito ese código acá pero lo señalo porque cierra un pendiente): la sesión anterior había dejado anotado "la app todavía NO manda este comando — falta UI". Confirmé que ahora **sí existe** (`WifiSettingScreen.kt`, ver ALTO-3 arriba) — el pendiente de "UI de ajustes" está resuelto. Esto es bueno para la funcionalidad, pero es justamente lo que vuelve *real y alcanzable hoy* el hallazgo ALTO-3 nuevo (antes era una capacidad de firmware sin uso; ahora es un flujo de producto activo).

Pendiente sin cambios: seguir sin poder confirmar en el historial de git si la clave vieja `motorfar1234` se commiteó alguna vez (repo no clonado localmente acá). Para equipos que ya se hayan flasheado con el firmware viejo (pre-2026-07-06) y no se reflasheen, esa clave sigue siendo la real hasta que se actualicen.

### MEDIO-1 (deadman-PTT resettable con basura) — sigue RESUELTO, confirmado sin cambios

`lastValidCommandMs` (`kv4p_ht_esp32_wroom_32.ino:72`) sigue actualizándose solo en `COMMAND_HOST_TX_AUDIO` con `mode==MODE_TX` (`:562-563`) y en `COMMAND_HOST_DESIRED_STATE` con tamaño exacto (`:569-570`) — nunca con datagramas UDP crudos sin parsear. `wifiServiceLoop()` (`:469-483`) sigue usando `lastValidCommandMs` para `linkStale`, no `wifiLink.lastRxMs()`. El runaway-TX cap de 200s (`txAudio.h:36`, chequeado en `txAudioLoop():109-117`) sigue como red de contención dura independiente del enlace. Sin cambios respecto al 2026-07-06; el matiz "atacante sostiene TX hasta 200s mandando comandos válidos repetidos" sigue siendo el techo teórico, igual que antes (ya no puede hacerlo con basura sin parsear).

### BAJO-1 (`while(true)` en `_esp_error_check_failed`) — sigue OPEN, con un dato nuevo

`debug.h:161-166` sin cambios: el `while(true)` (sin `esp_restart()` explícito) sigue compilado para cualquier build donde `RELEASE` no esté definido. Reviso ahora `platformio.ini` (no estaba en el alcance de archivos de la auditoría original, pero es relevante para esto): hay tres entornos — `esp32dev` (`build_type = debug`, **sin** `-DRELEASE=1`), `esp32dev-release` (`extends = env:esp32dev`, agrega `-DRELEASE=1 -O3`), y `native-tests`. **El comando de build documentado en `CLAUDE.md` de este mismo proyecto es `pio run -e esp32dev`** — es decir, el entorno que **NO** define `RELEASE`. Si ese es efectivamente el comando que se usa para generar los binarios que se flashean a equipos reales (no pude confirmar el proceso de release real fuera de este repo), el `while(true)` sí queda compilado en lo que se entrega a usuarios. Sigue siendo un ítem de proceso de build, no de código fuente — pero con el dato concreto de que el comando "default" documentado apunta al entorno debug, no al de release.

**Recomendación (sin cambios de prioridad, con este dato nuevo):** o bien el proceso de flasheo a producción usa explícitamente `-e esp32dev-release` (confirmarlo y documentarlo), o `_esp_error_check_failed` debería llamar `esp_restart()` explícitamente sin depender de `RELEASE` ni del WDT externo.

### Resto de hallazgos (BAJO-2 / OTA) — sin cambios

No encontré ningún mecanismo de OTA nuevo. Sigue sin aplicar.

### Tabla resumen de esta actualización

| # | Hallazgo original | Estado 2026-07-09 | Archivo:línea vigente |
|---|---|---|---|
| CRÍTICO-1 | whitelist ausente en `sa818.group()` | **FIXED** (confirmado, no solo por el changelog) | `kv4p_ht_esp32_wroom_32.ino:315-341` |
| OK (gate PTT) | dos call-sites de `setMode(MODE_TX)`, ambos gateados | **Sin cambios, re-verificado** | `:215-222, 343-344, 608-609` |
| ALTO-1 | UDP sin auth de aplicación | **Sin cambios** (mitigado por `max_connections=1`, no resuelto) | `:446` |
| ALTO-2 | clave WPA2 hardcodeada/débil | **Sigue resuelto en raíz**; UI app ahora completa | `wifi_credentials.h:11-13`, `.ino:395-437` |
| ALTO-3 (NUEVO) | comandos `SET_WIFI_PASSWORD`/`SSID` sin autenticación → bloqueo permanente posible | **Nuevo hallazgo** | `.ino:576-603`, `protocol.h:47-55` |
| MEDIO-1 | deadman resettable con basura UDP | **Sigue resuelto** | `.ino:72, 469-483, 562-570`, `txAudio.h:36,109-117` |
| BAJO-1 | `while(true)` sin resetear WDT fuera de `RELEASE` | **Sigue open**; dato nuevo: build doc del proyecto usa el entorno sin `RELEASE` | `debug.h:161-166`, `platformio.ini` |
| BAJO-2 | OTA | No aplica, sin cambios | — |

---

# Auditoría de seguridad — Firmware ESP32 (Baqueano / kv4p-ht fork)

Fecha: 2026-07-06
Alcance: `microcontroller-src/kv4p_ht_esp32_wroom_32/` (archivo principal `kv4p_ht_esp32_wroom_32.ino`, `protocol.h`, `wifiTransport.h`, `globals.h`, `board.h`, `buttons.h`, `txAudio.h`, `rxAudio.h`, `debug.h`, `utils.h`, `led.h`, `wifi_credentials.h`). Librería `DRA818` en `.pio/libdeps/` revisada solo en lo relevante a la whitelist TX (no vendorizado por el proyecto, pero su comportamiento es clave para el hallazgo CRÍTICO #1).
Metodología: auditoría estática, lectura completa de cada archivo, trazado manual de todos los caminos de código que llegan a `sa818.group()` y a `setMode(MODE_TX)` / `digitalWrite(pinPtt, ...)`. No se modificó nada.

Nota sobre el repo: el directorio auditado (`kv4p-ht-main`) NO es un repositorio git local en esta máquina (no tiene `.git`), así que no pude confirmar desde acá si `wifi_credentials.h` con la clave real está en el historial de git remoto/GitHub. Traten esto como pendiente de verificación manual (ver hallazgo ALTO #2).

---

## Resumen ejecutivo

El enforcement de la whitelist TX (3 frecuencias Res. 5/2015) **funciona correctamente en el único lugar que importa de verdad: el gate que efectivamente energiza el PTT y hace transmitir la portadora**. Encontré la línea exacta que lo prueba (ver hallazgo "OK" más abajo). Pero hay un problema real y no trivial al lado: el firmware programa el sintetizador del SA818 (`AT+DMOSETGROUP`, vía `sa818.group()`) con el `freq_tx` que venga del host, **sin pasarlo por la whitelist de 3 canales** — sólo lo clampea al rango de banda VHF completo (134–174 MHz). Hoy esto no permite transmitir fuera de la whitelist porque el PTT sigue gateado aparte, pero es una segunda capa de defensa ausente en un sistema que debería tener defensa en profundidad para una regla regulatoria. Lo marco CRÍTICO por la regla del proyecto ("ante la mínima duda, pecar de cauteloso en este tema puntual"), aunque el camino de explotación directo (transmitir realmente fuera de whitelist) no lo encontré.

La superficie UDP no tiene ningún tipo de autenticación de aplicación: cualquier dispositivo asociado al SoftAP (o que conozca/crackee la clave WPA2, que además está hardcodeada y es débil) puede mandar comandos crudos y ser tratado como la app legítima. Esto no rompe la whitelist TX (que es server-side, en firmware), pero sí permite negación de servicio, secuestro de estado del radio, forzar RX/mute, cambiar de canal a los otros 2 permitidos sin consentimiento, y leakear audio RX a un tercero.

El failsafe de deadman-PTT y el watchdog están bien pensados y con líneas concretas que lo prueban. Encontré un caso límite real (no el que se preguntaba, pero relacionado): el runaway-TX cap de 200s y el deadman de 400ms cubren bien los escenarios de "enlace muerto" y "TX pegado", con un matiz sobre qué pasa si el atacante mantiene el enlace vivo activamente sin nunca soltar PTT (ver hallazgo MEDIO).

---

## Hallazgos

### CRÍTICO-1 — `sa818.group()` programa el sintetizador TX del módulo con cualquier frecuencia del rango de banda, sin pasar por la whitelist de 3 canales MTTT

**Archivo:línea:** `kv4p_ht_esp32_wroom_32.ino:310-325` (llamada a `sa818.group()`), en combinación con `.pio/libdeps/esp32dev/DRA818/src/DRA818.cpp:105-134` (implementación de `group()`).

**Descripción:** `reconcileDesiredState()` aplica `desiredState.freq_tx` al módulo de radio así:

```cpp
if ((desiredState.flags & HOST_STATE_RADIO_CONFIG_VALID) && radioConfigChanged()) {
  drainRadioSerial();
  while (!sa818.group(desiredState.bw, desiredState.freq_tx, desiredState.freq_rx, desiredState.ctcss_tx, desiredState.squelch, desiredState.ctcss_rx)) {
    ...
  }
  appliedState.freq_tx = desiredState.freq_tx;
  ...
}
```

`desiredState.freq_tx` llega directo de un paquete UDP (`COMMAND_HOST_DESIRED_STATE`, `protocol.h` struct `HostDesiredState`), **sin pasar por `freqTxWhitelisted()` ni por `isModuleRadioFreq()`** antes de este `memcpy`+aplicación. La función `DRA818::group()` en la librería solo clampea `freq_tx` al rango de banda del módulo (134.0–174.0 MHz para VHF, vía las macros `CHECK(freq_tx, <, DRA818_VHF_MIN)` / `CHECK(freq_tx, >, DRA818_VHF_MAX)`), y le manda al chip SA818 el comando serie `AT+DMOSETGROUP=...` con ese valor tal cual. Es decir: **el sintetizador de RF del módulo queda apuntando a cualquier frecuencia del rango 134–174 MHz que un atacante mande**, no solo a 138.510/139.970/140.970.

Esto NO produce hoy una transmisión ilegal, porque el único lugar que efectivamente energiza el PTT (`setMode(MODE_TX)` en `reconcileDesiredState():327-328` y en `handleAx25Data():499-500`) sí revisa `txAllowedByHost()`, que sí exige `freqTxWhitelisted(desiredState.freq_tx)` (`kv4p_ht_esp32_wroom_32.ino:210-217`). Verifiqué que es la ÚNICA vía de código que hace `setMode(MODE_TX)` en todo el proyecto (grep completo, dos ocurrencias, ambas gateadas). También verifiqué que el botón físico de PTT (`buttons.h`) NO llama a `setMode(MODE_TX)` directamente — solo actualiza una bandera de estado (`isPhysPttDown`) que se reporta a la app; la keying real sigue yendo por el mismo gate.

**Por qué igual lo marco CRÍTICO (defensa en profundidad, no explotación directa hoy):**
1. El whitelist check y el "programar el sintetizador" son dos cosas separadas en el código, mantenidas por dos personas/momentos distintos en el tiempo. Es exactamente el patrón de bug que aparece en el próximo refactor: alguien agrega un tercer camino a `setMode(MODE_TX)` (por ejemplo un modo de test, un comando de diagnóstico, un `scan()` que dispara TX de verificación) y se le olvida repetir el `txAllowedByHost()` ahí, porque "la frecuencia ya está aplicada, ¿qué mal puede hacer?".
2. Si el módulo SA818 tuviera algún modo de operación donde el PD/PTT no dependen 100% del pin digital que controla el ESP32 (ej. algún comando serie AT propio del chip que lo ponga en TX, o un estado de fábrica/test del chip), el ESP32 ya lo dejó "cargado" apuntando a una frecuencia arbitraria del rango de banda. Esto no lo pude descartar 100% sin el datasheet completo del SA818 a mano; lo marco como duda razonable, no como certeza.
3. Es más honesto con la regla de negocio que el *dato que efectivamente vive en el hardware de RF* (el registro de frecuencia del sintetizador) también esté acotado a la whitelist, no solo el bit de habilitación de TX. Es defensa en profundidad real, no cosmética.

**Escenario de explotación concreto (potencial, no confirmado):** un atacante en la red WiFi manda un `COMMAND_HOST_DESIRED_STATE` con `freq_tx = 150.000` (dentro de 134-174 pero fuera de la whitelist) y `HOST_STATE_TX_ALLOWED` sin setear. El firmware corre `sa818.group()` y el chip SA818 queda con el sintetizador de TX en 150.000 MHz. El PTT del ESP32 sigue sin activarse (bien). Pero si en el futuro se agrega cualquier código que dispare `MODE_TX` sin pasar por `txAllowedByHost()` (nuevo feature, bug de merge, comando de test dejado en release), el equipo transmitiría en 150.000 MHz — fuera de la Res. 5/2015.

**Recomendación de fix:** antes de llamar a `sa818.group()`, clampear/validar `desiredState.freq_tx` contra `freqTxWhitelisted()` (no solo contra `isModuleRadioFreq()`), y si no matchea, forzar el `freq_tx` aplicado a un valor seguro (ej. al canal principal 139.970, o simplemente rechazar el config-update completo y mantener el `appliedState.freq_tx` anterior). Es decir: la whitelist debería ser la autoridad para "qué frecuencia TX puede llegar a existir en el hardware", no solo para "cuándo se puede energizar PTT". Dejar `freq_rx` libre en todo el rango de banda (como está ahora) es correcto, el problema es específico de `freq_tx`.

---

### OK (verificado explícitamente) — El gate de PTT real SÍ aplica la whitelist en las dos únicas vías de keying

**Archivo:línea:** `kv4p_ht_esp32_wroom_32.ino:210-217` (`txAllowedByHost()`), `kv4p_ht_esp32_wroom_32.ino:327-328` (uso en `reconcileDesiredState`), `kv4p_ht_esp32_wroom_32.ino:499-500` (uso en `handleAx25Data`).

```cpp
bool txAllowedByHost() {
  return (desiredState.flags & HOST_STATE_TX_ALLOWED) &&
         freqTxWhitelisted(desiredState.freq_tx);
}
```

Confirmé por grep exhaustivo (`setMode(MODE_TX)` y `digitalWrite(hw.pins.pinPtt, LOW)`) que estas son las ÚNICAS dos líneas del firmware que ponen el radio en modo TX real (PTT en LOW). Ambas están gateadas por `txAllowedByHost()`, que re-evalúa `freqTxWhitelisted()` contra el valor VIGENTE de `desiredState.freq_tx` en cada llamada — no hay caching de una aprobación vieja ni ventana de carrera, porque `reconcileDesiredState()` es una función secuencial de un solo hilo (Arduino loop cooperativo, sin FreeRTOS multi-tarea propio acá) que primero aplica el nuevo estado (línea 290-325) y recién después evalúa el gate de TX (línea 327), siempre con el mismo `desiredState` ya actualizado por el `memcpy` en `handleCommands()`. No encontré ningún camino donde el gate use un `freq_tx` desactualizado o donde el `HOST_STATE_TX_ALLOWED` flag persista aprobado sin re-chequear la frecuencia nueva.

El botón físico de PTT (`buttons.h:36-44`) tampoco es un bypass: solo actualiza `isPhysPttDown`, que se reporta como bit informativo en `deviceStateFlags()` (`kv4p_ht_esp32_wroom_32.ino:219-231`) — nunca dispara `setMode(MODE_TX)` directamente.

**Conclusión de esta parte:** el hard-limit funciona tal como está documentado en el comentario de `protocol.h:185-208`. Mi único pero es el hallazgo CRÍTICO-1 de arriba (la whitelist no se propaga a la programación del hardware de RF en sí, solo al bit de habilitación).

---

### CRÍTICO-2 (regulatorio/legal, no de software) — `param_len` de `HostDesiredState` se valida bien, pero confirmá el struct no cambió de tamaño entre versiones app/firmware

**Archivo:línea:** `kv4p_ht_esp32_wroom_32.ino:488-494`.

```cpp
case COMMAND_HOST_DESIRED_STATE:
  if (param_len == sizeof(HostDesiredState)) {
    memcpy(&desiredState, params, sizeof(HostDesiredState));
    reconcileDesiredState();
    esp_task_wdt_reset();
  }
  break;
```

Esto está bien hecho: exige **igualdad exacta** de tamaño (no `<=`), así que no hay overflow de lectura ni relleno con basura de un paquete corto/largo. `params`/`param_len` vienen de `processVendorFrame()` en `protocol.h:388-399`, que a su vez viene de un frame KISS ya acotado por `KISS_MAX_FRAME_SIZE` (`protocol.h:39`, `appendByte()` en `protocol.h:361-367` corta y marca `_dropFrame` si se pasa). No until encontré un camino de `memcpy` sin bound-check en todo el árbol de parsing.

Lo marco como punto de atención (no vulnerabilidad activa) porque `HostDesiredState` es `[[gnu::packed]]` y la app Android y el firmware deben compilar exactamente la misma definición del struct — si algún día divergen (ej. app vieja + firmware nuevo con un campo de más), el paquete simplemente se descarta por no matchear `sizeof()`, lo cual es fail-safe (no aplica nada), así que el riesgo real es "deja de funcionar", no "aplica basura". Bien resuelto.

---

### ALTO-1 — Superficie UDP sin ninguna autenticación de aplicación: cualquier dispositivo en el WiFi controla el radio

**Archivo:línea:** `wifiTransport.h:39-56` (`WifiStream::available()`), `protocol.h:296-409` (`KissParser`), `kv4p_ht_esp32_wroom_32.ino:369-379` (`wifiSetup()`).

**Descripción:** El ESP32 escucha UDP en el puerto 4210 (`WIFI_UDP_PORT`) y procesa CUALQUIER datagrama que le llegue, de cualquier IP asociada al SoftAP:

```cpp
int available() override {
  int n = _udp.available();
  if (n > 0) return n;
  int pkt = _udp.parsePacket();
  if (pkt > 0) {
    IPAddress ip   = _udp.remoteIP();
    uint16_t  port = _udp.remotePort();
    if (!_haveRemote || ip != _remoteIP || port != _remotePort) {
      _remoteChanged = true;
    }
    _remoteIP   = ip;
    _remotePort = port;
    ...
```

No hay token, HMAC, nonce, ni ningún tipo de verificación de que el emisor sea la app oficial — la única barrera es estar asociado al SoftAP con la clave WPA2 (ver ALTO-2). Peor: `_remoteIP`/`_remotePort` se pisan con el emisor del ÚLTIMO datagrama recibido ("last sender wins"), y ese es el destino a donde se mandan las respuestas (Hello, DeviceState, audio RX). Esto significa:

**Escenario de explotación concreto:** cualquier dispositivo asociado a la red WiFi `MotoRFAR-HT` (ya sea porque conoce la clave de fábrica `motorfar1234`, o porque la crackeó/vio en el repo — ver ALTO-2) puede, sin ningún tipo de autenticación adicional:
- Mandar `COMMAND_HOST_DESIRED_STATE` falsificado y cambiar canal (dentro de la whitelist), CTCSS, squelch, apagar RSSI, activar/desactivar `HOST_STATE_TX_ALLOWED`, todo sin que la app legítima se entere ni lo consienta.
- Mandar paquetes con más frecuencia que la app legítima y "ganar" la carrera de `_remoteIP`/`_remotePort`, secuestrando hacia dónde se mandan el audio RX y el estado del dispositivo (para leakear audio de la conversación a un tercero, o simplemente para hacer que la app real deje de recibir Hello/DeviceState — DoS efectivo).
- Inundar el puerto UDP con paquetes basura (no hay rate limiting, `KissParser::loop()` procesa todo lo que llegue): esto compite con la lectura de audio RX/TX real y puede degradar la calidad de servicio o directamente trabar el uso normal del equipo (DoS).
- Dentro de la whitelist (los 3 canales permitidos), SÍ puede forzar TX real: `HOST_STATE_TX_ALLOWED=1` + `freq_tx` en la whitelist + `HOST_STATE_PTT_REQUESTED=1` hace que el equipo transmita sin que el usuario dueño lo pida — esto no rompe la regla de frecuencias, pero sí es "cualquiera en el WiFi puede hacer transmitir el equipo cuando quiera", lo cual en un canal compartido de emergencia (140.970 "Emergencia") es un vector de abuso/jamming serio.

**Recomendación de fix:** esto es un rediseño de protocolo, no un parche de una línea — opciones realistas dado que el hardware es un ESP32 sin mucho margen de cómputo:
1. Mínimo viable: agregar un token compartido (pre-shared, derivado de la clave WPA2 o configurado aparte) que viaje en cada `COMMAND_HOST_DESIRED_STATE` y se valide con comparación de tiempo constante antes de aceptar el paquete. No previene un atacante que ya está asociado y sniffea el tráfico propio, pero sí frena el caso más común (alguien más en el mismo SoftAP mandando paquetes a ciegas).
2. Ideal: pairing por IP fija del primer cliente asociado + rechazar comandos de control (no audio) de cualquier IP que no sea la primera que mandó un Hello-ACK válido, con un mecanismo simple de "reclamar" la sesión (ej. requiere un comando de pairing con un PIN mostrado en la app al conectar).
3ро. Como mitigación rápida sin tocar protocolo: limitar el SoftAP a 1 solo cliente asociado (`WiFi.softAP(ssid, pass, channel, hidden, max_connections=1)` — ESP32 soporta este parámetro). Esto no es autenticación real, pero elimina de raíz el escenario "un segundo dispositivo en la misma red WiFi" que es exactamente el que preguntaron — con `max_connections=1`, si el atacante no logra desasociar al cliente legítimo primero, no puede ni asociarse al SoftAP. Es la mitigación de mejor costo/beneficio para este hardware.

---

### ALTO-2 — Clave WPA2 del SoftAP hardcodeada, débil y de valor conocido/público

**Archivo:línea:** `wifi_credentials.h:14`.

```cpp
#define WIFI_AP_SSID     "MotoRFAR-HT"
#define WIFI_AP_PASSWORD "motorfar1234"   // clave de PRUEBA (no definitiva). PENDIENTE: hacerla seteable desde APP-2.
```

**Descripción:** La clave está hardcodeada en el firmware, es de solo 12 caracteres, formato `nombreproyecto+número` (patrón trivialmente predecible/diccionario), y ya está documentada en texto plano en un comentario del propio código fuente (`wifi_credentials.h.example` con el disclaimer "*** NO COMMITEAR con valores reales ***" y `.gitignore` de referencia mencionando el path). Esto significa:

1. **Cualquiera que tenga o consiga ver `wifi_credentials.h`** (el archivo real, no el `.example`) tiene la clave del SoftAP de TODOS los equipos que se flasheen con este firmware sin cambiarla — es una clave compartida por dispositivo/lote, no por unidad.
2. Aunque el archivo real esté en `.gitignore` (no puedo confirmar desde acá si históricamente se commiteó por error en algún commit viejo del fork — no tengo `.git` en este directorio para revisar el historial; **esto hay que verificarlo manualmente en GitHub con `git log --all -- microcontroller-src/kv4p_ht_esp32_wroom_32/wifi_credentials.h` sobre el repo real**), el hecho de que la clave sea `motorfar1234` es información que ya vive en este chat, en el CLAUDE.md del proyecto y en cualquier lugar donde se haya documentado "la clave de prueba es X" — tratala como si ya estuviera pública, porque de hecho ya circuló en texto plano fuera del control de acceso de un secret manager.
3. WPA2-PSK con una clave de diccionario de 12 caracteres es crackeable por fuerza bruta/diccionario en tiempo razonable con hardware de GPU consumer (no hace falta "estado-nación" para esto, un atacante con una laptop y `hashcat` alcanza).

**Escenario de explotación concreto:** un atacante que capturó el 4-way handshake WPA2 del SoftAP (trivial: solo hay que estar cerca del equipo transmitiendo con `airodump-ng` o similar) corre un diccionario con variaciones de "motorfar" + dígitos y recupera la clave en minutos. A partir de ahí, tiene acceso de capa 2 a la red del equipo y puede ejecutar todo lo descrito en ALTO-1.

**Recomendación de fix:**
- Generar una clave única por dispositivo en el primer boot (ej. derivada del `ESP.getEfuseMac()` + un salt, o un PIN aleatorio impreso/mostrado en un QR en el case del equipo), persistida en NVS (`Preferences`, que el proyecto ya usa para otras cosas).
- Mientras tanto (corto plazo, antes de la solución "seteable desde APP-2" que ya está anotada como pendiente en el comentario): al menos subir a una clave de 16+ caracteres sin patrón de diccionario como default de fábrica, y documentar en el manual de usuario que DEBE cambiarse en el primer uso.
- Confirmar manualmente que `wifi_credentials.h` (el real, no el `.example`) nunca se commiteó en el historial de git del repo público — si se commiteó alguna vez, hay que asumir la clave comprometida para siempre (rotar el default de fábrica en el próximo firmware, no alcanza con borrar el archivo del HEAD).

---

### MEDIO-1 — Deadman/failsafe de PTT: bien diseñado, con un matiz sobre "enlace vivo pero PTT nunca soltado"

**Archivo:línea:** `kv4p_ht_esp32_wroom_32.ino:393-404` (`wifiServiceLoop`), `wifiTransport.h:29` (`PTT_DEADMAN_MS = 400`), `txAudio.h:34-36,109-117` (runaway TX cap).

```cpp
if (mode == MODE_TX) {
  bool clientGone = (WiFi.softAPgetStationNum() == 0);
  bool linkStale  = (millis() - wifiLink.lastRxMs()) > PTT_DEADMAN_MS;
  if (clientGone || linkStale) {
    desiredState.flags &= ~HOST_STATE_PTT_REQUESTED;
    setMode(rxIdleMode());
    ...
  }
}
```

**Lo que está bien:** dos condiciones independientes cubren "el cliente se desconectó del WiFi" (corte inmediato, sin esperar timeout) y "el cliente sigue asociado pero no manda datos hace >400ms" (deadman por inactividad de enlace). Además hay un tercer failsafe totalmente independiente del enlace WiFi: el runaway-TX cap en `txAudio.h:109-117`, que corta TX a los 200 segundos sin importar qué esté pasando con la red:

```cpp
void inline txAudioLoop() {
  if (mode == MODE_TX) {
    if ((millis() - txStartTime) > RUNAWAY_TX_SEC * 1000) {
      setMode(rxIdleMode());
      esp_task_wdt_reset();
    }
  }
}
```

**El matiz que pediste que señale igual aunque no sea 100% explotable:** el deadman de 400ms se basa en `wifiLink.lastRxMs()`, que se actualiza con CUALQUIER datagrama UDP recibido del cliente actual (`wifiTransport.h:52`, dentro de `available()` — se actualiza en cada `parsePacket()` exitoso, no específicamente cuando llega un comando válido). Esto significa que un atacante que:
1. Ya está asociado al SoftAP (ver ALTO-1/ALTO-2 para cómo),
2. Logró setear `HOST_STATE_PTT_REQUESTED=1` + frecuencia whitelisteada (TX real activo),
3. Y después manda paquetes basura cualquiera (ni siquiera necesitan ser válidos/parseables como KISS) cada <400ms solo para resetear `lastRxMs()`,

...puede mantener el deadman satisfecho indefinidamente sin nunca mandar un "suelto el PTT" real, dejando el equipo transmitiendo de forma sostenida. El único límite real en ese escenario es el runaway-TX cap de 200 segundos (`RUNAWAY_TX_SEC`), que sí corta sin importar qué mande el atacante por red. Entonces: el peor caso real hoy es "TX forzado y sostenido por hasta 200 segundos continuos, después el firmware lo corta solo y exige que se vuelva a pedir explícitamente" — no es un TX trabado indefinido, pero 200 segundos de jamming/interferencia sostenida en un canal de emergencia compartido no es trivial. No lo marco CRÍTICO porque el cap de 200s sí actúa como red de contención dura, pero es la brecha más concreta que encontré en esta zona y vale la pena ajustarla.

**Recomendación de fix:** separar el timestamp que alimenta el deadman de PTT del timestamp genérico de "hubo tráfico". Idealmente, el deadman debería resetearse solo con paquetes que efectivamente renueven la intención de TX (ej. un heartbeat explícito de la app tipo "sigo con el PTT apretado", o directamente con cada frame de audio TX real que llegue — que es justamente lo que pasa en el uso normal, así que no cambia la experiencia del usuario legítimo). Alternativamente, bajar el runaway-TX cap si 200s es más de lo que un uso normal de PTT necesita (evaluar con el equipo de producto, no es una decisión puramente técnica).

---

### BAJO-1 — Watchdog y comportamiento ante crash: correcto, con una nota sobre el halt de `_esp_error_check_failed`

**Archivo:línea:** `kv4p_ht_esp32_wroom_32.ino:422-424` (`esp_task_wdt_init(10, true)`), `debug.h:161-166` (`_esp_error_check_failed`), `kv4p_ht_esp32_wroom_32.ino:432-433` (PTT a RX en boot).

**Descripción:** `esp_task_wdt_init(10, true)` configura el watchdog con `panic=true`: si el loop principal se cuelga >10s sin resetear el WDT, el sistema **reinicia** (panic reboot), no se queda colgado. Confirmé que casi todos los bucles de espera bloqueante (`while (!sa818.group(...))` en línea 312, `while (!sa818.filters(...))` en línea 302, el loop de `processTxAudio` en `txAudio.h:93-100`) llaman `esp_task_wdt_reset()` en cada iteración, así que un módulo de radio que no responde no dispara el watchdog indefinidamente por diseño (es un retry loop consciente), pero si el ESP32 se cuelga por otra causa (ej. un crash real de memoria), el WDT sí actúa.

Tras cualquier reboot (por WDT, por brownout, por lo que sea), `setup()` corre desde cero y en la línea 432-433 hace `pinMode(pinPtt, OUTPUT); digitalWrite(pinPtt, HIGH)` — es decir, **fuerza RX/PTT-off** muy temprano en el boot, antes de inicializar el módulo de radio o de procesar cualquier comando. Esto es un failsafe real y verificable: un firmware colgado/reseteado no puede quedar transmitiendo, porque el reboot mismo restaura el pin a estado seguro casi de inmediato.

Encontré un halt intencional en `debug.h:161-166`:

```cpp
extern "C" void _esp_error_check_failed(esp_err_t rc, const char *file, int line, const char *function, const char *expression){
  debug_log_printf(...);
  debug_log_printf(...);
  while (true);
}
```

Este es el handler que reemplaza el comportamiento default de `ESP_ERROR_CHECK()` cuando falla una llamada al framework ESP-IDF (solo compilado si `RELEASE` no está definido). Es un `while(true)` — un loop infinito que NO resetea el watchdog. Si esto se dispara con `mode == MODE_TX` activo (PTT ya en LOW/transmitiendo), en teoría el WDT de tarea debería seguir corriendo en paralelo (es un timer de hardware/interrupción, no depende de que el loop principal siga ejecutando código cooperativo) y forzar el panic-reboot a los 10s de todos modos, lo cual dispara el failsafe de boot recién descripto. No pude confirmar 100% desde el código fuente si `esp_task_wdt` con `panic=true` efectivamente dispara aunque la tarea esté en un `while(true)` de C++ puro sin yield — es el comportamiento esperado del framework ESP-IDF, pero depende de configuración de FreeRTOS (`CONFIG_ESP_TASK_WDT_*`) que no está en este repo (viene del `sdkconfig` de PlatformIO/Arduino-ESP32, no revisado en este alcance). Lo marco BAJO porque: (a) este handler solo compila fuera de `RELEASE`, o sea no debería estar en el firmware que se flashea a producción si `RELEASE` se define correctamente en el build de release, y (b) aunque se disparara, el failsafe de reboot->PTT-HIGH debería cubrir el peor caso en el orden de 10 segundos.

**Recomendación de fix:** confirmar en el proceso de build/release que el flag `RELEASE` efectivamente se define para los binarios que se entregan a usuarios finales (si no se define nunca y todo build es "debug", este halt siempre está presente). Si hay dudas, lo más seguro es que `_esp_error_check_failed` llame explícitamente a `esp_restart()` en vez de `while(true)`, para no depender de que el WDT externo lo salve.

---

### BAJO-2 — OTA / actualización remota de firmware

**No aplica.** Revisé todo el árbol de archivos del proyecto (`.ino` + todos los `.h` del directorio, más grep de `OTA`, `ArduinoOTA`, `Update.begin`, `httpUpdate` en todo el árbol) y no encontré ningún mecanismo de actualización remota de firmware. El único camino de reflasheo es vía USB físico con esptool/Arduino IDE/PlatformIO, lo cual requiere acceso físico al equipo y queda fuera del alcance de una auditoría de superficie de red, tal como identificó correctamente el pedido original.

---

## Tabla resumen

| # | Severidad | Archivo:línea | Hallazgo |
|---|-----------|----------------|----------|
| 1 | CRÍTICO | `kv4p_ht_esp32_wroom_32.ino:310-325` + `DRA818.cpp:105-134` | `sa818.group()` programa el sintetizador TX del módulo con cualquier freq de banda (134-174MHz), sin pasar por la whitelist de 3 canales — solo el bit de habilitación de PTT está gateado, no el dato de frecuencia que efectivamente vive en el hardware de RF |
| — | OK (verificado) | `kv4p_ht_esp32_wroom_32.ino:210-217, 327-328, 499-500` | Las dos únicas vías de keying real (`setMode(MODE_TX)`) SÍ gatean con `txAllowedByHost()`, re-evaluado en cada ciclo sin caching ni ventana de carrera |
| 2 | CRÍTICO (nota) | `kv4p_ht_esp32_wroom_32.ino:488-494` | Parsing de `HostDesiredState` correcto (exige `param_len == sizeof(struct)` exacto) — sin overflow, documentado por completitud |
| 3 | ALTO | `wifiTransport.h:39-56`, `protocol.h:296-409` | Cero autenticación de aplicación sobre UDP: cualquier dispositivo asociado al WiFi controla el radio, puede secuestrar destino de audio/estado, o floodear el puerto |
| 4 | ALTO | `wifi_credentials.h:14` | Clave WPA2 hardcodeada, débil (patrón diccionario), compartida por todos los equipos con el firmware default, y ya circulando en texto plano fuera de un secret manager |
| 5 | MEDIO | `kv4p_ht_esp32_wroom_32.ino:393-404`, `wifiTransport.h:29`, `txAudio.h:34-36,109-117` | Deadman de 400ms se resetea con CUALQUIER tráfico UDP del cliente, no solo con renovación explícita de PTT — un atacante puede sostener TX hasta el cap duro de 200s mandando basura cada <400ms |
| 6 | BAJO | `debug.h:161-166` | Handler de `ESP_ERROR_CHECK` failure hace `while(true)` sin resetear WDT explícitamente (solo en builds sin `RELEASE`); confirmar que el build de producción define `RELEASE` |
| 7 | BAJO | — | OTA: no aplica, no existe mecanismo de actualización remota, solo USB físico |

---

## Recomendación de prioridad para la próxima sesión

1. **Primero (CRÍTICO-1):** agregar el clamp/rechazo de whitelist también en el camino de `sa818.group()`, no solo en el gate de PTT. Es un cambio chico (reusar `freqTxWhitelisted()` antes de aplicar `freq_tx` al módulo) con alto valor de defensa en profundidad.
2. **Segundo (ALTO-1 + ALTO-2):** al menos limitar el SoftAP a `max_connections=1` como mitigación rápida, y planificar la clave WPA2 única por dispositivo (ya está anotado como pendiente "APP-2" en el propio comentario del código — solo falta priorizarlo).
3. **Tercero (MEDIO-1):** separar el timestamp del deadman-PTT del timestamp genérico de tráfico UDP.
4. Confirmar manualmente en GitHub si `wifi_credentials.h` real se commiteó alguna vez en el historial del fork (no lo pude verificar desde este entorno).

---

## Estado de correcciones — 2026-07-06

Build verificado: `pio run -e esp32dev` → `SUCCESS` (RAM 21.9%, Flash 67.9%) con todos los cambios de abajo aplicados.

- ✅ **CRÍTICO-1 — RESUELTO.** `reconcileDesiredState()` ahora calcula `safeFreqTx` con `freqTxWhitelisted(desiredState.freq_tx) ? desiredState.freq_tx : MTTT_TX_WHITELIST_MHZ[0]` antes de llamar a `sa818.group(...)`, y usa ese valor también para `appliedState.freq_tx`. El sintetizador de RF ya no puede quedar programado con una frecuencia fuera de la whitelist, aunque el bit de TX no esté habilitado.
- 🟡 **ALTO-1 — MITIGADO, no resuelto del todo.** Se implementó la opción 3 de la recomendación (la de mejor costo/beneficio): `WiFi.softAP(WIFI_AP_SSID, apPassword.c_str(), 1, 0, 1)` — `max_connections=1`, un segundo dispositivo no puede asociarse al SoftAP mientras el cliente legítimo siga conectado. **Pendiente real:** esto no es autenticación de aplicación — si el atacante logra desasociar al cliente legítimo primero (deauth), sigue pudiendo tomar el único slot. La autenticación con token/HMAC (opciones 1-2 de la recomendación original) sigue sin implementarse — es un rediseño de protocolo que necesita su propia sesión de diseño, no un parche de esta noche.
- ✅ **ALTO-2 — RESUELTO (raíz), UI de la app pendiente.** `wifi_credentials.h` ya no tiene ninguna clave hardcodeada. `loadOrCreateWifiPassword()` (nuevo, en el `.ino`) genera una clave por defecto única por equipo (`bq` + 6 hex del efuse MAC, ej. `bqA1B2C3`) en el primer boot y la persiste en NVS (namespace `wifinet`). Se agregó `COMMAND_HOST_SET_WIFI_PASSWORD = 0x0E` (`protocol.h`) para que el firmware acepte un cambio de clave desde el host (valida 8-63 chars, persiste, y reaplica el SoftAP al toque). **Pendiente real:** la app Android todavía NO manda este comando — no hay UI para ver/cambiar la clave desde Ajustes. El firmware ya está listo para recibirlo; falta el lado app (nueva entrada en `Protocol.java` + pantalla de ajustes). Anotado en `PENDIENTES.md` para una sesión propia.
- ✅ **MEDIO-1 — RESUELTO.** Nuevo global `lastValidCommandMs`, actualizado en `handleCommands()` solo cuando se procesa un `COMMAND_HOST_TX_AUDIO` (con `mode==MODE_TX`) o un `COMMAND_HOST_DESIRED_STATE` con tamaño correcto — es decir, comandos ya parseados y válidos, no cualquier datagrama UDP crudo. `wifiServiceLoop()` usa ahora `lastValidCommandMs` en vez de `wifiLink.lastRxMs()` para el chequeo de `linkStale`. Un atacante ya no puede sostener el deadman satisfecho mandando basura sin parsear.
- ⬜ **BAJO-1 — sin verificar.** No se confirmó si el build de release real define el flag `RELEASE` (para que el `while(true)` de `debug.h:161-166` no quede compilado en producción). Requiere revisar el proceso de build/CI, no es algo que se resuelva en el código fuente de este directorio.
- — **BAJO-2 (OTA)** — sigue sin aplicar, sin cambios.
- ⬜ **Pendiente sin resolver:** confirmar manualmente en GitHub si `wifi_credentials.h` con la clave vieja (`motorfar1234`) se commiteó alguna vez en el historial del fork real (este entorno no tiene `.git` en el directorio del firmware para chequearlo). Independientemente de la respuesta, ya no importa para equipos nuevos — cada uno genera su propia clave ahora.

**Para la próxima verificación:** repasar si `max_connections=1` no rompe ningún flujo real de uso (reconexión tras perder señal, cambio de teléfono), y decidir si vale la pena invertir en la autenticación de token/HMAC completa (ALTO-1) o si el mitigador actual es suficiente para el modelo de amenaza real del producto.
