package ar.motorfar.app.ui;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.List;
import java.util.Arrays;

public class ToneHelper {

    private static final int SAMPLE_RATE = 44100;

    public static void playPttDown(float volume) {
        playTone(800, 30, volume, true);
    }

    public static void playPttUp(float volume) {
        playTone(600, 30, volume, true);
    }

    public static void playAlertBeep(float volume) {
        playTone(1200, 100, volume, false);
        sleep(50);
        playTone(1200, 100, volume, false);
    }

    public static void playEmergencyBeep(float volume) {
        playTone(800,  150, volume, false);
        sleep(30);
        playTone(1000, 150, volume, false);
        sleep(30);
        playTone(1200, 150, volume, false);
    }

    /**
     * Tono de advertencia para cuenta regresiva.
     * @param progress 0.0 a 1.0 (cuanto más cerca de 1, más agudo y rápido)
     */
    public static void playCountdownBeep(float progress, float volume) {
        int freq = (int) (600 + (progress * 1400)); // De 600Hz a 2000Hz
        int duration = (int) (150 - (progress * 100)); // De 150ms a 50ms
        playTone(freq, duration, volume, false);
    }

    public static void playStaticBurst(float volume) {
        playNoise(80, volume * 0.3f);
    }

    /**
     * Sonido de fin de transmisión estilo VHF real: tono de cortesía ("roger
     * beep") agudo y limpio seguido de una breve cola de squelch (static tail).
     * Corre en su propio hilo para no bloquear la UI al encadenar tono + ruido.
     */
    public static void playRogerBeep(float volume) {
        new Thread(() -> {
            playTone(1500, 80, volume, true);   // roger beep
            sleep(15);
            playNoise(110, volume * 0.22f);     // cola de squelch
        }).start();
    }

    private static void playTone(int freqHz, int durationMs, float volume, boolean fadeOut) {
        try {
            int numSamples = SAMPLE_RATE * durationMs / 1000;
            short[] samples = new short[numSamples];
            for (int i = 0; i < numSamples; i++) {
                double fade = fadeOut ? 1.0 - (double) i / numSamples : 1.0;
                samples[i] = (short) (Short.MAX_VALUE * volume * fade
                        * Math.sin(2 * Math.PI * freqHz * i / SAMPLE_RATE));
            }
            writeAndPlay(samples);
        } catch (Exception ignored) {}
    }

    private static void playNoise(int durationMs, float volume) {
        try {
            int numSamples = SAMPLE_RATE * durationMs / 1000;
            short[] samples = new short[numSamples];
            for (int i = 0; i < numSamples; i++) {
                samples[i] = (short) (Short.MAX_VALUE * volume * (Math.random() * 2 - 1));
            }
            writeAndPlay(samples);
        } catch (Exception ignored) {}
    }

    private static void writeAndPlay(short[] samples) {
        int minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        int bufSize = Math.max(minBuf, samples.length * 2);

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM) // Prioridad alta
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED) // Obligatorio en algunos sistemas
                .build();

        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufSize)
                .build();
        track.write(samples, 0, samples.length);
        track.play();
        track.release();
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
    // Valid tones as doubles for numeric comparison
    private static final double[] VALID_TONE_VALUES = {
            67, 71.9, 74.4, 77, 79.7, 82.5, 85.4, 88.5,
            91.5, 94.8, 97.4, 100, 103.5, 107.2, 110.9, 114.8,
            118.8, 123, 127.3, 131.8, 136.5, 141.3, 146.2, 151.4,
            156.7, 162.2, 167.9, 173.8, 179.9, 186.2, 192.8, 203.5,
            210.7, 218.1, 225.7, 233.6, 241.8, 250.3
    };

    // String representations for exact matching
    public static final List<String> VALID_TONE_STRINGS = Arrays.asList(
            "None", "67", "71.9", "74.4", "77", "79.7", "82.5", "85.4", "88.5",
            "91.5", "94.8", "97.4", "100", "103.5", "107.2", "110.9", "114.8",
            "118.8", "123", "127.3", "131.8", "136.5", "141.3", "146.2", "151.4",
            "156.7", "162.2", "167.9", "173.8", "179.9", "186.2", "192.8", "203.5",
            "210.7", "218.1", "225.7", "233.6", "241.8", "250.3"
    );

    public static boolean isValidTone(String tone) {
        return VALID_TONE_STRINGS.contains(tone);
    }

    /**
     * @param inputTone An rx or tx tone as it would be shown in the UI, such as "None", "82.5", or "100.0".
     * @return A normalized version of the time (e.g. closest valid tone within reason), or "None".
     */
    public static String normalizeTone(String inputTone) {
        if (inputTone == null || inputTone.trim().isEmpty()) {
            return "None";
        }

        String tone = inputTone.trim();

        // First check if it's an exact string match (including "None")
        if (isValidTone(tone)) {
            return tone;
        }

        // Try to parse as number
        try {
            double inputValue = Double.parseDouble(tone);

            // Special cases that should default to None
            if (inputValue == 0.0 || inputValue == 1.0) {
                return "None";
            }

            // Find the closest valid tone within 1.0 Hz tolerance
            double closestTone = -1;
            double minDistance = Double.MAX_VALUE;

            for (double validTone : VALID_TONE_VALUES) {
                double distance = Math.abs(inputValue - validTone);
                if (distance <= 1.0 && distance < minDistance) {
                    closestTone = validTone;
                    minDistance = distance;
                }
            }

            if (closestTone != -1) {
                // Return the string representation of the closest valid tone
                if (closestTone == (int)closestTone) {
                    return String.valueOf((int)closestTone); // e.g., 100 instead of 100.0
                } else {
                    return String.valueOf(closestTone);
                }
            }

        } catch (NumberFormatException e) {
            // Not a valid number, fall through to return "None"
        }

        // If we get here, the input wasn't a valid tone
        return "None";
    }

    /**
     * @param tone A rx or tx tone to find the index of, or "None" if no tone.
     * @return The index of the tone, as expected by a DRA818 or SA818S radio module.
     */
    public static int getToneIndex(String tone) {
        if (tone == null) {
            return -1;
        }

        // Normalize the tone first (handles numeric imprecision)
        String normalizedTone = normalizeTone(tone);

        // Find the index in VALID_TONE_STRINGS
        return VALID_TONE_STRINGS.indexOf(normalizedTone);
    }
}
