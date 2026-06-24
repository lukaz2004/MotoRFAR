# Politica de Privacidad — MotoRFAR HT

**Ultima actualizacion:** junio 2026

MotoRFAR HT es una aplicacion de radio VHF para motociclistas y vehiculos 4x4 en Argentina. Opera en frecuencias libres habilitadas por la Resolucion 5/2015 del Ministerio de Transporte (M.T.T.T.): 138.510, 139.970 y 140.970 MHz.

## Datos que usa la app

| Dato | Para que | Se envia a internet? |
|------|----------|---------------------|
| Ubicacion GPS | Mostrar tu posicion en el mapa y compartirla con tu grupo por radio | No |
| Audio del microfono | Transmitir voz por PTT a traves del modulo de radio | No |
| Frecuencia de operacion | Sintonizar el transceiver en los canales permitidos | No |
| Alias / indicativo | Identificarte dentro del grupo | No |
| Tiles de mapa (OSM) | Mostrar el mapa; se descargan de OpenStreetMap y se guardan en cache local | Solo la descarga inicial |

## Que NO hacemos

- **No recopilamos datos personales** para publicidad, analitica ni perfilamiento.
- **No enviamos datos a servidores propios ni de terceros.** Toda la comunicacion ocurre por radio VHF entre los dispositivos del grupo.
- **No almacenamos grabaciones de audio.** El audio PTT se transmite en tiempo real y no se guarda.
- **No rastreamos tu ubicacion en segundo plano.** El GPS solo se usa mientras la app esta abierta y visible.

## Almacenamiento local

La app guarda en tu dispositivo:
- Configuracion de radio (frecuencia, squelch, filtros)
- Cache de tiles del mapa para uso offline
- Tu alias e indicativo

Estos datos permanecen en tu telefono y se borran al desinstalar la app.

## Hardware

MotoRFAR HT requiere un modulo de radio externo (ESP32 + SA868S) conectado por USB. El hardware es propiedad del usuario. La app no modifica el firmware del modulo sin tu intervencion.

## Permisos del sistema

| Permiso | Motivo |
|---------|--------|
| Ubicacion (GPS) | Mostrar tu posicion en el mapa |
| Microfono | Transmitir audio por PTT |
| USB | Comunicarse con el modulo de radio |
| Internet | Descargar tiles del mapa (OpenStreetMap) |

## Codigo abierto

MotoRFAR HT es software libre bajo licencia **GNU GPL v3.0**. El codigo fuente esta disponible para su auditoria.

## Contacto

Si tenes preguntas sobre esta politica, escribinos a: **lukaz1979@gmail.com**
