# MotoRFAR HT — Sitio web

Sitio estático de producto. Una sola página HTML, sin dependencias locales.

## Publicar en GitHub Pages

**Paso 1 — Mover el archivo a la raíz del repo (o usar `/docs`)**

Opción A (raíz del repo):
```
cp _PROYECTO/web/index.html index.html
```

Opción B (carpeta `/docs`, recomendado para mantener orden):
```
mkdir -p docs
cp _PROYECTO/web/index.html docs/index.html
```

**Paso 2 — Activar GitHub Pages en el repositorio**

En GitHub → Settings → Pages:
- Source: `Deploy from a branch`
- Branch: `main` (o `master`)
- Folder: `/ (root)` si usaste la opción A, o `/docs` si usaste la opción B

**Paso 3 — Push y esperar ~1 minuto**

```bash
git add .
git commit -m "chore: agregar sitio web de producto"
git push
```

El sitio queda disponible en `https://<usuario>.github.io/<repo>/`.

## Desarrollo local

Abrir directamente en el navegador:
```
start _PROYECTO/web/index.html   # Windows
open  _PROYECTO/web/index.html   # macOS
```

No requiere servidor HTTP (todo el contenido es estático y las fuentes se cargan desde Google Fonts).
