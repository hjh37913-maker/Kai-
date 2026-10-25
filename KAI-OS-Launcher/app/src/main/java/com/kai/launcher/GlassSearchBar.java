package com.kai.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public class GlassSearchBar extends EditText {

    public GlassSearchBar(Context context, Theme theme) {
        super(context);
        setHint(R.string.search_hint);
        setHintTextColor(Glass.withAlpha(theme.textColor, 130));
        setTextColor(theme.textColor);
        setTextSize(15);
        setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        setSingleLine(true);
        setImeOptions(EditorInfo.IME_ACTION_GO);
        int pad = Glass.dp(context, 16);
        setPadding(pad + 4, pad - 2, pad + 4, pad - 2);
        setBackground(Glass.panel(context, theme, Math.max(16, theme.cornerDp)));
        setMinHeight(Glass.dp(context, 48));
    }
}
