package com.suop.tvlauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.suop.tvlauncher.grid.Grid;
import com.suop.tvlauncher.grid.GridItem;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Rejilla de apps: 4 columnas, orden alfabetico estable y docks configurable.
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
    public void elDockSeSacaDeLaRejilla() {
        Grid grid = Grid.of(Arrays.asList(
                item("youtube", "YouTube"), item("prime", "Prime Video"),
                item("plex", "Plex"), item("twitch", "Twitch"),
                item("spotify", "Spotify")),
                4, Arrays.asList("youtube", "prime", "plex", "twitch"));

        assertEquals(1, grid.size());
        assertEquals("Spotify", grid.at(0, 0).get().label);
    }

    @Test
    public void elDockConservaElOrdenPedidoAunQueNoSeaAlfabetico() {
        Grid grid = Grid.of(Arrays.asList(
                item("youtube", "YouTube"), item("prime", "Prime Video"),
                item("plex", "Plex"), item("twitch", "Twitch"),
                item("spotify", "Spotify")),
                4, Arrays.asList("twitch", "youtube", "prime", "plex"));

        List<String> dock = grid.dockPackages();
        assertEquals(Arrays.asList("twitch", "youtube", "prime", "plex"), dock);
        assertEquals(1, grid.size());
    }

    @Test
    public void unDockIncompletoNoInventaAppsQueElUsuarioNoPidio() {
        Grid grid = Grid.of(Arrays.asList(
                item("youtube", "YouTube"), item("prime", "Prime Video"),
                item("plex", "Plex"), item("spotify", "Spotify")),
                4, Arrays.asList("youtube", "prime"));

        // El dock tiene solo las pedidas; las demas se quedan en la rejilla.
        assertEquals(2, grid.dockPackages().size());
        assertEquals("youtube", grid.dockPackages().get(0));
        assertEquals("prime", grid.dockPackages().get(1));
        assertEquals(2, grid.size());
    }

    @Test
    public void unPaqueteDelDockQueNoExisteSeIgnora() {
        Grid grid = Grid.of(Arrays.asList(
                item("youtube", "YouTube"), item("spotify", "Spotify")),
                4, Arrays.asList("youtube", "noexiste"));

        assertEquals(1, grid.dockPackages().size());
        assertEquals("youtube", grid.dockPackages().get(0));
    }

    @Test
    public void lasColumnasDelDockSiempreSonCuatro() {
        Grid grid = Grid.of(Arrays.asList(
                item("youtube", "YouTube"), item("prime", "Prime Video"),
                item("plex", "Plex"), item("twitch", "Twitch"),
                item("spotify", "Spotify"), item("hbo", "HBO Max")),
                4, Arrays.asList("youtube", "prime", "plex", "twitch"));

        assertEquals(4, grid.dockPackages().size());
        // Spotify y HBO Max se quedan en la rejilla.
        assertEquals(2, grid.size());
    }
}
