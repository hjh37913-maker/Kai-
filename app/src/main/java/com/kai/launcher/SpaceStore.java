package com.kai.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Збереження вибраного простору та окремих налаштувань кожного простору. */
public final class SpaceStore {
    public static final int CLASSIC = 0;
    public static final int DASHBOARD = 1;
    public static final int FOCUS = 2;
    public static final int FREEFORM = 3;

    public static final int CARD_CLOCK = 1;
    public static final int CARD_BATTERY = 2;
    public static final int CARD_STORAGE = 4;
    public static final int CARD_MEMORY = 8;
    public static final int CARDS_ALL = 15;

    private static final String FILE = "kai_spaces";

    private final SharedPreferences sp;

    public SpaceStore(Context c) {
        sp = c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public int getCurrent() {
        int v = sp.getInt("current", CLASSIC);
        return (v < CLASSIC || v > FREEFORM) ? CLASSIC : v;
    }

    public void setCurrent(int id) {
        sp.edit().putInt("current", id).apply();
    }

    /** 0 = автоматично. */
    public int getClassicColumns() {
        return sp.getInt("classic_columns", 0);
    }

    public void setClassicColumns(int n) {
        sp.edit().putInt("classic_columns", n).apply();
    }

    public int getDashCards() {
        return sp.getInt("dash_cards", CARDS_ALL);
    }

    public void setDashCards(int mask) {
        sp.edit().putInt("dash_cards", mask).apply();
    }

    public List<String> getFocusApps() {
        String raw = sp.getString("focus_apps", "");
        List<String> keys = new ArrayList<String>();
        if (raw.length() == 0) return keys;
        for (String k : raw.split(";")) {
            if (k.length() > 0) keys.add(k);
        }
        return keys;
    }

    public void setFocusApps(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append(';');
            sb.append(keys.get(i));
        }
        sp.edit().putString("focus_apps", sb.toString()).apply();
    }

    public int getFocusMinutes() {
        return sp.getInt("focus_minutes", 25);
    }

    public void setFocusMinutes(int m) {
        sp.edit().putInt("focus_minutes", m).apply();
    }

    /** Момент завершення сесії (мс, epoch) або 0, якщо таймер не запущено. */
    public long getFocusEnd() {
        return sp.getLong("focus_end", 0L);
    }

    public void setFocusEnd(long t) {
        sp.edit().putLong("focus_end", t).apply();
    }

    public String getFreeform() {
        return sp.getString("freeform", "{}");
    }

    public void setFreeform(String json) {
        sp.edit().putString("freeform", json).apply();
    }
}
