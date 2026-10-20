package com.suop.tvlauncher.nav;

/**
 * Donde esta el foco: barra superior, rejilla o dock.
 */
public final class FocusState {

    public final Zone zone;
    /** Fila de la rejilla. */
    public final int row;
    /** Columna de la rejilla. */
    public final int column;
    /** Indice del boton en la barra superior. */
    public final int topIndex;
    /** Indice del icono en el dock. */
    public final int dockIndex;

    public FocusState(Zone zone, int row, int column, int topIndex, int dockIndex) {
        this.zone = zone;
        this.row = row;
        this.column = column;
        this.topIndex = topIndex;
        this.dockIndex = dockIndex;
    }

    @Override
    public String toString() {
        return "FocusState{" + zone + " r" + row + " c" + column
                + " top" + topIndex + " dock" + dockIndex + "}";
    }
}
