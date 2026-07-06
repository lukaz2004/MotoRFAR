# Navegación turn-by-turn propia — diseño

**Fecha:** 2026-07-05
**Estado:** Aprobado para pasar a plan de implementación. Necesita pulido en una próxima pasada (ver "Abierto para pulir" al final) antes de considerarse definitivo.

## Problema

Baqueano hoy no calcula rutas. El mapa interno (OSMDroid) solo muestra la posición del grupo y el track propio (`RoutePoint`, agrupado por `sessionId`). "IR A UBICACIÓN" abre Google Maps por intent `geo:` para navegación real, porque no existe API pública para inyectar waypoints dentro de una navegación activa de Google Maps.

El usuario típico anda en moto/4x4/bici/a pie en zonas sin cobertura de datos — cualquier solución de navegación tiene que funcionar 100% offline.

## Alcance

Navegación turn-by-turn completa: cálculo de ruta punto a punto, indicaciones de giro (visual + voz), todo offline. No es un ajuste chico — es un subsistema nuevo dentro de la app.

## Motor de ruteo: BRouter

Evaluados 3 motores:
- **BRouter (elegido):** Java puro, sin NDK. GPL-3.0, mismo esquema de licencia que ya usa Baqueano como fork de kv4p-ht. Los datos de rutas ya vienen pre-procesados y publicados por el proyecto (tiles `.rd5` de 5°×5°, servidos en `https://brouter.de/brouter/segments4/`) — no hace falta armar un pipeline de OSM propio. Perfil genérico incluido (tipo "car"/"trekking"), suficiente para el caso de uso (un solo perfil sirve para moto/4x4/bici/a pie).
- GraphHopper: descartado — instrucciones de giro más pulidas, pero requiere generar y hostear nosotros mismos los archivos de grafo desde un extracto OSM. Infraestructura que no tenemos hoy.
- Valhalla: descartado — C++/NDK, complejidad injustificada para un perfil único.

## Datos de ruteo: tamaño real (verificado)

- Extracto OSM crudo de Argentina (Geofabrik, `.osm.pbf`): 403 MB.
- Tiles BRouter (`.rd5`) que cubren Argentina: rango observado ~1–50 MB por tile de 5°×5°, ~20–25 tiles necesarios para cubrir el país → estimado **400–600 MB total**.
- Comparación: tiles raster (imágenes) que ya usa OSMDroid para lo visual son una categoría totalmente distinta y mucho más pesada — cachear el país completo a zoom útil (10-16) entraría en decenas de GB. Por eso los tiles visuales **no** se fuerzan a nivel país; siguen descargándose por área bajo demanda como ya hace `OfflineTilesDialog`.

## Arquitectura y componentes

Paquete nuevo `nav/` en la app, sin modificar lo existente — se monta sobre `MapScreen.kt`:

- **`RouteEngine`** — wrapper del motor BRouter. Corre el cálculo de ruta en background thread, nunca en el hilo principal.
- **`RouteDataManager`** — descarga y trackea qué tiles `.rd5` ya están en disco. Gestiona la descarga inicial y la descarga en background del resto del país (ver estrategia de descarga abajo).
- **`NavigationOverlay`** — dibuja la polyline de la ruta calculada + un cartel de "próximo giro" sobre el `MapView` ya existente.
- **`NavigationVoiceGuide`** — TextToSpeech que anuncia el giro. Pide audio focus transitorio con **ducking** (baja el volumen del radio VHF un instante en vez de cortarlo), coordinando con el manejo de audio focus que ya tiene `RadioAudioService` para PTT/recepción.
- **`NavigationSession`** — guarda la ruta activa y el paso actual, detecta desvío de ruta (distancia perpendicular a la polyline sobre un umbral) y dispara recálculo automático.

## Flujo de datos

1. Usuario elige destino: tocando un punto del mapa interno, **o** seleccionando la última posición conocida de un miembro del grupo (reusa la infraestructura de WAYPOINT existente).
2. `NavigationSession` pide ruta a `RouteEngine` (origen = último fix GPS, destino = elegido).
3. `RouteEngine` calcula sobre los tiles `.rd5` ya descargados, en background thread.
4. Devuelve polyline + lista de instrucciones de giro.
5. `NavigationOverlay` dibuja la ruta y el cartel de próximo giro sobre el mapa.
6. A medida que avanza el GPS, `NavigationSession` actualiza el paso actual, dispara la voz (`NavigationVoiceGuide`) al acercarse a un giro, y si detecta desvío de la ruta, recalcula automáticamente.

## Estrategia de descarga (sin bloquear la app)

La comunicación por VHF es la función crítica del producto — nunca puede depender de que termine una descarga de datos de ruteo. Por eso:

1. **Al primer inicio (con GPS ya con fix):** descarga obligatoria pero acotada de un solo tile `.rd5` — el que cubre la posición actual del usuario (~15-50 MB según densidad de la zona). Rápido incluso con señal débil, y ya cubre la zona donde el usuario arranca.
2. **Resto del país:** se descarga en background, sin bloquear ninguna otra función de la app, reanudable si se corta la conexión.
3. **Al pedir una ruta hacia una zona sin datos descargados todavía:** cartel de advertencia explícito — *"Esta ruta pasa por una zona sin datos offline descargados. Sin señal para completarla podés quedarte sin indicaciones en ese tramo. ¿Salir igual o esperar la descarga?"* — la decisión y el riesgo quedan en manos del usuario, nunca se falla en silencio ni se bloquea la app.
4. Sin datos de ruteo en absoluto para el punto de partida (falló hasta el tile inicial): no se puede calcular ruta, se informa claramente. El resto de la app (radio, grupo, chat, waypoints) sigue funcionando con normalidad — nunca depende de este subsistema.

Los tiles visuales (fondo del mapa) pueden faltar en zonas no visitadas — se ve gris debajo, pero la ruta calculada y las indicaciones de giro siguen funcionando igual, porque salen de los datos de ruteo + GPS, no de la imagen del mapa.

## Manejo de errores (resumen)

- Sin datos de ruteo para el punto de partida → bloquea el cálculo, mensaje claro.
- Sin fix GPS → no arranca navegación, avisa.
- Ruta hacia zona sin datos descargados → advertencia explícita, decisión del usuario (ver arriba).
- Desvío de la ruta en curso → recalcula automático desde la posición actual.
- Falla la descarga en background (sin señal) → reintento posterior, no bloquea nada mientras tanto.

## Testing

- `RouteEngine` y `NavigationSession`: unit tests con fixtures de GPS fijos y datos de tiles falsos, sin dispositivo.
- `NavigationOverlay` y `NavigationVoiceGuide`: dependen de mapa real/TTS — verificación manual en emulador/dispositivo, como ya se hizo con Man-Down.

## Abierto para pulir (antes de dar por definitivo)

- Tamaño exacto y criterio del "tile inicial obligatorio" — hoy se define como "el tile que cubre el GPS al primer inicio"; falta decidir qué pasa si el primer inicio ocurre sin fix GPS todavía (¿esperar el fix antes de arrancar la descarga, o usar la última ubicación conocida por otro medio?).
- UI exacta del cartel de advertencia (dónde aparece, se puede desactivar, se repite cada vez o solo la primera).
- Integración fina del ducking de audio con `RadioAudioService` — qué pasa si una instrucción de giro y una transmisión real del grupo coinciden en el mismo instante.
- Selección de destino sobre miembro del grupo: si esa posición no tiene fix reciente, comportamiento a definir.
