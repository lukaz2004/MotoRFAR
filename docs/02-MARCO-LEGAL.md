# 02 — Marco legal consolidado

> Esta es la base legal sobre la que se construye MotoRFAR. Cualquier cambio que toque frecuencias, potencia, modos de operacion o T&C tiene que verificar coherencia con este documento.

## La resolucion que habilita el proyecto

**Resolucion 5/2015 — Secretaria de Comunicaciones de la Nacion**
Publicada en Boletin Oficial el 21/01/2015.

### Caso de uso autorizado

Textual del considerando: actividades de viajes en zonas inhospitas y rurales, especificamente "travesias, expediciones, asistencia humanitaria, deportes de riesgo, montañismo, ciclo-viaje, motociclismo y similares".

MotoRFAR encaja en este encuadre de forma directa.

### Frecuencias asignadas

| Frecuencia | Rol | Mapeo en MotoRFAR |
|---|---|---|
| **139.970 MHz** | Uso prioritario | Canal "Grupo" (defecto) |
| **138.510 MHz** | Uso secundario | Canal "Alternativo" |
| **140.970 MHz** | Exclusivo emergencias | Canal "Emergencia" |

### Condiciones tecnicas

- **No requiere autorizacion individual** (Art. 2): no hace falta licencia ni tramite ENACOM para operar las estaciones moviles
- **Modulacion:** analogica O digital permitida (Art. 4)
- **Ancho de banda:** 11 kHz, canalizacion 12.5 kHz
- **Tipo de estacion:** moviles transportables (handheld, vehicular)
- **Estaciones base/fijas:** solo entidades publicas, con autorizacion
- **GPS:** explicitamente permitida la transmision de datos de posicionamiento (Art. 5)
- **Llamada selectiva por tono:** explicitamente permitida (CTCSS/DCS)

### Lo que dice sobre 140.970 MHz textualmente

> "Las instituciones Publicas y/o agrupaciones que circulen o esten establecidas en zonas inhospitas y rurales estaran atentas en 140.970 MHz para asistir a los necesitados."

Esto convierte la alerta EMERGENCY de MotoRFAR en una emision potencialmente recibida por Gendarmeria, Policia Rural, Defensa Civil y otros grupos M.T.T.T. en kilometros a la redonda. Es un valor agregado del proyecto.

### Homologacion del equipo

El Art. 4 exige que los equipos esten registrados en el Registro de Materiales de Telecomunicaciones de ENACOM. Esto se requiere para comercializacion, no para uso experimental individual. Ver `docs/04-ROADMAP.md` para fases.

## Por que NO PMR446, FRS u otras

Investigacion exhaustiva descarto otras opciones:

### PMR446 europeo (446 MHz)
- **NO esta adoptado en Argentina** como banda exenta
- Argentina es Region 2 de la UIT, PMR446 es de Region 1
- Los walkies que venden en MercadoLibre operando ahi estan en zona gris

### FRS/GMRS norteamericano (462-467 MHz)
- **Asignada a servicios comerciales** por Res. ENACOM 1191/2020
- ENACOM ordeno migracion de servicios fuera de 450-470 MHz
- No es banda libre en Argentina

### Res. 2750/98 - Uso Familiar UHF (462.5625-462.7125 MHz)
- Esta SI es libre pero con restricciones tecnicas incompatibles con kv4p HT:
  - Antena fija obligatoria (sin conector RF externo)
  - Maximo 500 mW
  - Modulacion solo F3E analogica
  - Timeout 3 minutos TX continua
  - Llamada selectiva con minimo 32 codigos
  - Homologacion individual del modelo
- El SA818-U con SMA externo NO califica
- Rediseño de hardware seria mayor

### Bandas de radioaficionado (144-148 MHz, 430-440 MHz)
- Requieren licencia ASOC ENACOM
- Target user de MotoRFAR no la tiene ni quiere sacarla
- Descartado como modo principal (ver `docs/03-DECISIONES.md` ADR-004)

## Marco regulatorio complementario

| Norma | Aplicacion |
|---|---|
| **Ley 19.798** | Marco general de telecomunicaciones |
| **Res. ENACOM 682/2023** | Reglamento administracion espectro |
| **Res. ENACOM 1133/2023** | Permisos uso experimental temporario (150 UTR/mes) |
| **Res. ENACOM 57/2026** | Nuevo regimen RAMATEL (homologacion) |
| **Res. SC 729/80** | Procedimiento homologacion equipos |
| **Res. ENACOM 1021/2025** | UTR vigente: $963.01 |

## Terminos y Condiciones de la app

Debe contener obligatoriamente:

1. Identificacion de Res. 5/2015 como marco habilitante
2. Restriccion de TX a las 3 frecuencias autorizadas
3. Aclaracion que el equipo es experimental open source
4. Limite de potencia maximo (5W estandar movil terrestre)
5. Responsabilidad del usuario por operar dentro del marco
6. Licencia GPL-3.0 heredada de kv4p HT
7. Datos personales: GPS local, no se envia a servidores

Ver implementacion actual en `KV4PHT/app/src/main/res/layout/activity_terms.xml`.

## Que pasa si vamos a v2 con LTE bridge

La conexion VHF -> Internet (gateway) introduce dos consideraciones nuevas:

1. **Datos personales (Ley 25.326):** el almacenamiento de coordenadas GPS en servidor implica responsabilidades. Solucionable con encriptacion end-to-end y opt-in explicito.
2. **Servicio de valor agregado:** un gateway/cloud puede o no requerir registro como SVA en ENACOM. Consultar antes de implementar.

Para v1.0 esto NO aplica porque todo es local.
