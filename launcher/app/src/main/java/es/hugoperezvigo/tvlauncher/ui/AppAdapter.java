package es.hugoperezvigo.tvlauncher.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import es.hugoperezvigo.tvlauncher.AppCatalog;
import es.hugoperezvigo.tvlauncher.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de la rejilla de apps.
 */
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {

    /** Interfaz con los callbacks de foco y de clic. */
    public interface Listener {
        void onTileFocused(int position, View tile);

        void onAppClicked(AppCatalog.Entry entry);
    }

    private final List<AppCatalog.Entry> items = new ArrayList<>();
    private final Listener listener;

    public AppAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    /** Sustituye la lista de apps. */
    public void setItems(List<AppCatalog.Entry> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /** Entrada por posicion. */
    public AppCatalog.Entry itemAt(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        return items.get(position);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tile, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AppCatalog.Entry entry = items.get(position);
        holder.icon.setIcon(entry.icon);
        holder.label.setText(entry.label);
        // El nombre solo se ve en la tesela enfocada.
        holder.label.setAlpha(0f);
        holder.icon.setFocused(false);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                setFocused(v);
                if (listener != null) {
                    listener.onTileFocused(holder.getBindingAdapterPosition(), v);
                }
            } else {
                clearFocus(v);
            }
        });
        // Sin esto la tesela no abre nada: la tecla OK acaba en performClick().
        holder.itemView.setOnClickListener(v -> {
            int current = holder.getBindingAdapterPosition();
            AppCatalog.Entry clicked = itemAt(current);
            if (listener != null && clicked != null) {
                listener.onAppClicked(clicked);
            }
        });
    }

    /**
     * Refresca el estado visual de una tesela: la enfocada crece y muestra
     * su nombre; el resto, no.
     */
    public void refreshFocus(View tile, boolean focused) {
        if (focused) {
            setFocused(tile);
        } else {
            clearFocus(tile);
        }
    }

    /**
     * Marca una tesela como enfocada: el icono crece *dentro* de sus limites
     * (si se escalase la vista, se saldria de la celda) y muestra su nombre.
     */
    void setFocused(View tile) {
        if (tile == null) {
            return;
        }
        RoundedIcon icon = tile.findViewById(R.id.icon);
        View label = tile.findViewById(R.id.label);
        if (icon != null) {
            icon.setFocused(true);
        }
        if (label != null) {
            label.animate().cancel();
            label.animate().alpha(1f).setDuration(160).start();
        }
    }

    /** Devuelve una tesela a su estado normal. */
    void clearFocus(View tile) {
        if (tile == null) {
            return;
        }
        RoundedIcon icon = tile.findViewById(R.id.icon);
        View label = tile.findViewById(R.id.label);
        if (icon != null) {
            icon.setFocused(false);
        }
        if (label != null) {
            label.animate().cancel();
            label.animate().alpha(0f).setDuration(140).start();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).packageName.hashCode();
    }

    /** Vista de una tesela. */
    static class Holder extends RecyclerView.ViewHolder {
        final RoundedIcon icon;
        final TextView label;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            label = itemView.findViewById(R.id.label);
        }
    }
}
