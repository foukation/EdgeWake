package com.fxzs.lingxiagent.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.AudioRecorderManager;
import com.fxzs.lingxiagent.util.ZUtils;

import timber.log.Timber;

public class MyMeetingRecordService extends Service {
    private static final int NOTIFICATION_ID =  2;
    private static final String CHANNEL_ID =  "MyMeetingRecordService";
    private Runnable timeUpdateRunnable;
    RemoteViews remoteViews;
    private Handler mainHandler;

    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        notificationManager = getSystemService(NotificationManager.class);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 创建通知渠道(Android 8.0+要求)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service",
                    NotificationManager.IMPORTANCE_NONE
            );
            getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);
        }
         remoteViews = new RemoteViews(getPackageName(), R.layout.notion_layout_record);
        remoteViews.setTextViewText(R.id.tv_recording_time, "00:00:00");

        // 创建Notification
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
//                .setContentTitle("会议录制服务")
//                .setContentText("服务正在运行...")
                .setCustomContentView(remoteViews)
                .setSmallIcon(R.drawable.app_logo)
                .setOnlyAlertOnce(true)
                .build();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
//                            GlobalToast.show((AppCompatActivity) context, context.getString(R.string.record_hint), GlobalToast.Type.ERROR);
            return START_NOT_STICKY;
        }
        // 启动前台服务并显示通知
        startForeground(NOTIFICATION_ID, notification);

        // 执行需要长时间运行的任务...

        Timber.tag(CHANNEL_ID).d("MyMeetingRecordService AudioRecorderManager initAudioRecorder");
//        AudioRecorderManager.getInstance().init(this);
//        AudioRecorderManager audioRecorderManager = new AudioRecorderManager(this);
//        VMRealtimeMeeting viewModel = new ViewModelProvider((ViewModelStoreOwner) getApplication()).get(VMRealtimeMeeting.class);
//        viewModel.initAudioRecorder(audioRecorderManager);

//        boolean success = AudioRecorderManager.getInstance().startRecording("");
//        if (!success) {
//            Timber.tag(CHANNEL_ID).d("开始录音失败");
//        }else {
//            Timber.tag(CHANNEL_ID).d("开始录音成功");
//        }
        startTimeUpdate();
        return START_STICKY; // 保持服务在内存中的状态
    }

    @Override
    public void onDestroy() {
        super.onDestroy();// 停止定时任务
        if (mainHandler != null && timeUpdateRunnable != null) {
            mainHandler.removeCallbacks(timeUpdateRunnable);
        }
        // 停止前台服务并移除通知
        stopForeground(true);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 注意：还需要在AndroidManifest.xml中声明此服务

    private void startTimeUpdate() {
        ZUtils.print("startTimeUpdate = ");
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
//                if (viewModel.getIsRecording().getValue() == Boolean.TRUE) {
//              long recordingStartTime =   AudioRecorderManager.getInstance().getRecordingStartTime();
//                    long elapsedTime = System.currentTimeMillis() - recordingStartTime;

                long elapsedTime =  AudioRecorderManager.getInstance().getRecordingCount();

                    updateRecordingTime(elapsedTime);
                ZUtils.print("elapsedTime = "+elapsedTime);
                    mainHandler.postDelayed(this, 1000);
//                remoteViews.setTextViewText(R.id.tv_recording_time, elapsedTime+"");
// 更新通知
                Notification notification = new Notification.Builder(MyMeetingRecordService.this, CHANNEL_ID)
                        .setCustomContentView(remoteViews)
                        .setSmallIcon(R.drawable.app_logo)
                        .setOnlyAlertOnce(true)
                        .build();
                notificationManager.notify(NOTIFICATION_ID, notification);
//                }
            }
        };
        mainHandler.post(timeUpdateRunnable);

    }


    private void updateRecordingTime(long elapsedMillis) {
        long seconds = elapsedMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        String timeText;
        if (hours > 0) {
            timeText = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            timeText = String.format("%02d:%02d", minutes, seconds % 60);
        }

        remoteViews.setTextViewText(R.id.tv_recording_time, timeText+"");
    }

}