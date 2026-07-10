# Auditoría de seguridad — Mapas offline por provincia (pipeline de generación + manifest)

**Fecha:** 2026-07-09
**Alcance:** `_PROYECTO/mapas_offline/provincias.json` (manifest commiteado), GitHub Release `mapas-v1` de `lukaz2004/MotoRFAR` (24 assets `.map` públicos), pipeline de generación descripto en `docs/superpowers/specs/2026-07-08-mapas-offline-por-provincia-design.md`, e historial de git del directorio `_PROYECTO/mapas_offline/`. Primera auditoría de este subsistema (no hay auditoría previa).
**Fuera de alcance:** el sub-proyecto 2 (ventana in-app de descarga) — **no existe código todavía**, así que los puntos 3/4 de abajo son revisión de diseño/requisitos, no revisión de código.
**Metodología:** revisión manual del manifest + historial completo de git de la carpeta relevante + consulta directa a la API de GitHub (`gh release view --json assets,body,...`) para verificar el estado real del release (no asumido desde el manifest). Solo lectura — no se modificó código, manifest, ni el release.

---

## Resumen ejecutivo

No hay nada CRÍTICO ni explotable hoy — es lógico, porque todavía no existe ningún consumidor (la app no descarga estos archivos, el sub-proyecto 2 no está construido). El riesgo real de este subsistema es **de diseño hacia adelante**: el manifest que se está fijando ahora, en `git`, va a ser la base de la que dependa el sub-proyecto 2, y hoy le falta la pieza más barata de integridad — un hash por archivo — justo cuando es gratis agregarla (antes de que exista un downloader en producción que dependa del schema actual).

El hallazgo central es simple: **`provincias.json` solo tiene `size_bytes`, no un hash**. Un tamaño en bytes no detecta corrupción de contenido (mismo tamaño, bytes distintos) ni un asset reemplazado con contenido malicioso del mismo tamaño. Lo llamativo es que **GitHub ya calcula el SHA-256 de cada asset del release y lo expone gratis vía API** (campo `digest`, verificado con `gh release view mapas-v1 --json assets` en esta auditoría) — no hace falta ni recalcular nada, solo copiarlo al manifest. Es la corrección más barata posible para el problema más importante de este subsistema.

Segundo punto real: cuando se construya el sub-proyecto 2, **HTTPS por sí solo no alcanza** como garantía de integridad — protege el tránsito, no protege contra un asset del propio release que haya sido corrompido, reemplazado, o mal generado antes de subirse. Esto importa doblemente acá porque el archivo descargado no es un dato inerte: lo va a parsear un parser binario (Mapsforge vía OSMDroid) del lado del teléfono, y este proyecto ya tiene, en su propia auditoría de la app, antecedentes reales de parsers que no validan longitud de buffer antes de leer (`AUDITORIA_SEGURIDAD_APP.md`, MEDIO-2, `PositionParser`). No hay motivo para asumir que el sub-proyecto 2 va a ser distinto si no se le pide explícitamente verificación de hash antes de parsear.

Todo lo demás — prolijidad del repo, publicidad intencional del release, consistencia manifest↔assets reales — verificó OK.

---

## Hallazgos

### CRÍTICO

Ninguno.

---

### ALTO

#### ALTO-1 — El manifest solo registra tamaño en bytes, no hash de contenido; GitHub ya calcula el hash y no se está aprovechando

**Archivo:** `_PROYECTO/mapas_offline/provincias.json` (cada entrada tiene `size_bytes` pero no `sha256` ni ningún otro campo de integridad).

**Problema:** un tamaño en bytes es una verificación de integridad muy débil para un blob binario grande que después va a alimentar un parser (Mapsforge). No detecta:
- Una descarga corrupta con el mismo tamaño final (bit flip, TCP corrupto no detectado por el checksum de capa de transporte, escritura a disco interrumpida que deja el archivo truncado pero con padding hasta el tamaño esperado, etc.).
- Un asset reemplazado en el release (accidental o deliberado) que mantenga casualmente o deliberadamente el mismo tamaño.

**Verificado en esta auditoría — la corrección es prácticamente gratis:** corrí `gh release view mapas-v1 --repo lukaz2004/MotoRFAR --json assets` y **cada uno de los 24 assets ya trae un campo `digest` con el SHA-256 calculado por GitHub** (ejemplo real: `buenos-aires.map` → `digest: sha256:ee023d3e7d3e6bfab825bc613f4852f7300d45925ef98343330d14a1ad970a4a`). GitHub calcula y expone este hash automáticamente al subir cualquier asset — no hace falta instalar nada ni recalcular localmente, solo leer el campo y copiarlo al manifest.

**Recomendación (antes de que arranque sub-proyecto 2):**
1. Agregar un campo `sha256` a cada entrada del manifest, poblado con el `digest` que ya devuelve `gh release view --json assets` (quitando el prefijo `sha256:`).
2. El futuro downloader del sub-proyecto 2 debe verificar ese hash contra el archivo descargado **antes** de pasarlo a OSMDroid/Mapsforge, y rechazar/reintentar si no coincide — no confiar solo en el código de estado HTTP 200 ni en el tamaño.
3. Es un cambio de schema del manifest, no del pipeline de generación — no requiere volver a correr Osmosis ni tocar los `.map` ya subidos.

---

#### ALTO-2 — Requisito de diseño pendiente para el sub-proyecto 2: verificación de integridad post-descarga, independiente de HTTPS

**Contexto:** el sub-proyecto 2 (ventana in-app) todavía no tiene código — esto es una revisión de requisitos de diseño, no de una implementación existente.

**Problema:** el diseño actual (ver spec, sección "Pasos del pipeline") no dice nada sobre cómo el futuro downloader va a validar lo que descarga. HTTPS (que va a usar por default, `network_security_config.xml` ya bloquea cleartext globalmente según `AUDITORIA_SEGURIDAD_APP.md`) protege la integridad **en tránsito** — evita que alguien en el medio (una AP WiFi hostil, un portal cautivo malicioso, un ISP entrometido) modifique los bytes mientras viajan. **No protege** contra:
- Un asset del release que ya estaba corrompido o alterado en el origen antes de que empezara la descarga (error de subida, compromiso de la cuenta `lukaz2004`, reemplazo intencional o accidental del asset — ver MEDIO-1 sobre reemplazo de assets).
- Una descarga interrumpida/truncada que el cliente HTTP no detecta como error (ej. conexión cortada a mitad de un archivo de 80MB en una red mala en zona sin cobertura, que es exactamente el escenario de uso real de esta app).

Esto importa más de lo normal en este proyecto puntual porque el consumidor final del archivo es **un parser binario** (Mapsforge, vía `osmdroid-mapsforge`). Un `.map` corrupto o malicioso no es solo "el mapa no carga" — es superficie de ataque contra el parser mismo (igual categoría de riesgo que ya se encontró y corrigió en este proyecto para el parser APRS, ver `AUDITORIA_SEGURIDAD_APP.md` MEDIO-2: acceso a índice de buffer antes de validar longitud).

**Recomendación para el diseño del sub-proyecto 2 (a incorporar en el próximo spec, no en este):**
1. Verificar el hash SHA-256 del archivo descargado contra el manifest (ver ALTO-1) antes de entregárselo a OSMDroid/Mapsforge — si no coincide, borrar el archivo y no cargarlo.
2. Descargar a un archivo temporal y solo mover/renombrar al path final después de que el hash verifique — evita que un archivo a medio descargar (o con el hash aún sin validar) quede en el lugar donde el resto de la app espera encontrar un `.map` válido.
3. Envolver la inicialización del parser Mapsforge en un `catch` explícito (mismo patrón defensivo que ya usa `RadioAudioService.handleAx25Packet()` para el parser APRS) — un hash coincidente reduce mucho el riesgo, pero no elimina la categoría "archivo válido en tránsito pero con un bug de parser que igual lo tumba".
4. No hace falta pinning de certificado TLS para este caso — el activo protegido es un archivo de mapas público, no una credencial; el hash pinned en el manifest (git-tracked) ya cubre el escenario de "asset comprometido en origen", que es el que HTTPS no cubre.

---

### MEDIO

#### MEDIO-1 — Los assets de un GitHub Release se pueden reemplazar sin cambiar la URL; el manifest en git (con hash) es el ancla de integridad real, no la URL

**Problema:** GitHub permite borrar y volver a subir un asset de un release manteniendo el mismo nombre de archivo — la URL de descarga (`.../releases/download/mapas-v1/buenos-aires.map`) no cambia, pero el contenido, el `id` interno del asset, y el `digest` sí. GitHub no mantiene un historial/diff de versiones anteriores de un asset (a diferencia del código, que sí tiene historial en git) — si alguien con permiso de escritura en el repo reemplaza un `.map` mañana, no queda un registro nativo de "esto cambió" visible para un consumidor externo que solo mire la URL.

Esto es precisamente lo que ya se observó en esta sesión (ver `NEXT_SESSION.md`, cierre 2026-07-09): existía un release `mapas-v1` previo de una sesión de chat anterior, y el pipeline de esta sesión generó su propio set de 24 archivos en paralelo con tamaños ligeramente distintos (~5-10%) antes de detectarlo y descartar el duplicado — confirmado por el dueño del proyecto como no maliciosos, pero es el ejemplo concreto de que **dos generaciones legítimas y no maliciosas del mismo pipeline ya produjeron binarios distintos bajo el mismo nombre de asset**. Un actor malicioso con las mismas credenciales de escritura podría hacer lo mismo a propósito, sin que la URL fija en el manifest lo detecte.

**Por qué esto no es ALTO:** requiere que el atacante ya tenga permisos de escritura sobre el repo/release (`lukaz2004/MotoRFAR`) — si eso pasa, hay problemas mayores que este manifest. No es un vector de ataque remoto nuevo, es una propiedad estructural de cómo funciona GitHub Releases que hay que tener presente al diseñar la verificación.

**Recomendación:** el manifest (`provincias.json`), al vivir en git, sí tiene historial real, tamper-evidence vía revisión de PR, y (una vez aplicado ALTO-1) un hash fijado en un commit específico. Esa es la ancla de integridad correcta — el sub-proyecto 2 debe verificar contra el hash del manifest en el commit que la app tiene embebido/descargado, no confiar en que "la URL es la misma, entonces el contenido es el mismo". Si en el futuro se regenera algún `.map` (ej. actualización de datos OSM), el flujo correcto es: regenerar → resubir asset → actualizar hash en el manifest → commit — nunca resubir sin actualizar el hash.

---

### BAJO

#### BAJO-1 — Atribución ODbL presente en el release de GitHub, pero ausente dentro de la app

**Archivo:** `KV4PHT/app/src/main/java/ar/motorfar/app/ui/AboutActivity.kt:152-158` (pantalla "Acerca de / Licencias")

**Problema:** los datos de OpenStreetMap están licenciados ODbL, que requiere atribución ("© contribuyentes de OpenStreetMap") dondequiera que se use/muestre esa data — no solo en la fuente de origen. Verificado que el **body del GitHub Release sí tiene la atribución correcta** ("licencia ODbL — © colaboradores de OpenStreetMap"), pero eso es visible solo para quien entre a la página del release en GitHub, no para el usuario final de la app. La pantalla `AboutActivity` (la que efectivamente ve el usuario) hoy solo lista `OSMDroid 6.1.18 — Apache 2.0` (licencia de la librería de código, no de los datos) — no hay mención a OpenStreetMap/ODbL como fuente de los propios datos de mapa.

**No es una vulnerabilidad de seguridad**, es una brecha de cumplimiento de licencia — el propio `CLAUDE.md` del proyecto ya trata este tipo de atribución como innegociable para GPL (kv4p/Vance Vagell), consistente con tratarla igual acá.

**Recomendación:** cuando se construya el sub-proyecto 2 (o antes, es independiente), agregar una línea a `AboutActivity.kt` del estilo: `Datos de mapa © colaboradores de OpenStreetMap (ODbL) — openstreetmap.org/copyright`. Cambio de una línea de texto, sin impacto funcional.

---

## Verificado OK (para que quede constancia)

1. **Los 24 assets reales del release coinciden byte a byte en tamaño con el manifest.** Comparé cada `size_bytes` de `provincias.json` contra el `size` real devuelto por `gh release view mapas-v1 --json assets` para las 24 provincias — coinciden exactamente en los 24 casos (ej. Buenos Aires: manifest `82419677` = release `82419677`). El manifest no quedó desincronizado del release real al momento de esta auditoría.

2. **Repo limpio — nada del pipeline quedó adentro por error.** `git log --stat -5 -- _PROYECTO/mapas_offline` muestra un único commit (`47ecfc8`) que agrega solo `provincias.json` (226 líneas, un archivo). `git status` no muestra nada pendiente relacionado (el único untracked es `imegenes baqueano edit/`, sin relación). Búsqueda de `.jar`/`.map`/`.pbf` en todo el árbol del repo no encontró nada del pipeline (`mapsforge-map-writer.jar`, `build_poly.py`, `simplify_poly.py`, extractos `.osm.pbf`, ni `.map` generados) — el jar suelto mencionado en el cierre de sesión efectivamente nunca llegó a commitearse. Búsqueda en el historial completo de `git log --all -p` sobre esa carpeta por patrones de token de GitHub (`ghp_`, `gho_`, `github_pat_`, `ghs_`) no encontró coincidencias — no quedó credencial de `gh` CLI expuesta en el historial.

3. **Publicidad del release (`isDraft: false`) es intencional, no un descuido.** El propio design doc (`docs/superpowers/specs/2026-07-08-mapas-offline-por-provincia-design.md`, "Fuente de datos"/pipeline paso 5) elige explícitamente GitHub Releases público como hosteo gratuito para estos archivos — es la decisión de arquitectura, no un release que se olvidó de poner en draft. Revisé el `body` completo del release vía `gh release view --json body` — es texto descriptivo de origen de datos y formato, sin URLs ni datos sensibles fuera de lugar, y **ya incluye la atribución ODbL correcta a nivel de release** (aunque no a nivel de app, ver BAJO-1). Los 24 assets listados son exactamente los 24 `.map` esperados (23 provincias + CABA) — no hay ningún asset extra ni inesperado (ej. ningún `.jar`, `.pbf`, ni archivo de configuración colado).

---

## Resumen de acciones recomendadas (por prioridad)

1. **(ALTO-1)** Agregar campo `sha256` a cada entrada de `provincias.json`, tomado directo del `digest` que ya devuelve `gh release view mapas-v1 --json assets` — cero recálculo necesario, es la corrección más barata posible al hallazgo más importante.
2. **(ALTO-2 / MEDIO-1)** Cuando se escriba el spec del sub-proyecto 2: incluir como requisito explícito de diseño la verificación de hash post-descarga antes de pasar el archivo a OSMDroid/Mapsforge, descarga a archivo temporal + move atómico, y un `catch` explícito alrededor de la inicialización del parser Mapsforge.
3. **(BAJO-1)** Agregar una línea de atribución ODbL/OpenStreetMap en `AboutActivity.kt` — no bloqueante, pero barato y ya es una convención que el proyecto respeta para GPL.

Nada de esto bloquea seguir con el sub-proyecto 2. Es mucho más barato corregir el schema del manifest ahora (un archivo JSON de 226 líneas, sin consumidores todavía) que después de que la app dependa de él.
