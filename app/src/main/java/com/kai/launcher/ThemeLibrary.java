package com.kai.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Збережені користувацькі теми та автозбережена чернетка KAI Studio. */
public final class ThemeLibrary {
    private static final String FILE = "kai_library";
    private static final String KEY_THEMES = "themes";
    private static final String KEY_DRAFT = "draft";

    private ThemeLibrary() {}

    public static final class Entry {
        public final String name;
        public final Theme theme;

        Entry(String name, Theme theme) {
            this.name = name;
            this.theme = theme;
        }
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static List<Entry> list(Context c) {
        List<Entry> result = new ArrayList<Entry>();
        String raw = sp(c).getString(KEY_THEMES, null);
        if (raw == null) return result;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String name = o.getString("name");
                Theme t = Theme.fromJson(o.getJSONObject("theme").toString());
                result.add(new Entry(name, t));
            }
        } catch (JSONException e) {
            // Пошкоджений запис: повертаємо те, що встигли прочитати.
        }
        return result;
    }

    private static void write(Context c, List<Entry> entries) {
        try {
            JSONArray arr = new JSONArray();
            for (Entry e : entries) {
                JSONObject o = new JSONObject();
                o.put("name", e.name);
                o.put("theme", new JSONObject(e.theme.toJson()));
                arr.put(o);
            }
            sp(c).edit().putString(KEY_THEMES, arr.toString()).apply();
        } catch (JSONException e) {
            // Запис з готових JSON-рядків не може бути невалідним.
        }
    }

    /** Зберігає тему під назвою; тема з такою самою назвою замінюється. */
    public static void save(Context c, String name, Theme theme) {
        Theme copy = theme.copy();
        copy.name = name;
        List<Entry> all = list(c);
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).name.equals(name)) {
                all.set(i, new Entry(name, copy));
                replaced = true;
                break;
            }
        }
        if (!replaced) all.add(new Entry(name, copy));
        write(c, all);
    }

    public static void delete(Context c, String name) {
        List<Entry> all = list(c);
        List<Entry> kept = new ArrayList<Entry>();
        for (Entry e : all) {
            if (!e.name.equals(name)) kept.add(e);
        }
        write(c, kept);
    }

    public static Theme loadDraft(Context c) {
        String raw = sp(c).getString(KEY_DRAFT, null);
        if (raw == null) return null;
        try {
            return Theme.fromJson(raw);
        } catch (JSONException e) {
            return null;
        }
    }

    public static void saveDraft(Context c, Theme t) {
        sp(c).edit().putString(KEY_DRAFT, t.toJson()).apply();
    }

    public static void clearDraft(Context c) {
        sp(c).edit().remove(KEY_DRAFT).apply();
    }
}
