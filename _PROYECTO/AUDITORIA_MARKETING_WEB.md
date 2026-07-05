# Auditoría de marketing — web comercial Baqueano (2026-07-05)

> Rehecha desde cero porque la auditoría original (sesión de otro chat, fines de junio)
> nunca se persistió a disco — ver `NEXT_SESSION.md`, alerta del 2026-07-02.

## Hallazgo principal antes de escribir una sola línea

Había **dos copias divergentes** de la web:
- `docs/index.html` — la que Netlify sirve en vivo. Ya tenía una auditoría de marketing
  sólida hecha en junio (commits `c1af6e6`, `96755be`, `dd4b1e1`): logo, capturas reales
  de la app, sección Man-Down, balizas de emergencia, FAQ, especificaciones, lista de
  espera. Ya usaba lenguaje amplio ("motos, 4×4, trekking, campo") — coincide con la
  regla de alcance anotada hoy en `CLAUDE.md`.
- `_PROYECTO/web/index.html` — un "rediseño visual" (tipografía Rajdhani, estética CRT
  verde) hecho más tarde que **no llevaba ese contenido**: sin logo, sin capturas, tono
  de "proyecto en desarrollo" con barras de progreso internas, tagline limitado a
  "motos y 4x4".

Arrancar literalmente de cero hubiera tirado el trabajo bueno que ya existía y no
estaba visible porque las dos copias no se habían reconciliado. En vez de eso: se tomó
`docs/index.html` (la buena) como base, se arregló lo que estaba roto, se sumaron las
fotos, y se sincronizó `_PROYECTO/web/index.html` para que sea idéntica — quedan como
un solo archivo de ahora en más (ver `_PROYECTO/web/README.md`, que ya documentaba
este flujo: se edita en `_PROYECTO/web/`, se copia a `docs/` para publicar).

## Posicionamiento

**El problema:** en el campo argentino (Patagonia, NOA, zonas rurales) no hay señal de
celular. Ni WhatsApp ni Zello sirven ahí — necesitan datos, que es exactamente lo que
falta. Las alternativas actuales tienen fricciones reales:

| Alternativa | Fricción |
|---|---|
| Handy VHF/UHF comercial (Baofeng, Motorola) | Sin mapa, sin GPS, sin alerta de caída, interfaz vieja |
| Radio banda ciudadana (CB 27MHz) | Alcance corto, en desuso |
| Mensajero satelital (Garmin inReach, Zoleo) | USD 300–500 + suscripción mensual obligatoria, solo texto/SOS, no voz en tiempo real |
| Apps con señal (WhatsApp, Zello) | Inútiles exactamente donde más se necesitan — sin señal, no funcionan |

**Baqueano:** radio VHF real (voz, no texto ni SOS binario) + interfaz que ya conocés
(tu propio celular) + sin licencia (canales libres Res. 5/2015) + sin costo mensual
nunca + seguridad automática (Man-Down: si te caés y no respondés, avisa solo).

## Público (regla de alcance — ver `CLAUDE.md`)

Motociclistas, 4×4/overlanding, ciclismo, trekking/senderismo, trabajo rural. La web ya
reflejaba esto correctamente en el copy; lo que faltaba era reforzarlo con fotos reales
en las secciones de "casos de uso" — se agregaron fotos aéreas ya generadas para el
grupo de motos y el grupo de 4×4 (ver abajo).

## Cambios aplicados hoy

1. **Formulario de lista de espera roto** — apuntaba a un Formspree con ID placeholder
   (`REEMPLAZAR_CON_ID`) nunca completado. Cualquier envío real fallaba en silencio en
   la web ya publicada. Reemplazado por un `mailto:` armado en JS: sin backend externo,
   funciona ya mismo. Mensaje de éxito corregido para reflejar el flujo real (se abre
   el cliente de mail, no queda anotado automáticamente en una base).
2. **Fotos integradas** en las cards "Grupos de motos" y "4×4 y Offroad" de la sección
   Casos de Uso — las dos fotos aéreas con overlay de señal/red que ya existían en
   `_PROYECTO/web/img/moto-01.jpg` y `moto-06.jpg`, ahora copiadas a `assets/`.
3. **Typo de CSS corregido** (`montoje-desc` → `montaje-desc`) que rompía el estilo de
   un párrafo en la card 4×4.
4. **Reconciliación**: `_PROYECTO/web/index.html` y `docs/index.html` vuelven a ser
   idénticos.

## Lo que falta (pendiente real, no se resolvió hoy)

- **Precio.** No hay ningún precio de venta decidido en ningún documento del proyecto.
  La web no puede tener un botón "Comprar" real hasta que exista — hoy el CTA es
  correctamente "lista de espera", no venta directa.
- **Fotos para ciclismo y trabajo rural.** Existen fotos para moto y 4×4 (con overlay
  de red ya generado), pero ninguna para los otros dos segmentos que sí están en el
  copy y en el selector del formulario. Esas dos cards quedan solo con ícono.
- **Lista de espera sin backend real.** El fix de hoy (mailto) funciona sin
  configuración, pero depende de que la persona interesada efectivamente envíe el
  mail desde su cliente. Si se quiere una lista real sin fricción, conviene crear una
  cuenta gratuita de Formspree (o similar) y reemplazar el mailto por ese endpoint —
  2 minutos, pendiente de que LuKaZ decida y cree la cuenta (no es algo que se pueda
  hacer sin sus credenciales).
- **Confirmar que el push a GitHub dispara el redeploy de Netlify.** El sitio actual
  en `baqueano.netlify.app` coincide exactamente con `docs/index.html` antes de este
  cambio, lo que sugiere que Netlify está conectado al repo de GitHub — pero no hay
  forma de confirmarlo sin loguearse a Netlify (`netlify login` requiere navegador
  interactivo, no disponible en esta sesión). Verificar después del push que el sitio
  en vivo se actualizó.
