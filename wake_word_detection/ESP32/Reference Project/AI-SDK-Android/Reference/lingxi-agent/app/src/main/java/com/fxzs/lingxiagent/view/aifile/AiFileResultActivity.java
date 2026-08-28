package com.fxzs.lingxiagent.view.aifile;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.ppt.PptPreviewActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class AiFileResultActivity extends BaseActivity<AiFileToolViewModel> {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    public static final String TAG = "AiFileResultActivity";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_IMAGE_URLS = "extra_image_urls";

    private WebView webView;
    private String currentUrl;
    private String currentTitle;
    private ArrayList<String> imageUrls;
    private ExecutorService executorService;
    private boolean downloadInProgress = false;
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ai_file_result;
    }

    @Override
    protected Class<AiFileToolViewModel> getViewModelClass() {
        return AiFileToolViewModel.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
        currentTitle = getIntent().getStringExtra(EXTRA_TITLE);
        currentUrl = getIntent().getStringExtra(EXTRA_URL);
        imageUrls = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS);

        Timber.tag(TAG).w("url = " + currentUrl);
        // Setup Header
        View back = findViewById(R.id.back);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }
        TextView tvTitle = findViewById(R.id.tv_header_title);
        if (tvTitle != null) {
            tvTitle.setText(currentTitle != null ? currentTitle : "转换结果");
        }

        executorService = Executors.newSingleThreadExecutor();

        findViewById(R.id.download_button).setOnClickListener(v -> checkPermissionAndDownload());

         webView = findViewById(R.id.webview);
        RecyclerView recyclerView = findViewById(R.id.rv_images);
        setupWebView();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            // Display images
            webView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new AiFileResultImageAdapter(imageUrls));
        } else if (currentUrl != null && !currentUrl.isEmpty()) {
            // Display single document URL
            webView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            webView.getSettings().setJavaScriptEnabled(true);

            String finalUrl = buildOnlinePreviewUrl(currentUrl);
            Timber.tag(TAG).d("Loading preview URL: %s", finalUrl);
            webView.loadUrl(finalUrl);

        } else {
            showToast("未找到可预览的内容");
            finish();
        }
    }

    @Override
    protected void setupObservers() {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWhiteStatusBar();
    }

    private String getFileTypeFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        String lowerCaseUrl = url.toLowerCase();
        if (lowerCaseUrl.endsWith(".pdf")) {
            return "pdf";
        } else if (lowerCaseUrl.endsWith(".doc") || lowerCaseUrl.endsWith(".docx")) {
            return "word";
        } else if (lowerCaseUrl.endsWith(".ppt") || lowerCaseUrl.endsWith(".pptx")) {
            return "ppt";
        } else if (lowerCaseUrl.endsWith(".xls") || lowerCaseUrl.endsWith(".xlsx")) {
            return "excel";
        }
        return null;
    }

    private String buildOnlinePreviewUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        try {
            String token = SharedPreferencesUtil.getToken();
            if (token == null) {
                token = "";
            }

            String encodedUrl = URLEncoder.encode(url, "UTF-8");
            String encodedToken = URLEncoder.encode(token, "UTF-8");
            String fileType = getFileTypeFromUrl(url);

            StringBuilder previewUrl = new StringBuilder("https://mobile-web.jmkjsh.com/#/previewApp?");
            previewUrl.append("url=").append(encodedUrl);
            previewUrl.append("&token=").append(encodedToken);
            if (fileType != null) {
                previewUrl.append("&type=").append(fileType);
            }

            String finalUrl = previewUrl.toString();
            Timber.tag(TAG).d("Built preview URL: " + finalUrl);
            return finalUrl;
        } catch (UnsupportedEncodingException e) {
            Timber.tag(TAG).e(e, "Failed to build preview URL");
            return url; // Fallback to original URL
        }
    }


    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // 启用JavaScript
        webSettings.setJavaScriptEnabled(true);

        // 启用缩放
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // 启用DOM存储
        webSettings.setDomStorageEnabled(true);

        // 设置User-Agent
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " LingxiAgent/1.0");

        // 设置WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Timber.tag("PptPreview").d( "开始加载页面: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Timber.tag("PptPreview").d( "页面加载完成: " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Timber.tag("PptPreview").e( "WebView加载失败: " + description);
                GlobalToast.show(AiFileResultActivity.this, "加载失败: " + description, GlobalToast.Type.ERROR);
//                showErrorMessage("加载失败: " + description);
            }
        });

        // 设置WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                Timber.tag("PptPreview").d( "加载进度: " + newProgress + "%");
            }
        });
    }

    private void checkPermissionAndDownload() {
        if ((currentUrl == null || currentUrl.isEmpty()) && (imageUrls == null || imageUrls.isEmpty())) {
            GlobalToast.show(this, "下载链接不可用", GlobalToast.Type.ERROR);
            return;
        }
        if (downloadInProgress) {
            GlobalToast.show(this, "正在下载中，请稍候", GlobalToast.Type.NORMAL);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startDownload();
        } else {
            if (hasStoragePermission()) {
                startDownload();
            } else {
                requestStoragePermission();
            }
        }
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        AppPermissionRequestManager.requestExternalStoragePermission(this, PERMISSION_REQUEST_CODE,
                "请授权手机存储，以便完成文件下载功能");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload();
            } else {
                GlobalToast.show(this, "需要存储权限才能下载文件", GlobalToast.Type.ERROR);
            }
        }
    }

    private void startDownload() {
        downloadInProgress = true;
        findViewById(R.id.download_button).setEnabled(false);
        findViewById(R.id.download_button).setAlpha(0.5f);
        GlobalToast.show(this, "正在下载文件...", GlobalToast.Type.NORMAL);

        executorService.execute(() -> {
            try {
                if (imageUrls != null && !imageUrls.isEmpty()) {
                    downloadImages(imageUrls, currentTitle != null ? currentTitle : "图片");
                } else {
                    downloadFile(currentUrl, currentTitle != null ? currentTitle : "文件");
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    downloadInProgress = false;
                    findViewById(R.id.download_button).setEnabled(true);
                    findViewById(R.id.download_button).setAlpha(1.0f);
                    GlobalToast.show(this, "下载失败: " + e.getMessage(), GlobalToast.Type.ERROR);
                });
            }
        });
    }

    private void downloadFile(String fileUrl, String baseName) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        String fileName = baseName + getFileExtensionFromUrl(fileUrl);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadWithMediaStore(connection, fileName);
        } else {
            downloadWithLegacyStorage(connection, fileName);
        }
    }

    private void downloadImages(ArrayList<String> urls, String baseName) throws IOException {
        int index = 1;
        for (String url : urls) {
            String name = baseName + "_" + index + getFileExtensionFromUrl(url);
            downloadImageToGallery(url, name);
            index++;
        }
        runOnUiThread(() -> {
            downloadInProgress = false;
            findViewById(R.id.download_button).setEnabled(true);
            findViewById(R.id.download_button).setAlpha(1.0f);
            GlobalToast.show(this, "图片已保存到相册", GlobalToast.Type.SUCCESS);
        });
    }

    private void downloadImageToGallery(String fileUrl, String fileName) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadImageWithMediaStore(connection, fileName);
        } else {
            downloadImageWithLegacyStorage(connection, fileName);
        }
    }

    private String getFileExtensionFromUrl(String url) {
        if (url == null) return "";
        int lastSlash = url.lastIndexOf('/');
        String name = lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
        int q = name.indexOf('?');
        if (q >= 0) {
            name = name.substring(0, q);
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private void downloadWithMediaStore(HttpURLConnection connection, String fileName) throws IOException {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, guessMimeType(fileName));
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LingXi_File");

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (InputStream input = connection.getInputStream();
                 OutputStream output = resolver.openOutputStream(uri)) {
                if (output != null) {
                    copyStream(input, output);
                    runOnUiThread(() -> {
                        downloadInProgress = false;
                        findViewById(R.id.download_button).setEnabled(true);
                        findViewById(R.id.download_button).setAlpha(1.0f);
                        showDownloadSuccessDialog("MEDIASTORE:/LingXi_File", fileName);
                    });
                } else {
                    throw new IOException("无法打开MediaStore输出流");
                }
            }
        } else {
            throw new IOException("无法创建MediaStore条目");
        }
    }

    private void downloadImageWithMediaStore(HttpURLConnection connection, String fileName) throws IOException {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LingXi_File");

        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (InputStream input = connection.getInputStream();
                 OutputStream output = resolver.openOutputStream(uri)) {
                if (output != null) {
                    copyStream(input, output);
                } else {
                    throw new IOException("无法打开相册输出流");
                }
            }
        } else {
            throw new IOException("无法创建相册条目");
        }
    }

    private void downloadWithLegacyStorage(HttpURLConnection connection, String fileName) throws IOException {
        File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LingXi_File");
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        File outputFile = new File(downloadDir, fileName);
        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(outputFile)) {
            copyStream(input, output);
            if (outputFile.exists() && outputFile.length() > 0) {
                runOnUiThread(() -> {
                    downloadInProgress = false;
                    findViewById(R.id.download_button).setEnabled(true);
                    findViewById(R.id.download_button).setAlpha(1.0f);
                    showDownloadSuccessDialog(outputFile.getParent(), fileName);
                });
            } else {
                throw new IOException("文件下载失败或文件为空");
            }
        }
    }

    private void downloadImageWithLegacyStorage(HttpURLConnection connection, String fileName) throws IOException {
        File pictureDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "LingXi_File");
        if (!pictureDir.exists()) {
            pictureDir.mkdirs();
        }

        File outputFile = new File(pictureDir, fileName);
        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(outputFile)) {
            copyStream(input, output);
            if (!outputFile.exists() || outputFile.length() <= 0) {
                throw new IOException("图片下载失败或文件为空");
            }
        }
    }

    private void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
    }

    private String guessMimeType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }

    private void showDownloadSuccessDialog(String downloadPath, String fileName) {
        String message;
        if (downloadPath.startsWith("MEDIASTORE:")) {
            message = "文件已保存到:\nDownloads/LingXi_File/" + fileName + "\n\n是否立即打开文件？";
        } else {
            message = "文件已保存到:\n" + fileName + "\n\n是否立即打开文件？";
        }

        new CommonDialog.Builder(this)
                .setTitle("下载成功")
                .setMessage(message)
                .setConfirmText("打开")
                .setCancelText("稍后")
                .setOnClickListener(new CommonDialog.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        openFile(downloadPath, fileName);
                    }

                    @Override
                    public void onCancel() {
                        if (downloadPath.startsWith("MEDIASTORE:")) {
                            GlobalToast.show(AiFileResultActivity.this,
                                    "文件已保存到 Downloads/LingXi_File 文件夹",
                                    GlobalToast.Type.SUCCESS);
                        } else {
                            GlobalToast.show(AiFileResultActivity.this,
                                    "文件已保存到 " + downloadPath + "/" + fileName,
                                    GlobalToast.Type.SUCCESS);
                        }
                    }
                })
                .show();
    }

    private void openFile(String downloadPath, String fileName) {
        try {
            if (downloadPath.startsWith("MEDIASTORE:")) {
                if (openFileWithMediaStore(fileName)) return;
                openDownloadFolderWithSAF();
            } else {
                File file = new File(downloadPath, fileName);
                if (file.exists()) {
                    openFileDirectly(file);
                } else {
                    GlobalToast.show(this, "文件不存在，正在打开下载文件夹", GlobalToast.Type.NORMAL);
                    openDownloadFolderLegacy(downloadPath);
                }
            }
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "打开文件失败");
            GlobalToast.show(this, "文件已保存到：" + (downloadPath.startsWith("MEDIASTORE:") ? "Downloads/LingXi_File/" + fileName : downloadPath + "/" + fileName), GlobalToast.Type.NORMAL);
        }
    }

    private boolean openFileWithMediaStore(String fileName) {
        try {
            Uri queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME};
            String selection = MediaStore.Downloads.DISPLAY_NAME + "=? AND " +
                    MediaStore.Downloads.RELATIVE_PATH + " LIKE ?";
            String[] selectionArgs = {fileName, "%LingXi_File%"};

            android.database.Cursor cursor = getContentResolver().query(
                    queryUri, projection, selection, selectionArgs,
                    MediaStore.Downloads.DATE_ADDED + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID));
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                cursor.close();

                String mimeType = guessMimeType(fileName);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(intent, "打开文件"));
                    return true;
                }
            } else {
                if (cursor != null) cursor.close();
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType(guessMimeType(fileName));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "选择应用打开文件"));
                return true;
            }
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "MediaStore 打开文件失败");
        }
        return false;
    }

    private void openFileDirectly(File file) {
        try {
            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", file);
            } else {
                fileUri = Uri.fromFile(file);
            }

            String mimeType = guessMimeType(file.getName());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "打开文件"));
            } else {
                GlobalToast.show(this, "没有找到可以打开此文件的应用", GlobalToast.Type.ERROR);
                openDownloadFolderLegacy(file.getParent());
            }
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "直接打开文件失败");
            GlobalToast.show(this, "打开文件失败，请手动查找文件", GlobalToast.Type.ERROR);
        }
    }

    private void openDownloadFolderWithSAF() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Uri initialUri = Uri.parse(
                        "content://com.android.externalstorage.documents/document/primary:Download%2FLingXi_File"
                );
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
            }
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                GlobalToast.show(this, "请在文件管理器中查找 Downloads/LingXi_File 文件夹", GlobalToast.Type.NORMAL);
            }
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "打开下载文件夹失败");
            GlobalToast.show(this, "请在文件管理器中查找 Downloads/LingXi_File 文件夹", GlobalToast.Type.NORMAL);
        }
    }

    private void openDownloadFolderLegacy(String downloadPath) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri folderUri = Uri.parse("file://" + downloadPath);
            intent.setDataAndType(folderUri, "resource/folder");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Intent genericIntent = new Intent(Intent.ACTION_VIEW);
                genericIntent.setType("*/*");
                if (genericIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(genericIntent, "选择文件管理器"));
                } else {
                    GlobalToast.show(this, "请手动查找文件：" + downloadPath, GlobalToast.Type.NORMAL);
                }
            }
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "打开下载文件夹失败");
            GlobalToast.show(this, "请手动查找文件：" + downloadPath, GlobalToast.Type.NORMAL);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (webView != null) {
//            webView.onDetachedFromWindow();
            webView.destroy();
        }
    }

}
