package com.suop.tvlauncher.grid;

/**
 * Una app de la rejilla: paquete y etiqueta visible.
 */
public final class GridItem {

    public final String packageName;
    public final String label;

    public GridItem(String packageName, String label) {
        this.packageName = packageName;
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GridItem)) return false;
        GridItem other = (GridItem) o;
        return packageName.equals(other.packageName);
    }

    @Override
    public int hashCode() {
        return packageName.hashCode();
    }

    @Override
    public String toString() {
        return packageName;
    }
}
