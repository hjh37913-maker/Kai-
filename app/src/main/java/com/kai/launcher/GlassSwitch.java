package com.kai.launcher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.Switch;

public class GlassSwitch extends Switch {

    public GlassSwitch(Context context, Theme theme, CharSequence label, boolean checked) {
        super(context);
        setText(label);
        setTextColor(theme.textColor);
        setChecked(checked);

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] {}
        };
        setThumbTintList(new ColorStateList(states,
                new int[] { theme.accentColor, 0xFFE5E7EB }));
        setTrackTintList(new ColorStateList(states,
                new int[] { Glass.withAlpha(theme.accentColor, 90), Glass.withAlpha(Color.WHITE, 40) }));
    }
}
