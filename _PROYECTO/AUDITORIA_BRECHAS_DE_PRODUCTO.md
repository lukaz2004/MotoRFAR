# Auditoría de brechas de producto — Baqueano

**Fecha:** 2026-07-09
**Alcance:** NO es auditoría de código ni de vulnerabilidades (eso ya existe en
`AUDITORIA_SEGURIDAD_*.md`). Esto es estructural/producto/negocio/proceso — cosas
que siguen siendo ciertas aunque cada línea de código esté perfecta. Encargo
explícito: "asumí que hay un defecto de diseño o algo importante que no vimos —
encontralo."
**Método:** lectura completa de `CLAUDE.md`, `05_VISION.md`, `01_CONTRATOS.md`,
`PENDIENTES.md`, `NEXT_SESSION.md` (todas las sesiones), `00_MAPA_MAESTRO.md`,
`02_AUDITORIA.md`, `AUDITORIA_SEGURIDAD_WEB.md`, `AUDITORIA_SEGURIDAD_FIRMWARE.md`,
`docs/index.html` (web comercial en vivo), grep dirigido sobre todo `_PROYECTO/`
y verificación de git remote/gitignore/historial del keystore. No se tocó nada.

---

## 🔴 BLOQUEANTE

### B1 — El producto casi no tiene ninguna verificación en las condiciones reales para las que existe
La cadena Man-Down/Emergencia es la razón de ser del producto (`05_VISION.md`
línea 8-11: "si hay un percance... el sistema protege solo"). Repasando cada
"✅ probado"/"✅ verificado" en `PENDIENTES.md` y `NEXT_SESSION.md`:

| Ítem clave | Qué dice "✅ probado" | Contra qué se probó en realidad |
|---|---|---|
| Man-Down (countdown, alerta, notificación) | `NEXT_SESSION.md:90` "Verificado en emulador con inyección de sensor real" | **Emulador**, acelerómetro inyectado por software. Nunca un golpe real. |
| Man-Down en background (fix del Service) | `NEXT_SESSION.md:78-95` | Mismo emulador. `PENDIENTES.md:33-34` explícitamente pendiente: botón "ESTOY BIEN" tocado a mano y alerta de otro integrante con la app cerrada — **nunca hecho, ni en emulador**. |
| Animaciones de Emergencia (PTT rojo, etc.) | `PENDIENTES.md:44-47` | Disparadas tocando el canal a mano, **no** por el flujo automático de 2s hold con radio conectada — eso "requiere equipo físico" y no se hizo. |
| Whitelist TX (firmware rechaza freq fuera de whitelist) | `00_MAPA_MAESTRO.md:29`, `02_AUDITORIA.md:13,45` — 🟡 desde 2026-06-23 **hasta hoy (07-09), sigue igual** | Nunca se probó con un SA818 físico transmitiendo. Es código auditado, no RF verificado. |
| Failsafe WiFi (deadman 400ms) | `NEXT_SESSION.md:392`, `PENDIENTES.md:210-212` | Nunca probado — requiere SA818 físico, sigue en la lista de pendientes desde FW-3a (23 de junio). |
| Alcance/confiabilidad RF real | — | **Cero menciones en todo el proyecto** de una prueba de distancia, propagación o dos radios físicos independientes hablando entre sí en el campo. No existe ese dato en ningún doc. |
| Prueba en dispositivo físico | Huawei P9, "probado... en mano, no andando" (`NEXT_SESSION.md:37,67-71`) | Un solo teléfono, en la mano, quieto. Nunca en una moto en movimiento, nunca con vibración real, nunca con un segundo equipo. |

**Conclusión concreta:** a la fecha, el 100% de la validación de la función de
seguridad central del producto (Man-Down/Emergencia) es emulador + un teléfono
en mano. **Cero pruebas con dos radios físicos independientes, cero pruebas en
vehículo en movimiento, cero pruebas de alcance RF real.** El propio historial
lo admite en cada cierre de sesión ("falta probar en dispositivo físico",
"requiere moto real") pero nunca se convierte en un ítem que bloquee nada —
sigue como bullet suelto sesión tras sesión desde el 04/07 hasta el 09/07 sin
resolverse. Para un producto vendido como "si te caés, el grupo lo sabe solo"
(`docs/index.html:1154`), esto es la brecha más grande del proyecto.

---

### B2 — El testeo real que falta (RF, dos equipos) requiere exactamente el escenario de riesgo legal que el proyecto no analizó
`PENDIENTES.md:252-259` marca correctamente la homologación ENACOM como
bloqueante **antes de vender**, y aclara que la lista de espera hoy no vende y
no es urgente. Correcto hasta ahí. Pero el propio roadmap técnico (FW-2, FW-3a
`--open-rx`+failsafe, FW-4) está bloqueado en "SA818 físico" desde hace más de
dos semanas (`00_MAPA_MAESTRO.md:29-32`), y una prueba real de "dos radios
hablando" (necesaria para B1) requiere casi con certeza un segundo equipo
transmisor en manos de otra persona — o al menos, transmitir con un segundo
equipo fuera del control exclusivo del desarrollador. `NEXT_SESSION.md:70-71`
ya pregunta "si hay más equipos viejos dando vueltas, valdría la pena repetir
el smoke test ahí" — es decir, el propio proyecto está considerando expandir
las pruebas a más gente/equipos.

**Ningún documento define la línea entre "pruebas personales de I+D" y
"distribución de un transmisor sin homologar a un tercero"** (que es
exactamente el supuesto que dispara Res. 729/80 + Ley 24.240 según el propio
`PENDIENTES.md:252-259`). No hace falta vender para tener exposición: prestarle
o regalarle una unidad a un amigo para el smoke test de RF real —el próximo
paso lógico del roadmap— ya la tiene. Esto no está en ningún checklist.

---

## 🟠 URGENTE

### U1 — Backup del keystore de firma: sigue sin hacerse, y es la única copia
`NEXT_SESSION.md:194-203` (cierre 2026-07-05): *"el backup físico del `.jks`
en un segundo lugar (fuera de esta PC) queda como acción suya, no está hecho
todavía."* `PENDIENTES.md:172-174` (última edición 2026-07-07, **dos días
después**) repite la misma alerta sin marcarla resuelta: *"Hacer backup de
`baqueano-release.jks` YA... sin él no se puede volver a firmar/actualizar la
app en Play Store. Las contraseñas están solo en `keystore.properties` en este
disco."* Verificado con `git log` + `.gitignore`: el `.jks` nunca estuvo en git
(correctamente ignorado por `**/*.jks`), así que **la única copia que existe
en el universo es un archivo en esta laptop**, sin backup en ningún otro lugar,
confirmado a la fecha de este reporte. Si el disco muere, la app Baqueano
pierde para siempre la capacidad de actualizarse en Play Store bajo la misma
identidad de firma — habría que republicar como app nueva, perdiendo reviews,
instalaciones y confianza acumulada.

### U2 — Ningún punto único de falla humano/de cuentas está documentado
Repo en GitHub (`lukaz2004/MotoRFAR`), sitio en Netlify (`baqueano.netlify.app`,
`AUDITORIA_SEGURIDAD_WEB.md:129` confirma cuenta propia con `siteId` único),
dominio, y el propio código fuente/`_PROYECTO/` — todo vive en una sola laptop
bajo una sola cuenta de una sola persona. `05_VISION.md:23-25` documenta
explícitamente que es un proyecto de una sola persona, sin financiamiento
("a pulmón", "LuKaZ tiene 47 años... no quiere depender de una jubilación").
Ningún archivo del proyecto (`_PROYECTO/*.md` completo, grep dirigido sin
resultados) menciona: qué pasa con las cuentas GitHub/Netlify/dominio si
LuKaZ no puede seguir, un maintainer de respaldo, un plan de continuidad para
los usuarios que ya dependen de Man-Down como red de seguridad, o siquiera un
canal de soporte más allá de un Gmail personal (`lukaz1979@gmail.com`, visto
en el `mailto:` de la web). Para un producto de seguridad física, la ausencia
total de este tema en cualquier documento es la brecha "cómo nadie lo notó"
más clara del pedido.

---

## 🟡 IMPORTANTE

### I1 — La promesa de marketing es más fuerte que lo que el producto puede garantizar para un usuario solo
`docs/index.html:649`: *"Sin internet, sin licencia, **nunca solo**."*
`docs/index.html:1154`: *"Si caés, el grupo **lo sabe solo**."*
`05_VISION.md:8-11`: *"si hay un percance... el sistema protege solo."*

Estas tres promesas —una en el hero de la web, dos en el corazón de la
propuesta de valor— asumen que **siempre hay un integrante de grupo real,
cerca, con el equipo prendido y sintonizado**, escuchando el canal de
Emergencia. El perfil de comprador temprano más probable (una sola unidad
comprada, sin nadie más en la familia/grupo con Baqueano todavía) es
precisamente el caso donde esta promesa es falsa: si sos el único con el
equipo, no hay "grupo" que reciba la alerta de Man-Down. Grep completo sobre
`_PROYECTO/` y `docs/index.html` de "911", "SOS satelital", "servicio de
emergencia", "llamada de emergencia": **cero resultados**. No existe ningún
puente hacia la red de emergencias convencional (celular/satelital) como
respaldo — el sistema depende 100% de RF VHF de corto alcance y de que otro
humano con el mismo equipo esté escuchando. La única mención de un límite a
esta promesa está en el FAQ (`docs/index.html:1222`, sobre el mapa: "el mapa
solo funciona para quienes tienen el kit completo"), pero el FAQ **no tiene
ninguna pregunta sobre "¿y si soy el único con el equipo?"** — justo el
escenario más probable en el lanzamiento real (pocas unidades vendidas al
principio, sin masa crítica de usuarios cercanos).

### I2 — El protocolo UDP sigue sin autenticación de aplicación real; el mismo hueco fue encontrado dos veces
`PENDIENTES.md:54-57` y `PENDIENTES.md:65-74` documentan el mismo hallazgo
estructural en dos sesiones de auditoría separadas (2026-07-06 y 2026-07-09):
cualquier dispositivo que gane el único slot del SoftAP puede mandar comandos
crudos sin autenticarse, incluyendo cambiar SSID/clave y dejar al dueño
bloqueado del equipo (recuperable solo reflasheando por USB, según
`AUDITORIA_SEGURIDAD_FIRMWARE.md:64`). Ambas veces se concluyó "no es un fix
de una sesión, necesita rediseño de protocolo (token/HMAC)" — y ambas veces
quedó anotado sin una sesión dedicada agendada. Esto no es un bug de código
nuevo: es una decisión de arquitectura de seguridad pendiente que el proyecto
ya identificó dos veces y todavía no priorizó ni calendarizó.

### I3 — El fundamento legal completo del producto es una sola resolución administrativa, nunca puesta en duda
Todo el proyecto —whitelist en firmware, "sin licencia, sin trámites"
(`CLAUDE.md:9`), la propuesta de valor entera— descansa en que la
Resolución 5/2015 (M.T.T.T.) siga vigente con las mismas 3 frecuencias libres.
Grep dirigido sobre `05_VISION.md`, `PENDIENTES.md`, `NEXT_SESSION.md`,
`00_MAPA_MAESTRO.md` y el resto de `_PROYECTO/` con variantes de "deroga",
"revoca", "reinterpreta", "riesgo regulatorio", "cambia la resolución":
**cero resultados en cualquier archivo del proyecto.** No se pide acá
investigar derecho de telecomunicaciones argentino — el punto es que el
proyecto nunca dejó registrado, ni como riesgo aceptado ni como plan de
contingencia, que la legalidad completa del negocio depende de una decisión
administrativa que el M.T.T.T. podría modificar o dejar sin efecto sin aviso.
Comparar con la homologación ENACOM, que sí está tratada como riesgo real y
documentado (`PENDIENTES.md:251-259`) — la Res. 5/2015 no recibió el mismo
tratamiento en ningún lugar.

---

## ⚪ A TENER EN CUENTA

### A1 — El waitlist actual no genera un dato confiable de demanda real
`AUDITORIA_SEGURIDAD_WEB.md:88` (hallazgo 3, ya documentado como bug de
producto no de seguridad): el formulario arma un `mailto:` client-side: si el
visitante no tiene cliente de correo configurado (común en desktop/mobile), el
submit no manda nada pero igual muestra "¡Casi listo!" — el usuario cree que
quedó anotado y no pasó nada. Esto significa que **el tamaño real de la lista
de espera es menor al que aparenta**, lo cual importa para decidir cuándo
vale la pena arrancar el trámite ENACOM (B2) — la señal de demanda que
alimenta esa decisión está subestimando la conversión real, no sobreestimando.

### A2 — Sin pantalla de historial de rutas ni exportación GPX
`PENDIENTES.md:50-51,113-114`: solo se guarda la última sesión por alias,
"Borrar ruta guardada" borra todo (no por viaje). No es de seguridad, pero es
una limitación de producto real para el caso de uso "trabajo rural" /
"trekking recurrente" que la Visión dice priorizar (`05_VISION.md:13-15`) —
nadie puede reconstruir un recorrido de una semana atrás.

### A3 — Onboarding todavía dice "MotoRFAR"/"GRUPO" en vez de "Baqueano"/"PRINCIPAL"
`PENDIENTES.md:28-32`: hallazgo del propio 07-07, sin resolver — quedó afuera
del rebrand. Primera pantalla que ve un usuario nuevo (Términos + Alias) sigue
con el nombre viejo del proyecto. Bajo riesgo pero es la primera impresión.

---

## Resumen de prioridad

| Nivel | Ítems |
|---|---|
| 🔴 Bloqueante | B1 (cero validación real de la función de seguridad central), B2 (el próximo paso de testeo RF choca con un vacío legal no analizado) |
| 🟠 Urgente | U1 (keystore sin backup, sigue así 4+ días después de la alerta), U2 (cero plan de continuidad/bus-factor) |
| 🟡 Importante | I1 (promesa de marketing "nunca solo" no sostenible para usuario solo, sin fallback a emergencias reales), I2 (protocolo UDP sin auth, encontrado 2 veces sin agendar fix), I3 (fundamento legal = 1 resolución, riesgo nunca registrado) |
| ⚪ A tener en cuenta | A1 (waitlist subestima demanda real), A2 (sin historial de rutas/GPX), A3 (onboarding con nombre viejo) |
