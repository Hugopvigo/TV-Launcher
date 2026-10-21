package es.hugoperezvigo.tvlauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import es.hugoperezvigo.tvlauncher.inputs.TvInputs;
import org.junit.Test;

/**
 * Entradas de TV: el sistema registra como tuner los servicios de
 * previsualizacion de las apps de streaming, y no son entradas de verdad.
 */
public class TvInputsTest {

    @Test
    public void losServiciosDePrevisualizacionNoSonEntradas() {
        assertTrue(TvInputs.isPreview(
                "com.google.android.videos/.tv.usecase.tvinput.playback.TvInputService"));
        assertTrue(TvInputs.isPreview(
                "com.skyshowtime.skyshowtime.google/com.peacock.peacocktv.recs.PreviewVideoInputService"));
    }

    @Test
    public void unaEntradaDeVerdadNoEsPrevisualizacion() {
        assertFalse(TvInputs.isPreview("HDMI1"));
        assertFalse(TvInputs.isPreview("com.mitv.tvtuner/.TunerService"));
        assertFalse(TvInputs.isPreview(null));
    }

    @Test
    public void hdmiYAvSePuedenCambiar() {
        assertTrue(TvInputs.isSwitchable(TvInputs.TYPE_HDMI));
        assertTrue(TvInputs.isSwitchable(TvInputs.TYPE_PHYSICAL_HDMI));
        assertTrue(TvInputs.isSwitchable(TvInputs.TYPE_COMPOSITE));
        assertTrue(TvInputs.isSwitchable(TvInputs.TYPE_PHYSICAL_COMPOSITE));
    }

    @Test
    public void unTunerVirtualNoSePuedeCambiar() {
        assertFalse(TvInputs.isSwitchable(TvInputs.TYPE_TUNER));
        assertFalse(TvInputs.isSwitchable(TvInputs.TYPE_VIRTUAL));
    }

    @Test
    public void elNumeroDeHdmiSeDeduceDelIdentificador() {
        assertEquals(2, TvInputs.hdmiNumber("HDMI2"));
        assertEquals(3, TvInputs.hdmiNumber("com.mitv.hdmi/.HDMI3InputService"));
        // Sin numero: la primera entrada.
        assertEquals(1, TvInputs.hdmiNumber("HDMI"));
        assertEquals(1, TvInputs.hdmiNumber(null));
    }
}
