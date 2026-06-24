# 03 — Decisiones de arquitectura (ADRs)

> Cada decision tiene su propio ADR (Architecture Decision Record). Para revertir una decision, escribir un ADR nuevo con numero siguiente que la supere; no editar los anteriores.

---

## ADR-001 — Pivot de UHF (PMR446) a VHF (Res. 5/2015) como banda primaria

**Fecha:** 2026-06-09
**Estado:** ACEPTADA

### Contexto

El fork original (kv4p-ht-main) y la primera adaptacion argentina (motoRFAR/) usaban los 16 canales PMR446 (446 MHz) asumiendo - incorrectamente - que eran libres en Argentina. Investigacion exhaustiva determino que PMR446 no esta adoptado por ENACOM. La unica banda VHF/UHF libre sin restricciones tecnicas mayores en Argentina es la VHF de Res. 5/2015.

### Decision

Pivotear el proyecto a SA818-V (VHF) con los 3 canales de Res. 5/2015 como banda primaria: 138.510, 139.970, 140.970 MHz.

### Consecuencias

**Positivas:**
- Marco legal solido y citable (resolucion dedicada al caso de uso)
- Hardware kv4p HT compatible sin modificar (SA818-V cubre el rango)
- Mejor propagacion en terreno mixto (VHF penetra mejor obstaculos que UHF)
- Antena externa permitida (a diferencia de Res. 2750/98 UHF familiar)
- Continuidad con APRS estandar 144.800 MHz para users con licencia

**Negativas:**
- 3 canales en lugar de 16; mitigable con CTCSS para subgrupos
- Antena fisicamente mas grande (~50 cm cuarto onda vs 16 cm UHF)
- Hay que actualizar codigo: `ArgentinaChannels.java`, T&C, descripcion Play Store

---

## ADR-002 — Modo unico, sin distincion licenciado/no-licenciado

**Fecha:** 2026-06-09
**Estado:** ACEPTADA

### Contexto

Inicialmente se planeo soportar dos modos: "Motociclista" (TX en Res. 5/2015) y "Radioaficionado" (TX completo en bandas amateur 144-148, 430-440 MHz). Esto complicaba onboarding, T&C y UX.

### Decision

Un solo modo de operacion. TX exclusivamente en los 3 canales de Res. 5/2015. RX permitida en toda la banda VHF del SA818-V (134-174 MHz) para que el usuario pueda monitorear APRS u otras frecuencias sin transmitir.

### Justificacion

Los radioaficionados ya tienen equipos comerciales propios (Yaesu, Icom, Kenwood). No son el target de MotoRFAR. Servirlos sumaba complejidad sin sumar mercado.

### Consecuencias

**Positivas:**
- UX limpia, sin pregunta de licencia en onboarding
- T&C corto y honesto, cita una sola resolucion
- Marketing claro: "Sin licencias ni tramites"
- Implementacion mas simple (sin gating por flag de licencia)

**Negativas:**
- Si en el futuro algun radioaficionado quiere usarlo en bandas amateur, no puede. Aceptable: no son el target.

---

## ADR-003 — Tres canales con nombres funcionales, no numericos

**Fecha:** 2026-06-09
**Estado:** ACEPTADA

### Decision

En la UI, los canales se llaman **Grupo / Alternativo / Emergencia**, no por su numero o frecuencia cruda. La frecuencia se muestra como detalle secundario.

### Justificacion

El target user no es radioaficionado. "139.970 MHz" no le dice nada. "Grupo" si.

### Consecuencias

- Strings.xml debe contener nombres en español
- En settings, el usuario puede ver la frecuencia tecnica pero la accion primaria es por nombre
- 140.970 "Emergencia" tiene tratamiento visual distinto (rojo) para evitar uso accidental como canal de charla

---

## ADR-004 — Estetica CRT militar amber/green alternable

**Fecha:** 2026-06-09
**Estado:** ACEPTADA

### Decision

Theme visual con dos variantes:
- **AMBAR** (defecto) - referencia: AN/PRC-148 MBITR
- **VERDE FOSFORO** - referencia: Harris Falcon III modo nocturno

Color base oscuro casi negro (#0e0904), textos en amber #FAC775 / green #4FBD3B segun theme. Tipografia monospace (JetBrains Mono o Share Tech Mono). Efectos sutiles: scan lines CRT, glow de fosforo en displays de frecuencia.

### Justificacion

- Funcional: alta legibilidad bajo sol con anteojos polarizados, no encandila de noche
- Identidad: diferencia visual fuerte vs apps genericas
- Pertinencia: el contexto de uso (motos, expediciones) refuerza esa estetica
- Costo: cero assets externos requeridos, todo CSS/XML

### Consecuencias

- Crear sistema de themes en `res/values/themes.xml` con dos variantes
- Toggle ambar/verde en settings
- Iconografia: lucide/tabler icons monocromos

Ver mockup de referencia en `docs/05-DISEÑO.md`.

---

## ADR-005 — OpenStreetMap (OSMDroid) en vez de Google Maps

**Fecha:** 2026-06-09
**Estado:** ACEPTADA

### Decision

Usar OSMDroid como engine de mapas dentro de la app. Permite caching offline de tiles, drawing de rutas, markers personalizados.

### Justificacion

- **Licencia:** OSMDroid es Apache 2.0, Google Maps SDK tiene termino comercial y limita uso offline
- **Offline real:** OSMDroid permite descargar zonas enteras (Patagonia ~80MB) y operar sin internet
- **Costo:** cero
- **Comunidad outdoor:** OSM tiene mejor cobertura de caminos rurales y senderos que Google en Argentina remota

### Consecuencias

- Dependencia: `org.osmdroid:osmdroid-android` en build.gradle
- Pedir permiso WRITE_EXTERNAL_STORAGE solo si descarga offline
- UI de descarga de zonas (rectangulo de seleccion + tamaño estimado)
- Alternativa futura para detalle outdoor: OpenAndroMaps vector tiles

---

## ADR-006 — Hardware: SA818-V + ESP32-S3 + jack PTT externo

**Fecha:** 2026-06-09
**Estado:** ACEPTADA

### Decision

PCB v2.0c del proyecto kv4p HT con:
- Modulo **SA818-V** (variante VHF, 134-174 MHz) - NO el SA818-U
- ESP32-S3 (alternativa al ESP32 original, mas memoria, USB-C nativo)
- **Conector pigtail SMA con cable corto** entre PCB y antena para absorber vibracion
- **Jack 3.5mm adicional** para PTT externo cableado al manillar
- Caja estanca impresa 3D (PETG o ASA) con SMA pasante

### Justificacion

Ver `docs/06-HARDWARE.md` para detalle. SA818-V por banda. ESP32-S3 por USB-C. Pigtail por durabilidad en moto. PTT externo por usabilidad con guantes.

### Consecuencias

- BOM diferente al v1.8c original (que usa DRA818V o variante UHF)
- Diseño de caja propia (no usar la del original)
- Codigo Android debe agregar deteccion de PTT por jack ademas de boton fisico PCB

---

## ADR-007 — Idioma: UI en español rioplatense, codigo en ingles

**Fecha:** 2026-06-09
**Estado:** ACEPTADA

### Decision

- Strings.xml en español rioplatense (no neutro, no peninsular)
- Comentarios de codigo, nombres de variables, nombres de archivos, commits: ingles
- Nombres de funciones de UI siguen convencion Android (camelCase ingles)

### Justificacion

- Target user es argentino primario
- Codigo en ingles abre puerta a colaboracion futura, alineado a kv4p HT upstream
- Sincretismo es estandar de industria

---

## ADR-008 — Migración de UI a Jetpack Compose

**Fecha:** 2026-06-10
**Estado:** ACEPTADA

### Contexto

Los Sprints 2 y 3 revelaron un problema estructural: Material3 sobreescribía los colores hardcodeados en nuestros custom drawables XML. Múltiples sesiones intentaron fijarlo con workarounds (setBackgroundColor() programático, AppCompatButton, hardcodeo de #4FBD3B en lugar de @color/...) sin éxito sostenible. La causa raíz es que el sistema de temas XML de Android tiene múltiples capas de override y Material3 gana en el nivel más específico. No es un bug: es el diseño del sistema.

### Decisión

Migrar la capa de UI (activity_main.xml y layouts hijos) a Jetpack Compose. Las capas de negocio y protocolo de radio (RadioAudioService, Protocol, TxWhitelist, ArgentinaChannels) no se tocan.

En Compose los colores son parámetros de función, no herencia de tema. Un Canvas.drawCircle(color = Color(0xFF4FBD3B)) siempre dibuja ese verde. El problema del override desaparece por construcción.

### Arquitectura de la migración

- MainActivity.java → renombrar a MainActivityLegacy.java (referencia de binding)
- MainActivity.kt → nuevo entry point, extiende ComponentActivity
- MainViewModel.java → no se toca; se observa con observeAsState() desde Kotlin
- MemoriesAdapter.java → reemplazado por ChannelRow composable (3 canales, RecyclerView es overhead innecesario)
- AndroidManifest.xml → apunta al nuevo MainActivity.kt

### Versiones adoptadas

- Kotlin 1.9.22
- Compose Compiler 1.5.10
- Compose BOM 2024.02.02
- AGP 8.13.2 (sin cambio)

Compatibilidad verificada contra tabla oficial Kotlin a Compose Compiler.

### Consecuencias

**Positivas:**
- Colores CRT garantizados; ningun theme del sistema puede sobreescribir un Brush.radialGradient de Canvas
- Efectos imposibles en XML (scan lines, glow de fosforo, LED pulsante) se vuelven triviales
- Elimina toda la logica workaround de los Sprints 2-3
- Preview en Android Studio sin correr dispositivo
- MainUiState hace explicito todo lo que la UI necesita saber

**Negativas / riesgos:**
- Requiere Kotlin en el proyecto (hasta ahora solo Java) — posibles conflictos Gradle
- Tiempo de compilacion incrementa ~30s con Compose symbol processing
- OSMDroid en Compose requiere AndroidView (wrapper de View clasica); no es nativo pero es la unica opcion

**No cambia:**
- Protocol.java, RadioAudioService.java, TxWhitelist.java, ArgentinaChannels.java — intocables

---

## ADR-009 — Kotlin 2.2.0 con Compose Compiler bundled (descarte de 1.9.22)

**Fecha:** 2026-06-10
**Estado:** ACEPTADA
**Supera:** ADR-008 (versiones adoptadas)

### Contexto

ADR-008 documentó Kotlin 1.9.22 + Compose Compiler 1.5.10. Al ejecutar Sprint 4, `navigation-compose:2.7.7` y `lifecycle-viewmodel-compose:2.7.0` traen transitivamente `kotlin-stdlib:2.0.21`, que Gradle resuelve a 2.2.0. El compilador 1.9.x no puede leer metadata de stdlib 2.x → error "incompatible version of Kotlin. actual metadata version is 2.2.0, but the compiler version 1.9.0 can read versions up to 2.0.0."

### Decision

Upgradar Kotlin plugin a 2.2.0 y usar `org.jetbrains.kotlin.plugin.compose:2.2.0`. En Kotlin 2.0+ el Compose Compiler está bundled: se elimina el bloque `composeOptions { kotlinCompilerExtensionVersion }` del build.gradle.

### Consecuencias

**Positivas:**
- Elimina el conflicto de stdlib de raíz
- No hay tabla de compatibilidad Kotlin↔ComposeCompiler que mantener
- Kotlin 2.x tiene mejor inferencia de tipos para Compose lambdas

**Negativas:**
- Si en el futuro se agrega kapt para otra biblioteca, puede haber tensión con el modo K2 del compilador Kotlin 2.x

---

## ADR-010 — RadioServiceAccessor.java como bridge Kotlin↔Lombok

**Fecha:** 2026-06-10
**Estado:** ACEPTADA

### Contexto

`MainViewModel.java` y `RadioAudioService.java` usan Lombok (`@Getter`, `@Setter`, `@Builder`). Los métodos generados por Lombok son invisibles al compilador Kotlin cuando Lombok corre como `annotationProcessor` (no kapt). Intentar `kapt 'org.projectlombok:lombok'` + `compileOnly` rompe la compilación Java de otras clases que consumen Lombok (`Protocol.java`, `RadioAudioService.java`).

### Decision

Mantener Lombok con `implementation + annotationProcessor` (sin kapt). Crear `RadioServiceAccessor.java` — clase Java final con métodos estáticos que delegan a los métodos Lombok-generados. Kotlin llama a `RadioServiceAccessor` en lugar de llamar directamente a los getters/setters.

### Consecuencias

**Positivas:**
- Ningún cambio a las clases intocables (RadioAudioService.java, MainViewModel.java)
- La compilación Java y Kotlin coexisten sin conflicto
- Patrón estándar y reversible

**Negativas:**
- Un archivo de indirección extra por cada clase Java+Lombok que Kotlin necesite consumir
- Si se migran las clases Java a Kotlin en el futuro, RadioServiceAccessor queda obsoleto (borrar)
