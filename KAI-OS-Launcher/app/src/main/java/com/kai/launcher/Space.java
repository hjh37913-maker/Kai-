package com.kai.launcher;

import android.content.Context;
import android.view.View;

/** Базовий клас простору (Classic, Dashboard, Focus, Freeform). */
public abstract class Space {
    protected final LauncherHost host;
    protected final Context ctx;
    protected final Theme theme;
    private View root;

    protected Space(LauncherHost host) {
        this.host = host;
        this.ctx = host.activity();
        this.theme = host.theme();
    }

    public abstract int titleRes();

    protected abstract View createView();

    public abstract void showSettings();

    /** Ліниве створення: простір, який ще не відкривали, нічого не займає. */
    public final View view() {
        if (root == null) root = createView();
        return root;
    }

    /** Список застосунків змінився. Викликається лише якщо view вже створено. */
    public final void appsChanged() {
        if (root != null) onAppsChanged();
    }

    public void onShow() {}

    public void onHide() {}

    protected void onAppsChanged() {}

    /** Натиснуто «Додому», поки цей простір активний. */
    public void onHome() {}

    /** Повертає true, якщо «Назад» оброблено. */
    public boolean onBack() {
        return false;
    }

    /** true: верхня панель перемикання просторів ховається. */
    public boolean immersive() {
        return false;
    }

    protected int dp(int v) {
        return Glass.dp(ctx, v);
    }
}
