package es.hugoperezvigo.tvlauncher.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Rejilla de apps en 4 columnas, a todo el ancho.
 *
 * No hay dock: todas las apps se muestran en la rejilla, en el orden en que
 * se pasan.
 *
 * La ultima fila puede quedar incompleta (13 apps en 4 columnas son 3 filas
 * completas y una con una sola tesela). La rejilla lo sabe y lo tiene en
 * cuenta: {@link #columnsIn(int)} y {@link #positionOf(int, int)}.
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
     * Cuantas teselas hay en una fila. La ultima puede tener menos que
     * {@link #columns()}.
     */
    public int columnsIn(int row) {
        if (row < 0 || row >= rows()) {
            return 0;
        }
        return Math.min(columns, size() - row * columns);
    }

    /** Ultima fila con teselas. */
    public int lastRow() {
        return Math.max(0, rows() - 1);
    }

    /**
     * Posicion en la lista de la tesela de una celda, o -1 si esa celda no
     * existe (la ultima fila puede estar incompleta).
     */
    public int positionOf(int row, int column) {
        if (row < 0 || row >= rows() || column < 0 || column >= columnsIn(row)) {
            return -1;
        }
        return row * columns + column;
    }

    /**
     * App en una posicion de la rejilla, o vacio si la celda no existe
     * (la ultima fila puede quedar incompleta).
     */
    public Optional<GridItem> at(int row, int column) {
        int position = positionOf(row, column);
        if (position < 0) {
            return Optional.empty();
        }
        return Optional.of(apps.get(position));
    }

    /** Etiqueta legible para depurar. */
    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "Grid{apps=%d, columns=%d}", size(), columns);
    }
}
