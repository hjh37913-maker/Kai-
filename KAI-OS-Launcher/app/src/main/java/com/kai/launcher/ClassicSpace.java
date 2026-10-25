package com.kai.launcher;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Класичний робочий стіл: пошук, сітка застосунків, док. */
public class ClassicSpace extends Space {
    private static final int[] COLUMN_VALUES = { 0, 3, 4, 5, 6, 7, 8 };

    private final List<AppInfo> shown = new ArrayList<AppInfo>();
    private AppGridAdapter adapter;
    private GridView grid;
    private LinearLayout dock;
    private EditText search;
    private TextView empty;

    public ClassicSpace(LauncherHost host) {
        super(host);
    }

    @Override
    public int titleRes() {
        return R.string.space_classic;
    }

    @Override
    protected View createView() {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);

        search = new GlassSearchBar(ctx, theme);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { applyFilter(); }
        });
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (!shown.isEmpty()) host.launch(shown.get(0));
                return true;
            }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.bottomMargin = dp(12);
        root.addView(search, sp);

        adapter = new AppGridAdapter(ctx, theme, shown);
        grid = new GridView(ctx);
        grid.setNumColumns(columns());
        grid.setVerticalSpacing(dp(16));
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < shown.size()) host.launch(shown.get(position));
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= shown.size()) return false;
                host.showAppMenu(shown.get(position));
                return true;
            }
        });

        FrameLayout gridFrame = new FrameLayout(ctx);
        gridFrame.addView(grid, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        empty = new TextView(ctx);
        empty.setText(R.string.no_results);
        empty.setTextColor(Glass.withAlpha(theme.textColor, 170));
        empty.setTextSize(15);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        gridFrame.addView(empty, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(gridFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dock = new LinearLayout(ctx);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(16), dp(10), dp(16), dp(10));
        dock.setBackground(Glass.panel(ctx, theme, theme.cornerDp + 4));
        LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(theme.iconSizeDp + 24));
        dp1.topMargin = dp(8);
        root.addView(dock, dp1);

        applyFilter();
        refreshDock();
        return root;
    }

    private int columns() {
        int cfg = host.store().getClassicColumns();
        if (cfg > 0) return cfg;
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        int widthDp = (int) (dm.widthPixels / dm.density);
        return Math.max(3, widthDp / (theme.iconSizeDp + 28));
    }

    /** Спочатку назви, що починаються із запиту (або зі слова в назві), потім ті, що містять його. */
    private void applyFilter() {
        Locale loc = Locale.getDefault();
        String q = search.getText().toString().trim().toLowerCase(loc);
        shown.clear();
        if (q.length() == 0) {
            shown.addAll(host.apps());
        } else {
            List<AppInfo> rest = new ArrayList<AppInfo>();
            for (AppInfo a : host.apps()) {
                String l = a.label.toLowerCase(loc);
                if (l.startsWith(q) || l.contains(" " + q)) {
                    shown.add(a);
                } else if (l.contains(q)) {
                    rest.add(a);
                }
            }
            shown.addAll(rest);
        }
        adapter.notifyDataSetChanged();
        empty.setVisibility(shown.isEmpty() && !host.apps().isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void refreshDock() {
        dock.removeAllViews();
        List<String> keys = Prefs.getDock(ctx);
        List<String> valid = new ArrayList<String>();

        for (String key : keys) {
            final AppInfo app = host.findByKey(key);
            if (app == null) continue;
            valid.add(key);

            RoundIconView icon = new RoundIconView(ctx, theme);
            icon.setIcon(app.icon);
            icon.setContentDescription(app.label);
            icon.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { host.launch(app); }
            });
            icon.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    host.showAppMenu(app);
                    return true;
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(theme.iconSizeDp - 4), dp(theme.iconSizeDp - 4));
            lp.leftMargin = dp(8);
            lp.rightMargin = dp(8);
            dock.addView(icon, lp);
        }

        // Прибрати зі збереженого списку застосунки, яких уже немає
        // (лише коли список застосунків завантажено).
        if (valid.size() != keys.size() && !host.allApps().isEmpty()) {
            Prefs.setDock(ctx, valid);
        }

        if (valid.isEmpty()) {
            TextView hint = new TextView(ctx);
            hint.setText(R.string.dock_hint);
            hint.setTextColor(Glass.withAlpha(theme.textColor, 150));
            hint.setTextSize(13);
            hint.setGravity(Gravity.CENTER);
            dock.addView(hint);
        }
    }

    @Override
    protected void onAppsChanged() {
        applyFilter();
        refreshDock();
    }

    @Override
    public void onHome() {
        search.setText("");
        search.clearFocus();
        Compat.hideKeyboard(search);
        grid.smoothScrollToPosition(0);
    }

    @Override
    public boolean onBack() {
        if (search != null && search.getText().length() > 0) {
            search.setText("");
            return true;
        }
        if (search != null && search.hasFocus()) {
            search.clearFocus();
            Compat.hideKeyboard(search);
            return true;
        }
        return false;
    }

    @Override
    public void showSettings() {
        List<CharSequence> names = new ArrayList<CharSequence>();
        names.add(ctx.getString(R.string.classic_columns));
        names.add(ctx.getString(R.string.hidden_apps));
        final boolean offerDefault = !host.isDefaultLauncher();
        if (offerDefault) names.add(ctx.getString(R.string.set_default));
        names.add(ctx.getString(R.string.about));

        GlassDialog.showItems(ctx, ctx.getString(R.string.space_settings),
                names.toArray(new CharSequence[0]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            pickColumns();
                        } else if (which == 1) {
                            host.showHiddenApps();
                        } else if (offerDefault && which == 2) {
                            host.requestDefaultLauncher();
                        } else {
                            host.showAbout();
                        }
                    }
                });
    }

    private void pickColumns() {
        CharSequence[] items = new CharSequence[COLUMN_VALUES.length];
        for (int i = 0; i < COLUMN_VALUES.length; i++) {
            items[i] = COLUMN_VALUES[i] == 0
                    ? ctx.getString(R.string.columns_auto)
                    : String.valueOf(COLUMN_VALUES[i]);
        }
        GlassDialog.showItems(ctx, ctx.getString(R.string.classic_columns), items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        host.store().setClassicColumns(COLUMN_VALUES[which]);
                        grid.setNumColumns(columns());
                    }
                });
    }
}
