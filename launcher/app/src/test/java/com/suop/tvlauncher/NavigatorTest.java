package com.suop.tvlauncher;

import static org.junit.Assert.assertEquals;

import com.suop.tvlauncher.nav.FocusState;
import com.suop.tvlauncher.nav.Zone;
import com.suop.tvlauncher.nav.Navigator;
import org.junit.Test;

/**
 * Navegacion determinista con D-pad.
 *
 * Reglas del prompt:
 *  - Up/Down mueven exactamente una fila.
 *  - Up desde el dock sube a la barra superior.
 *  - Los extremos de la barra superior no se caen a las teselas.
 *  - Se recuerda la ultima tesela enfocada.
 */
public class NavigatorTest {

    /** 3 filas de 4 columnas, 4 huecos de dock, barra superior con 3 botones. */
    private Navigator navigator() {
        return new Navigator(3, 4, 4, 3);
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
    public void abajoEnLaUltimaFilaBajaAlDock() {
        Navigator n = navigator();
        FocusState s = n.down().down().down().focus();
        assertEquals(Zone.DOCK, s.zone);
        assertEquals(0, s.dockIndex);
    }

    @Test
    public void arribaDesdeElDockSubeALaBarraSuperior() {
        Navigator n = navigator();
        FocusState s = n.down().down().down().up().focus();
        assertEquals(Zone.TOP_BAR, s.zone);
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
    public void elExtremoIzquierdoDeLaBarraNoSeCaeaLasTeselas() {
        Navigator n = navigator();
        n.up();                 // barra superior, indice 0
        FocusState s = n.left().focus(); // en el extremo izquierdo
        assertEquals(Zone.TOP_BAR, s.zone);
        assertEquals(0, s.topIndex);
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
    public void elExtremoDerechoDeLaBarraNoSeCaeaLasTeselas() {
        Navigator n = navigator();
        n.up();
        n.right();
        n.right();               // ultimo boton de la barra
        FocusState s = n.right().focus(); // en el extremo derecho
        assertEquals(Zone.TOP_BAR, s.zone);
        assertEquals(2, s.topIndex);
    }

    @Test
    public void izquierdaYderechaMuevenDentroDeLaFila() {
        Navigator n = navigator();
        n.right();
        FocusState s = n.right().focus();
        assertEquals(2, s.column);
    }

    @Test
    public void enElDockIzquierdaYderechaMuevenDeIcono() {
        Navigator n = navigator();
        n.down().down().down();  // dock
        n.right();
        FocusState s = n.right().focus();
        assertEquals(Zone.DOCK, s.zone);
        assertEquals(2, s.dockIndex);
    }

    @Test
    public void enElDockLaDerechaNoSeSaleDelUltimoIcono() {
        Navigator n = navigator();
        n.down().down().down();
        n.right();
        n.right();
        n.right();
        FocusState s = n.right().focus();
        assertEquals(3, s.dockIndex);
    }

    @Test
    public void laFilaSeAjustaAlNumeroDeColumnasReales() {
        // 2 appas: una sola fila con 2 columnas.
        Navigator n = new Navigator(1, 2, 4, 3);
        FocusState s = n.right().focus();
        assertEquals(1, s.column);
        // No hay segunda fila: abajo lleva al dock.
        FocusState s2 = n.down().focus();
        assertEquals(Zone.DOCK, s2.zone);
    }

    @Test
    public void abajoEnLaUltimaFilaNoSeSaleDelDock() {
        Navigator n = navigator();
        n.down().down().down();
        FocusState s = n.down().focus();
        assertEquals(Zone.DOCK, s.zone);
    }
}
