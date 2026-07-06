# Auditoría de seguridad — Web comercial Baqueano

**Fecha:** 2026-07-05
**Alcance:** `docs/index.html` (publicado en `https://baqueano.netlify.app`), `_PROYECTO/web/index.html` (fuente), `_PROYECTO/web/README.md`, `.netlify/netlify.toml`, `.netlify/state.json`.
**Tipo de sitio:** estático, una sola página, sin backend propio, sin build step, sin JS de terceros más allá de Google Fonts. Superficie de ataque real: chica.

---

## Resumen ejecutivo

`docs/index.html` y `_PROYECTO/web/index.html` son **byte a byte idénticos** (diff vacío) — no hay drift entre fuente y publicado, eso está bien.

El hallazgo más importante no es una vulnerabilidad clásica, es un **cambio de diseño no documentado**: el formulario de "lista de espera" **no manda los datos a ningún backend ni servicio de terceros**. Al hacer submit arma un `mailto:` con los datos cargados en query string y le pasa la posta al cliente de correo del usuario (`window.location.href = 'mailto:...'`). Esto significa:

- No hay ningún endpoint, API key o webhook expuesto que alguien pueda abusar — porque no existe tal endpoint. Cero riesgo de exfiltración de la lista de espera vía un servicio de terceros mal configurado.
- Pero tampoco hay lista de espera real del lado servidor: cada "anotado" depende de que el visitante efectivamente aprieteenviar en su cliente de mail (Outlook/Gmail/etc.), y vos recibís cada entrada como un mail individual a `lukaz1979@gmail.com`. Si el navegador no tiene un cliente de mail configurado (muy común en desktop, y en mobile si no hay app de correo default), el submit no hace nada visible más que mostrar el cartel de éxito — el usuario cree que se anotó y en realidad no pasó nada. Esto es un problema de producto/conversión, no de seguridad, pero vale la pena que lo sepas porque cambia todo el análisis de "qué datos hay que proteger" (spoiler: ningún dato queda almacenado en ningún lado del lado tuyo excepto lo que llegue por mail).

No encontré XSS, no encontré secretos ni API keys expuestas, no encontré mixed content, no encontré referencias a la URL vieja de Netlify. Sí hay ausencia total de headers de seguridad (`_headers`/`netlify.toml` no configura nada), lo cual es una falta de hardening básica de bajo costo aunque de riesgo real bajo dado que no hay lógica server-side ni cookies de sesión que proteger.

---

## Hallazgos

### 1. [BAJO] Sin protección de framing / clickjacking — falta `X-Frame-Options` / `frame-ancestors`

**Ubicación:** no existe `docs/_headers` ni `_PROYECTO/web/_headers`, y `.netlify/netlify.toml` tiene `headers = []` (vacío, sin ninguna directiva).

**Problema:** Netlify no agrega por default ningún header de seguridad. Cualquiera puede embeber `https://baqueano.netlify.app` dentro de un `<iframe>` en un sitio de terceros.

**Escenario de explotación concreto:** un atacante arma una página con el iframe de Baqueano superpuesto con elementos invisibles (clickjacking clásico), y engaña al usuario para que haga click en algo que en el iframe corresponde al botón "Anotarme →" del formulario, o a cualquier link (WhatsApp, mailto, GitHub). El impacto real es bajo porque:
- El form no hace ninguna acción sensible del lado servidor (no hay POST a un backend, no hay estado de sesión, no hay acción destructiva) — como mucho el atacante logra que la víctima dispare un `mailto:` con datos que la víctima ya tipeó a mano, no hay "un click y listo" que robe algo de valor.
- No hay login, no hay cuenta de usuario, no hay dinero de por medio.

Igual, conviene cerrarlo porque es gratis (una línea de config) y elimina cualquier vector de UI redressing futuro si el sitio crece.

**Recomendación:** agregar un archivo `docs/_headers` (Netlify lee `_headers` desde la carpeta de publish) con:

```
/*
  X-Frame-Options: DENY
  Content-Security-Policy: frame-ancestors 'none'
  X-Content-Type-Options: nosniff
  Referrer-Policy: strict-origin-when-cross-origin
```

Ojo: como el `publish` en `.netlify/netlify.toml` apunta a la raíz del proyecto completo (`C:\Users\lukaz\...\MotoRFAR-MTTT`, ver hallazgo 5) y no a `docs/`, hay que confirmar primero cuál es el directorio de publish real configurado en el dashboard de Netlify antes de decidir dónde poner el `_headers` — si Netlify está sirviendo desde `/docs`, el archivo va en `docs/_headers`; si sirve desde la raíz, va en la raíz del repo.

---

### 2. [BAJO] Sin Content-Security-Policy

**Ubicación:** mismo lugar que el hallazgo anterior — no hay CSP en ningún lado.

**Problema:** sin CSP, si en el futuro se introduce alguna vulnerabilidad de inyección (aunque hoy no la hay, ver hallazgo de XSS más abajo), no hay una capa de defensa adicional que mitigue el impacto.

**Escenario de explotación concreto:** hoy, ninguno directo — es defensa en profundidad para el futuro, no un problema activo. El único script inline es el propio (`<script>` al final del archivo) y el único recurso externo es Google Fonts.

**Recomendación:** junto con el `_headers` del hallazgo 1, agregar algo conservador que no rompa nada:

```
Content-Security-Policy: default-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src https://fonts.gstatic.com; script-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'none'; frame-ancestors 'none';
```

(`'unsafe-inline'` en `style-src`/`script-src` es necesario porque todo el CSS y JS está inline en el HTML — sacarlo implicaría mover a archivos `.css`/`.js` externos, lo cual es un cambio de arquitectura, no de seguridad urgente).

---

### 3. [INFORMATIVO] Formulario de waitlist no manda datos a ningún backend — usa `mailto:` puro

**Ubicación:** `docs/index.html` líneas 1260-1285 (el `<form id="waitlist-form">`) y líneas 1398-1418 (el JS que lo procesa).

```js
form.addEventListener('submit', (e) => {
  e.preventDefault();
  const d = new FormData(form);
  const lineas = [ ... 'Nombre: ' + d.get('nombre'), 'Email: ' + d.get('email'), ... ];
  const asunto = encodeURIComponent('Baqueano — nuevo anotado en lista de espera');
  const cuerpo = encodeURIComponent(lineas.join('\n'));
  window.location.href = 'mailto:lukaz1979@gmail.com?subject=' + asunto + '&body=' + cuerpo;
  ...
});
```

**No es una vulnerabilidad** — de hecho es el diseño más seguro posible desde el punto de vista de superficie de ataque: no hay endpoint de terceros, no hay API key de Netlify Forms ni de ningún otro servicio (Formspree, Getform, etc.) expuesta en el cliente, no hay servidor propio que pueda ser atacado, no hay base de datos de waitlist que alguien pueda exfiltrar por un endpoint mal protegido. El único punto de contacto con el "backend" sos vos leyendo tu bandeja de entrada.

**Por qué lo anoto igual:** el `action=` del form nunca fue a Netlify Forms ni a un servicio externo — no hay `data-netlify="true"` ni ningún atributo asociado a Netlify Forms en el `<form>`. Esto descarta por completo el escenario que pediste evaluar (URL/API key de un servicio de terceros expuesta y abusable para spam o exfiltración) porque no existe ese servicio en absoluto. Riesgo: cero en ese frente.

**Efecto colateral no relacionado con seguridad (para que lo tengas en el radar):** en navegadores donde no hay cliente de mail configurado como default (común en desktop sin Outlook/Mail configurado, y en algunos navegadores mobile), `window.location.href = 'mailto:...'` no dispara nada visible. El usuario ve igual el cartel de "¡Casi listo!" (porque el `display:none` del form y el `display:block` del success corren siempre, sin chequear si el mailto realmente abrió algo) y cree que se anotó, cuando en realidad el mail nunca se compuso ni se envió. No es un hallazgo de seguridad, es un bug de UX/conversión — lo marco porque cambia la expectativa de "cuántos leads reales estás recibiendo" vs. "cuántos submits hizo la gente".

**Recomendación (opcional, producto no seguridad):** si en algún momento se quiere una lista de espera confiable sin depender de que el visitante tenga un cliente de mail configurado, la opción de menor superficie de ataque sigue siendo **Netlify Forms** (Netlify parsea el POST del lado suyo, no hace falta ninguna API key en el cliente, solo agregar `data-netlify="true"` al form y un input hidden `form-name`). Igual de "cero secretos expuestos" que el enfoque actual, pero con persistencia real. No es un fix de seguridad, es una mejora de producto — no lo hagas solo por este reporte.

---

### 4. [INFORMATIVO] Recursos de terceros — Google Fonts, sin SRI

**Ubicación:** `docs/index.html` líneas 8-10.

```html
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@300;400;500;600;700&family=DM+Mono:wght@300;400;500&family=DM+Sans:ital,wght@0,300;0,400;0,500;1,300&display=swap" rel="stylesheet" />
```

**Problema:** el `<link rel="stylesheet">` de Google Fonts no tiene `integrity` (SRI). Es un `<link>`, no un `<script>`, así que el riesgo real es más bajo que si fuera JS ejecutable — un CSS comprometido podría en el peor caso inyectar `@import` o `background: url(...)` con exfiltración de datos vía CSS (técnicas de "CSS injection exfiltration"), pero el vector de ataque requeriría que Google Fonts esté comprometido, algo extremadamente improbable dado que es infraestructura de Google con altísima disponibilidad y reputación.

**Nota técnica:** Google Fonts CSS dinámico (que sirve distinto CSS según user-agent) es una de las razones por las que Google **no soporta SRI** en ese endpoint — el hash cambiaría por navegador. No es negligencia del sitio, es una limitación del servicio de Google Fonts.

**Todo por HTTPS:** confirmado, no hay ningún `http://` en el archivo (grep completo, cero resultados) — no hay mixed content.

**Recomendación:** no accionable de forma práctica sin dejar de usar Google Fonts (por ejemplo, self-hosteando las fuentes, lo cual es una opción legítima de performance/privacidad pero excede el alcance de esta auditoría de seguridad). Si en algún momento se quiere eliminar la dependencia de un tercero por privacidad (Google Fonts loguea IPs de quien pide la fuente), self-hostear los `.woff2` es la vía, pero es una decisión de producto/privacidad, no un fix de seguridad urgente.

---

### 5. [MEDIO] `.netlify/netlify.toml` commiteado con `publish` apuntando a un path absoluto local de Windows

**Ubicación:** `C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\.netlify\netlify.toml`, línea 10:

```toml
[build]
publish = "C:\\Users\\lukaz\\OneDrive\\Escritorio\\MotoRFAR-MTTT"
```

**Problema:** este archivo está trackeado en git (confirmé con `git ls-files` — tanto `.netlify/netlify.toml` como `.netlify/state.json` están commiteados, no ignorados). El path absoluto expone:
- Tu nombre de usuario de Windows (`lukaz`) — dato mínimo, no crítico, pero es info personal en un repo que probablemente sea público en GitHub (el footer del sitio linkea a `github.com/lukaz2004/MotoRFAR`).
- Tu estructura de carpetas local (`OneDrive\Escritorio\MotoRFAR-MTTT`) — filtración de información de bajo impacto, típica de "no debería estar en el repo pero tampoco es grave si está".

Esto **no es un secreto** (no es una API key ni un token), pero es metadata de tu máquina que no tiene por qué estar en un repo público, y además es un archivo que normalmente `.netlify/` completo debería estar en `.gitignore` (es config local del CLI de Netlify, generada automáticamente al vincular el sitio, no algo para versionar).

`state.json` en cambio solo tiene el `siteId` (`a41e42c1-378c-4c57-8647-e0c9f2651642`), que es un identificador público sin valor de credencial — Netlify no expone nada explotable con solo el siteId sin las credenciales de tu cuenta.

**Escenario de explotación concreto:** ninguno directo y crítico — no hay forma de que alguien tome control del sitio Netlify solo con el `siteId` o con tu username de Windows. Es pura higiene de repo, no un vector de ataque real.

**Recomendación:**
1. Agregar `.netlify/` al `.gitignore` (falta esa línea; hoy el `.gitignore` no lo contempla).
2. Sacar `.netlify/netlify.toml` y `.netlify/state.json` del tracking de git (`git rm -r --cached .netlify/`) en un commit separado, para que futuras regeneraciones de esos archivos (que van a seguir teniendo tu path local) no vuelvan a colarse.
3. No hace falta rotar nada — no hay ninguna credencial ahí, solo lo mencionado arriba.

---

### 6. [BAJO] Ausencia de rate limiting / anti-spam en el "formulario" — no aplica

Descartado como hallazgo real: como el formulario no pega contra ningún backend (ver hallazgo 3), no existe superficie para spam automatizado del lado servidor — como mucho, alguien podría scriptear aperturas masivas de `mailto:` en su propio navegador, lo cual solo le llena la bandeja de *él*, no la tuya, porque el envío del mail requiere acción humana explícita en su cliente de correo. No hay nada que rate-limitear porque no hay endpoint.

---

### 7. Verificaciones que dieron OK (sin hallazgos)

- **XSS:** no hay ningún `innerHTML`, `outerHTML`, `document.write`, ni interpolación de datos de usuario en el DOM. El único JS que toca el DOM son `classList.toggle`, `style.display`, y el armado de un string para `mailto:` (que va a la barra de direcciones del cliente de mail, no se renderiza como HTML en ningún lado). No hay lectura de `location.search` ni de ningún parámetro de URL reflejado en la página. Sitio 100% estático, sin superficie de inyección.
- **Mixed content:** cero URLs `http://` en el archivo — todo `https://` o relativo.
- **URL vieja de Netlify (`gorgeous-taffy-xxxx.netlify.app` u otra):** no aparece ninguna referencia, ni en `docs/index.html` ni en `_PROYECTO/web/index.html`. Todos los links externos (`GitHub`, `wa.me`, `mailto:`) apuntan a destinos correctos y no hay ningún link absoluto hardcodeado al dominio del sitio mismo (todos los anchors internos son relativos, `#seccion`).
- **Secretos/API keys en el HTML/JS:** ninguno. No hay tokens, keys, ni credenciales de ningún servicio.
- **Consistencia fuente/publicado:** `docs/index.html` y `_PROYECTO/web/index.html` son idénticos byte a byte.
- **Rel en links externos:** todos los `target="_blank"` (GitHub, WhatsApp, kv4p-ht) llevan `rel="noopener"`, que previene el ataque clásico de `window.opener` reverse tabnabbing. Bien implementado.

---

## Priorización para vos, mañana

1. Confirmá en el dashboard de Netlify si el `publish directory` real es `docs/` — a partir de ahí, agregá el archivo `_headers` en esa misma carpeta con las 4 líneas del hallazgo 1+2 (X-Frame-Options, CSP, X-Content-Type-Options, Referrer-Policy). 5 minutos, cero riesgo de romper nada.
2. Sacá `.netlify/` del control de versiones (`.gitignore` + `git rm -r --cached .netlify/`) — housekeeping, no hay nada urgente pero es gratis hacerlo ahora que lo tenés fresco.
3. El formulario de waitlist está bien como está en términos de seguridad (cero superficie de ataque de terceros) — si en algún momento querés mejorar la tasa de conversión real (gente que efectivamente queda anotada aunque no tenga cliente de mail configurado), ahí sí evaluá Netlify Forms, pero eso es decisión de producto, no de seguridad, y no es parte de esta auditoría.

No hay ningún hallazgo CRÍTICO ni ALTO. La superficie de ataque de este sitio es genuinamente chica porque no hay backend, no hay base de datos, no hay autenticación, y el único "formulario" en realidad delega todo al cliente de correo del visitante.

---

## Estado de correcciones — 2026-07-06

- ✅ **Hallazgo 1+2 (headers) — RESUELTO.** Creado `docs/_headers` (y su fuente `_PROYECTO/web/_headers`, mismo patrón que `index.html`) con `X-Frame-Options: DENY`, `Content-Security-Policy: frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`. Se asumió `docs/` como directorio de publish real (confirmado por fetch en vivo de `baqueano.netlify.app` matcheando `docs/index.html`) — **falta confirmar en el dashboard de Netlify** que efectivamente sirve desde ahí y no desde otra config.
- ✅ **Hallazgo 5 (`.netlify/` commiteado) — RESUELTO.** `.netlify/` agregado a `.gitignore` y sacado del tracking (`git rm -r --cached`). Los archivos siguen en disco, solo dejaron de versionarse.
- — **Hallazgo 3 (mailto) y hallazgo 4 (Google Fonts sin SRI):** sin acción, tal como recomendaba el propio reporte (no son vulnerabilidades explotables, son notas informativas/de producto).
- ⬜ **Pendiente:** confirmar en el dashboard real de Netlify cuál es el `publish directory` configurado, para asegurar que `_headers` está en la carpeta correcta.
