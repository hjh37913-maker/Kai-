package com.kai.launcher;

import android.graphics.drawable.Drawable;

public final class AppInfo {
    public final String label;
    public final String packageName;
    public final String className;
    public final Drawable icon;
    public final boolean system;

    public AppInfo(String label, String packageName, String className, Drawable icon, boolean system) {
        this.label = label;
        this.packageName = packageName;
        this.className = className;
        this.icon = icon;
        this.system = system;
    }

    public String key() {
        return packageName + "/" + className;
    }
}
