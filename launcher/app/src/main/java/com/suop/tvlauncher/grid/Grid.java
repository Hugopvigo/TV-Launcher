package com.suop.tvlauncher.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Rejilla de apps en 4 columnas, con un dock aparte.
 *
 * El dock se fija por paquete (en el orden pedido) y se saca de la rejilla.
 * Si el dock pide menos de cuatro apps, se completa con las que queden, de
 * modo que la fila del dock siempre tiene cuatro iconos.
 */
public final class Grid {

    /** Numero de columnas de la rejilla y del dock. */
    public static final int COLUMNS = 4;

    private final List<GridItem> apps;   // apps de la rejilla, ya ordenadas
    private final List<GridItem> dock;   // apps del dock, en el orden pedido
    private final int columns;

    private Grid(List<GridItem> apps, List<GridItem> dock, int columns) {
        this.apps = Collections.unmodifiableList(apps);
        this.dock = Collections.unmodifiableList(dock);
        this.columns = columns;
    }

    /**
     * Rejilla sin dock.
     */
    public static Grid of(List<GridItem> items, int columns) {
        return of(items, columns, Collections.<String>emptyList());
    }

    /**
     * Rejilla con dock: {@code dockPackages} en el orden deseado.
     *
     * El dock contiene exactamente los paquetes pedidos que esten instalados,
     * en ese orden y como mucho cuatro. No se rellena con otras apps: si una
     * de las pedidas no esta, el dock tiene menos iconos, pero nunca aparece
     * una app que el usuario no eligio.
     */
    public static Grid of(List<GridItem> items, int columns, List<String> dockPackages) {
        Set<String> wanted = new LinkedHashSet<>(dockPackages);

        List<GridItem> dock = new ArrayList<>();
        for (String pkg : wanted) {
            for (GridItem item : items) {
                if (item.packageName.equals(pkg) && !dock.contains(item)) {
                    dock.add(item);
                }
            }
        }

        List<GridItem> rest = new ArrayList<>();
        for (GridItem item : items) {
            if (!dock.contains(item)) {
                rest.add(item);
            }
        }
        if (dock.size() > COLUMNS) {
            dock = new ArrayList<>(dock.subList(0, COLUMNS));
        }

        sort(rest);
        return new Grid(rest, dock, columns);
    }

    private static void sort(List<GridItem> items) {
        Collections.sort(items, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    /** Numero de filas de la rejilla. */
    public int rows() {
        return size() == 0 ? 0 : (size() + columns - 1) / columns;
    }

    /** Numero de columnas. */
    public int columns() {
        return columns;
    }

    /** Numero de apps en la rejilla (sin contar el dock). */
    public int size() {
        return apps.size();
    }

    /** Apps del dock, en orden. */
    public List<GridItem> dockItems() {
        return dock;
    }

    /** Paquetes del dock, en orden. */
    public List<String> dockPackages() {
        List<String> out = new ArrayList<>(dock.size());
        for (GridItem item : dock) {
            out.add(item.packageName);
        }
        return out;
    }

    /**
     * App en una posicion de la rejilla, o vacio si la celda no existe
     * (la ultima fila puede quedar incompleta).
     */
    public java.util.Optional<GridItem> at(int row, int column) {
        int index = row * columns + column;
        if (row < 0 || row >= rows() || column < 0 || column >= columns) {
            return java.util.Optional.empty();
        }
        if (index < 0 || index >= apps.size()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(apps.get(index));
    }

    /** Etiqueta legible para depurar. */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "Grid{apps=%d, dock=%d, columns=%d}", size(), dock.size(), columns);
    }
}
