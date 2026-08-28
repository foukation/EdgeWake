package com.fxzs.lingxiagent.model.preview;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.model.chat.callback.StsCallback;
import com.fxzs.lingxiagent.util.ZUtil.SessionUpload;
import com.google.gson.JsonObject;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class FilePreviewRepository {

    private static final String TAG = "FilePreviewRepository";

    public interface PreviewCallback {
        void onProgress(String status);
        void onSuccess(String url);
        void onError(String message);
    }

    private final CompositeDisposable disposables = new CompositeDisposable();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void generatePreview(Context context, String downloadUrl, PreviewCallback callback) {
        Timber.tag(TAG).d("Generating preview for URL: %s", downloadUrl);

        new Thread(() -> {
            // 1. Download the file
            mainHandler.post(() -> callback.onProgress("正在下载文件..."));
            File downloadedFile = downloadFile(context, downloadUrl);
            if (downloadedFile == null) {
                mainHandler.post(() -> callback.onError("下载转换结果失败"));
                return;
            }
            Timber.tag(TAG).d("File downloaded to: %s", downloadedFile.getAbsolutePath());

            // 2. Re-upload the file
            mainHandler.post(() -> callback.onProgress("正在上传文件..."));
            SessionUpload.upload(context, downloadedFile.getAbsolutePath(),SessionUpload.getUrlFileSuffix(downloadUrl), new StsCallback() {
                @Override
                public void progress(long percent) {
                    mainHandler.post(() -> callback.onProgress("正在上传: " + percent + "%"));
                }

                @Override
                public void callback(String newUrl) {
                    if (newUrl == null || newUrl.isEmpty()) {
                        Timber.tag(TAG).e("Re-upload finished but URL is empty.");
                        mainHandler.post(() -> callback.onError("重新上传文件失败"));
                        return;
                    }
                    Timber.tag(TAG).d("Re-upload success: %s", newUrl);

                    // 3. Get File ID
                    mainHandler.post(() -> callback.onProgress("正在获取预览链接..."));
                    callback.onSuccess(newUrl);
//                    getFileId(newUrl, callback);
                }

                @Override
                public void error(@Nullable CosXmlClientException clientException, @Nullable CosXmlServiceException serviceException) {
                    String errorMsg = "重新上传文件失败";
                    if (clientException != null) {
                        errorMsg += ": " + clientException.getMessage();
                    }
                    if (serviceException != null) {
                        errorMsg += ": " + serviceException.getMessage();
                    }
                    Timber.tag(TAG).e(errorMsg);
                    String finalErrorMsg = errorMsg;
                    mainHandler.post(() -> callback.onError(finalErrorMsg));
                }
            });
        }).start();
    }

    //分页预览，用于下载pdf临时预览，所以下载下来就好了
    public void generatePreviewPage(Context context, String downloadUrl, PreviewCallback callback) {
        Timber.tag(TAG).d("Generating preview for URL: %s", downloadUrl);

        new Thread(() -> {
            // 1. Download the file
            mainHandler.post(() -> callback.onProgress("正在下载文件..."));
            File downloadedFile = downloadFile(context, downloadUrl);
            if (downloadedFile == null) {
                mainHandler.post(() -> callback.onError("下载转换结果失败"));
                return;
            }
            Timber.tag(TAG).d("File downloaded to: %s", downloadedFile.getAbsolutePath());
            callback.onSuccess(downloadedFile.getAbsolutePath());
            // 2. Re-upload the file
//            mainHandler.post(() -> callback.onProgress("正在上传文件..."));
//            SessionUpload.upload(context, downloadedFile.getAbsolutePath(), new StsCallback() {
//                @Override
//                public void progress(long percent) {
//                    mainHandler.post(() -> callback.onProgress("正在上传: " + percent + "%"));
//                }
//
//                @Override
//                public void callback(String newUrl) {
//                    if (newUrl == null || newUrl.isEmpty()) {
//                        Timber.tag(TAG).e("Re-upload finished but URL is empty.");
//                        mainHandler.post(() -> callback.onError("重新上传文件失败"));
//                        return;
//                    }
//                    Timber.tag(TAG).d("Re-upload success: %s", newUrl);
//
//                    // 3. Get File ID
//                    mainHandler.post(() -> callback.onProgress("正在获取预览链接..."));
//                    callback.onSuccess(newUrl);
////                    getFileId(newUrl, callback);
//                }
//
//                @Override
//                public void error(@Nullable CosXmlClientException clientException, @Nullable CosXmlServiceException serviceException) {
//                    String errorMsg = "重新上传文件失败";
//                    if (clientException != null) {
//                        errorMsg += ": " + clientException.getMessage();
//                    }
//                    if (serviceException != null) {
//                        errorMsg += ": " + serviceException.getMessage();
//                    }
//                    Timber.tag(TAG).e(errorMsg);
//                    String finalErrorMsg = errorMsg;
//                    mainHandler.post(() -> callback.onError(finalErrorMsg));
//                }
//            });
        }).start();
    }

    public File downloadFile(Context context, String url) {
        try {
            Request request = new Request.Builder().url(url).build();
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful() || response.body() == null) {
                Timber.tag(TAG).e("Download failed: %s", response.message());
                return null;
            }

            File dir = new File(context.getCacheDir(), "wps_results");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            Timber.tag(TAG).e("downloadFile url : %s",url);
            String fileName;
            try {
                int lastSlash = url.lastIndexOf('/');
                int question = url.indexOf('?');
                if (lastSlash >= 0) {
                    if (question > lastSlash) {
                        // 形如 .../xxx.docx?xxx
                        fileName = url.substring(lastSlash + 1, question);
                    } else {
                        // 无 query 参数，直接取最后一段
                        fileName = url.substring(lastSlash + 1);
                    }
                } else {
                    // 没有 '/'，整体当作文件名
                    fileName = url;
                }
            } catch (Exception e) {
                Timber.tag(TAG).e(e, "parse fileName failed, fallback to default name");
                fileName = "downloaded_file";
            }
            File file = new File(dir, fileName);

            try (InputStream in = response.body().byteStream();
                 OutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
            return file;
        } catch (SocketTimeoutException e) {
            Timber.tag(TAG).e(e, "Download timeout");
            return null;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Download exception");
            return null;
        }
    }

    private void getFileId(String fileUrl, PreviewCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("fileUrl", fileUrl);

        disposables.add(PreviewApiClient.getApiService().getFileId(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    Timber.tag(TAG).d("getFileId response: %s", response);
                    if (response != null && response.has("data")) {
                        JsonObject data = response.getAsJsonObject("data");
                        if (data.has("fileId")) {
                            String fileId = data.get("fileId").getAsString();
                            Timber.tag(TAG).d("Got fileId: %s", fileId);
                            callback.onSuccess(fileId);
                            return;
                        }
                    }
                    callback.onError("获取预览链接失败");
                }, throwable -> {
                    Timber.tag(TAG).e(throwable, "getFileId failed");
                    callback.onError("获取预览链接失败: " + throwable.getMessage());
                }));
    }

    public void cancel() {
        disposables.clear();
    }
}
