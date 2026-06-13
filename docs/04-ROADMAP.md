# 04 — Roadmap

> Lista explicita de que va y que no en cada version. Sirve como contrato anti-scope-creep: si algo no esta aca, no se hace todavia.

## v1.0 — MVP funcional (objetivo: 3-4 meses)

Lo minimo viable que un grupo real puede usar en una salida.

### Hardware
- [ ] PCB v2.0c basada en kv4p HT con SA818-V (ver `docs/06-HARDWARE.md`)
- [ ] Caja impresa 3D estanca con SMA pasante
- [ ] Pigtail SMA macho-hembra anti-vibracion
- [ ] Jack 3.5mm para PTT externo
- [ ] Boton PTT impermeable montaje manillar con cable de 1m
- [ ] Antena cuarto onda VHF (~50 cm) con base SMA

### Software (Android)
- [ ] Migracion completa de PMR446 a 3 canales Res. 5/2015 en `ArgentinaChannels.java`
- [ ] Rename del package a `ar.motorfar.app` (o similar) - revisar AGP/Gradle
- [ ] Theme amber/green CRT con toggle en settings
- [ ] UI con tres canales nombrados Grupo / Alternativo / Emergencia
- [ ] PTT grande central + soporte para PTT externo via jack
- [ ] Balizas GPS digitales periodicas (intervalo configurable 30s-5min)
- [ ] Alertas EMERGENCY / STOP / REGROUP con GPS adjunto
- [ ] EMERGENCIA con confirmacion (slide-to-confirm o hold 2s)
- [ ] Mapa OSMDroid con marcadores del grupo
- [ ] Descarga de zonas offline (interfaz simple)
- [ ] Configuracion de alias del usuario (sin login, sin cuenta)
- [ ] T&C reescrito citando Res. 5/2015
- [ ] Onboarding inicial: aceptar T&C, configurar alias, primer canal
- [ ] Animación PTT durante TX: 3 anillos radiales que se expanden desde el botón (estilo señal RF), staggered, fade-out al expandirse. Solo activos mientras `isTxActive == true`. Implementar en `PttButton.kt` con `InfiniteTransition` + `drawCircle` en Canvas Compose. **Opción A elegida** (descartadas: osciloscopio orbital, radar sweep, espectro de barras).
- [ ] Sonidos: beep alerta, click PTT, static burst
- [ ] Modo "Solo escucha" para acompañantes (TX deshabilitado)

### Documentacion
- [ ] README del repositorio
- [ ] Manual de armado del hardware (PDF con fotos)
- [ ] Manual de uso de la app (PDF)
- [ ] Privacy Policy publica
- [ ] Pagina de descarga del APK

### Distribucion
- [ ] Repositorio publico en GitHub bajo GPL-3.0
- [ ] APK descargable desde Releases de GitHub
- [ ] PCB Gerber compartido para que cualquiera pueda fabricar
- [ ] Listado en MercadoLibre del kit ensamblado (no homologado, venta como "kit experimental")

---

## v1.1 — Polish y features de uso real (1-2 meses post v1.0)

Features que solo tiene sentido construir despues de feedback real de v1.0.

### Software
- [ ] Compartir waypoints en vivo por radio (toca mapa -> manda lat/lon al grupo)
- [ ] Mensajes pre-formateados (Combustible bajo, Parada tecnica, Necesito ayuda, etc.)
- [ ] Deteccion de caida por acelerometro (alerta EMERGENCY automatica)
- [ ] Integracion PTT de intercom Bluetooth (Sena, Cardo, EJEAS)
- [ ] Voz a texto para mensajes urgentes en marcha
- [ ] Grafo de calidad de enlace mutuo (quien escucha a quien)
- [ ] CTCSS por subgrupo (codigos personalizables)
- [ ] Filtro de grupo por nombre en beacon GPS — campo opcional en el paquete de posición que permite que dos grupos en el mismo canal (139.970) no se mezclen en el mapa. Prerequisito más simple que CTCSS para separar grupos cercanos. Considerar implementar antes que CTCSS.
- [ ] Exportar tracks GPX de la salida

### Documentacion
- [ ] Manual extendido en formato web
- [ ] Videos tutoriales basicos

### Distribucion
- [ ] Considerar publicacion en F-Droid si v1.0 demostro estabilidad

---

## v2.0 — Producto completo y comercial (6+ meses post v1.0, condicional a validacion)

Solo si v1.0 demostro mercado real (50+ unidades vendidas, 10+ grupos activos).

### Hardware
- [ ] Diseño industrial profesional de la caja (no impresion 3D)
- [ ] PCB optimizada para fabricacion en lote 500+
- [ ] **Homologacion ENACOM completa** del modelo (ver `docs/02-MARCO-LEGAL.md`)
- [ ] Etiquetado reglamentario Res. 82/2015

### Software
- [ ] Bridge VHF + LTE con servicio cloud propio
  - Backend minimo (no me arme un Kubernetes, una VPS chiquita alcanza)
  - Tracking web publico/privado configurable
  - SMS automatico en emergencia si hay cobertura
- [ ] Compartir rutas GPX entre miembros del grupo
- [ ] Voz comprimida diferida con Codec2
- [ ] Mesh repeating cooperativo entre miembros del grupo

### Distribucion
- [ ] **Publicacion en Google Play Store** (requiere homologacion previa)
- [ ] Tienda online propia con kits ensamblados
- [ ] Programa de testers/grupos partners (clubes 4x4, peñas de moto)

### Estructura legal
- [ ] Inscripcion en Registro de Actividades RAMATEL (fabricacion + comercializacion)
- [ ] Encomienda COPITEC firmada
- [ ] Ensayos de laboratorio acreditado
- [ ] Definir vehiculo legal (Monotributo, SAS, otro)

---

## Explicitamente descartado para todas las versiones

Para mantener foco. Estas ideas estan archivadas, no en proceso:

- Foto o video por radio (ancho de banda insuficiente)
- Login con Google/Facebook/Apple (no es necesario)
- Backend antes de tener 50+ usuarios reales pidiendolo
- Pagos in-app
- Encriptacion fuerte de voz (no es legal para usuarios no licenciados)
- Soporte iOS (no hay acceso USB equivalente)
- Repetidores autonomos sin movil
- Trunked radio
- Soporte para bandas que requieren licencia (decision ADR-002)

Si en algun momento se quiere reabrir alguna, escribir un ADR nuevo en `docs/03-DECISIONES.md`.
