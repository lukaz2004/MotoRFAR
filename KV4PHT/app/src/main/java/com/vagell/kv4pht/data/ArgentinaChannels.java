/*
MotoRFAR — Canales PMR446 Argentina
Frecuencias ENACOM de uso libre sin licencia
Licencia: GNU GPL v3
*/

package com.vagell.kv4pht.data;

import java.util.ArrayList;
import java.util.List;

public class ArgentinaChannels {

    public static final String PRELOADED_KEY   = "argentina_channels_preloaded";
    public static final String PRELOADED_VALUE = "v2_pmr446";

    /**
     * Devuelve los 16 canales PMR446 habilitados por ENACOM.
     * Banda 446 MHz, separación 12.5 kHz, uso libre sin licencia.
     */
    public static List<ChannelMemory> getAll() {
        List<ChannelMemory> channels = new ArrayList<>();

        double[] pmrFreqs = {
            446.0063, 446.0188, 446.0313, 446.0438,
            446.0563, 446.0688, 446.0813, 446.0938,
            446.1063, 446.1188, 446.1313, 446.1438,
            446.1563, 446.1688, 446.1813, 446.1938
        };

        for (int i = 0; i < pmrFreqs.length; i++) {
            ChannelMemory ch = new ChannelMemory();
            ch.name = "PMR446 Canal " + (i + 1);
            ch.frequency = String.format(java.util.Locale.US, "%.4f", pmrFreqs[i]);
            ch.offset = ChannelMemory.OFFSET_NONE;
            ch.offsetKhz = 0;
            ch.group = "PMR446";
            channels.add(ch);
        }

        return channels;
    }
}
