# Assets visuales

Esta carpeta contiene mockups, screenshots y referencias visuales del proyecto.

## Contenido esperado

- `motorfar_preview.png` — mockup de la pantalla principal (regenerar en Claude Code primera sesion; ver `docs/05-DISEÑO.md`)
- Screenshots futuros de la app real cuando este implementada

## Como regenerar el mockup

El mockup fue diseñado en chat de Claude. Para regenerarlo se puede pedir a Claude Code:

```
Genera el mockup de la pantalla principal de MotoRFAR como PNG. Usa la especificacion del docs/05-DISEÑO.md (theme ambar CRT militar) y los elementos descriptos para la pantalla main. Guardalo en docs/assets/motorfar_preview.png.
```

Claude Code puede usar Playwright + un HTML mockup (renderizado a 600x1100 con device_scale_factor 2 funciona bien).
