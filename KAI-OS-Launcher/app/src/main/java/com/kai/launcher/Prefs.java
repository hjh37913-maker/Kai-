package com.kai.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public final class Prefs {
    private static final String FILE = "kai_prefs";
    private static final String KEY_DOCK = "dock";
    private static final String KEY_HIDDEN = "hidden";
    private static final String KEY_DEFAULT_ASKED = "default_asked";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static List<String> readList(Context c, String key) {
        String raw = sp(c).getString(key, "");
        List<String> keys = new ArrayList<String>();
        if (raw.length() == 0) return keys;
        for (String k : raw.split(";")) {
            if (k.length() > 0) keys.add(k);
        }
        return keys;
    }

    private static void writeList(Context c, String key, List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append(';');
            sb.append(keys.get(i));
        }
        sp(c).edit().putString(key, sb.toString()).apply();
    }

    public static List<String> getDock(Context c) {
        return readList(c, KEY_DOCK);
    }

    public static void setDock(Context c, List<String> keys) {
        writeList(c, KEY_DOCK, keys);
    }

    public static List<String> getHidden(Context c) {
        return readList(c, KEY_HIDDEN);
    }

    public static void setHidden(Context c, List<String> keys) {
        writeList(c, KEY_HIDDEN, keys);
    }

    public static boolean wasDefaultAsked(Context c) {
        return sp(c).getBoolean(KEY_DEFAULT_ASKED, false);
    }

    public static void setDefaultAsked(Context c) {
        sp(c).edit().putBoolean(KEY_DEFAULT_ASKED, true).apply();
    }
}
