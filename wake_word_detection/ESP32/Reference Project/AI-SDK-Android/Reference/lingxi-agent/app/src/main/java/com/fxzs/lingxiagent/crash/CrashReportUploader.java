package com.fxzs.lingxiagent.crash;

import android.content.Context;

import androidx.annotation.WorkerThread;

import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

/**
 * 负责：在应用下次启动时，逐个读取本地崩溃文件并 POST 上报
 */
public final class CrashReportUploader {

    private static final String TAG = "CrashReportUploader";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();

    public void uploadAll(Context context) {
        File[] files = CrashLogger.listPendingCrashFiles(context);
        for (File f : files) {
            try {
                String json = readAll(f);
                postJson(json, new Callback() {
                    @Override public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        Timber.tag(TAG).w( "upload failed: " + f.getName() + ", err=" + e.getMessage());
                        // 失败保留文件，等待下次再传
                    }
                    @Override public void onResponse(@NotNull Call call, @NotNull Response response) {
                        if (response.isSuccessful()) {
                            CrashLogger.deleteFileQuietly(f);
                            Timber.tag(TAG).i( "uploaded & deleted: " + f.getName());
                        } else {
                            Timber.tag(TAG).w( "upload non-2xx: " + f.getName() + ", code=" + response.code());
                        }
                        response.close();
                    }
                });
            } catch (Exception e) {
                Timber.tag(TAG).e( "read crash file error: " + f.getName(), e);
            }
        }
    }

    private String readAll(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int read = fis.read(buf);
            return new String(buf, 0, read, StandardCharsets.UTF_8);
        }
    }

    @WorkerThread
    private void postJson(String json, Callback callback) {
        String url = CrashReportConfig.getEndpointUrl();
        RequestBody body = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);

        // 携带用户Token
        String token = SharedPreferencesUtil.getToken();
        if (token != null && !token.isEmpty()) {
            builder.addHeader(Constants.HEADER_AUTHORIZATION, Constants.HEADER_BEARER + token);
        }
        AIServiceManager.Companion.getInstance().getHeaderInfo(builder);
        Request request = builder.build();
        client.newCall(request).enqueue(callback);
    }
}

