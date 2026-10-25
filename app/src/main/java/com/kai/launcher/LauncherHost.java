package com.kai.launcher;

import android.app.Activity;

import java.util.List;

/** Те, що простори можуть просити в головної активності. */
public interface LauncherHost {
    Activity activity();
    Theme theme();
    SpaceStore store();
    /** Видимі застосунки (без прихованих). */
    List<AppInfo> apps();
    /** Усі встановлені застосунки, включно з прихованими. */
    List<AppInfo> allApps();
    AppInfo findByKey(String key);
    void launch(AppInfo app);
    void showAppMenu(AppInfo app);
    void switchSpace(int id);
    void showHiddenApps();
    void showAbout();
    boolean isDefaultLauncher();
    void requestDefaultLauncher();
}
