package com.suop.tvlauncher.nav;

/**
 * Donde esta el foco: barra superior o rejilla.
 */
public final class FocusState {

    public final Zone zone;
    /** Fila de la rejilla. */
    public final int row;
    /** Columna de la rejilla. */
    public final int column;
    /** Indice del boton en la barra superior. */
    public final int topIndex;

    public FocusState(Zone zone, int row, int column, int topIndex) {
        this.zone = zone;
        this.row = row;
        this.column = column;
        this.topIndex = topIndex;
    }

    @Override
    public String toString() {
        return "FocusState{" + zone + " r" + row + " c" + column + " top" + topIndex + "}";
    }
}
