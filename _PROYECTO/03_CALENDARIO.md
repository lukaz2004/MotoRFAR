# MOTO HT — Calendario y Secuencia

> No son fechas. Es el ORDEN y las DEPENDENCIAS. Última revisión: 2026-06-23.
> F0 ya está decidido (WiFi). El diagrama refleja el estado real de hoy.

---

## DIAGRAMA DE DEPENDENCIAS (estado actual)

```
✅ F0 (WiFi decidido)
✅ FW-1 (compila+flashea)
✅ FW-2 (código+flash OK)        ← falta: test RF con SA818
✅ FW-3a (Hello OK por WiFi)     ← falta: --open-rx + failsafe con SA818

          ┌──────────────────────────┐
          │  SA818 físico disponible │  ← DESBLOQUEADOR HARDWARE
          └──────────┬───────────────┘
                     ▼
        FW-2 test RF · FW-3a --open-rx · FW-3a failsafe · FW-4 J2 test

✅ APP-1 (sprints 1-9 mergeados)
   ├── ⬜ Tiles offline OSMDroid    ← arrancable HOY
   ├── ⬜ Privacy Policy            ← arrancable HOY
   └── ⬜ GitHub Release v1.0-beta1 ← tras tiles + privacy

⬜ APP-2 (WiFi client)  ← arranca tras FW-3a validado completo
   └── depende de: Contrato A (✅ definido), FW-3a validado

⬜ HW-1 pendiente: DRC GUI + decisión extensión (recomendado: cerrar 82.55mm HOY)
⬜ HW-2/3/4 ← tras HW-1

⬜ EST-1 (carcasa paramétrica) ← arrancable HOY
⬜ EST-2/3 ← bloqueada por HW-4

⬜ WEB ← arrancable en paralelo con APP-2

⬜ INT ← todo lo anterior
```

---

## OLA ACTUAL — Arrancable sin hardware (AHORA)

| Tarea | Rama | Notas |
|-------|------|-------|
| **Tiles offline OSMDroid** | APP-1 | Botón en MapScreen + OfflineDownloadManager + dialog |
| **Privacy Policy** | APP-1 | `PRIVACY-POLICY.md` + pantalla Settings |
| **GitHub Release v1.0-beta1** | APP-1 | Tras tiles + privacy. `gh release create` |
| **DRC GUI KiCad + decisión 82.55mm** | HW-1 | Abre el camino a HW-2/3/4 |
| **EST-1 carcasa paramétrica** | EST | Sólido Fusion, sin malla STL |
| **WEB borrador** | WEB | Stack, dominio, contenido mínimo |

---

## OLA SIGUIENTE — Requiere SA818 físico

| Tarea | Rama | Notas |
|-------|------|-------|
| FW-3a `--open-rx` | FW | Ver frames RX_AUDIO por WiFi |
| FW-3a failsafe | FW | Cortar WiFi en TX → vuelve a RX |
| FW-2 test RF | FW | TX fuera de whitelist = bloqueado |
| FW-4 test J2 | FW | PTT externo dispara TX |

---

## OLA POST-SA818 — APP-2 y HW

| Tarea | Rama | Notas |
|-------|------|-------|
| **APP-2 WiFi client** | APP | Reemplazar usbSerial → UDP 4210, bindeo socket, keepalive PTT |
| **HW-2** antena dual | HW | WiFi 2.4GHz externa + SMA VHF pigtail |
| **HW-3** alimentación 5V | HW | Solo entrada 5V, SIN power-path (WiFi = no carga USB) |
| **HW-4** Gerbers DRC final | HW | Gate para ordenar JLCPCB |
| **FW-3b** TCP/UDP split | FW | Comandos TCP confiable, audio UDP |
| **APP-3** disclaimer WiFi | APP | Reemplaza el texto OTG |
| **CTCSS/DCS UI** | APP | Post APP-2 |
| **EST-2** ajuste placa final | EST | Bloqueada por HW-4 |

---

## OLA FINAL

| Tarea | Rama |
|-------|------|
| EST-3 (print → CNC aluminio) | EST |
| **INT** ensamblaje + test sistema completo | INT |

---

## GATES DE CONTROL

- **Antes de ordenar PCB:** DRC GUI = 0 críticos. Extensión decidida. Gerbers regenerados post-cambio.
- **Antes de APP-2:** FW-3a validado completo (Hello + RX audio + failsafe). Contrato A cerrado ✅.
- **Antes de venta:** checklist GPL completo (repo público, pantalla licencias, flash libre, revisión legal).
- **Antes de INT:** cada rama pasó su test propio.

---

## ESTIMACIÓN DE ESFUERZO RESTANTE

| Tarea | Esfuerzo |
|-------|----------|
| APP-1 (tiles + privacy + release) | Bajo-Medio |
| APP-2 WiFi client | **Alto** (reescritura capa comunicación) |
| HW-1 cierre (DRC + decisión) | Bajo |
| HW-2/3/4 | Medio |
| FW-3b TCP/UDP | Medio |
| EST-1 | Medio |
| EST-2/3 | Medio |
| WEB | Bajo-Medio |
| INT | Medio |
