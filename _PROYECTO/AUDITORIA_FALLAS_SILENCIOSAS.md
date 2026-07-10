# Auditoria de fallas silenciosas -- App Baqueano (`ar.motorfar.app`)

**Fecha:** 2026-07-09
**Alcance:** `KV4PHT/app/src/main/java/ar/motorfar/app/` (app Android). Auditoria estatica, solo lectura -- no se modifico codigo.
**Motivacion:** se encontro un bug real de esta clase -- una alerta de EMERGENCIA puede transmitirse a nivel radio sin que nadie del grupo la escuche, y ni el emisor ni la app se enteran (sin ack, sin retry, sin error en ningun lado). Esta auditoria parte de la hipotesis de que si un flujo de seguridad falla asi de silencioso, probablemente no es el unico.
**Criterio de ranking:** por consecuencia real para alguien que depende de esta app *fuera de cobertura*, no por severidad clasica de seguridad. Un "conectado" falso en pantalla es peor aca que la mayoria de los hallazgos de una auditoria de seguridad tradicional, porque no hay backend ni crash reporting -- lo que falla en el campo, fallado queda, para siempre, sin que nadie -ni el usuario, ni el desarrollador- se entere jamas.

---

## Resumen ejecutivo

El bug que motivo esta auditoria no es un caso aislado -- es el sintoma de un patron que se repite en varios puntos: **la app confirma exito sin haber verificado nada, y los canales de error que si existen en el diseno no estan conectados a ninguna UI.**

El hallazgo mas serio no es codigo nuevo: es que `RadioAudioServiceCallbacks` (la interfaz que el Service usa para avisarle a la Activity de fallos -- firmware desactualizado, modulo de radio no encontrado, posicion desconocida, error de chat) tiene **10 de sus ~19 callbacks sin implementar** en `MainActivity.kt`. No es un olvido de una linea: son los callbacks especificamente disenados para el caso "algo fallo, avisale al usuario" -- y caen en un no-op vacio por diseno de la interfaz (`default void unknownLocation() {}`). El resultado es un patron consistente: la app hace su mejor esfuerzo de forma optimista, no verifica el resultado, y si algo sale mal en el camino, no hay ningun lado a donde ese error pueda ir.

Combinado con la falta total de reintento/heartbeat despues del handshake inicial y con que la app nunca pide el permiso `POST_NOTIFICATIONS` en runtime, hay al menos tres formas independientes de terminar con **"CONECTADO" en pantalla mientras el radio no transmite ni un bit**, y al menos dos formas de que **una alerta de Man-Down o EMERGENCIA muestre "enviada" sin haber salido nunca del telefono**.

Nada de esto es exotico -- son los caminos de falla mas comunes en el uso real: el modulo de radio del equipo se afloja con la vibracion de la ruta, el GPS tarda en dar senal en un valle, el firmware de un equipo no esta actualizado, el telefono es un Android 7 gama baja sin acelerometro decente. Son exactamente los escenarios de campo que este proyecto dice tomar en serio.

---

## Hallazgos

### CRITICO

#### CRITICO-1 -- El radio puede quedar en `BAD_FIRMWARE` (no funcional) mostrando "CONECTADO" en pantalla, sin reintento y sin aviso, hasta que el usuario reinicie la app a mano

**Archivos:**
- `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:1376-1450` (`scheduleHelloTimeout`, `validateHello`)
- `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:1150-1156` (`isConnectionReady`)
- `KV4PHT/app/src/main/java/ar/motorfar/app/ui/MainActivity.kt:291-326` (`serviceCallbacks`, no implementa `missingFirmware`, `outdatedFirmware`, `radioModuleNotFound`)
- `KV4PHT/app/src/main/java/ar/motorfar/app/ui/compose/components/AppStatusBar.kt:49-60` (el LED/label "CONECTADO"/"SIN RADIO" solo lee `isConnected`)

**Problema:** hay tres caminos donde el handshake con el ESP32 falla *despues* de que el socket WiFi/UDP ya esta abierto, y **ninguno de los tres llama a `radioMissing()`** (el unico metodo que pone `isConnected = false` en la UI via `MainActivity.kt:300`):

1. **Timeout de HELLO** (`RadioAudioService.java:1376-1389`) -- si el ESP32 no manda el HELLO inicial en 60s, cae en `setMode(RadioMode.BAD_FIRMWARE)` + `getCallbacks().missingFirmware()` -- callback que **no esta implementado en ningun lado del codigo** (confirmado por busqueda global, cero resultados de `override fun missingFirmware`).
2. **HELLO invalido o firmware desactualizado** (`:1413-1430`) -- mismo patron: `getCallbacks().outdatedFirmware(...)`, tampoco implementado.
3. **Modulo de radio VHF no detectado por el ESP32** (`:1432-1439`, `RADIO_STATUS_NOT_FOUND`) -- mismo patron: `getCallbacks().radioModuleNotFound()`, tampoco implementado.

En los tres casos, `wifiTransport` **sigue vivo** (nadie llama `closePortAndReset()`), asi que `isConnectionReady()` (linea 1150-1156, que solo chequea que el socket UDP este abierto y la IP del ESP32 conocida -- no que el handshake haya terminado bien) **sigue devolviendo `true`**. Como `ConnectionController` (`ConnectionController.java:23`) solo reintenta cuando `!isConnectionReady()`, el reintento automatico **nunca se dispara** -- la app queda parada en `BAD_FIRMWARE` para siempre, sin loop de recuperacion.

**Por que esto es CRITICO y no un detalle tecnico:** el escenario de disparo mas plausible en el uso real de este proyecto es justamente el que motiva la Res. 5/2015 y el hardware de mochila/moto: **un conector del modulo VHF que se afloja con la vibracion de la ruta**, o un **equipo con firmware sin actualizar** que alguien lleva prestado. En cualquiera de esos casos: el usuario ve el LED del `AppStatusBar` en verde/"CONECTADO" (porque el socket sigue abierto), puede intentar transmitir un PTT o una alerta de EMERGENCIA, y **nada sale** -- sin ningun cartel, sin ningun reintento automatico, sin forma de saber por que, hasta que fuerce el cierre de la app y la vuelva a abrir. En un grupo separandose en un cruce de caminos sin senal, esto es indistinguible de "esta todo bien" hasta que alguien necesita usar la radio de verdad.

**Recomendacion:** en los tres puntos (`:1384`, `:1417`/`:1427`, `:1435`), ademas de invocar el callback especifico, llamar tambien a `radioMissing()` (o exponer un nuevo estado de UI equivalente) para que `isConnected` refleje la realidad y `ConnectionController` pueda reintentar. Como minimo, implementar `missingFirmware()`, `outdatedFirmware()` y `radioModuleNotFound()` en `MainActivity.kt` con un cartel visible -- hoy caen en el no-op por defecto de la interfaz.

---

#### CRITICO-2 -- `fireManDownAlert()` muestra "Alerta de emergencia enviada" sin haber verificado que se transmitio nada

**Archivos:**
- `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:768-782` (`fireManDownAlert`)
- `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:846-862` (`transmitEmergencyAlert`)
- `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:1885-1927` (`sendPositionBeacon`, 4 early-return silenciosos)
- `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:2017-2040` (`sendChatMessage`, retorna "exito" aunque el envio se haya descartado adentro)
- `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:2048-2065` (`txAX25Packet`, 3 early-return silenciosos, la causa raiz real)

**Problema:** cuando dispara Man-Down (`fireManDownAlert`, lineas 768-782), la secuencia es:
```java
private void fireManDownAlert() {
    ...
    transmitEmergencyAlert();               // fire-and-forget, ver abajo
    NotificationCompat.Builder sentNotif = new NotificationCompat.Builder(this, ALERT_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Alerta de emergencia enviada")
            .setContentText("Se transmitio tu posicion por 140.970 MHz (canal Emergencia)")
            ...
    nm.notify(MANDOWN_NOTIFICATION_ID, sentNotif.build());
}
```
La notificacion de "enviada" se posta **inmediatamente despues** de llamar a `transmitEmergencyAlert()`, sin esperar ni chequear resultado alguno. `transmitEmergencyAlert()` (lineas 846-862) a su vez llama a `sendPositionBeacon()` sin mirar su resultado, y programa `sendChatMessage("CQ", alertText)` para 2 segundos despues via `handler.postDelayed` -- es decir, la notificacion de "enviada" sale **antes** de que el mensaje de texto de la alerta siquiera se intente enviar.

Cada uno de esos dos envios tiene multiples caminos de descarte silencioso:
- `sendPositionBeacon()` (lineas 1890-1893, 1900-1903, 1906-1910, 1912-1916) hace `return` sin transmitir nada si: el radio no esta conectado, TX no esta permitido por whitelist, el modo no es RX/SCAN, falta el permiso `ACCESS_FINE_LOCATION`, o no hay Google Play Services. Los dos ultimos casos llaman a `getCallbacks().unknownLocation()` -- **callback que jamas se implementa en `MainActivity.kt`** (confirmado, cero resultados de `override fun unknownLocation`), asi que ni siquiera ese aviso llega a ningun lado.
- `sendChatMessage()` (lineas 2017-2040) delega en `txAX25Packet()` (lineas 2048-2065), que descarta el paquete con solo un `Log.e(...)` si TX no esta permitido, si el modo no es RX (por ejemplo si sigue en `TX` por un PTT fisico pegado, o en `SCAN`), o si `hostToEsp32 == null`. **`sendChatMessage()` no propaga ninguno de esos tres fallos** -- su unico `catch` es para `IllegalArgumentException` al construir el paquete (linea 2034-2038, si llama a `getCallbacks().chatError(...)`, tambien sin implementar en la UI); en los tres casos de `txAX25Packet()` que retornan `void`, `sendChatMessage()` sigue de largo y **devuelve `messageNumber - 1` igual** (linea 2039) -- un valor que parece "numero de mensaje enviado con exito" aunque el paquete nunca haya salido del telefono.

**Por que esto es CRITICO:** es el mismo bug de fondo que motivo esta auditoria, pero un nivel *antes* de la RF -- aca ni siquiera se confirma que el paquete salio del *telefono* hacia el equipo de radio, y aun asi se le muestra a la persona (que puede estar inconsciente o semi-consciente tras una caida, es el escenario de diseno de Man-Down) una notificacion explicita de "se transmitio tu posicion". Si alguien mas del grupo estaba mirando el telefono de la victima para confirmar que la alerta salio, ve una confirmacion falsa.

**Recomendacion:** que `sendPositionBeacon()`/`sendChatMessage()`/`txAX25Packet()` devuelvan o propaguen un resultado real (ya existe el patron -- `startPtt()` se cambio de `void` a `boolean` el 2026-07-07 por exactamente este motivo, ver commit `bad712c`), y que `fireManDownAlert()` solo muestre "enviada" cuando ambos envios confirmen exito -- o, como minimo, muestre un aviso explicito de "no se pudo confirmar el envio" cuando no lo confirman. Implementar `unknownLocation()` y `chatError()` en `MainActivity.kt` en vez de dejarlos en el no-op por defecto.

---

#### CRITICO-3 -- La app nunca pide `POST_NOTIFICATIONS` en runtime: en Android 13+ todas las notificaciones de seguridad pueden no mostrarse nunca, sin que la app se entere

**Archivos:**
- `KV4PHT/app/src/main/AndroidManifest.xml:11` (el permiso esta declarado)
- Busqueda global en `KV4PHT/app/src/main/java`: cero resultados para `POST_NOTIFICATIONS`, `ActivityResultContracts.RequestPermission` sobre ese permiso, o `areNotificationsEnabled()`
- `KV4PHT/app/build.gradle:16,21` (`compileSdk 35`, `targetSdk 35`)
- Afecta directamente: `RadioAudioService.java:784-817` (`postManDownNotification`), `:870-883` (`notifyIncomingAlert`), `:774-781` (confirmacion de Man-Down enviado), `:656-669` (mensajes de chat APRS)

**Problema:** con `targetSdk 35`, `POST_NOTIFICATIONS` es un permiso "dangerous" que Android 13+ (API 33+) deniega **por defecto** hasta que la app lo pida explicitamente en runtime y el usuario lo conceda. Declararlo en el manifest (linea 11) es necesario pero no alcanza. Revise todo `MainActivity.kt` (que si implementa `ActivityResultContracts.RequestPermission()` dos veces, para `ACCESS_FINE_LOCATION`, lineas 234-246) y no hay ningun request equivalente para notificaciones -- tampoco en el flujo de onboarding (`OnboardingActivity.kt`, `AliasSetupOnboarding.kt`, sin resultados de busqueda).

`NotificationManager.notify()` en Android con el permiso denegado **no tira excepcion ni error** -- simplemente no muestra nada. No hay ningun `catch` que "trague" este fallo porque no hace falta: la API esta disenada para fallar asi de silenciosa a proposito (evitar que apps hostiles detecten el estado del permiso).

**Impacto concreto en cada camino de notificacion:**
- `postManDownNotification()` (`:784-817`) -- la cuenta regresiva de Man-Down con el boton "ESTOY BIEN . CANCELAR" no aparece. Si hay un `setFullScreenIntent()` (linea 811) que en teoria abre la Activity encima de todo -- pero ese mecanismo tambien depende del canal de notificacion y en muchos fabricantes/OEM de la disposicion de "mostrar sobre otras apps"; sin el permiso base, el comportamiento no esta garantizado.
- `notifyIncomingAlert()` (`:870-883`) -- cuando otro integrante del grupo transmite EMERGENCIA/DETENCION/REAGRUPAMIENTO y la app de quien recibe esta en segundo plano (el escenario para el que este mecanismo se creo explicitamente, ver comentario linea 864-869), la notificacion puede no aparecer nunca. La unica otra via de aviso es `getCallbacks().packetReceived(aprsPacket)` (linea 1873), que tambien es un no-op si la Activity no esta bindeada.
- La confirmacion "Alerta de emergencia enviada" (linea 774-781, ver CRITICO-2) -- tampoco se ve, sumandose al problema de esa notificacion siendo ademas potencialmente falsa.

**Por que esto es CRITICO aca especificamente:** en Android 13+ (que ya es la mayoria del parque activo en 2026, y el proyecto soporta explicitamente equipos "gama baja" nuevos que suelen venir con Android reciente) esto puede significar que **ninguna alerta de seguridad se muestre jamas**, en cualquier telefono donde el usuario no haya ido manualmente a Ajustes -> Apps -> Baqueano -> Notificaciones y las haya habilitado a mano (algo que ningun usuario hace sin que la app se lo pida). No hay ningun indicador en la UI de la app de "las notificaciones estan desactivadas" tampoco.

**Recomendacion:** pedir `POST_NOTIFICATIONS` con `ActivityResultContracts.RequestPermission()` durante el onboarding, mismo patron ya usado para `ACCESS_FINE_LOCATION` en `MainActivity.kt:234-246`. Adicionalmente, chequear `NotificationManagerCompat.areNotificationsEnabled()` al arrancar y mostrar un cartel persistente en la UI si estan desactivadas (no alcanza con pedirlo una vez, el usuario puede revocarlo despues).

---

### ALTO

#### ALTO-1 -- `transmitGroupAlert()` (EMERGENCIA/DETENCION/REAGRUPAMIENTO manual) registra el mensaje como "enviado" en el chat local *antes* de confirmar que hay radio conectada

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/ui/MainActivity.kt:1236-1295`

**Problema:**
```kotlin
private fun transmitGroupAlert(type: AlertHelper.AlertType) {
    ...
    addAlertToChat(type, userAlias, outgoing = true, lat = lat, lon = lon)   // linea 1252 -- SIEMPRE
    ToneHelper.playEmergencyBeep(alertVolume / 100f)
    val service = radioService
    if (service != null && uiState.value.isConnected) {                    // linea 1257 -- recien ACA se chequea
        ...
        service.sendChatMessage("CQ", alertText)                           // linea 1280 -- retorno ignorado
```
`addAlertToChat` se ejecuta **incondicionalmente**, sin importar si `radioService` es `null` o si `uiState.value.isConnected` es `false`. El mensaje queda en el historial de chat local marcado como `isOutgoing = true`, visualmente identico a un mensaje que si salio por RF. El comentario en el codigo (linea 1251, "Siempre registrar la alerta en el chat (con o sin radio)") documenta esto como decision consciente ("modo simulacion" sin radio, ver tambien `sendChatMessage` de chat normal en linea 1202), pero no hay ninguna distincion visual entre "esto se transmitio" y "esto quedo en modo simulacion local" en la UI del chat -- mismo problema en el chat de texto normal (`sendChatMessage`, lineas 1192-1203).

Y aun en el camino donde si hay radio conectada, el retorno de `service.sendChatMessage(...)` (linea 1280, ver tambien CRITICO-2) se ignora -- si `txAX25Packet` lo descarta silenciosamente adentro, nadie en `MainActivity` se entera.

**Impacto:** un usuario que dispara EMERGENCIA sin el radio bindeado/conectado (por ejemplo, apenas despues de abrir la app, antes de que `onServiceConnected` termine el handshake) ve el mensaje aparecer en su propio chat exactamente igual que si hubiera salido por RF. No hay forma de distinguir, mirando la pantalla, una alerta que sirvio de una que no.

**Recomendacion:** diferenciar visualmente en el chat los mensajes "solo local / no transmitido" de los efectivamente enviados por RF (ej. icono de reloj/nube tachada vs. check), y no marcar `isOutgoing` como enviado hasta tener confirmacion de `sendChatMessage`.

---

#### ALTO-2 -- `FallDetectionManager.start()` no hace nada si el telefono no tiene el sensor necesario, sin avisar -- el toggle de Man-Down puede quedar "activado" en la UI sin monitorear nunca

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/ui/FallDetectionManager.kt:38-42`

```kotlin
fun start() {
    accelerometer?.let {
        sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
    }
}
```
Si `sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)` (linea 25) devuelve `null` -- algo real en el hardware Android 7 gama baja que el proyecto soporta explicitamente desde el bajon de `minSdk` a 24 (ver `AUDITORIA_SEGURIDAD_APP.md`, comentarios sobre "equipos viejos", y el commit `ee56894` "soporte Android 7 (Huawei P9)") -- el `?.let { }` no hace absolutamente nada. No hay `Log`, no hay callback, no hay excepcion.

`RadioAudioService.setManDownEnabled(true)` (`RadioAudioService.java:701-708`) llama a `fallDetectionManager.start()` sin mirar ningun resultado (el metodo es `void`), y el switch de Man-Down en Ajustes (`SettingsActivity.java:299`, `AliasSettingScreen.kt:231`) queda marcado "activado" en base al `AppSetting` guardado, **no** en base a si el sensor efectivamente existe.

**Impacto:** en un telefono sin `TYPE_LINEAR_ACCELERATION` (o sin ningun acelerometro utilizable), el usuario activa el switch, la UI le confirma "Man-Down: activado", y la funcion **nunca va a disparar una alerta ante una caida real** -- sin ningun error, sin ningun indicio, ni en ese momento ni nunca.

**Recomendacion:** que `start()` devuelva `Boolean` (mismo patron que `startPtt()` tras el fix de `bad712c`), y que `setManDownEnabled()`/la UI de Ajustes muestren un aviso explicito ("Este equipo no tiene el sensor necesario para Man-Down") cuando el sensor no esta disponible, en vez de dejar el switch en verde sin que funcione nada detras.

---

#### ALTO-3 -- `WifiTransport`: cualquier frame de TX (audio, PTT, alerta) se descarta en silencio si el socket UDP esta cerrado o en medio de una reconexion

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/radio/WifiTransport.java:70-83`

```java
public final Protocol.FrameWriter frameWriter = (frame, frameSize) -> {
    DatagramSocket s   = socket;
    InetAddress    addr = espAddress;
    if (s == null || addr == null || s.isClosed()) return;   // linea 73 -- descarte total, silencioso
    sendExecutor.execute(() -> {
        try {
            s.send(new DatagramPacket(frame, frameSize, addr, UDP_PORT));
        } catch (IOException e) {
            Log.w(TAG, "UDP send error", e);                  // linea 80 -- tampoco sube a ningun lado
        }
    });
};
```
Este es el punto final por donde pasa *todo* lo que la app transmite hacia el equipo por WiFi -- audio de PTT, balizas APRS, mensajes de chat, y las alertas de EMERGENCIA/Man-Down. El comentario de la clase (lineas 25-30) documenta que esto es deliberado ("Flow control: disabled -- UDP has no backpressure"), lo cual es razonable para audio en tiempo real (perder un frame de audio de PTT no vale la pena reintentar), pero el mismo mecanismo fire-and-forget se usa para los paquetes AX.25 de las alertas de seguridad, que si se pierden aca, se pierden del todo -- sin reintento, sin aviso hacia `RadioAudioService`, y por lo tanto sin ninguna chance de que el CRITICO-2 de arriba llegue siquiera a enterarse de que hubo un intento de envio fallido en esta capa.

**Impacto:** en el instante exacto en que el socket esta cerrandose (reconexion en curso, WiFi momentaneamente inestable -- comun en un vehiculo en movimiento, exactamente el contexto de uso real de este proyecto), cualquier intento de TX en esa ventana desaparece sin dejar rastro mas alla de un `Log.w` que nadie va a ver jamas en el campo.

**Recomendacion:** que `frameWriter` devuelva o reporte de alguna forma (callback, contador expuesto) cuando un frame se descarto por socket cerrado, para que capas superiores (en particular `txAX25Packet`/`sendChatMessage`) puedan enterarse y no asumir exito solo porque la llamada no tiro excepcion.

---

#### ALTO-4 -- Sin heartbeat/verificacion de vida despues del handshake: un ESP32 trabado (firmware colgado, WiFi/AP vivo) puede quedar "conectado" indefinidamente en la UI sin transmitir ni recibir nada

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java` -- busqueda global de heartbeat/watchdog/staleness sobre todo el archivo: sin resultados.

**Problema:** el handshake HELLO (`startProtocolHandshake()`, lineas 1367-1374) corre **una sola vez** al conectar. Una vez que `radioConnected()` (linea 1356-1365) marca `isConnected = true` en la UI, no hay ningun mecanismo posterior que vuelva a confirmar que el ESP32 sigue respondiendo -- ni un ping periodico, ni un timeout sobre la llegada de `COMMAND_DEVICE_STATE` (que si llega regularmente via polling de RSSI/squelch, `handleDeviceState()`, lineas 1750-1772, pero nadie mide cuanto hace que no llega uno). `isConnectionReady()` (lineas 1150-1156) sigue devolviendo `true` mientras el socket UDP siga tecnicamente abierto, sin importar si hay trafico real.

Un firmware ESP32 que se cuelga (deadlock de tarea, watchdog interno que no dispara, o simplemente deja de procesar comandos) pero mantiene el SoftAP WiFi activo -- un modo de falla plausible de firmware embebido, y distinto del caso ya cubierto en CRITICO-1 (que es sobre el handshake fallando explicitamente) -- deja a la app sin ninguna senal de que algo esta mal. El LED de `AppStatusBar` sigue en verde.

**Impacto:** el usuario ve "CONECTADO", puede intentar un PTT o una alerta, y el envio via `WifiTransport.frameWriter` (ver ALTO-3) va a "salir" exitosamente desde el punto de vista de UDP (el socket sigue abierto, `send()` no tira excepcion aunque nadie del otro lado este escuchando -- UDP no tiene ack de entrega a nivel de sistema operativo) sin que la app tenga forma de saber que el ESP32 nunca lo proceso.

**Recomendacion:** agregar un heartbeat periodico liviano (ping/pong KISS, o medir tiempo desde el ultimo `COMMAND_DEVICE_STATE`/`COMMAND_HELLO` recibido) y, si se supera un umbral razonable (ej. 2-3x el intervalo esperado de polling), tratar la conexion como perdida y disparar el mismo camino que `radioMissing()`.

---

### MEDIO

#### MEDIO-1 -- Reseed de canales Argentina: si falla en el primer arranque, el usuario queda con la lista de canales vacia sin ningun aviso

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/data/AppDatabase.java:111-150`

El metodo completo (lectura del flag de precarga, transaccion de delete+insert de `ChannelMemory`, guardado del flag) esta envuelto en un unico `catch (Exception e) { Log.e(TAG, "Error al precargar canales Argentina.", e); }` (lineas 147-149), corriendo en un `Executors.newSingleThreadExecutor()` fire-and-forget (`preloadArgentinaChannelsIfNeeded`, lineas 97-99) disparado desde `getInstance()` la primera vez que se abre la base. Si la transaccion falla (disco lleno, DB corrupta, etc.) en el primerisimo arranque de un usuario nuevo, la tabla `ChannelMemory` queda vacia y **no hay ningun cartel, toast, ni indicio en la UI** de que los canales predefinidos (los que garantizan frecuencias dentro de la whitelist Res. 5/2015) no se cargaron. El usuario ve una lista de canales vacia y no tiene forma de saber si es un bug o si tiene que cargarlos a mano.

**Recomendacion:** exponer el resultado del seed (exito/fallo) hacia la UI de alguna forma -- aunque sea un `Log` no alcanza en un dispositivo sin conexion a internet donde nadie va a revisar logcat.

#### MEDIO-2 -- `MainViewModel.loadDataAsync` / `deleteMemoryAsync` no tienen ningun manejo de excepciones -- un fallo de DB deja el callback sin ejecutarse, sin aviso

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/ui/MainViewModel.java:69-104`

```java
public void loadDataAsync(Runnable callback) {
    databaseExecutor.execute(() -> {
        loadData();       // sin try/catch
        callback.run();   // nunca se ejecuta si loadData() tira excepcion
    });
}
```
Si `channelMemoryDao().getAll()` o `aprsMessageDao().getAll()` (dentro de `loadData()`, lineas 56-67) tiran una excepcion (ej. `SQLiteException` por corrupcion o espacio), el `callback` que depende de "ya termine de cargar" nunca se ejecuta, y no hay ningun log ni senal -- la excepcion queda en el hilo del `ExecutorService` sin mas destino que el `UncaughtExceptionHandler` por defecto del sistema (stdout, invisible en produccion). Mismo patron en `deleteMemoryAsync` (lineas 99-104).

**Recomendacion:** envolver ambos en try/catch y, como minimo, loguear con contexto; considerar invocar el callback igual con un flag de error para que la UI pueda salir de un estado de carga infinita.

#### MEDIO-3 -- `startMovementUpdates()`/`stopMovementUpdates()` en `MainActivity` tragan cualquier excepcion sin loguear nada -- degrada SmartBeaconing sin dejar rastro

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/ui/MainActivity.kt:804-825`

```kotlin
try {
    lm.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 1000L, 2f, movementListener)
} catch (e: Exception) { /* GPS no disponible */ }   // linea 817 -- ni Log.w
...
try { lm.removeUpdates(movementListener) } catch (e: Exception) { }   // linea 824 -- idem
```
Sin ni un `Log.w`. Si esto falla (proveedor GPS deshabilitado por el usuario, o inexistente en el dispositivo), `movementListener` nunca recibe actualizaciones de velocidad/rumbo, y `GpsBeaconManager`/`SmartBeaconing` (`GpsBeaconManager.kt:16-19`) quedan con `MovementState()` por defecto (velocidad 0, sin rumbo) para siempre -- el modo SmartBeaconing deja de adaptar el intervalo de balizado a la velocidad real y cae en un intervalo fijo subóptimo, silenciosamente. No es la perdida total de la baliza (el balizado fijo de `RadioAudioService.startBeaconScheduler()` es independiente y sigue funcionando), pero es una degradacion real de una feature de seguridad de ruta sin ningun rastro, ni siquiera en logcat.

**Recomendacion:** al menos un `Log.w(TAG, "No se pudo iniciar actualizaciones de movimiento para SmartBeaconing", e)`.

#### MEDIO-4 -- `saveRoutePoint()` escribe a Room desde un `Executor` sin ningun try/catch -- un punto de ruta/waypoint se puede perder sin que nadie se entere

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/ui/MainActivity.kt:835-854`

```kotlin
executor.execute {
    RadioServiceAccessor.getAppDb(viewModel).routePointDao().insert(point)   // linea 852, sin try/catch
}
```
Si `insert()` tira una excepcion, ese punto de ruta desaparece silenciosamente del historial persistido (aunque ya este en el `_routePoints` en memoria via `_routePoints.update` en linea 850, asi que la sesion actual no se ve afectada -- el impacto real es que el historial de ruta guardado en Room queda incompleto sin que quede ningun registro de por que).

**Recomendacion:** try/catch con `Log.w` como minimo; bajo impacto real dado que no es una funcion de seguridad en tiempo real, pero barato de agregar.

---

### BAJO

#### BAJO-1 -- Callbacks de `RadioAudioServiceCallbacks` sin implementar mas alla de los ya cubiertos arriba

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:265-293` (interfaz) vs. `KV4PHT/app/src/main/java/ar/motorfar/app/ui/MainActivity.kt:291-326` (implementacion)

Ademas de `unknownLocation`, `chatError`, `missingFirmware`, `outdatedFirmware`, `radioModuleNotFound` (ya cubiertos en CRITICO-1/2), tampoco estan implementados: `hideSnackBar`, `radioModuleHandshake`, `audioTrackCreated`, `startingAprsBeacon`, `scannedToMemory`, `initialDeviceStateReceived`, `sentAprsBeacon`, `forcedPttStart`, `forcedPttEnd`, `setRadioType`, `showNotification`. La mayoria de estos son informativos de bajo impacto de seguridad (`audioTrackCreated`, `scannedToMemory`), pero `sentAprsBeacon` (la unica confirmacion *real* de que una baliza de posicion efectivamente se transmitio, con lat/long/frecuencia reales, a diferencia de la notificacion optimista de CRITICO-2) tampoco tiene ningun consumidor en la UI -- es la pieza que ya existe en el diseno para resolver CRITICO-2 correctamente y no esta conectada.

**Recomendacion:** al menos conectar `sentAprsBeacon` como la fuente real de verdad para cualquier futura confirmacion visual de "posicion enviada", en vez de la notificacion optimista actual.

---

## Verificado OK (para que quede constancia)

1. **`AppDatabase` -- migraciones sin `fallbackToDestructiveMigration`** (`AppDatabase.java:88-90`) -- si falta una migracion, tira `IllegalStateException` explicita en vez de borrar datos silenciosamente. Correcto, ya senalado tambien en `AUDITORIA_SEGURIDAD_APP.md`.
2. **`WifiTransport.close()`** (`WifiTransport.java:143-158`) -- el unico `catch (Exception ignored) {}` aca (linea 156) es al desregistrar el `NetworkCallback`, que puede tirar si ya estaba desregistrado; no oculta ningun fallo real de transmision, es limpieza de mejor esfuerzo durante un cierre que de por si ya esta pasando. Verificado OK.
3. **`RadioAudioService.tryToStopRadioModule()`** (`:636-648`) y **`closePortAndReset()`** (`:1158-1187`) -- los `catch (Exception ignored)` aca son durante teardown/shutdown explicito del propio Service o de una reconexion intencional; no esconden un fallo que el usuario necesite conocer en el momento, son limpieza de recursos que de todas formas se estan cerrando. Verificado OK.
4. **`PositionParser`/`Parser.java`/parser APRS en general** -- los `catch (Exception e)` amplios en `handleAx25Packet()` (`RadioAudioService.java:1832-1876`) descartan paquetes entrantes malformados sin crashear. Ya cubierto en profundidad en `AUDITORIA_SEGURIDAD_APP.md` (MEDIO-2); correcto que sea silencioso en RX (es trafico de terceros en un canal compartido, un paquete corrupto de otro handheld no es un evento que el usuario necesite ver).
5. **`manDownWakeLock`** (`:751`, `762`, `771`) -- tiene timeout de contencion de 60s explicito documentado; peor caso es CPU despierta de mas, no indefinidamente. Ya verificado en la auditoria de seguridad previa, sin cambios desde entonces.

---

## Resumen de acciones recomendadas (por prioridad de consecuencia real)

1. **(CRITICO-1)** Llamar a `radioMissing()` (o equivalente) en los 3 caminos de fallo de handshake post-conexion (`RadioAudioService.java:1384, 1417/1427, 1435`) e implementar `missingFirmware()`/`outdatedFirmware()`/`radioModuleNotFound()` en `MainActivity.kt` -- hoy son no-ops silenciosos y el radio puede quedar "conectado" en pantalla sin funcionar, sin reintento automatico.
2. **(CRITICO-2)** No mostrar "alerta enviada" en `fireManDownAlert()` (`RadioAudioService.java:774-781`) sin confirmacion real de envio; implementar `unknownLocation()` y `chatError()` en la UI.
3. **(CRITICO-3)** Pedir `POST_NOTIFICATIONS` en runtime durante onboarding y verificar `areNotificationsEnabled()` con aviso persistente si esta desactivado.
4. **(ALTO-1 a ALTO-4)** Diferenciar visualmente mensajes "solo local" de "transmitidos" en el chat; que `FallDetectionManager.start()` reporte si el sensor no existe; que `WifiTransport.frameWriter` reporte descartes; agregar heartbeat post-handshake.
5. **(MEDIO-1 a MEDIO-4)** Try/catch con logging minimo y/o senal a UI en los 4 puntos de DB/GPS listados -- bajo costo, cierran deuda de robustez real aunque de menor frecuencia de disparo.
6. **(BAJO-1)** Conectar `sentAprsBeacon` como fuente de verdad para una futura confirmacion visual correcta de "posicion enviada".

Ningun hallazgo de esta lista requiere parar el proyecto, pero CRITICO-1, 2 y 3 son baratos de arreglar (en su mayoria son "conectar un callback que ya existe" o "agregar un chequeo de resultado que ya se hizo una vez para `startPtt()`") y cierran exactamente la clase de bug que motivo esta auditoria.
