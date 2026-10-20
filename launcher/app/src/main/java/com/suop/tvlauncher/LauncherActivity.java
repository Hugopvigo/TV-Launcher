package com.suop.tvlauncher;

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

import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.suop.tvlauncher.grid.Grid;
import com.suop.tvlauncher.grid.GridItem;
import com.suop.tvlauncher.nav.FocusState;
import com.suop.tvlauncher.nav.Navigator;
import com.suop.tvlauncher.ui.AppAdapter;
import com.suop.tvlauncher.ui.Clock;
import com.suop.tvlauncher.ui.GlassHighlight;
import com.suop.tvlauncher.ui.Greeting;
import com.suop.tvlauncher.ui.Tint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Launcher estilo Apple TV para Android TV.
 *
 * Distribucion:
 *   - Barra superior: saludo, reloj y fecha a la izquierda; pastillas a la derecha.
 *   - Rejilla de 4 columnas con las apps de TV.
 *   - Banda del dock con cuatro iconos.
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
    private TextView dockFocusName;
    private TextView btnInputs;
    private TextView btnWifi;
    private TextView btnSettings;
    private LinearLayout inputsPanel;
    private final com.suop.tvlauncher.ui.RoundedIcon[] dockIcons = new com.suop.tvlauncher.ui.RoundedIcon[4];

    private final List<AppCatalog.Entry> entries = new ArrayList<>();
    private List<String> dockPackages = new ArrayList<>();
    private Grid gridModel;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, CLOCK_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        greeting = findViewById(R.id.greeting);
        clock = findViewById(R.id.clock);
        date = findViewById(R.id.date);
        dockFocusName = findViewById(R.id.dock_focus_name);
        btnInputs = findViewById(R.id.btn_inputs);
        btnWifi = findViewById(R.id.btn_wifi);
        btnSettings = findViewById(R.id.btn_settings);
        inputsPanel = findViewById(R.id.inputs_panel);

        grid = findViewById(R.id.grid);
        dockIcons[0] = findViewById(R.id.dock_0);
        dockIcons[1] = findViewById(R.id.dock_1);
        dockIcons[2] = findViewById(R.id.dock_2);
        dockIcons[3] = findViewById(R.id.dock_3);

        highlight = new GlassHighlight(this);

        // El dock se configura en res/values/config.xml
        String[] dock = getResources().getStringArray(R.array.dock_packages);
        dockPackages = new ArrayList<>(Arrays.asList(dock));

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

        btnInputs.setOnClickListener(v -> toggleInputs());
        btnWifi.setOnClickListener(v -> { /* la pastilla es informativa */ });
        btnSettings.setOnClickListener(v -> openSettings());
        btnInputs.setOnFocusChangeListener((v, f) -> onTopBarFocus(f, 0));
        btnWifi.setOnFocusChangeListener((v, f) -> onTopBarFocus(f, 1));
        btnSettings.setOnFocusChangeListener((v, f) -> onTopBarFocus(f, 2));

        for (int i = 0; i < dockIcons.length; i++) {
            final int index = i;
            dockIcons[i].setOnClickListener(v -> launchDock(index));
            dockIcons[i].setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    if (navigator != null) {
                        navigator.syncDock(index);
                    }
                    showDockName(index);
                    highlight.setVisibility(View.GONE);
                    // El icono del dock crece un poco al enfocarse.
                    v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150).start();
                } else {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                }
            });
        }

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

        // El dock se lee de la configuracion; se ignoran los paquetes que
        // no estén instalados.
        String[] configured = getResources().getStringArray(R.array.dock_packages);
        dockPackages = new ArrayList<>();
        for (String pkg : configured) {
            for (AppCatalog.Entry entry : entries) {
                if (entry.packageName.equals(pkg) && !dockPackages.contains(pkg)) {
                    dockPackages.add(pkg);
                }
            }
        }

        gridModel = AppCatalog.buildGrid(entries, dockPackages);

        // La rejilla son las apps que no estan en el dock.
        List<AppCatalog.Entry> gridEntries = new ArrayList<>();
        for (AppCatalog.Entry entry : entries) {
            if (!dockPackages.contains(entry.packageName)) {
                gridEntries.add(entry);
            }
        }
        adapter.setItems(gridEntries);

        buildDock();
        rebuildNavigator();
        refreshDockName();

        // Foco inicial en la primera tesela. Se espera a que la rejilla
        // tenga hijos: antes de eso no hay ninguna tesela que enfocar.
        if (gridEntries.size() > 0) {
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

    private void buildDock() {
        for (int i = 0; i < dockIcons.length; i++) {
            String pkg = i < dockPackages.size() ? dockPackages.get(i) : null;
            AppCatalog.Entry entry = null;
            if (pkg != null) {
                for (AppCatalog.Entry candidate : entries) {
                    if (candidate.packageName.equals(pkg)) {
                        entry = candidate;
                        break;
                    }
                }
            }
            if (entry != null) {
                dockIcons[i].setIcon(entry.icon);
                dockIcons[i].setVisibility(View.VISIBLE);
                dockIcons[i].setContentDescription(entry.label);
            } else {
                dockIcons[i].setIcon(null);
                dockIcons[i].setVisibility(View.INVISIBLE);
            }
        }
    }

    private void rebuildNavigator() {
        int rows = gridModel == null ? 0 : gridModel.rows();
        navigator = new Navigator(rows, Grid.COLUMNS, dockPackages.size(), 3);
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
                    if (inputsPanel.getVisibility() == View.VISIBLE) {
                        inputsPanel.setVisibility(View.GONE);
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
                || after.topIndex != before.topIndex
                || after.dockIndex != before.dockIndex) {
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
            case DOCK:
                highlight.setVisibility(View.GONE);
                if (state.dockIndex < dockIcons.length) {
                    dockIcons[state.dockIndex].requestFocus();
                    showDockName(state.dockIndex);
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
        // fuera del cristal.
        View icon = tile.findViewById(R.id.icon);
        View anchor = icon != null ? icon : tile;

        int[] anchorPos = new int[2];
        int[] containerPos = new int[2];
        anchor.getLocationOnScreen(anchorPos);
        ((View) highlight.getParent()).getLocationOnScreen(containerPos);

        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        int left = anchorPos[0] - containerPos[0] - pad;
        int top = anchorPos[1] - containerPos[1] - pad;
        Rect target = new Rect(left, top,
                left + anchor.getWidth() + pad * 2,
                top + anchor.getHeight() + pad * 2);

        highlight.setVisibility(View.VISIBLE);
        moveGlass(target);
    }

    /**
     * Anima el cristal hacia un rectangulo con un rebote suave.
     */
    private void moveGlass(final Rect target) {
        final RectF from = new RectF(highlight.bounds());
        if (from.isEmpty()) {
            highlight.moveTo(target.left, target.top, target.right, target.bottom, 20f);
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
            highlight.moveTo(left, top, right, bottom, 20f);
        });
        animator.start();
    }

    private void showDockName(int index) {
        String pkg = index < dockPackages.size() ? dockPackages.get(index) : null;
        if (pkg == null) {
            dockFocusName.setText("");
            return;
        }
        for (AppCatalog.Entry entry : entries) {
            if (entry.packageName.equals(pkg)) {
                dockFocusName.setText(entry.label);
                // Resplandor con el color del icono, aclarado.
                int color = dominantColor(entry);
                dockFocusName.setShadowLayer(26f, 0f, 0f, Tint.brighter(color, 1.7f));
                return;
            }
        }
        dockFocusName.setText("");
    }

    /**
     * Nombre que se muestra sobre el dock. Con el foco en la rejilla se
     * mantiene el ultimo icono del dock que se enfoco.
     */
    private void refreshDockName() {
        int index = navigator != null ? navigator.focus().dockIndex : 0;
        if (index >= dockPackages.size()) {
            index = 0;
        }
        showDockName(index);
    }

    /**
     * Color dominante del icono de una app, para el resplandor.
     */
    private int dominantColor(AppCatalog.Entry entry) {
        if (entry.icon == null) {
            return 0xFF8E8E93;
        }
        try {
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                    48, 48, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            entry.icon.setBounds(0, 0, 48, 48);
            entry.icon.draw(canvas);

            long r = 0, g = 0, b = 0;
            int count = 0;
            for (int y = 0; y < 48; y += 4) {
                for (int x = 0; x < 48; x += 4) {
                    int pixel = bitmap.getPixel(x, y);
                    if (Tint.alpha(pixel) < 128) {
                        continue;
                    }
                    r += Tint.red(pixel);
                    g += Tint.green(pixel);
                    b += Tint.blue(pixel);
                    count++;
                }
            }
            bitmap.recycle();
            if (count == 0) {
                return 0xFF8E8E93;
            }
            return (int) (0xFF000000
                    | ((r / count) << 16)
                    | ((g / count) << 8)
                    | (b / count));
        } catch (Exception e) {
            return 0xFF8E8E93;
        }
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

    /** Las pastillas de la barra crecen un poco al enfocarse. */
    private void updateTopBarFocus(boolean focused) {
        float scale = focused ? 1.06f : 1f;
        for (View view : new View[]{btnInputs, btnWifi, btnSettings}) {
            view.animate().scaleX(scale).scaleY(scale).setDuration(120).start();
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

    private void launchDock(int index) {
        String pkg = index < dockPackages.size() ? dockPackages.get(index) : null;
        if (pkg == null) {
            return;
        }
        for (AppCatalog.Entry entry : entries) {
            if (entry.packageName.equals(pkg)) {
                AppCatalog.launch(this, entry.component);
                return;
            }
        }
    }

    private void openSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            // Si falla Ajustes, se intenta el launcher de apps.
        }
    }

    /**
     * Desplegable de entradas bajo la pastilla.
     */
    private void toggleInputs() {
        if (inputsPanel.getVisibility() == View.VISIBLE) {
            inputsPanel.setVisibility(View.GONE);
            return;
        }
        inputsPanel.removeAllViews();
        List<String> inputs = availableInputs();
        if (inputs.isEmpty()) {
            addInputRow(getString(R.string.input_none), null);
        } else {
            for (String input : inputs) {
                addInputRow(input, input);
            }
        }
        inputsPanel.setVisibility(View.VISIBLE);
    }

    private void addInputRow(String label, final String input) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextColor(getResources().getColor(R.color.text_primary));
        row.setTextSize(18f);
        row.setPadding(28, 18, 28, 18);
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setOnClickListener(v -> {
            if (input != null) {
                switchInput(input);
            }
            inputsPanel.setVisibility(View.GONE);
        });
        inputsPanel.addView(row);
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
