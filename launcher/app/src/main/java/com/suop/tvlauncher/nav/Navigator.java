package com.suop.tvlauncher.nav;



/**
 * Navegacion determinista con D-pad.
 *
 * Zonas de foco, de arriba abajo:
 *   TOP_BAR  botones de la barra superior (entradas, wifi, ajustes)
 *   GRID     rejilla de apps, en filas de {@code columns}
 *   DOCK     fila de iconos fija abajo
 *
 * Reglas:
 *   - Up/Down mueven exactamente una fila dentro de la rejilla.
 *   - Up desde la primera fila sube a la barra superior.
 *   - Down desde la ultima fila baja al dock.
 *   - Up desde el dock sube a la barra superior.
 *   - En los extremos de la barra superior el foco no se cae a las teselas.
 *   - Se recuerda la ultima tesela enfocada de la rejilla.
 */
public final class Navigator {

    // Enum de zona (anidado en FocusState).


    private final int rows;
    private final int columns;
    private final int dockCount;
    private final int topCount;

    private Zone zone = Zone.GRID;
    private int row;
    private int column;
    private int topIndex;
    private int dockIndex;

    /** Ultima tesela de la rejilla enfocada, para volver a ella. */
    private int lastRow;
    private int lastColumn;

    public Navigator(int rows, int columns, int dockCount, int topCount) {
        this.rows = Math.max(0, rows);
        this.columns = Math.max(1, columns);
        this.dockCount = Math.max(0, dockCount);
        this.topCount = Math.max(0, topCount);
    }

    /** Estado actual del foco. */
    public FocusState focus() {
        return new FocusState(zone, row, column, topIndex, dockIndex);
    }

    public Navigator up() {
        switch (zone) {
            case GRID:
                if (row > 0) {
                    setGrid(row - 1, column);
                } else if (topCount > 0) {
                    zone = Zone.TOP_BAR;
                }
                break;
            case DOCK:
                // Desde el dock se sube a la barra superior.
                if (topCount > 0) {
                    zone = Zone.TOP_BAR;
                } else if (rows > 0) {
                    setGrid(lastRow, lastColumn);
                }
                break;
            case TOP_BAR:
            default:
                // En la barra superior, arriba no hace nada.
                break;
        }
        return this;
    }

    public Navigator down() {
        switch (zone) {
            case TOP_BAR:
                // Vuelve a la ultima tesela enfocada.
                if (rows > 0) {
                    zone = Zone.GRID;
                    setGrid(lastRow, lastColumn);
                } else if (dockCount > 0) {
                    zone = Zone.DOCK;
                }
                break;
            case GRID:
                if (row < rows - 1) {
                    setGrid(row + 1, column);
                } else if (dockCount > 0) {
                    zone = Zone.DOCK;
                }
                break;
            case DOCK:
            default:
                // Abajo en el dock no hace nada.
                break;
        }
        return this;
    }

    public Navigator left() {
        switch (zone) {
            case GRID:
                if (column > 0) {
                    setGrid(row, column - 1);
                }
                break;
            case TOP_BAR:
                // En el extremo izquierdo el foco no se cae a las teselas.
                if (topIndex > 0) {
                    topIndex--;
                }
                break;
            case DOCK:
                if (dockIndex > 0) {
                    dockIndex--;
                }
                break;
            default:
                break;
        }
        return this;
    }

    public Navigator right() {
        switch (zone) {
            case GRID:
                if (column < columns - 1) {
                    setGrid(row, column + 1);
                }
                break;
            case TOP_BAR:
                // En el extremo derecho el foco no se cae a las teselas.
                if (topIndex < topCount - 1) {
                    topIndex++;
                }
                break;
            case DOCK:
                if (dockIndex < dockCount - 1) {
                    dockIndex++;
                }
                break;
            default:
                break;
        }
        return this;
    }

    private void setGrid(int newRow, int newColumn) {
        zone = Zone.GRID;
        row = Math.max(0, Math.min(newRow, Math.max(0, rows - 1)));
        column = Math.max(0, Math.min(newColumn, columns - 1));
        lastRow = row;
        lastColumn = column;
    }

    /**
     * Sincroniza con un foco real llegado desde el sistema (por ejemplo, cuando
     * el usuario navega con el raton o cuando Android mueve el foco solo).
     */
    public Navigator syncTopBar(int index) {
        zone = Zone.TOP_BAR;
        topIndex = Math.max(0, Math.min(index, Math.max(0, topCount - 1)));
        return this;
    }

    /** Sincroniza con un icono del dock enfocado. */
    public Navigator syncDock(int index) {
        zone = Zone.DOCK;
        dockIndex = Math.max(0, Math.min(index, Math.max(0, dockCount - 1)));
        return this;
    }

    /** Sincroniza con una tesela de la rejilla enfocada. */
    public Navigator syncGrid(int newRow, int newColumn) {
        setGrid(newRow, newColumn);
        return this;
    }
}
