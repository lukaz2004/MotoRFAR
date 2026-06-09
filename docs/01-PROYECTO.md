# 01 — Proyecto: visión y usuario

## Una línea

MotoRFAR es un sistema de comunicación grupal por radio VHF para motociclistas y vehículos 4x4 en Argentina, sin licencia y sin cobertura celular.

## El problema concreto

Un grupo de 5-20 motos sale de Buenos Aires a la Patagonia. A partir de Bahía Blanca para el sur, la cobertura celular se rompe seguido. WhatsApp deja de andar. Si alguien pincha, se queda sin nafta, sufre una caída o se separa del grupo, no hay forma de avisar. Las soluciones actuales fallan cada una a su modo:

- **Intercoms Bluetooth** (Sena, Cardo) — alcance máximo 1-2 km entre motos, no funcionan con grupos grandes, no comparten GPS, cuestan USD 80-400 por casco.
- **Walkies chinos** (Baofeng, etc.) — baratos pero sin GPS, sin alertas, interfaz horrible, frecuencias en zona gris legal.
- **InReach / SPOT satelitales** — funcionan, pero USD 30-50/mes de suscripción más equipo de USD 300+. Para uso ocasional, prohibitivo.
- **WhatsApp** — depende de cobertura celular, que es lo que justamente falta.

## El usuario objetivo

**Persona 1 — "El líder de grupo"** (35-55 años, masculino mayoritario, ingresos medios)
- Organiza salidas de fin de semana o vacaciones con grupo de motos
- Tiene moto adventure/trail (Royal Enfield Himalayan, BMW GS, Honda Africa Twin, Yamaha Ténéré)
- Usa intercom Bluetooth pero le molesta que se corte con la moto de adelante
- Usa Google Maps y app de tracking, sabe lo que es GPX
- Pagaría USD 100-200 por una solución que resuelve el problema bien

**Persona 2 — "El integrante del grupo"** (25-50 años, ingresos medios-bajos)
- Va con el grupo del líder
- Quiere lo mismo pero gastar lo mínimo
- Pagaría USD 50-100 si el líder lo recomienda

**Persona 3 — "El expedicionario 4x4"** (40-60, ingresos medios-altos)
- Sale en grupo de 4x4 a Patagonia, Cuyo, Puna
- Ya tiene radio VHF Yaesu/Icom en la camioneta, pero quiere algo mas practico para el celular
- Pagaria mas por features avanzadas (rutas compartidas, mapas)

## Propuesta de valor

> "Comunicacion por radio para tu grupo de motos, sin licencia, sin internet, sin cuota mensual. Anda donde quieras, todos siguen conectados."

Las tres cosas que diferencian:

1. **Marco legal solido** - Res. 5/2015 fue redactada exactamente para este caso de uso
2. **GPS del grupo en vivo** sin depender de internet
3. **Alertas estructuradas** (EMERGENCY/STOP/REGROUP) escuchables tambien por terceros M.T.T.T. en la zona

## Restricciones del proyecto

- **Equipo:** un solo desarrollador (LuKaZ), tiempo limitado fuera del trabajo principal
- **Presupuesto:** minimo posible, "a pulmon"
- **Hardware:** PCB fabricada en lote pequeño (~50 unidades primer batch) en JLCPCB
- **Distribucion v1:** APK por GitHub, hardware por MercadoLibre o venta directa
- **Distribucion v2 (objetivo):** Play Store + tienda online

## Lo que MotoRFAR NO es

Para evitar scope creep y mantener foco:

- **NO** es un sistema de radioaficionados - no requiere ni asume licencia ASOC
- **NO** es un competidor de Garmin InReach - no usa satelite
- **NO** es un intercom de casco - convive con intercoms Bluetooth existentes
- **NO** es una red social, app de comunidad ni track-sharing publico
- **NO** es un producto de uso urbano cotidiano
