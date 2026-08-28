package com.fxzs.lingxiagent.service;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.fxzs.lingxiagent.BuildConfig;
import com.fxzs.lingxiagent.IYAApplication;
import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.WakeVoiceActivity;
import com.fxzs.lingxiagent.lingxi.float_manager.FloatWindowHelper;
import com.fxzs.lingxiagent.lingxi.float_manager.WakeVoiceCallback;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AsrManager;
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.AgentStatus;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.util.audio.OnPlayerListener;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.util.audio.TtsMediaPlayer;

import java.util.List;

import ai.dueros.wakeup.DuWakeup;
import ai.dueros.wakeup.DuWakeupError;
import ai.dueros.wakeup.WakeupCallback;
import ai.dueros.wakeup.WakeupEventInfo;
import ai.dueros.wakeup.config.DuWakeupIntent;
import ai.dueros.wakeup.config.WakeupInitConfig;
import timber.log.Timber;

public class WakeUpService extends Service {

    private static final String TAG = "WakeUpService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "wakeup_service_channel";

    /** 聆听模式录音期间暂停唤醒引擎，释放麦克风 */
    public static final String ACTION_PAUSE_WAKEUP = "com.fxzs.lingxiagent.action.PAUSE_WAKEUP";
    /** 聆听模式结束录音后恢复唤醒引擎 */
    public static final String ACTION_RESUME_WAKEUP = "com.fxzs.lingxiagent.action.RESUME_WAKEUP";

    private long startServiceTime = 0;
    private Handler mainHandler;
    private boolean wakeupInitialized = false;
    private boolean wakeupEngineRunning = false;
    private int listenModePauseDepth = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.tag(TAG).e("DuWakeup onCreate");
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DuWakeup", "DuWakeup onStartCommand action="
                + (intent != null ? intent.getAction() : "null"));
        if (startServiceTime == 0) {
            startServiceTime = System.currentTimeMillis();
        }

        String action = intent != null ? intent.getAction() : null;
        if (ACTION_PAUSE_WAKEUP.equals(action)) {
            pauseWakeupEngine();
            return START_STICKY;
        }
        if (ACTION_RESUME_WAKEUP.equals(action)) {
            resumeWakeupEngine();
            return START_STICKY;
        }

        ensureWakeupEngineStarted();
        return START_STICKY;
    }

    private void ensureWakeupEngineStarted() {
        if (listenModePauseDepth > 0) {
            Timber.tag(TAG).d("Listen mode holds wake pause, skip start");
            return;
        }
        DuWakeup duWakeup = DuWakeup.getInstance();
        if (duWakeup == null) {
            Timber.tag(TAG).e("DuWakeup instance is null.");
            return;
        }
        if (!wakeupInitialized) {
            initDuWakeup(duWakeup);
            setupWakeupCallback(duWakeup);
            wakeupInitialized = true;
        }
        if (!wakeupEngineRunning) {
            startDuWakeupEngine(duWakeup);
        }
    }

    private void initDuWakeup(DuWakeup duWakeup) {
        WakeupInitConfig cfg = new WakeupInitConfig();
        if (BuildConfig.FLAVOR.contains("Beta")) {
            duWakeup.setLogLevel(Log.DEBUG);
            cfg.wakeConfig.saveWakeupAudio = true;
        } else {
            duWakeup.setLogLevel(Log.WARN);
            cfg.wakeConfig.saveWakeupAudio = false;
        }
        cfg.wakeConfig.wakeThreshold = 0.90f;
        cfg.wakeConfig.wakeInterval = 1900;
        cfg.wakeConfig.detectionWindowsFrames = 40;
        cfg.wakeConfig.thresholdFramesCount = 1;
        cfg.isCustomWords = true;
        duWakeup.init(IYAApplication.getAppContext(), cfg);
    }

    private void setupWakeupCallback(DuWakeup duWakeup) {
        duWakeup.setWakeupCallback(new WakeupCallback() {
            @Override
            public void onInit() {
                Timber.tag(TAG).e("onInit");
            }

            @Override
            public void onStart() {
                Timber.tag(TAG).e("onStart");
            }

            @Override
            public void onWakeup(WakeupEventInfo wakeupEventInfo) {
                Timber.tag(TAG).d("onWakeup word:%s", wakeupEventInfo.word);
                if (System.currentTimeMillis() - startServiceTime < 1500) {
                    Timber.tag(TAG).d("服务刚启动，忽略本次唤醒");
                    return;
                }
                if (isKillScreen()){
                    Timber.tag(TAG).d("系统息屏");
                    return;
                }
                if (isLocked()){
                    Timber.tag(TAG).d("系统锁屏");
                    return;
                }
                if (AsrManager.isRecognizing()) {
                    Timber.tag(TAG).d("AsrManager正在识别中，忽略本次唤醒");
                    return;
                }
//                if (IYAApplication.getInstance().isAppInForeground()){
//                    Timber.tag(TAG).d("App在前台");
//                    return;
//                }

//                if (FloatWindowHelper.isShowing()){
//                    Timber.tag(TAG).d("已经显示悬浮界面");
//                    return;
//                }
//
                if (IYAApplication.getInstance().getCurrentActivity() instanceof WakeVoiceActivity){
                    WakeVoiceActivity wakeVoiceActivity = (WakeVoiceActivity) IYAApplication.getInstance().getCurrentActivity();
                    wakeVoiceActivity.finish();
                    wakeVoiceActivity.overridePendingTransition(0,0);
                }
//                if  (!(IYAApplication.getInstance().getCurrentActivity() instanceof WakeVoiceActivity) && IYAApplication.getInstance().getCurrentActivity() != null){//不在悬浮页不能唤醒
//                    Timber.tag(TAG).d("已经显示悬浮界面"+IYAApplication.getInstance().getCurrentActivity());
//                    return;
//                }

                mainHandler.post(() -> {
                    if ( AgentStatus.INSTANCE.isRunning()) {
                        return;
                    }
                    Timber.tag(TAG).d("APP在后台，调起至前台");
                    if (!isAppInForeground(WakeUpService.this)) {
                        Timber.tag(TAG).d("APP在后台，调起至前台");
                        playTTS("我在",true);
                    } else {
                        // 前台：显示可点击 Toast
//                        Toast toast = Toast.makeText(WakeUpService.this, "语音唤醒成功", Toast.LENGTH_LONG);
//                        View toastView = toast.getView();
//                        if (toastView != null) {
//                            toastView.setOnClickListener(v -> {
//                                toast.cancel();
//                                launchAppToForeground(WakeUpService.this,"");
//                            });
//                            toastView.setClickable(true);
//                            toastView.setFocusable(true);
//                        toast.setGravity(Gravity.TOP, 0, 0);
//                        toast.show();
//                        Log.d(TAG, "APP已在前台，无需调起");
                        playTTS("嗯",true);
                    }

                });
            }

            @Override
            public void onWakeupFrameThreshold(float v) {}

            @Override
            public void onAudioData(short[] shorts) {}

            @Override
            public void onStop() {
                Timber.tag(TAG).e("onStop");
            }

            @Override
            public void onError(DuWakeupError duWakeupError) {
                Timber.tag(TAG).e("onError：" + duWakeupError.message);
            }

            @Override
            public void onRelease() {
                Timber.tag(TAG).e("onRelease");
            }
        });
    }

    private void startDuWakeupEngine(DuWakeup duWakeup) {
        try {
            DuWakeupIntent wakeupIntent = new DuWakeupIntent();
            wakeupIntent.wakeupWords = new String[]{"灵犀灵犀"};
            duWakeup.start(wakeupIntent);
            wakeupEngineRunning = true;
            Timber.tag(TAG).d("DuWakeup engine started");
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Failed to start DuWakeup engine");
            wakeupEngineRunning = false;
        }
    }

    private void pauseWakeupEngine() {
        listenModePauseDepth++;
        if (listenModePauseDepth > 1) {
            Timber.tag(TAG).d("DuWakeup pause depth=%d", listenModePauseDepth);
            return;
        }
        DuWakeup duWakeup = DuWakeup.getInstance();
        if (duWakeup == null || !wakeupEngineRunning) {
            Timber.tag(TAG).d("DuWakeup pause skipped, engine not running");
            return;
        }
        try {
            duWakeup.stop();
            wakeupEngineRunning = false;
            Timber.tag(TAG).d("DuWakeup paused for listen mode");
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Failed to pause DuWakeup");
        }
    }

    private void resumeWakeupEngine() {
        if (listenModePauseDepth <= 0) {
            Timber.tag(TAG).d("DuWakeup resume ignored, pause depth=0");
            return;
        }
        listenModePauseDepth--;
        if (listenModePauseDepth > 0) {
            Timber.tag(TAG).d("DuWakeup resume deferred, pause depth=%d", listenModePauseDepth);
            return;
        }
        DuWakeup duWakeup = DuWakeup.getInstance();
        if (duWakeup == null) {
            Timber.tag(TAG).e("DuWakeup instance is null on resume");
            return;
        }
        if (!wakeupInitialized) {
            ensureWakeupEngineStarted();
            return;
        }
        if (wakeupEngineRunning) {
            return;
        }
        startDuWakeupEngine(duWakeup);
        Timber.tag(TAG).d("DuWakeup resumed after listen mode");
    }

    /**
     * 判断APP是否在前台运行（优化：避免空指针）
     */
    private boolean isAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null || appProcesses.isEmpty()) {
            return false;
        }

        String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && packageName.equals(appProcess.processName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 调起APP至前台（不重建任务栈）
     */
    private void launchAppToForeground(Context context,String content) {
        try {
            Intent intent = new Intent();
            Timber.tag(TAG).d("当前activity%s", IYAApplication.getInstance().getCurrentActivity());
            intent.setClass(context, WakeVoiceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("formFloatContent", content);

            ActivityOptions options = ActivityOptions.makeCustomAnimation(
                    context,
                    R.anim.wake_enter,
                    R.anim.wake_exit
            );

            context.startActivity(intent, options.toBundle());
        } catch (Exception e) {
            Timber.tag(TAG).e("bring to front failed%s", e.getMessage());
        }
    }

    private void initWakeUp() {
        Timber.tag(TAG).e("DuWakeup initWakeUp");
        DuWakeup duWakeup = DuWakeup.getInstance();
        if (duWakeup == null) {
            Timber.tag(TAG).e("DuWakeup instance is null during init.");
            return;
        }
        duWakeup.setLogLevel(Log.INFO);
        WakeupInitConfig cfg = new WakeupInitConfig();
        cfg.wakeConfig.wakeThreshold = 0.90f;
        cfg.wakeConfig.wakeInterval = 1900;
        cfg.wakeConfig.detectionWindowsFrames = 40;
        cfg.wakeConfig.thresholdFramesCount = 1;
        duWakeup.init(IYAApplication.getAppContext(), cfg);
    }

    /**
     * 创建通知渠道（IMPORTANCE_HIGH 是必须的）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "唤醒服务",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("语音唤醒后台服务");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * 创建前台服务通知（保活核心）
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("语音唤醒服务运行中")
                .setContentText("正在监听唤醒词...")
                .setSmallIcon(R.drawable.app_logo)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // ⚠️ 改为 HIGH（原为 LOW）
                .setOngoing(true)
                .setShowWhen(false)
                .setSilent(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DuWakeup duWakeup = DuWakeup.getInstance();
        if (duWakeup != null) {
            try {
                duWakeup.stop();
            } catch (Exception e) {
                Timber.tag(TAG).e("Failed to stop DuWakeup." + e);
            }
            try {
                duWakeup.release();
            } catch (Exception e) {
                Timber.tag(TAG).e("Failed to release DuWakeup." + e);
            }
        }
        wakeupInitialized = false;
        wakeupEngineRunning = false;
        listenModePauseDepth = 0;
        FloatWindowHelper.hideFloatWindow();
        if (mainHandler != null){
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
    private void playTTS(String text,boolean isShowFloat) {
        TTSManager.Companion.getInstance().stop();
        try {
           // TTSManager ttsManager = TTSManager.Companion.getInstance();
            TTSManager.getInstance().setOnPlayerListener(new OnPlayerListener() {
                @Override
                public void playerStart() {
                    Timber.tag(TAG).e("播放TTS开始: %s", text);

                }

                @Override
                public void playerStop() {
                    Timber.tag(TAG).e("播放TTS结束: %s", text);
                    if (!isShowFloat){
                        return;
                    }
                   showFloatView();
                }
            });
            TtsMediaPlayer.getInstance().playRawSound(R.raw.wakeup,this);
//            if (ttsManager != null) {
//                ttsManager.playTTS(text);
//            }

            TrackerUtils.trackWakeUpEvent();


        } catch (Exception e) {
            Timber.tag(TAG).e("播放TTS失败: %s", e.getMessage());
        }
    }

    private final WakeVoiceCallback callback = FloatWindowHelper.createDefaultCallback(this);

    private void showFloatView(){
        if (!AuthHelper.getInstance().isLogin()) {
            WakeupProxyFloatWindow.Companion.getInstance(this).showAndJump();
            return;
        }

        // ====================== 全部通过 helper 显示 ======================
        FloatWindowHelper.showFloatWindow(this, callback);
    }

    /**
     * 是否息屏
     * @return true
     */
    private boolean isKillScreen(){
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        boolean isScreenOff;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            isScreenOff = !pm.isInteractive();
        } else {
            isScreenOff = !pm.isScreenOn();
        }

        return isScreenOff;
    }

    /**
     * 判断是否锁屏
     *
     * @return
     */
    private boolean isLocked() {
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean isLocked = km.isKeyguardLocked();
        boolean isSecureLocked = km.isKeyguardSecure();
        return isLocked;
    }
}