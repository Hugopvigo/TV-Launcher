package es.hugoperezvigo.tvlauncher;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import es.hugoperezvigo.tvlauncher.grid.Grid;
import es.hugoperezvigo.tvlauncher.grid.GridItem;
import es.hugoperezvigo.tvlauncher.nav.FocusState;
import es.hugoperezvigo.tvlauncher.nav.Navigator;
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
public class LauncherActivity extends AppCompatActivity implements AppAdapter.FocusListener {

    private static final long CLOCK_INTERVAL_MS = 20_000L;

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
    private LinearLayoutHolder inputsHolder;

    private final List<AppCatalog.Entry> entries = new ArrayList<>();
    private Grid gridModel;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, CLOCK_INTERVAL_MS);
        }
    };

    /** Envoltorio para no arrastrar el tipo de la vista del panel. */
    private static final class LinearLayoutHolder {
        final android.widget.LinearLayout view;

        LinearLayoutHolder(android.widget.LinearLayout view) {
            this.view = view;
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
        inputsHolder = new LinearLayoutHolder(findViewById(R.id.inputs_panel));

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
        btnWifi.setOnClickListener(v -> { /* la pastilla es informativa */ });
        btnSettings.setOnClickListener(v -> openSettings());
        btnInputs.setOnFocusChangeListener((v, f) -> onTopBarFocus(f, 0));
        btnWifi.setOnFocusChangeListener((v, f) -> onTopBarFocus(f, 1));
        btnSettings.setOnFocusChangeListener((v, f) -> onTopBarFocus(f, 2));

        updateClock();
        updateWifiLabel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(clockTick);
        loadApps();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockTick);
    }

    // ---------------------------------------------------------------- apps

    private void loadApps() {
        entries.clear();
        entries.addAll(AppCatalog.load(this, getPackageName()));

        // Orden: las apps de config.xml primero, el resto alfabeticamente
        // y las marcadas como ultimas al final.
        List<String> first = Arrays.asList(
                getResources().getStringArray(R.array.apps_first));
        List<String> last = Arrays.asList(
                getResources().getStringArray(R.array.apps_last));

        gridModel = AppCatalog.buildGrid(entries, first, last);

        // El adaptador recibe las apps en el mismo orden que la rejilla.
        List<AppCatalog.Entry> ordered = new ArrayList<>();
        for (GridItem item : gridModel.items()) {
            for (AppCatalog.Entry entry : entries) {
                if (entry.packageName.equals(item.packageName)) {
                    ordered.add(entry);
                    break;
                }
            }
        }
        adapter.setItems(ordered);

        navigator = new Navigator(gridModel.rows(), Grid.COLUMNS, 3);

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

    private void updateWifiLabel() {
        String name = currentWifiName();
        btnWifi.setText(name == null
                ? getString(R.string.action_wifi)
                : getString(R.string.action_wifi) + " " + name);
    }

    private String currentWifiName() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return null;
            }
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null || !info.isConnected()) {
                return null;
            }
            // El SSID no es publico en Android 10+, asi que se muestra solo
            // el tipo de red. En Android 9 se puede leer con permisos.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return null;
            }
            return info.getTypeName();
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------- navegacion

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    moveFocus(Direction.DOWN);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    moveFocus(Direction.UP);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    moveFocus(Direction.LEFT);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    moveFocus(Direction.RIGHT);
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    activateFocused();
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
                    if (inputsHolder.view.getVisibility() == View.VISIBLE) {
                        inputsHolder.view.setVisibility(View.GONE);
                        return true;
                    }
                    break;
                default:
                    break;
            }
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
        if (adapter.getItemCount() == 0) {
            return;
        }
        int position = row * Grid.COLUMNS + column;
        if (position >= adapter.getItemCount()) {
            position = adapter.getItemCount() - 1;
        }
        final int target = position;
        // Si la tesela no esta a la vista, se desplaza la rejilla hasta ella.
        View child = layoutManager.findViewByPosition(target);
        if (child == null || !isVisible(child)) {
            grid.smoothScrollToPosition(target);
        }
        grid.post(() -> {
            View view = layoutManager.findViewByPosition(target);
            if (view != null) {
                view.requestFocus();
                onTileFocused(target, view);
            } else {
                // La tesela aun no existe: se reintenta tras el scroll.
                grid.postDelayed(() -> {
                    View retry = layoutManager.findViewByPosition(target);
                    if (retry != null) {
                        retry.requestFocus();
                        onTileFocused(target, retry);
                    }
                }, 120);
            }
        });
    }

    /** Si la vista esta dentro de los limites visibles de la rejilla. */
    private boolean isVisible(View view) {
        Rect bounds = new Rect();
        view.getGlobalVisibleRect(bounds);
        Rect parent = new Rect();
        grid.getGlobalVisibleRect(parent);
        return bounds.bottom <= parent.bottom && bounds.top >= parent.top;
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

    private void activateFocused() {
        View focused = getCurrentFocus();
        if (focused == null) {
            return;
        }
        if (focused == btnInputs) {
            toggleInputs();
        } else if (focused == btnSettings) {
            openSettings();
        } else if (focused == btnWifi) {
            // La pastilla de Wi-Fi es informativa.
        }
    }

    private void openSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            // Si falla Ajustes, no hay nada mas que hacer.
        }
    }

    /**
     * Desplegable de entradas bajo la pastilla.
     */
    private void toggleInputs() {
        if (inputsHolder.view.getVisibility() == View.VISIBLE) {
            inputsHolder.view.setVisibility(View.GONE);
            return;
        }
        inputsHolder.view.removeAllViews();
        List<String> inputs = availableInputs();
        if (inputs.isEmpty()) {
            addInputRow(getString(R.string.input_none), null);
        } else {
            for (String input : inputs) {
                addInputRow(input, input);
            }
        }
        inputsHolder.view.setVisibility(View.VISIBLE);
    }

    private void addInputRow(String label, final String input) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextColor(getResources().getColor(R.color.text_primary));
        row.setTextSize(17f);
        row.setPadding(28, 18, 28, 18);
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setOnClickListener(v -> {
            if (input != null) {
                switchInput(input);
            }
            inputsHolder.view.setVisibility(View.GONE);
        });
        inputsHolder.view.addView(row);
    }

    /**
     * Entradas disponibles en el dispositivo.
     */
    private List<String> availableInputs() {
        List<String> inputs = new ArrayList<>();
        try {
            android.media.tv.TvInputManager manager =
                    (android.media.tv.TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (manager != null) {
                List<android.media.tv.TvInputInfo> list = manager.getTvInputList();
                if (list != null) {
                    for (android.media.tv.TvInputInfo info : list) {
                        int type = info.getType();
                        if (type == android.media.tv.TvInputInfo.TYPE_HDMI) {
                            inputs.add(getString(R.string.input_hdmi, hdmiNumber(info)));
                        } else if (type == android.media.tv.TvInputInfo.TYPE_COMPOSITE) {
                            inputs.add(getString(R.string.input_av));
                        } else if (type == android.media.tv.TvInputInfo.TYPE_TUNER) {
                            inputs.add(getString(R.string.input_live_tv));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Sin framework de TV: no hay entradas.
        }
        return inputs;
    }

    /**
     * Numero de puerto HDMI de una entrada, deducido de su id.
     * El framework no expone el numero directamente en la API publica.
     */
    private int hdmiNumber(android.media.tv.TvInputInfo info) {
        String id = info.getId();
        if (id == null) {
            return 1;
        }
        // Los ids de HDMI suelen acabar en el numero de puerto.
        int lastDigit = 0;
        for (int i = id.length() - 1; i >= 0; i--) {
            char c = id.charAt(i);
            if (c >= '0' && c <= '9') {
                lastDigit = c - '0';
                break;
            }
        }
        return lastDigit == 0 ? 1 : lastDigit;
    }

    /**
     * Cambia a una entrada concreta.
     */
    private void switchInput(String label) {
        try {
            android.media.tv.TvInputManager manager =
                    (android.media.tv.TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (manager == null) {
                return;
            }
            List<android.media.tv.TvInputInfo> list = manager.getTvInputList();
            if (list == null) {
                return;
            }
            for (android.media.tv.TvInputInfo info : list) {
                int type = info.getType();
                boolean hdmi = type == android.media.tv.TvInputInfo.TYPE_HDMI;
                if (hdmi && getString(R.string.input_hdmi, hdmiNumber(info)).equals(label)) {
                    // El framework publico no permite cambiar la entrada:
                    // se abre el selector de entradas del sistema, que si lo hace.
                    openSystemInputPicker();
                    return;
                }
            }
        } catch (Exception e) {
            // Cambio de entrada no permitido.
        }
    }

    /**
     * Abre el selector de entradas del propio sistema.
     */
    private void openSystemInputPicker() {
        try {
            Intent intent = new Intent("android.media.tv.action.SHOW_TV_INPUTS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ignored) {
                // Nada que hacer.
            }
        }
    }
}
