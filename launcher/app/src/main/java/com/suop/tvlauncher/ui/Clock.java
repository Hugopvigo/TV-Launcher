package com.suop.tvlauncher.ui;

import java.util.Calendar;
import java.util.Locale;

/**
 * Reloj y fecha de la barra superior.
 */
public final class Clock {

    private Clock() {
    }

    /** Hora en formato 24h con dos digitos: "09:05". */
    public static String format(Calendar calendar, Locale locale) {
        return String.format(locale, "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
    }

    /** Fecha larga: "martes, 20 de octubre". */
    public static String formatDate(Calendar calendar, Locale locale) {
        String month = String.format(locale, "%tB", calendar.getTime());
        String day = String.format(locale, "%te", calendar.getTime());
        return month + " " + day;
    }
}
