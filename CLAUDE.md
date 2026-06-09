# CLAUDE.md — Boot loader del proyecto MotoRFAR

> **Esta es la primera lectura obligatoria de cualquier sesión de Claude Code en este proyecto.** Leer también los docs referenciados antes de proponer cambios estructurales.

## Qué es MotoRFAR

App Android + accesorio hardware abierto que permite comunicación de radio VHF grupal entre motociclistas y vehículos 4x4 en zonas sin cobertura celular. Opera bajo el marco legal argentino de la **Resolución 5/2015 (M.T.T.T.)** en los 3 canales libres VHF (138.510, 139.970, 140.970 MHz). Sin licencia. Sin trámites.

Es un fork especializado de [kv4p HT](https://github.com/VanceVagell/kv4p-ht) (de Vance Vagell, KV4P).

## Estado actual

- **Fase:** desarrollo de v1.0 (MVP)
- **Equipo:** un desarrollador solo (LuKaZ), presupuesto ajustado
- **Hardware target:** ESP32-S3 + SA818-V (módulo VHF) + antena externa
- **Idioma de la app:** español rioplatense
- **Idioma del código:** inglés (estándar de la industria)

## Reglas operativas para cualquier sesión

1. **Antes de proponer features nuevas, leer `docs/04-ROADMAP.md`.** Lo que está en v1.0 se hace, lo que está en v1.1/v2.0 se posterga.
2. **Antes de cuestionar una decisión técnica, leer `docs/03-DECISIONES.md`.** Si la decisión está ahí, fue debatida y tiene fundamento. Para revertirla hay que escribir un ADR nuevo.
3. **Antes de tocar frecuencias o aspectos legales, leer `docs/02-MARCO-LEGAL.md`.** Operar fuera del marco invalida el proyecto.
4. **Antes de proponer cambios visuales, leer `docs/05-DISEÑO.md`.** La estética CRT ámbar/verde no es decorativa, es funcional.
5. **TDD por defecto** — el plugin Superpowers fuerza tests antes de código. Respetar el flujo, no saltearlo.
6. **No commits directos a main.** Trabajar en feature branches, abrir PR aunque sea solo de uno mismo.

## Mapa de los docs

| Archivo | Contenido |
|---|---|
| `docs/01-PROYECTO.md` | Visión, usuario objetivo, propuesta de valor |
| `docs/02-MARCO-LEGAL.md` | Res. 5/2015 + decisiones legales consolidadas |
| `docs/03-DECISIONES.md` | ADRs: cada decisión técnica con contexto y consecuencias |
| `docs/04-ROADMAP.md` | v1.0 / v1.1 / v2.0 con features explícitas |
| `docs/05-DISEÑO.md` | Sistema visual: paleta ámbar/verde, tipografía, mockup |
| `docs/06-HARDWARE.md` | Especificación de PCB, SA818-V, antena, PTT externo |

## Stack de tooling esperado (instalado a nivel usuario)

- Plugin **superpowers** para flujos de brainstorm/spec/plan/TDD
- Plugin **security-guidance** (configurado vía `.claude/claude-security-guidance.md`)
- Plugin **code-review** para revisiones multi-agente
- MCP **context7** para docs de librerías Android al día
- MCP **sequential-thinking** para decisiones de arquitectura
- MCP **github** (token READ-ONLY)
- MCP **fetch** para investigación

Si alguno falta, recomendar instalación pero no bloquear el trabajo.

## Próxima acción al abrir el proyecto

Si es la primera sesión post-setup, ejecutar:

```
/brainstorm Auditar el estado actual del código KV4PHT, identificar archivos que requieren modificación para implementar la migración a 3 canales VHF según docs/04-ROADMAP.md sprint 1, y generar plan de ejecución.
```
