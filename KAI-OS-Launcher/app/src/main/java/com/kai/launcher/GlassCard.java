package com.kai.launcher;

import android.content.Context;

/** Скляна картка: менше заокруглення й більші відступи, ніж у GlassPanel. */
public class GlassCard extends GlassPanel {

    public GlassCard(Context context, Theme theme) {
        super(context, theme, Math.max(12, theme.cornerDp - 8));
        int p = Glass.dp(context, 18);
        setPadding(p, p, p, p);
    }
}
