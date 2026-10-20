package com.suop.tvlauncher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
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
 * Dibuja tambien un degradado interior muy suave, para que el icono no
 * quede plano sobre el negro.
 */
public class RoundedIcon extends View {

    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF bounds = new RectF();
    private final Rect source = new Rect();

    private Drawable icon;
    private float cornerRadius;

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

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float r = Math.min(cornerRadius, Math.min(w, h) / 2f);

        bounds.set(0, 0, w, h);
        clip.reset();
        clip.addRoundRect(bounds, r, r, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);

        if (icon != null) {
            // El icono se recorta a cuadrado, centrado.
            float size = Math.min(w, h);
            float left = (w - size) / 2f;
            float top = (h - size) / 2f;
            source.set(0, 0, Math.max(1, icon.getIntrinsicWidth()),
                    Math.max(1, icon.getIntrinsicHeight()));
            icon.setBounds((int) left, (int) top, (int) (left + size), (int) (top + size));
            icon.draw(canvas);
        }
        canvas.restore();
    }
}
