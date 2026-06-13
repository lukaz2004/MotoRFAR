# SPRINT 7 — Onboarding + T&C + Rename package

> Plan generado 2026-06-10. Input: SESSION-LOG.md (estado post Sprint 6), ROADMAP.md.

## Objetivo del sprint

Al terminar, la app puede distribuirse: tiene un flujo de onboarding real
(T&C → alias → canal), el T&C cita la Res. 5/2015, y el package está renombrado
a `ar.motorfar.app`. La deuda de tests instrumented queda cerrada.

## ¿Por qué este conjunto?

Sin onboarding ni T&C la app no se puede distribuir legalmente ni éticamente.
El rename del package es bloqueante para publicar en GitHub Releases o F-Droid:
`com.vagell.kv4pht` en el APK indica que es un fork sin identidad propia.
Los tests instrumented son deuda acumulada de 3 sprints.

Todo lo de este sprint es infraestructura de distribución — funcionalidad pura,
cero estética nueva.

## Capas que NO se tocan

- `ArgentinaChannels.java` — ya tiene los 3 canales del Sprint 6
- `ToneHelper.java` — completo
- `GpsBeaconManager.kt` — completo
- `Protocol.java` — intocable
- `TxWhitelist.java` — intocable
- Todo el sistema de alertas del Sprint 6

## Estado inicial conocido (post Sprint 6)

| Feature | Estado |
|---|---|
| Canales nombrados GRUPO/ALT/EMERGENCIA | ✅ Sprint 6 |
| Alertas STOP/REGROUP/EMERGENCY | ✅ Sprint 6 |
| beaconInterval hot-reload | ✅ Sprint 6 |
| Onboarding flow | ❌ no implementado |
| T&C reescrito Res. 5/2015 | ❌ no implementado |
| Rename package ar.motorfar.app | ❌ pendiente (bloqueante para distribuir) |
| Tests instrumented | ❌ deuda acumulada sprints 5-6 |

---

## Fases de ejecución

---

### FASE A — Tests instrumented [~1-2h]

Deuda acumulada de sprints 5 y 6. Usar `composeTestRule` con `createAndroidComposeRule<MainActivity>`.

**A1. `MainScreenTest.kt` — actualizar para Sprint 6:**
```kotlin
@Test fun channelSelector_shows_three_channels() {
    composeTestRule.onNodeWithText("GRUPO").assertIsDisplayed()
    composeTestRule.onNodeWithText("ALTERNATIVO").assertIsDisplayed()
    composeTestRule.onNodeWithText("EMERGENCIA").assertIsDisplayed()
}

@Test fun pttButton_exists() {
    composeTestRule.onNodeWithContentDescription("PTT").assertIsDisplayed()
}

@Test fun emergency_button_exists() {
    composeTestRule.onNodeWithText("EMERGENCIA").assertIsDisplayed()
}
```

**A2. `AlertBannerInstrumentedTest.kt`:**
```kotlin
@Test fun alert_banner_hidden_on_start() {
    composeTestRule.onNodeWithTag("alert_banner").assertDoesNotExist()
}
```

Commit: `test(A): tests instrumented MainScreen + AlertBanner`

---

### FASE B — Rename package [~1-2h]

Cambiar `com.vagell.kv4pht` → `ar.motorfar.app` en todo el proyecto.

**B1. Refactor automático en Android Studio:**
- Right-click en el package root → Refactor → Rename
- Actualizar `applicationId` en `build.gradle`:
```groovy
defaultConfig {
    applicationId "ar.motorfar.app"
    ...
}
```

**B2. Verificar que no queden referencias al package viejo:**
```bash
grep -r "com.vagell.kv4pht" --include="*.kt" --include="*.java" --include="*.xml" .
```
Resultado esperado: 0 matches (o solo en comentarios históricos).

**B3. Actualizar `AndroidManifest.xml`:**
```xml
<manifest package="ar.motorfar.app" ...>
```

**B4. Verificar `TxWhitelist.java`** — si tiene referencias al package, actualizar.

**Tests Fase B:**
- `./gradlew test` — 130+ tests deben seguir pasando
- `assembleDebug` SUCCESSFUL con nuevo package

Commit: `refactor(B): rename package com.vagell.kv4pht → ar.motorfar.app`

---

### FASE C — T&C reescrito [~1h]

El T&C actual es del fork kv4p HT (en inglés, para EE.UU.). Necesita:
- Idioma: español rioplatense
- Marco legal: Res. 5/2015 M.T.T.T. citada explícitamente
- Frecuencias: las 3 de ArgentinaChannels (138.510 / 139.970 / 140.970 MHz)
- Sin mencionar licencias de radioaficionado (no son necesarias bajo Res. 5/2015)

**C1. Contenido del nuevo T&C:**
```
TÉRMINOS Y CONDICIONES DE USO — MotoRFAR

Esta aplicación opera bajo la Resolución 5/2015 del Ministerio de Transporte
de la Nación Argentina, que habilita el uso de las frecuencias VHF
138.510 MHz, 139.970 MHz y 140.970 MHz sin licencia individual para
comunicaciones de grupos en tránsito.

USO PERMITIDO:
- Comunicación grupal entre vehículos en tránsito
- Transmisión de posición GPS en las frecuencias habilitadas
- Alertas de emergencia entre miembros del grupo

RESPONSABILIDAD DEL USUARIO:
- Esta aplicación requiere hardware compatible (módulo SA818-V)
- El usuario es responsable del uso dentro del marco de la Res. 5/2015
- No usar para comunicaciones individuales fuera del grupo en tránsito
- No usar fuera del territorio argentino sin verificar la normativa local

PRIVACIDAD:
- Esta aplicación no recopila ni transmite datos a servidores externos
- Toda la comunicación es local vía radio VHF
- La posición GPS se transmite únicamente por radio al grupo

Al continuar, aceptás estos términos.
```

**C2. Implementar `TermsScreen.kt`** — pantalla simple con el texto + botón ACEPTAR.
Si el usuario no aceptó → no puede usar la app (verificar flag en Room).

Commit: `feat(C): T&C en español citando Res. 5/2015`

---

### FASE D — Onboarding flow [~2-3h]

Primera vez que el usuario abre la app: T&C → Alias → Canal → App principal.
Solo se muestra una vez. Si ya completó el onboarding, va directo a la app.

**D1. Flag de onboarding en AppSetting:**
```kotlin
const val SETTING_ONBOARDING_COMPLETE = "onboarding_complete"  // "true" / "false"
```

**D2. `OnboardingActivity.kt`** — Activity separada con NavHost:
```
Paso 1: TermsScreen    → ACEPTAR → Paso 2
Paso 2: AliasSetup     → GUARDAR → Paso 3
Paso 3: ChannelSelect  → LISTO   → MainActivity
```

Barra de progreso superior: `1 de 3 → 2 de 3 → 3 de 3`.

**D3. `MainActivity.kt`** — al arrancar, verificar flag:
```kotlin
override fun onCreate(...) {
    val onboardingDone = settingsRepo.get(SETTING_ONBOARDING_COMPLETE) == "true"
    if (!onboardingDone) {
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
        return
    }
    // resto del setUp normal
}
```

**D4. Pantalla `ChannelSelectOnboarding.kt`** — versión simplificada del selector:
```
¿En qué canal está tu grupo?

[GRUPO]        138.510 MHz — Canal principal
[ALTERNATIVO]  139.970 MHz — Canal de respaldo
[EMERGENCIA]   140.970 MHz — Solo emergencias
```
Selección obligatoria antes de continuar.

**Tests Fase D:**
- `OnboardingFlagTest.kt` — `SETTING_ONBOARDING_COMPLETE = "false"` → OnboardingActivity se lanza
- `OnboardingFlagTest.kt` — `SETTING_ONBOARDING_COMPLETE = "true"` → MainActivity directo
- `TermsScreenTest.kt` — botón ACEPTAR guarda flag en Room

Commit: `feat(D): onboarding flow — T&C + alias + canal (primera apertura)`

---

## Definición de "hecho" del Sprint 7

- [ ] `./gradlew test` pasa todos los tests (130 previos + tests nuevos)
- [ ] Tests instrumented pasan en emulador
- [ ] `grep -r "com.vagell.kv4pht"` devuelve 0 resultados en código fuente
- [ ] `applicationId` es `ar.motorfar.app` en build.gradle
- [ ] Primera apertura → muestra T&C → alias → canal → app
- [ ] Segunda apertura → va directo a app (onboarding ya completado)
- [ ] T&C menciona Res. 5/2015 y las 3 frecuencias explícitamente
- [ ] `assembleDebug` SUCCESSFUL
- [ ] SESSION-LOG.md actualizado al cierre

---

## Notas para la sesión de Claude Code

1. **Rename de package**: hacer el refactor automático de Android Studio/IntelliJ
   es más seguro que hacerlo a mano. Si Claude Code no tiene acceso al IDE,
   usar `find + sed` pero verificar cada archivo antes de guardar.

2. **`TxWhitelist.java`**: este archivo valida frecuencias y puede tener el package
   hardcodeado. Verificar con grep antes y después del rename.

3. **Onboarding y tests**: el onboarding redirige a otra Activity en `onCreate`.
   Para testear MainActivity sin onboarding, setear el flag a `true` en el setUp
   del test instrumented.

4. **T&C y legales**: el texto del T&C en Fase C es un draft funcional.
   No es asesoramiento legal profesional. Si en el futuro se distribuye
   comercialmente, revisar con alguien de derecho TIC.

5. **Branch**: crear `sprint/7-onboarding-package` desde `main` al arrancar.
