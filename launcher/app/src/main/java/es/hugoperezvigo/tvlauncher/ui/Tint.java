package es.hugoperezvigo.tvlauncher.ui;

/**
 * Ajustes de color para el resplandor ambiental.
 */
public final class Tint {

    private Tint() {
    }

    /**
     * Aclara un color multiplicando sus canales, sin pasarse de 255.
     * Sirve para que el resplandor de un banner siga viéndose sobre negro.
     */
    public static int brighter(int color, float factor) {
        int r = (int) Math.min(255, red(color) * factor);
        int g = (int) Math.min(255, green(color) * factor);
        int b = (int) Math.min(255, blue(color) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    /** Canal alpha de un color. */
    public static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }
}
