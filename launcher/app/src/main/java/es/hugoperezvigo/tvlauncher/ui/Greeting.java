package es.hugoperezvigo.tvlauncher.ui;

/**
 * Saludo segun la hora del dia.
 */
public enum Greeting {

    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT;

    /** Saludo para una hora dada (0-23). */
    public static Greeting forHour(int hour) {
        if (hour >= 6 && hour < 12) {
            return MORNING;
        }
        if (hour >= 12 && hour < 20) {
            return AFTERNOON;
        }
        if (hour >= 20) {
            return EVENING;
        }
        return NIGHT;
    }
}
