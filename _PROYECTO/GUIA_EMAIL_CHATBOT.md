# Guía: Email profesional + WhatsApp chatbot

## 1. Email profesional (info@baqueano.com)

### Paso 1 — Dominio
Dos opciones:
- **NIC Argentina** — `.com.ar` ~ARS 800/año → https://nic.ar/es/dominios (requiere CUIT)
- **Namecheap** — `.com` ~USD 12/año → https://namecheap.com (tarjeta internacional)

### Paso 2 — Reenvío con ImprovMX (gratis)
1. Ir a https://improvmx.com
2. Crear cuenta gratis
3. Ingresar tu dominio (ej: `baqueano.com.ar`)
4. ImprovMX te da 2 registros MX para configurar en NIC Argentina o Namecheap
5. Crear alias: `info@baqueano.com.ar` → reenvía a `lukaz1979@gmail.com`
6. Verificar que lleguen los mails (puede tardar hasta 24h en propagarse)

### Paso 3 — Respuestas automáticas con Brevo (gratis)
1. Ir a https://brevo.com → crear cuenta gratuita (300 mails/día)
2. Crear una campaña de "Automation"
3. Trigger: "Cuando recibo email en info@baqueano.com.ar"
4. Respuesta: personalizar el texto de auto-respuesta

**Texto sugerido para auto-respuesta:**
```
Hola, gracias por contactarte con Baqueano.

Recibimos tu mensaje y te responderemos a la brevedad (generalmente en 24-48 horas).

Si tenés una pregunta urgente, podés escribirnos por WhatsApp: [tu número]

— El equipo de Baqueano
```

---

## 2. WhatsApp Business chatbot multilenguaje (ManyChat)

### Requisitos previos
- Número de teléfono dedicado para WhatsApp Business (puede ser el tuyo)
- Cuenta de Facebook Business (necesaria para la API de WhatsApp)

### Paso 1 — WhatsApp Business API
1. Ir a https://business.facebook.com
2. Crear cuenta de negocio → "Baqueano"
3. Agregar número de WhatsApp Business
4. Verificar el número (código SMS)

### Paso 2 — ManyChat
1. Ir a https://manychat.com → crear cuenta gratuita
2. Conectar WhatsApp Business
3. Crear flujo de chatbot:

**Flujo sugerido:**
```
Usuario escribe cualquier mensaje
  ↓
"¡Hola! Soy el bot de Baqueano 🏍️
  Puedo ayudarte con:
  1️⃣ ¿Qué es Baqueano?
  2️⃣ ¿Cómo funciona?
  3️⃣ ¿Dónde lo consigo?
  4️⃣ Hablar con una persona"

→ Si elige 1: explicación del producto
→ Si elige 2: link a baqueano.netlify.app
→ Si elige 3: "Estamos armando la tienda online, dejá tu email y te avisamos"
→ Si elige 4: "Te conectamos con el equipo, escribinos al +54 [tu número]"
```

### Soporte multilenguaje
ManyChat detecta el idioma del usuario automáticamente. Se puede configurar el mismo flujo en:
- Español (principal)
- Inglés (para usuarios internacionales)
- Portugués (mercado brasileño)

### Plan gratuito
- Hasta 1000 contactos activos
- Flujos ilimitados
- Sin restricciones de mensajes entrantes

---

## 3. Formulario web (Formspree)

El formulario de la web (`docs/index.html`) tiene:
```html
<form action="https://formspree.io/f/REEMPLAZAR_CON_ID">
```

Para activarlo:
1. Ir a https://formspree.io → crear cuenta gratuita
2. Crear nuevo formulario → te da un ID
3. Reemplazar `REEMPLAZAR_CON_ID` con tu ID real en `docs/index.html`
4. Commit + push → funciona automáticamente

Los envíos llegan a lukaz1979@gmail.com.

---

## Orden recomendado
1. Dominio (15 min) → NIC Argentina o Namecheap
2. ImprovMX (10 min) → reenvío inmediato
3. Formspree (5 min) → formulario web activo
4. WhatsApp Business + ManyChat (30-60 min) → chatbot

Total estimado: 1-2 horas de configuración.
