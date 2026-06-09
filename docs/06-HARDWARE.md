# 06 — Hardware

## Vision general

Modulo accesorio para celular Android. Conexion USB-C. Sin display propio, sin bateria propia (toma del celular o de un powerbank en moto).

```
[Celular Android]
       |
    USB-C  ← alimentacion 5V + serial bidireccional
       |
[PCB MotoRFAR]
       |
    ESP32-S3  ← microcontrolador, USB nativo, audio I/O
       |
    SA818-V   ← modulo transceptor FM VHF 134-174 MHz
       |
   Conector SMA  ← cable pigtail anti-vibracion
       |
    Antena VHF  ← cuarto onda ~50cm o equivalente
```

## BOM (Bill of Materials) v1.0

Lista basada en PCB v2.0c del kv4p HT original, adaptada a VHF.

| Componente | Especificacion | Cantidad | Origen | Precio estimado USD |
|---|---|---|---|---|
| ESP32-S3-DevKitC-1 | ESP32-S3-WROOM-1, 8MB flash, USB-C | 1 | AliExpress / MercadoLibre | 8-12 |
| **SA818-V** | Modulo VHF 134-174 MHz, 1W, NF-FM | 1 | AliExpress (NiceRF oficial) | 9-12 |
| PCB v2.0c | Fabricada en JLCPCB | 1 | JLCPCB | 2-5 (lote de 5) |
| LM358 op-amp | Procesamiento audio | 1 | Cualquiera | 0.50 |
| Resistencias 0805 | 1k, 10k, 100k variadas | 12-15 | Kit generico | 1 |
| Capacitores 0805 | 100nF, 10uF | 8-10 | Kit generico | 1 |
| Conector SMA | Hembra montaje PCB | 1 | AliExpress | 1 |
| Conector USB-C | Hembra montaje PCB | 1 | AliExpress | 1.50 |
| Jack 3.5mm | Hembra montaje PCB, 3 contactos | 1 | AliExpress | 0.80 |
| LED 0805 | Status indicators (rojo, verde, amarillo) | 3 | Generico | 0.30 |
| **Pigtail SMA macho-hembra** | 15-20cm, RG174 o RG316 | 1 | AliExpress | 2-3 |
| **Antena VHF** | Cuarto onda 144 MHz, base SMA | 1 | AliExpress / VHF supplier | 5-15 |
| **PTT externo** | Boton momentaneo IP67 + cable 1m + jack 3.5mm | 1 | AliExpress | 5-8 |
| Tornilleria y caja 3D | Imprimir local en PETG | 1 | Filamento propio | 1-2 |

**Total estimado por unidad armada:** USD 40-65 segun proveedores y volumen.

## Ubicacion de componentes claves en el PCB

Modificaciones a verificar/agregar sobre el v2.0c original:

1. **Reemplazar DRA818U/SA818U por SA818-V.** Pinout identico, solo cambia el modulo soldado.
2. **Agregar jack 3.5mm en paralelo con boton PTT existente.** Tip = PTT activate, sleeve = GND.
3. **Verificar capacidad SMA conector PCB-mount** - confirmar que soporta cable pigtail con ferrula crimpada.
4. **LED indicators externos** - 3 LEDs visibles desde fuera de la caja: TX (rojo), RX (verde), GPS (amarillo controlado desde la app via USB).

## Anti-vibracion (critico en moto)

El problema #1 a resolver: el conector SMA directo en PCB es el punto mas fragil con vibracion constante.

**Solucion v1.0:**
- **PCB en caja estanca** fijada al chasis con damper de goma (silentblocks)
- **Pigtail SMA macho-hembra** de 15-20 cm con cable flexible (RG174 o RG316)
- **Antena en posicion alta del vehiculo** (top case en moto, techo en 4x4)
- **Caja con conector SMA hembra pasante en la tapa** - el pigtail conecta PCB interno con conector externo, asi la antena no transmite vibracion al PCB

**Solucion v1.5 (mejora):**
- Soporte para antena tipo Diamond MR-77 (mas robusta, base magnetica para 4x4)
- Cable coaxial calidad superior (Times Microwave LMR-200)

## Caja estanca impresa 3D

Diseño propio (no usar la caja original del kv4p HT que no es estanca).

### Especificaciones
- **Material:** PETG o ASA (no PLA - se ablanda al sol)
- **Grosor pared:** 2.4mm minimo
- **Sellado:** o-ring perimetral entre cuerpo y tapa
- **Pasacables:** prensaestopa PG7 para cable PTT externo
- **Conector SMA externo:** SMA hembra pasante con O-ring estanco

### Diseño tentativo
- Dimension externa: ~80 x 55 x 30 mm
- Conector USB-C en lateral con tapa de goma para cuando no se usa
- Soporte RAM-mount estandar o tornillos M4 en la base para fijar
- LED status visibles a traves de ventana de PETG transparente o difusor

Modelado en FreeCAD o Fusion 360 (LuKaZ ya tiene experiencia con Ender 3 Pro para imprimirla).

## Antena

### Para moto

**Opcion A - Whip flexible cuarto onda**
- Longitud: ~50 cm
- Base: SMA macho
- Montaje: en top case trasero con base magnetica o tornillo
- Ventaja: barata (USD 5-10), eficiente
- Desventaja: vibra mucho, antiesteta a algunos

**Opcion B - Rubber duck corta**
- Longitud: ~15-20 cm
- Base: SMA macho
- Montaje: directo en caja PCB
- Ventaja: prolija
- Desventaja: alcance reducido en VHF (las cortas son ineficientes en VHF)

**Recomendacion v1.0:** Opcion A, en top case. Para v1.1 evaluar Diamond MR-77 con base magnetica de techo.

### Para 4x4

- Antena base magnetica techo (Diamond MR-77, Comet HA-750, similares)
- Cable coaxial 3-5m hasta el habitaculo
- PCB en consola con celular en soporte
- Mucho mejor performance por ganancia y altura

## PTT externo

### Especificacion del boton

- Tipo: momentaneo, NO mantenido
- Proteccion: IP67 minimo (lluvia, polvo, salpicaduras)
- Color: rojo o naranja visible (no negro - se pierde en el manillar)
- Cuerpo: metal o plastico reforzado, diametro 16-22mm
- Cable: 1-1.5m, flexible, terminado en jack 3.5mm macho TS (tip-sleeve)

### Conexion electrica

- **Jack 3.5mm en PCB:** tip a entrada PTT del SA818 (en paralelo con boton fisico de PCB), sleeve a GND
- Pull-up resistor en PCB para evitar floating
- Codigo Android debe poder leer el estado tambien (para feedback visual de PTT presionado)

### Montaje en moto

- Abrazadera de manillar 22mm (estandar moto) con tornillo M5
- Posicion: lado izquierdo del manillar, accesible con pulgar sin soltar el embrague
- Alternativa: pedalera o boton de tanque

## Tests pre-fabricacion lote serio

Antes de pedir 50 PCBs a JLCPCB, verificar con prototipo de 5:

1. **Conectividad USB-C** sobre Android (probado con Pixel y mid-range gama media)
2. **Recepcion RX** en los 3 canales con antena real
3. **Transmision TX** dentro de spec de potencia (medir con SWR meter casero o pedido)
4. **Audio I/O** sin distorsion ni clipping
5. **Consumo de corriente** en TX y RX (verificar que el cel no se apaga)
6. **Durabilidad PTT externo** - 1000+ accionamientos sin falla
7. **Estanqueidad caja 3D** - sumergir 30 min en agua, verificar interior seco

## Lote inicial planeado

- **Prototipos:** 5 unidades para testing personal y feedback (LuKaZ + grupo cercano)
- **Lote 1 piloto:** 25-50 unidades para users early adopters (clubes de moto contactados)
- **Lote 2 (si v1 valida):** 200+ unidades con proceso de homologacion en curso

## Costos asociados al hardware

| Item | Costo USD aprox |
|---|---|
| Prototipos 5 unidades armadas | 250-400 |
| Lote piloto 25 unidades | 1000-1500 |
| Homologacion completa ENACOM (futuro, ver `docs/02-MARCO-LEGAL.md`) | 1700-4600 |

Sin homologacion, el hardware se distribuye como "kit experimental open source" sin pretension comercial - estrategia v1.0.
