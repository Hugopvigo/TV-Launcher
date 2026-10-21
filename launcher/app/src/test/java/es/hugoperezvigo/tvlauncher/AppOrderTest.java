package es.hugoperezvigo.tvlauncher;

import static org.junit.Assert.assertEquals;

import es.hugoperezvigo.tvlauncher.grid.GridItem;
import es.hugoperezvigo.tvlauncher.ui.AppOrder;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Orden de las apps en la rejilla: las usadas primero, las menos al final.
 */
public class AppOrderTest {

    private static GridItem item(String pkg, String label) {
        return new GridItem(pkg, label);
    }

    @Test
    public void lasAppsPrioritariasVanPrimeroYEnEseOrden() {
        List<GridItem> apps = Arrays.asList(
                item("com.google.android.youtube.tvmusic", "YouTube Music"),
                item("com.amazon.amazonvideo.livingroom", "Prime Video"),
                item("com.google.android.youtube.tv", "YouTube"),
                item("com.wbd.stream", "HBO Max"),
                item("com.skyshowtime.skyshowtime.google", "SkyShowtime"),
                item("com.spotify.tv.android", "Spotify"),
                item("com.plexapp.android", "Plex"));

        List<String> order = Arrays.asList(
                "com.google.android.youtube.tv",
                "com.amazon.amazonvideo.livingroom",
                "com.skyshowtime.skyshowtime.google",
                "com.wbd.stream",
                "com.spotify.tv.android");

        List<GridItem> sorted = AppOrder.sort(apps, order, java.util.Collections.<String>emptyList());

        assertEquals("com.google.android.youtube.tv", sorted.get(0).packageName);
        assertEquals("com.amazon.amazonvideo.livingroom", sorted.get(1).packageName);
        assertEquals("com.skyshowtime.skyshowtime.google", sorted.get(2).packageName);
        assertEquals("com.wbd.stream", sorted.get(3).packageName);
        assertEquals("com.spotify.tv.android", sorted.get(4).packageName);
        // Las demas, alfabeticamente detras.
        assertEquals("com.plexapp.android", sorted.get(5).packageName);
        assertEquals("com.google.android.youtube.tvmusic", sorted.get(6).packageName);
    }

    @Test
    public void lasAppsMarcadasComoUltimasVanAlFinal() {
        List<GridItem> apps = Arrays.asList(
                item("com.google.android.videos", "Play Peliculas"),
                item("com.google.android.youtube.tv", "YouTube"),
                item("com.plexapp.android", "Plex"));

        List<String> first = Arrays.asList("com.google.android.youtube.tv");
        List<String> last = Arrays.asList("com.google.android.videos");

        List<GridItem> sorted = AppOrder.sort(apps, first, last);

        assertEquals("com.google.android.youtube.tv", sorted.get(0).packageName);
        assertEquals("com.plexapp.android", sorted.get(1).packageName);
        assertEquals("com.google.android.videos", sorted.get(2).packageName);
    }

    @Test
    public void unaAppPrioritariaQueNoEstaInstaladaSeIgnora() {
        List<GridItem> apps = Arrays.asList(
                item("com.plexapp.android", "Plex"),
                item("com.google.android.youtube.tv", "YouTube"));

        List<String> first = Arrays.asList("noexiste", "com.google.android.youtube.tv");

        List<GridItem> sorted = AppOrder.sort(apps, first, java.util.Collections.<String>emptyList());

        assertEquals(2, sorted.size());
        assertEquals("com.google.android.youtube.tv", sorted.get(0).packageName);
    }

    @Test
    public void unaAppNoPrioritariaQuedaAlfabeticamente() {
        List<GridItem> apps = Arrays.asList(
                item("z", "Zeta"),
                item("a", "Alfa"),
                item("m", "Mike"));

        List<GridItem> sorted = AppOrder.sort(apps,
                java.util.Collections.<String>emptyList(),
                java.util.Collections.<String>emptyList());

        assertEquals("Alfa", sorted.get(0).label);
        assertEquals("Mike", sorted.get(1).label);
        assertEquals("Zeta", sorted.get(2).label);
    }
}
