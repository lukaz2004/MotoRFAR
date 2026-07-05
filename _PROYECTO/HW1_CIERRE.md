# HW-1 — Verificación DRC completa (2026-07-02)

## 🏁 CIERRE 2026-07-04 — estado final de la sesión
**Hecho hoy (verificado con DRC/ERC real, no a ojo):**
- [x] Borrar stub RF viejo de J1
- [x] Mover texto silkscreen 'Baqueano V 2.0.1'
- [x] Rutear traza RF C5→J1 (se había roto al borrar el stub)
- [x] Rutear conexión real C9→R2 (filtro mic audio — era bug heredado, no basura)
- [x] Limpiar vías/tracks muertos sobrantes de la rama vieja de C9
- [x] Corregir esquemático J2 (cableado invertido Pin1/Pin2)
- [x] Confirmar SW1/SW2 ya excluidos de placa (no hacía falta tocar)
- [x] Confirmar footprints J1/J3 ya correctos (no hacía falta tocar)

**Pendiente, sin tocar, para otro día:**
- [ ] C15/C32 — pin2 debería ir a GND (hoy en `/PTT Button Left`, compartido con
  R12 y J2). Requiere reubicar el símbolo en el esquemático, no es texto. Bajo
  riesgo (no afecta la placa física ya fabricable), pero necesita sesión con
  supervisión en vivo, no edición ciega.

**Próxima sesión:** auditoría punto por punto de todo lo marcado ✅ arriba antes
de dar HW-1 por 100% cerrado — no asumir que "verificado hoy" = "verificado para
siempre". Placa lista para mandar a fabricar en el estado actual.

> Reporte crudo: `drc_2026-07-02.json` (esta carpeta). Esquemático des-BOMeado
> (backup: `kv4p-ht.kicad_sch.bak-BOM-2026-07-02` junto al original).

## ✅ CONFIRMADO POR DRC — no tocar
- **Ruteo PTT de LuKaZ (R14→C33→U5.29→J3): CERO errores.** No aparece en el reporte.
- Conectividad completa: el único "unconnected" es un resto huérfano de C9, no una conexión funcional.
- U5 = WROOM-32U-N4 consistente entre esquemático y PCB.
- J3 existe en ambos lados (símbolo PTT_OPT en sch + PinHeader en PCB).
- Gerbers/producción ya generados (production-vhf / production-uhf).

## ⚠️ CORRECCIÓN 2026-07-04 — el punto 2 original estaba mal diagnosticado
Lo que abajo se llamaba "restos huérfanos de C9" **no es basura**: `Net-(C9-Pad1)`
conecta el pad1 de C9 con el pad2 de R2 (R2-pad1 = `/Radio Mic In`, filtro de
entrada de micrófono hacia el ESP32). La ruta nunca se completó — falta un tramo
real de ~58mm entre el final de la rama de C9 (vía @158.1,132.7) y el inicio de
la rama de R2 (vía @157.4,74.4). Es un bug heredado (no se detectó en el DRC
"cero errores" de la sesión anterior porque nadie verificó que el otro extremo
tocaba un pad real), no cobre para borrar.

Primer intento (a mano, sesión 2026-07-04) se abandonó por generar violaciones
de clearance 0mm contra zonas GND/+5V al no re-rellenar zonas tras editar — se
restauró desde backup (`kv4p-ht.kicad_pcb.bak-HW1-2026-07-04`). Segundo intento
(mismo día, LuKaZ en GUI) **funcionó**: C9→R2 conectado, DRC confirma 0 pads sin
conectar, sin nuevas violaciones de clearance ni zonas partidas. Las 2 vías
muertas que quedaron de la rama vieja (F.Cu-B.Cu conectadas a una sola capa) se
borraron también — reporte final: 90 violaciones, todas heredadas 🟢.

## 🔧 PENDIENTE REAL — PCB, GUI, ~10 min (borrar basura, no rutear nada)
1. **Borrar stub RF viejo de J1**: pista `Net-(J1-In)` 5,46mm en F.Cu @ (150.1, 68.3).
   Resto de la traza del SMA anterior. Elimina 3 violaciones. ✅ HECHO (confirmado
   2026-07-04, ya no aparece en DRC).
2. ~~Borrar restos huérfanos de C9~~ — VER CORRECCIÓN ARRIBA. Era rutear, no borrar.
   ✅ HECHO 2026-07-04 (C9→R2 conectado, confirmado con DRC).
3. **Mover texto 'Baqueano V 2.0.1'** de F.Cu → F.Silkscreen @ (140.5, 82.0).
   Hoy es cobre real y choca con vía GND y zona. Elimina 2. ✅ HECHO (confirmado
   2026-07-04, ya no aparece en DRC).

## ✅ HECHO 2026-07-04 — traza RF C5→J1
El pad2 de C5 (150.876, 68.453) y el pad1 de J1 (154.05, 68.326), ambos en
`Net-(J1-In)`, quedaron desconectados por el borrado del stub viejo (ver arriba,
punto 1). Se re-ruteó a mano en GUI (router interactivo, F.Cu). Confirmado con
DRC: ya no aparece en "elementos no conectados".

## 🔧 PENDIENTE REAL — Esquemático
4. **J2 (PTT_EXT) cableado al revés en el sch**: ✅ HECHO 2026-07-04. Símbolo
   GND (#PWR78) movido 2.54mm para caer exacto en Pin 1, cable de Pin 1 movido
   a Pin 2. Verificado con `kicad-cli sch erc` (0 menciones a J2/#PWR78) y
   netlist (Pin1→GND, Pin2→/PTT Button Left, coincide con el PCB). DRC de
   paridad esquema↔PCB bajó de 95 a 93 (los 2 net_conflict de J2 desaparecieron).
5. **SW1 y SW2 (PTT)**: ✅ ya estaban excluidos (`on_board no`, `dnp yes`) —
   el punto ya estaba resuelto de antes, no hacía falta tocar nada.
6. **Campos Footprint J1/J3**: ✅ ya estaban correctos (J1 = U.FL Hirose,
   J3 = PinHeader_1x02) — el punto ya estaba resuelto de antes.
7. **C15/C32 (pin2 en /PTT Button Left, PCB los tiene a GND)**: ⚠️ SIGUE
   PENDIENTE, no se tocó. Es más invasivo de lo que parece: el pin2 de ambos
   comparte nodo cableado con R12 y J2 (el mismo bus /PTT Button Left) —
   mover solo ese pin a GND requiere reubicar el símbolo, no es un cambio de
   texto. Documentado desde el inicio como "decisión final de LuKaZ" — no se
   resuelve sin supervisión en vivo. 100% cosmético/documentación, no afecta
   la placa física ni bloquea fabricación.

## 🟢 HEREDADO — no bloquea JLCPCB, se documenta y NO se trabaja
- 4× hole_clearance en J5 (USB-C): agujeros NPTH del propio conector a 0,185mm
  de sus pads GND (regla del proyecto pide 0,2). Margen del footprint, fabricable.
- 3× copper_edge: pads GND de J2/U1 al borde de placa — diseño original a propósito.
- 2× items_not_allowed: R12/U2 dentro del keepout de antena de U5 — con el
  WROOM-32U (antena externa) el keepout ya no tiene razón física.
- 80× lib_footprint_mismatch + 85× campo Datasheet: mantenimiento de librería,
  cero impacto eléctrico ni de fabricación.

## Criterio de cierre HW-1
Hecho lo de arriba (puntos 1–6) y re-corrido el DRC:
`kicad-cli pcb drc` debe dar **0 errores nuevos** (los heredados 🟢 pueden
excluirse en el diálogo DRC o quedar documentados acá). Ahí HW-1 pasa a
CERRADO.md con este archivo como evidencia — y no se toca nunca más.
