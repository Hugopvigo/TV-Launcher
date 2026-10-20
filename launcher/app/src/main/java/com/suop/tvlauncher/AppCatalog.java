package com.suop.tvlauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;

import com.suop.tvlauncher.grid.Grid;
import com.suop.tvlauncher.grid.GridItem;

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
     */
    public static List<Entry> load(Context context, String selfPackage) {
        PackageManager pm = context.getPackageManager();

        // Se conserva la primera actividad encontrada por componente.
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
                String key = info.activityInfo.packageName + "/"
                        + info.activityInfo.name;
                if (!found.containsKey(key)) {
                    found.put(key, info);
                }
            }
        }

        List<Entry> entries = new ArrayList<>();
        for (ResolveInfo info : found.values()) {
            ActivityInfo activityInfo = info.activityInfo;
            String pkg = activityInfo.packageName;
            if (pkg.equals(selfPackage)) {
                continue; // el launcher propio no se muestra a si mismo
            }
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

    /** Convierte las entradas en items de rejilla. */
    public static List<GridItem> toGridItems(List<Entry> entries) {
        List<GridItem> items = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            items.add(new GridItem(entry.packageName, entry.label));
        }
        return items;
    }

    /** Construye la rejilla con el dock pedido. */
    public static Grid buildGrid(List<Entry> entries, List<String> dockPackages) {
        return Grid.of(toGridItems(entries), Grid.COLUMNS, dockPackages);
    }

    /** Abre una app por su componente. */
    public static void launch(Context context, ComponentName component) {
        if (component == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "no se pudo abrir " + component + ": " + e);
        }
    }

    /** Usuario actual del proceso. */
    public static UserHandle currentUser() {
        return UserHandle.getUserHandleForUid(Process.myUid());
    }
}
