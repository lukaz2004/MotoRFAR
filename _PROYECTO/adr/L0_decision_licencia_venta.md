# L0 — Decisión: Licencia y comercialización

**Fecha:** 2026-06-23
**Estado:** ✅ Decidido

## Contexto
Se evalúa comercializar MotoRFAR HT. Pregunta inicial: ¿hay que salir del proyecto
kv4p-ht (GPLv3) para poder vender / cerrar el código? Objetivo real aclarado por el
usuario: **poder vender el producto sin problemas legales**; publicar el fuente del
firmware o poner atribuciones en un disclaimer NO molesta. Lo que se quiere es
libertad para comerciar.

## Auditoría de licencias (leída de cada LICENSE en .pio/libdeps)
| Componente | Licencia | Tipo |
|---|---|---|
| Firmware kv4p (app code) | GPL-3.0 | copyleft fuerte |
| arduino-audio-tools | GPL-3.0 | copyleft fuerte |
| esp32-afsk (APRS) | GPL-3.0-only | copyleft fuerte |
| DRA818 | LGPL-3.0 | copyleft débil |
| arduino-libopus (Opus) | BSD-3-Clause | permisiva |
| Core Arduino-ESP32 | LGPL-2.1+ / Apache-2.0 | mixta |
| App Android (fork de kv4p-ht) | GPL-3.0 | copyleft fuerte |

Licencia dominante del firmware Y de la app: **GPL-3.0**.

## Decisión
- **Vender es 100% compatible con GPL.** Se venden equipos con firmware GPL todo el
  tiempo. NO se hace reescritura clean-room. NO se sale de kv4p-ht.
- **Firmware:** sigue siendo fork GPL-3.0. Se publica el fuente. Se sigue construyendo
  sobre él sin tirar nada (FW-3b, CTCSS, etc. suman).
- **App:** la app actual también es fork GPL-3.0. Como vender con GPL está OK, **no hace
  falta tirar la app madura** (171 tests, sprints 5-9): se puede evolucionar (incluido el
  rework de la capa de comunicación USB→WiFi) y queda GPL. Una app NUEVA de cero solo
  tendría sentido por OTRAS razones (UX/arquitectura), NO por licencia.
- **Lo que se protege/cierra:** marca (registro aparte, GPL no la toca) y hardware. El
  código queda GPL y eso no impide vender.

## Obligaciones de compliance para VENDER (checklist)
1. Publicar el fuente correspondiente del firmware que va en el equipo (repo público +
   scripts de build). La clave del AP NO es fuente obligatorio (config secreta).
2. Avisos de licencia/copyright en pantalla "Acerca de / Licencias" (cubre Opus BSD y
   apunta al fuente de las GPL/LGPL).
3. **Anti-tivoización (GPLv3 §6, "User Products"):** el equipo debe quedar
   **flasheable por el usuario** (instalar firmware modificado). El ESP32 se flashea por
   USB sin bootloader bloqueado → se cumple. **NO agregar secure-boot/firmware firmado
   que impida flashear en producción.**
4. LGPL (DRA818, partes del core): permitir relinkeo / sustituir la lib modificada.
5. Sin EULA que restrinja el firmware (sobre la app, EULA propio está OK).

## Consecuencias
- Se descarta la reescritura clean-room (respondía a una necesidad que no era la real).
- El moat del producto está en app/UX/hardware/marca, no en esconder el firmware.
- **No soy abogado:** antes de vender en serio, conviene una revisión con IP/licencias.
  El camino es estándar y de bajo riesgo siguiendo el checklist.
