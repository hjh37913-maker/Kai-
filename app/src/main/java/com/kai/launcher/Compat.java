package com.kai.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.view.inputmethod.InputMethodManager;

/** Невеликі обгортки для відмінностей між версіями Android (minSdk 24 … targetSdk 36). */
final class Compat {

    interface BackHandler {
        void onBack();
    }

    private Compat() {}

    /** Малює під системними панелями; відступи потім виставляє {@link #applyInsets}. */
    @SuppressWarnings("deprecation")
    static void edgeToEdge(Activity a) {
        Window w = a.getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            w.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    /** Відступи кореневого вʼю = системні панелі + вирізи екрана + {@code padPx}. */
    @SuppressWarnings("deprecation")
    static void applyInsets(View root, final int padPx) {
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets in) {
                int l;
                int t;
                int r;
                int b;
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets i = in.getInsets(
                            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    l = i.left;
                    t = i.top;
                    r = i.right;
                    b = i.bottom;
                } else {
                    l = in.getSystemWindowInsetLeft();
                    t = in.getSystemWindowInsetTop();
                    r = in.getSystemWindowInsetRight();
                    b = in.getSystemWindowInsetBottom();
                }
                v.setPadding(l + padPx, t + padPx, r + padPx, b + padPx);
                return in;
            }
        });
        root.requestApplyInsets();
    }

    /** Реєструє приймач системних сповіщень (Android 14+ вимагає вказати прапорець). */
    static void registerSystemReceiver(Context c, BroadcastReceiver r, IntentFilter f) {
        if (Build.VERSION.SDK_INT >= 33) {
            c.registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            c.registerReceiver(r, f);
        }
    }

    /**
     * «Назад» для Android 13+ (predictive back). На старіших версіях активність
     * сама викликає свій обробник зі старого onBackPressed().
     */
    static Object registerBack(Activity a, final BackHandler h) {
        if (Build.VERSION.SDK_INT >= 33) {
            OnBackInvokedCallback cb = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    h.onBack();
                }
            };
            a.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb);
            return cb;
        }
        return null;
    }

    static void unregisterBack(Activity a, Object token) {
        if (Build.VERSION.SDK_INT >= 33 && token instanceof OnBackInvokedCallback) {
            a.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((OnBackInvokedCallback) token);
        }
    }

    static void hideKeyboard(View v) {
        if (v == null) return;
        InputMethodManager imm = (InputMethodManager) v.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
