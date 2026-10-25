package com.kai.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

/**
 * Premium glass material factory.
 * Uses controlled alpha gradients. Real blur is avoided by default for performance
 * and consistent results across API levels. Text contrast is preserved.
 */
public final class Glass {

    private Glass() {}

    public static int withAlpha(int color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * Premium glass panel: soft vertical highlight, restrained edge, no neon.
     */
    public static GradientDrawable panel(Context c, Theme t, float radiusDp) {
        int baseAlpha = Math.max(8, t.glassAlpha);
        int top = withAlpha(0xFFFFFFFF, Math.min(160, baseAlpha + (t.lightweight ? 4 : 16)));
        int bottom = withAlpha(0xFF000000, Math.min(120, baseAlpha + 4));

        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[] { top, bottom });
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 0.5f)), withAlpha(Color.WHITE, Math.min(48, t.strokeAlpha)));
        return g;
    }

    /** Darker, higher-contrast panel for dialogs. */
    public static GradientDrawable dialogPanel(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xF0121824);
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 1)), withAlpha(Color.WHITE, 48));
        return g;
    }

    /** Subtle selected state for controls. */
    public static GradientDrawable selected(Context c, Theme t, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, radiusDp));
        g.setColor(withAlpha(t.accentColor, 72));
        g.setStroke(Math.max(1, dp(c, 1.2f)), withAlpha(t.accentColor, 180));
        return g;
    }
}
