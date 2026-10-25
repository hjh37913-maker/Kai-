package com.kai.launcher;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * KAI Studio: редактор теми з живим попереднім переглядом, undo/redo,
 * порівнянням «до/після», автозбереженою чернеткою та бібліотекою тем.
 * Активна тема лаунчера змінюється лише після натискання «Застосувати».
 */
public class StudioActivity extends Activity {

    // ---------- Параметри з повзунками ----------

    private abstract static class Param {
        final int labelRes;
        final int min;
        final int max;

        Param(int labelRes, int min, int max) {
            this.labelRes = labelRes;
            this.min = min;
            this.max = max;
        }

        abstract int get(Theme t);

        abstract void set(Theme t, int v);
    }

    private static final Param[] P_GLASS = new Param[] {
        new Param(R.string.glass_opacity, 10, 200) {
            @Override int get(Theme t) { return t.glassAlpha; }
            @Override void set(Theme t, int v) { t.glassAlpha = v; }
        },
        new Param(R.string.stroke_opacity, 0, 255) {
            @Override int get(Theme t) { return t.strokeAlpha; }
            @Override void set(Theme t, int v) { t.strokeAlpha = v; }
        },
        new Param(R.string.corner_radius, 8, 40) {
            @Override int get(Theme t) { return t.cornerDp; }
            @Override void set(Theme t, int v) { t.cornerDp = v; }
        }
    };

    private static final Param P_DIM = new Param(R.string.bg_dim, 0, 200) {
        @Override int get(Theme t) { return t.dimAlpha; }
        @Override void set(Theme t, int v) { t.dimAlpha = v; }
    };

    private static final Param[] P_ICONS = new Param[] {
        new Param(R.string.icon_size, 40, 84) {
            @Override int get(Theme t) { return t.iconSizeDp; }
            @Override void set(Theme t, int v) { t.iconSizeDp = v; }
        },
        new Param(R.string.icon_padding, 5, 35) {
            @Override int get(Theme t) { return t.iconPaddingPct; }
            @Override void set(Theme t, int v) { t.iconPaddingPct = v; }
        },
        new Param(R.string.label_size, 10, 16) {
            @Override int get(Theme t) { return t.labelSizeSp; }
            @Override void set(Theme t, int v) { t.labelSizeSp = v; }
        }
    };

    private static final Param P_ANIM = new Param(R.string.studio_anim, 0, 400) {
        @Override int get(Theme t) { return t.animMs; }
        @Override void set(Theme t, int v) { t.animMs = v; }
    };

    private static final int S_GLASS = 0;
    private static final int S_COLORS = 1;
    private static final int S_ICONS = 2;
    private static final int S_MOTION = 3;
    private static final int S_THEMES = 4;
    private static final int[] SECTION_TITLES = {
            R.string.studio_sec_glass, R.string.studio_sec_colors, R.string.studio_sec_icons,
            R.string.studio_sec_motion, R.string.studio_sec_themes };

    private static final int[] ACCENTS = {
            0xFF0A84FF, 0xFF5AC8FA, 0xFF30D158, 0xFFFF9F0A,
            0xFFFF375F, 0xFFBF5AF2, 0xFF64D2FF, 0xFFFFD60A,
            0xFF8E8E93, 0xFFFFFFFF
    };
    private static final int[] TEXT_COLORS = {
            0xFFF8FAFC, 0xFFE2E8F0, 0xFFF1F5F9, 0xFFCBD5E1 };

    private static final int MAX_UNDO = 60;

    /** Інтерфейс самої студії має фіксований вигляд і не залежить від теми, яку редагують. */
    private final Theme ui = ThemeStore.defaultTheme();
    private final Theme defaults = ThemeStore.defaultTheme();

    private Theme base;   // «До»: активна тема на момент відкриття або останнього застосування
    private Theme draft;  // «Після»: те, що редагується
    private final List<String> undo = new ArrayList<String>();
    private final List<String> redo = new ArrayList<String>();
    private boolean showBefore;
    private int section = S_GLASS;
    private boolean wide;

    private final List<AppInfo> apps = new ArrayList<AppInfo>();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private LinearLayout toolbarHost;
    private LinearLayout toolsHost;
    private LinearLayout propsHost;
    private FrameLayout previewHost;
    private GlassButton undoBtn;
    private GlassButton redoBtn;
    private Object backToken;

    // ---------- Життєвий цикл ----------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Compat.edgeToEdge(this);
        backToken = Compat.registerBack(this, new Compat.BackHandler() {
            @Override public void onBack() { closeStudio(); }
        });
        Theme active = ThemeStore.load(this);
        base = active.copy();
        Theme d = ThemeLibrary.loadDraft(this);
        if (d != null && !d.toJson().equals(active.toJson())) {
            draft = d;
            Toast.makeText(this, R.string.studio_draft_restored, Toast.LENGTH_SHORT).show();
        } else {
            draft = active.copy();
        }
        buildUi();

        io.execute(new Runnable() {
            @Override
            public void run() {
                final List<AppInfo> loaded = AppRepository.load(StudioActivity.this);
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        apps.clear();
                        apps.addAll(loaded);
                        refreshPreview();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Compat.unregisterBack(this, backToken);
        io.shutdownNow();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        buildUi(); // стан (draft, undo/redo) лишається в полях активності
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        closeStudio();
    }

    private void closeStudio() {
        if (!draft.toJson().equals(ThemeStore.load(this).toJson())) {
            Toast.makeText(this, R.string.studio_draft_saved, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    // ---------- Допоміжне ----------

    private int dp(int v) {
        return Glass.dp(this, v);
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(ui.textColor);
        t.setTextSize(14);
        return t;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private GlassButton small(String text, boolean selected, View.OnClickListener l) {
        GlassButton b = new GlassButton(this, ui, text, selected);
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        b.setOnClickListener(l);
        return b;
    }

    private Theme parse(String json) {
        try {
            return Theme.fromJson(json);
        } catch (JSONException e) {
            return ThemeStore.defaultTheme();
        }
    }

    // ---------- Undo / redo / чернетка ----------

    private void pushUndo() {
        String json = draft.toJson();
        if (!undo.isEmpty() && undo.get(undo.size() - 1).equals(json)) return;
        undo.add(json);
        if (undo.size() > MAX_UNDO) undo.remove(0);
        redo.clear();
        updateUndoState();
    }

    private void doUndo() {
        if (undo.isEmpty()) return;
        redo.add(draft.toJson());
        draft = parse(undo.remove(undo.size() - 1));
        onDraftChanged(true);
    }

    private void doRedo() {
        if (redo.isEmpty()) return;
        undo.add(draft.toJson());
        draft = parse(redo.remove(redo.size() - 1));
        onDraftChanged(true);
    }

    private void onDraftChanged(boolean rebuildProps) {
        draft.clamp();
        ThemeLibrary.saveDraft(this, draft);
        boolean wasBefore = showBefore;
        showBefore = false;
        refreshPreview();
        if (wasBefore) renderToolbar(); else updateUndoState();
        if (rebuildProps) renderProps();
    }

    private void updateUndoState() {
        if (undoBtn != null) undoBtn.setTextColor(Glass.withAlpha(ui.textColor, undo.isEmpty() ? 90 : 255));
        if (redoBtn != null) redoBtn.setTextColor(Glass.withAlpha(ui.textColor, redo.isEmpty() ? 90 : 255));
    }

    // ---------- Дії панелі інструментів ----------

    private void applyDraft() {
        ThemeStore.applyWithBackup(this, draft);
        base = draft.copy();
        ThemeLibrary.clearDraft(this);
        showBefore = false;
        refreshPreview();
        renderToolbar();
        Toast.makeText(this, R.string.studio_applied, Toast.LENGTH_LONG).show();
    }

    private void revertApplied() {
        Theme prev = ThemeStore.swapWithPrevious(this);
        if (prev == null) {
            Toast.makeText(this, R.string.studio_no_previous, Toast.LENGTH_SHORT).show();
            return;
        }
        pushUndo();
        base = prev.copy();
        draft = prev.copy();
        ThemeLibrary.clearDraft(this);
        showBefore = false;
        refreshPreview();
        renderToolbar();
        renderProps();
    }

    private void resetToDefault() {
        pushUndo();
        draft = ThemeStore.defaultTheme();
        onDraftChanged(true);
    }

    private void discardDraft() {
        pushUndo();
        draft = ThemeStore.load(this);
        ThemeLibrary.clearDraft(this);
        showBefore = false;
        refreshPreview();
        renderToolbar();
        renderProps();
    }

    private void saveAsTheme() {
        GlassDialog.input(this, getString(R.string.studio_theme_name), draft.name,
                new GlassDialog.TextResult() {
                    @Override
                    public void onText(String text) {
                        if (text.length() == 0) {
                            Toast.makeText(StudioActivity.this,
                                    R.string.studio_name_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        draft.name = text;
                        ThemeLibrary.save(StudioActivity.this, text, draft);
                        ThemeLibrary.saveDraft(StudioActivity.this, draft);
                        Toast.makeText(StudioActivity.this,
                                R.string.studio_saved, Toast.LENGTH_SHORT).show();
                        if (section == S_THEMES) renderProps();
                    }
                });
    }

    private void showMoreMenu() {
        final List<Integer> actions = new ArrayList<Integer>();
        List<CharSequence> names = new ArrayList<CharSequence>();
        actions.add(0);
        names.add(getString(R.string.studio_save_as));
        actions.add(1);
        names.add(getString(R.string.studio_reset_default));
        if (ThemeStore.hasPrevious(this)) {
            actions.add(2);
            names.add(getString(R.string.studio_revert_applied));
        }
        actions.add(3);
        names.add(getString(R.string.studio_discard_draft));

        GlassDialog.showItems(this, getString(R.string.studio_title),
                names.toArray(new CharSequence[0]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (actions.get(which)) {
                            case 0: saveAsTheme(); break;
                            case 1: resetToDefault(); break;
                            case 2: revertApplied(); break;
                            default: discardDraft(); break;
                        }
                    }
                });
    }

    // ---------- Побудова інтерфейсу ----------

    private void buildUi() {
        wide = getResources().getConfiguration().screenWidthDp >= 720;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0B0F18);
        Compat.applyInsets(root, dp(8));

        toolbarHost = new LinearLayout(this);
        toolbarHost.setOrientation(LinearLayout.HORIZONTAL);
        toolbarHost.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(toolbarHost, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 8));

        previewHost = new FrameLayout(this);
        ScrollView previewScroll = new ScrollView(this);
        previewScroll.addView(previewHost, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        propsHost = new LinearLayout(this);
        propsHost.setOrientation(LinearLayout.VERTICAL);
        ScrollView propsScroll = new ScrollView(this);
        propsScroll.addView(propsHost, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        toolsHost = new LinearLayout(this);

        if (wide) {
            toolsHost.setOrientation(LinearLayout.VERTICAL);
            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.HORIZONTAL);
            body.addView(toolsHost, lp(dp(170), ViewGroup.LayoutParams.MATCH_PARENT, 0, 0, 8, 0));
            body.addView(previewScroll, lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0, 8, 0));
            ((LinearLayout.LayoutParams) previewScroll.getLayoutParams()).weight = 1f;
            body.addView(propsScroll, lp(dp(320), ViewGroup.LayoutParams.MATCH_PARENT, 0, 0, 0, 0));
            root.addView(body, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            toolsHost.setOrientation(LinearLayout.HORIZONTAL);
            root.addView(previewScroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 5f));
            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            hs.addView(toolsHost, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            root.addView(hs, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 6, 0, 6));
            root.addView(propsScroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 6f));
        }

        setContentView(root);
        renderToolbar();
        renderTools();
        renderProps();
        refreshPreview();
    }

    private void renderToolbar() {
        toolbarHost.removeAllViews();

        undoBtn = small("\u21B6", false, new View.OnClickListener() {
            @Override public void onClick(View v) { doUndo(); }
        });
        undoBtn.setContentDescription(getString(R.string.cd_undo));
        toolbarHost.addView(undoBtn, lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 4, 0));

        redoBtn = small("\u21B7", false, new View.OnClickListener() {
            @Override public void onClick(View v) { doRedo(); }
        });
        redoBtn.setContentDescription(getString(R.string.cd_redo));
        toolbarHost.addView(redoBtn, lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 4, 0));

        GlassButton compare = small(getString(R.string.studio_compare), showBefore,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showBefore = !showBefore;
                        refreshPreview();
                        renderToolbar();
                    }
                });
        toolbarHost.addView(compare, lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 4, 0));

        GlassButton apply = small(getString(R.string.studio_apply), true, new View.OnClickListener() {
            @Override public void onClick(View v) { applyDraft(); }
        });
        toolbarHost.addView(apply, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 4, 0));
        ((LinearLayout.LayoutParams) apply.getLayoutParams()).weight = 1f;

        GlassButton more = small("\u22EF", false, new View.OnClickListener() {
            @Override public void onClick(View v) { showMoreMenu(); }
        });
        more.setContentDescription(getString(R.string.cd_more));
        toolbarHost.addView(more, lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 4, 0));

        GlassButton close = small("\u2715", false, new View.OnClickListener() {
            @Override public void onClick(View v) { closeStudio(); }
        });
        close.setContentDescription(getString(R.string.cd_close));
        toolbarHost.addView(close, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        updateUndoState();
    }

    private void renderTools() {
        toolsHost.removeAllViews();
        for (int i = 0; i < SECTION_TITLES.length; i++) {
            final int id = i;
            GlassButton b = small(getString(SECTION_TITLES[i]), i == section, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    section = id;
                    renderTools();
                    renderProps();
                }
            });
            LinearLayout.LayoutParams p = wide
                    ? lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 6)
                    : lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 6, 0);
            toolsHost.addView(b, p);
        }
    }

    private void renderProps() {
        propsHost.removeAllViews();
        GlassCard card = new GlassCard(this, ui);
        switch (section) {
            case S_GLASS: fillGlass(card); break;
            case S_COLORS: fillColors(card); break;
            case S_ICONS: fillIcons(card); break;
            case S_MOTION: fillMotion(card); break;
            default: fillThemes(card); break;
        }
        propsHost.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // ---------- Розділи властивостей ----------

    private void fillGlass(LinearLayout card) {
        for (Param p : P_GLASS) addParamRow(card, p);

        GlassSwitch light = new GlassSwitch(this, ui, getString(R.string.lightweight), draft.lightweight);
        light.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                if (draft.lightweight == checked) return;
                pushUndo();
                draft.lightweight = checked;
                onDraftChanged(false);
            }
        });
        card.addView(light, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 12, 0, 0));
    }

    private void fillColors(LinearLayout card) {
        addParamRow(card, P_DIM);
        addOrbitColorPicker(card);
        addColorRow(card, R.string.studio_text_color, TEXT_COLORS, false);
    }

    private void addOrbitColorPicker(LinearLayout parent) {
        parent.addView(label(getString(R.string.studio_accent)),
                lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 16, 0, 8));

        FrameLayout orbit = new FrameLayout(this);
        int size = dp(180);
        orbit.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        ((LinearLayout.LayoutParams) orbit.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;

        // Central preview button
        final View center = new View(this);
        GradientDrawable centerBg = new GradientDrawable();
        centerBg.setShape(GradientDrawable.OVAL);
        centerBg.setColor(draft.accentColor);
        centerBg.setStroke(dp(3), Color.WHITE);
        center.setBackground(centerBg);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(64), dp(64));
        cp.gravity = Gravity.CENTER;
        orbit.addView(center, cp);

        // Orbiting color chips
        final int n = ACCENTS.length;
        for (int i = 0; i < n; i++) {
            final int color = ACCENTS[i];
            final View chip = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(color);
            g.setStroke(dp(1), Glass.withAlpha(Color.WHITE, 180));
            chip.setBackground(g);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (draft.accentColor == color) return;
                    pushUndo();
                    draft.accentColor = color;
                    centerBg.setColor(color);
                    center.invalidate();
                    onDraftChanged(false);
                }
            });
            int chipSize = dp(28);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(chipSize, chipSize);
            orbit.addView(chip, lp);

            // Animate orbit
            final float radius = dp(68);
            final float startAngle = (float) (i * 2 * Math.PI / n);
            chip.animate().setDuration(0);
            android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(8000);
            anim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            anim.setInterpolator(new android.view.animation.LinearInterpolator());
            anim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(android.animation.ValueAnimator a) {
                    float t = (Float) a.getAnimatedValue();
                    float angle = startAngle + t * (float) (2 * Math.PI);
                    float x = (float) (Math.cos(angle) * radius) + size / 2f - chipSize / 2f;
                    float y = (float) (Math.sin(angle) * radius) + size / 2f - chipSize / 2f;
                    chip.setX(x);
                    chip.setY(y);
                }
            });
            anim.start();
        }

        parent.addView(orbit);
    }

    private void fillIcons(LinearLayout card) {
        for (Param p : P_ICONS) addParamRow(card, p);

        card.addView(label(getString(R.string.icon_style)), lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 12, 0, 4));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int[] styleRes = { R.string.style_native, R.string.style_glass, R.string.style_solid, R.string.style_mono };
        int[] styleIds = { Theme.STYLE_NATIVE, Theme.STYLE_GLASS, Theme.STYLE_SOLID, Theme.STYLE_MONO };
        for (int i = 0; i < styleRes.length; i++) {
            final int style = styleIds[i];
            GlassButton b = small(getString(styleRes[i]), draft.iconStyle == style,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (draft.iconStyle == style) return;
                            pushUndo();
                            draft.iconStyle = style;
                            onDraftChanged(true);
                        }
                    });
            row.addView(b, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 3, 0, 3, 0));
            ((LinearLayout.LayoutParams) b.getLayoutParams()).weight = 1f;
        }
        card.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void fillMotion(LinearLayout card) {
        addParamRow(card, P_ANIM);
        TextView hint = label(getString(R.string.studio_anim_hint));
        hint.setTextSize(12);
        hint.setTextColor(Glass.withAlpha(ui.textColor, 160));
        card.addView(hint, lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 6, 0, 0));
    }

    private void fillThemes(LinearLayout card) {
        GlassButton saveAs = small(getString(R.string.studio_save_as), true, new View.OnClickListener() {
            @Override public void onClick(View v) { saveAsTheme(); }
        });
        card.addView(saveAs, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 12));

        card.addView(label(getString(R.string.studio_builtin)));
        for (final Theme preset : ThemeStore.presets()) {
            addThemeRow(card, preset.name, preset, false);
        }

        card.addView(label(getString(R.string.studio_mine)), lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 12, 0, 0));
        List<ThemeLibrary.Entry> mine = ThemeLibrary.list(this);
        if (mine.isEmpty()) {
            TextView empty = label(getString(R.string.studio_empty_mine));
            empty.setTextColor(Glass.withAlpha(ui.textColor, 150));
            card.addView(empty, lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 6, 0, 0));
        }
        for (ThemeLibrary.Entry e : mine) {
            addThemeRow(card, e.name, e.theme, true);
        }
    }

    private void addThemeRow(LinearLayout parent, final String name, final Theme theme, final boolean deletable) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView t = label(name);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(small(getString(R.string.studio_open), false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pushUndo();
                draft = theme.copy();
                onDraftChanged(true);
            }
        }));

        if (deletable) {
            GlassButton del = small(getString(R.string.studio_delete), false, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GlassDialog.confirm(StudioActivity.this, getString(R.string.studio_delete),
                            getString(R.string.studio_delete_confirm, name),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ThemeLibrary.delete(StudioActivity.this, name);
                                    renderProps();
                                }
                            });
                }
            });
            row.addView(del, lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 4, 0, 0, 0));
        }

        parent.addView(row, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 6, 0, 0));
    }

    private void addParamRow(LinearLayout parent, final Param p) {
        final String name = getString(p.labelRes);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        final TextView t = label(name + ": " + p.get(draft));
        head.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        GlassButton resetParam = small("\u21BA", false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int def = p.get(defaults);
                if (p.get(draft) == def) return;
                pushUndo();
                p.set(draft, def);
                onDraftChanged(true);
            }
        });
        resetParam.setContentDescription(getString(R.string.cd_reset_value));
        head.addView(resetParam);
        parent.addView(head, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 8, 0, 0));

        GlassSlider s = new GlassSlider(this, ui, p.min, p.max, p.get(draft), new GlassSlider.OnValue() {
            @Override
            public void onValue(int v) {
                p.set(draft, v);
                t.setText(name + ": " + v);
                onDraftChanged(false);
            }
        });
        s.setOnStart(new Runnable() {
            @Override public void run() { pushUndo(); }
        });
        parent.addView(s, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addColorRow(LinearLayout parent, int labelRes, final int[] palette, final boolean accent) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(label(getString(labelRes)),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        GlassButton resetColor = small("\u21BA", false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int def = accent ? defaults.accentColor : defaults.textColor;
                int cur = accent ? draft.accentColor : draft.textColor;
                if (cur == def) return;
                pushUndo();
                if (accent) draft.accentColor = def; else draft.textColor = def;
                onDraftChanged(true);
            }
        });
        resetColor.setContentDescription(getString(R.string.cd_reset_value));
        head.addView(resetColor);
        parent.addView(head, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 12, 0, 4));

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        int current = accent ? draft.accentColor : draft.textColor;
        for (final int color : palette) {
            View chip = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(color);
            boolean selected = color == current;
            g.setStroke(dp(selected ? 3 : 1), selected ? Color.WHITE : Glass.withAlpha(Color.WHITE, 100));
            chip.setBackground(g);
            chip.setContentDescription(String.format("#%06X", color & 0xFFFFFF));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int cur = accent ? draft.accentColor : draft.textColor;
                    if (cur == color) return;
                    pushUndo();
                    if (accent) draft.accentColor = color; else draft.textColor = color;
                    onDraftChanged(true);
                }
            });
            chips.addView(chip, lp(dp(36), dp(36), 3, 0, 3, 0));
        }
        parent.addView(chips, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // ---------- Попередній перегляд ----------

    private void refreshPreview() {
        if (previewHost == null) return;
        previewHost.removeAllViews();
        previewHost.addView(buildPreview(showBefore ? base : draft), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private View buildPreview(Theme t) {
        GradientDrawable backdrop = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] { 0xFF1C2B4B, 0xFF0A0E1A });
        backdrop.setCornerRadius(dp(20));
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(backdrop);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable dim = new GradientDrawable();
        dim.setColor(Glass.withAlpha(Color.BLACK, t.dimAlpha));
        dim.setCornerRadius(dp(20));
        col.setBackground(dim);
        frame.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Пошук (справжній компонент із теми)
        GlassSearchBar sb = new GlassSearchBar(this, t);
        sb.setFocusable(false);
        sb.setFocusableInTouchMode(false);
        col.addView(sb, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 20, 0, 12));

        // Ряд іконок: справжні застосунки, якщо вже завантажені
        int previewIcon = Math.min(t.iconSizeDp, 76);
        LinearLayout icons = new LinearLayout(this);
        icons.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 4; i++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            RoundIconView iv = new RoundIconView(this, t);
            iv.setIcon(previewDrawable(i));
            cell.addView(iv, new LinearLayout.LayoutParams(dp(previewIcon), dp(previewIcon)));
            TextView tv = new TextView(this);
            tv.setText(previewLabel(i));
            tv.setTextColor(t.textColor);
            tv.setTextSize(t.labelSizeSp);
            tv.setSingleLine(true);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tv.setGravity(Gravity.CENTER);
            cell.addView(tv, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4, 0, 0));
            icons.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        col.addView(icons, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 12));

        // Картка з іншими компонентами
        GlassCard card = new GlassCard(this, t);
        TextView ct = new TextView(this);
        ct.setText(R.string.studio_card_title);
        ct.setTextColor(t.textColor);
        ct.setTextSize(16);
        card.addView(ct);
        card.addView(new GlassButton(this, t, getString(R.string.studio_button), false),
                lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 8, 0, 0));
        card.addView(new GlassSlider(this, t, 0, 100, 60, null), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(new GlassSwitch(this, t, getString(R.string.studio_switch), true));
        col.addView(card, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 12));

        // Док
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(12), dp(8), dp(12), dp(8));
        dock.setBackground(Glass.panel(this, t, t.cornerDp + 6));
        int dockIcon = Math.min(t.iconSizeDp - 4, 72);
        for (int i = 4; i < 8; i++) {
            RoundIconView iv = new RoundIconView(this, t);
            iv.setIcon(previewDrawable(i));
            dock.addView(iv, lp(dp(dockIcon), dp(dockIcon), 6, 0, 6, 0));
        }
        col.addView(dock, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Позначка «До» / «Після»
        TextView badge = new TextView(this);
        badge.setText(showBefore ? R.string.studio_before : R.string.studio_after);
        badge.setTextColor(ui.textColor);
        badge.setTextSize(12);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        badge.setBackground(Glass.dialogPanel(this, 12));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        bp.setMargins(0, dp(8), dp(8), 0);
        frame.addView(badge, bp);

        return frame;
    }

    private Drawable previewDrawable(int i) {
        if (i < apps.size()) return apps.get(i).icon;
        return getResources().getDrawable(android.R.drawable.sym_def_app_icon, getTheme());
    }

    private String previewLabel(int i) {
        return i < apps.size() ? apps.get(i).label : getString(R.string.preview_label);
    }
}
