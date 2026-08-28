package com.fxzs.lingxiagent.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.R;

import timber.log.Timber;

public class MyForegroundService extends Service {
    private static final int NOTIFICATION_ID =  1;
    private static final String CHANNEL_ID =  "MyForegroundService";

    public static volatile boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        // 创建通知渠道(Android 8.0+要求)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);
        }
        // 创建Notification
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("前台服务")
                .setContentText("服务正在运行...")
                .setSmallIcon(R.drawable.app_logo)
                .setOnlyAlertOnce(true)
                .build();

        // 启动前台服务并显示通知
        startForeground(NOTIFICATION_ID, notification);

        // 执行需要长时间运行的任务...

        return START_STICKY; // 保持服务在内存中的状态
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.tag("TAG").d("onDestroy");
        isRunning = false;
        // 停止前台服务并移除通知
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 注意：还需要在AndroidManifest.xml中声明此服务
}