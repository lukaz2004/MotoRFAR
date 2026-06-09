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
