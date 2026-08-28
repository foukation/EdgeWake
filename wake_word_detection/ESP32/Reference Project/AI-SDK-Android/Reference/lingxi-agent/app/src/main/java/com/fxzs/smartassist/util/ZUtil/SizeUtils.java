package com.fxzs.smartassist.util.ZUtil;
import android.content.res.Resources;
import android.util.TypedValue;

/**
 * 尺寸单位转换工具类
 */
public class SizeUtils {

    /**
     * dp 转 px
     */
    public static int dpToPx(float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                Resources.getSystem().getDisplayMetrics()
        );
    }

    /**
     * sp 转 px
     */
    public static int spToPx(float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                Resources.getSystem().getDisplayMetrics()
        );
    }

    /**
     * px 转 dp
     */
    public static float pxToDp(float value) {
        float scale = Resources.getSystem().getDisplayMetrics().density;
        return (value / scale + 0.5f);
    }

    /**
     * px 转 sp
     */
    public static float pxToSp(float value) {
        float scale = Resources.getSystem().getDisplayMetrics().scaledDensity;
        return (value / scale + 0.5f);
    }
}

