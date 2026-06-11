/*
MotoRFAR — Tests for M.T.T.T. channel definitions (Res. 5/2015)
License: GNU GPL v3
*/

package com.vagell.kv4pht.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.List;

/**
 * Verifies the preloaded Argentina channels match Resolución 5/2015
 * (Secretaría de Comunicaciones): the three license-free VHF frequencies
 * for M.T.T.T. activities (expeditions, motorcycling, remote areas).
 *
 * 139.970 MHz — primary  ("Principal")
 * 138.510 MHz — secondary ("Alternativo")
 * 140.970 MHz — emergencies ONLY ("Emergencia")
 */
public class ArgentinaChannelsTest {

    @Test
    public void getAll_returnsExactlyThreeChannels() {
        List<ChannelMemory> channels = ArgentinaChannels.getAll();
        assertNotNull(channels);
        assertEquals(3, channels.size());
    }

    @Test
    public void firstChannel_isGrupo_139970() {
        ChannelMemory ch = ArgentinaChannels.getAll().get(0);
        assertEquals("GRUPO", ch.name);
        assertEquals("139.9700", ch.frequency);
    }

    @Test
    public void secondChannel_isAlternativo_138510() {
        ChannelMemory ch = ArgentinaChannels.getAll().get(1);
        assertEquals("ALTERNATIVO", ch.name);
        assertEquals("138.5100", ch.frequency);
    }

    @Test
    public void thirdChannel_isEmergencia_140970() {
        ChannelMemory ch = ArgentinaChannels.getAll().get(2);
        assertEquals("EMERGENCIA", ch.name);
        assertEquals("140.9700", ch.frequency);
    }

    @Test
    public void allChannels_belongToMtttGroup_simplexNoOffset() {
        for (ChannelMemory ch : ArgentinaChannels.getAll()) {
            assertEquals("MTTT", ch.group);
            assertEquals(ChannelMemory.OFFSET_NONE, ch.offset);
            assertEquals(0, ch.offsetKhz);
        }
    }

    @Test
    public void preloadVersion_isV6ChannelsTactical() {
        // Bumping this value forces existing installs to re-seed channels.
        assertEquals("v6_channels_tactical", ArgentinaChannels.PRELOADED_VALUE);
    }
}
