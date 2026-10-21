package es.hugoperezvigo.tvlauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import es.hugoperezvigo.tvlauncher.grid.Grid;
import es.hugoperezvigo.tvlauncher.grid.GridItem;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Rejilla de apps: 4 columnas a todo el ancho, sin dock.
 */
public class GridTest {

    private static GridItem item(String pkg, String label) {
        return new GridItem(pkg, label);
    }

    @Test
    public void reparteEnColumnasDeCuatro() {
        Grid grid = Grid.of(Arrays.asList(
                item("c", "C"), item("d", "D"), item("e", "E"), item("f", "F"),
                item("g", "G"), item("h", "H")), 4);

        assertEquals(2, grid.rows());
        assertEquals(4, grid.columns());
        assertEquals(6, grid.size());
    }

    @Test
    public void laUltimaFilaSeRellenaConHuecos() {
        Grid grid = Grid.of(Arrays.asList(
                item("a", "A"), item("b", "B"), item("c", "C"), item("d", "D"),
                item("e", "E")), 4);

        assertEquals(2, grid.rows());
        assertEquals(5, grid.size());
        // La celda (1,3) no existe: se calcula con base vacia.
        assertTrue(grid.at(1, 3).isEmpty());
        assertEquals("E", grid.at(1, 0).get().label);
    }

    @Test
    public void todasLasAppsQuedanEnLaRejilla() {
        Grid grid = Grid.of(Arrays.asList(
                item("youtube", "YouTube"), item("prime", "Prime Video"),
                item("plex", "Plex"), item("twitch", "Twitch"),
                item("spotify", "Spotify")), 4);

        // No hay dock: las 5 apps estan en la rejilla.
        assertEquals(5, grid.size());
    }

    @Test
    public void lasColumnasDelDockYaNoExisten() {
        Grid grid = Grid.of(Arrays.asList(
                item("a", "A"), item("b", "B")), 4);

        assertEquals(1, grid.rows());
        assertEquals(2, grid.size());
    }

    @Test
    public void unaRejillaVaciaNoTieneFilas() {
        Grid grid = Grid.of(Arrays.<GridItem>asList(), 4);

        assertEquals(0, grid.rows());
        assertEquals(0, grid.size());
        assertTrue(grid.at(0, 0).isEmpty());
    }

    @Test
    public void lasCeldasFueraDeRangoEstanVacias() {
        Grid grid = Grid.of(Arrays.asList(item("a", "A")), 4);

        assertTrue(grid.at(-1, 0).isEmpty());
        assertTrue(grid.at(0, -1).isEmpty());
        assertTrue(grid.at(0, 4).isEmpty());
        assertTrue(grid.at(5, 0).isEmpty());
    }

    @Test
    public void elOrdenSeMantieneTalComoSePasa() {
        List<GridItem> items = Arrays.asList(
                item("b", "B"), item("a", "A"), item("c", "C"));

        Grid grid = Grid.of(items, 4);

        assertEquals("B", grid.at(0, 0).get().label);
        assertEquals("A", grid.at(0, 1).get().label);
        assertEquals("C", grid.at(0, 2).get().label);
    }

    @Test
    public void laUltimaFilaPuedeEstarIncompleta() {
        Grid grid = Grid.of(Arrays.asList(
                item("a", "A"), item("b", "B"), item("c", "C"), item("d", "D"),
                item("e", "E")), 4);

        assertEquals(4, grid.columnsIn(0));
        assertEquals(1, grid.columnsIn(1));
        assertEquals(1, grid.lastRow());
        assertEquals(0, grid.columnsIn(9));
    }

    @Test
    public void laPosicionDeUnaCeldaQueNoExisteEsMenosUno() {
        Grid grid = Grid.of(Arrays.asList(
                item("a", "A"), item("b", "B"), item("c", "C"), item("d", "D"),
                item("e", "E")), 4);

        assertEquals(4, grid.positionOf(1, 0));
        assertEquals(-1, grid.positionOf(1, 1));
        assertEquals(-1, grid.positionOf(0, 4));
    }
}
