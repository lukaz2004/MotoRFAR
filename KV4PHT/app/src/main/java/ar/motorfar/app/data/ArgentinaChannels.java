/*
MotoRFAR — Canales VHF Resolución 5/2015 (M.T.T.T.)
Frecuencias de uso libre sin licencia para travesías, expediciones,
motociclismo y actividades en zonas rurales/inhóspitas.
Licencia: GNU GPL v3
*/

package ar.motorfar.app.data;

import java.util.Arrays;
import java.util.List;

// 139.970 MHz — prioritaria (Grupo) · 138.510 — secundaria (Alternativo)
// 140.970 MHz — EXCLUSIVA emergencias (Emergencia)
public class ArgentinaChannels {

    public static final String PRELOADED_KEY   = "argentina_channels_preloaded";
    public static final String PRELOADED_VALUE = "v8_channels_principal";

    // 2026-07-06: tonos CTCSS por defecto para Grupo/Alternativo — dejan que
    // varios grupos compartan el mismo canal legal sin escucharse entre sí
    // (ver PENDIENTES.md, "Feature — CTCSS/DCS por canal"). CTCSS filtra lo
    // que se ESCUCHA, no separa el RF real: si dos grupos transmiten a la vez
    // igual colisionan. Emergencia queda SIN tono a propósito — debe ser
    // audible para cualquiera, no filtrada.
    private static final String GROUP_CTCSS       = "100.0";
    private static final String ALTERNATIVO_CTCSS = "123.0";

    public static List<ChannelMemory> getAll() {
        return Arrays.asList(
            channel("PRINCIPAL",   "139.9700", GROUP_CTCSS),
            channel("ALTERNATIVO", "138.5100", ALTERNATIVO_CTCSS),
            channel("EMERGENCIA",  "140.9700", null)
        );
    }

    private static ChannelMemory channel(String name, String frequency, String ctcssTone) {
        ChannelMemory ch = new ChannelMemory();
        ch.name      = name;
        ch.frequency = frequency;
        ch.offset    = ChannelMemory.OFFSET_NONE;
        ch.offsetKhz = 0;
        ch.group     = "MTTT";
        ch.txTone    = ctcssTone;
        ch.rxTone    = ctcssTone;
        return ch;
    }
}
