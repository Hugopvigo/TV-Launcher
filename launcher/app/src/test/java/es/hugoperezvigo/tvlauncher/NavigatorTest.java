package es.hugoperezvigo.tvlauncher;

import static org.junit.Assert.assertEquals;

import es.hugoperezvigo.tvlauncher.grid.Grid;
import es.hugoperezvigo.tvlauncher.grid.GridItem;
import es.hugoperezvigo.tvlauncher.nav.FocusState;
import es.hugoperezvigo.tvlauncher.nav.Navigator;
import es.hugoperezvigo.tvlauncher.nav.Zone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Navegacion determinista con D-pad.
 *
 * Reglas del prompt:
 *  - Up/Down mueven exactamente una fila.
 *  - Up desde la primera fila sube a la barra superior.
 *  - Los extremos de la barra superior no se caen a las teselas.
 *  - Se recuerda la ultima tesela enfocada.
 *  - La ultima fila puede estar incompleta: el foco no cae en un hueco.
 */
public class NavigatorTest {

    /** Rejilla de 4 columnas con el numero de apps indicado. */
    private Navigator navigator(int appCount) {
        List<GridItem> items = new ArrayList<>();
        for (int i = 0; i < appCount; i++) {
            items.add(new GridItem("app" + i, "App " + i));
        }
        return new Navigator(Grid.of(items, 4), 3);
    }

    /** 3 filas de 4 columnas, barra superior con 3 botones. */
    private Navigator navigator() {
        return navigator(12);
    }

    @Test
    public void empiezaEnLaPrimeraTesela() {
        FocusState s = navigator().focus();
        assertEquals(Zone.GRID, s.zone);
        assertEquals(0, s.row);
        assertEquals(0, s.column);
    }

    @Test
    public void abajoAvanzaExactamenteUnaFila() {
        FocusState s = navigator().down().focus();
        assertEquals(Zone.GRID, s.zone);
        assertEquals(1, s.row);
        assertEquals(0, s.column);
    }

    @Test
    public void arribaAvanzaExactamenteUnaFila() {
        FocusState s = navigator().down().down().up().focus();
        assertEquals(1, s.row);
    }

    @Test
    public void arribaDesdeLaPrimeraFilaSubeALaBarraSuperior() {
        FocusState s = navigator().up().focus();
        assertEquals(Zone.TOP_BAR, s.zone);
        assertEquals(0, s.topIndex);
    }

    @Test
    public void abajoDesdeLaBarraSuperiorVuelveALaUltimaTeselaRecordada() {
        Navigator n = navigator();
        // Vamos a la fila 0, columna 2.
        n.right();
        n.right();
        n.up();                  // a la barra superior
        n.right();               // nos movemos por la barra: la tesela recordada no cambia
        FocusState s = n.down().focus(); // y de vuelta a la rejilla
        assertEquals(Zone.GRID, s.zone);
        assertEquals(0, s.row);
        assertEquals(2, s.column);
    }

    @Test
    public void laFilaRecordadaSobreviveALaVisitaALaBarra() {
        Navigator n = navigator();
        n.down();                // fila 1
        n.right();               // columna 1
        n.up();                  // fila 0
        n.up();                  // barra superior
        FocusState s = n.down().focus(); // de vuelta
        assertEquals(0, s.row);
        assertEquals(1, s.column);
    }

    @Test
    public void elExtremoIzquierdoDeLaBarraNoSeCaeLasTeselas() {
        Navigator n = navigator();
        n.up();                 // barra superior, indice 0
        FocusState s = n.left().focus(); // en el extremo izquierdo
        assertEquals(Zone.TOP_BAR, s.zone);
        assertEquals(0, s.topIndex);
    }

    @Test
    public void elExtremoDerechoDeLaBarraNoSeCaeLasTeselas() {
        Navigator n = navigator();
        n.up();
        n.right();
        n.right();               // ultimo boton de la barra
        FocusState s = n.right().focus(); // en el extremo derecho
        assertEquals(Zone.TOP_BAR, s.zone);
        assertEquals(2, s.topIndex);
    }

    @Test
    public void enLaBarraSuperiorDerechaAvanzaDeBoton() {
        Navigator n = navigator();
        n.up();                       // barra superior, indice 0
        FocusState s = n.right().focus();
        assertEquals(Zone.TOP_BAR, s.zone);
        assertEquals(1, s.topIndex);
    }

    @Test
    public void enLaBarraSuperiorIzquierdaRetrocedeDeBoton() {
        Navigator n = navigator();
        n.up();
        n.right();
        FocusState s = n.left().focus();
        assertEquals(0, s.topIndex);
    }

    @Test
    public void izquierdaYderechaMuevenDentroDeLaFila() {
        Navigator n = navigator();
        n.right();
        FocusState s = n.right().focus();
        assertEquals(2, s.column);
    }

    @Test
    public void enLaUltimaFilaAbajoNoSeSaleDeLaRejilla() {
        Navigator n = navigator().down().down();
        FocusState s = n.down().focus();
        assertEquals(Zone.GRID, s.zone);
        assertEquals(2, s.row);
    }

    @Test
    public void laFilaSeAjustaAlNumeroDeColumnasReales() {
        // 2 apps: una sola fila con 2 columnas.
        Navigator n = navigator(2);
        FocusState s = n.right().focus();
        assertEquals(1, s.column);
        // No hay segunda fila: abajo se queda.
        FocusState s2 = n.down().focus();
        assertEquals(Zone.GRID, s2.zone);
        assertEquals(0, s2.row);
    }

    // ------------------------------------------- ultima fila incompleta

    @Test
    public void laUltimaFilaIncompletaSeAjusta() {
        // 13 apps: 3 filas de 4 y una ultima con una sola.
        Navigator n = navigator(13);
        n.down().down().down();
        FocusState s = n.focus();
        assertEquals(3, s.row);
        assertEquals(0, s.column);
    }

    @Test
    public void elFocoNoCaeEnUnHuecoDeLaUltimaFila() {
        // 13 apps: 3 filas de 4 y una ultima con una sola.
        Navigator n = navigator(13);
        n.down().down().down();
        n.right();                       // no hay segunda tesela en esa fila
        FocusState s = n.focus();
        assertEquals(3, s.row);
        assertEquals(0, s.column);
    }

    @Test
    public void izquierdaDesdeLaUltimaFilaSaltaAlFinalDeLaDeArriba() {
        // 13 apps: 3 filas de 4 y una ultima con una sola.
        Navigator n = navigator(13);
        n.down().down().down();
        FocusState s = n.left().focus();
        assertEquals(2, s.row);
        assertEquals(3, s.column);
    }

    @Test
    public void abajoAjustaLaColumnaALaUltimaFila() {
        // 13 apps: al bajar a la ultima fila la columna 3 no existe.
        Navigator n = navigator(13);
        n.right().right().right();       // columna 3 de la fila 0
        FocusState s = n.down().down().down().focus();
        assertEquals(3, s.row);
        assertEquals(0, s.column);
    }
}
