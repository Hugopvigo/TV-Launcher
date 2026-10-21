package es.hugoperezvigo.tvlauncher.inputs;

import java.util.Locale;

/**
 * Utilidades sobre las entradas de TV, sin dependencias de Android.
 *
 * El framework de TV de Android 9 mezcla dos cosas muy distintas:
 * las entradas de verdad (HDMI, AV, un tuner fisico) y los servicios de
 * *previsualizacion* de las apps de streaming, que el sistema registra como
 * si fueran tuner. En el Mi TV Stick solo existen estos ultimos: por eso el
 * desplegable de «Entradas» tiene que filtrarlos antes de enseñarlos.
 */
public final class TvInputs {

    /** Sintonizador. */
    public static final int TYPE_TUNER = 1;
    /** Video compuesto. */
    public static final int TYPE_COMPOSITE = 2;
    /** HDMI. */
    public static final int TYPE_HDMI = 3;
    /** Entrada virtual (previsualizacion de una app). */
    public static final int TYPE_VIRTUAL = 6;
    /** HDMI fisico. */
    public static final int TYPE_PHYSICAL_HDMI = 7;
    /** Video compuesto fisico. */
    public static final int TYPE_PHYSICAL_COMPOSITE = 9;

    private TvInputs() {
    }

    /**
     * ¿Es un servicio de previsualizacion de video y no una entrada real?
     *
     * Play Peliculas y SkyShowtime registran servicios de este tipo y el
     * sistema los cuenta como tuner: no se puede cambiar a ellos, asi que no
     * se enseñan como entradas.
     */
    public static boolean isPreview(String inputId) {
        if (inputId == null) {
            return false;
        }
        String id = inputId.toLowerCase(Locale.ROOT);
        return id.contains("playback") || id.contains("preview");
    }

    /**
     * ¿Es un tipo de entrada que se puede cambiar de verdad?
     */
    public static boolean isSwitchable(int type) {
        return type == TYPE_HDMI
                || type == TYPE_PHYSICAL_HDMI
                || type == TYPE_COMPOSITE
                || type == TYPE_PHYSICAL_COMPOSITE;
    }

    /**
     * Numero de puerto HDMI deducido del identificador del servicio.
     * El framework no expone el numero en la API publica.
     */
    public static int hdmiNumber(String inputId) {
        if (inputId == null) {
            return 1;
        }
        for (int i = inputId.length() - 1; i >= 0; i--) {
            char c = inputId.charAt(i);
            if (c >= '0' && c <= '9') {
                int digit = c - '0';
                return digit == 0 ? 1 : digit;
            }
        }
        return 1;
    }
}
