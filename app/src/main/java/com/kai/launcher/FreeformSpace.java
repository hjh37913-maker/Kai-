package com.kai.launcher;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Вільний простір: значки застосунків і годинник можна розташовувати де завгодно.
 * Тап відкриває застосунок; довге утримання і перетягування переміщує елемент;
 * довге утримання без руху відкриває меню (розмір, видалити).
 * Макети зберігаються окремо для телефона/планшета та портрета/ландшафту.
 */
public class FreeformSpace extends Space {
    private static final int TYPE_APP = 0;
    private static final int TYPE_CLOCK = 1;
    private static final float[] STEPS = { 0.7f, 1.0f, 1.4f, 1.8f };

    private static final class Item {
        int type;
        String key = "";
        float fx = 0.5f;
        float fy = 0.5f;
        float scale = 1.0f;
    }

    private static final class FreeformCanvas extends FrameLayout {
        FreeformCanvas(Context c) {
            super(c);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                Object tag = child.getTag();
                if (tag instanceof Item) {
                    Item it = (Item) tag;
                    float maxX = Math.max(0, getWidth() - child.getWidth());
                    float maxY = Math.max(0, getHeight() - child.getHeight());
                    child.setTranslationX(it.fx * maxX);
                    child.setTranslationY(it.fy * maxY);
                }
            }
        }
    }

    private final List<Item> items = new ArrayList<Item>();
    private FreeformCanvas canvas;

    public FreeformSpace(LauncherHost host) {
        super(host);
    }

    @Override
    public int titleRes() {
        return R.string.space_freeform;
    }

    @Override
    protected View createView() {
        canvas = new FreeformCanvas(ctx);
        loadItems();
        rebuildViews();
        return canvas;
    }

    @Override
    protected void onAppsChanged() {
        rebuildViews();
    }

    // ---------- Збереження (окремо для кожного типу екрана) ----------

    private String layoutKey() {
        Configuration cfg = ctx.getResources().getConfiguration();
        boolean tablet = cfg.smallestScreenWidthDp >= 600;
        boolean land = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE;
        return (tablet ? "tablet" : "phone") + (land ? "_land" : "_port");
    }

    private void loadItems() {
        items.clear();
        try {
            JSONObject rootObj = new JSONObject(host.store().getFreeform());
            JSONArray arr = rootObj.optJSONArray(layoutKey());
            if (arr == null) return;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Item it = new Item();
                it.type = o.optInt("t", TYPE_APP);
                it.key = o.optString("k", "");
                it.fx = (float) o.optDouble("x", 0.5);
                it.fy = (float) o.optDouble("y", 0.5);
                it.scale = (float) o.optDouble("s", 1.0);
                items.add(it);
            }
        } catch (JSONException e) {
            // Пошкоджені дані: починаємо з порожнього макета.
            items.clear();
        }
    }

    private void save() {
        JSONObject rootObj;
        try {
            rootObj = new JSONObject(host.store().getFreeform());
        } catch (JSONException e) {
            rootObj = new JSONObject();
        }
        try {
            JSONArray arr = new JSONArray();
            for (Item it : items) {
                JSONObject o = new JSONObject();
                o.put("t", it.type);
                o.put("k", it.key);
                o.put("x", it.fx);
                o.put("y", it.fy);
                o.put("s", it.scale);
                arr.put(o);
            }
            rootObj.put(layoutKey(), arr);
            host.store().setFreeform(rootObj.toString());
        } catch (JSONException e) {
            // Запис простих значень не може викликати помилку.
        }
    }

    // ---------- Побудова елементів ----------

    private void rebuildViews() {
        canvas.removeAllViews();

        if (items.isEmpty()) {
            TextView hint = new TextView(ctx);
            hint.setText(R.string.freeform_empty);
            hint.setTextColor(Glass.withAlpha(theme.textColor, 150));
            hint.setGravity(Gravity.CENTER);
            canvas.addView(hint, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return;
        }

        for (Item it : items) {
            View v = makeView(it);
            if (v == null) continue; // застосунок ще не завантажено або видалено; елемент лишається в макеті
            canvas.addView(v);
        }
    }

    private View makeView(Item it) {
        if (it.type == TYPE_CLOCK) {
            GlassPanel p = new GlassPanel(ctx, theme);
            TextClock tc = new TextClock(ctx);
            tc.setTextColor(theme.textColor);
            tc.setTextSize(36 * it.scale);
            p.addView(tc);
            p.setTag(it);
            p.setContentDescription(ctx.getString(R.string.freeform_clock_title));
            p.setOnTouchListener(new DragTouch());
            p.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return p;
        }

        AppInfo app = host.findByKey(it.key);
        if (app == null) return null;

        int iconPx = (int) (dp(theme.iconSizeDp) * it.scale);
        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);

        RoundIconView icon = new RoundIconView(ctx, theme);
        icon.setIcon(app.icon);
        cell.addView(icon, new LinearLayout.LayoutParams(iconPx, iconPx));

        TextView label = new TextView(ctx);
        label.setText(app.label);
        label.setTextColor(theme.textColor);
        label.setTextSize(theme.labelSizeSp);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        label.setShadowLayer(3f, 0f, 1f, 0xAA000000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        cell.addView(label, lp);

        cell.setTag(it);
        cell.setContentDescription(app.label);
        cell.setOnTouchListener(new DragTouch());
        cell.setLayoutParams(new FrameLayout.LayoutParams(
                iconPx + dp(24), ViewGroup.LayoutParams.WRAP_CONTENT));
        return cell;
    }

    // ---------- Перетягування ----------

    private final class DragTouch implements View.OnTouchListener {
        private float downX;
        private float downY;
        private float startTx;
        private float startTy;
        private boolean longPressed;
        private boolean moved;
        private boolean cancelled;
        private Runnable longPress;

        @Override
        public boolean onTouch(final View v, MotionEvent e) {
            final int slop = ViewConfiguration.get(ctx).getScaledTouchSlop();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startTx = v.getTranslationX();
                    startTy = v.getTranslationY();
                    longPressed = false;
                    moved = false;
                    cancelled = false;
                    longPress = new Runnable() {
                        @Override
                        public void run() {
                            longPressed = true;
                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(100).start();
                        }
                    };
                    v.postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                    return true;

                case MotionEvent.ACTION_MOVE: {
                    float dx = e.getRawX() - downX;
                    float dy = e.getRawY() - downY;
                    boolean beyondSlop = Math.abs(dx) > slop || Math.abs(dy) > slop;
                    if (!longPressed) {
                        if (beyondSlop) {
                            cancelled = true;
                            v.removeCallbacks(longPress);
                        }
                        return true;
                    }
                    if (beyondSlop) moved = true;
                    Item it = (Item) v.getTag();
                    float maxX = Math.max(0, canvas.getWidth() - v.getWidth());
                    float maxY = Math.max(0, canvas.getHeight() - v.getHeight());
                    float tx = Math.max(0, Math.min(maxX, startTx + dx));
                    float ty = Math.max(0, Math.min(maxY, startTy + dy));
                    v.setTranslationX(tx);
                    v.setTranslationY(ty);
                    it.fx = maxX > 0 ? tx / maxX : 0f;
                    it.fy = maxY > 0 ? ty / maxY : 0f;
                    return true;
                }

                case MotionEvent.ACTION_UP:
                    v.removeCallbacks(longPress);
                    if (longPressed) {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        if (moved) {
                            save();
                        } else {
                            showItemMenu((Item) v.getTag());
                        }
                    } else if (!cancelled) {
                        Item it = (Item) v.getTag();
                        v.performClick();
                        if (it.type == TYPE_APP) {
                            AppInfo app = host.findByKey(it.key);
                            if (app != null) host.launch(app);
                        }
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    v.removeCallbacks(longPress);
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    if (longPressed && moved) save();
                    return true;

                default:
                    return true;
            }
        }
    }

    private int stepIndex(float scale) {
        int best = 0;
        for (int i = 1; i < STEPS.length; i++) {
            if (Math.abs(STEPS[i] - scale) < Math.abs(STEPS[best] - scale)) best = i;
        }
        return best;
    }

    private void showItemMenu(final Item it) {
        String title;
        if (it.type == TYPE_CLOCK) {
            title = ctx.getString(R.string.freeform_clock_title);
        } else {
            AppInfo app = host.findByKey(it.key);
            title = app != null ? app.label : ctx.getString(R.string.freeform_remove);
        }
        CharSequence[] menu = {
                ctx.getString(R.string.freeform_smaller),
                ctx.getString(R.string.freeform_bigger),
                ctx.getString(R.string.freeform_remove)
        };
        GlassDialog.showItems(ctx, title, menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int idx = stepIndex(it.scale);
                if (which == 0) {
                    it.scale = STEPS[Math.max(0, idx - 1)];
                } else if (which == 1) {
                    it.scale = STEPS[Math.min(STEPS.length - 1, idx + 1)];
                } else {
                    items.remove(it);
                }
                save();
                rebuildViews();
            }
        });
    }

    // ---------- Додавання елементів ----------

    private void addItem(int type, String key) {
        Item it = new Item();
        it.type = type;
        it.key = key;
        int n = items.size() % 6;
        it.fx = 0.15f + 0.12f * n;
        it.fy = 0.15f + 0.12f * n;
        items.add(it);
        save();
        rebuildViews();
    }

    @Override
    public void showSettings() {
        CharSequence[] menu = {
                ctx.getString(R.string.freeform_add_app),
                ctx.getString(R.string.freeform_add_clock),
                ctx.getString(R.string.freeform_clear)
        };
        GlassDialog.showItems(ctx, ctx.getString(R.string.space_settings), menu,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            pickApp();
                        } else if (which == 1) {
                            addItem(TYPE_CLOCK, "");
                        } else {
                            GlassDialog.confirm(ctx, ctx.getString(R.string.freeform_clear),
                                    ctx.getString(R.string.freeform_clear_confirm),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int w) {
                                            items.clear();
                                            save();
                                            rebuildViews();
                                        }
                                    });
                        }
                    }
                });
    }

    private void pickApp() {
        final List<AppInfo> all = host.apps();
        if (all.isEmpty()) return;
        CharSequence[] names = new CharSequence[all.size()];
        for (int i = 0; i < all.size(); i++) names[i] = all.get(i).label;
        GlassDialog.showItems(ctx, ctx.getString(R.string.freeform_add_app), names,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addItem(TYPE_APP, all.get(which).key());
                    }
                });
    }
}
