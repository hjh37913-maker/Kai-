package com.kai.launcher;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Простір фокусування: таймер і вибрані застосунки.
 * Він нічого не блокує і не змінює системних обмежень.
 */
public class FocusSpace extends Space {
    private static final int[] PRESETS = { 15, 25, 45 };
    private static final int[] LENGTHS = { 5, 10, 15, 20, 25, 30, 45, 60, 90 };

    private LinearLayout root;
    private TextView timerText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<AppInfo> focusApps = new ArrayList<AppInfo>();

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long end = host.store().getFocusEnd();
            if (end > 0 && end <= System.currentTimeMillis()) {
                host.store().setFocusEnd(0);
                Toast.makeText(ctx, R.string.focus_done, Toast.LENGTH_LONG).show();
                if (root != null) root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                rebuild();
                return;
            }
            refreshText();
            if (host.store().getFocusEnd() > 0) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    public FocusSpace(LauncherHost host) {
        super(host);
    }

    @Override
    public int titleRes() {
        return R.string.space_focus;
    }

    @Override
    public boolean immersive() {
        return true;
    }

    @Override
    protected View createView() {
        root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        rebuild();
        return root;
    }

    private boolean isRunning() {
        return host.store().getFocusEnd() > 0;
    }

    private void rebuild() {
        if (root == null) return;
        root.removeAllViews();
        resolveApps();

        boolean running = isRunning();

        TextView status = new TextView(ctx);
        status.setText(running ? R.string.focus_running : R.string.focus_idle);
        status.setTextColor(Glass.withAlpha(theme.textColor, 200));
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        root.addView(status, wrap(0, 16, 0, 0));

        timerText = new TextView(ctx);
        timerText.setTextColor(theme.textColor);
        timerText.setTextSize(64);
        timerText.setGravity(Gravity.CENTER);
        root.addView(timerText, wrap(0, 4, 0, 8));
        refreshText();

        if (!running) {
            LinearLayout presets = new LinearLayout(ctx);
            presets.setOrientation(LinearLayout.HORIZONTAL);
            presets.setGravity(Gravity.CENTER);
            int current = host.store().getFocusMinutes();
            for (final int m : PRESETS) {
                GlassButton b = new GlassButton(ctx, theme,
                        ctx.getString(R.string.focus_minutes, m), m == current);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        host.store().setFocusMinutes(m);
                        rebuild();
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.leftMargin = dp(4);
                lp.rightMargin = dp(4);
                presets.addView(b, lp);
            }
            root.addView(presets, wrap(0, 0, 0, 8));
        }

        GlassButton startStop = new GlassButton(ctx, theme,
                ctx.getString(running ? R.string.focus_stop : R.string.focus_start), !running);
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(tick);
                if (isRunning()) {
                    host.store().setFocusEnd(0);
                } else {
                    long ms = host.store().getFocusMinutes() * 60000L;
                    host.store().setFocusEnd(System.currentTimeMillis() + ms);
                    handler.postDelayed(tick, 1000);
                }
                rebuild();
            }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(200),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.bottomMargin = dp(12);
        root.addView(startStop, sp);

        if (focusApps.isEmpty()) {
            TextView hint = new TextView(ctx);
            hint.setText(R.string.focus_apps_empty);
            hint.setTextColor(Glass.withAlpha(theme.textColor, 150));
            hint.setGravity(Gravity.CENTER);
            root.addView(hint, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            GridView grid = new GridView(ctx);
            grid.setNumColumns(Math.max(3, Math.min(5, focusApps.size())));
            grid.setVerticalSpacing(dp(12));
            grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
            grid.setAdapter(new AppGridAdapter(ctx, theme, focusApps));
            grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    host.launch(focusApps.get(position));
                }
            });
            root.addView(grid, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }

        LinearLayout bottom = new LinearLayout(ctx);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        GlassButton settings = new GlassButton(ctx, theme, ctx.getString(R.string.focus_settings), false);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showSettings(); }
        });
        GlassButton exit = new GlassButton(ctx, theme, ctx.getString(R.string.focus_exit), false);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { host.switchSpace(SpaceStore.CLASSIC); }
        });
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        l1.rightMargin = dp(4);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        l2.leftMargin = dp(4);
        bottom.addView(settings, l1);
        bottom.addView(exit, l2);
        root.addView(bottom, wrap(0, 8, 0, 0));
    }

    private LinearLayout.LayoutParams wrap(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    private void resolveApps() {
        focusApps.clear();
        for (String key : host.store().getFocusApps()) {
            AppInfo a = host.findByKey(key);
            if (a != null) focusApps.add(a);
        }
    }

    private void refreshText() {
        if (timerText == null) return;
        long end = host.store().getFocusEnd();
        long ms = end > 0
                ? Math.max(0, end - System.currentTimeMillis())
                : host.store().getFocusMinutes() * 60000L;
        long totalSec = (ms + 999) / 1000;
        timerText.setText(String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60));
    }

    @Override
    protected void onAppsChanged() {
        rebuild();
    }

    @Override
    public void onShow() {
        handler.removeCallbacks(tick);
        if (root != null) tick.run();
    }

    @Override
    public void onHide() {
        handler.removeCallbacks(tick);
    }

    @Override
    public void showSettings() {
        CharSequence[] items = {
                ctx.getString(R.string.focus_pick_apps),
                ctx.getString(R.string.focus_length)
        };
        GlassDialog.showItems(ctx, ctx.getString(R.string.space_settings), items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) pickApps(); else pickLength();
                    }
                });
    }

    private void pickApps() {
        final List<AppInfo> all = host.apps();
        if (all.isEmpty()) return;
        CharSequence[] names = new CharSequence[all.size()];
        final boolean[] checked = new boolean[all.size()];
        List<String> current = host.store().getFocusApps();
        for (int i = 0; i < all.size(); i++) {
            names[i] = all.get(i).label;
            checked[i] = current.contains(all.get(i).key());
        }
        GlassDialog.showMultiChoice(ctx, ctx.getString(R.string.focus_pick_apps), names, checked,
                new GlassDialog.MultiResult() {
                    @Override
                    public void onResult(boolean[] result) {
                        List<String> keys = new ArrayList<String>();
                        for (int i = 0; i < all.size(); i++) {
                            if (result[i]) keys.add(all.get(i).key());
                        }
                        host.store().setFocusApps(keys);
                        rebuild();
                    }
                });
    }

    private void pickLength() {
        CharSequence[] items = new CharSequence[LENGTHS.length];
        for (int i = 0; i < LENGTHS.length; i++) {
            items[i] = ctx.getString(R.string.focus_minutes, LENGTHS[i]);
        }
        GlassDialog.showItems(ctx, ctx.getString(R.string.focus_length), items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isRunning()) return; // довжину не змінюємо під час сесії
                        host.store().setFocusMinutes(LENGTHS[which]);
                        rebuild();
                    }
                });
    }
}
