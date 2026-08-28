package com.fxzs.lingxiagent.crash;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.BuildConfig;
import com.fxzs.lingxiagent.util.DeviceUUIDGenerator;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责：
 * 1) 捕获未处理异常
 * 2) 序列化崩溃信息并写入本地文件
 */
public final class CrashLogger implements Thread.UncaughtExceptionHandler {

    private static final String CRASH_DIR_NAME = "crash_logs";
    private static final String CRASH_FILE_PREFIX = "crash_";

    private final Context appContext;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    // 事件节流：记录每种 eventType 最近一次落盘时间，避免短时间内重复写入
    private static final ConcurrentHashMap<String, Long> sLastEventTime = new ConcurrentHashMap<>();

    private CrashLogger(Context context) {
        this.appContext = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashLogger(context));
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        try {
            writeCrashToFile(t, e);
        } catch (Throwable ignore) {
        }
        // 交还给系统/原有 handler，保证正常崩溃流程（让系统收尾、重启等）
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
        }
    }

    private void writeCrashToFile(Thread t, Throwable e) {
        writeEventToFile(appContext,
                e.getClass().getSimpleName(),
                e.getMessage() == null ? "" : e.getMessage(),
                getStackTraceString(e));
    }

    public static void logEvent(Context context, String eventType, String message, String stackTrace) {
        writeEventToFile(context.getApplicationContext(), eventType, message, stackTrace);
    }

    private static void writeEventToFile(Context context, String eventType, String message, String stackTrace) {
        // 节流：同一 eventType 在最小上报间隔内仅记录一次
        final String key = eventType == null ? "UNKNOWN" : eventType;
        long now = System.currentTimeMillis();
        Long last = sLastEventTime.get(key);
        if (last != null && now - last < CrashReportConfig.getMinEventReportIntervalMs()) {
            return;
        }
        sLastEventTime.put(key, now);

        File dir = new File(context.getFilesDir(), CRASH_DIR_NAME);
        if (!dir.exists()) dir.mkdirs();

        String time = ZonedDateTime.now(ZoneOffset.ofHours(8))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String fileName = CRASH_FILE_PREFIX + System.currentTimeMillis() + ".json";
        File file = new File(dir, fileName);

        try (FileWriter fw = new FileWriter(file)) {
            JSONObject obj = new JSONObject();
            obj.put("logContent", stackTrace == null ? "" : stackTrace);
            obj.put("deviceId", DeviceUUIDGenerator.getDeviceUUID(context));
            obj.put("deviceModel", Build.MODEL);
            obj.put("deviceBrand", Build.BRAND);
            obj.put("osVersion", "Android " + Build.VERSION.RELEASE);
            obj.put("appVersion", BuildConfig.VERSION_NAME);
            obj.put("crashTime", time);
            obj.put("stackTrace", stackTrace == null ? "" : stackTrace);
            obj.put("errorMessage", (eventType == null ? "" : eventType) + (message == null || message.isEmpty() ? "" : (": " + message)));
            obj.put("eventType", key);
            fw.write(obj.toString());
            fw.flush();
        } catch (Exception ex) {
            // 忽略
        }

        // 保留数量控制：超出最大数量则删除最旧的
        enforceRetention(dir);
    }

    private static void enforceRetention(File dir) {
        try {
            File[] files = dir.listFiles(pathname -> pathname.isFile() && pathname.getName().startsWith(CRASH_FILE_PREFIX) && pathname.getName().endsWith(".json"));
            if (files == null) return;
            int max = CrashReportConfig.getMaxRetainedFiles();
            if (files.length <= max) return;
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            int toDelete = files.length - max;
            for (int i = 0; i < toDelete; i++) {
                deleteFileQuietly(files[i]);
            }
        } catch (Throwable ignore) {}
    }

    private static String getStackTraceString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append('\n');
        for (StackTraceElement el : e.getStackTrace()) {
            sb.append("    at ")
              .append(el.getClassName()).append('.')
              .append(el.getMethodName())
              .append('(').append(el.getFileName()).append(':').append(el.getLineNumber()).append(')')
              .append('\n');
        }
        Throwable cause = e.getCause();
        while (cause != null) {
            sb.append("Caused by: ").append(cause).append('\n');
            for (StackTraceElement el : cause.getStackTrace()) {
                sb.append("    at ")
                  .append(el.getClassName()).append('.')
                  .append(el.getMethodName())
                  .append('(').append(el.getFileName()).append(':').append(el.getLineNumber()).append(')')
                  .append('\n');
            }
            cause = cause.getCause();
        }
        return sb.toString();
    }

    public static File[] listPendingCrashFiles(Context context) {
        File dir = new File(context.getFilesDir(), CRASH_DIR_NAME);
        File[] files = dir.listFiles(pathname -> pathname.isFile() && pathname.getName().startsWith(CRASH_FILE_PREFIX) && pathname.getName().endsWith(".json"));
        return files != null ? files : new File[0];
    }

    public static boolean deleteFileQuietly(File f) {
        try {
            return f != null && f.exists() && f.delete();
        } catch (Throwable ignore) {
            return false;
        }
    }
}

