# SPRINT 9 — Pulido de campo v1.0 (mapa + rail + audio + solo escucha)

> Doc creado 2026-06-15 (Sesión 9). Input: testing de campo en emulador
> (Pixel 10 Pro XL, capturas 7:07 / 7:09) + revisión del working tree.

## ⏩ CONTINUAR ACÁ (handoff 2026-06-15)

**Estado en una línea:** todo el feedback de campo de hoy YA está escrito como
cambios **sin commitear** en 4 archivos sobre `main`. Falta compilar, commitear
en branch, reinstalar y re-testear. **No hay features nuevas que escribir.**

Por qué las capturas mostraban los problemas: el APK instalado en el emulador
era viejo (anterior a estos cambios). El código ya los resuelve.

Working tree sucio (sin stash; branch = origin/main + estos 4):
```
 M KV4PHT/app/src/main/java/ar/motorfar/app/ui/MainActivity.kt
 M KV4PHT/app/src/main/java/ar/motorfar/app/ui/ToneHelper.java
 M KV4PHT/app/src/main/java/ar/motorfar/app/ui/compose/MainScreen.kt
 M KV4PHT/app/src/main/java/ar/motorfar/app/ui/compose/MapScreen.kt
```

**Pasos para mañana (en orden):**

1. Verificar el tree:
   ```powershell
   git -C "C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT" status --short
   ```
2. Compilar (usar el JDK 21 de Android Studio; el Java del sistema es 8 e incompatible con AGP):
   ```powershell
   $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
   cd C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\KV4PHT
   .\gradlew assembleDebug
   ```
   Si NO compila → arreglar lo que falte. Es lo ÚNICO potencialmente bloqueante.
3. Commitear en branch (NO directo a main):
   ```powershell
   git checkout -b sprint/9-field-polish
   git add KV4PHT/app/src/main/java/ar/motorfar/app/ui/MainActivity.kt `
           KV4PHT/app/src/main/java/ar/motorfar/app/ui/ToneHelper.java `
           KV4PHT/app/src/main/java/ar/motorfar/app/ui/compose/MainScreen.kt `
           KV4PHT/app/src/main/java/ar/motorfar/app/ui/compose/MapScreen.kt
   git commit -m "feat(sprint-9): fix isla camara + PTT en mapa + roger beep + feedback solo-escucha"
   ```
4. Reinstalar en el emulador (esto era lo que faltaba — el APK estaba viejo):
   ```powershell
   .\gradlew installDebug
   ```
5. Re-testear los 5 ítems (checklist más abajo).
6. Cierre: `.\gradlew testDebugUnitTest` → actualizar SESSION-LOG → PR.

## Feedback de campo → estado real en el código

| # | Feedback (capturas) | Ya resuelto en | Verificar tras rebuild |
|---|---|---|---|
| 1 | Rail pisa la isla de la cámara | `.displayCutoutPadding()` en el Row raíz (MainActivity) | Rail despejado del punch-hole en AMBAS rotaciones |
| 2 | "RUTA ARRIBA" ¿qué es? | Toggle heading-up + indicador (MapScreen) | Alterna ruta-arriba / norte-arriba; ▲ refleja el modo |
| 3 | Falta PTT / volver en el mapa | `MapPttButton` sobre el zoom + `NavigationRail` en landscape | Hold-to-talk transmite; rail visible en el mapa |
| 4 | Sonido radio al emitir/terminar | `playPttDown` (key-down) + `playRogerBeep` (fin TX) | Suena al apretar y al soltar (chequear volumen del emu) |
| 5 | Solo escucha sin indicio + funciones "muertas" | Toggle relleno cuando activo + Toasts (toggle y bloqueo) | Ícono cambia; Toast al togglear; Toast al intentar TX |

## Checklist de re-test (build fresco)

- [ ] #1 — rail no toca la isla (rotar el emulador a ambos lados)
- [ ] #2 — RUTA ARRIBA alterna y el mapa rota con el rumbo
- [ ] #3 — PTT del mapa transmite ("TX"); el rail navega sin botón físico
- [ ] #4 — key-down al apretar PTT + roger beep al soltar
- [ ] #5 — ícono escucha con estado inequívoco + ambos Toasts
- [ ] EMERGENCIA sigue disponible incluso en modo escucha

## Estética ("la que hablamos")

- Hay 3 temas seleccionables en el TopBar: **VERDE · CRT**, **AMBAR · CRT**,
  **DÍA · SOL**. Las capturas estaban en ámbar (el verde es un tema, no el default).
- Mapa con filtro táctico oscuro (`darkTacticalTileFilter`) en temas oscuros,
  sin filtro en Día. HUD con coordenadas + zoom + rumbo.
- Nada que cambiar acá — sólo confirmar que se ve bien en el build fresco.

## Arrastrado de Sprint 8 / pendiente real (post-verificación)

- [ ] Mergear el PR de Sprint 8 (quedó abierto para review)
- [ ] **Revocar los tokens gh expuestos** en la Sesión 8 + regenerar uno limpio (seguridad)
- [ ] Borrar `EmergencyConfirmDialog.kt` (deprecado, sin referencias)
- [ ] `TermsActivity.java` → extraer constantes a `AppSetting` (Finding 5 MEDIUM)

## Diferido a v1.1

- PTT externo vía jack 3.5mm (audio focus + test de hardware)
- OSMDroid tiles offline (hoy el botón "Descargar mapas" es un Toast "próximamente")
- Verificación con SA818-V real conectado

---

> Nota de la Sesión 9: no se commiteó ni compiló nada esta noche a propósito.
> El criterio fue: verificar build ANTES de commitear, y commitear en branch,
> no en `main`. El working tree quedó intacto con los 4 archivos modificados.
