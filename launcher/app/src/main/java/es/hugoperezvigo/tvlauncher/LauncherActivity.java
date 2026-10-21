package es.hugoperezvigo.tvlauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import es.hugoperezvigo.tvlauncher.grid.Grid;
import es.hugoperezvigo.tvlauncher.inputs.TvInputs;
import es.hugoperezvigo.tvlauncher.nav.FocusState;
import es.hugoperezvigo.tvlauncher.nav.Navigator;
import es.hugoperezvigo.tvlauncher.nav.Zone;
import es.hugoperezvigo.tvlauncher.ui.AppAdapter;
import es.hugoperezvigo.tvlauncher.ui.Clock;
import es.hugoperezvigo.tvlauncher.ui.GlassHighlight;
import es.hugoperezvigo.tvlauncher.ui.Greeting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Launcher estilo Apple TV para Android TV.
 *
 * Distribucion:
 *   - Barra superior: saludo, reloj grande y fecha a la izquierda;
 *     menus de entradas, Wi-Fi y ajustes a la derecha.
 *   - Rejilla de 4 columnas con todas las apps, a todo el ancho.
 */
public class LauncherActivity extends AppCompatActivity implements AppAdapter.Listener {

    private static final long CLOCK_INTERVAL_MS = 20_000L;

    /** Botones de la barra superior: entradas, Wi-Fi y ajustes. */
    private static final int TOP_BAR_COUNT = 3;

    /** Accion del sistema que abre el selector de entradas de TV. */
    private static final String ACTION_SHOW_TV_INPUTS = "android.media.tv.action.SHOW_TV_INPUTS";

    private RecyclerView grid;
    private AppAdapter adapter;
    private GridLayoutManager layoutManager;
    private GlassHighlight highlight;
    private Navigator navigator;

    private TextView greeting;
    private TextView clock;
    private TextView date;
    private TextView btnInputs;
    private TextView btnWifi;
    private TextView btnSettings;
    private LinearLayout inputsPanel;

    /** Vista que tenia el foco antes de abrir el desplegable de entradas. */
    private View panelReturnFocus;

    private Grid gridModel;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, CLOCK_INTERVAL_MS);
        }
    };

    /** Una entrada del desplegable. */
    private static final class InputOption {
        final String label;
        /** App a abrir, o null si lo que se abre es el selector del sistema. */
        final ComponentName component;
        final boolean systemPicker;

        InputOption(String label, ComponentName component, boolean systemPicker) {
            this.label = label;
            this.component = component;
            this.systemPicker = systemPicker;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        greeting = findViewById(R.id.greeting);
        clock = findViewById(R.id.clock);
        date = findViewById(R.id.date);
        btnInputs = findViewById(R.id.btn_inputs);
        btnWifi = findViewById(R.id.btn_wifi);
        btnSettings = findViewById(R.id.btn_settings);
        inputsPanel = findViewById(R.id.inputs_panel);

        grid = findViewById(R.id.grid);

        highlight = new GlassHighlight(this);

        adapter = new AppAdapter(this);
        layoutManager = new GridLayoutManager(this, Grid.COLUMNS);
        grid.setLayoutManager(layoutManager);
        grid.setAdapter(adapter);
        // Las teselas cogen el foco, no la rejilla entera.
        grid.setFocusable(false);
        grid.setFocusableInTouchMode(false);
        grid.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        // Al desplazar, el cristal sigue a la tesela enfocada.
        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                View focused = focusedTile();
                if (focused != null) {
                    moveHighlightTo(focused);
                }
            }
        });

        // El cristal va detras de los iconos: se anade como primera hija
        // del contenedor de la rejilla.
        ViewGroup container = findViewById(R.id.grid_container);
        container.addView(highlight, 0, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        highlight.setVisibility(View.GONE);

        // Menus de la barra superior.
        btnInputs.setOnClickListener(v -> toggleInputs());
        btnWifi.setOnClickListener(v -> openWifiSettings());
        btnSettings.setOnClickListener(v -> openSettings());
        btnInputs.setOnFocusChangeListener((v, f) -> onTopBarFocus(f, 0));
        btnWifi.setOnFocusChangeListener((v, f) -> onTopBarFocus(f, 1));
        btnSettings.setOnFocusChangeListener((v, f) -> onTopBarFocus(f, 2));

        updateClock();
        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(clockTick);
        // Al volver de una app se recupera la ultima tesela enfocada, no la
        // primera de la rejilla.
        restoreFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockTick);
    }

    // ---------------------------------------------------------------- apps

    private void loadApps() {
        List<AppCatalog.Entry> loaded = AppCatalog.load(this, getPackageName());

        // Orden: las apps de config.xml primero, el resto alfabeticamente
        // y las marcadas como ultimas al final.
        List<String> first = Arrays.asList(
                getResources().getStringArray(R.array.apps_first));
        List<String> last = Arrays.asList(
                getResources().getStringArray(R.array.apps_last));

        List<AppCatalog.Entry> ordered = AppCatalog.order(loaded, first, last);
        gridModel = AppCatalog.buildGrid(ordered);
        adapter.setItems(ordered);

        navigator = new Navigator(gridModel, TOP_BAR_COUNT);

        // Foco inicial en la primera tesela. Se espera a que la rejilla
        // tenga hijos: antes de eso no hay ninguna tesela que enfocar.
        if (!ordered.isEmpty()) {
            grid.getViewTreeObserver().addOnGlobalLayoutListener(
                    new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (grid.getChildCount() == 0) {
                                return;
                            }
                            grid.getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);
                            focusGridCell(0, 0);
                        }
                    });
        }
    }

    /** Devuelve el foco a donde estaba antes de abrir otra app. */
    private void restoreFocus() {
        if (navigator == null || grid.getChildCount() == 0) {
            return;
        }
        FocusState state = navigator.focus();
        if (state.zone == Zone.TOP_BAR) {
            View top = topBarChild(state.topIndex);
            if (top != null) {
                top.requestFocus();
            }
        } else {
            focusGridCell(state.row, state.column);
        }
    }

    // ------------------------------------------------------------- reloj

    private void updateClock() {
        Calendar calendar = Calendar.getInstance();
        Locale locale = Locale.getDefault();
        clock.setText(Clock.format(calendar, locale));
        date.setText(Clock.formatDate(calendar, locale));

        Greeting g = Greeting.forHour(calendar.get(Calendar.HOUR_OF_DAY));
        switch (g) {
            case MORNING:
                greeting.setText(R.string.greeting_morning);
                break;
            case AFTERNOON:
                greeting.setText(R.string.greeting_afternoon);
                break;
            case EVENING:
                greeting.setText(R.string.greeting_evening);
                break;
            default:
                greeting.setText(R.string.greeting_night);
                break;
        }
    }

    // ---------------------------------------------------------- navegacion

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;

        // La tecla OK se consume entera, subida y bajada. Si la subida
        // llegase al framework, este haria un segundo clic y la app se
        // abriria dos veces.
        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
            if (down) {
                activateFocused();
            }
            return true;
        }

        if (!down) {
            return super.dispatchKeyEvent(event);
        }

        switch (code) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (panelOpen()) {
                    moveInPanel(1);
                } else {
                    moveFocus(Direction.DOWN);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (panelOpen()) {
                    moveInPanel(-1);
                } else {
                    moveFocus(Direction.UP);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (panelOpen()) {
                    closePanel();
                } else {
                    moveFocus(Direction.LEFT);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!panelOpen()) {
                    moveFocus(Direction.RIGHT);
                }
                return true;
            case KeyEvent.KEYCODE_HOME:
                // El launcher ya esta en casa.
                return true;
            case KeyEvent.KEYCODE_TV_INPUT:
                // Boton de entradas del mando.
                toggleInputs();
                return true;
            case KeyEvent.KEYCODE_SETTINGS:
                // Boton de ajustes del mando.
                openSettings();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (panelOpen()) {
                    closePanel();
                    return true;
                }
                break;
            default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    private enum Direction { UP, DOWN, LEFT, RIGHT }

    private void moveFocus(Direction direction) {
        if (navigator == null) {
            return;
        }
        FocusState before = navigator.focus();
        switch (direction) {
            case UP:
                navigator.up();
                break;
            case DOWN:
                navigator.down();
                break;
            case LEFT:
                navigator.left();
                break;
            case RIGHT:
                navigator.right();
                break;
            default:
                break;
        }
        FocusState after = navigator.focus();

        // Se reaplica el foco si cambio la zona o el indice dentro de ella.
        if (after.zone != before.zone
                || after.row != before.row
                || after.column != before.column
                || after.topIndex != before.topIndex) {
            applyFocus(after);
        }
    }

    private void applyFocus(FocusState state) {
        switch (state.zone) {
            case TOP_BAR:
                highlight.setVisibility(View.GONE);
                View top = topBarChild(state.topIndex);
                if (top != null) {
                    top.requestFocus();
                }
                break;
            case GRID:
            default:
                focusGridCell(state.row, state.column);
                break;
        }
    }

    private View topBarChild(int index) {
        switch (index) {
            case 0:
                return btnInputs;
            case 1:
                return btnWifi;
            case 2:
                return btnSettings;
            default:
                return null;
        }
    }

    private void focusGridCell(int row, int column) {
        if (gridModel == null || adapter.getItemCount() == 0) {
            return;
        }
        final int position = gridModel.positionOf(row, column);
        if (position < 0) {
            return;
        }
        View view = layoutManager.findViewByPosition(position);
        if (view == null) {
            // La tesela todavia no existe: se fuerza el layout y se enfoca
            // cuando la rejilla la haya creado.
            layoutManager.scrollToPositionWithOffset(position, 0);
            focusTileWhenLaidOut(position, 0);
            return;
        }
        if (!rowAligned(row)) {
            alignRowToTop(row);
        }
        if (view.requestFocus()) {
            onTileFocused(position, view);
        }
    }

    /**
     * ¿La fila esta pegada al borde superior de la rejilla?
     */
    private boolean rowAligned(int row) {
        int position = gridModel.positionOf(row, 0);
        if (position < 0) {
            return true;
        }
        View view = layoutManager.findViewByPosition(position);
        if (view == null) {
            return false;
        }
        int offset = view.getTop() - grid.getScrollY() - grid.getPaddingTop();
        return Math.abs(offset) <= 2;
    }

    /**
     * Alinea una fila arriba del todo de la rejilla.
     *
     * El scroll es de pixeles, no un reposicionamiento: asi la tesela
     * enfocada no se recicla y no se pierde el foco, que es lo que pasaba
     * con {@code scrollToPositionWithOffset}.
     */
    private void alignRowToTop(int row) {
        int position = gridModel.positionOf(row, 0);
        if (position < 0) {
            return;
        }
        View view = layoutManager.findViewByPosition(position);
        if (view == null) {
            return;
        }
        int delta = view.getTop() - grid.getScrollY() - grid.getPaddingTop();
        if (delta != 0) {
            grid.scrollBy(0, delta);
        }
    }

    /**
     * Enfoca una tesela cuando la rejilla ya la ha creado.
     */
    private void focusTileWhenLaidOut(final int position, final int attempt) {
        grid.getViewTreeObserver().addOnGlobalLayoutListener(
                new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        android.view.ViewTreeObserver observer = grid.getViewTreeObserver();
                        if (observer.isAlive()) {
                            observer.removeOnGlobalLayoutListener(this);
                        }
                        View view = layoutManager.findViewByPosition(position);
                        if (view == null) {
                            if (attempt < 5) {
                                grid.postDelayed(
                                        () -> focusTileWhenLaidOut(position, attempt + 1), 120);
                            }
                            return;
                        }
                        if (view.requestFocus()) {
                            onTileFocused(position, view);
                        }
                    }
                });
    }

    @Override
    public void onTileFocused(int position, View tile) {
        if (navigator != null && position >= 0) {
            navigator.syncGrid(position / Grid.COLUMNS, position % Grid.COLUMNS);
        }
        // Se asegura el estado visual: al reciclar vistas, el listener de
        // foco puede no haberse disparado para la tesela enfocada.
        adapter.refreshFocus(tile, true);
        moveHighlightTo(tile);
    }

    @Override
    public void onAppClicked(AppCatalog.Entry entry) {
        AppCatalog.launch(this, entry.component);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            return;
        }
        // Al recuperar el foco (por ejemplo tras un scroll), la etiqueta y el
        // cristal se refrescan para la tesela que tenga el foco ahora.
        View focused = focusedTile();
        if (focused != null) {
            int position = layoutManager.getPosition(focused);
            if (position != RecyclerView.NO_POSITION) {
                onTileFocused(position, focused);
            }
        }
    }

    /**
     * Tesela que tiene el foco ahora, o null si el foco no esta en la rejilla.
     */
    private View focusedTile() {
        View focused = getCurrentFocus();
        if (focused == null) {
            return null;
        }
        ViewParent parent = focused.getParent();
        while (parent instanceof View) {
            if (parent == grid) {
                return focused;
            }
            focused = (View) parent;
            parent = focused.getParent();
        }
        return null;
    }

    /**
     * Coloca el cristal sobre la tesela enfocada, con un salto suave.
     */
    private void moveHighlightTo(View tile) {
        if (tile == null) {
            return;
        }
        // El cristal rodea al icono, no a la celda entera: la etiqueta queda
        // fuera del cristal. El icono ya crece dentro de sus propios limites,
        // asi que el cristal lo sigue sin salirse de la celda.
        View icon = tile.findViewById(R.id.icon);
        View anchor = icon != null ? icon : tile;

        int[] anchorPos = new int[2];
        int[] containerPos = new int[2];
        anchor.getLocationOnScreen(anchorPos);
        ((View) highlight.getParent()).getLocationOnScreen(containerPos);

        int left = anchorPos[0] - containerPos[0];
        int top = anchorPos[1] - containerPos[1];
        Rect target = new Rect(left, top,
                left + anchor.getWidth(), top + anchor.getHeight());

        highlight.setVisibility(View.VISIBLE);
        moveGlass(target);
    }

    /**
     * Anima el cristal hacia un rectangulo con un rebote suave.
     */
    private void moveGlass(final Rect target) {
        final RectF from = new RectF(highlight.bounds());
        if (from.isEmpty()) {
            highlight.moveTo(target.left, target.top, target.right, target.bottom, 26f);
            return;
        }
        highlight.animate().cancel();
        highlight.setTranslationX(0);
        highlight.setTranslationY(0);

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(180);
        animator.setInterpolator(new OvershootInterpolator(1.2f));
        animator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            float left = from.left + (target.left - from.left) * t;
            float top = from.top + (target.top - from.top) * t;
            float right = from.right + (target.right - from.right) * t;
            float bottom = from.bottom + (target.bottom - from.bottom) * t;
            highlight.moveTo(left, top, right, bottom, 26f);
        });
        animator.start();
    }

    private void onTopBarFocus(boolean focused, int index) {
        if (focused) {
            if (navigator != null) {
                navigator.syncTopBar(index);
            }
            highlight.setVisibility(View.GONE);
        }
        updateTopBarFocus(focused);
    }

    /** Las pastillas de la barra crecen con un rebote al enfocarse. */
    private void updateTopBarFocus(boolean focused) {
        float scale = focused ? 1.05f : 1f;
        for (View view : new View[]{btnInputs, btnWifi, btnSettings}) {
            view.animate().cancel();
            view.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(focused ? 200 : 140)
                    .setInterpolator(new OvershootInterpolator(2.2f))
                    .start();
        }
    }

    // ------------------------------------------------------------- acciones

    /**
     * Activa lo que tenga el foco.
     *
     * En una tesela se abre la app que dice la propia tesela, mirando su
     * posicion en la rejilla: si el foco se pierde un instante al
     * recomponer la rejilla, el sistema lo pasa a la primera tesela y se
     * abriria la app equivocada.
     */
    private void activateFocused() {
        View focused = getCurrentFocus();
        if (focused == null) {
            return;
        }
        if (focused == btnInputs) {
            toggleInputs();
            return;
        }
        if (focused == btnWifi) {
            openWifiSettings();
            return;
        }
        if (focused == btnSettings) {
            openSettings();
            return;
        }
        View tile = focusedTile();
        if (tile != null) {
            int position = layoutManager.getPosition(tile);
            AppCatalog.Entry entry = adapter.itemAt(position);
            if (entry != null) {
                AppCatalog.launch(this, entry.component);
            }
            return;
        }
        // Filas del desplegable de entradas: cada una tiene su listener.
        focused.performClick();
    }

    private void openSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.w("TVLauncher", "no se pudieron abrir los ajustes: " + e);
        }
    }

    private void openWifiSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            // Si falla Wi-Fi, no hay nada mas que hacer.
        }
    }

    // ------------------------------------------------------------- entradas

    private boolean panelOpen() {
        return inputsPanel.getVisibility() == View.VISIBLE;
    }

    /**
     * Desplegable de entradas bajo la pastilla.
     */
    private void toggleInputs() {
        if (panelOpen()) {
            closePanel();
            return;
        }
        panelReturnFocus = getCurrentFocus();
        buildInputsPanel();
        inputsPanel.setVisibility(View.VISIBLE);
        focusPanelRow(0);
    }

    private void closePanel() {
        if (!panelOpen()) {
            return;
        }
        inputsPanel.setVisibility(View.GONE);
        View back = panelReturnFocus;
        panelReturnFocus = null;
        if (back != null) {
            back.requestFocus();
        } else {
            btnInputs.requestFocus();
        }
    }

    private void buildInputsPanel() {
        inputsPanel.removeAllViews();
        List<InputOption> options = inputOptions();
        if (options.isEmpty()) {
            addInputRow(getString(R.string.input_none), null);
            return;
        }
        for (InputOption option : options) {
            addInputRow(option.label, option);
        }
    }

    private void addInputRow(String label, final InputOption option) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextColor(getResources().getColor(R.color.text_secondary));
        row.setTextSize(17f);
        row.setPadding(28, 18, 28, 18);
        row.setBackgroundResource(R.drawable.input_row_background);
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setOnFocusChangeListener((v, focused) -> ((TextView) v).setTextColor(
                getResources().getColor(focused ? R.color.text_primary : R.color.text_secondary)));
        row.setOnClickListener(v -> {
            if (option != null) {
                if (option.systemPicker) {
                    openSystemInputPicker();
                } else {
                    AppCatalog.launch(this, option.component);
                }
            }
            closePanel();
        });
        inputsPanel.addView(row);
    }

    /**
     * Las entradas que se pueden abrir de verdad.
     *
     * El Mi TV Stick no tiene HDMI ni tuner propios (el HDMI es del
     * televisor), y el sistema registra los servicios de previsualizacion de
     * las apps de streaming como si fueran tuner. Solo se listan las entradas
     * reales: el selector del sistema, si existe, y la TV en directo.
     */
    private List<InputOption> inputOptions() {
        List<InputOption> options = new ArrayList<>();
        boolean picker = hasSystemInputPicker();

        if (picker) {
            for (TvInputInfo info : tvInputs()) {
                int type = info.getType();
                if (type == TvInputs.TYPE_HDMI || type == TvInputs.TYPE_PHYSICAL_HDMI) {
                    addOption(options, getString(R.string.input_hdmi,
                            TvInputs.hdmiNumber(info.getId())), null, true);
                } else if (type == TvInputs.TYPE_COMPOSITE
                        || type == TvInputs.TYPE_PHYSICAL_COMPOSITE) {
                    addOption(options, getString(R.string.input_av), null, true);
                } else if (type == TvInputInfo.TYPE_TUNER && !TvInputs.isPreview(info.getId())) {
                    addOption(options, getString(R.string.input_live_tv), null, true);
                }
            }
        }

        // La TV en directo: la entrada real del stick.
        ComponentName liveTv = AppCatalog.liveTvComponent(this);
        if (liveTv != null) {
            addOption(options, getString(R.string.input_live_tv), liveTv, false);
        }
        return options;
    }

    /** Anade una entrada si no hay otra con la misma etiqueta. */
    private void addOption(List<InputOption> options, String label,
                           ComponentName component, boolean systemPicker) {
        for (InputOption option : options) {
            if (option.label.equals(label)) {
                return;
            }
        }
        options.add(new InputOption(label, component, systemPicker));
    }

    /**
     * Entradas declaradas por el dispositivo.
     */
    private List<TvInputInfo> tvInputs() {
        List<TvInputInfo> list = new ArrayList<>();
        try {
            TvInputManager manager =
                    (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (manager != null) {
                List<TvInputInfo> infos = manager.getTvInputList();
                if (infos != null) {
                    list.addAll(infos);
                }
            }
        } catch (Exception e) {
            // Sin framework de TV: no hay entradas.
        }
        return list;
    }

    /** ¿El sistema tiene selector de entradas? */
    private boolean hasSystemInputPicker() {
        PackageManager pm = getPackageManager();
        try {
            return pm.resolveActivity(new Intent(ACTION_SHOW_TV_INPUTS), 0) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Abre el selector de entradas del propio sistema.
     */
    private void openSystemInputPicker() {
        try {
            Intent intent = new Intent(ACTION_SHOW_TV_INPUTS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            // El sistema no tiene selector de entradas.
        }
    }

    // ------------------------------------------------- foco del desplegable

    /** Mueve el foco dentro del desplegable de entradas. */
    private void moveInPanel(int delta) {
        int count = inputsPanel.getChildCount();
        if (count == 0) {
            return;
        }
        int index = panelRowIndex(getCurrentFocus());
        if (index < 0) {
            focusPanelRow(0);
            return;
        }
        int next = index + delta;
        if (next < 0 || next >= count) {
            // El foco no se sale del desplegable.
            return;
        }
        focusPanelRow(next);
    }

    private int panelRowIndex(View view) {
        if (view == null) {
            return -1;
        }
        for (int i = 0; i < inputsPanel.getChildCount(); i++) {
            if (inputsPanel.getChildAt(i) == view) {
                return i;
            }
        }
        return -1;
    }

    private void focusPanelRow(int index) {
        if (index < 0 || index >= inputsPanel.getChildCount()) {
            return;
        }
        inputsPanel.getChildAt(index).requestFocus();
    }
}
