# Session log — MotoRFAR

---

## 2026-06-09 · Sesión 0 — Setup e investigación inicial

- Stack instalado (PowerShell 7, Git, uv/uvx, plugins, MCPs)
- Investigación legal: Res. 5/2015 adoptada como marco, PMR446/FRS descartados
- Decisiones: SA818-V, VHF, 3 canales, modo único sin licencia, OSMDroid
- Scaffold completo: CLAUDE.md, 6 docs, gitignore, security guidance
- Git inicializado, 222 archivos en commit inicial

---

## 2026-06-09 · Sesión 1 — Sprint 1 completo (Fases A-D)

- A: ArgentinaChannels → 3 canales VHF (6/6 tests)
- B: TX whitelist hard-limit — imposible salir de las 3 frecuencias (12/12 tests)
- C: EMERGENCIA → 140.970 automático (8/8 tests)
- D: T&C v2.0 + strings sin PMR446
- PATH Windows corregido, GitHub configurado

---

## 2026-06-09 / 2026-06-10 · Sesión 2 — Sprint 2 visual + Sprint 3 (incompleto)

**Lo que se hizo:**
- Paleta verde táctico CRT completa (colors.xml reescrito)
- Share Tech Mono descargada y aplicada (res/font/share_tech_mono.ttf)
- Botones AppCompatButton con drawables custom (btn_emergency, btn_stop, btn_regroup)
- PTT con gradiente radial oval (ptt_button.xml como layer-list)
- rounded_corners con gradiente oscuro y borde verde
- memory_channel_bg/selected con layer-list y gradiente
- memory_channel_bg_emergency con borde rojo
- MemoriesAdapter.setHighlighted() → usa setBackground() con drawables (no más setBackgroundColor que ignora XML)
- Canal EMERGENCIA detectado por frecuencia y borde rojo aplicado
- S-meter hardcodeado a #4FBD3B para evitar override de Material3
- Canales renombrados: Grupo → PRINCIPAL
- Orden botones: EMERGENCIA arriba, DETENCIÓN+REAGRUPAR abajo
- Bottom nav: PTT + Mensajes con ícono micrófono
- Version bump v4_principal_vhf (re-seed forzado)
- RecyclerView: wrap_content (eliminado space vacío)
- GitHub actualizado

**Estado visual al cierre:**
- Build exitoso, 59/59 tests pasando
- La app corre en emulador, la estructura es correcta
- La brecha visual con el mockup verde todavía existe:
  - S-meter puede mostrar ámbar en algunas builds (override programático en MainActivity)
  - El canal PRINCIPAL no siempre muestra el borde verde (depende del estado al cargar)
  - EMERGENCIA channel card puede no mostrar borde rojo hasta que el adapter hace el first bind

**Causa raíz del bloqueo:**
Esta sesión acumuló demasiado contexto (~200K tokens). Las últimas horas trabajé con ventana saturada, generando más errores que soluciones en el visual.

---

## Estado actual del repo

Commits recientes:
- fix: botones AppCompat + S-meter verde + canal EMERGENCIA rojo
- fix: canal activo con borde verde real + PTT 90dp
- feat(Sprint3): visual CRT completo — fuente, botones 3D, PTT radial
- fix: version bump v4_principal_vhf

Tests: 59/59 PASS
Build: SUCCESSFUL
GitHub: https://github.com/lukaz2004/MotoRFAR

---

## 2026-06-10 · Sesión 3 — Plan Sprint 4 (write-plan)

**Lo que se hizo:**
- Lectura de CLAUDE.md, SESSION-LOG.md, docs/04-ROADMAP.md, docs/05-DISEÑO.md, 03-DECISIONES.md
- Auditoría de build.gradle: detectado que Kotlin plugin NO estaba en el proyecto (solo kotlin-bom como impl dep), Compose no habilitado, kotlin 1.8.22 incompatible con Compose Compiler 1.5.x
- Auditoría de activity_main.xml (463 líneas) → mapeados todos los componentes XML a equivalentes Compose
- Escrito docs/SPRINT-4.md con plan completo de 3 fases (A: infra, B: binding service, C: OSMDroid)
- Escrito ADR-008 en docs/03-DECISIONES.md: documenta la decisión de migrar a Compose y por qué

**Decisiones tomadas:**
- Kotlin 1.9.22 + Compose Compiler 1.5.10 + Compose BOM 2024.02.02 (tabla oficial de compatibilidad)
- MainActivity.java → MainActivityLegacy.java (referencia), MainActivity.kt es el nuevo entry point
- MemoriesAdapter.java reemplazado por ChannelRow composable (3 canales no necesitan RecyclerView)
- Colors CRT como parámetros de función (Brush.radialGradient, Canvas) — no herencia de tema
- OSMDroid via AndroidView wrapper dentro de MapScreen composable

**Qué queda pendiente:**
- EJECUCIÓN completa del plan (Fases A → B → C en Claude Code)
- Crear branch sprint/4-compose-ui
- A0: smoke test 59 tests antes de tocar nada
- A1: rename MainActivity.java → MainActivityLegacy.java

---

## Para la próxima sesión — Sprint 4 (Compose migration)

Arrancar Claude Code en MotoRFAR-MTTT con este prompt:

```
Leé CLAUDE.md, docs/SESSION-LOG.md y docs/SPRINT-4.md.

El plan de Sprint 4 ya está escrito y auditado. Ir directo a ejecución:

1. Crear branch sprint/4-compose-ui
2. Ejecutar Fase A → B → C en orden, con TDD
3. Commit atómico al cerrar cada fase

No hacer brainstorming — las decisiones están en ADR-008 (docs/03-DECISIONES.md).
La primera acción es A0: ./gradlew test para confirmar que los 59 tests pasan antes de tocar nada.
```

---

## 2026-06-10 · Sesión 4 — Sprint 4 ejecutado (Compose UI completo)

**Lo que se hizo:**

- A0: confirmados 59 tests pre-existentes antes de tocar nada
- Branch `sprint/4-compose-ui` creado
- A1: `git mv MainActivity.java → MainActivityLegacy.java`, manifest y refs actualizadas
- A2: Gradle — Kotlin 2.2.0 + Compose Compiler bundled + Compose BOM 2024.02.02 + OSMDroid 6.1.18
- A3-A7: theme CRT (Color.kt, Type.kt, AppTheme.kt, MotoRFARTheme.kt), estado (MainUiState, MainUiAction), 5 componentes Compose, MainScreen.kt
- Fase A: 16 tests nuevos, commit `feat(A)`
- Fase B: MainActivity.kt Kotlin — ServiceConnection con RadioBinder, MutableStateFlow, callbacks, handleAction, alert flow con GPS permission — commit `feat(B)`
- Fase C: MapScreen.kt (OSMDroid via AndroidView), GroupMember.kt, NavHost + NavigationBar — commit `feat(C)`

**Tests al cierre:** 75/75 PASS (unit). `assembleDebug` SUCCESSFUL.

**Decisiones nuevas:** ADR-009 (Kotlin 2.2.0 bundled Compose Compiler), ADR-010 (RadioServiceAccessor bridge Kotlin↔Lombok).

**Pendiente:** push + PR, tests instrumented en dispositivo, GroupMember conectado a GPS real (Sprint 5).

---
