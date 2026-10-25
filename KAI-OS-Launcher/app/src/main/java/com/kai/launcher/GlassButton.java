package com.kai.launcher;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

public class GlassButton extends TextView {

    public GlassButton(Context context, Theme theme, CharSequence text, boolean selected) {
        super(context);
        setText(text);
        setTextColor(theme.textColor);
        setTextSize(14);
        setGravity(Gravity.CENTER);
        setClickable(true);
        setSingleLine(true);
        setMinHeight(Glass.dp(context, 48));
        setPadding(Glass.dp(context, 18), Glass.dp(context, 10),
                Glass.dp(context, 18), Glass.dp(context, 10));

        if (selected) {
            setBackground(Glass.selected(context, theme, 22));
        } else {
            setBackground(Glass.panel(context, theme, 22));
        }
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        animate().alpha(pressed ? 0.75f : 1f)
                .scaleX(pressed ? 0.94f : 1f)
                .scaleY(pressed ? 0.94f : 1f)
                .setDuration(pressed ? 80 : 220)
                .setInterpolator(pressed ? new android.view.animation.DecelerateInterpolator()
                        : new OvershootInterpolator(1.2f))
                .start();
    }
}
