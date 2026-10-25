package com.kai.launcher;

import android.content.Context;
import android.widget.LinearLayout;

/** Вертикальна скляна панель-контейнер. */
public class GlassPanel extends LinearLayout {

    public GlassPanel(Context context, Theme theme) {
        this(context, theme, theme.cornerDp);
    }

    public GlassPanel(Context context, Theme theme, int radiusDp) {
        super(context);
        setOrientation(VERTICAL);
        setBackground(Glass.panel(context, theme, radiusDp));
        int p = Glass.dp(context, 16);
        setPadding(p, p, p, p);
    }

    public void applyTheme(Theme theme, int radiusDp) {
        setBackground(Glass.panel(getContext(), theme, radiusDp));
    }
}
