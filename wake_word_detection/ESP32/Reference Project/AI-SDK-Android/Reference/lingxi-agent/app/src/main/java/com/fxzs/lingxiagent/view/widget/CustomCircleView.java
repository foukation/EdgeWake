package com.fxzs.lingxiagent.view.widget;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CustomCircleView extends View {
    private Paint paint;
    private float radius = 0f;
    private static int maxRadius = 500;
    private static final int SPEED = 4;

    private List<Float> radii = new ArrayList<>(); // 动态存储所有圆的半径
    private static final int DELAY_MS = 500; // 每500ms生成一个新圆
    private Handler handler = new Handler(Looper.getMainLooper());
    public CustomCircleView(Context context) {
        super(context);
        init();
    }

    public CustomCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        maxRadius = w / 2; // 设置MAX_RADIUS为控件宽度的一半（圆半径到边缘）
    }
//    private void init() {
//        paint = new Paint();
//        paint.setColor(Color.parseColor("#ECF6FF"));
//        paint.setStyle(Paint.Style.FILL);
//    }
//
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        // Draw circles from center
        canvas.drawCircle(width / 2, height / 2, radius, paint);

        // Increase radius and invalidate for animation
        radius += SPEED;
        if (radius > maxRadius) {
            radius = 0f;
        }
        invalidate();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#ECF6FF"));
        paint.setStyle(Paint.Style.FILL);
        startNewCircle(); // 开始第一个圆
    }


//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        int width = getWidth();
//        int height = getHeight();
//
//        // 绘制所有圆
//        for (Float radius : radii) {
//            if (radius != null && radius > 0) {
//                canvas.drawCircle(width / 2, height / 2, radius, paint);
//            }
//        }
//
//        // 更新所有圆的状态
//        boolean needsInvalidate = false;
//        for (int i = 0; i < radii.size(); i++) {
//            Float radius = radii.get(i);
//            if (radius != null && radius > 0 && radius < MAX_RADIUS) {
//                radii.set(i, radius + SPEED);
//                needsInvalidate = true;
//            }
//        }
//
//        if (needsInvalidate) {
//            invalidate();
//        }
//    }

//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        int width = getWidth();
//        int height = getHeight();
//
//        // 绘制所有活跃的圆
//        for (Float radius : radii) {
//            if (radius != null && radius > 0) {
//                canvas.drawCircle(width / 2, height / 2, radius, paint);
//            }
//        }
//
//        // 更新所有圆的状态
//        boolean needsInvalidate = false;
//        for (int i = 0; i < radii.size(); i++) {
//            Float radius = radii.get(i);
//            if (radius != null && radius > 0) {
//                radii.set(i, radius + SPEED);
//                if (radius <= MAX_RADIUS) {
//                    needsInvalidate = true;
//                } else {
//                    radii.set(i, 0f); // 达到MAX_RADIUS后隐藏
//                }
//            }
//        }
//
//        if (needsInvalidate) {
//            invalidate();
//        }
//    }

    private void startNewCircle() {
        radii.add(1f); // 添加新圆
        invalidate();

        // 每500ms启动一个新圆
        handler.postDelayed(this::startNewCircle, DELAY_MS);
    }
}