package com.kai.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

public final class GlassDialog {

    public interface MultiResult {
        void onResult(boolean[] checked);
    }

    public interface TextResult {
        void onText(String text);
    }

    private GlassDialog() {}

    private static AlertDialog.Builder builder(Context c) {
        return new AlertDialog.Builder(c, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
    }

    private static AlertDialog finish(Context c, AlertDialog d) {
        d.getWindow().setBackgroundDrawable(Glass.dialogPanel(c, 28));
        d.show();
        return d;
    }

    public static AlertDialog showItems(Context c, CharSequence title, CharSequence[] items,
                                        DialogInterface.OnClickListener listener) {
        return finish(c, builder(c).setTitle(title).setItems(items, listener).create());
    }

    public static AlertDialog message(Context c, CharSequence title, CharSequence message) {
        return finish(c, builder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .create());
    }

    public static AlertDialog confirm(Context c, CharSequence title, CharSequence message,
                                      DialogInterface.OnClickListener onYes) {
        return finish(c, builder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onYes)
                .setNegativeButton(android.R.string.cancel, null)
                .create());
    }

    public static AlertDialog showMultiChoice(Context c, CharSequence title, CharSequence[] items,
                                              final boolean[] checked, final MultiResult result) {
        return finish(c, builder(c)
                .setTitle(title)
                .setMultiChoiceItems(items, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checked[which] = isChecked;
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.onResult(checked);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create());
    }

    public static AlertDialog input(Context c, CharSequence title, String initial,
                                    final TextResult result) {
        final EditText et = new EditText(c);
        et.setPadding(Glass.dp(c, 16), Glass.dp(c, 12), Glass.dp(c, 16), Glass.dp(c, 12));
        et.setTextColor(0xFFFFFFFF);
        et.setHintTextColor(Glass.withAlpha(0xFFFFFFFF, 120));
        et.setText(initial);
        et.setSelection(et.getText().length());
        return finish(c, builder(c)
                .setTitle(title)
                .setView(et)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.onText(et.getText().toString().trim());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create());
    }
}
