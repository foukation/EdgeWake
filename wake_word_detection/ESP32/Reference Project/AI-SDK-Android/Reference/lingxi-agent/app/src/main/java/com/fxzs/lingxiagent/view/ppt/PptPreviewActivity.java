package com.fxzs.lingxiagent.view.ppt;

import static com.fxzs.lingxiagent.model.common.Constants.PPT_PREVIEW;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.cmdc.ai.assist.constraint.AIAssistConfig;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.PptLifecycleManager;
import com.fxzs.lingxiagent.util.PptStateManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.view.FixTouchWebView;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.viewmodel.ppt.VMPptPreview;

import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import timber.log.Timber;

/**
 * PPT预览Activity - 使用WebView预览
 */
public class PptPreviewActivity extends BaseActivity<VMPptPreview> {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private ImageView backButton;
    private ImageView downloadButton;
    private ImageView closeButton;
    private TextView titleText;
    private FixTouchWebView pptWebView;
    
    private String pptId;
    private int sessionId;
    private String taskId;
    private PptStateManager stateManager;
    private PptLifecycleManager lifecycleManager;

    private final Handler saveHandler = new Handler(Looper.getMainLooper());
    private final Runnable saveActionRunnable1s = this::saveAction;
    private final Runnable saveActionRunnable5s = this::saveAction;
    private final Runnable saveActionRunnable10s = this::saveAction;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置沉浸式状态栏 - 参考会议页面样式
        setupImmersiveStatusBar();

        // 初始化管理器
        stateManager = PptStateManager.getInstance(this);
        lifecycleManager = new PptLifecycleManager(this);
        
        // 从Intent或状态管理器获取数据
        pptId = getIntent().getStringExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_PPT_ID);
        String pptUrl = getIntent().getStringExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_PPT_URL);
        String topic = getIntent().getStringExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_TOPIC);
        sessionId = getIntent().getIntExtra("session_id", 0);
        taskId = getIntent().getStringExtra("task_id");

        // 从状态管理器获取当前项目信息
        com.fxzs.lingxiagent.model.ppt.dto.PptProject currentProject = stateManager.getCurrentProject();
        if (currentProject != null) {
            if (pptId == null) {
                pptId = currentProject.getId();
            }
            if (pptUrl == null) {
                pptUrl = currentProject.getPptUrl();
            }
            if (topic == null) {
                topic = currentProject.getTopic();
            }
        }
        
        Timber.tag("PptPreview").d( "初始化 - pptId: " + pptId + ", pptUrl: " + pptUrl + ", topic: " + topic + ", sessionId: " + sessionId + ", taskId: " + taskId);
        
        setupWebView();

        // 进入页面先换取WPS文件ID，供下载时获取真实下载地址
        viewModel.fetchWpsFileId(pptUrl);

        // 加载PPT数据
        viewModel.loadPptData(pptId, pptUrl, topic, sessionId, taskId);
        saveAction();
        scheduleSaveActions();
    }
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ppt_preview;
    }
    
    @Override
    protected Class<VMPptPreview> getViewModelClass() {
        return VMPptPreview.class;
    }

    @Override
    protected void setupDataBinding() {
        // 简化版本不需要复杂的数据绑定
    }
    
    @Override
    protected void initializeViews() {
        backButton = findViewById(R.id.back_button);
        downloadButton = findViewById(R.id.download_button);
        closeButton = findViewById(R.id.close_button);
        titleText = findViewById(R.id.title_text);
        pptWebView = findViewById(R.id.ppt_webview);

        setupClickListeners();
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        downloadButton.setOnClickListener(v -> {
            Timber.tag("PptPreview").d("点击下载按钮");

            // saveFile 不是固定存在于页面全局作用域，先探测再调用
//            pptWebView.evaluateJavascript("(function(){return typeof saveFile === 'function';})()", value -> {
//                Timber.tag("PptPreview").d("saveFile函数探测结果: " + value);
//                if ("true".equals(value)) {
//                    pptWebView.evaluateJavascript("saveFile('')", jsResult ->{
//                                Timber.tag("PptPreview").d("saveFile调用结果: " + jsResult);
//                                checkPermissionAndDownload();
//                            }
//                            );
//                }
////                else {
////                    // H5未暴露saveFile时，回退到原生下载流程
////                    checkPermissionAndDownload();
////                }
//            });


//            pptWebView.loadUrl("javascript:saveFile()");
//                pptWebView.post(() -> pptWebView.evaluateJavascript("saveFile('')", jsResult ->
//                        Timber.tag("PptPreview").d("callback触发saveFile结果: " + jsResult)));

            pptWebView.evaluateJavascript("saveFile('')", jsResult ->{
                        Timber.tag("PptPreview").d("saveFile调用结果: " + jsResult);
                        checkPermissionAndDownload();
                    }
            );

        });
        
        closeButton.setOnClickListener(v -> {
            Timber.tag("PptPreview").d( "点击关闭按钮");
            finish();
        });
    }
    
    /**
     * 检查权限并下载
     */
    private void checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 不需要申请存储权限，直接下载
            viewModel.downloadPpt(this);
        } else {
            // Android 9及以下需要检查存储权限
            if (hasStoragePermission()) {
                viewModel.downloadPpt(this);
            } else {
                requestStoragePermission();
            }
        }
    }
    
    /**
     * 检查是否有存储权限
     */
    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this, 
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 请求存储权限
     */
    private void requestStoragePermission() {
//        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//            // 用户之前拒绝过权限，显示解释
//            new AlertDialog.Builder(this)
//                    .setTitle("需要存储权限")
//                    .setMessage("为了能够下载PPT文件，我们需要访问存储的权限。")
//                    .setPositiveButton("授权", (dialog, which) -> {
//                        ActivityCompat.requestPermissions(this,
//                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                                PERMISSION_REQUEST_CODE);
//                    })
//                    .setNegativeButton("取消", null)
//                    .create()
//                    .show();
//        } else {
//            // 首次请求权限
//            AppPermissionRequestManager.requestExternalStoragePermission(this, PERMISSION_REQUEST_CODE);
//        }
        AppPermissionRequestManager.requestExternalStoragePermission(this, PERMISSION_REQUEST_CODE,"请授权手机存储，以便完成PPT下载功能");
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，开始下载
                Timber.tag("PptPreview").d( "存储权限被授予");
                viewModel.downloadPpt(this);
            } else {
                // 权限被拒绝
                Timber.tag("PptPreview").d( "存储权限被拒绝");
                GlobalToast.show(this, "需要存储权限才能下载文件", GlobalToast.Type.ERROR);
            }
        }
    }
    
    /**
     * 显示PPT下载成功对话框（参考会议摘要实现）
     */
    private void showPptDownloadSuccessDialog(String downloadPath) {
        // 获取文件名
        String fileName = viewModel.getPptTitle().get() + ".pptx";
        
        // 根据下载路径类型显示不同的提示信息
        String message;
        if (downloadPath.startsWith("MEDIASTORE:")) {
            message = "PPT文件已保存到:\nDownloads/LingXi_PPT/" + fileName + "\n\n是否立即打开文件？";
        } else {
            message = "PPT文件已保存到:\n" + fileName + "\n\n是否立即打开文件？";
        }
        
        new CommonDialog.Builder(this)
                .setTitle("下载成功")
                .setMessage(message)
                .setConfirmText("打开")
                .setCancelText("稍后")
                .setOnClickListener(new CommonDialog.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        // 直接打开PPT文件
                        openPptFile(downloadPath, fileName);
                    }

                    @Override
                    public void onCancel() {
                        // 用户选择稍后，显示文件保存位置提示
                        if (downloadPath.startsWith("MEDIASTORE:")) {
                            GlobalToast.show(PptPreviewActivity.this, 
                                "文件已保存到 Downloads/LingXi_PPT 文件夹", 
                                GlobalToast.Type.SUCCESS);
                        } else {
                            GlobalToast.show(PptPreviewActivity.this, 
                                "文件已保存到 " + downloadPath + "/" + fileName, 
                                GlobalToast.Type.SUCCESS);
                        }
                    }
                })
                .show();
    }
    
    /**
     * 打开PPT文件（优先直接打开文件，失败后导航到文件夹）
     */
    private void openPptFile(String downloadPath, String fileName) {
        try {
            // 检查是否是MediaStore下载（Android 10+）
            if (downloadPath.startsWith("MEDIASTORE:")) {
                // Android 10+ 使用MediaStore下载，无法直接访问文件路径
                // 优先尝试打开文件类型应用
                if (openFileWithMediaStore(fileName)) {
                    return; // 成功打开文件
                }
                // 如果无法打开文件应用，则导航到下载文件夹
                openDownloadFolderWithSAF();
            } else {
                // Android 9及以下或传统存储方式
                File pptFile = new File(downloadPath, fileName);
                Timber.tag("PptPreview").d( "检查传统存储文件存在性: " + pptFile.getAbsolutePath());
                Timber.tag("PptPreview").d( "文件存在: " + pptFile.exists() + ", 可读: " + pptFile.canRead());
                
                if (pptFile.exists()) {
                    Timber.tag("PptPreview").d( "文件存在，尝试直接打开");
                    openFileDirectly(pptFile);
                } else {
                    // 文件不存在，导航到下载文件夹
                    Timber.tag("PptPreview").w( "文件不存在: " + pptFile.getAbsolutePath());
                    GlobalToast.show(this, "文件不存在，正在打开下载文件夹", GlobalToast.Type.NORMAL);
                    openDownloadFolderLegacy(downloadPath);
                }
            }
        } catch (Exception e) {
            
            Timber.tag("PptPreview").e( "打开PPT文件失败"+ e);
            GlobalToast.show(this, "文件已保存到：" + (downloadPath.startsWith("MEDIASTORE:") ? "Downloads/LingXi_PPT/" + fileName : downloadPath + "/" + fileName), GlobalToast.Type.NORMAL);
        }
    }
    
    /**
     * Android 10+ 使用MediaStore打开文件
     */
    private boolean openFileWithMediaStore(String fileName) {
        try {
            // 方法1：尝试直接通过内容URI打开MediaStore中的文件
            Timber.tag("PptPreview").d( "尝试通过MediaStore打开文件: " + fileName);
            
            // 构建查询URI来查找我们刚下载的文件
            Uri queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME};
            String selection = MediaStore.Downloads.DISPLAY_NAME + "=? AND " + 
                             MediaStore.Downloads.RELATIVE_PATH + " LIKE ?";
            String[] selectionArgs = {fileName, "%LingXi_PPT%"};
            
            android.database.Cursor cursor = getContentResolver().query(
                queryUri, projection, selection, selectionArgs, 
                MediaStore.Downloads.DATE_ADDED + " DESC");
                
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID));
                Uri contentUri = android.content.ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                cursor.close();
                
                Timber.tag("PptPreview").d( "找到文件，URI: " + contentUri);
                
                // 尝试通过内容URI打开文件
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, "application/vnd.openxmlformats-officedocument.presentationml.presentation");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(intent, "打开PPT文件"));
                    return true;
                } else {
                    Timber.tag("PptPreview").w("没有找到可以打开PPT文件的应用");
                }
            } else {
                Timber.tag("PptPreview").w("在MediaStore中未找到文件: " + fileName);
                if (cursor != null) cursor.close();
            }
            
            // 方法2：如果上面的方法失败，尝试通过通用方式打开PPT文件类型
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "选择应用打开PPT文件"));
                return true;
            }
            
        } catch (Exception e) {
            Timber.tag("PptPreview").e( "MediaStore打开文件失败"+ e);
        }
        return false;
    }
    
    /**
     * Android 9及以下直接打开文件
     */
    private void openFileDirectly(File pptFile) {
        try {
            Uri fileUri;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ 使用FileProvider
                fileUri = FileProvider.getUriForFile(this, 
                    getPackageName() + ".fileprovider", pptFile);
            } else {
                // Android 6.0及以下使用直接URI
                fileUri = Uri.fromFile(pptFile);
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "打开PPT文件"));
            } else {
                GlobalToast.show(this, "没有找到可以打开PPT文件的应用", GlobalToast.Type.ERROR);
                // 如果没有PPT应用，则打开文件管理器
                openDownloadFolderLegacy(pptFile.getParent());
            }
        } catch (Exception e) {
            Timber.tag("PptPreview").e( "直接打开文件失败"+ e);
            GlobalToast.show(this, "打开文件失败，请手动查找文件", GlobalToast.Type.ERROR);
        }
    }
    
    /**
     * Android 10+ 使用存储访问框架打开下载文件夹
     */
    private void openDownloadFolderWithSAF() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Uri initialUri = Uri.parse(
                        "content://com.android.externalstorage.documents/document/primary:Download%2FLingXi_PPT"
                );
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
            }
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // 如果没有文件管理器，显示提示
                GlobalToast.show(this, "请在文件管理器中查找 Downloads/LingXi_PPT 文件夹", GlobalToast.Type.NORMAL);
            }
        } catch (Exception e) {
            Timber.tag("PptPreview").e( "打开下载文件夹失败"+ e);
            GlobalToast.show(this, "请在文件管理器中查找 Downloads/LingXi_PPT 文件夹", GlobalToast.Type.NORMAL);
        }
    }
    
    /**
     * Android 9及以下打开下载文件夹
     */
    private void openDownloadFolderLegacy(String downloadPath) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri folderUri = Uri.parse("file://" + downloadPath);
            intent.setDataAndType(folderUri, "resource/folder");
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // 尝试使用通用文件管理器
                Intent genericIntent = new Intent(Intent.ACTION_VIEW);
                genericIntent.setType("*/*");
                if (genericIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(genericIntent, "选择文件管理器"));
                } else {
                    GlobalToast.show(this, "请手动查找文件：" + downloadPath, GlobalToast.Type.NORMAL);
                }
            }
        } catch (Exception e) {
            Timber.tag("PptPreview").e( "打开下载文件夹失败"+ e);
            GlobalToast.show(this, "请手动查找文件：" + downloadPath, GlobalToast.Type.NORMAL);
        }
    }

    private void sendDeviceDataToWeb() {
        try {
            AIAssistConfig aiAssistConfig = AIServiceManager.Companion.getInstance().getAiAssistConfig();
            JSONObject json = new JSONObject();

            json.put("deviceId", aiAssistConfig.getDeviceId());
            json.put("deviceNo", aiAssistConfig.getDeviceNo());
            json.put("productId", aiAssistConfig.getProductId());
            json.put("deviceSecret", aiAssistConfig.getDeviceSecret());
            json.put("deviceModel", "pad");
            json.put("mode", 2);

            String script = String.format("window.AppBridge.receiveData(%s);", json);

            pptWebView.evaluateJavascript(script, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupWebView() {
        WebSettings webSettings = pptWebView.getSettings();
        
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

        // 注入JS桥接，H5调用androidBride.saveFile后，在callback里回调前端saveFile
        pptWebView.addJavascriptInterface(new AndroidtoJs(this, name -> {
            if ("saveFile".equals(name)) {

//                pptWebView.loadUrl("javascript:saveFile()");
//                saveAction();
            }
        }), "androidBride");
        
        // 设置WebViewClient
        pptWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Timber.tag("PptPreview").d( "开始加载页面: " + url);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                sendDeviceDataToWeb();
                Timber.tag("PptPreview").d( "页面加载完成: " + url);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Timber.tag("PptPreview").e( "WebView加载失败: " + description);
                GlobalToast.show(PptPreviewActivity.this, "加载失败: " + description, GlobalToast.Type.ERROR);
                showErrorMessage("加载失败: " + description);
            }
        });
        
        // 设置WebChromeClient
        pptWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                Timber.tag("PptPreview").d( "加载进度: " + newProgress + "%");
            }
        });
    }

    private void saveAction() {
        pptWebView.post(() -> pptWebView.evaluateJavascript("saveFile('')", jsResult ->
                Timber.tag("PptPreview").d("callback触发saveFile结果: " + jsResult)));
    }

    private void scheduleSaveActions() {
        saveHandler.removeCallbacks(saveActionRunnable1s);
        saveHandler.removeCallbacks(saveActionRunnable5s);
        saveHandler.removeCallbacks(saveActionRunnable10s);
        saveHandler.postDelayed(saveActionRunnable1s, 1000);
        saveHandler.postDelayed(saveActionRunnable5s, 5000);
        saveHandler.postDelayed(saveActionRunnable10s, 10000);
    }

    @Override
    protected void setupObservers() {
        // Observe download progress
        viewModel.getDownloadInProgress().observe(this, inProgress -> {
            if (inProgress != null) {
                downloadButton.setEnabled(!inProgress);
                downloadButton.setAlpha(inProgress ? 0.5f : 1.0f);
            }
        });
        
        viewModel.getDownloadProgress().observe(this, progress -> {
            // 不再显示实时进度Toast，由ViewModel中的全局Toast处理
            Timber.tag("PptPreview").d( "下载进度: " + progress);
        });

        // Observe WebView URL
        viewModel.getWebViewUrl().observe(this, pptUrl -> {
            if (pptUrl != null && !pptUrl.isEmpty()) {
                Timber.tag("PptPreview").d( "加载PPT预览: " + pptUrl);
                loadPptPreview(pptUrl);
            } else {
                Timber.tag("PptPreview").d( "没有PPT URL，显示错误提示");
                showErrorMessage("PPT文件不存在");
            }
        });
        
        // Observe title
        viewModel.getPptTitle().observe(this, title -> {
            if (title != null && !title.isEmpty()) {
                titleText.setText(title);
            }
        });

        viewModel.getDownloadFinish().observe(this, downloadPath -> {
            if (downloadPath != null && !downloadPath.isEmpty()) {
                // 参考会议摘要的导出成功弹窗实现
                showPptDownloadSuccessDialog(downloadPath);
            }
        });
    }
    
    /**
     * 加载PPT预览
     */
    private void loadPptPreview(String pptUrl) {
        String previewUrl = buildOnlinePreviewUrl(pptUrl);
        Timber.tag("PptPreview").d( "加载预览URL: " + previewUrl);
        pptWebView.loadUrl(previewUrl);
    }
    
    /**
     * 构建在线预览URL - 使用自定义预览服务
     * 格式: https://mobile-web.jmkjsh.com/#/previewApp?id={sessionId}&taskId={taskId}&url={pptUrl}&token={token}
     */
    private String buildOnlinePreviewUrl(String pptUrl) {
        if (pptUrl == null || pptUrl.isEmpty()) {
            return pptUrl;
        }
        
        try {
            // 获取当前用户的 token
            String token = SharedPreferencesUtil.getToken();
            if (token == null || token.isEmpty()) {
                Timber.tag("PptPreview").w("Token为空，使用默认预览方式");
                token = "";
            }
            
            // 从 ViewModel 获取 sessionId 和 taskId（如果 Activity 中没有）
            int finalSessionId = sessionId;
            String finalTaskId = taskId;
            
            if (finalSessionId == 0 && viewModel != null) {
                finalSessionId = viewModel.getSessionId();
            }
            if ((finalTaskId == null || finalTaskId.isEmpty()) && viewModel != null) {
                finalTaskId = viewModel.getTaskId();
            }
            
            // URL 编码 pptUrl 和 token
            String encodedPptUrl = URLEncoder.encode(pptUrl, "UTF-8");
            String encodedToken = URLEncoder.encode(token, "UTF-8");
            
            // 构建预览 URL
            StringBuilder previewUrl = new StringBuilder(PPT_PREVIEW);
            previewUrl.append("id=").append(finalSessionId);
            previewUrl.append("&taskId=").append(finalTaskId != null ? finalTaskId : "");
            previewUrl.append("&url=").append(encodedPptUrl);
            previewUrl.append("&token=").append(encodedToken);
            
            String finalUrl = previewUrl.toString();
            Timber.tag("PptPreview").d("构建预览URL: " + finalUrl);
            return finalUrl;
        } catch (UnsupportedEncodingException e) {
            Timber.tag("PptPreview").e("构建预览URL失败: " + e);
            // 如果编码失败，回退到原始 URL
            return pptUrl;
        } catch (Exception e) {
            Timber.tag("PptPreview").e("构建预览URL失败: " + e);
            return pptUrl;
        }
    }
    
    /**
     * 显示错误信息
     */
    private void showErrorMessage(String message) {
        Timber.tag("PptPreview").e( "显示错误信息: " + message);
        GlobalToast.show(this, message, GlobalToast.Type.ERROR);
        
        String errorHtml = "<html><body style='display:flex;justify-content:center;align-items:center;height:100vh;font-family:Arial;'>" +
                          "<div style='text-align:center;'>" +
                          "<h2>预览失败</h2>" +
                          "<p>" + message + "</p>" +
                          "</div></body></html>";
        pptWebView.loadData(errorHtml, "text/html", "UTF-8");
    }
    
    /**
     * 设置沉浸式状态栏 - 参考会议页面样式
     */
    private void setupImmersiveStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));

            // 延迟设置确保生效
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }, 100);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 设置状态栏文字为深色
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 确保状态栏样式在Resume时也正确
        setupImmersiveStatusBar();
    }

    @Override
    protected void onDestroy() {
        saveHandler.removeCallbacks(saveActionRunnable1s);
        saveHandler.removeCallbacks(saveActionRunnable5s);
        saveHandler.removeCallbacks(saveActionRunnable10s);
        super.onDestroy();
        if (pptWebView != null) {
            pptWebView.onDetachedFromWindow();
            pptWebView.destroy();
        }
    }
}
