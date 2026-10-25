package com.kai.launcher;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LauncherActivity extends Activity implements LauncherHost {

    private static final int DOCK_MAX = 5;
    private static final int REQ_ROLE_HOME = 1001;
    private static final long RELOAD_DEBOUNCE_MS = 400;

    private static final int A_PIN = 0;
    private static final int A_INFO = 1;
    private static final int A_HIDE = 2;
    private static final int A_UNINSTALL = 3;

    /** Усі встановлені застосунки. */
    private final List<AppInfo> installed = new ArrayList<AppInfo>();
    /** Те, що бачить користувач: без прихованих. */
    private final List<AppInfo> visible = new ArrayList<AppInfo>();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private SpaceStore store;
    private Theme theme;
    private String themeJson;

    private Space[] spaces;
    private int current;
    private boolean animating;
    private boolean resumed;
    private boolean loadedOnce;

    private LinearLayout chrome;
    private FrameLayout content;
    private BroadcastReceiver pkgReceiver;
    private Object backToken;

    private final Runnable reloadTask = new Runnable() {
        @Override
        public void run() {
            reload();
        }
    };

    // ---------- LauncherHost ----------

    @Override public Activity activity() { return this; }
    @Override public Theme theme() { return theme; }
    @Override public SpaceStore store() { return store; }
    @Override public List<AppInfo> apps() { return visible; }
    @Override public List<AppInfo> allApps() { return installed; }

    @Override
    public AppInfo findByKey(String key) {
        for (AppInfo a : visible) {
            if (a.key().equals(key)) return a;
        }
        return null;
    }

    @Override
    public void launch(AppInfo app) {
        Compat.hideKeyboard(getCurrentFocus());
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(new ComponentName(app.packageName, app.className));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
            scheduleReload(); // застосунок міг бути видалений
        }
    }

    @Override
    public void showAppMenu(final AppInfo app) {
        final List<String> dockKeys = Prefs.getDock(this);
        final boolean pinned = dockKeys.contains(app.key());

        final List<Integer> actions = new ArrayList<Integer>();
        List<CharSequence> names = new ArrayList<CharSequence>();
        actions.add(A_PIN);
        names.add(getString(pinned ? R.string.unpin : R.string.pin));
        actions.add(A_INFO);
        names.add(getString(R.string.app_info));
        actions.add(A_HIDE);
        names.add(getString(R.string.menu_hide));
        if (!app.system) {
            actions.add(A_UNINSTALL);
            names.add(getString(R.string.menu_uninstall));
        }

        GlassDialog.showItems(this, app.label, names.toArray(new CharSequence[0]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (actions.get(which)) {
                            case A_PIN:
                                togglePin(app, dockKeys, pinned);
                                break;
                            case A_INFO:
                                openSystem(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + app.packageName)));
                                break;
                            case A_HIDE:
                                hideApp(app);
                                break;
                            default:
                                openSystem(new Intent(Intent.ACTION_DELETE,
                                        Uri.parse("package:" + app.packageName)));
                                break;
                        }
                    }
                });
    }

    private void togglePin(AppInfo app, List<String> dockKeys, boolean pinned) {
        if (pinned) {
            dockKeys.remove(app.key());
        } else {
            if (dockKeys.size() >= DOCK_MAX) {
                Toast.makeText(this, R.string.dock_full, Toast.LENGTH_SHORT).show();
                return;
            }
            dockKeys.add(app.key());
        }
        Prefs.setDock(this, dockKeys);
        spaces[SpaceStore.CLASSIC].appsChanged();
    }

    private void hideApp(AppInfo app) {
        List<String> hidden = Prefs.getHidden(this);
        if (!hidden.contains(app.key())) hidden.add(app.key());
        Prefs.setHidden(this, hidden);
        applyVisibility();
        Toast.makeText(this, getString(R.string.app_hidden, app.label), Toast.LENGTH_LONG).show();
    }

    private void openSystem(Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showHiddenApps() {
        if (installed.isEmpty()) return;
        final List<AppInfo> all = new ArrayList<AppInfo>(installed);
        CharSequence[] names = new CharSequence[all.size()];
        final boolean[] checked = new boolean[all.size()];
        List<String> hidden = Prefs.getHidden(this);
        for (int i = 0; i < all.size(); i++) {
            names[i] = all.get(i).label;
            checked[i] = hidden.contains(all.get(i).key());
        }
        GlassDialog.showMultiChoice(this, getString(R.string.hidden_apps), names, checked,
                new GlassDialog.MultiResult() {
                    @Override
                    public void onResult(boolean[] result) {
                        List<String> keys = new ArrayList<String>();
                        for (int i = 0; i < all.size(); i++) {
                            if (result[i]) keys.add(all.get(i).key());
                        }
                        Prefs.setHidden(LauncherActivity.this, keys);
                        applyVisibility();
                    }
                });
    }

    @Override
    @SuppressWarnings("deprecation")
    public void showAbout() {
        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            // власний пакет завжди є
        }
        GlassDialog.message(this, getString(R.string.app_name),
                getString(R.string.about_version, version) + "\n\n" + getString(R.string.about_body));
    }

    @Override
    public boolean isDefaultLauncher() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null
                && getPackageName().equals(ri.activityInfo.packageName);
    }

    @Override
    public void requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = (RoleManager) getSystemService(Context.ROLE_SERVICE);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                    Toast.makeText(this, R.string.already_default, Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQ_ROLE_HOME);
                    return;
                } catch (Exception ignored) {
                    // переходимо до системних налаштувань
                }
            }
        }
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception e) {
            openSystem(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    @Override
    public void switchSpace(final int id) {
        if (id == current || animating) return;
        final Space old = spaces[current];
        final Space next = spaces[id];
        final int dir = id > current ? 1 : -1;

        old.onHide();
        current = id;
        store.setCurrent(id);

        if (theme.lightweight || theme.animMs <= 0) {
            showSpace(next);
            next.onShow();
            return;
        }

        animating = true;
        final View oldView = old.view();
        int duration = Math.max(180, theme.animMs + 40);
        OvershootInterpolator spring = new OvershootInterpolator(0.6f);
        DecelerateInterpolator exit = new DecelerateInterpolator(1.4f);

        oldView.animate()
                .alpha(0f)
                .scaleX(0.94f)
                .scaleY(0.94f)
                .translationX(-dir * dp(16))
                .setDuration(duration * 2 / 3)
                .setInterpolator(exit)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        oldView.setAlpha(1f);
                        oldView.setScaleX(1f);
                        oldView.setScaleY(1f);
                        oldView.setTranslationX(0f);
                        showSpace(next);
                        View nv = next.view();
                        nv.setAlpha(0f);
                        nv.setScaleX(0.96f);
                        nv.setScaleY(0.96f);
                        nv.setTranslationX(dir * dp(16));
                        nv.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .translationX(0f)
                                .setDuration(duration)
                                .setInterpolator(spring)
                                .withEndAction(new Runnable() {
                                    @Override public void run() { animating = false; }
                                });
                        next.onShow();
                    }
                });
    }

    // ---------- Життєвий цикл ----------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Compat.edgeToEdge(this);
        store = new SpaceStore(this);
        theme = ThemeStore.load(this);
        themeJson = theme.toJson();
        current = store.getCurrent();
        buildUi();
        backToken = Compat.registerBack(this, new Compat.BackHandler() {
            @Override public void onBack() { handleBack(); }
        });
        registerPackageReceiver();
        reload();
    }

    private void registerPackageReceiver() {
        pkgReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                scheduleReload();
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addDataScheme("package");
        Compat.registerSystemReceiver(this, pkgReceiver, f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        Theme latest = ThemeStore.load(this);
        String json = latest.toJson();
        if (!json.equals(themeJson)) {
            theme = latest;
            themeJson = json;
            animating = false;
            buildUi();
        }
        spaces[current].onShow();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        spaces[current].onHide();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pkgReceiver != null) {
            try {
                unregisterReceiver(pkgReceiver);
            } catch (IllegalArgumentException ignored) {
                // вже знято
            }
            pkgReceiver = null;
        }
        Compat.unregisterBack(this, backToken);
        ui.removeCallbacksAndMessages(null);
        io.shutdownNow();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        animating = false;
        buildUi(); // колонки, макет Вільного простору й рядки залежать від конфігурації
        if (resumed) spaces[current].onShow();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        spaces[current].onHome();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Лаунчер не закривається кнопкою «Назад» (Android 12 і старіші).
        handleBack();
    }

    private void handleBack() {
        spaces[current].onBack();
    }

    // ---------- UI ----------

    private int dp(int v) {
        return Glass.dp(this, v);
    }

    private void buildUi() {
        if (spaces != null) {
            spaces[current].onHide();
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Glass.withAlpha(Color.BLACK, theme.dimAlpha));
        Compat.applyInsets(root, dp(12));

        spaces = new Space[] {
                new ClassicSpace(this),
                new DashboardSpace(this),
                new FocusSpace(this),
                new FreeformSpace(this)
        };

        chrome = new LinearLayout(this);
        chrome.setOrientation(LinearLayout.HORIZONTAL);
        chrome.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(8);
        root.addView(chrome, cp);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        showSpace(spaces[current]);
        if (loadedOnce) {
            for (Space s : spaces) s.appsChanged();
        }
    }

    private void showSpace(Space s) {
        content.removeAllViews();
        content.addView(s.view(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        chrome.setVisibility(s.immersive() ? View.GONE : View.VISIBLE);
        buildChrome();
    }

    private void buildChrome() {
        chrome.removeAllViews();

        for (int i = 0; i < spaces.length; i++) {
            final int id = i;
            GlassButton b = new GlassButton(this, theme, getString(spaces[i].titleRes()), i == current);
            b.setTextSize(13);
            b.setPadding(dp(6), dp(10), dp(6), dp(10));
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { switchSpace(id); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.rightMargin = dp(6);
            chrome.addView(b, lp);
        }

        GlassButton more = new GlassButton(this, theme, "⋯", false);
        more.setContentDescription(getString(R.string.cd_more));
        more.setPadding(dp(12), dp(8), dp(12), dp(8));
        more.setMinWidth(dp(48));
        more.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { spaces[current].showSettings(); }
        });
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mp.rightMargin = dp(4);
        chrome.addView(more, mp);

        GlassButton gear = new GlassButton(this, theme, "⚙", false);
        gear.setContentDescription(getString(R.string.studio_title));
        gear.setPadding(dp(12), dp(8), dp(12), dp(8));
        gear.setMinWidth(dp(48));
        gear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LauncherActivity.this, StudioActivity.class));
            }
        });
        chrome.addView(gear, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // ---------- Дані ----------

    private void scheduleReload() {
        ui.removeCallbacks(reloadTask);
        ui.postDelayed(reloadTask, RELOAD_DEBOUNCE_MS);
    }

    private void reload() {
        if (io.isShutdown()) return;
        io.execute(new Runnable() {
            @Override
            public void run() {
                final List<AppInfo> loaded = AppRepository.load(LauncherActivity.this);
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isDestroyed()) return;
                        installed.clear();
                        installed.addAll(loaded);
                        loadedOnce = true;
                        applyVisibility();
                        maybeAskDefaultLauncher();
                    }
                });
            }
        });
    }

    /** Перераховує список видимих застосунків і оновлює всі простори. */
    private void applyVisibility() {
        List<String> hidden = Prefs.getHidden(this);
        visible.clear();
        for (AppInfo a : installed) {
            if (!hidden.contains(a.key())) visible.add(a);
        }
        for (Space s : spaces) s.appsChanged();
    }

    /** Один раз після першого запуску пропонуємо зробити KAI головним екраном. */
    private void maybeAskDefaultLauncher() {
        if (Prefs.wasDefaultAsked(this) || isFinishing() || isDestroyed()) return;
        Prefs.setDefaultAsked(this);
        if (isDefaultLauncher()) return;
        GlassDialog.confirm(this, getString(R.string.default_prompt_title),
                getString(R.string.default_prompt_body),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestDefaultLauncher();
                    }
                });
    }
}
