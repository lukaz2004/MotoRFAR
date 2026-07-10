# Auditoría de confiabilidad — Subsistema de alertas de emergencia (Baqueano)

**Fecha:** 2026-07-09
**Alcance:** `KV4PHT/app/` — ciclo de vida completo de Man-Down, EMERGENCIA, DETENCIÓN y REAGRUPAMIENTO, del lado emisor y receptor. Auditoría estática de confiabilidad/diseño de seguridad ("¿esto llega a un humano que pueda actuar, y el emisor se entera de si funcionó?"), no es una auditoría de seguridad clásica. Solo lectura — no se modificó código.
**Metodología:** trazado manual del flujo completo (sensor → countdown → Service → transporte RF → recepción → notificación) en `FallDetectionManager.kt`, `RadioAudioService.java`, `MainActivity.kt`, `AlertHelper.java`, `ArgentinaChannels.java`, `ToneHelper.java`, `RadioModuleController.java`, `TermsScreen.kt`, `AliasSettingScreen.kt`. Búsqueda de todos los call sites de las funciones clave para confirmar código muerto o guards ausentes.

**Punto de partida (dado, no re-verificado en esta auditoría):** `RadioAudioService.transmitEmergencyAlert()` (`RadioAudioService.java:847-862`) fuerza el cambio a 140.970 MHz para transmitir Man-Down/EMERGENCIA, pero no existe ningún camino en la UI que arranque `RadioMode.SCAN` desde cero — el grupo nunca está realmente escuchando esa frecuencia salvo coincidencia. Esta auditoría parte de ese hallazgo confirmado y busca variantes de la misma familia de bug en el resto del subsistema de alertas.

---

## Resumen ejecutivo

El bug de origen (nadie escucha 140.970) no es un caso aislado — es un síntoma de un patrón más amplio: **en absolutamente ningún punto de la cadena de alertas (Man-Down automático ni botones manuales) el emisor recibe una confirmación que dependa del resultado real de la transmisión.** El caso más grave es Man-Down: la notificación "✅ Alerta de emergencia enviada — Se transmitió tu posición por 140.970 MHz" se dispara **incondicionalmente**, sin importar si el radio estaba conectado, si el GPS tuvo fix, o si el paquete efectivamente salió por RF. Un usuario que use la app solo con el teléfono (sin el equipo ESP32/SA818 emparejado) y sufra una caída real recibe la misma confirmación tranquilizadora de "listo, ya avisé" que alguien cuya alerta sí salió — con la diferencia de que en el primer caso nadie se entera nunca.

El segundo hallazgo más severo es operacional, no de código: `onTaskRemoved()` apaga el `Service` completo apenas el usuario desliza la app fuera de "recientes" — un gesto habitual y no malicioso. Esto no solo desactiva el envío de Man-Down (ya trackeado en `PENDIENTES.md`), sino también **la capacidad de recibir la alerta de cualquier otro integrante del grupo, y la detección de caída del propio acelerómetro** — las tres cosas mueren juntas, sin ningún aviso al usuario de que pasó, y sin ninguna mitigación de plataforma (no hay pedido de exención de optimización de batería en todo el código).

No hay nada CRÍTICO en el sentido de "dato filtrado" o "RCE" — es un tipo de hallazgo distinto: **falsos positivos de confirmación en una feature de rescate de vida**, que es funcionalmente peor que un fallo silencioso sin confirmación, porque activamente desalienta al usuario a buscar ayuda por otra vía.

---

## Hallazgos

### CRÍTICO

#### CRÍTICO-1 — Man-Down muestra "alerta enviada" sin importar si algo se transmitió realmente

**Archivos:** `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:768-782` (`fireManDownAlert`), `:846-862` (`transmitEmergencyAlert`), `:1884-1927` (`sendPositionBeacon`), `:2048-2065` (`txAX25Packet`), `:288` (`unknownLocation` default no-op).

**Problema:** `fireManDownAlert()` es fire-and-forget respecto del resultado real de la transmisión:

```java
private void fireManDownAlert() {
    ...
    transmitEmergencyAlert();                 // async, sin valor de retorno
    NotificationCompat.Builder sentNotif = ...
        .setContentTitle("⚠ Alerta de emergencia enviada")
        .setContentText("Se transmitió tu posición por 140.970 MHz (canal Emergencia)")
    nm.notify(MANDOWN_NOTIFICATION_ID, sentNotif.build());   // se muestra SIEMPRE
}
```

`transmitEmergencyAlert()` no chequea `isConnectionReady()`/`isRadioConnected()` antes de arrancar la secuencia. Cada eslabón de la cadena que sí valida el estado real falla **en silencio**, solo con `Log.d`/`Log.e`, sin propagar nada de vuelta a `fireManDownAlert()`:

- `sendPositionBeacon()` línea 1890-1893: `if (!isRadioConnected() || !isTxAllowed()) { Log.d(...); return; }` — sin equipo ESP32/SA818 emparejado (celular solo) o con el WiFi del equipo caído en ese instante, esto corta acá.
- Si falla el permiso de ubicación o Google Play Services, se llama `getCallbacks().unknownLocation()` (líneas 1908, 1914, 1924, 1926) — pero `unknownLocation()` es `default void unknownLocation() {}` (línea 288) y **no tiene ninguna implementación override en todo el código** (confirmado por búsqueda global) — un no-op total.
- Incluso si el radio está conectado y hay fix GPS, `sendChatMessage()` → `txAX25Packet()` línea 2057-2059: `if (hostToEsp32 == null) { Log.e(...); return; }` — mismo patrón, silencioso.

**Escenario real:** usuario prueba la app en el teléfono antes de comprar/emparejar el equipo de radio (o el equipo se queda sin batería, o el WiFi del SoftAP tiene un hiccup momentáneo — plausible en movimiento). `FallDetectionManager` funciona con el acelerómetro del teléfono, **totalmente independiente del hardware de radio** — el countdown arranca igual, llega a cero, `fireManDownAlert()` corre, no sale ni un bit por RF, y el usuario (o quien mire su teléfono después) ve "✅ Alerta de emergencia enviada — Se transmitió tu posición" en la notificación y en cualquier lugar donde se refleje ese estado. Es la peor combinación posible: cero ayuda en camino, y una señal activa de que sí la hay, que desalienta buscarla por otra vía (llamar a alguien, caminar a buscar señal, etc.).

**Por qué es CRÍTICO y no ALTO:** a diferencia de un fallo silencioso sin feedback (que ya sería malo), acá hay una **afirmación positiva y específica de éxito** ("140.970 MHz", el ícono de check) que es falsa. El overlay en pantalla (`manDownCountdownTick(null)` en `MainActivity.kt:294-296`) tampoco distingue éxito de fracaso — simplemente hace desaparecer la cuenta regresiva sin decir nada del resultado.

**Recomendación:** hacer que `transmitEmergencyAlert()` devuelva o reporte (via callback) si `isRadioConnected()`/`isTxAllowed()` eran verdaderos al momento de intentar, y que `fireManDownAlert()` muestre una notificación distinta ("⚠ No se pudo transmitir — sin conexión al equipo, reintentando" o similar) cuando el intento falla en el primer chequeo. Como mínimo, agregar reintentos cuando `wifiTransport`/`hostToEsp32` vuelvan a estar disponibles después de un fallo — hoy no hay ningún retry, el intento fallido no deja rastro que dispare un segundo intento.

---

#### CRÍTICO-2 — `onTaskRemoved()` apaga emisión, recepción Y detección de caída juntas, sin aviso ni mitigación

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:650-654`

```java
@Override
public void onTaskRemoved(Intent rootIntent) {
    super.onTaskRemoved(rootIntent);
    stopSelf();
}
```

**Qué mata exactamente "apagar" el Service** (ya apuntado como pendiente en `PENDIENTES.md:35-36`, pero sin cuantificar el alcance real):
- `fallDetectionManager` vive en el `Service` (`RadioAudioService.java:158,475-478`) — al morir el Service, el listener del acelerómetro se desregistra. **No solo deja de poder avisar de una caída propia — deja de poder DETECTARLA.**
- `handleAx25Packet()` → `notifyIncomingAlert()` (líneas 870-883, 1855-1868) es el único camino de recepción de alertas de otros integrantes del grupo, y corre exclusivamente dentro del `Service`. Con el `Service` muerto, **tampoco se reciben las alertas de nadie más**, aunque el radio siga físicamente prendido y en rango.
- La conexión WiFi/USB al equipo (`wifiTransport`, `hostToEsp32`) se cierra con el resto del teardown del Service.

Es decir: deslizar la app fuera de "recientes" (gesto común, no malicioso — muchos usuarios lo hacen por costumbre de "limpiar" apps, y algunos OEMs lo hacen automáticamente por agresividad de gestión de batería) apaga **las tres patas del sistema de seguridad a la vez**, sin que el usuario reciba ningún aviso proactivo de que pasó. La notificación foreground del `Service` (que sería la única pista visual) directamente desaparece junto con el resto — no queda ni un indicio pasivo.

**Sin mitigación de plataforma:** búsqueda global de `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`/`isIgnoringBatteryOptimizations` en todo `KV4PHT/app/src/main/java` → sin resultados. La app nunca pide al usuario la exención de optimización de batería que reduciría (aunque no eliminaría del todo) la frecuencia con la que Android mata procesos en segundo plano en fabricantes agresivos (Xiaomi, Huawei, Samsung con "app hibernation", etc.) — y tampoco mitiga el swipe-away explícito, que es una acción directa del usuario, no del OS.

**Escenario real:** rider arranca Baqueano al salir, la usa un rato, después la desliza de "recientes" sin apagarla de verdad (pensando que sigue corriendo en segundo plano, como la mayoría de las apps) — o el teléfono lo hace por él vía gestión agresiva de batería. A partir de ahí, ni Man-Down detecta nada, ni el teléfono recibe alertas de sus compañeros, indefinidamente, hasta que alguien vuelva a abrir la app.

**Recomendación:**
- Pedir `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` durante el onboarding (con explicación clara del porqué, ya que es un permiso que Play Store trata con cautela — justificable acá por ser una app de seguridad de vida).
- Mostrar una notificación persistente y explícita cuando se detecta que el `Service` se apagó por `onTaskRemoved` mientras Man-Down estaba activo — hoy no hay ninguna.
- Evaluar `START_STICKY` + lógica de auto-relanzamiento explícita en vez de `stopSelf()` incondicional, si el objetivo de producto es que Man-Down sobreviva al swipe (hoy es una decisión de producto pendiente según `PENDIENTES.md`, esta auditoría solo cuantifica el alcance).

---

### ALTO

#### ALTO-1 — Botones manuales EMERGENCIA/DETENCIÓN/REAGRUPAMIENTO no confirman ni desmienten el envío — `getSentConfirmation()` existe pero está muerto

**Archivos:** `KV4PHT/app/src/main/java/ar/motorfar/app/ui/MainActivity.kt:1237-1295` (`transmitGroupAlert`), `KV4PHT/app/src/main/java/ar/motorfar/app/ui/AlertHelper.java:95-102` (`getSentConfirmation`).

**Problema:** `transmitGroupAlert()` solo intenta transmitir por radio si `service != null && uiState.value.isConnected` (línea 1257). Si no hay radio conectada, el flujo entero salta directo a "guardar en el chat local" (línea 1252, siempre se ejecuta) y termina ahí — sin ningún toast, notificación o indicación de que **la alerta nunca salió por RF**, solo quedó guardada localmente en el teléfono del emisor. El único indicio es el LED "SIN RADIO" de `AppStatusBar.kt` en la pantalla principal, que es pasivo y fácil de no registrar en el momento de tocar un botón de emergencia.

`AlertHelper.getSentConfirmation(AlertType)` (líneas 95-102) existe específicamente para este propósito — devuelve textos como "Alerta de emergencia enviada" — pero una búsqueda global confirma que **no tiene ningún call site en toda la base de código** fuera de su propia definición. Está escrito pero nunca cableado a la UI.

**Por qué es ALTO y no CRÍTICO:** a diferencia de Man-Down (CRÍTICO-1), acá no hay una afirmación *falsa* de éxito — simplemente no hay ninguna afirmación. Es un silencio en vez de una mentira, pero para un botón que un usuario presiona conscientemente esperando que "algo pase", el silencio total también es un fallo de confiabilidad real: el usuario no tiene forma de saber si debe buscar ayuda por otro medio.

**Recomendación:** cablear `getSentConfirmation()`/una alternativa de "no se pudo transmitir — sin conexión al equipo" a un `Toast`/Snackbar en `transmitGroupAlert()`, condicionado al mismo chequeo `service != null && uiState.value.isConnected` que ya existe en el código.

---

#### ALTO-2 — Solo EMERGENCIA fuerza el canal no-monitoreado; el texto de confirmación afirma monitoreo que no existe

**Archivos:** `KV4PHT/app/src/main/java/ar/motorfar/app/ui/AlertHelper.java:64-68` (`getConfirmationText`), `:88-93` (`getTargetFrequency`), `KV4PHT/app/src/main/java/ar/motorfar/app/ui/MainActivity.kt:1260-1275` (`transmitGroupAlert`).

**Problema:** confirmado por código — `getTargetFrequency()` solo fuerza 140.970 MHz para `AlertType.EMERGENCY`; STOP y REGROUP se transmiten en el canal de grupo/alternativo que el usuario ya tenía activo (`return currentFreq;`, línea 92). El comentario en `MainActivity.kt:1238-1240` lo confirma explícitamente como decisión de diseño intencional del 2026-07-06.

Esto significa que, dado el bug de origen (nadie escucha 140.970 por defecto), **la alerta menos urgente (REAGRUPAMIENTO) tiene más probabilidad real de ser escuchada por el grupo que la más urgente (EMERGENCIA/Man-Down)** — porque STOP/REGROUP se quedan en el canal donde el grupo efectivamente está, mientras que EMERGENCIA se va a un canal donde, por diseño de la restricción de chat libre agregada el 2026-07-06, nadie tiene motivo para estar parado.

Agrava esto que `AlertHelper.getConfirmationText(AlertType.EMERGENCY)` (líneas 64-68), el texto que el usuario lee y confirma antes de disparar la alerta, dice explícitamente:

> "Se transmitirá por 140.970 MHz — canal de emergencias M.T.T.T., **monitoreado por otros grupos y entidades en la zona.**"

Esa afirmación de monitoreo no tiene ningún respaldo en el código — no hay ningún mecanismo (ni de este proyecto ni conocido de terceros) que garantice que alguien esté efectivamente escuchando 140.970 en un momento dado. El texto le da al usuario una falsa sensación de que presionar EMERGENCIA es más seguro que STOP/REGROUP, cuando en la práctica actual es al revés.

**Recomendación:** no es un problema de código nuevo — es la misma raíz del hallazgo de origen, pero con una manifestación concreta y accionable: corregir el copy de `getConfirmationText(EMERGENCY)` para no afirmar monitoreo que no está garantizado, y priorizar la resolución del bug de origen (algún mecanismo de multi-canal/SCAN real) antes que cualquier otra feature nueva, dado que hoy el botón más urgente de la app es el menos confiable de los tres.

---

#### ALTO-3 — Alerta entrante a otros integrantes usa notificación estándar; la propia caída usa audio de alarma exclusivo — asimetría que debilita justo la señal que necesita ser más fuerte

**Archivos:** `KV4PHT/app/src/main/java/ar/motorfar/app/radio/RadioAudioService.java:467-472` (canal `ALERT_NOTIFICATION_CHANNEL_ID`), `:870-883` (`notifyIncomingAlert`), vs. `:819-838` (`requestManDownAudioFocus`/`releaseManDownAudioFocus`, `AudioAttributes.USAGE_ALARM`) y `KV4PHT/app/src/main/java/ar/motorfar/app/ui/ToneHelper.java:39-43,93-97` (`playCountdownBeep`, también `USAGE_ALARM` + `FLAG_AUDIBILITY_ENFORCED`).

**Problema:** el canal de notificaciones de alerta se crea así:

```java
NotificationChannel alertChan = new NotificationChannel(
        ALERT_NOTIFICATION_CHANNEL_ID, "Alertas de seguridad",
        NotificationManager.IMPORTANCE_HIGH);
alertChan.setDescription(...);
nm.createNotificationChannel(alertChan);   // sin setSound(), sin setBypassDnd(true)
```

`IMPORTANCE_HIGH` da heads-up + sonido/vibración por defecto del sistema, pero **no** es audio de alarma (no usa `AudioAttributes.USAGE_ALARM`, no fuerza volumen, no tiene `setBypassDnd(true)`) — es indistinguible en robustez de cualquier notificación de mensajería común. En cambio, la cuenta regresiva de la propia caída (`ToneHelper.playCountdownBeep`) sí usa `USAGE_ALARM` + `FLAG_AUDIBILITY_ENFORCED` + `AudioFocusRequest` exclusivo (`AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE`), diseñado específicamente para "notarse siempre" (comentario propio en línea 735-736 del código).

**Por qué importa:** la persona que se cayó ya sabe que se cayó — el audio de alarma ahí es para que cancele a tiempo si fue falsa alarma. La persona que **tiene que enterarse para ir a ayudar** es la que recibe una notificación estándar, que:
- Puede estar en Modo No Molestar (plausible viajando/durmiendo en un alto del grupo) — nada en el código pide `ACCESS_NOTIFICATION_POLICY` ni marca el canal para saltarse el DND, así que el sistema la silenciaría sin avisar a la app.
- Compite con ruido de viento/casco en moto, sonido de motor, teléfono en un tank bag — un chime de notificación estándar es mucho más fácil de perder que un tono de alarma forzado a volumen audible.

**Recomendación:** usar el mismo patrón de `USAGE_ALARM`/audio focus exclusivo para `notifyIncomingAlert()` que ya existe para el countdown local, y evaluar pedir acceso a política de notificaciones (`NotificationManager.isNotificationPolicyAccessGranted()` + `setBypassDnd(true)` en el canal) específicamente para el canal de alertas de seguridad — con el disclaimer correspondiente al usuario de por qué se pide ese permiso sensible.

---

### MEDIO

#### MEDIO-1 — Ninguna comunicación explícita de que Man-Down solo sirve si hay alguien en rango escuchando esa frecuencia en ese momento — riesgo particular para el uso solitario que el proyecto dice soportar

**Archivos:** `KV4PHT/app/src/main/java/ar/motorfar/app/ui/onboarding/TermsScreen.kt:88-91` (sección 5, exención de responsabilidad genérica), `KV4PHT/app/src/main/java/ar/motorfar/app/ui/compose/AliasSettingScreen.kt:221` (copy del toggle Man-Down).

**Problema:** `CLAUDE.md` es explícito en que Baqueano es "para cualquier usuario en cualquier medio de transporte o a pie... que necesite comunicarse en zona sin red de datos" — no asume grupo. El copy de Man-Down (`AliasSettingScreen.kt:221`, ya corregido en la ronda de seguridad del 2026-07-06 para aclarar que la transmisión es sin cifrar) sigue sin decir que la alerta **solo tiene efecto si hay alguien en rango de radio Y sintonizado en ese canal en ese momento exacto** — para un usuario solo, sin grupo cerca, Man-Down no tiene a quién avisarle, punto que no está comunicado en ningún lugar del flujo de activación del toggle.

`TermsScreen.kt` (sección 5, líneas 88-91) tiene un disclaimer genérico de "as-is, sin garantías de funcionamiento, cobertura ni recepción" — cubre el caso legalmente, pero es texto legal genérico leído una sola vez al instalar la app (patrón "aceptar sin leer" estándar), no una advertencia específica en el momento en que el usuario activa el toggle de Man-Down pensando en su propia seguridad.

**Recomendación:** agregar una línea específica al copy de `AliasSettingScreen.kt` (o un diálogo la primera vez que se activa el toggle) del tipo: "Solo funciona si hay alguien de tu grupo (u otro usuario Baqueano) dentro de alcance de radio y escuchando el canal en ese momento — no reemplaza un plan de rescate si viajás solo." Cambio de copy, no de código — mismo patrón que ya se usó para la aclaración de "sin cifrar" agregada el 2026-07-06.

---

## Verificado OK (para que quede constancia — no repetir análisis en próximas rondas)

1. **CTCSS/squelch del canal Emergencia — correcto, sin bug.** `ArgentinaChannels.java:33` define `EMERGENCIA` con tono `null` a propósito (comentario línea 24). Tanto `tuneToFreq()` (usado por el auto-switch de Man-Down/EMERGENCIA, `RadioAudioService.java:922-923`) como `tuneToMemory()` vía `ToneHelper.getToneIndex(null)` (`ToneHelper.java:197-207`, retorna `-1` de forma segura, sin NPE) fuerzan tono 0 (sin CTCSS) al sintonizar Emergencia por cualquiera de los dos caminos. No hay forma de que quede un tono CTCSS "pegado" de otro canal al pasar a Emergencia.

2. **Squelch — no hay control de usuario que pueda dejarlo "demasiado agresivo".** El squelch está hardcodeado a `0` (abierto) tanto en el estado inicial como en el de reset de `RadioModuleController.java:61,339`, y solo se sube transitoriamente a `1` durante el avance activo de un `SCAN` en curso (`RadioAudioService.java:1622-1626`), restaurándose después (líneas 1556-1559). El slider de squelch en `SettingsActivity.java:212,252` es código muerto — confirmado por búsqueda global que ningún otro archivo lanza `SettingsActivity` — no es alcanzable desde la navegación Compose actual, así que no hay forma de que un usuario deje un squelch alto guardado que se coma la alerta.

3. **Frescura del GPS en el paquete transmitido — correcto.** Tanto Man-Down (`transmitEmergencyAlert()` → `sendPositionBeacon()`) como los botones manuales (`transmitGroupAlert()` → `service.sendPositionBeacon()`, `MainActivity.kt:1277`) usan el mismo camino: `FusedLocationProviderClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, token)` (`RadioAudioService.java:1917-1926`) — un fix fresco solicitado en el momento, no el último conocido cacheado. El filtro `isUsableLocation()` (0,0) que se agregó esta sesión solo afecta a `MainActivity.getLastKnownLocation()`, que alimenta exclusivamente la posición cosmética mostrada en el chat local (`addAlertToChat`, líneas 1246-1252) — nunca lo que efectivamente sale por RF. Sin regresión de staleness introducida por ese filtro.

---

## Recomendaciones — resumen por prioridad

1. **(CRÍTICO-1)** Dejar de mostrar "alerta enviada" en Man-Down quando la transmisión falló en cualquier eslabón (sin radio, sin GPS, sin TX permitido) — hoy es un fire-and-forget que siempre confirma éxito. Agregar retry cuando la conexión vuelva.
2. **(CRÍTICO-2)** Pedir exención de optimización de batería en onboarding; avisar explícitamente si `onTaskRemoved` apagó el Service mientras Man-Down estaba activo — hoy mata envío + recepción + detección juntos, sin rastro.
3. **(ALTO-1)** Cablear `AlertHelper.getSentConfirmation()` (ya escrito, nunca usado) a un Toast/Snackbar en `transmitGroupAlert()` para los tres botones manuales.
4. **(ALTO-2)** Corregir el copy de `getConfirmationText(EMERGENCY)` que afirma monitoreo garantizado en 140.970 — no existe tal garantía hoy. Priorizar el bug de origen (multi-canal/SCAN real) dado que hoy EMERGENCIA es el botón menos confiable de los tres.
5. **(ALTO-3)** Usar `USAGE_ALARM`/audio focus exclusivo (mismo patrón que el countdown local) para `notifyIncomingAlert()`, y evaluar pedir bypass de Do Not Disturb para el canal de alertas de seguridad.
6. **(MEDIO-1)** Agregar al copy de Man-Down una aclaración específica de que solo funciona si hay alguien en rango y escuchando — no asumir "alguien se va a enterar" para el caso de uso solitario que el proyecto dice soportar.

Nada de esto es un bloqueante de build ni requiere rediseño de protocolo — son todos cambios acotados a lógica de callback/copy, en el mismo espíritu que los fixes ya aplicados el 2026-07-06/07 para Man-Down. El más urgente de resolver antes de cualquier uso real en campo es CRÍTICO-1: una alerta de vida-o-muerte que miente sobre su propio éxito es peor que no tener la feature.
