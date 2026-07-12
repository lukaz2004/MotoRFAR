package ar.motorfar.app.radio;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC de integridad para el transporte UDP -- ver diseño completo y sus límites
 * reales en _PROYECTO/AUTH_UDP_DISENO.md. NO wireado todavía al envío real de
 * paquetes: activarlo requiere que el firmware sepa validar el tag agregado
 * (ver el documento), así que hoy es solo la utilidad, sin uso en producción.
 * El secreto es la clave WPA2 vigente del equipo (ver
 * MainActivity.pairedWifiPassword) -- mismo trade-off ya documentado: no
 * protege contra alguien que ya tiene esa clave, sí contra tráfico corrupto,
 * mal formado o repetido (junto con el chequeo de secuencia del lado firmware).
 */
public final class UdpAuth {

    public static final int TAG_LEN = 8;

    private UdpAuth() {}

    /** @return los primeros {@link #TAG_LEN} bytes del HMAC-SHA256(key, data), o null si la clave es inválida. */
    public static byte[] computeTag(byte[] key, byte[] data) {
        if (key == null || key.length == 0) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] full = mac.doFinal(data);
            byte[] tag = new byte[TAG_LEN];
            System.arraycopy(full, 0, tag, 0, TAG_LEN);
            return tag;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return null;
        }
    }

    public static byte[] keyFromPassword(String password) {
        return password.getBytes(StandardCharsets.US_ASCII);
    }

    /** Comparación en tiempo constante -- evita filtrar por timing cuánto matchea un tag falsificado. */
    public static boolean tagsMatch(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
