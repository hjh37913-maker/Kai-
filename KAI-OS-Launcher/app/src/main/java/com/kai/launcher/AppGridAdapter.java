package com.kai.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.animation.OvershootInterpolator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Спільний адаптер сітки: кругла іконка + підпис. Список передається за посиланням. */
public class AppGridAdapter extends BaseAdapter {
    private final Context ctx;
    private final Theme theme;
    private final List<AppInfo> items;

    public AppGridAdapter(Context ctx, Theme theme, List<AppInfo> items) {
        this.ctx = ctx;
        this.theme = theme;
        this.items = items;
    }

    @Override public int getCount() { return items.size(); }
    @Override public Object getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            LinearLayout cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            v.animate().scaleX(0.90f).scaleY(0.90f)
                                    .setDuration(90)
                                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                    .start();
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            v.animate().scaleX(1f).scaleY(1f)
                                    .setDuration(280)
                                    .setInterpolator(new OvershootInterpolator(1.4f))
                                    .start();
                            break;
                    }
                    return false;
                }
            });
            cell.setGravity(Gravity.CENTER_HORIZONTAL);

            RoundIconView icon = new RoundIconView(ctx, theme);
            int size = Glass.dp(ctx, theme.iconSizeDp);
            cell.addView(icon, new LinearLayout.LayoutParams(size, size));

            TextView label = new TextView(ctx);
            label.setTextColor(theme.textColor);
            label.setTextSize(theme.labelSizeSp);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setGravity(Gravity.CENTER);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            label.setShadowLayer(0f, 0f, 0f, 0);
            label.setPadding(Glass.dp(ctx, 2), 0, Glass.dp(ctx, 2), 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Glass.dp(ctx, 6);
            cell.addView(label, lp);

            h = new Holder();
            h.icon = icon;
            h.label = label;
            cell.setTag(h);
            convertView = cell;
        } else {
            h = (Holder) convertView.getTag();
        }

        AppInfo app = items.get(position);
        h.icon.setIcon(app.icon);
        h.label.setText(app.label);
        return convertView;
    }

    private static final class Holder {
        RoundIconView icon;
        TextView label;
    }
}
