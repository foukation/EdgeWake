package com.fxzs.lingxiagent.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.fxzs.lingxiagent.R;

/**
 * 用户体验优化工具类
 * 提供各种用户体验增强功能
 */
public class UXOptimizer {
    
    /**
     * 为按钮添加点击反馈效果
     */
    public static void addClickFeedback(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    // 按下时缩小
                    v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    // 释放时恢复
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                    break;
            }
            return false; // 不消费事件，让其他监听器继续处理
        });
    }
    
    /**
     * 为视图添加淡入动画
     */
    public static void fadeIn(View view, long duration) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }
    
    /**
     * 为视图添加淡出动画
     */
    public static void fadeOut(View view, long duration, Runnable onComplete) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            })
            .start();
    }
    
    /**
     * 为视图添加滑入动画
     */
    public static void slideIn(View view, long duration, boolean fromLeft) {
        float startX = fromLeft ? -view.getWidth() : view.getWidth();
        view.setTranslationX(startX);
        view.setVisibility(View.VISIBLE);
        view.animate()
            .translationX(0f)
            .setDuration(duration)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }
    
    /**
     * 为视图添加滑出动画
     */
    public static void slideOut(View view, long duration, boolean toLeft, Runnable onComplete) {
        float endX = toLeft ? -view.getWidth() : view.getWidth();
        view.animate()
            .translationX(endX)
            .setDuration(duration)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                    view.setTranslationX(0f); // 重置位置
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            })
            .start();
    }
    
    /**
     * 为视图添加弹跳动画
     */
    public static void bounce(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f);
        
        scaleX.setDuration(300);
        scaleY.setDuration(300);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        
        scaleX.start();
        scaleY.start();
    }
    
    /**
     * 为视图添加摇摆动画（用于错误提示）
     */
    public static void shake(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
        animator.setDuration(500);
        animator.start();
    }
    
    /**
     * 为按钮添加加载状态
     */
    public static void setButtonLoading(Button button, boolean loading, String loadingText, String normalText) {
        button.setEnabled(!loading);
        button.setText(loading ? loadingText : normalText);
        
        if (loading) {
            // 添加加载动画
            Animation rotation = AnimationUtils.loadAnimation(button.getContext(), R.anim.rotate_loading);
            button.startAnimation(rotation);
        } else {
            button.clearAnimation();
        }
    }
    
    /**
     * 为视图添加加载状态（通用版本）
     */
    public static void setViewLoading(View view, boolean loading) {
        view.setEnabled(!loading);
        
        if (loading) {
            // 添加加载动画
            Animation rotation = AnimationUtils.loadAnimation(view.getContext(), R.anim.rotate_loading);
            view.startAnimation(rotation);
        } else {
            view.clearAnimation();
        }
    }
    
    /**
     * 提供触觉反馈
     */
    public static void vibrate(Context context, long milliseconds) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(milliseconds);
            }
        } catch (Exception e) {
            // 忽略振动错误
        }
    }
    
    /**
     * 提供轻微触觉反馈
     */
    public static void lightVibrate(Context context) {
        vibrate(context, 50);
    }
    
    /**
     * 提供成功反馈
     */
    public static void successFeedback(Context context, View view) {
        lightVibrate(context);
        bounce(view);
    }
    
    /**
     * 提供错误反馈
     */
    public static void errorFeedback(Context context, View view) {
        vibrate(context, 100);
        shake(view);
    }
    
    /**
     * 为视图添加脉冲动画
     */
    public static void pulse(View view, long duration) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f);
        
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        alpha.setDuration(duration);
        
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);
        
        scaleX.start();
        scaleY.start();
        alpha.start();
    }
    
    /**
     * 停止脉冲动画
     */
    public static void stopPulse(View view) {
        view.clearAnimation();
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setAlpha(1f);
    }
    
    /**
     * 为视图添加呼吸灯效果
     */
    public static void breathe(View view) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.3f, 1f);
        alpha.setDuration(2000);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.start();
    }
    
    /**
     * 停止呼吸灯效果
     */
    public static void stopBreathe(View view) {
        view.clearAnimation();
        view.setAlpha(1f);
    }
    
    /**
     * 为视图添加进入动画
     */
    public static void animateIn(View view, AnimationType type, long duration) {
        switch (type) {
            case FADE_IN:
                fadeIn(view, duration);
                break;
            case SLIDE_IN_LEFT:
                slideIn(view, duration, true);
                break;
            case SLIDE_IN_RIGHT:
                slideIn(view, duration, false);
                break;
            case BOUNCE:
                view.setVisibility(View.VISIBLE);
                bounce(view);
                break;
        }
    }
    
    /**
     * 为视图添加退出动画
     */
    public static void animateOut(View view, AnimationType type, long duration, Runnable onComplete) {
        switch (type) {
            case FADE_OUT:
                fadeOut(view, duration, onComplete);
                break;
            case SLIDE_OUT_LEFT:
                slideOut(view, duration, true, onComplete);
                break;
            case SLIDE_OUT_RIGHT:
                slideOut(view, duration, false, onComplete);
                break;
            default:
                if (onComplete != null) {
                    onComplete.run();
                }
                break;
        }
    }

    /**
     * 为视图添加淡入动画
     */
    public static void scaleIn(View view, long duration, Runnable onComplete,float scale) {
        if (view == null){
            return;
        }
        view.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(duration)
                .withEndAction(onComplete)
                .start();
    }
    
    /**
     * 动画类型枚举
     */
    public enum AnimationType {
        FADE_IN,
        FADE_OUT,
        SLIDE_IN_LEFT,
        SLIDE_IN_RIGHT,
        SLIDE_OUT_LEFT,
        SLIDE_OUT_RIGHT,
        BOUNCE,
        SHAKE
    }
}