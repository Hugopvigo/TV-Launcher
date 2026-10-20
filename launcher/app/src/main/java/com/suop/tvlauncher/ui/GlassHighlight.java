package com.suop.tvlauncher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * El "cristal": un unico elemento translucido que se mueve de tesela en
 * tesela en vez de dibujar un borde en cada tesela.
 *
 * Se dibuja *detras* de los iconos: solo se ve el halo alrededor de la
 * tesela enfocada, con un degradado suave y un resplandor difuminado.
 */
public class GlassHighlight extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private float radius = 24f;

    public GlassHighlight(Context context) {
        this(context, null);
    }

    public GlassHighlight(Context context, AttributeSet attrs) {
        super(context, attrs);
        fill.setStyle(Paint.Style.FILL);
        glow.setStyle(Paint.Style.FILL);
        setWillNotDraw(false);
    }

    /**
     * Coloca el cristal en una posicion, de forma instantanea.
     */
    public void moveTo(float left, float top, float right, float bottom, float cornerRadius) {
        this.radius = cornerRadius;
        rect.set(left, top, right, bottom);
        // Degradado diagonal suave, como el cristal de tvOS.
        fill.setShader(new LinearGradient(left, top, right, bottom,
                new int[]{0x30FFFFFF, 0x14FFFFFF},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        // Resplandor difuminado por debajo.
        glow.setShader(new RadialGradient(
                rect.centerX(), rect.centerY(),
                Math.max(rect.width(), rect.height()) * 0.62f,
                new int[]{0x1AFFFFFF, 0x00FFFFFF},
                new float[]{0.55f, 1f}, Shader.TileMode.CLAMP));
        invalidate();
    }

    /** Oculta el cristal. */
    public void hide() {
        rect.setEmpty();
        invalidate();
    }

    /** Rectangulo actual, para depurar. */
    public RectF bounds() {
        return rect;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (rect.isEmpty()) {
            return;
        }
        canvas.drawRoundRect(rect, radius, radius, glow);
        canvas.drawRoundRect(rect, radius, radius, fill);
    }
}
