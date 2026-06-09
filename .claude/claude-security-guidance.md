# Security guidance — MotoRFAR

> Configuracion para el plugin security-guidance de Claude Code. Reglas especificas de este proyecto que se suman a las reglas generales del plugin.

## Reglas de codigo

1. **Cero hardcoding de credenciales o tokens.** API keys, tokens, URLs de servicios, todo via variables de entorno o configuracion runtime. Nunca commiteado al repo.

2. **No usar `eval`, `exec` ni equivalentes** en codigo Android Java/Kotlin. No introducir nuevas dependencias que lo hagan.

3. **Permisos Android al minimo.** Cada permiso solicitado en AndroidManifest.xml debe tener justificacion documentada en comentario adyacente. Los esperados:
   - ACCESS_FINE_LOCATION (GPS, balizas)
   - RECORD_AUDIO (PTT)
   - INTERNET (solo si v2 LTE bridge - en v1.0 NO)
   - BLUETOOTH_CONNECT (PTT de intercom v1.1+)
   - Cualquier permiso adicional requiere ADR en `docs/03-DECISIONES.md`.

4. **Validar input de radio.** Los paquetes digitales recibidos via SA818 son entrada no confiable. Parsear con cuidado, validar longitud, verificar checksums, no asumir nada del contenido.

5. **Sanitizar GPS.** Coordenadas recibidas pueden ser invalidas o spoofed. Verificar que estan dentro de rangos validos (-90/+90 lat, -180/+180 lon) antes de procesar.

6. **No logear data sensible** en logs de produccion. Posiciones GPS, alias de usuarios, contenido de mensajes - todo eso fuera de Logcat en builds release.

7. **Modo offline first.** La app debe ser plenamente funcional sin internet. Cualquier feature que requiera red debe degradarse gracefully cuando no hay conexion.

## Reglas de privacidad

1. **Sin telemetria de usuario por defecto.** No analytics, no crash reporting automatico, no envio de datos de uso. Si se quiere agregar, opt-in explicito con explicacion clara.

2. **GPS se procesa local.** Las posiciones del grupo se comunican solo via radio VHF entre ellos. No hay cloud en v1.0. (En v2.0 con LTE bridge, opt-in explicito y end-to-end encryption.)

3. **Alias de usuario es libre.** No pedir nombre real, telefono, email. No autenticacion. No cuenta.

4. **Privacy Policy clara y honesta** en `KV4PHT/app/src/main/res/values/strings.xml` y como pantalla in-app.

## Reglas de licencias

1. **Heredamos GPL-3.0 de kv4p HT.** Cualquier dependencia que se agregue debe ser compatible con GPL-3.0 (MIT, Apache, BSD, LGPL OK; nada con restriccion comercial o copyleft incompatible).

2. **Atribuir kv4p HT y Vance Vagell** en About screen, README y Play Store description.

3. **No incluir codigo propietario** ni reverse-engineered de Yaesu, Icom, Sena, etc.

## Reglas de tooling

1. **Antes de agregar una dependencia nueva** (Gradle), verificar en Maven Central o sources oficiales. No usar repos sospechosos.

2. **Pin de versiones.** No usar `+` ni rangos abiertos en build.gradle. Versiones fijas.

3. **Verificar checksums de releases descargadas** cuando se agreguen libs externas como JARs.

## Reglas de TX por radio

Esto es seguridad fisica/regulatoria, no solo software:

1. **HARD-LIMIT en codigo de las 3 frecuencias permitidas.** TX en cualquier otra frecuencia debe ser fisicamente imposible sin recompilar la app. Esto protege al usuario de violacion regulatoria accidental.

2. **HARD-LIMIT de potencia a 5W.** El SA818 soporta 1W por defecto, pero hay variantes 2W. Hardcodear el comando de potencia maxima en el firmware o setup serial.

3. **Logging de TX** local (no remoto) para auditabilidad si fuera necesario.

## Alertas y comportamiento

1. **EMERGENCIA siempre requiere confirmacion** (hold 2s o slide). Nunca un solo tap.

2. **No auto-TX sin accion del usuario** salvo balizas GPS configuradas explicitamente.

3. **Mensajes recibidos no abren acciones automaticas.** El usuario decide que hacer con cada uno.

## Auditoria periodica

Cada feature mayor (PR significativo) debe pasar:

- Code review por subagente
- Security scan en cambios de superficie de TX/RX o permisos
- Verificacion contra este documento
