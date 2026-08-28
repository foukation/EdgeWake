package com.fxzs.lingxiagent.view.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 语音波形动画视图
 */
public class VoiceWaveView extends View {
    
    private Paint wavePaint;
    private List<Float> waveHeights;
    private int waveCount = 5;
    private float maxWaveHeight;
    private float minWaveHeight;
    private boolean isAnimating = false;
    private Random random = new Random();
    
    // 动画相关
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 100; // 100ms更新一次
    
    public VoiceWaveView(Context context) {
        super(context);
        init();
    }
    
    public VoiceWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public VoiceWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        wavePaint = new Paint();
        wavePaint.setColor(0xFFFFFFFF); // 白色
        wavePaint.setStrokeWidth(4f);
        wavePaint.setStrokeCap(Paint.Cap.ROUND);
        wavePaint.setAntiAlias(true);
        
        waveHeights = new ArrayList<>();
        for (int i = 0; i < waveCount; i++) {
            waveHeights.add(0.3f); // 初始高度
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        maxWaveHeight = h * 0.8f;
        minWaveHeight = h * 0.2f;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (waveHeights.isEmpty()) return;
        
        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2;
        
        // 计算每个波形的间距
        float totalSpacing = width * 0.6f; // 使用60%的宽度
        float startX = width * 0.2f; // 从20%位置开始
        float spacing = totalSpacing / (waveCount - 1);
        
        // 绘制波形
        for (int i = 0; i < waveHeights.size(); i++) {
            float x = startX + i * spacing;
            float waveHeight = waveHeights.get(i) * maxWaveHeight;
            
            // 绘制上下对称的线条
            canvas.drawLine(x, centerY - waveHeight / 2, x, centerY + waveHeight / 2, wavePaint);
        }
        
        // 如果正在动画，继续更新
        if (isAnimating) {
            updateWaveHeights();
            postInvalidateDelayed(UPDATE_INTERVAL);
        }
    }
    
    private void updateWaveHeights() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            return;
        }
        lastUpdateTime = currentTime;
        
        // 更新每个波形的高度
        for (int i = 0; i < waveHeights.size(); i++) {
            float currentHeight = waveHeights.get(i);
            float targetHeight = 0.3f + random.nextFloat() * 0.7f; // 0.3到1.0之间
            
            // 平滑过渡
            float newHeight = currentHeight + (targetHeight - currentHeight) * 0.3f;
            waveHeights.set(i, newHeight);
        }
    }
    
    /**
     * 开始动画
     */
    public void startAnimation() {
        isAnimating = true;
        invalidate();
    }
    
    /**
     * 停止动画
     */
    public void stopAnimation() {
        isAnimating = false;
        // 重置波形高度
        for (int i = 0; i < waveHeights.size(); i++) {
            waveHeights.set(i, 0.3f);
        }
        invalidate();
    }
    
    /**
     * 设置音量级别（0-1）
     */
    public void setVolumeLevel(float level) {
        // 根据音量调整波形
        if (level > 0 && isAnimating) {
            for (int i = 0; i < waveHeights.size(); i++) {
                float baseHeight = 0.2f + level * 0.6f;
                float randomFactor = 0.8f + random.nextFloat() * 0.4f; // 0.8到1.2
                waveHeights.set(i, Math.min(baseHeight * randomFactor, 1.0f));
            }
            invalidate();
        }
    }
}