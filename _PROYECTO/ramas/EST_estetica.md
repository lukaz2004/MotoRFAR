# RAMA EST — Estética / Carcasa

> Para arrancar: *"Trabajemos ETAPA EST-[N] del proyecto MOTO HT, leé `_PROYECTO\ramas\EST_estetica.md` (etapa EST-N) y seguimos."*
> Leé `01_CONTRATOS.md` (Contrato C).

**Herramienta:** Fusion 360 + MCP bridge (reconectar si se cae).
**Referencia:** KB9VQQ Thin Case. **STLs:** `...\MotoRFAR-MTTT\box v1.stl`, `box v2.stl`.

## Quirks Fusion MCP (documentados, respetar)
- Solo documentos "Part" de componente único → usar BRep bodies con `NewBodyFeatureOperation`.
- `addSimple` (no setSymmetricExtent), `setByOffset` (no setByThreePoints).
- Selección de perfil: el de MAYOR ÁREA (no `item(0)`) cuando hay nervaduras que parten el sketch.
- Librería apariencias en español: buscar por `.name.lower()` substring (ej. 'verde','mate').
- Importar STL: MeshUnits.MillimeterMeshUnit + `meshBodies.add()`.

---

## EST-1 — Reconstrucción paramétrica (ARRANCABLE YA)

### Por qué
La malla STL NO es editable bien y **CNC necesita sólido real, no malla facetada.** Hay que reconstruir la carcasa como sólido paramétrico desde cero, con las medidas reales.

### Decisiones de diseño ya tomadas
- SIN relieve lateral de PTT (va conector externo IP67).
- Ventana USB-C a medida del J5 real.
- Mantener la lógica de forma del KB9VQQ: nervaduras (rigidez), bosses (anti-arqueo), esquinas redondeadas (anti-rajadura).

### Datos
- Placa base: 38.1 × 82.55mm (CONFIRMAR tamaño final con HW-4 antes de finalizar).
- Para CNC fresa 6mm: ningún corte interno con radio < 3mm.

### Pasos
1. Reconstruir cuerpo paramétrico con medidas reales (exterior ~50 × 93 × 15mm del KB9VQQ, ajustar).
2. Bolsillo interno para la placa, con clearance.
3. Bosses + nervaduras como sólido (no copiados de la malla).
4. Esquinas internas con radio ≥ 3mm (compatibilidad fresa 6mm).

### Verificar
- [ ] Es sólido paramétrico (no malla)
- [ ] Sin relieve PTT
- [ ] Radios internos ≥ 3mm para CNC
- [ ] ⚠️ NO finalizar hasta que HW-4 confirme tamaño de placa

---

## EST-2 — Ajuste a placa final + antenas (DEPENDE de HW-4)

### Pre-requisito
- HW-4 cerrado: dimensiones finales y posiciones de conectores (Contrato C).

### Pasos
1. Ajustar bolsillo y contorno al tamaño final de placa.
2. Ventanas/cortes para: USB-C, SMA VHF, antena WiFi (si F0=WiFi), conector PTT, conector alimentación.
3. **Antenas externas:** 2 salidas (VHF + WiFi), pensadas para montaje en alto (techo 4x4/moto). Carcasa aluminio = jaula Faraday; antenas SIEMPRE externas.
4. Puntos de montaje para soporte de moto (manillar).

### Verificar 🔴
- [ ] Todos los conectores coinciden con posiciones reales del Contrato C
- [ ] Antenas externas, separadas
- [ ] Montaje a moto resuelto

---

## EST-3 — Print 3D → CNC aluminio (DEPENDE de EST-1/2)

### Pasos
1. Exportar STL → imprimir en 3D (Ender 3 Pro: 205°C, bed 60°C, retracción 4.5mm — perfil ya calibrado).
2. Validar ajuste físico de la placa real impresa.
3. Corregir lo que haga falta.
4. CAM en Fusion: setup de fresado, fresa 6mm, desbaste + acabado → G-code.
5. (Detalles finos como ventilación → taladro a mano, la fresa 6mm no los hace).

### Verificar
- [ ] Placa entra bien en la impresión de prueba
- [ ] G-code generado con fresa 6mm, radios respetados
- [ ] Plan para detalles finos (taladro manual)

## TESTS DE LA RAMA
- Impresión 3D valida ajuste antes de gastar aluminio.
- G-code simulado sin colisiones.
