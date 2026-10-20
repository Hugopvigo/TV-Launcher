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
 * Se dibuja *detras* de los iconos, como un halo: un resplandor difuminado
 * con la forma del icono, sin bordes rectos ni recuadros.
 */
public class GlassHighlight extends View {

    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private float radius = 26f;

    public GlassHighlight(Context context) {
        this(context, null);
    }

    public GlassHighlight(Context context, AttributeSet attrs) {
        super(context, attrs);
        glow.setStyle(Paint.Style.FILL);
        fill.setStyle(Paint.Style.FILL);
        setWillNotDraw(false);
    }

    /**
     * Coloca el cristal en una posicion, de forma instantanea.
     */
    public void moveTo(float left, float top, float right, float bottom, float cornerRadius) {
        this.radius = cornerRadius;
        rect.set(left, top, right, bottom);
        float cx = rect.centerX();
        float cy = rect.centerY();
        float r = Math.max(rect.width(), rect.height()) / 2f;

        // Resplandor: un halo blanco que se apaga hacia fuera.
        glow.setShader(new RadialGradient(cx, cy, r * 1.15f,
                new int[]{0x59FFFFFF, 0x26FFFFFF, 0x00FFFFFF},
                new float[]{0.55f, 0.80f, 1f}, Shader.TileMode.CLAMP));
        // Relleno: apenas un velo, para que el icono se vea limpio.
        fill.setShader(new RadialGradient(cx, cy, r,
                new int[]{0x1FFFFFFF, 0x00FFFFFF},
                new float[]{0.6f, 1f}, Shader.TileMode.CLAMP));
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
        canvas.drawCircle(rect.centerX(), rect.centerY(),
                Math.max(rect.width(), rect.height()) * 0.62f, glow);
        canvas.drawRoundRect(rect, radius, radius, fill);
    }
}
