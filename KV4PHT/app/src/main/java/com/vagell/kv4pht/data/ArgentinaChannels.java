/*
MotoRFAR — Canales VHF Resolución 5/2015 (M.T.T.T.)
Frecuencias de uso libre sin licencia para travesías, expediciones,
motociclismo y actividades en zonas rurales/inhóspitas.
Licencia: GNU GPL v3
*/

package com.vagell.kv4pht.data;

import java.util.Arrays;
import java.util.List;

// 139.970 MHz — prioritaria (Grupo) · 138.510 — secundaria (Alternativo)
// 140.970 MHz — EXCLUSIVA emergencias (Emergencia)
public class ArgentinaChannels {

    public static final String PRELOADED_KEY   = "argentina_channels_preloaded";
    public static final String PRELOADED_VALUE = "v4_principal_vhf";

    public static List<ChannelMemory> getAll() {
        return Arrays.asList(
            channel("Principal",       "139.9700"),
            channel("Alternativo", "138.5100"),
            channel("Emergencia",  "140.9700")
        );
    }

    private static ChannelMemory channel(String name, String frequency) {
        ChannelMemory ch = new ChannelMemory();
        ch.name      = name;
        ch.frequency = frequency;
        ch.offset    = ChannelMemory.OFFSET_NONE;
        ch.offsetKhz = 0;
        ch.group     = "MTTT";
        return ch;
    }
}
