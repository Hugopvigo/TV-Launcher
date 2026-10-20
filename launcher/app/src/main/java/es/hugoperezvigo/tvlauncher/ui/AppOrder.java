package es.hugoperezvigo.tvlauncher.ui;

import es.hugoperezvigo.tvlauncher.grid.GridItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Orden de las apps en la rejilla.
 *
 * Tres bloques, en este orden:
 *   1. Las prioritarias, en el orden indicado.
 *   2. El resto, alfabeticamente.
 *   3. Las marcadas como ultimas.
 *
 * Los paquetes que no esten instalados se ignoran.
 */
public final class AppOrder {

    private AppOrder() {
    }

    /**
     * Ordena las apps segun las listas de prioridad y de ultimas.
     */
    public static List<GridItem> sort(List<GridItem> apps,
                                       List<String> first,
                                       List<String> last) {
        List<GridItem> result = new ArrayList<>(apps);
        Collections.sort(result, new Comparator<GridItem>() {
            @Override
            public int compare(GridItem a, GridItem b) {
                int rankA = rank(a.packageName, first, last);
                int rankB = rank(b.packageName, first, last);
                if (rankA != rankB) {
                    return Integer.compare(rankA, rankB);
                }
                // Dentro del mismo bloque, el orden de prioridad manda.
                int posA = position(a.packageName, first);
                int posB = position(b.packageName, first);
                if (posA >= 0 && posB >= 0 && posA != posB) {
                    return Integer.compare(posA, posB);
                }
                int lastA = position(a.packageName, last);
                int lastB = position(b.packageName, last);
                if (lastA >= 0 && lastB >= 0 && lastA != lastB) {
                    return Integer.compare(lastA, lastB);
                }
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return result;
    }

    /** Bloque: 0 prioritarias, 1 resto, 2 ultimas. */
    private static int rank(String pkg, List<String> first, List<String> last) {
        if (contains(first, pkg)) {
            return 0;
        }
        if (contains(last, pkg)) {
            return 2;
        }
        return 1;
    }

    /** Posicion en la lista, o -1 si no esta. */
    private static int position(String pkg, List<String> list) {
        return list.indexOf(pkg);
    }

    private static boolean contains(List<String> list, String pkg) {
        return list != null && list.contains(pkg);
    }
}
