# Baqueano — Estado de sesión (28/06/2026)

## App Android — bugs de Gemini corregidos ✅ (commit 6082bfc)

1. **RadioAudioService.java línea 209** — carácter `b` suelto que rompía la compilación → eliminado
2. **RadioAudioService.java** — referencia a `MainActivityLegacy` (borrada por Gemini) → reemplazado por `MainActivity`
3. **FallDetectionManager.kt** — `SENSOR_DELAY_NORMAL` (200ms, muy lento para caídas) → `SENSOR_DELAY_GAME` (20ms)
4. **TxWhitelist** — verificado, las 3 frecuencias ENACOM intactas ✅

## Pendiente decidir ⚠️

La detección de caídas arranca automáticamente. Un bache fuerte puede disparar una falsa alarma en el canal de emergencia 140.970 MHz. Conviene agregar un toggle en la UI para habilitarla/deshabilitarla.

## Email profesional

- Dominio: NIC Argentina .com.ar (~ARS 800/año) o Namecheap .com (~USD 12/año)
- **ImprovMX** (gratis) reenvía info@baqueano.com a lukaz1979@gmail.com
- **Brevo** (gratis, 300 mails/día) para respuestas automáticas

## Chatbot WhatsApp multilenguaje

- **ManyChat** — drag & drop, multilenguaje, formularios, gratis hasta 1000 contactos
- Se conecta a WhatsApp Business directamente
- Puedo armar el flujo de FAQ basado en el contenido de la web

---

¿Qué seguimos? Toggle Man-Down en la app, o chatbot/dominio?
