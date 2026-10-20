package es.hugoperezvigo.tvlauncher.nav;

/**
 * Navegacion determinista con D-pad.
 *
 * Dos zonas de foco:
 *   TOP_BAR  botones de la barra superior (entradas, wifi, ajustes)
 *   GRID     rejilla de apps, en filas de {@code columns}
 *
 * Reglas:
 *   - Up/Down mueven exactamente una fila dentro de la rejilla.
 *   - Up desde la primera fila sube a la barra superior.
 *   - Down desde la ultima fila se queda en la ultima fila.
 *   - En los extremos de la barra superior el foco no se cae a las teselas.
 *   - Se recuerda la ultima tesela enfocada de la rejilla.
 */
public final class Navigator {

    private final int rows;
    private final int columns;
    private final int topCount;

    private Zone zone = Zone.GRID;
    private int row;
    private int column;
    private int topIndex;

    /** Ultima tesela de la rejilla enfocada, para volver a ella. */
    private int lastRow;
    private int lastColumn;

    public Navigator(int rows, int columns, int topCount) {
        this.rows = Math.max(0, rows);
        this.columns = Math.max(1, columns);
        this.topCount = Math.max(0, topCount);
    }

    /** Estado actual del foco. */
    public FocusState focus() {
        return new FocusState(zone, row, column, topIndex);
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
                }
                break;
            case GRID:
                if (row < rows - 1) {
                    setGrid(row + 1, column);
                }
                break;
            default:
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

    /** Sincroniza con una tesela de la rejilla enfocada. */
    public Navigator syncGrid(int newRow, int newColumn) {
        setGrid(newRow, newColumn);
        return this;
    }
}
