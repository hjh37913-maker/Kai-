package com.kai.launcher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.SeekBar;

/** Повзунок із цілим діапазоном [min, max]. */
public class GlassSlider extends SeekBar {

    public interface OnValue {
        void onValue(int value);
    }

    private final int min;
    private Runnable onStart;

    public GlassSlider(Context context, Theme theme, int min, int max, int value, final OnValue listener) {
        super(context);
        this.min = min;
        setMax(max - min);
        setProgress(Math.max(min, Math.min(max, value)) - min);

        ColorStateList accent = ColorStateList.valueOf(theme.accentColor);
        setProgressTintList(accent);
        setThumbTintList(accent);
        setProgressBackgroundTintList(ColorStateList.valueOf(Glass.withAlpha(Color.WHITE, 48)));
        setPadding(Glass.dp(context, 4), 0, Glass.dp(context, 4), 0);

        setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && listener != null) {
                    listener.onValue(progress + GlassSlider.this.min);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (onStart != null) onStart.run();
            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /** Викликається один раз на початку кожного жесту перетягування. */
    public void setOnStart(Runnable r) {
        onStart = r;
    }

    public int getValue() {
        return getProgress() + min;
    }
}
