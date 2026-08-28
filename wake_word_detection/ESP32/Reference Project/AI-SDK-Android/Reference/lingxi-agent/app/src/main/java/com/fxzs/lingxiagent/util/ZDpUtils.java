package com.fxzs.lingxiagent.util;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class ZDpUtils {

    public static float pxToDp(Activity context, float px) {
        DisplayMetrics dm = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);
        float densityDpi = dm.densityDpi;
        return px / (densityDpi / 160f);
    }

    public static int dpToPx(Activity context, float dp) {
        DisplayMetrics dm = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);
        float density = dm.density;
        return Math.round(dp * density);
    }

    public static int dpToPx2(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
