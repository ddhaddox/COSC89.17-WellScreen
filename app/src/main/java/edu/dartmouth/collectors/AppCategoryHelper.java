// File: edu/dartmouth/collectors/AppCategoryHelper.java
package edu.dartmouth.collectors;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

public class AppCategoryHelper {
    public static String getAppCategory(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        String categoryName = "Unknown";

        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int category = appInfo.category;
                categoryName = getCategoryName(category);
            } else {
                categoryName = "Other";
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return categoryName;
    }

    private static String getCategoryName(int category) {
        switch (category) {
            case ApplicationInfo.CATEGORY_GAME:
                return "Game";
            case ApplicationInfo.CATEGORY_AUDIO:
                return "Audio";
            case ApplicationInfo.CATEGORY_VIDEO:
                return "Video";
            case ApplicationInfo.CATEGORY_IMAGE:
                return "Image";
            case ApplicationInfo.CATEGORY_SOCIAL:
                return "Social";
            case ApplicationInfo.CATEGORY_NEWS:
                return "News";
            case ApplicationInfo.CATEGORY_MAPS:
                return "Maps";
            case ApplicationInfo.CATEGORY_PRODUCTIVITY:
                return "Productivity";
            default:
                return "Other";
        }
    }
}
