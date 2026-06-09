# 05 — Diseño visual

## Concepto

Estetica de **radio militar/tactica CRT** con dos themes alternables. Referencias visuales:

- **AN/PRC-148 MBITR** - handheld VHF/UHF moderno US Army (display ambar)
- **Harris Falcon III** - modo nocturno verde fosforo
- **Soviet R-105M** - estetica verde olivo industrial (inspiracion menor, mas referencia de labeling)

La eleccion no es estetica gratuita: el contraste alto sobre fondo oscuro es legible bajo sol fuerte con anteojos polarizados y de noche no encandila.

## Paleta de colores

### Theme AMBAR (defecto)

```
Fondo principal:        #050505  (casi negro, no negro puro)
Fondo display:          #0e0904  (oscuro con tinte ambar)
Fondo elementos:        #1a1208
Border sutil:           #3a2807
Border activo:          #EF9F27  (ambar fuerte)
Texto principal:        #FAC775  (ambar claro)
Texto secundario:       #BA7517  (ambar medio)
Texto deshabilitado:    #854F0B  (ambar oscuro)
Texto sutil:            #633806
Acento brillante:       #EF9F27  (LED, indicadores activos)
Glow LED:               #EF9F27 con opacity 0.4

EMERGENCIA - rojo:
Fondo:                  #501313
Border:                 #E24B4A
Texto:                  #F09595
```

### Theme VERDE FOSFORO (alternativo)

```
Fondo principal:        #050505
Fondo display:          #040c04  (oscuro con tinte verde)
Fondo elementos:        #0c1408
Border sutil:           #1f3a1f
Border activo:          #4FBD3B
Texto principal:        #8FE875  (verde claro)
Texto secundario:       #4FBD3B  (verde medio)
Texto deshabilitado:    #1f5511
Acento brillante:       #4FBD3B
Glow LED:               #4FBD3B con opacity 0.4

EMERGENCIA - rojo: (igual que ambar, el rojo no cambia)
```

## Tipografia

**Primaria (datos numericos, frecuencias, dis    plays):** JetBrains Mono - peso 400 y 500.
Alternativas si gratis no funciona: Share Tech Mono, Major Mono Display.

**Secundaria (textos largos):** Inter o system default sans-serif.

Razon: monospace para datos refuerza la identidad CRT. Sans-serif para parrafos por legibilidad.

## Iconografia

Set unico: **Tabler Icons** (open source, MIT, gratuito, ~5000 iconos).
Estilo: outline (no relleno), peso uniforme.

Iconos clave a usar:
- `antenna` para indicador de radio
- `microphone` para PTT
- `users` para grupo
- `map-pin` para waypoints
- `refresh` para REAGRUPAR
- `player-pause` para PARADA
- `alert-triangle` para EMERGENCIA
- `settings` para configuracion

## Layout / componentes

### Status bar superior (12-16px alto)
- Hora a izquierda
- Modo (VHF) + bateria a derecha
- Color: texto secundario, tipografia monospace

### Header de app (40-48px alto)
- LED indicador a izquierda (brillante si TX/RX activo)
- "MOTORFAR" en mono espaciado
- Icono settings a derecha

### Selector de canal (~80px alto)
- Grid de 3 columnas iguales
- Canal activo: borde ambar fuerte, fondo levemente coloreado
- Canales inactivos: borde sutil, texto deshabilitado
- Click cambia canal con animacion corta

### Display de frecuencia (~120px alto)
- Numero grande de frecuencia, monospace, peso 500
- Modo (MHz · FM) a la derecha, sutil
- VU-meter debajo: 10 barras con gradiente de altura segun señal
- Label "SEÑAL · X/10" debajo
- Border 0.5px sutil

### Group widget (~120-150px alto)
- Header con icono usuarios + contador "X MOTOS CERCA"
- Lista de hasta 4 miembros: alias + distancia + rumbo cardinal
- Tipografia monospace para alineacion

### PTT central (140-160px circle)
- Circulo grande con gradiente radial ambar
- Icono microfono grande + label "PTT"
- Border 3-4px en ambar claro
- Sombra/glow exterior sutil
- Estados: idle / transmitiendo (color mas vivo) / disabled

### Botones de alerta (60-80px alto cada uno)
- Row de 2 botones iguales: REAGRUPAR + PARADA
- Debajo un boton ancho EMERGENCIA en rojo
- EMERGENCIA requiere confirmacion (slide o hold)

## Efectos sutiles

Usar con moderacion. Mucho efecto cansa:

- **Scan lines** en displays: gradient repeat cada 2px, opacity 5-8%
- **Glow de texto** en numeros de frecuencia: text-shadow color ambar 0 0 4px opacity 30%
- **Subtle noise** en el fondo: posible textura SVG generada con CSS feTurbulence
- **Animacion del LED** del header: pulso suave de opacity al recibir/transmitir

## Mockup de referencia

El mockup actual de la pantalla principal esta en:
`docs/assets/motorfar_preview.png` (ver instrucciones de copia abajo)

## Sonidos

Todos sintetizados con AudioContext de Android (cero assets):

- **Click PTT pressed:** square wave 800Hz, 30ms, fade out
- **Click PTT released:** square wave 600Hz, 30ms, fade out
- **Alert incoming - tipo STOP/REGROUP:** double beep 1200Hz, 100ms each, gap 50ms
- **Alert incoming - tipo EMERGENCY:** triple beep ascendente 800/1000/1200Hz, 150ms each
- **Static burst (alguien empieza a TX):** white noise 80ms, low volume
- **GPS lock:** chord ascendente 600+800Hz, 200ms

Volumen general configurable en settings (0-100%, defecto 70%).

## Animaciones

- **Cambio de canal:** transicion de opacity y border-color, 200ms ease
- **Apertura de alerta:** slide up + fade in, 300ms ease-out
- **LED status:** opacity pulse infinito durante TX/RX, 1.5s cycle
- **VU-meter:** update suave a 30fps, no saltos bruscos

## Reglas de diseño

1. **Densidad sobre belleza.** Es una app de operacion bajo presion (en marcha, con guantes). La pantalla principal debe mostrar TODO lo crucial sin scroll: canal, frecuencia, señal, grupo, PTT, alertas.
2. **Target touch >= 48dp.** Para uso con guantes. PTT y EMERGENCIA mas grandes (60-80dp).
3. **Dos colores no son cuatro colores.** Theme ambar usa una sola familia, escala de tonos. No mezclar.
4. **Texto siempre legible.** Nunca poner texto chico sin contraste alto. Usar luz ambiente del sistema cuando se pueda.
5. **No glassmorphism, no blur.** Estamos en una radio, no en una app de Apple. Bordes nitidos.
