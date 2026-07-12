# Autenticación UDP — diseño (2026-07-11)

> Sesión de diseño pedida por `AUDITORIA_SEGURIDAD_FIRMWARE.md` (hallazgo
> ALTO-1). Decisión tomada en chat con LuKaZ: reusar la clave WPA2 del
> equipo como secreto, en vez de un emparejamiento físico separado (ver
> "Qué NO resuelve esto" abajo — la decisión fue informada).

## Resumen de la decisión

- **Secreto:** la clave WPA2 vigente del equipo (la misma que ya existe,
  única por dispositivo, cambiable desde Ajustes > WiFi).
- **Mecanismo:** HMAC-SHA256 truncado a 8 bytes, agregado al final del
  payload de `COMMAND_HOST_DESIRED_STATE` (el comando de control real —
  canal, PTT, TX allowed). Protección contra replay reusando el campo
  `sequence` que el struct ya tiene.
- **Alcance:** solo `COMMAND_HOST_DESIRED_STATE`. Audio (TX/RX) queda sin
  autenticar — HMAC por cada frame de audio es caro para el ESP32 y no
  frena nada que un atacante no pueda hacer ya (no puede fabricar RF real
  con audio falso).
- **Activación:** automática y transparente la primera vez que el usuario
  cambia la clave WiFi desde la app (ver "Bootstrap" abajo) — cero UI
  nueva de emparejamiento.

## Qué NO resuelve esto (importante, dicho explícitamente)

Esto NO es una autenticación de aplicación real. El secreto (clave WPA2)
es el mismo dato que hace falta para asociarse al SoftAP en primer lugar
— cualquiera que ya esté asociado (porque conoce/crackeó la clave) puede
calcular el mismo HMAC que la app legítima. **No cierra el hueco que
`AUDITORIA_SEGURIDAD_FIRMWARE.md` dejó pendiente en ALTO-1** (atacante
que desautentica al cliente legítimo y roba el único slot de
`max_connections=1` — ese atacante ya tiene la clave, así que también
puede firmar comandos válidos).

Lo que sí aporta, con honestidad:
- Filtra tráfico corrupto/mal formado o de clientes no maliciosos que por
  error/bug mandan basura al puerto UDP.
- Protección contra replay de paquetes capturados (con el chequeo de
  `sequence`), independiente de si el atacante conoce o no la clave.
- Es un escalón: el formato de wire (tag al final del payload) queda
  listo para el día que se quiera migrar a un secreto de emparejamiento
  real (atado al botón físico PTT, como se discutió y se descartó por
  ahora) — ese cambio futuro solo reemplaza *qué* secreto se usa para
  firmar, no el formato del paquete ni el chequeo de replay.

Si en algún momento el modelo de amenaza real lo justifica (ej. el canal
de Emergencia se usa en un contexto donde jamming intencional es un
riesgo serio), retomar la opción de emparejamiento físico descartada acá.

## Wire format

`COMMAND_HOST_DESIRED_STATE` (0x0D) hoy manda `HostDesiredState.BYTE_LEN`
= 22 bytes crudos (`Protocol.java:155-182`). Con auth:

```
payload = HostDesiredState (22 bytes) + HMAC-SHA256(key, HostDesiredState)[0:8]
        = 30 bytes totales
```

- `key` = bytes ASCII de la clave WiFi vigente (`UdpAuth.keyFromPassword()`,
  ya implementado del lado Android).
- El HMAC se calcula sobre los 22 bytes crudos del struct (antes de
  aplicar KISS escaping — el escaping es de transporte, no de contenido).
- Truncar a 8 bytes (64 bits) es suficiente para este modelo de amenaza
  (no es una barrera criptográfica fuerte, es integridad + anti-replay
  liviano) y mantiene el paquete chico para AFSK/VHF.

## Lado firmware (pendiente — no está en este repo)

`kv4p-ht-main/microcontroller-src/` vive en la máquina de LuKaZ, no en
esta sesión. Esto es lo que hay que implementar ahí (pseudocódigo /C++
real, para pegar en la próxima sesión de firmware):

```cpp
// wifi_credentials.h o donde se persiste apPassword ya existe
#define UDP_AUTH_TAG_LEN 8

// Nuevo: recordar la última sequence aceptada para anti-replay.
// Se resetea a 0 en cada boot (no hace falta persistir en NVS).
static uint32_t lastAcceptedSequence = 0;

bool verifyAuthTag(const uint8_t* payload, size_t payloadLen,
                    const uint8_t* tag, const char* wifiPassword) {
  uint8_t computed[32]; // SHA256 full digest
  // mbedtls_md_hmac (ya disponible en el framework ESP-IDF/Arduino-ESP32)
  mbedtls_md_context_t ctx;
  mbedtls_md_init(&ctx);
  mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(MBEDTLS_MD_SHA256), 1);
  mbedtls_md_hmac_starts(&ctx, (const uint8_t*)wifiPassword, strlen(wifiPassword));
  mbedtls_md_hmac_update(&ctx, payload, payloadLen);
  mbedtls_md_hmac_finish(&ctx, computed);
  mbedtls_md_free(&ctx);

  // Comparación en tiempo constante
  uint8_t diff = 0;
  for (int i = 0; i < UDP_AUTH_TAG_LEN; i++) diff |= computed[i] ^ tag[i];
  return diff == 0;
}

// En el handler de COMMAND_HOST_DESIRED_STATE (kv4p_ht_esp32_wroom_32.ino:488-494):
case COMMAND_HOST_DESIRED_STATE:
  if (authEnabled) {
    if (param_len != sizeof(HostDesiredState) + UDP_AUTH_TAG_LEN) break; // descarta silencioso
    const uint8_t* tag = params + sizeof(HostDesiredState);
    if (!verifyAuthTag(params, sizeof(HostDesiredState), tag, apPassword.c_str())) break;
    HostDesiredState incoming;
    memcpy(&incoming, params, sizeof(HostDesiredState));
    if (incoming.sequence <= lastAcceptedSequence) break; // replay, descarta
    lastAcceptedSequence = incoming.sequence;
    memcpy(&desiredState, &incoming, sizeof(HostDesiredState));
  } else {
    // comportamiento actual, sin cambios
    if (param_len != sizeof(HostDesiredState)) break;
    memcpy(&desiredState, params, sizeof(HostDesiredState));
  }
  reconcileDesiredState();
  esp_task_wdt_reset();
  break;
```

**`authEnabled`:** flag persistido en NVS (mismo namespace `wifinet` que ya
usa `loadOrCreateWifiPassword()`), en `false` hasta que el firmware procese
un `COMMAND_HOST_SET_WIFI_PASSWORD` válido — momento en el que pasa a
`true` para siempre (no hay vuelta atrás sin reflashear). Esto matchea el
bootstrap del lado app (ver abajo): el mismo evento que "activa" el auth
del lado firmware es el que hace que la app empiece a firmar.

## Bootstrap (por qué no rompe nada al desplegar)

Problema real: si la app empieza a mandar el tag antes de que el firmware
sepa validarlo, el firmware actual (`param_len == sizeof(HostDesiredState)`
exacto, sin más) **descarta silenciosamente todo paquete de control** —
el equipo deja de responder a cambios de canal/PTT hasta reflashear. Por
eso, en este pase:

1. **Lado Android — hecho hoy:** `AppSetting.SETTING_WIFI_PASSWORD` guarda
   la clave la próxima vez que se cambia desde Ajustes > WiFi
   (`MainActivity.saveWifiPasswordLocally()`), y `UdpAuth.java` tiene la
   utilidad HMAC lista. **No wireado al envío real** (`Protocol.Sender`
   sigue mandando `COMMAND_HOST_DESIRED_STATE` sin tag, exactamente igual
   que antes) — activar esto sin el firmware que lo entienda rompe la
   comunicación con el hardware real de LuKaZ.
2. **Lado firmware — próxima sesión (Windows):** implementar el
   pseudocódigo de arriba. Mientras `authEnabled == false` (clave de
   fábrica sin cambiar), el firmware sigue aceptando paquetes sin tag
   exactamente como hoy — cero regresión para equipos que nunca
   cambiaron la clave.
3. **Una vez el firmware esté flasheado con soporte de auth:** recién ahí
   activar el lado Android (agregar el tag a `Sender.sendDesiredState()`
   cuando `pairedWifiPassword != null`, mandar sin tag si es null —
   coherente con que el firmware todavía acepta ambos formatos mientras
   `authEnabled` sea false).

**No activar el paso 3 sin haber confirmado el paso 2 en el equipo físico
real.** Este orden es la parte no negociable de este diseño.

## Checklist para la próxima sesión

- [ ] Firmware: agregar `UDP_AUTH_TAG_LEN`, `lastAcceptedSequence`,
      `authEnabled` (NVS), `verifyAuthTag()`, y el branching en el handler
      de `COMMAND_HOST_DESIRED_STATE` (ver pseudocódigo arriba).
- [ ] Firmware: `COMMAND_HOST_SET_WIFI_PASSWORD` setea `authEnabled = true`
      tras aplicar la clave nueva (persistir en NVS).
- [ ] Firmware: build + flash + smoke test — confirmar que un equipo con
      clave de fábrica (sin cambiar) sigue funcionando igual que hoy.
- [ ] Firmware: cambiar la clave desde la app, confirmar que
      `authEnabled` pasa a true y que paquetes SIN tag después de eso se
      descartan (probar a mano, ej. con `fw3a_smoke.py` mandando el
      formato viejo a propósito).
- [ ] Android: en `Protocol.Sender.sendDesiredState()`, agregar el tag
      (`UdpAuth.computeTag`) cuando `pairedWifiPassword != null` — pasar
      la clave desde `MainActivity`/`RadioAudioService` hasta
      `RadioModuleController`/`Protocol.Sender` (hoy no tienen ese dato,
      hace falta enchufarlo).
- [ ] Android: probar con el equipo real flasheado — cambiar canal/PTT
      después de haber cambiado la clave, confirmar que sigue funcionando
      con el tag agregado.
- [ ] Confirmar que dispositivos ya en campo (si los hay) con clave de
      fábrica sin cambiar migran bien al actualizar firmware (no deberían
      notar nada hasta que cambien la clave por primera vez).
