# MOTO HT — Herramientas, Agentes y Plugins

Evaluación de qué necesitás para este proyecto, qué ya tenés, y qué conviene sumar.

---

## LO QUE YA TENÉS (suficiente para la mayoría)

| Herramienta | Para qué | Estado |
|-------------|----------|--------|
| **Desktop Commander** | Ejecutar todo local, archivos, scripts, KiCad por CLI | ✅ Funcionando |
| **KiCad 10.0** + python pcbnew | Editar/validar PCB | ✅ Instalado |
| **Fusion 360** + MCP bridge | Carcasa 3D | ✅ (reconectar bridge cuando se cae) |
| **Android Studio** + gradle | Compilar app | ✅ |
| **PlatformIO** (o Arduino IDE) | Compilar firmware ESP32 | ⬜ Verificar instalación en FW-1 |
| **Claude Code** + plugins (ECC) | Ejecución autónoma de sprints | ✅ Ya usado en Sprint 7 |

---

## LO QUE CONVIENE SUMAR

### Para Firmware WiFi (si F0=WiFi) — RAMA FW-3
- **ESP32 de prueba suelto** (físico): para prototipar WiFi SIN arriesgar la placa buena. Cualquier devkit ESP32-WROOM-32 sirve. **Crítico para validar F0.**
- **Wireshark** o similar: para depurar el tráfico de red del prototipo WiFi.
- Librerías ESP32: `WiFi.h`, `AsyncUDP` o `ESPAsyncWebServer` (vienen con el core ESP32, no son compra).

### Para validar audio/latencia — RAMA F0
- Un segundo teléfono Android o el ESP32 de prueba para medir latencia real del enlace WiFi con audio PTT.

### Para CNC (estética) — RAMA EST-3
- Fusion 360 ya hace el CAM (G-code). No necesitás software nuevo.
- Acceso a la fresadora (tercerizado o propio) — la fresa de 6mm ya confirmada.

---

## AGENTES / FORMA DE TRABAJO RECOMENDADA

### Metodología de chats separados (lo que vos pediste)
- **1 chat = 1 etapa.** Cada chat lee solo el archivo de su rama + contratos. Contexto liviano.
- Al terminar una etapa, Claude actualiza el archivo de esa rama con lo hecho y el estado.
- Los contratos (`01_CONTRATOS.md`) son la memoria compartida entre chats.

### Claude Code para las ramas de software (FW, APP)
- Ya usás ECC (everything-claude-code) y `--dangerously-skip-permissions`.
- Mantené tu metodología de sprints (CLAUDE.md bootloader, SESSION-LOG.md, SPRINT-N.md, ADRs) DENTRO de cada rama de software.

### Desktop Commander para HW y EST
- Todo el trabajo de KiCad y Fusion sigue por DC, local, como hasta ahora.

---

## ¿NECESITÁS PLUGINS NUEVOS DE CLAUDE?
**No imprescindibles.** Lo que tenés cubre el proyecto. Lo único "nuevo" es hardware físico (un ESP32 de prueba) para validar WiFi sin riesgo — eso no es un plugin, es una compra de ~US$5-10 muy recomendable antes de comprometer la arquitectura.

---

## REGLA DE ORO DEL PROYECTO
**Nunca rutear cobre crítico (cerca de ESP32/USB/RF) por script a ciegas.** Eso va SIEMPRE en la GUI de KiCad, con vista de las 4 capas. Lección ya aprendida y pagada en esta etapa de diseño.
