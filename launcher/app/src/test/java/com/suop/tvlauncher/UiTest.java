package es.hugoperezvigo.tvlauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import es.hugoperezvigo.tvlauncher.ui.Clock;
import es.hugoperezvigo.tvlauncher.ui.Greeting;
import es.hugoperezvigo.tvlauncher.ui.Tint;
import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;

/**
 * Presentacion: saludo, reloj y color de ambiente.
 */
public class UiTest {

    @Test
    public void elSaludoDependeDeLaHora() {
        assertEquals(Greeting.MORNING, Greeting.forHour(8));
        assertEquals(Greeting.AFTERNOON, Greeting.forHour(15));
        assertEquals(Greeting.EVENING, Greeting.forHour(21));
        assertEquals(Greeting.NIGHT, Greeting.forHour(3));
    }

    @Test
    public void lasFronterasDelSaludoSonExactas() {
        assertEquals(Greeting.NIGHT, Greeting.forHour(5));
        assertEquals(Greeting.MORNING, Greeting.forHour(6));
        assertEquals(Greeting.MORNING, Greeting.forHour(11));
        assertEquals(Greeting.AFTERNOON, Greeting.forHour(12));
        assertEquals(Greeting.AFTERNOON, Greeting.forHour(19));
        assertEquals(Greeting.EVENING, Greeting.forHour(20));
        assertEquals(Greeting.EVENING, Greeting.forHour(23));
        assertEquals(Greeting.NIGHT, Greeting.forHour(0));
    }

    @Test
    public void laHoraSeFormateaConDosDigitos() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 20, 9, 5);
        assertEquals("09:05", Clock.format(c, Locale.getDefault()));
    }

    @Test
    public void lasTreceSeMuestranComo23() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 20, 13, 7);
        assertEquals("13:07", Clock.format(c, Locale.getDefault()));
    }

    @Test
    public void laFechaLargaEnEspanol() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 20, 9, 5);
        String s = Clock.formatDate(c, new Locale("es", "ES"));
        assertTrue("Deberia contener el dia 20: " + s, s.contains("20"));
        assertTrue("Deberia contener octubre: " + s, s.toLowerCase(Locale.getDefault()).contains("octubre"));
        assertTrue("Deberia contener el dia de la semana: " + s, s.contains("martes"));
    }

    @Test
    public void elTinteSeAjustaParaQueResalteSobreNegro() {
        int base = 0xFF3B82F6; // azul
        int t = Tint.brighter(base, 1.6f);
        // Los tres canales suben, y no se desborda ninguno.
        assertTrue(Tint.red(t) > Tint.red(base));
        assertTrue(Tint.green(t) > Tint.green(base));
        assertTrue(Tint.blue(t) > Tint.blue(base));
        assertTrue(Tint.red(t) <= 255);
        assertTrue(Tint.green(t) <= 255);
        assertTrue(Tint.blue(t) <= 255);
    }

    @Test
    public void elTinteNoSeDesvanece() {
        int base = 0xFF808080;
        int t = Tint.brighter(base, 1.5f);
        assertTrue(Tint.red(t) > 0);
    }
}
