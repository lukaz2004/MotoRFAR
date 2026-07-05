# CLAUDE.md — MotoRFAR HT

> **IMPORTANTE:** El sistema de gestión del proyecto vive en `_PROYECTO/`.
> Leé `_PROYECTO/NEXT_SESSION.md` y `_PROYECTO/PENDIENTES.md` para entender el estado real.
> Este archivo es solo contexto técnico de base, NO es la fuente de estado del proyecto.

## Qué es

Radio VHF grupal para cualquiera que se mueva sin cobertura de red tradicional en Argentina: motociclistas, 4x4, ciclistas, senderistas, trabajo rural, a pie o en cualquier otro medio. Opera en los 3 canales libres de la Resolución 5/2015 (M.T.T.T.): 138.510 / 139.970 / 140.970 MHz. Sin licencia. Sin trámites.
Fork de [kv4p HT](https://github.com/VanceVagell/kv4p-ht) (Vance Vagell, KV4P), GPL-3.0.

## Ubicación de componentes

| Qué | Dónde |
|-----|-------|
| Gestión del proyecto | `_PROYECTO/` — FUENTE DE VERDAD |
| App Android | `KV4PHT/` |
| Firmware ESP32 | `..\kv4p-ht-main\microcontroller-src\kv4p_ht_esp32_wroom_32\` |
| PCB | `..\kv4p-ht-main\pcb\v2.0e\kv4p-ht\` |

## Comandos clave

```powershell
# Build Android
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd KV4PHT && .\gradlew assembleDebug

# Build Firmware
$env:PLATFORMIO_BUILD_DIR = 'C:\Users\lukaz\pio-build'
& 'C:\Users\lukaz\pio-venv\Scripts\pio.exe' run -e esp32dev -d '..\kv4p-ht-main\microcontroller-src'

# kicad-cli
C:\Users\lukaz\AppData\Local\Programs\KiCad\10.0\bin\kicad-cli.exe
```

## Reglas que no se negocian

- **Baqueano no es solo para motos.** Es para cualquier usuario en cualquier medio de transporte
  o a pie (moto, 4x4, bici, senderismo, trabajo rural) que necesite comunicarse en zona sin red
  de datos tradicional. Ninguna feature (Man-Down, alertas, textos legales, copy) se diseña o
  redacta asumiendo motociclista como único usuario — usar lenguaje neutral de actividad/vehículo.
- **Whitelist TX:** solo 3 frecuencias Res. 5/2015. El firmware es la autoridad final.
- **Nunca rutear cobre crítico del PCB por script** — siempre GUI KiCad.
- **No commits directos a main.** Feature branches + PR.
- **Idioma del código:** inglés. **Idioma de la app:** español rioplatense.
