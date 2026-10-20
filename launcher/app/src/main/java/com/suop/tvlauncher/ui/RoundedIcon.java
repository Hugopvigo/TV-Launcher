package com.suop.tvlauncher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Icono de app con forma de cuadrado redondeado, al estilo de tvOS.
 *
 * Los iconos de TV vienen de muchas formas: los de las apps de Google son
 * cuadrados, pero algunos de Xiaomi son banners anchos. Al recortarlos a un
 * cuadrado con esquinas redondeadas todos se ven igual de bien.
 *
 * El crecimiento al enfocarse se dibuja *dentro* del icono, nunca
 * escalando la vista: si se escalase la vista, el icono se saldría de su
 * celda en la rejilla y quedaria cortado.
 */
public class RoundedIcon extends View {

    /** Proporcion del icono dentro de la vista cuando no esta enfocado. */
    private static final float SIZE_NORMAL = 0.84f;
    /** Proporcion cuando esta enfocado: ocupa la vista entera. */
    private static final float SIZE_FOCUSED = 1f;
    /** Radio de las esquinas, como proporcion del lado. */
    private static final float CORNER = 0.22f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF bounds = new RectF();
    private final Rect source = new Rect();

    private Drawable icon;
    private float cornerRadius;
    private boolean focused;

    public RoundedIcon(Context context) {
        this(context, null);
    }

    public RoundedIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        cornerRadius = 24f * getResources().getDisplayMetrics().density;
    }

    /** Icono a mostrar. */
    public void setIcon(Drawable drawable) {
        this.icon = drawable;
        invalidate();
    }

    public Drawable getIcon() {
        return icon;
    }

    /** El icono crece al enfocarse, dentro de sus propios limites. */
    public void setFocused(boolean isFocused) {
        if (this.focused == isFocused) {
            return;
        }
        this.focused = isFocused;
        invalidate();
    }

    public boolean isFocusedIcon() {
        return focused;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        float side = Math.min(w, h);
        float factor = focused ? SIZE_FOCUSED : SIZE_NORMAL;
        float draw = side * factor;
        float left = (w - draw) / 2f;
        float top = (h - draw) / 2f;

        // El recorte redondeado se ajusta al tamano real del icono, para que
        // al crecer no le "coman" las esquinas.
        float r = Math.min(cornerRadius * factor, draw * CORNER);
        bounds.set(left, top, left + draw, top + draw);
        clip.reset();
        clip.addRoundRect(bounds, r, r, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clip);

        if (icon != null) {
            source.set(0, 0, Math.max(1, icon.getIntrinsicWidth()),
                    Math.max(1, icon.getIntrinsicHeight()));
            icon.setBounds((int) left, (int) top, (int) (left + draw), (int) (top + draw));
            icon.draw(canvas);
        }
        canvas.restore();
    }
}
