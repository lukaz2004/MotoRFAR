# 🚀 EMPEZÁ ACÁ — MOTO HT

> Este es el único archivo que necesitás abrir para arrancar. Te dice la ruta y la frase exacta para cada etapa.

---

## RUTA RAÍZ DEL PROYECTO
```
C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\_PROYECTO\
```

## CÓMO ARRANCAR CUALQUIER ETAPA (en un chat NUEVO y limpio)

Copiá y pegá esta frase, cambiando el código de etapa:

> **"Leé el archivo `C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\_PROYECTO\ramas\[ARCHIVO]` y arrancamos la ETAPA [CÓDIGO] del proyecto MOTO HT. Antes de empezar, leé también `01_CONTRATOS.md`."**

Claude lee SOLO ese archivo (liviano, no 100k de contexto) y ejecuta el paso a paso.

---

## FRASES LISTAS PARA COPIAR (por etapa)

| Querés hacer... | Frase para el chat nuevo |
|-----------------|--------------------------|
| **Validar WiFi (la llave)** | "Leé `_PROYECTO\ramas\F0_arquitectura.md` y arrancamos ETAPA F0 de MOTO HT" |
| **Whitelist TX (seguridad)** | "Leé `_PROYECTO\ramas\FW_firmware.md` y arrancamos ETAPA FW-2 de MOTO HT" |
| **Compilar firmware** | "Leé `_PROYECTO\ramas\FW_firmware.md` y arrancamos ETAPA FW-1 de MOTO HT" |
| **Mergear sprints app** | "Leé `_PROYECTO\ramas\APP_app.md` y arrancamos ETAPA APP-1 de MOTO HT" |
| **Reconstruir carcasa** | "Leé `_PROYECTO\ramas\EST_estetica.md` y arrancamos ETAPA EST-1 de MOTO HT" |
| **Línea base PCB** | "Leé `_PROYECTO\ramas\HW_hardware.md` y arrancamos ETAPA HW-1 de MOTO HT" |

(La tabla completa de etapas está en `00_MAPA_MAESTRO.md`)

---

## 📍 EL PASO A SEGUIR (recomendación de Claude)

**Cuando tengas el ESP32 de prueba (en unos días):**

### Paso 1 — EMPEZÁ POR F0 (la llave maestra)
Es lo primero porque define el 40% del resto. Abrí un chat nuevo y decí:
> *"Leé `C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\_PROYECTO\ramas\F0_arquitectura.md` y arrancamos la ETAPA F0 del proyecto MOTO HT."*

Eso valida si el WiFi sirve. Si funciona, desaparece todo el problema de carga que sufrimos.

### Mientras tanto / en paralelo (NO necesitan el ESP32):
Podés correr estas en chats separados, cuando quieras, sin esperar F0:
- **FW-2** (whitelist TX) — lo más urgente de seguridad
- **APP-1** (mergear sprints pendientes)
- **EST-1** (reconstruir la carcasa como sólido)

### Qué NECESITÁS comprar antes de F0:
- **1 ESP32-WROOM-32 de prueba** (~US$5-10). NO la placa buena. Es para prototipar WiFi sin riesgo.

---

## ✅ CHECKLIST: ¿estamos listos para arrancar?

- [x] Mapa maestro con todas las etapas y dependencias
- [x] Auditoría del estado real (qué tenemos / qué falta / riesgos)
- [x] Los 3 contratos (interfaces entre ramas)
- [x] Calendario con orden y qué arrancar ya
- [x] Herramientas evaluadas (no falta nada nuevo salvo el ESP32 de prueba)
- [x] Paso a paso de cada rama (F0, FW, HW, APP, EST, INT)
- [x] Ubicación de toda la info y los backups documentada
- [x] Frases listas para copiar por etapa
- [ ] ESP32 de prueba comprado (lo conseguís en unos días)
- [ ] Memoria de Claude borrada (opcional, para arrancar limpio)

**TODO LISTO. Solo falta el ESP32 físico para arrancar F0.**

---

## RECORDATORIOS CLAVE (no olvidar)
- 🔴 Nunca rutear cobre crítico del PCB por script — siempre GUI KiCad.
- 🔴 Hacer backup del PCB antes de cualquier cambio.
- 🟢 La whitelist TX (3 frecuencias Res 5/2015) es la prioridad de seguridad.
- 🟢 Cada chat = 1 etapa = contexto liviano. Los contratos son la memoria compartida.
- 🟢 Borrar la memoria de Claude NO toca estos archivos (viven en tu disco).
