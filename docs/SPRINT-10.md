# SPRINT 10 — Cierre v1.0: tiles offline + Privacy Policy + Release

> Creado 2026-06-23. Branch: `sprint/10-offline-docs` desde `main` limpio.
> Diseñado para ejecución autónoma en sesiones largas (celular → PC).

## Estado verificado al inicio

- ✅ README.md — existe desde el commit inicial
- ✅ EmergencyConfirmDialog.kt — ya eliminado
- ✅ Sprints 1-9 archivados en `docs/archive/`

**Lo que falta realmente para v1.0:**

1. Descarga de tiles OSMDroid offline — no existe ni el botón ni el código
2. Privacy Policy — `docs/PRIVACY-POLICY.md` no existe
3. GitHub Release v1.0-beta1 — sin releases publicados

---

## Fase A — Descarga de tiles OSMDroid offline

El roadmap dice "Descarga de zonas offline (interfaz simple)". Hoy no hay nada: ni botón, ni código.

### A1 — Botón en MapScreen.kt

Agregar un cuarto `MapControlButton` en la columna de controles del mapa:

```kotlin
MapControlButton(
    label  = "⬇",
    colors = colors,
    onClick = { showDownloadDialog = true }
)
```

Y el estado/dialog:
```kotlin
var showDownloadDialog by remember { mutableStateOf(false) }
if (showDownloadDialog) {
    DownloadMapDialog(
        mapView   = mapView,
        onDismiss = { showDownloadDialog = false }
    )
}
```

### A2 — OfflineDownloadManager.kt

Crear `KV4PHT/app/src/main/java/ar/motorfar/app/map/OfflineDownloadManager.kt`.

Responsabilidades:
- `estimateTileCount(boundingBox, minZoom, maxZoom): Int`
- `downloadTiles(boundingBox, minZoom, maxZoom, onProgress, onComplete, onError)`
- `cancel()`

Usar `OSMDroid`'s `OfflineTileProvider` / tile downloader. Guardar en caché de OSMDroid
(ruta estándar: `context.filesDir/osmdroid/tiles/`).

Zoom a descargar: `[zoomActual, zoomActual + 1, zoomActual + 2]` — simple, no configurable.

### A3 — DownloadMapDialog.kt

Compose dialog en `KV4PHT/app/src/main/java/ar/motorfar/app/ui/compose/`:

```
┌─────────────────────────────────┐
│  Descargar área visible         │
│  ~N tiles · ~X MB estimado      │
│                                 │
│  [████████░░] 64 / 100          │
│                                 │
│      [CANCELAR]  [DESCARGAR]    │
└─────────────────────────────────┘
```

Estados: IDLE → estimación → descargando (progress) → completo / error.

### A4 — Tests

`OfflineDownloadManagerTest.kt` — mínimo:
- `estimateTileCount_returnsPositive` para bounding box válido
- `download_callsOnComplete` con mock del tile downloader

### Commit Fase A:
```
git commit -m "feat(sprint-10/A): descarga tiles OSMDroid offline para área visible"
```

---

## Fase B — Privacy Policy

Archivo: `docs/PRIVACY-POLICY.md`

La app no tiene cuenta, no manda datos a internet, el GPS va por radio local.
Texto en español, apto para publicar como página estática.

Contenido mínimo:
- Qué datos procesa (GPS → solo radio VHF local, no servidores)
- Qué NO hace (sin cuenta, sin analytics, sin ads, sin servidores propios)
- Contacto (lukaz1979@gmail.com)
- Fecha de vigencia

### Commit Fase B:
```
git commit -m "docs(sprint-10/B): Privacy Policy en español"
```

---

## Fase C — GitHub Release v1.0-beta1

### C1 — Build release

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\KV4PHT
.\gradlew assembleRelease
```

APK en: `app/build/outputs/apk/release/app-release-unsigned.apk`

> Si no hay keystore configurado: el APK saldrá sin firmar o con la firma de debug.
> Para beta privada alcanza con el APK de debug (`assembleDebug`).
> Documentar la decisión de firma en SESSION-LOG.

### C2 — Publicar release

```powershell
gh release create v1.0-beta1 `
    KV4PHT\app\build\outputs\apk\release\app-release.apk `
    --title "MotoRFAR HT v1.0-beta1" `
    --notes "Primera beta. Hardware requerido: ESP32 + SA818-V. Android 8+. Solo Argentina (Res. 5/2015)."
```

### Commit Fase C:
```
git commit -m "release: v1.0-beta1 publicado en GitHub Releases"
```

---

## Checklist de cierre

- [ ] A: Botón ⬇ en mapa funciona — descarga tiles reales del área visible
- [ ] A: OfflineDownloadManager con tests pasando
- [ ] B: `docs/PRIVACY-POLICY.md` completo y commitado
- [ ] C: APK subido a GitHub Releases v1.0-beta1
- [ ] Build final verde
- [ ] SESSION-LOG actualizado
- [ ] PR mergeado a main

---

## No va en este sprint

- Selector de zona personalizado para offline (solo área visible)
- Manual PDF de armado (requiere hardware físico con fotos)
- Firmar con keystore de producción (va con la release definitiva, no beta)
