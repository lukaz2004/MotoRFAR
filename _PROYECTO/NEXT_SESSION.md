# BAQUEANO — Prompt de arranque de sesión
> Copiá y pegá esto al inicio de cada chat. Claude lee este archivo + `05_VISION.md` y arranca.

## ✅ CIERRE 2026-07-14 — Mapas offline por provincia: probado y funcionando en el Huawei P9
Tres causas independientes bloqueaban la pantalla nueva de mapas offline (cada
una tapaba a la siguiente, se fueron pelando una por una con evidencia real
de logcat/TLS, no a los tiros):
1. **Repo privado.** `lukaz2004/MotoRFAR` estaba privado -- `raw.githubusercontent.com`
   y los assets del release devuelven 404 sin auth aunque el release no sea
   draft. LuKaZ lo puso público a mano (yo no puedo cambiar visibilidad de
   repos). Antes de recomendarlo se revisó todo el historial de commits (no
   solo el HEAD) buscando secrets/API keys/credenciales -- nada encontrado;
   `.gitignore` ya excluía `keystore.properties`/`*.jks`/`.env` desde antes.
2. **Android 7.0 (API 24, el Huawei P9) no trae ISRG Root X1 de fábrica**
   (recién viene en 7.1.1+). GitHub/Fastly usa Let's Encrypt -- sin ese root
   como trust-anchor, la cadena TLS no valida en ese equipo especifico,
   aunque el resto de la app (tiles OSM, etc.) sí ande. Fix: se bundleó el
   cert (`res/raw/isrg_root_x1.pem`) y se agregó un `domain-config` en
   `network_security_config.xml` scoped a `github.com`/`githubusercontent.com`
   con ese cert como anchor extra (no se tocó el resto de la política HTTPS).
3. **Bug de Compose real, ya con el manifest y la descarga andando:**
   `LaunchedEffect(state)` en `ProvinceRow` (`OfflineVectorMapsScreen.kt`)
   estaba keyeado sobre el mismo objeto `state` que su propio callback de
   progreso reemplazaba en cada chunk leído -- cada tick de progreso
   cancelaba y relanzaba el efecto, matando la descarga en curso apenas
   arrancaba (por eso "no descarga" pasaba siempre, al toque). El archivo
   igual terminaba de escribirse y pasaba la verificación de hash (la
   cancelación llegaba después del write, por cómo corren las corrutinas de
   IO bloqueante), pero la UI mostraba error igual. Fix: key estable
   (`LaunchedEffect(info.iso, state is DownloadState.Downloading)`).

**Verificado en el dispositivo real (Huawei P9, Android 7.0), no solo build verde:**
lista de provincias carga, descarga de CABA (7,4 MB) se completa con hash
verificado (7.764.134 bytes, coincide con el manifest), "VER MAPA" renderiza
el vector Mapsforge con calles/costa/pan-zoom. `DESCARGAR MAPA DE ARGENTINA`
(OSMDroid) sigue mostrando su mensaje de "no permite descarga masiva" --
eso es la política del propio servidor OSM, no un bug, ya andaba así.

## 🗺️ CIERRE 2026-07-13 — Mapas offline vectoriales (Mapsforge), pantalla nueva
Sub-proyecto 2 de mapas offline (ver PENDIENTES.md) implementado: la app solo
tenía OSMDroid (raster), pero el manifest ya publicado (`provincias.json`)
generó `.map` vectoriales Mapsforge -- motores incompatibles. Se agregó
Mapsforge como motor SEPARADO, sin tocar `MapScreen.kt` (OSMDroid en vivo,
con GPS/ruta/waypoints, frágil):
- Dependencias nuevas (`build.gradle`): `mapsforge-map-android`,
  `mapsforge-map-reader`, `mapsforge-themes` (0.25.0, Maven Central). Ojo:
  `MapsforgeThemes` vive en el artifact `mapsforge-themes`, no en
  `mapsforge-map`/`mapsforge-map-android` -- si falta, tira "Unresolved
  reference" aunque el import esté bien escrito.
- `ProvinceMapRepository.kt` (paquete `ar.motorfar.app.maps`): trae el
  manifest en vivo desde GitHub (raw.githubusercontent.com, no bundleado en
  el APK), descarga con `HttpURLConnection` + verifica SHA-256, borra el
  archivo si no matchea.
- `OfflineVectorMapsScreen.kt`: lista de provincias con descarga + progreso,
  visor Mapsforge de solo lectura (pan/zoom, sin GPS/ruta/marcadores --
  eso queda para otra sesión si se pide).
- Ruta `"offline_maps"` en el `NavHost` de `MainActivity.kt`, acceso desde
  Ajustes (`AliasSettingScreen.kt`, botón "MAPAS OFFLINE POR PROVINCIA →").

**Build verde, instalado en el Huawei P9 -- falta la prueba manual real**:
entrar a Ajustes → Mapas offline, descargar la provincia más chica (revisar
`size_bytes` en el manifest) y confirmar que el mapa renderiza con pan/zoom,
y que `MapScreen.kt` sigue andando igual que antes.

## 🌐 Decisión 2026-07-13 — dominio propio: `baqueanoapp.com`
`baqueano.com`/`.com.ar` están tomados por terceros (verificado por WHOIS/NIC.ar).
`baqueanoapp.com` está libre (verificado). Plan más económico aprobado por
LuKaZ: comprar SOLO el dominio (~US$10-20/año, Namecheap o Hostinger), seguir
usando **Netlify gratis** para el hosting del sitio (ya funciona, no hace
falta pagar hosting nuevo), y **Zoho Mail plan free** (hasta 5 casillas,
`vos@baqueanoapp.com`, $0/año, sin IMAP/POP en el free). Pendiente: LuKaZ
tiene que comprar el dominio y dar de alta Zoho Mail a mano (pagos/altas de
cuenta no los hace el asistente) — avisar cuando esté para apuntar el DNS de
Netlify + los MX de Zoho al dominio nuevo.

## ⚡ CIERRE 2026-07-13 (cierre final de la sesión) — 4to fix (SSID/clave) + límite de hardware confirmado
Cuarto bug de la misma familia: `WifiSettingScreen` gateaba "GUARDAR CLAVE/SSID"
con `isConnected` (exige handshake completo CON módulo de radio) — en este
ESP32 sin SA818 esos botones quedaban permanentemente deshabilitados, aunque
el firmware procesa `COMMAND_HOST_SET_WIFI_PASSWORD`/`SSID` sin depender del
módulo. Fix: nuevo callback `wifiLinkChanged(boolean)` (se dispara apenas el
Hello es válido, antes del chequeo de radio module status); `WifiSettingScreen`
ahora gatea con `isWifiLinkUp` en vez de `isConnected`. **Probado en vivo:
funciona.** Commit `51fff02`.

**Límite de hardware confirmado (no es bug, es diseño correcto):** con el
ESP32 sin SA818, tanto el aviso de enlace colgado (`staleConnectionCheck`,
solo se programa dentro de `radioConnected()`) como los comandos de radio
(`COMMAND_HOST_DESIRED_STATE` — canal, frecuencia, beacon, chat; gateados por
`transportReady` en `RadioModuleController`, que solo se activa en
`markRadioTransportReady()`) dependen de pasar el chequeo de módulo de radio
encontrado, que este hardware nunca pasa. **No hay forma de probar esos dos
caminos sin un SA818 físico conectado** — pendiente para cuando esté
disponible. Lo único operable sin el módulo es lo que ya se probó (WiFi
SSID/clave), porque no pasa por `radioModule`.

**Total de la sesión: 4 bugs reales + 1 confirmación de límite de hardware,
todo commiteado** (`a8e790a`, `93cb853`, `51fff02`).

## ⚡ CIERRE 2026-07-13 (continuación misma sesión) — 3 bugs más, todos ligados a DeviceState sin gatear cuando no hay radio
Después del handshake WiFi (ver cierre debajo), LuKaZ reportó bips de alerta que
no paraban en cualquier pantalla, con "SIN RADIO" en pantalla — y el PTT en
pantalla "se despresionaba solo" al mantenerlo apretado. Los 3 bugs, todos en
`RadioAudioService.handleDeviceState()` / callbacks relacionados, con la MISMA
causa raíz: **el firmware manda `COMMAND_DEVICE_STATE` a través de
`handleCommands()` sin ninguna relación con si el Hello validó el módulo de
radio** — sin SA818 físico conectado (hardware de esta sesión), esos paquetes
traen lecturas de GPIO flotantes/"fantasma", y el código reaccionaba a todos
sin filtrar por si el radio realmente terminó el handshake:

1. **`radioMissing()` no reseteaba `mode`:** si hubo un handshake exitoso antes
   en la sesión (`mode==RX`), cualquier caída posterior que pasara por
   `radioMissing()` (a diferencia de los caminos de `validateHello()` que sí
   hacen `setMode(BAD_FIRMWARE)`) dejaba `mode` pegado en `RX` para siempre.
   Fix: `radioMissing()` ahora también hace `setMode(BAD_FIRMWARE)`.
2. **`moduleTxStateChanged(false)` sonaba el roger beep en cada paquete, no
   solo en la transición real TX→no-TX** — sin radio real, el equipo siempre
   reporta `txActive=false`, así que sonaba sin parar. Fix en `MainActivity.kt`:
   solo suena si el estado previo (`uiState.value.isTxActive`) era `true`.
3. **El PTT físico (`didPhysPttChange()`) y el propio `moduleTxStateChanged()`
   corrían para CUALQUIER `DeviceState`**, no solo cuando el radio está
   operando. Esto también pisaba `isTxActive` en la UI en cada paquete —
   aunque el usuario tuviera el PTT en pantalla apretado, un DeviceState
   fantasma lo hacía "soltarse solo" visualmente. Fix: ambos ahora están
   detrás de un gate `radioOperating = mode ∈ {RX, TX, SCAN}` en
   `handleDeviceState()`.

**Verificado en vivo** (rebuild + reinstall + reabrir la app cada vez): bips
pararon, PTT se sostiene bien mientras se mantiene apretado. Los 5 fixes de
esta sesión (2 del cierre de WiFi + estos 3) siguen **sin commitear**.

## ⚡ CIERRE 2026-07-13 (nueva sesión) — WiFi resuelto: 2 bugs reales encontrados y arreglados, handshake HELLO confirmado en vivo
Continuación directa del cierre anterior (ver debajo, "WiFi da timeout, causa sin
confirmar"). La hipótesis de esa sesión (el ESP32 se resetea por watchdog sin el
SA818) **quedó descartada por evidencia real** — con el monitor serie corriendo
durante varios intentos de conexión, el ESP32 nunca se reinició ni una vez.

**Causa real del "contraseña incorrecta" / timeout inicial:** la memoria NVS del
equipo estaba vacía (`nvs_open failed: NOT_FOUND` en el boot), así que la clave
WPA2 `12345678` seteada en la sesión anterior no sobrevivió — el firmware generó
sola una clave nueva derivada de la MAC. Se leyó la MAC real (`b4:bf:e9:02:06:c8`)
con esptool en modo bootloader (BOOT sostenido a mano) y se calculó la clave
vigente: **`bqE9BFB4`** (case-sensitive). Con esa clave el celular conectó.
Aparte: la app **no tiene ningún campo para unirse a la red por primera vez** —
`WifiSettingScreen.kt` solo sirve para cambiar la clave/SSID una vez ya conectado;
la asociación WiFi real es siempre por Ajustes nativos de Android.

**2 bugs reales en el código, encontrados con logcat real (Huawei P9 por adb) y
arreglados — commits pendientes de crear:**
1. **`WifiTransport.java` (`onAvailable()`):** el ESP32 solo aprende la IP:puerto
   del cliente (y recién ahí manda su HELLO) del PRIMER datagrama UDP que
   recibe — ver comentario en `wifiTransport.h` del firmware. La app nunca
   mandaba nada antes de esperar el HELLO: los dos lados esperaban que el otro
   hablara primero, deadlock total. Nunca se había probado este camino con la
   app real antes de hoy (antes solo con un script Python `fw3a_smoke.py`).
   Fix: mandar un datagrama de 1 byte apenas se bindea el socket UDP.
2. **`RadioAudioService.java` (`scheduleHelloTimeout()`):** cuando el HELLO daba
   timeout (60s), el código nunca llamaba a `closePortAndReset()` — `hostToEsp32`/
   `wifiTransport` quedaban vivos, `isConnectionReady()` seguía dando `true` para
   siempre, y `ConnectionController` nunca volvía a reintentar. La app quedaba
   en "SIN RADIO" para siempre sin ningún reintento automático. Fix: limpiar el
   estado antes de marcar el intento terminado.

**Verificado en vivo:** con los dos fixes, se vio en logcat **85 segundos
seguidos de telemetría real del firmware llegando por WiFi** (`measureLoopFrequency`
a ~98Hz) — el handshake HELLO se completó de punta a punta por primera vez en
todo el proyecto. Después se cortó la conexión, pero fue porque LuKaZ apagó el
WiFi del celular a mano (no un bug nuevo) — y con el fix del bug 2, la app
reintentó sola en vez de quedarse colgada.

**Herramientas nuevas que valieron la pena:**
- `pio device monitor` (no el script casero roto) para ver el boot real del
  ESP32 sin ambigüedad.
- `adb` (el Huawei P9 ya estaba autorizado de sesiones anteriores) para sacar
  screenshots (`adb exec-out screencap -p`) y logcat filtrado por proceso
  (`adb logcat -d --pid=...`) del celu directamente — mucho más confiable que
  pedirle capturas de pantalla a LuKaZ, que quedaban atrapadas en el celular.

**Pendiente para la próxima:**
- ⬜ Commitear los 2 fixes (`WifiTransport.java`, `RadioAudioService.java`) —
  quedaron aplicados en el working tree, sin commitear todavía.
- ⬜ Retomar el objetivo original: probar el aviso de enlace colgado
  (`STALE_CONNECTION_TIMEOUT_MS`) y los comandos UDP del protocolo, ahora que
  el handshake sí funciona.
- ⬜ Confirmar si los 3 tonos personalizados (arranque/conexión/alarma) suenan
  bien — el de conexión ya sonó hoy ("bips adicionales" que reportó LuKaZ),
  falta confirmación explícita de que era el tono correcto.
- ⬜ El botón "SIN RADIO" en rojo es genérico: no distingue "sin WiFi" de "WiFi
  ok pero sin módulo SA818" — no es un bug, pero podría ser más claro. No
  tocado hoy, fuera de foco.

## ⚡ CIERRE 2026-07-13 (definitivo) — WiFi da timeout, causa sin confirmar; script de diagnóstico serie roto
Sesión larga, varios frentes. Resumen de lo que quedó firme y lo que no.

**✅ Firme, sin dudas:**
- Firmware actual reflasheado en el ESP32 y verificado por hash (esptool, las
  4 particiones OK). Maniobra correcta documentada abajo.
- SSID único confirmado en el aire: `Baqueano-BFB4` (visible por escaneo WiFi
  real desde PC y celular).
- Huawei P9 con la app recién compilada instalada — incluye los 3 tonos
  personalizados nuevos (arranque, conexión, alarma de reinicio; ver spec
  local `docs/superpowers/specs/2026-07-13-tonos-personalizados-design.md`)
  y el diseño de autenticación UDP (`docs/superpowers/specs/2026-07-13-autenticacion-protocolo-udp-design.md`,
  **diseño aprobado, sin implementar en código todavía**).
- Clave WPA2 del equipo cambiada a `12345678` (por comando USB, para
  testing en casa — NO es la clave que generó el firmware solo).

**❌ Sin resolver — la app no logra conectarse por WiFi al equipo:**
- El celular intenta asociarse a `Baqueano-BFB4` con la clave `12345678` y
  siempre termina en "Finalizó el tiempo de espera para la conexión... Error
  de conexión" (timeout genérico de Android, NO "contraseña incorrecta"
  específico).
- Hipótesis principal, sin confirmar: el ESP32 se resetea solo (crash o
  watchdog) en algún punto del arranque posterior a levantar el WiFi,
  probablemente por no tener el módulo SA818/DRA818 conectado — el propio
  firmware avisa "vas a ver resets del watchdog, es esperado" en el banner
  de boot. Si el reset ocurre a mitad del handshake WPA2/DHCP del celular,
  explicaría el timeout.
- **Esta hipótesis NO está confirmada con evidencia limpia** — ver bug de
  tooling abajo.

**🐛 Hallazgo importante sobre la herramienta de diagnóstico (para la próxima sesión):**
El script casero de captura serie (`read_esp32.ps1`, armado sobre la marcha
hoy en el scratchpad de la sesión, no vive en el repo) tiene un bug real: si
el puerto COM se cierra a mitad de captura (ej. el usuario desconecta el
USB), el `catch` solo atrapa `TimeoutException` — una desconexión real tira
otra excepción que cae fuera del catch y el loop reintenta sin parar,
generando cientos de miles de "bytes" que en realidad son el mismo mensaje
de error repetido, no datos reales del ESP32. Esto invalida las lecturas de
"cantidad de bytes"/"velocidad" de varias capturas de hoy — **no confiar en
esas cifras de esta sesión**. El texto capturado en sí (cuando el puerto
seguía abierto) parece real, pero la frecuencia de repetición no se pudo
medir de forma confiable.
- **Para la próxima sesión de firmware:** o se arregla ese script (manejar
  cualquier excepción de puerto cerrado, no solo timeout) o se usa el
  monitor serie de PlatformIO (`pio device monitor`), que ya maneja esto
  bien y es la herramienta estándar — más confiable que reinventar la rueda
  a las apuradas.

**Próximo paso sugerido:** con una herramienta de captura serie confiable,
confirmar o descartar el crash-loop real (buscar mensaje de pánico/Guru
Meditation, no solo contar líneas repetidas). Si se confirma, es 100%
esperable sin el módulo SA818 — no bloquea seguir diseñando/codeando en
otros frentes, pero sí bloquea cualquier prueba de protocolo real por WiFi
hasta conseguir el módulo o entender exactamente qué crashea sin él.

**Pendiente sin verificar (quedó interrumpido por el problema de WiFi):**
si los 3 tonos personalizados nuevos suenan bien en el Huawei — la sesión
se desvió a debuggear la conexión WiFi antes de poder confirmar el chime de
"conexión exitosa" en uso real (nunca llegó a conectar). El de arranque de
la app sí debería haberse escuchado al abrir la app instalada.

## ⚡ ACTUALIZACIÓN 2026-07-13 (más tarde) — reflash del ESP32 COMPLETADO
Con LuKaZ ya en casa y el ESP32 a mano, se terminó lo que había quedado a
mitad de camino más temprano hoy:
- **Causa real de los intentos fallidos de flasheo:** no bastaba con
  sostener BOOT antes de arrancar el comando — el gesto tiene que hacerse
  **mientras esptool ya está activamente intentando conectar** (sostener
  BOOT, tocar EN una vez sin soltar BOOT, soltar BOOT después). El wrapper
  de `pio run -t upload` además ocultaba el error real de esptool
  ("Wrong boot mode detected") — diagnosticado invocando `esptool.py`
  directo.
- **Flasheo exitoso** vía `esptool.py write_flash` directo (bootloader +
  partitions + boot_app0 + firmware, offsets estándar Arduino-ESP32), hash
  verificado en las 4 particiones, sin errores.
- **El "hard reset" automático de esptool al final (RTS→EN) NO fue
  confiable en esta placa** — el chip quedó sin arrancar el firmware nuevo
  hasta que LuKaZ desconectó/reconectó el USB a mano. Con eso, confirmado
  por LuKaZ mirando la lista de WiFi de su celular: **aparece `Baqueano-`**
  (SSID único nuevo), reemplazando al `MotoRFAR-HT` viejo — firmware
  actual corriendo de verdad.
- **Huawei P9 con la app recién compilada instalada** (incluye los 3 tonos
  personalizados nuevos: arranque, conexión, alarma de reinicio — ver
  `docs/superpowers/specs/2026-07-13-tonos-personalizados-design.md`).

**Para la próxima sesión con este hardware:** ya está todo listo para
retomar el objetivo original — probar handshake HELLO, el aviso de enlace
colgado, comandos UDP del protocolo — firmware actualizado, teléfono
autorizado y con la app instalada. Nota de maniobra para no repetir la
misma pérdida de tiempo: cualquier reflash futuro en este ESP32 necesita
BOOT+EN sincronizado con el comando ya corriendo, y power-cycle manual
después si el equipo no arranca solo.

## ⚡ CIERRE 2026-07-13 — verificación de hardware: firmware del ESP32 está VIEJO, reflash a mitad de camino
Sesión corta desde el trabajo (LuKaZ sin el módulo SA818, solo el ESP32 pelado
a mano). Verificado:
- **ESP32 en COM9 responde** — SoftAP arriba, UDP 4210 escuchando.
- **Hallazgo real: el firmware flasheado es de ANTES del 2026-07-06.** El
  SoftAP transmite con SSID `MotoRFAR-HT` (confirmado por escaneo WiFi real
  desde la PC, 93% señal) — el código actual ya genera `Baqueano-XXXX` único
  por equipo (`loadOrCreateWifiSsid()`, cambio del 2026-07-06/07). Ninguno de
  los fixes de las últimas sesiones (SSID único, cambio de clave/SSID desde
  la app, aviso de enlace colgado del 2026-07-10) está corriendo en este
  equipo todavía.
- **Huawei P9 (EVA_L09) autorizado por `adb`** — quedó listo para probar la
  app en próxima sesión, no hizo falta nada más de este lado.
- **Reflash intentado, quedó a mitad de camino:** `pio run -t upload` compiló
  limpio (Flash 67.9%, RAM 21.9%) pero el upload por esptool falló al
  conectar por COM9 — esta placa necesita sostener el botón **BOOT** a mano
  en el momento exacto de la conexión (el DTR no lo hace solo, ya anotado en
  el toolchain). LuKaZ no estaba físicamente presente para hacerlo (sesión
  desde el trabajo).

**Para la próxima vez que LuKaZ esté en casa con el ESP32 a mano:**
1. Sostener BOOT y correr de nuevo el comando de flash de `CLAUDE.md`
   (`pio run -e esp32dev -d '..\kv4p-ht-main\microcontroller-src' -t upload
   --upload-port COM9`).
2. Una vez flasheado, retomar el objetivo original del 2026-07-10: probar
   handshake HELLO, aviso de enlace colgado (`STALE_CONNECTION_TIMEOUT_MS`),
   comandos UDP del protocolo — con el Huawei ya autorizado, listo para
   sumarse a la prueba.

## ⚡ CIERRE 2026-07-10 — auditoría profunda + 10 fixes de confiabilidad de alertas
Arrancó con un pedido de auditoría de seguridad de rutina y terminó en un
hallazgo grande: **Man-Down/EMERGENCIA podía transmitir al vacío y la app
igual decía "enviada".** Trazado end-to-end (`transmitEmergencyAlert()`
fuerza el cambio a 140.970 para transmitir, pero no hay ningún modo de
escaneo alcanzable desde la UI — el grupo normalmente NO está parado en
ese canal, por diseño, ya que el chat libre está bloqueado ahí). Se
lanzaron 3 auditorías paralelas en background (app, firmware, y una nueva
de confiabilidad de alertas + fallas silenciosas) que confirmaron el
patrón: **en ningún punto de la cadena de alertas el emisor sabía si algo
realmente salió, y en cada punto la UI igual decía que sí.** Reportes
completos en `AUDITORIA_SEGURIDAD_APP.md` (actualización),
`AUDITORIA_SEGURIDAD_FIRMWARE.md` (actualización — **CRÍTICO-1 de la
whitelist TX confirmado FIJO**), `AUDITORIA_SEGURIDAD_MAPAS_OFFLINE.md`
(nuevo), `AUDITORIA_CONFIABILIDAD_ALERTAS.md` (nuevo),
`AUDITORIA_FALLAS_SILENCIOSAS.md` (nuevo), `AUDITORIA_BRECHAS_DE_PRODUCTO.md`
(nuevo, hallazgos de negocio/legal — cero validación real en hardware, sin
backup del keystore, sin plan de continuidad, marketing que promete más de
lo que el producto entrega para usuario solo).

**10 commits de fixes, paso a paso, build verde en cada uno:**
1. **Corta la confirmación falsa de "alerta enviada"** en Man-Down
   (`fireManDownAlert()`) y en los botones manuales (`transmitGroupAlert()`)
   — ahora chequean conexión real antes de decidir qué notificación mostrar,
   y usan `AlertHelper.getSentConfirmation()` (existía en el código, nunca
   se llamaba). Texto de "monitoreado por entidades" corregido (era falso).
2. **`onTaskRemoved()` ya no mata el Service** al deslizar la app de
   recientes — antes apagaba detección de caída, recepción, y envío juntos,
   sin aviso. Se agregó un botón real "DESCONECTAR RADIO" en la
   notificación (el único mecanismo previo, `setDeleteIntent`, era código
   muerto porque `setOngoing(true)` bloquea el swipe-to-dismiss).
3. **Pide excepción de optimización de batería** al activar Man-Down
   (contextual, no a ciegas al abrir la app).
4. **"CONECTADO" ahora avisa si el equipo lleva 15s sin responder** — antes
   solo confirmaba que el socket existía, no que el ESP32 seguía
   respondiendo. Implementado como AVISO, no bloqueo (sin hardware real no
   se puede calibrar un timeout que corte PTT/alertas sin riesgo de falso
   positivo).
5. **Pide `POST_NOTIFICATIONS` en runtime** al activar Man-Down (ya estaba
   en el manifest, pero eso no alcanza en Android 13+).
6. **Alerta entrante de EMERGENCIA: cartel de pantalla completa + sonido de
   alarma** — antes solo una notificación normal silenciable por No
   Molestar. DETENCIÓN/REAGRUPAMIENTO no llevan este tratamiento (correcto,
   no son emergencias reales).
7. **Copy de 140.970 corregido dos veces** — primero de "monitoreado por
   entidades" (falso) a "nadie la escucha" (también una afirmación fuerte
   sin verificar), después a algo honesto: el grupo la recibe solo si tiene
   el radio en ese canal en ese momento, sin garantía. Ninguna certeza
   afirmada en ninguna dirección.
8. **Protocolo de respuesta escrito en el cartel de EMERGENCIA** (pedido
   explícito de LuKaZ): "Parate a revisar. Llamalo por los 3 canales. Sin
   respuesta: pedí ayuda o avisá a emergencias/autoridades." Puesto en el
   momento exacto en que alguien lo necesita, no enterrado en Ajustes.
9. **PTT físico ahora confirma con sonido** — crítico para el caso que
   justifica que exista un botón físico: pantalla rota tras una caída,
   usuario operando a ciegas. `forcedPttStart()/forcedPttEnd()` eran
   callbacks default no-op nunca implementados — el botón transmitía sin
   ningún aviso.
10. **Tono distinto para PTT aceptado vs. rechazado**, físico y en
    pantalla — mismo criterio que un HT real (pedido explícito de LuKaZ).
    Antes sonaba el mismo tono para "transmitiendo" y para "rechazado".

**Lo que queda abierto, a propósito, sin codear a ciegas:**
- El problema de fondo (nadie garantiza estar en 140.970) sigue sin
  resolver — es rediseño de protocolo/UX (scan por defecto vs. transmitir
  en el canal actual), necesita su propia sesión de diseño. Ver detalle en
  `PENDIENTES.md`.
- Nada de lo de hoy se probó en dispositivo físico — mismo pendiente de
  siempre. El timeout de 15s del aviso de enlace colgado es un punto de
  partida sin calibrar.
- ALTO-1/ALTO-3 de seguridad (protocolo UDP del firmware sin
  autenticación) sigue igual — ya anotado como su propia sesión de diseño
  desde el 2026-07-09.
- Idea sin construir: video para la web explicando el protocolo de
  respuesta a EMERGENCIA, una vez que la funcionalidad de la app esté
  resuelta y estable (orden explícito de LuKaZ, no adelantar).

## ⚡ CIERRE 2026-07-09 (segunda parte) — pipeline de mapas offline EJECUTADO, release publicado
- **Mapas offline por provincia — pipeline corrido de punta a punta** (ya no es
  solo diseño, contradice la entrada de más abajo que decía "sin código
  todavía" — esa quedó vieja en cuestión de horas). 24/24 provincias (23 +
  CABA) generadas como `.map` Mapsforge y publicadas en GitHub Release
  `mapas-v1` de `lukaz2004/MotoRFAR` (**público, no draft** — assets
  descargables sin auth). Manifest provincia→URL→tamaño en
  `_PROYECTO/mapas_offline/provincias.json` (commit `47ecfc8`). Total 24
  archivos, ~366MB.
- **Herramientas:** Osmosis (portable, requiere Java 17+, instalado Temurin 21
  aparte en `C:\Users\lukaz\jdk21` — el `JAVA_HOME` que usa Gradle para la app
  Android NO se tocó) + plugin `mapsforge-map-writer`. Límites de provincia
  vía Overpass (`admin_level=4`) — `polygons.openstreetmap.org` (la fuente
  típica de `.poly`) está caída, se armó un conversor propio desde geometría
  cruda de Overpass. Buenos Aires (570 anillos, delta del Paraná) hacía que el
  filtro de Osmosis no convergiera — se agregó simplificación Douglas-Peucker.
  Todo el detalle de bloqueos/soluciones en el historial de la sesión (no
  repetido acá).
- ✅ **Aclarado con LuKaZ:** el release `mapas-v1` preexistente (`createdAt`
  dos días antes de esta sesión, publicado horas antes de lanzar el pipeline)
  viene de otra sesión de chat previa que corrió este mismo pipeline —
  confirmado, no es contenido de origen desconocido. El pipeline de esta
  sesión generó su propio set de 24 `.map` en paralelo (tamaños ~5-10%
  distintos pero plausibles, mismo relation id de OSM), lo detectó a mitad de
  camino, borró su duplicado y el manifest quedó apuntando al release ya
  existente.
- **Sub-proyecto 2 (ventana in-app):** ahora sí tiene URLs reales para
  consumir (`_PROYECTO/mapas_offline/provincias.json`). Arrancable cuando se
  retome.
- Carpeta de trabajo `C:\Users\lukaz\mapas-baqueano-build\` (fuera de
  OneDrive, ~1.3GB: extracto de Argentina + extractos por provincia + los
  `.map` generados) quedó sin borrar por si hace falta reprocesar algo.

## ⚡ CIERRE 2026-07-09 (primera parte) — mapas offline (diseño pipeline) + testing real Huawei P9 commiteado
- **Mapas offline por provincia:** diseño del pipeline de generación aprobado
  (`docs/superpowers/specs/2026-07-08-mapas-offline-por-provincia-design.md`
  — GitHub Releases, Mapsforge `.map` por provincia vía osmium-tool + Osmosis,
  sin costo de hosteo). **Sin código todavía** — requiere instalar herramientas
  (no están en esta máquina) y correr el pipeline como sesión larga/desatendida
  aparte. Sub-proyecto 2 (ventana in-app con links) espera las URLs reales que
  produzca ese pipeline.
- **Working tree de varias sesiones (probado en Huawei P9 real, nunca
  commiteado) — ahora en 3 commits:**
  1. Fix crash real: "Descargar mapa de Argentina" tiraba `TileSourcePolicyException`
     sin capturar (Mapnik bloquea descarga masiva, `FLAG_NO_BULK`) — ahora avisa
     con Toast. + PTT del Mapa unificado visualmente con el de la pantalla
     principal (`PttVisual` compartido).
  2. **minSdk bajado de 26 a 24** para soportar Android 7 (probado en Huawei P9
     real) — encontró varios crashes reales que el emulador nunca mostró
     (`NoClassDefFoundError`/`AbstractMethodError` en APIs 26+/30+, guardados
     detrás de `SDK_INT` checks). De paso: Man-Down usaba
     `TYPE_ACCELEROMETER` (incluye gravedad, el umbral de quietud nunca se
     cumplía con el teléfono quieto) → `TYPE_LINEAR_ACCELERATION`; umbral de
     impacto 25→50 (disparaba con vibración de motor); WakeLock propio para
     la cuenta regresiva; GPS a veces devolvía (0,0) cacheado sin fix real;
     `isConnected` se marcaba true antes de terminar el handshake real
     (PTT se rechazaba en silencio en esa ventana); race entre reseed de
     canales y carga inicial (`MainViewModel.loadData()`) que podía cachear
     la tabla vacía para siempre.
  3. UI adaptada a pantalla chica real (360×640dp): PTT y DETENCIÓN/REAGRUPAR
     quedaban tapados por la barra de navegación, texto de canales/alertas
     cortado en horizontal. Cartel "Sin conexión con el equipo" movido de la
     pantalla principal a Ajustes > WiFi.
- ⬜ **Nada de esto se probó en moto real** — todo lo de arriba fue verificado
  en el Huawei P9 físico pero en mano, no andando. Sigue pendiente lo de
  siempre (Man-Down en uso real, umbrales de calibración).
- ⬜ Ver si el Huawei P9 fue el único dispositivo de prueba — si hay más
  equipos viejos dando vueltas, valdría la pena repetir el smoke test ahí.

## ⚡ CIERRE 2026-07-06/07 — background fix Man-Down, restricción Emergencia, seguridad, SSID único, retoques UI, animaciones de emergencia
Sesión larga con muchas rondas de feedback en vivo probando el APK. Resumen
por tema (detalle histórico completo estaba en `PENDIENTES.md`, recortado
de ahí tras este cierre):

- **Man-Down dejaba de funcionar en segundo plano — bug real corregido:**
  `FallDetectionManager` y el callback de alertas vivían atados al ciclo de
  vida de `MainActivity`, no al `RadioAudioService` (foreground service).
  Con la app minimizada, `onStop()` apagaba el acelerómetro — exactamente el
  escenario real de uso (teléfono guardado, pantalla apagada). Movido todo
  al Service: countdown, disparo de alerta, notificación de alta prioridad
  con "ESTOY BIEN · CANCELAR", alertas de otros integrantes ahora generan
  notificación propia (antes se perdían en silencio si la app no estaba
  bindeada). Sumado: pantalla propia vía full-screen intent (bypassa
  pantalla bloqueada), atajo de volumen físico (3x VOLUMEN- cancela, pensado
  para pantalla rota tras una caída), countdown escalado según fuerza G del
  golpe, copy en modo potencial ("posible caída", no "caída detectada").
  Verificado en emulador con inyección de sensor real (impacto + quietud).
  ⬜ **Falta:** probar en dispositivo físico — botón "ESTOY BIEN" tocado a
  mano, alerta de otro integrante con la app cerrada (no simulable en
  emulador sin un segundo radio/peer). `onTaskRemoved()` sigue matando el
  Service si el usuario desliza la app fuera de "recientes" — decisión de
  producto pendiente (¿debería sobrevivir a eso, tipo Life360?).

- **Canal Emergencia restringido a uso real:** chat libre, STOP/Reagrupamiento
  y el balizado de rutina ahora se bloquean si el canal activo es 140.970
  (antes nada lo impedía). Tonos CTCSS por defecto para Principal/Alternativo
  (Emergencia sin tono a propósito). Pantalla de selección de tonos nueva
  (`TonesSettingScreen.kt`) con explicación en criollo de cómo funciona el
  tono y sus límites reales.

- **Auditoría de seguridad aplicada:** whitelist TX también en `sa818.group()`
  (defensa en profundidad), clave WPA2 única por equipo (ya no hardcodeada),
  deadman de PTT desacoplado del tráfico UDP genérico, validación de origen
  UDP, fix parsing APRS, copy de privacidad Man-Down, headers de seguridad
  en la web. **SSID único por equipo** (`Baqueano-XXXX`, derivado del efuse
  MAC) para que dos equipos cercanos no se confundan de red — motivado por
  pregunta real de seguridad del usuario (dos motos a 20m con el mismo SSID
  fijo). Configurable desde la app (Ajustes > WiFi) junto con la clave.

- **Retoques de UI pedidos en vivo probando el APK real** (varias rondas de
  feedback, cada una encontró algo real):
  - Descarga de mapas offline unificada (había dos entradas, una placeholder).
  - Canal "GRUPO" renombrado a "PRINCIPAL" (juega con "ALTERNATIVO").
  - Mapa: sacados los botones +/- de zoom (redundantes, el pellizco ya
    hacía zoom); RUMBO/MI GPS movidos junto al HUD de coordenadas arriba
    (antes competían con el PTT, riesgo de toque accidental manejando);
    WAYPOINT movido ahí también (estaba fuera de lugar en pantalla principal).
  - "Borrar ruta guardada" movido de la barra superior a Ajustes.
  - Emergencia activa ahora se nota de verdad: botón de canal se rellena de
    rojo sólido, cartel de frecuencia se pone rojo, botón PTT y sus anillos
    se ponen rojos, ecualizador se pone rojo, botón de confirmación
    ("⚠ EMERGENCIA") parpadea — todo sincronizado a un solo estado derivado
    (`MainUiState.isEmergencyActive`). Esto reveló un bug real: el flujo
    automático de alerta (mantener 2s) solo retunaba el hardware, nunca
    avisaba a la UI — corregido en `transmitGroupAlert()`.
  - Botón PTT: "PTT"/"TX" → "PUSH TO TALK"/"TRANSMITIENDO", con borde blanco
    detrás del texto (se perdía contra el degradé de fondo). Mismo
    tratamiento en el PTT compacto del Mapa.
  - Ícono de Baqueano agregado a la barra superior (mipmap real, no el XML
    de ícono adaptativo — eso crasheaba `painterResource`).
  - Tono CTCSS visible en pantalla principal junto a "MHz · FM · SIMPLEX".
  - **Bug real encontrado en producción (probado en celular):** el reseed de
    canales (`preloadArgentinaChannelsIfNeeded`) hacía cada `delete()` y el
    `insertAll()` como operaciones sueltas sin transacción — la LiveData de
    `getAll()` se invalidaba después de cada una, y la pantalla principal
    podía agarrar la tabla vacía a mitad del reseed (nombre de canal caía a
    "SIMPLEX", tono sin canal para mostrar). Corregido con
    `db.runInTransaction(...)`.
  - Build verificado (`gradlew assembleDebug`/`assembleRelease`) y
    confirmado visualmente en emulador en cada ronda.

- ⬜ **Hallazgo nuevo, no resuelto:** `MainViewModel.channelMemories` es un
  snapshot cargado una sola vez (`loadData()`), no una LiveData reactiva de
  Room — queda desincronizado del canal real que usa `ChannelRow` (que sí
  es reactivo). Esto hace que `tuneToChannel()` (al tocar un canal a mano)
  a veces muestre "SIMPLEX" en vez del nombre real. No afecta el flujo
  automático de Emergencia (usa un string literal, no este lookup), pero
  conviene unificarlo a la fuente reactiva en una próxima sesión.
- ⬜ **Animaciones de emergencia sin probar con radio real conectada:** el
  flujo automático (mantener 2s el botón "⚠ EMERGENCIA") requiere
  `uiState.isConnected == true`, que el emulador no tiene sin hardware real
  — se verificó el mecanismo (mismo estado derivado) tocando manualmente el
  canal Emergencia, que sí disparó todo correctamente, pero falta confirmar
  el flujo automático de punta a punta en dispositivo físico con equipo.
- ⬜ Renombrar `ic_launcher_moto.png` → el archivo dice "moto" pero la imagen
  ya es el escudo Baqueano (rebrand viejo, solo el nombre del archivo quedó
  desactualizado). Cosmético, no bloquea nada.
- ⬜ No existe pantalla de historial de rutas — solo se guarda/muestra la
  última sesión por alias; "Borrar ruta guardada" borra todo, no por viaje.
  Sería feature nueva si se pide.

## ⚡ CIERRE 2026-07-05 (tercera parte) — diseño navegación + limpieza de pendientes + keystore + push
- **Navegación turn-by-turn propia — diseño aprobado, sin código todavía:**
  spec completo en `_PROYECTO/NAV_TURN_BY_TURN_DISENO.md`. Motor elegido:
  **BRouter** (Java puro, sin NDK, GPL-3.0 — mismo esquema de licencia que ya
  usa Baqueano; datos de ruteo pre-procesados por el proyecto, no hace falta
  pipeline OSM propio). Tamaño real verificado: ~400-600MB para cubrir toda
  Argentina (contra tamaños reales de tiles `.rd5`, extracto Geofabrik y
  paquete OsmAnd). Estrategia de descarga en 2 etapas para no bloquear nunca
  la función crítica (radio/grupo): tile local obligatorio al primer inicio +
  resto del país en background. Voz (TTS) + flecha visual, con **ducking** de
  audio sobre el radio VHF en vez de cortarlo. **El propio spec deja 4 puntos
  para pulir antes de armar el plan de implementación** (ver sección "Abierto
  para pulir" del archivo) — no dar por definitivo sin repasarlos.
- **Correcciones de estado real, quedaba desactualizado:**
  - EST-1 carcasa: estaba anotada con tareas pendientes (modelado, corte,
    impresión) que LuKaZ ya había resuelto fuera de las sesiones de chat —
    marcada CERRADA.
  - Web Netlify: estaba anotada con "reclamar cuenta + renombrar" pendiente;
    verificado por fetch real que `baqueano.netlify.app` ya está online con la
    cuenta reclamada — corregido en `PENDIENTES.md` y `NEXT_SESSION.md`.
- **Copy de la web corregido:** las 3 menciones de "30 segundos" fijo en
  Man-Down (ya no es así desde el countdown variable por fuerza G) → "cuenta
  regresiva" sin número fijo. Copy de CTCSS ("Sin interferencias, sin
  confusión...") sobrevendía → corregido a lo que realmente hace (filtra lo
  que escuchás, no separa el RF real).
- **CTCSS/DCS por canal — verificado, no hacía falta tocar código:**
  `reconcileDesiredState()` ya pasa `ctcss_tx`/`ctcss_rx` a `sa818.group(...)`
  (línea 312 de `kv4p_ht_esp32_wroom_32.ino`). Quedaba anotado como "no
  verificado" desde hace rato — ya funciona.
- **Keystore de release generado:** `KV4PHT/baqueano-release.jks` (RSA 2048,
  alias `baqueano`, válido hasta 2053) + `KV4PHT/keystore.properties` con
  contraseña fuerte generada al azar (gitignorado). Encontrado y reemplazado
  un `keystore.properties` viejo a medio armar (contraseña débil, ruta rota,
  apuntaba a un `.jks` que nunca existió). Primer intento del certificado puso
  `OU=MotoRFAR` por error de Claude — corregido y regenerado con todo en
  `Baqueano` antes de que se usara para firmar nada. Guía de backup (Bitwarden
  como Nota Segura, no Login) dada a LuKaZ — **el backup físico del `.jks` en
  un segundo lugar (fuera de esta PC) queda como acción suya, no está hecho
  todavía.**
- **Push a GitHub:** los ~30 commits locales de la sesión larga anterior +
  los de hoy quedaron subidos a `origin/main` (`ce76180..eee7010`). Ya no hay
  nada pendiente de pushear.
- **Sesión termina acá.** LuKaZ puede retomar mañana a la mañana si tiene
  tiempo — punto de partida sugerido: repasar los 4 puntos de "Abierto para
  pulir" del spec de navegación y, si cierran, pasar a armar el plan de
  implementación (`writing-plans`).

## ⚡ CIERRE 2026-07-05 (segunda parte) — web comercial + registro de ruta
- **Web reconstruida de punta a punta** sobre `docs/index.html` (la que ya estaba
  en vivo, más madura de lo que parecía — ver auditoría en
  `AUDITORIA_MARKETING_WEB.md`): íconos SVG en vez de emoji, animaciones de
  hover/mouse, sección Comparativa nueva (vs. handy VHF e intercom Bluetooth,
  con gancho creativo vs. armar un grupo de Discord/Zello por LTE), "Cómo
  funciona" movido arriba del todo (justo después del hero), copy menos técnico
  (sin nombrar módulo/chip en el flujo principal), alimentación corregida a
  USB-C en todos lados (ya no dice 12V, esa idea se había descartado), PTT
  aclarado como término (Push To Talk) y corregido para no asumir manillar en
  pasos genéricos, ejemplo de "200 motos" en CTCSS corregido (sobrevendía).
  `_PROYECTO/web/index.html` y `docs/index.html` quedan sincronizados — ver
  `_PROYECTO/web/README.md` para el flujo (se edita ahí, se copia a docs/).
- **Registro de ruta arreglado en la app** (no solo copy): antes mezclaba todo
  el historial en una sola línea infinita sin forma de borrarla. Ahora agrupa
  por sesión (`RoutePoint.sessionId`, migración Room 7→8) y hay botón de borrar
  con confirmación. Build verificado (`assembleDebug` OK).
- **Hallazgo legal real, no cosmético:** homologación ENACOM del equipo es un
  trámite obligatorio y separado de que el canal sea libre para el usuario —
  vender sin homologar es infracción sancionable (Res. 729/80, Ley 24.240).
  El copy "Legal por diseño" sobrevendía esto, corregido. Homologación queda
  como bloqueante real antes de vender de verdad (no urgente mientras solo haya
  lista de espera) — anotado en `PENDIENTES.md`.
- **Pendientes anotados, sin construir:** Man-Down con countdown variable según
  fuerza G del golpe; marcar y compartir POIs propios (reusaría el botón
  WAYPOINT existente); navegación turn-by-turn propia (feature grande, no se
  puede inyectar en la navegación de Google Maps desde afuera); exportar ruta a
  GPX; adaptar frecuencias a otros países (técnicamente trivial en el firmware,
  el problema es la investigación legal país por país, no el código).
- **Sin pushear a GitHub** — todos los commits de web quedaron locales en
  `main`, a la espera de que LuKaZ revise antes de subir.
- **Sesión larga, costo alto** (~$120) por la cantidad de idas y vueltas de
  diseño/copy en vivo. Para la próxima: si se retoma la web, arrancar leyendo
  este cierre + `AUDITORIA_MARKETING_WEB.md` antes de tocar nada.

## ⚡ CIERRE 2026-07-05 (primera parte) — sesión de orden (nada de código nuevo, solo prolijidad)
- **Los 3 frentes sueltos del cierre anterior, commiteados:** rediseño web (`c942c00`),
  rebrand de íconos (`1461aaa`), limpieza Compose + rename MotoRFAR→Baqueano (`1905c96`).
  Working tree limpio salvo `imegenes baqueano edit/` (16MB, material fuente de video, no
  se toca).
- **Rescatado un cierre real de HW-1 del 2026-07-04 que nunca quedó anotado** (pasó el
  mismo día que Man-Down y se perdió en el cierre de esa sesión): stub RF J1 borrado,
  C9→R2 conectado (bug heredado real), J2 corregido en esquemático, DRC final en 90
  violaciones heredadas. Commiteado en `2d1555f` junto con `05_VISION.md` (aprobado el
  02/07, tampoco se había commiteado) y el schema Room v7. Detalle completo en
  `HW1_CIERRE.md`. Único pendiente real: C15/C32 (pin2 a GND), requiere sesión en vivo.
- **Limpieza `_PROYECTO/`:** borrados 15 archivos de iteración de DRC/ERC del 07-04
  (`drc_verify_2026-07-04*.json` a-j, `drc_final_2026-07-02.json`, `erc_after_j2.json`,
  `erc_verify_now.rpt`, `netlist_check.net`) — todos superados por los reportes finales
  ya commiteados arriba. También `tools_cierre_hw1.py` (script de un solo uso, ya
  ejecutado) y `ESTADO_SESION.txt` (resumen viejo del 28/06, redundante con este archivo).
- **Man-Down — sigue igual que el 04/07:** falta prueba en dispositivo físico y calibrar
  umbrales de `FallDetectionManager.kt`. Bloqueado en hardware, no hay nada más para
  hacer sin la moto real — no reabrir sin datos nuevos.

## ⚡ CIERRE 2026-07-04 — estado más reciente (si contradice algo de abajo, MANDA ESTO)
- **Sesión anterior colgada:** un `layout-land/activity_main.xml` legacy (huérfano de antes de la
  migración a Compose, nadie lo referenciaba) rompía el merge de recursos de AAPT2 con un error
  críptico ("layout file should've changed") y probablemente eso colgó el chat previo. Borrado junto
  con `MainActivityLegacy.java` y `layout/activity_main.xml` (mismo legacy, también muerto).
  Commit `aad9a5e`.
- **Man-Down — countdown completo y probado (commit `9858d8c`):** el toggle vivía en el
  `SettingsActivity` legacy, que ya no es alcanzable desde la navegación Compose actual → la
  función estaba wireada (countdown, overlay, tono, DB) pero **inaccesible para el usuario**.
  Agregado el toggle en `AliasSettingScreen.kt` (opt-in, default OFF). De paso, dos bugs reales
  encontrados probando en emulador:
  - Fuga de audio focus al cancelar ("ESTOY BIEN"): liberaba con una API vieja que no hacía nada;
    el foco exclusivo (silencia música/GPS) quedaba trabado para siempre.
  - **Crash real:** `WifiTransport` mandaba el UDP en el mismo thread que lo llamaba →
    `NetworkOnMainThreadException` justo cuando el countdown llega a 0 y dispara
    `transmitGroupAlert(EMERGENCY)` — la app se caía exactamente al mandar la alerta de
    emergencia. Ahora corre en un executor propio (aplica también a baliza y chat).
  - Probado end-to-end en emulador (Pixel_10_Pro_XL, caída simulada por acelerómetro): toggle,
    countdown, cancelar, y countdown completo → alerta se manda sin crashear.
  - ⬜ Falta: probar en dispositivo físico (el emulador valida la lógica, no el umbral de impacto
    real en moto). Umbrales de `FallDetectionManager` (30s countdown, impacto 25 m/s², quietud
    1.5) son valores de arranque sin calibrar con uso real.
- **⚠️ Working tree con 3 frentes sueltos sin commitear** (no tocados esta sesión, dejados
  adrede — cada uno es su propia etapa):
  1. Rediseño visual completo de `_PROYECTO/web/index.html` (tipografía Rajdhani, paleta
     verde retocada, título ya dice Baqueano).
  2. Rebrand de íconos: los 5 mipmaps (`ic_launcher*`) recomprimidos/actualizados.
  3. Limpieza menor Compose: `MainViewModel.java` (executor pool), `MapScreen.kt` (Polyline de
     ruta), `activity_terms.xml` (texto MotoRFAR→Baqueano).
  Correr `git status` antes de asumir árbol limpio en el próximo chat.
- **Emulador:** quedó corriendo y se cerró solo (o se lo mató) durante la sesión — no hay AVD
  activo al cierre.

## ⚡ CIERRE 2026-07-02
- **HW-1:** DRC completo corrido (CLI + GUI de LuKaZ, misma foto). **Ruteo PTT de LuKaZ: LIMPIO, cero errores.**
  Quedan 6 retoques de GUI (~25 min) con coordenadas exactas en `HW1_CIERRE.md` → hacerlos, re-correr
  DRC, y HW-1 pasa a CERRADO definitivo con evidencia. Las 169 marcas heredadas están clasificadas
  como no bloqueantes en ese archivo: NO se trabajan, NO se re-auditan.
- **05_VISION.md:** creado y aprobado por LuKaZ. Se lee en cada arranque junto a este archivo.
- **Esquemático:** BOM UTF-8 (3 bytes inválidos) eliminado; backup `kv4p-ht.kicad_sch.bak-BOM-2026-07-02`.
- **Conectores MCP:** Spotify / Shopify / Vercel / GDrive DESCONECTADOS (2026-07-02). Ignorar menciones viejas.
- **Python del sistema: FUNCIONA** (3.14.6 + pip 26.1.2, verificado 2026-07-02). Regla "Python roto" obsoleta.
  PlatformIO sigue en pio-venv (el build del firmware no se migra).
- **PRÓXIMA ETAPA — REBRAND total → Baqueano (chat propio, 1 chat = 1 etapa):** renombrar carpetas,
  app, SSID (MotoRFAR-HT → Baqueano-HT), web, docs. MotoRFAR muere como nombre: el producto ya no es
  solo para motos. ⚠️ Límite GPL: los headers de copyright kv4p/Vance Vagell dentro de los archivos
  fuente y el texto de la licencia NO se tocan (atribución obligatoria); todo lo demás se renombra.
- **Pendiente de organización:** crear `CERRADO.md` / `HISTORIAL.md` / `IDEAS.md` + adelgazar este
  archivo (mover historia vieja a HISTORIAL). Primera tarea del próximo chat de organización.
- **Alerta web:** SIGUE VIGENTE (ver abajo), sin cambios hoy.

---

## CONTEXTO (no leer más archivos salvo que la tarea lo pida)

**Proyecto:** MotoRFAR HT (fork kv4p, GPL-3.0) — radio VHF/UHF que convierte un Android en transceptor ham, montado en moto/4x4. Nombre comercial: **Baqueano**.

**Rutas clave:**
- Proyecto: `C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\_PROYECTO\`
- Firmware: `C:\Users\lukaz\OneDrive\Escritorio\kv4p-ht-main\kv4p-ht-main\microcontroller-src\`
- App Android: `C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\KV4PHT\`
- PCB: `...\kv4p-ht-main\pcb\v2.0e\kv4p-ht\kv4p-ht.kicad_pcb`
- Web: `C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\docs\` → publicada en Netlify

**Frecuencias TX permitidas (Res 5/2015 MTTT):** 139.970 · 138.510 · 140.970 MHz — whitelist en firmware ES LA AUTORIDAD FINAL.

---

## ESTADO AL 2026-07-02

### 🔴 ALERTA — WEB en estado divergente, sesión de auditoría no persistida a disco
- `docs\index.html` (deployado en Netlify) sin cambios desde 29/06 01:18 — 71K, 1238 líneas.
- `_PROYECTO\web\index.html` (fuente/staging) sin cambios desde 30/06 01:02 — 32K, 832 líneas.
- Chat "Subir fotos al proyecto web" (activo hasta 01/07 tarde-noche) hizo fetch a baqueano.netlify.app, auditoría completa de diseño/contenido, y reconstruyó la página — pero trabajó en el filesystem efímero del contenedor (`/home/claude/baqueano-web-assets/`) y nunca copió el resultado final a estos paths vía Desktop Commander. Ese trabajo puede estar accesible solo reabriendo ese chat puntual.
- **Antes de tocar la web de nuevo:** buscar ese chat con `conversation_search` ("auditoria diseño landing baqueano") y confirmar si el HTML final es recuperable. Si no, rehacer la auditoría — esta vez cerrando con copia a disco.
- Hasta resolver esto, los dos `index.html` siguen siendo versiones distintas — no asumir cuál es la vigente.

### 🟡 Conectores MCP — estado real (verificado 2026-07-02)
- **Vercel**: registrado pero no autorizado en esta sesión (`No approval received` al listar teams). Si se quiere usar para deploy/debug de la web, hay que reautorizarlo desde la configuración de Claude primero.
- **Google Drive**: mismo estado — registrado, no autorizado.
- **Shopify**: conectado y con herramientas activas, sin uso todavía en este proyecto. Relevante recién en la etapa de venta comercial.
- **Spotify**: conectado, sin relevancia para este proyecto.

---

## ESTADO AL 2026-06-30

### Cambios de la sesión 2026-06-30
- ✅ **App ícono**: Todos los mipmap (mdpi→xxxhdpi) reemplazados con badge Baqueano — `ic_launcher.png`, `ic_launcher_moto.png`, `ic_launcher_round.png` en las 5 densidades. Splash screen automático en Android 12+.
- ✅ **Web galería real**: 6 fotos reales en `_PROYECTO/web/img/` (moto-01 a moto-06, ~50-200KB c/u). Video demo con logo Baqueano en `_PROYECTO/web/baqueano-demo.mp4` (1.6MB). HTML actualizado con grilla 3×2 real, hover/zoom y captions.
- ✅ **Videos procesados**: Watermark Gemini tapado (blur quirúrgico + logo Baqueano overlay) en los 2 videos originales (`imegenes baqueano edit/mp4.mp4` y `mp4 (1).mp4`).
- ✅ **Diseño carcasa EST-1**: Especificaciones completas definidas:
  - Construcción sandwich: 2 tapas aluminio 3mm + frame 18mm corte por agua
  - Dimensiones externas: **170 × 75 × 24mm**
  - 8 tornillos M3 en la tapa (3 por lado largo, 1 por lado corto)
  - Frente: botón M16 Short Body High Head con LED azul (esquina superior izquierda, 10mm de margen), USB-C centrado, tapa PETG 60×20×4mm antena WiFi (parte inferior)
  - Borde superior: SMA VHF (15mm del borde derecho, 15mm del borde frontal) con pigtail RG316 → antena varilla inox 50cm
  - Sin conector 5V separado (alimenta por USB-C)
  - Terminaciones: aluminio arenado crudo ó anodizado negro sobre arenado
- ✅ **Prompt render prototipo**: Desarrollado prompt iterativo para Gemini Image — estrategia por capas (caja+tornillos+USB-C primero, luego botón, luego antenas)

---

## ESTADO AL 2026-06-28

### Cambios de la sesión 2026-06-28
- ✅ **Web:** baqueano.netlify.app EN VIVO — hero animado, capturas reales de la app, logo
- ✅ **App (Gemini fixes, commit 6082bfc):** 3 bugs corregidos:
  - `RadioAudioService.java` línea 209: carácter `b` suelto → eliminado
  - `RadioAudioService.java`: `MainActivityLegacy` (borrada) → `MainActivity`
  - `FallDetectionManager.kt`: `SENSOR_DELAY_NORMAL` → `SENSOR_DELAY_GAME` (20ms)
  - TxWhitelist verificado ✅
- ✅ **App (commit dd06575):** Toggle Man-Down en Ajustes — desactivado por defecto (evita falsas alarmas en baches)
- ⬜ **Email profesional:** info@baqueano.com → ver `_PROYECTO/GUIA_EMAIL_CHATBOT.md`
- ⬜ **WhatsApp chatbot:** ManyChat → ver `_PROYECTO/GUIA_EMAIL_CHATBOT.md`

---

## ESTADO AL 2026-06-27

| Etapa | Estado | Bloqueante |
|-------|--------|------------|
| F0 WiFi | ✅ SoftAP `MotoRFAR-HT`, UDP 4210, IP 192.168.4.1 | — |
| FW-1 línea base | ✅ compila + flashea | — |
| FW-2 whitelist TX | 🟡 código+flash OK | test RF real con SA818 |
| FW-3a WiFi transport | ✅ flasheado + Hello OK | `--open-rx` + failsafe (SA818) |
| FW-3b TCP/UDP split | ⬜ roadmap | tras FW-3a validado completo |
| FW-4 PTT J2 | 🟡 auditado | test físico con SA818 |
| HW-1 PCB | ✅ CERRADO (2026-06-29) | — |
| HW-2/3/4 | ⬜ | tras HW-1 |
| APP-1 | ✅ CERRADO | — |
| APP-2 WiFi client | 🟡 transport + UI guide hechos (PR #8+#9) | test real con SA818+ESP32 |
| APP-3 disclaimer | ⬜ | tras APP-2 |
| EST-1 carcasa | ⬜ arrancable | — |
| WEB | ✅ publicada Netlify | reclamar cuenta + renombrar a baqueano.netlify.app |
| INT | ⬜ | todo lo anterior |

---

## PENDIENTES CONCRETOS (sin hardware)

**HW-1 — 4 tareas GUI KiCad antes de ordenar JLCPCB:**
1. Rutear 3 pistas cortas PTT Right: R14-pad1 → C33-pad1 → U5-pad29 → J3-pad1
2. Verificar traza RF J1 (U.FL Hirose — pad central más chico que SMA anterior)
3. Verificar posición LOGO1 (y=140) y J3 (x=163.5, ~1.6mm borde derecho)
4. DRC completo (diálogo se abre off-screen: Win+Shift+Flecha)
5. Actualizar esquemático: agregar J3 + cambiar U5 a WROOM-32U-N4
6. Regenerar Gerbers + BOM + ZIP

**APP — pendientes sin hardware:**
- ✅ Fix ProGuard/R8 → APK release firmado (2026-06-24)
- ✅ Pantalla "Acerca de / Licencias" (2026-06-24)
- ✅ WifiTransport UDP + Protocol.FrameWriter (PR #8, 2026-06-27)
- ✅ WifiConnectBanner — guía al usuario a la red MotoRFAR-HT (PR #9, 2026-06-27)
- ⬜ Verificar Hello/handshake + audio RX/TX por WiFi (requiere SA818 físico)

**WEB:**
- Reclamar cuenta Netlify (si expiró: `netlify deploy --dir=docs --prod --allow-anonymous`)
- Renombrar URL a `baqueano.netlify.app`

**Con SA818 físico (desbloqueador hardware):**
- FW-3a `--open-rx` → ver frames RX_AUDIO por WiFi
- FW-3a failsafe → cortar WiFi en TX, confirmar vuelta a RX
- FW-2 test RF → TX fuera de whitelist = bloqueado en hardware
- FW-4 → J2 dispara TX

---

## TOOLCHAIN (para no perder tiempo)

**PlatformIO (ESP32):**
```
[Environment]::SetEnvironmentVariable('PLATFORMIO_BUILD_DIR','C:\Users\lukaz\pio-build','Process')
& 'C:\Users\lukaz\pio-venv\Scripts\pio.exe' run -e esp32dev -d 'C:\Users\lukaz\OneDrive\Escritorio\kv4p-ht-main\kv4p-ht-main\microcontroller-src'
```
⚠️ Python system installer ROTO — usar SIEMPRE `C:\Users\lukaz\pio-venv\Scripts\pio.exe`

**Flash ESP32 (COM9):** maniobra BOOT manual — sostener BOOT mientras esptool conecta (DTR no baja GPIO0 solo). Flash completo (bootloader + partitions + firmware).

**Android build:**
```
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
cd MotoRFAR-MTTT\KV4PHT && .\gradlew assembleDebug
```
Emulador: `C:\Users\lukaz\AppData\Local\Android\Sdk\emulator\emulator.exe -avd Pixel_10_Pro_XL`

**KiCad CLI:** `C:\Users\lukaz\AppData\Local\Programs\KiCad\10.0\bin\kicad-cli.exe`
⚠️ NUNCA rutear cobre crítico PCB por script — siempre GUI KiCad.

---

## LINKS RÁPIDOS
- Release v1.0-beta1: https://github.com/lukaz2004/MotoRFAR/releases/tag/v1.0-beta1
- Web Netlify: https://baqueano.netlify.app (cuenta reclamada, URL final — verificado online 2026-07-05)
- PCB backup pre-cambios: `AppData\Local\Temp\kv4p-ht-BACKUP-pre-82mm.kicad_pcb`
- PCB backup original: `AppData\Local\Temp\kv4p-ht-ORIGINAL.kicad_pcb`

---

## CÓMO ARRANCAR CADA ETAPA (si querés el detalle completo)

| Tarea | Decile a Claude |
|-------|----------------|
| HW-1 GUI KiCad | "Leé `_PROYECTO\ramas\HW_hardware.md` y cerramos HW-1" |
| Fix ProGuard | "Leé `_PROYECTO\ramas\APP_app.md` y arreglamos ProGuard/R8" |
| Pantalla licencias | "Leé `_PROYECTO\ramas\APP_app.md` y agregamos pantalla Acerca de/Licencias" |
| FW-3a validación SA818 | "Leé `_PROYECTO\ramas\FW_firmware.md` (FW-3a) y hacemos validación con SA818" |
| APP-2 WiFi client | "Leé `_PROYECTO\ramas\APP_app.md` (APP-2) y `_PROYECTO\01_CONTRATOS.md`" |
| EST-1 carcasa | "Leé `_PROYECTO\ramas\EST_estetica.md` y arrancamos EST-1" |
| Web Netlify | "Leé `_PROYECTO\web\README.md` y reclamamos/renombramos el sitio" |

---

## REGLAS QUE NO SE NEGOCIAN
- 🔴 Nunca script para rutear cobre crítico en KiCad → GUI siempre.
- 🔴 Whitelist TX: firmware es la autoridad final (no confiar solo en la app).
- 🔴 Todo trabajo hecho en filesystem efímero de una sesión (bash_tool, `/home/claude/...`) se copia al disco real vía Desktop Commander ANTES de cerrar el chat. Si no se copia, se pierde — ya pasó con el rebuild de la web (ver ESTADO AL 2026-07-02).
- 🟢 Backup del PCB ANTES de cualquier cambio.
- 🟢 Cada chat = 1 etapa. Los contratos (`01_CONTRATOS.md`) son la memoria compartida entre ramas.
- 🟢 Python system installer roto en esta máquina — siempre pio-venv.
