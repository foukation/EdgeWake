package com.fxzs.lingxiagent.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.R;
import com.fxzs.smartassist.util.ZUtil.SizeUtils;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;


/**
 * 创建者：ZyOng
 * 描述：阴影工具类
 * 创建时间：2025/7/31 下午4:13
 */

public class ShadowUtils {

    /**
     * 为 View 设置阴影 + 圆角 + 可选描边（stroke）
     *
     * @param view             目标视图
     * @param context          上下文
     * @param elevationDp      阴影高度（dp，默认 8dp）
     * @param shadowColor      阴影颜色（如 ContextCompat.getColor(...)）
     * @param cornerRadiusDp   圆角半径（dp，默认 16dp）
     * @param hasStroke        是否绘制边框
     * @param strokeColor      边框颜色
     * @param strokeWidthDp    边框宽度（dp，默认 1dp）
     * @param backgroundColor  背景色（默认白色）
     */
    public static void applyShadow(@NonNull View view,
                                   @NonNull Context context,
                                   @FloatRange(from = 0f) float elevationDp,
                                   @ColorInt int shadowColor,
                                   @FloatRange(from = 0f) float cornerRadiusDp,
                                   boolean hasStroke,
                                   @ColorInt int strokeColor,
                                   @FloatRange(from = 0f) float strokeWidthDp,
                                   @ColorInt int backgroundColor) {

        float cornerRadius = cornerRadiusDp > 0 ? SizeUtils.dpToPx(cornerRadiusDp) : SizeUtils.dpToPx(16);
        float elevationPx = elevationDp > 0 ? SizeUtils.dpToPx(elevationDp) : SizeUtils.dpToPx(8);
        float strokeWidthPx = strokeWidthDp > 0 ? SizeUtils.dpToPx(strokeWidthDp) : SizeUtils.dpToPx(1);

        ShapeAppearanceModel shapeModel = new ShapeAppearanceModel.Builder()
                .setAllCorners(CornerFamily.ROUNDED, cornerRadius)
                .build();

        MaterialShapeDrawable shapeDrawable = new MaterialShapeDrawable(shapeModel);
        shapeDrawable.setFillColor(ColorStateList.valueOf(backgroundColor != 0 ? backgroundColor : Color.WHITE));

        if (hasStroke) {
            shapeDrawable.setStroke(strokeWidthPx, ColorStateList.valueOf(strokeColor));
            int padding = (int) (strokeWidthPx / 2);
            shapeDrawable.setPadding(padding, padding, padding, padding);
        }

        // 设置阴影相关
        shapeDrawable.setShadowCompatibilityMode(MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS);
        shapeDrawable.initializeElevationOverlay(context);
        shapeDrawable.setElevation(elevationPx);
        shapeDrawable.setShadowColor(shadowColor);

        // 禁止父布局裁剪，才能显示阴影
        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).setClipChildren(false);
        }

        // 设置背景
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(shapeDrawable);
        } else {
            view.setBackgroundDrawable(shapeDrawable);
        }
    }

    // 快捷默认调用
    public static void applyDefaultShadow(@NonNull View view, @NonNull Context context) {
        applyShadow(
                view,
                context,
                10, // elevation dp
                ContextCompat.getColor(context, R.color.color_606F8B),
                16,
                false,
                Color.TRANSPARENT,
                0,
                Color.WHITE
        );
    }
    public static void applyDefaultShadowTransparent(@NonNull View view, @NonNull Context context) {
        applyShadow(
                view,
                context,
                10, // elevation dp
                ContextCompat.getColor(context, R.color.color_606F8B),
                16,
                false,
                Color.TRANSPARENT,
                0,
                Color.TRANSPARENT
        );
    }

    public static void applyDefaultShadow(@NonNull View view, @NonNull Context context,int color) {
        applyShadow(
                view,
                context,
                10, // elevation dp
                color,
                16,
                false,
                Color.TRANSPARENT,
                0,
                Color.WHITE
        );
    }

    public static void applyDefaultShadow(@NonNull View view, @NonNull Context context,int color,int background) {
        applyShadow(
                view,
                context,
                10, // elevation dp
                color,
                16,
                false,
                Color.TRANSPARENT,
                0,
                background
        );
    }
}

