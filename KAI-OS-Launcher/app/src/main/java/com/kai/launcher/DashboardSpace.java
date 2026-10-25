package com.kai.launcher;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Locale;

/** Огляд: лише реальні дані пристрою (час, батарея, сховище, памʼять). */
public class DashboardSpace extends Space {
    private LinearLayout list;
    private TextView batteryText;
    private ProgressBar batteryBar;
    private TextView storageText;
    private ProgressBar storageBar;
    private TextView memoryText;
    private ProgressBar memoryBar;
    private BroadcastReceiver batteryReceiver;

    public DashboardSpace(LauncherHost host) {
        super(host);
    }

    @Override
    public int titleRes() {
        return R.string.space_dashboard;
    }

    @Override
    protected View createView() {
        ScrollView sv = new ScrollView(ctx);
        list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rebuild();
        return sv;
    }

    private void rebuild() {
        list.removeAllViews();
        batteryText = null;
        batteryBar = null;
        storageText = null;
        storageBar = null;
        memoryText = null;
        memoryBar = null;

        int cards = host.store().getDashCards();

        if ((cards & SpaceStore.CARD_CLOCK) != 0) {
            GlassCard c = new GlassCard(ctx, theme);
            TextClock time = new TextClock(ctx);
            // Формат за замовчуванням сам слідує системному 12/24-годинному режиму.
            time.setTextColor(theme.textColor);
            time.setTextSize(52);
            time.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
            c.addView(time);
            TextClock date = new TextClock(ctx);
            String datePattern = DateFormat.getBestDateTimePattern(
                    Locale.getDefault(), "EEEEdMMMM");
            date.setFormat24Hour(datePattern);
            date.setFormat12Hour(datePattern);
            date.setTextColor(Glass.withAlpha(theme.textColor, 180));
            date.setTextSize(15);
            c.addView(date);
            addCard(c);
        }

        if ((cards & SpaceStore.CARD_BATTERY) != 0) {
            GlassCard c = titledCard(R.string.battery_title);
            batteryText = value();
            batteryBar = bar();
            c.addView(batteryText);
            c.addView(batteryBar, barParams());
            addCard(c);
        }

        if ((cards & SpaceStore.CARD_STORAGE) != 0) {
            GlassCard c = titledCard(R.string.storage_title);
            storageText = value();
            storageBar = bar();
            c.addView(storageText);
            c.addView(storageBar, barParams());
            addCard(c);
        }

        if ((cards & SpaceStore.CARD_MEMORY) != 0) {
            GlassCard c = titledCard(R.string.memory_title);
            memoryText = value();
            memoryBar = bar();
            c.addView(memoryText);
            c.addView(memoryBar, barParams());
            addCard(c);
        }

        refreshDevice();
    }

    private void addCard(GlassCard c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(16);
        list.addView(c, lp);
    }

    private GlassCard titledCard(int titleRes) {
        GlassCard c = new GlassCard(ctx, theme);
        TextView t = new TextView(ctx);
        t.setText(titleRes);
        t.setTextColor(Glass.withAlpha(theme.textColor, 200));
        t.setTextSize(13);
        c.addView(t);
        return c;
    }

    private TextView value() {
        TextView t = new TextView(ctx);
        t.setTextColor(theme.textColor);
        t.setTextSize(20);
        return t;
    }

    private ProgressBar bar() {
        ProgressBar b = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        b.setMax(100);
        b.setProgressTintList(ColorStateList.valueOf(theme.accentColor));
        b.setProgressBackgroundTintList(ColorStateList.valueOf(Glass.withAlpha(Color.WHITE, 70)));
        return b;
    }

    private LinearLayout.LayoutParams barParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        return lp;
    }

    /** Сховище і памʼять оновлюємо при показі; жодних постійних фонових завдань. */
    private void refreshDevice() {
        if (storageText != null) {
            try {
                StatFs st = new StatFs(Environment.getDataDirectory().getPath());
                long total = st.getTotalBytes();
                long free = st.getAvailableBytes();
                long used = total - free;
                storageText.setText(ctx.getString(R.string.storage_used,
                        Formatter.formatFileSize(ctx, used), Formatter.formatFileSize(ctx, total)));
                storageBar.setProgress(total > 0 ? (int) (used * 100L / total) : 0);
            } catch (Exception e) {
                storageText.setText(R.string.unavailable);
            }
        }

        if (memoryText != null) {
            try {
                ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                memoryText.setText(ctx.getString(R.string.memory_free,
                        Formatter.formatFileSize(ctx, mi.availMem),
                        Formatter.formatFileSize(ctx, mi.totalMem)));
                long usedMem = mi.totalMem - mi.availMem;
                memoryBar.setProgress(mi.totalMem > 0 ? (int) (usedMem * 100L / mi.totalMem) : 0);
            } catch (Exception e) {
                memoryText.setText(R.string.unavailable);
            }
        }
    }

    private void updateBattery(Intent i) {
        if (batteryText == null || i == null) return;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            batteryText.setText(R.string.unavailable);
            return;
        }
        int pct = level * 100 / scale;
        int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int statusRes;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            statusRes = R.string.battery_charging;
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            statusRes = R.string.battery_full;
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING
                || status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            statusRes = R.string.battery_discharging;
        } else {
            statusRes = R.string.battery_unknown;
        }
        String text = pct + "% \u00B7 " + ctx.getString(statusRes);
        int temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
        if (temp != Integer.MIN_VALUE && temp > 0) {
            text += " \u00B7 " + String.format(Locale.getDefault(), "%.1f \u00B0C", temp / 10f);
        }
        batteryText.setText(text);
        batteryBar.setProgress(pct);
    }

    @Override
    public void onShow() {
        refreshDevice();
        if (batteryText != null && batteryReceiver == null) {
            batteryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateBattery(intent);
                }
            };
            Compat.registerSystemReceiver(ctx, batteryReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    }

    @Override
    public void onHide() {
        if (batteryReceiver != null) {
            try {
                ctx.unregisterReceiver(batteryReceiver);
            } catch (IllegalArgumentException ignored) {
                // вже знято
            }
            batteryReceiver = null;
        }
    }

    @Override
    public void showSettings() {
        final int[] bits = {
                SpaceStore.CARD_CLOCK, SpaceStore.CARD_BATTERY,
                SpaceStore.CARD_STORAGE, SpaceStore.CARD_MEMORY };
        CharSequence[] names = {
                ctx.getString(R.string.dash_clock), ctx.getString(R.string.dash_battery),
                ctx.getString(R.string.dash_storage), ctx.getString(R.string.dash_memory) };
        int mask = host.store().getDashCards();
        final boolean[] checked = new boolean[bits.length];
        for (int i = 0; i < bits.length; i++) checked[i] = (mask & bits[i]) != 0;

        GlassDialog.showMultiChoice(ctx, ctx.getString(R.string.dash_cards), names, checked,
                new GlassDialog.MultiResult() {
                    @Override
                    public void onResult(boolean[] result) {
                        int m = 0;
                        for (int i = 0; i < bits.length; i++) {
                            if (result[i]) m |= bits[i];
                        }
                        host.store().setDashCards(m);
                        onHide();
                        rebuild();
                        onShow();
                    }
                });
    }
}
