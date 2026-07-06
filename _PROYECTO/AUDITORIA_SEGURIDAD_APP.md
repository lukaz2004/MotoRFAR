# Auditoría de seguridad — App Baqueano (`ar.motorfar.app`)

**Fecha:** 2026-07-05
**Alcance:** `KV4PHT/` (app Android). Auditoría estática, nivel paranoico ("resistir estado-nación"). Solo lectura — no se modificó código.
**Metodología:** revisión manual de código fuente + búsqueda en historial completo de git + revisión de manifest, gradle, y configuración de red.

---

## Resumen ejecutivo

No encontré ningún hallazgo CRÍTICO. La postura de seguridad general es sólida para lo que es: una app de radio VHF sin backend propio, sin autenticación de usuarios, sin dinero involucrado. El diseño ya asume correctamente que **el medio de transmisión (VHF simplex) es inherentemente inseguro** (sin cifrado, sin autenticación de origen) y no intenta fingir lo contrario. Los riesgos reales son de **denegación de servicio local por paquetes RF malformados** (mitigado con `catch` genéricos, pero no en el nivel más profundo del parser) y un par de puntos de **defensa en profundidad** que se pueden reforzar fácil.

El punto que más me preocupa para "resistir estado-nación" no es de código: es que **la app corre en un teléfono con GPS + radio en texto claro**, así que cualquier adversario con un SDR y línea de vista puede triangular a todo el grupo sin tocar ni una línea de este código. Eso es una limitación física del medio (VHF sin cifrado), no un bug — pero hay que asegurarse de que esté bien comunicado al usuario (ver hallazgo MEDIO-1).

Nada bloqueante para seguir operando. Las recomendaciones son incrementales.

---

## Hallazgos

### CRÍTICO

Ninguno.

---

### ALTO

Ninguno confirmado como explotable de forma práctica hoy. Ver MEDIO-2 para el caso más cercano a "alto" (bug real de parsing, pero con red de contención que lo neutraliza).

---

### MEDIO

#### MEDIO-1 — Ubicación GPS transmitida en claro por VHF: falta reforzar la comunicación de esto como limitación de diseño, no de bug

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:1602-1669` (`performPositionBeacon`, `sendPositionBeacon`), `KV4PHT/app/src/main/java/ar/motorfar/app/ui/MainActivity.kt` (flujo Man-Down, SendWaypoint)

**Problema:** cada baliza APRS (posición propia, waypoints compartidos, y la posición automática que dispara Man-Down) se transmite como paquete AX.25 sin cifrar por las 3 frecuencias VHF públicas. Cualquier persona con un handheld o SDR sintonizado a 138.510/139.970/140.970 MHz puede recibir, decodificar (formato APRS es público) y geolocalizar en tiempo real a cualquier miembro del grupo, incluyendo el momento exacto de una caída (Man-Down) con su posición GPS.

**Esto es correcto por diseño** — no hay forma de cifrar sin licencia/hardware distinto, y cifrar rompería la interoperabilidad con otros usuarios del canal. No es un bug de código.

**Escenario de riesgo real:** un adversario con recursos (la barra que pone "estado-nación" en el pedido) podría:
1. Escuchar pasivamente el canal y armar un patrón de movimiento de un usuario específico (stalking).
2. Usar el Man-Down (que transmite posición automáticamente sin confirmación si no se cancela) como oráculo: sabe exactamente cuándo alguien tuvo un accidente y dónde, antes que lo sepa cualquier otro humano.
3. Suplantar (spoof) balizas APRS de otros miembros del grupo — ver MEDIO-2 sobre falta de autenticación de origen.

**Verificado OK:** el código nunca intenta fingir que esto es privado o cifrado — no hay ninguna variable/comentario engañoso tipo "encrypted" cerca de este flujo. Bien.

**Recomendación:**
- Confirmar que `PrivacyPolicyActivity` y el texto de onboarding/Man-Down (`AliasSettingScreen.kt:215`, "cuenta 30s y avisa al grupo") dejen explícito que la posición se transmite **sin cifrado, visible para cualquiera en el canal**, no solo "al grupo". Ahora mismo el copy dice "avisa al grupo" lo cual puede sugerir privacidad que no existe.
- Considerar agregar un disclaimer específico la primera vez que se activa Man-Down o Modo Ruta, no solo en la política de privacidad enterrada en ajustes.
- No es un fix de código, es un fix de copy/UX — pero es la mitigación real disponible.

---

#### MEDIO-2 — `PositionParser.parseUncompressed`: lectura fuera de rango antes del chequeo de longitud

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/aprs/parser/PositionParser.java:39-55`

**Problema:**
```java
public static Position parseUncompressed(byte[] msgBody, int cursor) throws Exception {
    ...
    if (msgBody[0] == '/' || msgBody[0] == '@') {
        // With a prepended timestamp, jump over it.
        if (msgBody[cursor+6] == 'z') {           // ← línea 44: lee cursor+6 SIN validar longitud
            ...
        }
    }
    if (msgBody.length < cursor + 19) {            // ← línea 53: el chequeo de longitud viene DESPUÉS
        throw new UnparsablePositionException("Uncompressed packet too short");
    }
```
El acceso a `msgBody[cursor+6]` (y los siguientes índices dentro del `if`) ocurre **antes** de validar que el buffer tenga longitud suficiente. Con `cursor=1` (el valor real usado en producción, ver `parseUncompressed(byte[] msgBody)` en línea 139-141) y un `msgBody` corto (ej. un paquete APRS malformado o truncado de 5 bytes), esto dispara `ArrayIndexOutOfBoundsException` en vez de la excepción controlada `UnparsablePositionException`.

**Escenario de explotación concreto:** cualquier persona con un handheld VHF/SDR sintonizado al canal puede transmitir un paquete AX.25 con payload APRS deliberadamente truncado (empezando con `/` o `@` pero de menos de 7 bytes) para forzar esta excepción en el teléfono de cualquier miembro del grupo que esté escuchando.

**Por qué no es CRÍTICO ni ALTO en la práctica:** el único punto de entrada real de este parser desde la red es `RadioAudioService.handleAx25Packet()` (línea 1525-1550), que envuelve **todo** el parseo en un `catch (Exception e)` genérico (línea 1547) y descarta el paquete silenciosamente. `ArrayIndexOutOfBoundsException` es una `RuntimeException`, así que ese `catch` la atrapa igual. **No hay crash ni DoS explotable hoy** gracias a esa red de contención en la capa superior. Verificado OK ese `catch` — es la razón por la que esto baja de "crash remoto explotable" a "deuda de robustez".

**Riesgo residual:** si en el futuro alguien llama a `PositionParser.parseUncompressed` desde otro punto sin ese `catch` amplio (por ejemplo, un test, una herramienta de importación de logs APRS, o una futura feature que reprocese paquetes guardados), el bug queda listo para explotarse ahí. Es una bomba de tiempo de mantenimiento, no un exploit activo.

**Recomendación:** mover el chequeo `if (msgBody.length < cursor + 19)` **antes** del bloque que lee `msgBody[cursor+6]`. Es un cambio de 3 líneas, cero impacto funcional.

---

#### MEDIO-3 — `WifiTransport`: sin validación de origen del paquete UDP recibido

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/radio/WifiTransport.java:162-196` (`startReceiving`)

**Problema:** `DatagramSocket.receive(pkt)` acepta paquetes de **cualquier IP y puerto** en la red WiFi del SoftAP, no solo de `192.168.4.1:4210` (la IP fija documentada del ESP32, líneas 22-23, 36-37). No hay ningún chequeo tipo `pkt.getAddress().equals(espAddress)` antes de pasar el contenido al `KissParser`.

**Escenario de explotación concreto:** el SoftAP del ESP32 usa una clave WPA2 compartida (`motorfar1234`, ver `_PROYECTO/PENDIENTES.md:135,167` — hoy de prueba, ya marcado como pendiente cambiar). Cualquier dispositivo que conozca esa clave y esté asociado al mismo AP puede:
1. Enviar UDP arbitrario al puerto 4210 del teléfono (el teléfono es cliente, no hace falta apuntar al ESP32; solo hace falta saber a qué puerto efímero está escuchando el teléfono, que es visible por tráfico de red local o por fuerza bruta de puertos).
2. Inyectar frames KISS/KV4P falsos que el `KissParser` va a procesar igual que si vinieran del ESP32 real — incluyendo falsos `COMMAND_HELLO`, `COMMAND_RX_AUDIO`, o `COMMAND_DEVICE_STATE` con `radioModuleStatus`/`RfModuleType` arbitrarios.
3. Esto no permite transmitir en frecuencias fuera de whitelist (eso lo decide y aplica la propia app + firmware, ver hallazgo "verificado OK" de whitelist abajo) pero sí permite:
   - Desincronizar el estado de la UI (mostrar frecuencia/squelch/RSSI falsos).
   - Inyectar audio RX falso (`COMMAND_RX_AUDIO`) — molestia, no compromiso de seguridad real.
   - Posible confusión operativa en un contexto de emergencia (ej. hacer creer que el radio está en un estado que no está).

**Nivel de riesgo real:** requiere que el atacante ya esté asociado al WiFi del grupo (conoce o brutea la clave WPA2 de 12 caracteres alfanuméricos simple — `motorfar1234` es débil pero es un problema de firmware, no de esta app). Una vez adentro de esa red, el impacto vía este bug específico es limitado a spoofing de estado/audio, no a exfiltración de datos ni a TX fuera de whitelist. No es CRÍTICO, pero es la puerta de entrada más barata para alguien que ya está en el WiFi.

**Recomendación:**
```java
DatagramPacket pkt = new DatagramPacket(buf, buf.length);
s.receive(pkt);
if (!pkt.getAddress().equals(espAddress)) {
    continue; // descartar paquete de origen no confiable
}
```
Defensa en profundidad barata: 3 líneas, sin downside funcional (el ESP32 siempre tiene IP fija `192.168.4.1` por diseño del SoftAP). Esto no reemplaza la necesidad de cambiar la clave WPA2 default del firmware (ya trackeado en `PENDIENTES.md`), pero cierra el vector desde el lado de la app.

---

### BAJO

#### BAJO-1 — `RECV_BUFFER_SIZE` fijo en 4096 en `WifiTransport`, sin chequeo explícito de paquetes UDP anormalmente grandes

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/radio/WifiTransport.java:38,165-177`

`DatagramPacket` trunca automáticamente si el paquete entrante excede `buf.length`, así que no hay overflow de buffer real (Java lo maneja de forma segura). Pero un paquete UDP de un actor malicioso en la red mayor a 4096 bytes se trunca silenciosamente y se pasa igual al parser KISS, lo cual podría producir un frame corrupto. El `KissParser` sí valida longitud máxima de frame (`KISS_MAX_FRAME_SIZE`, `Protocol.java:560-563` — `dropFrame` si excede) así que el impacto es nulo en la práctica. **Verificado OK** en la capa del parser KISS — solo dejo la nota porque el truncamiento silencioso en la capa UDP podría ser confuso al debuggear. No requiere acción.

#### BAJO-2 — `activity-alias` MainActivity `exported="true"` — correcto y necesario, pero confirmar que no haya lógica sensible alcanzable vía intent externo

**Archivo:** `KV4PHT/app/src/main/AndroidManifest.xml:80-97`

`MainActivity` es `exported="true"` porque es el launcher (`android.intent.category.LAUNCHER`) — esto es obligatorio y correcto, no es un hallazgo real. También expone la acción custom `ar.motorfar.app.OPEN_CHAT_ACTION` (línea 90-93) sin permiso declarado, lo que significa que **cualquier otra app instalada en el mismo teléfono** puede lanzar `MainActivity` con esa acción. Revisé el manejo — no vi que abra pantallas de configuración sensibles ni ejecute TX directamente desde el intent, solo navega a la pantalla de chat. Impacto: bajo, alguien podría forzar la apertura de la app y de la pantalla de chat desde otra app maliciosa instalada (molestia de UX, no fuga de datos ni TX no autorizado). **Recomendación opcional:** si se quiere ser estricto, agregar `android:permission` custom (`signature` level) a ese intent-filter, pero dado que no dispara ninguna acción de TX ni expone datos, lo dejo como BAJO informativo, no accionable con urgencia.

---

## Verificado OK (para que quede constancia)

1. **Whitelist TX — defensa en profundidad correcta.** Confirmé que la app tiene su propia validación independiente del firmware: `TxWhitelist.java` (frecuencias `139.970` / `138.510` / `140.970` MHz, tolerancia de 0.0005 MHz) se aplica en `RadioAudioService.canTransmitOnFrequency()` / `updateTxAllowed()` en **todos** los caminos que fijan frecuencia de TX: `tuneToFreq()` (línea 644), `tuneToMemory()` (línea 696), y `updateTxLimitsForBand()` (línea 1221, llamado en el handshake). No encontré ningún camino de debug/flag/bypass que salte esta validación antes de habilitar `radioModule.setTxAllowed(...)`. La app **no confía ciegamente en que el firmware bloquee**; valida de su lado también, tal como pide la regla del proyecto.

2. **Manejo de secretos — sin hardcodeos, sin exposición en git.**
   - `KV4PHT/app/build.gradle:7-43` lee `keystore.properties` desde el filesystem local (`rootProject.file(...)`), nunca hardcodea contraseñas. Si el archivo no existe, el build simplemente no firma release (fallback seguro, no crashea ni usa defaults inseguros).
   - `git log --all --full-history -- KV4PHT/keystore.properties KV4PHT/baqueano-release.jks` → **sin resultados**, nunca existieron en ningún commit de todo el historial (139 commits revisados).
   - Búsqueda adicional en todo el historial (`git log --all -p`) por patrones `storePassword|keyPassword|keyAlias|motorfar1234|wpa|passphrase` → solo aparecen menciones en `_PROYECTO/PENDIENTES.md` (documentación del pendiente, no secretos reales) y una coincidencia binaria irrelevante.
   - `git ls-files` confirma que solo `KV4PHT/keystore.properties.template` está trackeado (el template, sin contraseñas reales) — el `.properties` real y el `.jks` real nunca se agregaron.
   - `.gitignore` cubre `local.properties`; confirmá manualmente que `keystore.properties` y `*.jks` están en el `.gitignore` de `KV4PHT/` (mencionado en el commit `6732f7c` que introdujo el pipeline de firma) — no aparecen en `git status` como trackeados, consistente con que el ignore funciona.
   - **La clave WPA2 `motorfar1234` del SoftAP no aparece en ningún lugar del código fuente de la app** (`grep` sobre `KV4PHT/app/src` → sin resultados). Es puramente un secreto de firmware/config de red, fuera del alcance de la app — la app se conecta a la red WiFi como cualquier cliente, sin necesidad de conocer ni loguear la clave (Android maneja la asociación WiFi vía UI del sistema, no vía código de la app).

3. **Logging de coordenadas GPS — no se loguean coordenadas reales.** Revisé todos los `Log.d/i/w/e` en `RadioAudioService.java` relacionados a posición/beacon (líneas 1564-1667) — todos loguean únicamente mensajes de estado ("Skipping position beacon...", "Beaconing position via APRS") sin interpolar `latitude`/`longitude` en el string. Búsqueda global por patrón `Log\.\w+\(.*[Ll]at|[Ll]ong|position` en todo `src/main/java` no encontró logging de coordenadas.

4. **Almacenamiento de rutas GPS (Room) — sin cifrado, pero consistente con el resto de la app y sin exposición adicional.** `RoutePoint.kt`/`RoutePointDao.kt` guardan `latitude`/`longitude`/`timestamp`/`alias`/`sessionId` en SQLite plano vía Room (`kv4pht-db`), sin `SQLCipher` ni cifrado a nivel de campo. Esto es coherente con que Android ya sandboxea la DB de la app (solo accesible por la propia app o con root/debug USB), y con que el resto del proyecto no cifra nada localmente (photos, mapas offline, etc. tampoco). No lo marco como hallazgo accionable porque cifrar esto sin cifrar el resto de la app sería inconsistente y de bajo valor real — el modelo de amenaza relevante acá es "alguien con acceso físico + ADB + dispositivo desbloqueado", que ya implica compromiso total del teléfono. Si se quiere subir el estándar a futuro, se puede evaluar `SQLCipher` para toda la DB de una, no solo esta tabla.

5. **Migraciones Room — sin `fallbackToDestructiveMigration`.** `AppDatabase.java:88-90` deja comentado explícitamente por qué no usan destructive fallback: prefieren `IllegalStateException` en vez de borrar datos silenciosamente si falta una migración. Buena práctica, evita pérdida silenciosa de datos de ruta del usuario.

6. **`network_security_config.xml` — restrictivo y correcto.** `cleartextTrafficPermitted="false"` global, con la única excepción siendo el dominio público `repeaterbook.com` (también HTTPS-only). Comentario explícito: "La app no tiene backend propio — toda comunicación es por RF." Correcto y minimalista.

7. **Permisos del manifest — alineados con el principio de mínimo privilegio.** Revisé los 14 permisos declarados en `AndroidManifest.xml` contra su uso real: `RECORD_AUDIO`/`MODIFY_AUDIO_SETTINGS` (PTT/audio), `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`/`FOREGROUND_SERVICE_LOCATION` (balizas GPS), `INTERNET`/`CHANGE_NETWORK_STATE`/`ACCESS_NETWORK_STATE` (WiFi UDP al ESP32 — este es el único uso de `INTERNET`, no hay llamadas a servicios externos salvo RepeaterBook), `FOREGROUND_SERVICE*` (servicio de radio en background), `POST_NOTIFICATIONS`/`VIBRATE`/`WAKE_LOCK` (alertas), `WRITE_EXTERNAL_STORAGE` correctamente acotado con `maxSdkVersion="28"` (comentario explica el porqué). No encontré permisos sobrantes sin uso aparente.

8. **Componentes exportados — todos `exported="false"` salvo `MainActivity` (launcher, obligatorio) y `MainActivity` para el intent custom `OPEN_CHAT_ACTION`.** El único `Service` (`RadioAudioService`) es `exported="false"` — correcto, nada externo puede bindearse o iniciarlo directamente. No hay `BroadcastReceiver` declarado en el manifest en absoluto (el `ACTION_USB_PERMISSION` se registra dinámicamente vía `PendingIntent.getBroadcast`, patrón estándar y no explotable por otras apps sin conocer el intent exacto y sin permiso).

9. **Parser KISS (`Protocol.KissParser`) — buena validación de límites.** `appendByte()` corta el frame (`dropFrame = true`) si excede `KISS_MAX_FRAME_SIZE` (línea 560-563). `processFrame()` valida `payloadLen > 0 && payloadLen <= PROTO_MTU` antes de pasar datos AX.25 (línea 578). `processVendorFrame()` valida longitud mínima de header antes de leer el prefijo `KV4P` (línea 587) y descarta comandos desconocidos sin intentar interpretarlos (línea 606-610). Buen manejo defensivo de un parser que recibe bytes crudos desde la red/USB.

10. **`APRSAdapter.java` — sin inyección vía `geo:` URI.** El click handler (línea 123-131) construye un `geo:lat,long?q=lat,long` con valores `double` ya parseados (no strings crudos del paquete recibido), por lo que no hay vector de inyección de URI malformada. Correcto.

---

## Dependencias (`build.gradle`)

Revisé `KV4PHT/app/build.gradle`. No pude ejecutar `npm audit`/equivalentes Gradle en este entorno de solo-auditoría, así que esto es revisión manual por nombre/versión:

- La mayoría de las dependencias de AndroidX/Compose están en versiones razonablemente recientes (`compose-bom:2024.02.02`, `room:2.7.2`, `lifecycle:2.9.1`).
- `org.osmdroid:osmdroid-android:6.1.18` y `com.google.android.gms:play-services-location:21.3.0` — sin CVEs conocidas por mí que salten a la vista por versión.
- `io.github.dkaukov:esp32-flash-lib:1.1.16` y `io.github.jaredmdobson:concentus:1.0.1` — librerías de nicho, bajo mantenimiento; no tengo forma de confirmar CVEs sin acceso a internet/`gradle dependencies` en este entorno. **Recomendación:** correr `./gradlew app:dependencies` y `./gradlew app:dependencyCheckAnalyze` (si se agrega el plugin OWASP Dependency-Check) como parte del pipeline, no lo pude ejecutar acá.
- `org.projectlombok:lombok:1.18.30` — versión reciente, sin problema.

No until until se corra una herramienta real (`gradle dependencyCheck` o Snyk) esto queda como pendiente de verificación automatizada, no como hallazgo confirmado.

---

## Resumen de acciones recomendadas (por prioridad)

1. **(MEDIO-3)** Agregar validación de origen (`pkt.getAddress().equals(espAddress)`) en `WifiTransport.startReceiving()` — 3 líneas, cierra el vector de spoofing UDP desde la red WiFi del grupo.
2. **(MEDIO-2)** Reordenar el chequeo de longitud antes del acceso a índices en `PositionParser.parseUncompressed()` — deuda de robustez, no explotable hoy por el `catch` amplio en `RadioAudioService`, pero fácil de arreglar y evita que se vuelva un problema si el parser se reutiliza en otro contexto sin ese `catch`.
3. **(MEDIO-1)** Reforzar el copy de Man-Down/Modo Ruta/Waypoints para dejar explícito que la posición viaja sin cifrar y es visible por cualquiera en el canal — no solo "al grupo". Cambio de texto, no de código.
4. Ejecutar `./gradlew app:dependencies` con una herramienta de CVE scanning cuando haya conectividad para cerrar el punto de dependencias.
5. Ya trackeado en `PENDIENTES.md`: cambiar la clave WPA2 default del firmware (`motorfar1234`) por una configurable — no es responsabilidad de la app, pero es el problema de raíz detrás de MEDIO-3.

Nada de esto bloquea seguir operando el proyecto. Ninguno es urgente al nivel de "parar todo", pero los 3 MEDIO son baratos de resolver y suben el piso de seguridad real.

---

## Estado de correcciones — 2026-07-06

Build verificado: `gradlew assembleDebug` → `BUILD SUCCESSFUL` con los 3 cambios de abajo aplicados.

- ✅ **MEDIO-3 — RESUELTO.** `WifiTransport.startReceiving()` ahora descarta cualquier `DatagramPacket` cuya IP de origen no coincida con `espAddress` (IP fija del ESP32, aprendida al conectar). Cierra el vector de spoofing UDP desde otro dispositivo asociado al mismo WiFi.
- ✅ **MEDIO-2 — RESUELTO.** En `PositionParser.parseUncompressed()`, el chequeo `msgBody.length < cursor + 19` se movió antes de cualquier acceso a índice del buffer (incluido `msgBody[0]`). Un paquete APRS truncado ya no puede disparar `ArrayIndexOutOfBoundsException` — siempre cae en la excepción controlada `UnparsablePositionException`.
- ✅ **MEDIO-1 — RESUELTO (copy).** El texto de Man-Down en `AliasSettingScreen.kt` ya no dice "avisa al grupo" ni "cuenta 30s" (desactualizado desde el countdown variable por fuerza G) — ahora aclara explícitamente que la posición se transmite **sin cifrar por VHF, audible para cualquiera en el canal**, no solo el grupo.
- ⬜ **Pendiente sin resolver:** correr `./gradlew app:dependencies` + una herramienta de CVE scanning (Snyk/OWASP Dependency-Check) sobre `io.github.dkaukov:esp32-flash-lib` y `io.github.jaredmdobson:concentus` — no se pudo ejecutar en este entorno de auditoría, sigue como verificación manual futura.
- ⬜ **Relacionado, ver auditoría de firmware:** el root cause de MEDIO-3 (clave WPA2 compartida/hardcodeada) se resolvió del lado firmware (clave única por equipo), pero la UI de la app para ver/cambiar esa clave desde Ajustes todavía no existe — anotado en `PENDIENTES.md`.

**Para la próxima verificación:** confirmar en dispositivo real que la validación de IP en `WifiTransport` no rompe la reconexión tras perder señal (la IP del ESP32 no debería cambiar nunca al ser fija por `softAPConfig`, pero vale confirmarlo con hardware real).
