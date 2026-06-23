# SPRINT 10 — Cierre v1.0: tiles offline + cleanup + docs + release

> Creado 2026-06-23 (Sesión 11). Input: roadmap v1.0 restante tras Sprint 9 mergeado.
> Diseñado para ejecución autónoma en sesiones largas (celular → PC).
> Branch: `sprint/10-offline-docs` desde `main` limpio.

## Estado del proyecto al inicio de este sprint

Sprints 1–9 mergeados en main. Build verde. 151+ tests pasando.

**Features v1.0 ya implementadas:**
- ✅ Migración canales Res. 5/2015 (GRUPO / ALTERNATIVO / EMERGENCIA)
- ✅ Rename package `ar.motorfar.app`
- ✅ Theme ámbar/verde CRT + toggle día
- ✅ PTT grande + animación radial durante TX
- ✅ Balizas GPS periódicas con hot-reload de intervalo
- ✅ Alertas STOP / REGROUP / EMERGENCIA con GPS adjunto
- ✅ EMERGENCIA con hold 2s + progress canvas
- ✅ Mapa OSMDroid con marcadores del grupo + PTT overlay + heading-up
- ✅ Configuración de alias
- ✅ T&C citando Res. 5/2015
- ✅ Onboarding completo (terms → alias → canal)
- ✅ Sonidos: click PTT + roger beep + alerta
- ✅ Modo "Solo escucha"

**Pendiente v1.0 — este sprint:**
- Descarga de tiles OSMDroid offline (botón actual = Toast "próximamente")
- Cleanup deuda técnica: EmergencyConfirmDialog.kt (deprecado), TermsActivity constants
- README del repositorio
- Privacy Policy (texto legal en español)
- GitHub Release con APK descargable
- Trackear archivos hardware al repo (STL, f3d, PSD)

---

## Fase A — Cleanup de deuda técnica

**Objetivo:** eliminar código muerto y cerrar los hallazgos pendientes de Sprint 7/8.

### A1 — Eliminar EmergencyConfirmDialog.kt

Archivo: `KV4PHT/app/src/main/java/ar/motorfar/app/ui/compose/EmergencyConfirmDialog.kt`

Fue deprecado en Sprint 8 cuando se implementó `EmergencyConfirmButton` con hold inline.
Ya no tiene referencias. Simplemente borrar.

```powershell
Remove-Item 'KV4PHT\app\src\main\java\ar\motorfar\app\ui\compose\EmergencyConfirmDialog.kt'
```

Verificar que no tiene imports: `grep -r "EmergencyConfirmDialog" KV4PHT/` debe devolver vacío.

### A2 — Extraer constantes de TermsActivity.java a AppSetting (Finding 5 MEDIUM)

`TermsActivity.java` tiene constantes de SharedPreferences hardcodeadas que también usa
`OnboardingHelper.kt`. La constante clave es `PREF_TERMS_ACCEPTED` (o similar).

Pasos:
1. Leer `TermsActivity.java` y `OnboardingHelper.kt` para identificar las constantes duplicadas.
2. Moverlas a `AppSetting.kt` como `const val`.
3. Actualizar las referencias en ambos archivos.
4. Correr tests — no deben romperse.

### A3 — Trackear archivos hardware

Agregar al repo los archivos de diseño físico que están en el root sin trackear:
- `baqueano logo.psd`
- `box v1.stl`
- `box v2.stl`
- `carcaza v1.f3d`
- `docs/assets/enclosure-v1/` (carpeta)
- `_PROYECTO/` (carpeta)

Crear `docs/hardware/` y mover los STL/f3d ahí. Mantener PSD en `docs/assets/`.

### Commit de cierre Fase A:
```
git commit -m "chore(sprint-10/A): cleanup EmergencyConfirmDialog + constantes AppSetting + hardware files"
```

---

## Fase B — Descarga de tiles OSMDroid offline

**Objetivo:** reemplazar el Toast "próximamente" por descarga real de tiles para la vista actual.

### Contexto

- `MapScreen.kt` tiene un botón "Descargar mapa" que actualmente lanza un Toast.
- OSMDroid tiene soporte nativo de caché: `TileDownloader` y `SqlTileWriter`.
- Implementación simple: descargar el área visible + 1 nivel de zoom adicional.

### B1 — Dependencia y permisos

En `build.gradle` verificar que `osmdroid-android` ya está. Si no, agregar:
```kotlin
implementation 'org.osmdroid:osmdroid-android:6.1.x'
```

Permisos en `AndroidManifest.xml` ya necesarios: `WRITE_EXTERNAL_STORAGE` (API < 29) y
`ACCESS_NETWORK_STATE`. Verificar que están.

### B2 — OfflineDownloadManager.kt

Crear `KV4PHT/app/src/main/java/ar/motorfar/app/map/OfflineDownloadManager.kt`:

```kotlin
class OfflineDownloadManager(
    private val context: Context,
    private val mapView: MapView
) {
    // Calcula tiles del bounding box visible para zooms [actual, actual+1, actual+2]
    fun downloadCurrentArea(
        onProgress: (Int, Int) -> Unit,   // (descargado, total)
        onComplete: () -> Unit,
        onError: (String) -> Unit
    )

    fun estimateTileCount(): Int  // Para mostrar al usuario antes de confirmar

    fun cancelDownload()
}
```

Implementar con `OSMDroidOfflineTileProvider` y `OfflineTileProvider`.
Guardar en `osmdroid/tiles/` en el directorio de caché de la app.

### B3 — DownloadMapDialog.kt

Dialog Compose que:
1. Muestra estimación de tiles y tamaño (~N tiles, ~X MB estimado).
2. Botón DESCARGAR → progress circular con "N / total tiles".
3. Botón CANCELAR activo durante la descarga.
4. Al terminar: "Descarga completa. Área disponible sin conexión."

### B4 — Conectar al botón en MapScreen.kt

En `MapScreen.kt`, reemplazar el Toast "próximamente" con:
```kotlin
var showDownloadDialog by remember { mutableStateOf(false) }
// ...
if (showDownloadDialog) {
    DownloadMapDialog(
        mapView = mapView,
        onDismiss = { showDownloadDialog = false }
    )
}
```

### B5 — Test unitario de OfflineDownloadManager

`OfflineDownloadManagerTest.kt`: al menos 2 tests:
- `estimateTileCount_returnsPositive_forValidBoundingBox`
- `downloadCurrentArea_callsOnComplete_whenSuccess` (mock del TileDownloader)

### Commit de cierre Fase B:
```
git commit -m "feat(sprint-10/B): descarga tiles OSMDroid offline para área visible"
```

---

## Fase C — README + Privacy Policy

**Objetivo:** los dos docs públicos mínimos para lanzar.

### C1 — README.md en la raíz del repo

Estructura:
```markdown
# MotoRFAR HT
[badge: license GPL-3.0] [badge: build status]

Comunicación de radio VHF grupal para motociclistas — sin celular, sin licencia.
Opera bajo Resolución 5/2015 (M.T.T.T.) en 138.510 / 139.970 / 140.970 MHz.

## ¿Qué es?
## Hardware necesario
## Cómo instalar el APK
## Cómo armar el hardware (link a docs/06-HARDWARE.md)
## Marco legal
## Licencia
## Créditos (fork de kv4p HT de Vance Vagell)
```

Tono: directo, técnico, en español. No más de 200 líneas.

### C2 — Privacy Policy

Archivo: `docs/PRIVACY-POLICY.md`

La app:
- No tiene cuenta de usuario
- No envía datos a servidores externos
- Las balizas GPS se transmiten solo por radio VHF local (no a internet)
- Los logs de radio son locales en el dispositivo
- No tiene analytics, no tiene ads

Texto en español. Formato apto para publicar como webpage estática.
Incluir: fecha de vigencia, alcance, qué datos se procesan (GPS local), contacto (email de LuKaZ).

### Commit de cierre Fase C:
```
git commit -m "docs(sprint-10/C): README publico + Privacy Policy"
```

---

## Fase D — GitHub Release v1.0-beta1

**Objetivo:** APK descargable públicamente para los primeros testers.

### D1 — Build release firmado

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd MotoRFAR-MTTT\KV4PHT
.\gradlew assembleRelease
```

El APK release queda en `app/build/outputs/apk/release/app-release.apk`.
Si no hay keystore: generar uno nuevo y documentar en `docs/03-DECISIONES.md` (ADR-015).

> **IMPORTANTE:** Si se genera un keystore nuevo, guardarlo en lugar seguro FUERA del repo.
> Nunca commitear el .jks ni la contraseña.

### D2 — Crear release en GitHub

```powershell
gh release create v1.0-beta1 `
    app/build/outputs/apk/release/app-release.apk `
    --title "MotoRFAR HT v1.0-beta1" `
    --notes "Primera beta pública. Hardware: ESP32-S3 + SA818-V. Requiere Android 8+."
```

### D3 — Actualizar README con link al APK

En README.md, sección "Cómo instalar el APK", agregar el link directo al asset del release.

### Commit de cierre Fase D:
```
git commit -m "release(v1.0-beta1): APK publicado en GitHub Releases"
```

---

## Checklist de cierre del sprint

- [ ] A: EmergencyConfirmDialog.kt eliminado, sin referencias huérfanas
- [ ] A: Constantes TermsActivity movidas a AppSetting
- [ ] A: Archivos hardware trackeados en repo
- [ ] B: Botón "Descargar mapa" funciona — descarga tiles reales, progress, cancel
- [ ] B: Tests OfflineDownloadManager OK
- [ ] C: README.md publicado en raíz del repo
- [ ] C: PRIVACY-POLICY.md completo
- [ ] D: APK release firmado y subido a GitHub Releases v1.0-beta1
- [ ] Build final verde: `assembleRelease` sin warnings
- [ ] SESSION-LOG actualizado con entrada Sprint 10
- [ ] PR abierto y mergeado a main

---

## Lo que NO va en este sprint (v1.1 o posterior)

- Tiles offline con selector de zona personalizado (solo área visible por ahora)
- Manual PDF de armado con fotos (requiere hardware físico para fotos reales)
- Manual PDF de uso (post-beta con feedback real)
- Página web propia de descarga (F-Droid / web propia van en v1.1)
- Integraciones PTT Bluetooth
- OSMDroid con capa de rutas GPX

---

> **Para sesiones largas desde celular:** ejecutar fase por fase en orden.
> Cada fase tiene commit propio. Si una fase no compila, reportar sin avanzar a la siguiente.
> No se necesita input del usuario entre fases a menos que aparezca un bloqueante.
