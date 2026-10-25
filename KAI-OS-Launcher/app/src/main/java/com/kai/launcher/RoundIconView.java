package com.kai.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Кругла іконка: стиль, прозорість і відступи беруться з теми. */
public class RoundIconView extends View {
    private static final ColorMatrixColorFilter MONO_FILTER;

    static {
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0f);
        MONO_FILTER = new ColorMatrixColorFilter(cm);
    }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private Drawable icon;
    private Theme theme;

    public RoundIconView(Context context, Theme theme) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        fill.setStyle(Paint.Style.FILL);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(density);
        applyTheme(theme);
    }

    public void applyTheme(Theme t) {
        theme = t;
        edge.setColor(Glass.withAlpha(Color.WHITE, t.strokeAlpha));
        edge.setStrokeWidth(Math.max(1f, density * 0.7f));
        updateFill();
        invalidate();
    }

    public void setIcon(Drawable d) {
        icon = d;
        invalidate();
    }

    private void updateFill() {
        int h = getHeight();
        if (theme.iconStyle == Theme.STYLE_NATIVE) {
            fill.setShader(null);
            fill.setColor(Color.TRANSPARENT);
            edge.setColor(Color.TRANSPARENT);
            return;
        }

        edge.setColor(Glass.withAlpha(Color.WHITE, Math.min(80, theme.strokeAlpha)));
        edge.setStrokeWidth(Math.max(1f, density * 0.6f));

        if (theme.iconStyle == Theme.STYLE_SOLID) {
            fill.setShader(null);
            fill.setColor(theme.accentColor);
        } else if (theme.iconStyle == Theme.STYLE_MONO) {
            fill.setShader(null);
            fill.setColor(Glass.withAlpha(0xFF1F2937, Math.max(40, theme.glassAlpha)));
        } else if (!theme.lightweight && h > 0) {
            fill.setShader(new LinearGradient(0, 0, 0, h,
                    Glass.withAlpha(Color.WHITE, theme.glassAlpha + 18),
                    Glass.withAlpha(0xFF0F172A, theme.glassAlpha),
                    Shader.TileMode.CLAMP));
        } else {
            fill.setShader(null);
            fill.setColor(Glass.withAlpha(Color.WHITE, theme.glassAlpha));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateFill();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float d = Math.min(w, h);
        float r = d / 2f;
        float cx = w / 2f;
        float cy = h / 2f;

        if (theme.iconStyle != Theme.STYLE_NATIVE) {
            float inset = density * 1.2f;
            float left = (w - d) / 2f + inset;
            float top = (h - d) / 2f + inset;
            float right = left + d - inset * 2f;
            float bottom = top + d - inset * 2f;
            float radius = d * 0.22f;
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, fill);
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, edge);
        }

        if (icon != null) {
            int inset = (int) (d * (theme.iconStyle == Theme.STYLE_NATIVE ? Math.max(12, theme.iconPaddingPct) : theme.iconPaddingPct));
            int left = (int) ((w - d) / 2f) + inset;
            int top = (int) ((h - d) / 2f) + inset;
            int right = (int) ((w + d) / 2f) - inset;
            int bottom = (int) ((h + d) / 2f) - inset;
            icon.setBounds(left, top, right, bottom);
            icon.setColorFilter(theme.iconStyle == Theme.STYLE_MONO ? MONO_FILTER : null);
            icon.draw(canvas);
        }
    }
}
