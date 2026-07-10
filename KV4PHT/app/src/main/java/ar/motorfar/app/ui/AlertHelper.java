/*
MotoRFAR — Alertas de grupo para motociclistas
Basado en kv4p HT (https://kv4p.com) por Vance Vagell
Licencia: GNU GPL v3
*/

package ar.motorfar.app.ui;

import ar.motorfar.app.radio.TxWhitelist;

/**
 * Tipos y textos de alertas APRS para grupos de moto.
 * Se envían como mensajes APRS con posición GPS adjunta.
 *
 * Niveles:
 *   EMERGENCY    — Situación grave, necesita asistencia urgente (rojo)
 *   STOP         — Detención no planificada, algo a resolver (amarillo)
 *   REGROUP      — Parada voluntaria, esperar al grupo (verde)
 *
 * EMERGENCY se transmite SIEMPRE en 140.970 MHz (canal exclusivo de
 * emergencias Res. 5/2015). Tu propio grupo NO la escucha si no tiene un
 * radio sintonizado justo en ese canal en ese momento -- eso es un hecho
 * verificado en el código (no hay modo de escaneo accesible desde la UI).
 * Que algún radioaficionado ajeno al grupo esté monitoreando esa frecuencia
 * por convención es posible pero no está verificado ni garantizado -- no
 * afirmar ninguna de las dos cosas como certeza en el copy de cara al usuario.
 */
public class AlertHelper {

    /** Frecuencia de emergencia M.T.T.T. en formato "xxx.xxxx" */
    public static final String EMERGENCY_FREQ = "140.9700";

    public enum AlertType {
        EMERGENCY,  // Rojo   — situación grave
        STOP,       // Amarillo — detención no planificada
        REGROUP     // Verde  — reagrupamiento
    }

    /**
     * Texto del mensaje APRS (máx. 67 caracteres).
     */
    public static String buildMessage(AlertType type, String callsign) {
        String id = (callsign != null && !callsign.trim().isEmpty())
            ? callsign.trim().toUpperCase() : "SIN-ID";

        switch (type) {
            case EMERGENCY:
                return "ALERTA " + id + " DETENIDO - SOLICITA CONTACTO";
            case STOP:
                return "DETENCION " + id + " - INCONVENIENTE EN RUTA";
            case REGROUP:
                return "REAGRUPAMIENTO " + id + " - ESPERAR EN POSICION";
            default:
                return "ALERTA " + id;
        }
    }

    public static String getConfirmationTitle(AlertType type) {
        switch (type) {
            case EMERGENCY:   return "⚠ Alerta de emergencia";
            case STOP:        return "Aviso de detención";
            case REGROUP:     return "Reagrupamiento";
            default:          return "Alerta";
        }
    }

    public static String getConfirmationText(AlertType type) {
        switch (type) {
            case EMERGENCY:
                return "Se enviará tu posición GPS y una alerta de emergencia al grupo.\n\n"
                     + "⚠ Se transmite por 140.970 MHz, la frecuencia de emergencias M.T.T.T.\n"
                     + "— tu grupo solo la recibe si su radio está sintonizada a ese canal\n"
                     + "justo en este momento. No hay garantía de eso.\n\n"
                     + "¿Confirmar?";
            case STOP:
                return "Se enviará tu posición GPS y un aviso de detención al grupo.\n\n¿Confirmar?";
            case REGROUP:
                return "Se enviará tu posición GPS y un aviso de reagrupamiento al grupo.\n\n¿Confirmar?";
            default:
                return "¿Enviar alerta?";
        }
    }

    /**
     * Determina la frecuencia objetivo para transmitir la alerta.
     *
     * EMERGENCY → siempre 140.970 MHz (Emergencia Res. 5/2015)
     * STOP / REGROUP → canal activo del usuario
     *
     * @param type         Tipo de alerta
     * @param currentFreq  Frecuencia activa del usuario en formato "xxx.xxxx"
     * @return             Frecuencia destino en formato "xxx.xxxx"
     */
    public static String getTargetFrequency(AlertType type, String currentFreq) {
        if (type == AlertType.EMERGENCY) {
            return EMERGENCY_FREQ;
        }
        return currentFreq;
    }

    public static String getSentConfirmation(AlertType type) {
        switch (type) {
            case EMERGENCY: return "Alerta de emergencia enviada";
            case STOP:      return "Aviso de detención enviado";
            case REGROUP:   return "Aviso de reagrupamiento enviado";
            default:        return "Alerta enviada";
        }
    }
}
