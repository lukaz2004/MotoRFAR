# MotoRFAR

**Comunicación de grupo por radio VHF para motociclistas y vehículos 4x4 en Argentina.**

Sin licencia · Sin cobertura celular requerida · Sin cuota mensual.

## Cómo funciona

Un módulo hardware abierto (ESP32 + SA818-V) se conecta por USB-C al celular Android. La app MotoRFAR convierte ese módulo en un radio VHF que opera en los 3 canales libres de la **Resolución 5/2015** del ENACOM:

- **139.970 MHz** — Canal grupal por defecto
- **138.510 MHz** — Canal alternativo
- **140.970 MHz** — Emergencias (monitoreado a nivel nacional por M.T.T.T.)

La app suma encima del audio de radio: posicionamiento GPS continuo del grupo, alertas EMERGENCY/STOP/REGROUP con coordenadas adjuntas, mapas offline OpenStreetMap, y mensajes pre-formateados.

## Para quién

Grupos de motos en touring largas (Patagonia, NOA, Cuyo), expediciones 4x4, andinistas y cualquier actividad outdoor donde el celular no tiene señal y el grupo necesita coordinarse.

## Origen

Fork especializado para Argentina de [kv4p HT](https://github.com/VanceVagell/kv4p-ht) (Vance Vagell, KV4P), bajo licencia GPL-3.0.

## Estado

En desarrollo activo. Ver `docs/04-ROADMAP.md` para detalle de versiones.
