package com.suop.tvlauncher.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rejilla de apps en 4 columnas, a todo el ancho.
 *
 * No hay dock: todas las apps se muestran en la rejilla, en el orden en que
 * se pasan.
 */
public final class Grid {

    /** Numero de columnas de la rejilla. */
    public static final int COLUMNS = 4;

    private final List<GridItem> apps;
    private final int columns;

    private Grid(List<GridItem> apps, int columns) {
        this.apps = Collections.unmodifiableList(apps);
        this.columns = columns;
    }

    /**
     * Rejilla con las apps dadas, en ese orden.
     */
    public static Grid of(List<GridItem> items, int columns) {
        List<GridItem> copy = new ArrayList<>(items);
        return new Grid(copy, Math.max(1, columns));
    }

    /** Numero de filas de la rejilla. */
    public int rows() {
        return size() == 0 ? 0 : (size() + columns - 1) / columns;
    }

    /** Numero de columnas. */
    public int columns() {
        return columns;
    }

    /** Numero de apps en la rejilla. */
    public int size() {
        return apps.size();
    }

    /** Las apps en su orden. */
    public List<GridItem> items() {
        return apps;
    }

    /**
     * App en una posicion de la rejilla, o vacio si la celda no existe
     * (la ultima fila puede quedar incompleta).
     */
    public java.util.Optional<GridItem> at(int row, int column) {
        if (row < 0 || row >= rows() || column < 0 || column >= columns) {
            return java.util.Optional.empty();
        }
        int index = row * columns + column;
        if (index >= apps.size()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(apps.get(index));
    }

    /** Etiqueta legible para depurar. */
    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "Grid{apps=%d, columns=%d}", size(), columns);
    }
}
