package com.kai.launcher;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Модель теми KAI. Зберігається як версійований JSON.
 * Невідомі ключі ігноруються, відсутні беруться зі значень за замовчуванням,
 * тому нові версії можуть додавати поля, не ламаючи старі збережені теми.
 */
public final class Theme {
    public static final int CURRENT_VERSION = 1;

    public static final int STYLE_GLASS = 0;
    public static final int STYLE_SOLID = 1;
    public static final int STYLE_MONO = 2;
    public static final int STYLE_NATIVE = 3;

    public String name = "KAI Clear";
    public int glassAlpha = 18;
    public int strokeAlpha = 28;
    public int cornerDp = 24;
    public int iconSizeDp = 58;
    public int labelSizeSp = 12;
    public int dimAlpha = 40;
    public boolean lightweight = false;
    public int animMs = 200;

    public Theme copy() {
        Theme t = new Theme();
        t.copyFrom(this);
        return t;
    }

    public void copyFrom(Theme o) {
        name = o.name;
        glassAlpha = o.glassAlpha;
        strokeAlpha = o.strokeAlpha;
        cornerDp = o.cornerDp;
        iconSizeDp = o.iconSizeDp;
        iconPaddingPct = o.iconPaddingPct;
        iconStyle = o.iconStyle;
        accentColor = o.accentColor;
        textColor = o.textColor;
        labelSizeSp = o.labelSizeSp;
        dimAlpha = o.dimAlpha;
        lightweight = o.lightweight;
        animMs = o.animMs;
    }

    public void clamp() {
        glassAlpha = clampInt(glassAlpha, 10, 200);
        strokeAlpha = clampInt(strokeAlpha, 0, 255);
        cornerDp = clampInt(cornerDp, 8, 40);
        iconSizeDp = clampInt(iconSizeDp, 40, 84);
        iconPaddingPct = clampInt(iconPaddingPct, 5, 35);
        iconStyle = clampInt(iconStyle, STYLE_GLASS, STYLE_NATIVE);
        labelSizeSp = clampInt(labelSizeSp, 10, 16);
        dimAlpha = clampInt(dimAlpha, 0, 200);
        animMs = clampInt(animMs, 0, 400);
        if (name == null || name.length() == 0) name = "KAI";
    }

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("version", CURRENT_VERSION);
            o.put("name", name);
            o.put("glassAlpha", glassAlpha);
            o.put("strokeAlpha", strokeAlpha);
            o.put("cornerDp", cornerDp);
            o.put("iconSizeDp", iconSizeDp);
            o.put("iconPaddingPct", iconPaddingPct);
            o.put("iconStyle", iconStyle);
            o.put("accentColor", accentColor);
            o.put("textColor", textColor);
            o.put("labelSizeSp", labelSizeSp);
            o.put("dimAlpha", dimAlpha);
            o.put("lightweight", lightweight);
            o.put("animMs", animMs);
            return o.toString();
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Theme fromJson(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        Theme t = new Theme();
        // Схема лишається v1: нові поля лише додаються з розумними значеннями за замовчуванням.
        // Якщо колись знадобиться зміна змісту полів, міграція робитиметься тут за o.optInt("version").
        t.name = o.optString("name", t.name);
        t.glassAlpha = o.optInt("glassAlpha", t.glassAlpha);
        t.strokeAlpha = o.optInt("strokeAlpha", t.strokeAlpha);
        t.cornerDp = o.optInt("cornerDp", t.cornerDp);
        t.iconSizeDp = o.optInt("iconSizeDp", t.iconSizeDp);
        t.iconPaddingPct = o.optInt("iconPaddingPct", t.iconPaddingPct);
        t.iconStyle = o.optInt("iconStyle", t.iconStyle);
        t.accentColor = o.optInt("accentColor", t.accentColor);
        t.textColor = o.optInt("textColor", t.textColor);
        t.labelSizeSp = o.optInt("labelSizeSp", t.labelSizeSp);
        t.dimAlpha = o.optInt("dimAlpha", t.dimAlpha);
        t.lightweight = o.optBoolean("lightweight", t.lightweight);
        t.animMs = o.optInt("animMs", t.animMs);
        t.clamp();
        return t;
    }

    private static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
