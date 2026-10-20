package com.suop.tvlauncher.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.suop.tvlauncher.AppCatalog;
import com.suop.tvlauncher.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de la rejilla de apps.
 */
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {

    /** Interfaz con el callback de foco. */
    public interface FocusListener {
        void onTileFocused(int position, View tile);
    }

    private final List<AppCatalog.Entry> items = new ArrayList<>();
    private final FocusListener listener;

    public AppAdapter(FocusListener listener) {
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
        holder.icon.setScaleX(1f);
        holder.icon.setScaleY(1f);
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
     * Marca una tesela como enfocada: crece un poco y muestra su nombre.
     */
    void setFocused(View tile) {
        if (tile == null) {
            return;
        }
        View icon = tile.findViewById(R.id.icon);
        View label = tile.findViewById(R.id.label);
        if (icon != null) {
            icon.animate().cancel();
            icon.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.6f))
                    .start();
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
        View icon = tile.findViewById(R.id.icon);
        View label = tile.findViewById(R.id.label);
        if (icon != null) {
            icon.animate().cancel();
            icon.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
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
