package com.kai.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AppRepository {

    private AppRepository() {}

    /** Синхронне завантаження: викликати з фонового потоку. */
    public static List<AppInfo> load(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);
        List<AppInfo> result = new ArrayList<AppInfo>();
        Set<String> seen = new HashSet<String>();
        String self = context.getPackageName();

        for (ResolveInfo ri : resolved) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (self.equals(pkg)) continue;
            String key = pkg + "/" + ri.activityInfo.name;
            if (!seen.add(key)) continue;

            String label;
            try {
                CharSequence l = ri.loadLabel(pm);
                label = (l == null || l.length() == 0) ? pkg : l.toString();
            } catch (RuntimeException e) {
                label = pkg;
            }
            Drawable icon;
            try {
                icon = ri.loadIcon(pm);
            } catch (RuntimeException e) {
                icon = pm.getDefaultActivityIcon();
            }
            ApplicationInfo ai = ri.activityInfo.applicationInfo;
            boolean system = ai != null && (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            result.add(new AppInfo(label, pkg, ri.activityInfo.name, icon, system));
        }

        final Collator collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
        Collections.sort(result, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a, AppInfo b) {
                return collator.compare(a.label, b.label);
            }
        });
        return result;
    }
}
