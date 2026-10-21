package es.hugoperezvigo.tvlauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

import es.hugoperezvigo.tvlauncher.grid.Grid;
import es.hugoperezvigo.tvlauncher.grid.GridItem;
import es.hugoperezvigo.tvlauncher.ui.AppOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogo de apps de TV instaladas.
 *
 * Se usa PackageManager y no LauncherApps: en Android 9 LauncherApps solo
 * devuelve las apps que el sistema considera "de perfil propio" y se dejaba
 * fuera Amazon Video, HBO Max, Play Games y Videos.
 */
public final class AppCatalog {

    private static final String TAG = "TVLauncher";

    /** Paquete de la app de TV en directo de Google TV. */
    private static final String GOOGLE_TV_PACKAGE = "com.google.android.tv";
    /** Actividad principal de esa app. */
    private static final String GOOGLE_TV_ACTIVITY = "com.android.tv.TvActivity";

    /** Categoria de la TV en directo. No es publica en el SDK. */
    private static final String CATEGORY_LIVE_TV = "android.intent.category.LIVE_TV";

    private AppCatalog() {
    }

    /** Una app de TV con su icono. */
    public static final class Entry {
        public final String packageName;
        public final String label;
        public final ComponentName component;
        public final Drawable icon;

        public Entry(String packageName, String label, ComponentName component, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.component = component;
            this.icon = icon;
        }
    }

    /**
     * Apps de TV del usuario actual, ordenadas alfabeticamente.
     *
     * Se consultan por separado LEANBACK_LAUNCHER y LAUNCHER: en un mismo
     * intent, Android exige que la app declare *todas* las categorias, asi
     * que juntas solo saldrian las que declaran las dos.
     *
     * De cada paquete se queda una sola actividad: el sistema tambien
     * devuelve las clases {@code *Application} que declaran el filtro, y
     * esas no se pueden abrir.
     */
    public static List<Entry> load(Context context, String selfPackage) {
        PackageManager pm = context.getPackageManager();

        // Se conserva la mejor actividad de cada paquete.
        Map<String, ResolveInfo> found = new LinkedHashMap<>();
        for (String category : new String[]{
                Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN);
            query.addCategory(category);
            List<ResolveInfo> resolved = pm.queryIntentActivities(query, 0);
            if (resolved == null) {
                continue;
            }
            for (ResolveInfo info : resolved) {
                if (info.activityInfo == null) {
                    continue;
                }
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(selfPackage)) {
                    continue; // el launcher propio no se muestra a si mismo
                }
                ResolveInfo previous = found.get(pkg);
                if (previous == null || isBetterActivity(previous, info)) {
                    found.put(pkg, info);
                }
            }
        }

        List<Entry> entries = new ArrayList<>();
        for (ResolveInfo info : found.values()) {
            ActivityInfo activityInfo = info.activityInfo;
            String pkg = activityInfo.packageName;
            CharSequence raw = info.loadLabel(pm);
            String label = raw != null ? raw.toString() : pkg;
            Drawable icon = null;
            try {
                icon = activityInfo.loadIcon(pm);
            } catch (Exception e) {
                icon = null;
            }
            entries.add(new Entry(pkg, label,
                    new ComponentName(activityInfo.packageName, activityInfo.name), icon));
        }

        Collections.sort(entries, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return entries;
    }

    /**
     * Una actividad sirve mejor que la otra: las clases de aplicacion
     * ({@code *Application}) no se pueden abrir.
     */
    private static boolean isBetterActivity(ResolveInfo current, ResolveInfo candidate) {
        return isApplicationClass(current.activityInfo.name)
                && !isApplicationClass(candidate.activityInfo.name);
    }

    private static boolean isApplicationClass(String className) {
        if (className == null) {
            return false;
        }
        String simple = className.substring(className.lastIndexOf('.') + 1);
        return simple.contains("Application");
    }

    /** Convierte las entradas en items de rejilla. */
    public static List<GridItem> toGridItems(List<Entry> entries) {
        List<GridItem> items = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            items.add(new GridItem(entry.packageName, entry.label));
        }
        return items;
    }

    /**
     * Ordena las apps segun la configuracion: las prioritarias primero, el
     * resto alfabeticamente y las marcadas como ultimas al final.
     */
    public static List<Entry> order(List<Entry> entries,
                                    List<String> first,
                                    List<String> last) {
        Map<String, Entry> byPackage = new LinkedHashMap<>();
        for (Entry entry : entries) {
            byPackage.put(entry.packageName, entry);
        }
        List<Entry> ordered = new ArrayList<>(entries.size());
        for (GridItem item : AppOrder.sort(toGridItems(entries), first, last)) {
            Entry entry = byPackage.get(item.packageName);
            if (entry != null) {
                ordered.add(entry);
            }
        }
        return ordered;
    }

    /** Construye la rejilla con todas las apps, en el orden configurado. */
    public static Grid buildGrid(List<Entry> ordered) {
        return Grid.of(toGridItems(ordered), Grid.COLUMNS);
    }

    /** Abre una app por su componente. */
    public static void launch(Context context, ComponentName component) {
        if (component == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "no se pudo abrir " + component + ": " + e);
        }
    }

    /**
     * La app de TV en directo, si la hay.
     *
     * Se busca primero una app que declare la categoria LIVE_TV. El Mi TV
     * Stick no declara ninguna: su TV en directo es la app de Google TV.
     */
    public static ComponentName liveTvComponent(Context context) {
        PackageManager pm = context.getPackageManager();
        ResolveInfo info = pm.resolveActivity(
                new Intent(Intent.ACTION_MAIN).addCategory(CATEGORY_LIVE_TV), 0);
        if (info != null && info.activityInfo != null) {
            return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
        }
        Intent googleTv = new Intent(Intent.ACTION_MAIN)
                .setComponent(new ComponentName(GOOGLE_TV_PACKAGE, GOOGLE_TV_ACTIVITY));
        if (pm.resolveActivity(googleTv, 0) != null) {
            return new ComponentName(GOOGLE_TV_PACKAGE, GOOGLE_TV_ACTIVITY);
        }
        return null;
    }
}
