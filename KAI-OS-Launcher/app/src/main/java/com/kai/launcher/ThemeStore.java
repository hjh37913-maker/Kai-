package com.kai.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;

public final class ThemeStore {
    private static final String FILE = "kai_theme";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_PREVIOUS = "previous";

    private ThemeStore() {}

    public static Theme defaultTheme() {
        return new Theme();
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static Theme parseOrNull(String raw) {
        if (raw == null) return null;
        try {
            return Theme.fromJson(raw);
        } catch (JSONException e) {
            return null;
        }
    }

    /** Завантажує активну тему; при відсутності або пошкодженні повертає тему за замовчуванням. */
    public static Theme load(Context c) {
        Theme t = parseOrNull(sp(c).getString(KEY_ACTIVE, null));
        return t != null ? t : defaultTheme();
    }

    /** Застосовує тему, зберігаючи попередню активну для відкоту. */
    public static void applyWithBackup(Context c, Theme t) {
        t.clamp();
        String old = load(c).toJson();
        sp(c).edit().putString(KEY_PREVIOUS, old).putString(KEY_ACTIVE, t.toJson()).apply();
    }

    public static boolean hasPrevious(Context c) {
        return parseOrNull(sp(c).getString(KEY_PREVIOUS, null)) != null;
    }

    /**
     * Міняє місцями активну й попередню теми. Повертає нову активну тему
     * або null, якщо відкочуватися нікуди. Повторний виклик відкотить відкат.
     */
    public static Theme swapWithPrevious(Context c) {
        Theme prev = parseOrNull(sp(c).getString(KEY_PREVIOUS, null));
        if (prev == null) return null;
        String current = load(c).toJson();
        sp(c).edit().putString(KEY_ACTIVE, prev.toJson()).putString(KEY_PREVIOUS, current).apply();
        return prev;
    }

    public static Theme[] presets() {
        Theme clear = defaultTheme();
        clear.name = "KAI Clear";
        clear.iconStyle = Theme.STYLE_NATIVE;
        clear.accentColor = 0xFF0A84FF;
        clear.glassAlpha = 18;
        clear.strokeAlpha = 28;
        clear.cornerDp = 22;
        clear.dimAlpha = 40;
        clear.animMs = 200;

        Theme graphite = new Theme();
        graphite.name = "KAI Graphite";
        graphite.iconStyle = Theme.STYLE_NATIVE;
        graphite.accentColor = 0xFF94A3B8;
        graphite.glassAlpha = 24;
        graphite.strokeAlpha = 36;
        graphite.cornerDp = 20;
        graphite.dimAlpha = 70;

        Theme deepBlue = new Theme();
        deepBlue.name = "KAI Deep Blue";
        deepBlue.iconStyle = Theme.STYLE_NATIVE;
        deepBlue.accentColor = 0xFF60A5FA;
        deepBlue.glassAlpha = 30;
        deepBlue.strokeAlpha = 52;
        deepBlue.cornerDp = 20;
        deepBlue.dimAlpha = 85;
        deepBlue.animMs = 180;

        Theme glass = new Theme();
        glass.name = "KAI Glass";
        glass.iconStyle = Theme.STYLE_GLASS;
        glass.accentColor = 0xFFA5B4FC;
        glass.glassAlpha = 42;
        glass.strokeAlpha = 64;
        glass.cornerDp = 24;
        glass.dimAlpha = 60;
        glass.animMs = 200;

        Theme minimal = new Theme();
        minimal.name = "KAI Minimal";
        minimal.iconStyle = Theme.STYLE_NATIVE;
        minimal.accentColor = 0xFFE5E7EB;
        minimal.glassAlpha = 16;
        minimal.strokeAlpha = 24;
        minimal.cornerDp = 14;
        minimal.dimAlpha = 30;
        minimal.lightweight = true;
        minimal.animMs = 100;
        minimal.labelSizeSp = 11;

        Theme studio = new Theme();
        studio.name = "KAI Studio";
        studio.iconStyle = Theme.STYLE_SOLID;
        studio.accentColor = 0xFF818CF8;
        studio.glassAlpha = 36;
        studio.strokeAlpha = 56;
        studio.cornerDp = 22;
        studio.dimAlpha = 95;
        studio.iconSizeDp = 54;
        studio.animMs = 170;

        return new Theme[] { clear, graphite, deepBlue, glass, minimal, studio };
    }
}
