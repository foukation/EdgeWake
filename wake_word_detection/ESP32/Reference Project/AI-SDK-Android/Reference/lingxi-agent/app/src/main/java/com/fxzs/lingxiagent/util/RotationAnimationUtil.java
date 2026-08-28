package com.fxzs.lingxiagent.util;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.fxzs.lingxiagent.view.chat.DeepResearchAdapter;

/**
 * 属性动画工具类，用于实现View绕中心旋转的效果
 */
public class RotationAnimationUtil {

    /**
     * 使View绕自身中心旋转指定角度
     * @param view 需要旋转的View
     * @param fromDegrees 起始角度
     * @param toDegrees 结束角度
     * @param duration 动画持续时间(毫秒)
     * @return ObjectAnimator对象，可用于控制动画
     */
    public static ObjectAnimator rotate(View view, float fromDegrees, float toDegrees, long duration) {
        return rotate(view, fromDegrees, toDegrees, duration, new AccelerateDecelerateInterpolator(), null);
    }

    /**
     * 使View绕自身中心旋转指定角度，带插值器和动画监听
     * @return ObjectAnimator对象，可用于控制动画
     */
    public static ObjectAnimator rotate(View view, float fromDegrees, float toDegrees, long duration,
                                        Interpolator interpolator, ValueAnimator.AnimatorUpdateListener listener) {
        final ObjectAnimator[] animatorHolder = new ObjectAnimator[1];

        // 确保在View测量完成后再设置旋转中心
        setupPivotPoint(view, () -> {
            // 创建旋转动画
            ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(
                    view,
                    "rotation",
                    fromDegrees,
                    toDegrees
            );

            // 设置动画属性
            rotationAnimator.setDuration(duration);
            rotationAnimator.setInterpolator(interpolator != null ? interpolator : new AccelerateDecelerateInterpolator());

            if (listener != null) {
                rotationAnimator.addUpdateListener(listener);
            }

            // 启动动画
            rotationAnimator.start();

            animatorHolder[0] = rotationAnimator;
        });

        return animatorHolder[0];
    }
    /**
     * 使View绕自身中心无限旋转
     * @param view 需要旋转的View
     * @param duration 旋转一周的时间(毫秒)
     * @return ObjectAnimator对象，可用于控制动画（暂停、取消等）
     */
    public static void rotateIndefinitely(View view, long duration, DeepResearchAdapter.AnimationCallback animationCallback) {
        rotateIndefinitely(view, duration, new AccelerateDecelerateInterpolator(),animationCallback);
    }
    /**
     * 使View绕自身中心无限旋转
     * @param view 需要旋转的View
     * @param duration 旋转一周的时间(毫秒)
     * @return ObjectAnimator对象，可用于控制动画（暂停、取消等）
     */
//    public static ObjectAnimator rotateIndefinitely(View view, long duration) {
//        return rotateIndefinitely(view, duration, new AccelerateDecelerateInterpolator());
//    }

    /**
     * 使View绕自身中心无限旋转，带插值器
     * @param view 需要旋转的View
     * @param duration 旋转一周的时间(毫秒)
     * @param interpolator 插值器
     *
     */
    public static void rotateIndefinitely(View view, long duration, Interpolator interpolator, DeepResearchAdapter.AnimationCallback animationCallback) {

        setupPivotPoint(view, () -> {
            ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f);
            rotationAnimator.setDuration(duration);
            rotationAnimator.setInterpolator(interpolator != null ? interpolator : new AccelerateDecelerateInterpolator());
            rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
            rotationAnimator.setRepeatMode(ValueAnimator.RESTART);
            rotationAnimator.start();
            animationCallback.back(rotationAnimator);
        });

    }

    /**
     * 使View绕自身中心无限旋转，带插值器
     * @param view 需要旋转的View
     * @param duration 旋转一周的时间(毫秒)
     * @param interpolator 插值器
     * @return ObjectAnimator对象，可用于控制动画（暂停、取消等）
     */
   /* public static ObjectAnimator rotateIndefinitely(View view, long duration, Interpolator interpolator) {
        final ObjectAnimator[] animatorHolder = new ObjectAnimator[1];

        setupPivotPoint(view, () -> {
            ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f);
            rotationAnimator.setDuration(duration);
            rotationAnimator.setInterpolator(interpolator != null ? interpolator : new AccelerateDecelerateInterpolator());
            rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
            rotationAnimator.setRepeatMode(ValueAnimator.RESTART);
            rotationAnimator.start();

            animatorHolder[0] = rotationAnimator;
        });

        return animatorHolder[0];
    }*/

    /**
     * 确保在View测量完成后设置旋转支点并执行动画
     */
    private static void setupPivotPoint(View view, Runnable animationTask) {
        // 检查View是否已经测量完成
        if (view.getWidth() > 0 && view.getHeight() > 0) {
            // 已经测量完成，直接设置支点
            view.setPivotX(view.getWidth() / 2f);
            view.setPivotY(view.getHeight() / 2f);
            animationTask.run();
        } else {
            // 尚未测量完成，添加监听器等待测量完成
            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // 移除监听器，避免重复调用
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        //noinspection deprecation
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }

                    // 设置支点为中心
                    view.setPivotX(view.getWidth() / 2f);
                    view.setPivotY(view.getHeight() / 2f);

                    // 执行动画任务
                    animationTask.run();
                }
            });
        }
    }

    /**
     * 取消指定的旋转动画
     * @param animator 要取消的动画对象
     */
    public static void cancelRotation(ObjectAnimator animator) {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    /**
     * 批量取消多个旋转动画
     * @param animators 要取消的动画对象数组
     */
    public static void cancelAllRotations(ObjectAnimator... animators) {
        if (animators == null) return;

        for (ObjectAnimator animator : animators) {
            cancelRotation(animator);
        }
    }
}
