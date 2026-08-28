package com.fxzs.lingxiagent.view.excel;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.cmdc.ai.assist.constraint.AIAssistConfig;
import com.fxzs.lingxiagent.BuildConfig;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.NetworkStateManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.ppt.PptPreviewActivity;
import com.fxzs.lingxiagent.viewmodel.excel.VMExcel;

import org.json.JSONObject;

import java.io.File;

import timber.log.Timber;


public class AiExcelContainActivity extends BaseActivity {

    public String TAG = "AiExcelContainActivity";
    private WebView webView;
    private MyWebChromeClient chromeClient;

    String id;
    private View ll_title;
    private boolean hasRetried = false;
    private NetworkStateManager networkStateManager;

    private  String currentUrl;
    @Override
    protected int getLayoutResource() {
        return R.layout.act_ai_excel_container;
    }

    @Override
    protected Class getViewModelClass() {
        return VMExcel.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
        init();
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

            webView.evaluateJavascript(script, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init() {

        findViewById(R.id.back).setOnClickListener(view -> finish());

        webView = findViewById(R.id.webview);
        ll_title = findViewById(R.id.ll_title);

        // 配置WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Timber.tag(TAG).d("shouldOverrideUrlLoading(url) = " + url);
                return false; // false=让 WebView 自己加载
            }
        
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                String url = request.getUrl() != null ? request.getUrl().toString() : "";
                Timber.tag(TAG).d("shouldOverrideUrlLoading(request) = " + url);
                return false;
            }
        
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                Timber.tag(TAG).d("onPageStarted url = " + url);
                super.onPageStarted(view, url, favicon);
            }
        
            @Override
            public void onPageFinished(WebView view, String url) {
                Timber.tag(TAG).d("onPageFinished url = " + url);
                super.onPageFinished(view, url);
                sendDeviceDataToWeb();
                if(url.startsWith(Constants.AI_EXCEL_HOME)){
                    ll_title.setVisibility(View.GONE);
                }else {
                    ll_title.setVisibility(View.VISIBLE);
                }
            }
        
            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                Timber.tag(TAG).d("doUpdateVisitedHistory url = " + url + ", isReload=" + isReload);
                super.doUpdateVisitedHistory(view, url, isReload);
            }
            @Override
            public void onReceivedError(WebView view, int code, String description, String failingUrl) {

                if (code == ERROR_HOST_LOOKUP
                        || code == ERROR_CONNECT
                        || code == ERROR_TIMEOUT) {

                    retryOrShowFallback();
                }
            }
        });
        chromeClient =  new MyWebChromeClient(this);
        webView.setWebChromeClient(chromeClient);

//        try {
//            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
//            if (cm != null) {
//                cm.registerDefaultNetworkCallback(
//                        new ConnectivityManager.NetworkCallback() {
//                            @Override
//                            public void onAvailable(@NonNull android.net.Network network) {
//                                Timber.tag(TAG).w("CONNECTIVITY_SERVICE 网络重连了");
//                                runOnUiThread(() -> {
//                                    hasRetried = false;
//                                    webView.reload();
//                                });
//                            }
//                        }
//                );
//            } else {
//                Timber.tag(TAG).w("CONNECTIVITY_SERVICE is null");
//            }
//        } catch (Throwable t) {
//            Timber.tag(TAG).e(t, "registerDefaultNetworkCallback failed");
//        }
        if (networkStateManager == null) {
            networkStateManager = new NetworkStateManager(this);
            setupNetworkStateListener();
        }


        String token = SharedPreferencesUtil.getAccessToken();
        Timber.tag("TAG").d("token = " + token);
        String url = Constants.AI_EXCEL_HOME + token;
        String clientId = Constants.CLIENT_ID;
        if (BuildConfig.FLAVOR.contains("tablet")) {
            clientId = Constants.CLIENT_PAD_ID;
        } else {
            clientId = Constants.CLIENT_ID;
        }
        url +=  "&clientId="+clientId;
        if (getIntent() != null) {
            id = getIntent().getStringExtra(Constant.INTENT_ID);
            if(id != null){
                url +=  "&id="+id;
            }
        }
        Timber.tag("TAG").d("url = "+url);
        currentUrl = url;
        webView.loadUrl(url);
        webView.addJavascriptInterface(new AndroidtoJs(this, new JSCallback() {
            @Override
            public void callback(String name) {

            }
        }), "androidBride");//AndroidtoJS类对象映射到js的test对象


    }
    @Override
    protected void setupObservers() {
        // 监听 ViewModel 的 LiveData

    }




//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e(TAG,"onActivityResult = requestCode = "+requestCode+". resultCode = "+resultCode+ " data = "+(data!=null?data.getData():""));
        Log.e(TAG,"onActivityResult = requestCode = "+requestCode+". resultCode = "+resultCode+ " getDataString = "+(data!=null?data.getDataString():""));
//        if (requestCode == PhotoUtils.RESULT_CODE_CAMERA && resultCode == Activity.RESULT_OK) {
//            //拍照并确定
//            //可以考虑--压缩图片（这里因为我司H5那边做了压缩，所以客户端就可以不做了）
////            mFilePathCallback?.onReceiveValue(arrayOf(Uri.parse(PhotoUtils.PATH_PHOTO)))；
//            Log.e(TAG,"onActivityResult = PATH_PHOTO  " +PhotoUtils.PATH_PHOTO );
//
//            File temp = new File(PhotoUtils.PATH_PHOTO);
////            Uri.fromFile(temp);
//            Uri uri =
//                    FileProvider.getUriForFile(getContext(), getActivity().getPackageName() + ".fileprovider", temp);
//            Log.e(TAG,"onActivityResult = PATH_PHOTO uri " +uri );
//
//            chromeClient.onReceiveValue(new Uri[]{uri});
////            chromeClient.onReceiveValue(new Uri[]{Uri.parse(PhotoUtils.PATH_PHOTO)});
//        }else if(requestCode == PhotoUtils.RESULT_CODE_PHOTO && resultCode == Activity.RESULT_OK){
////            Uri result0 =  data.getClipData().getItemAt(0).getUri();
////            Log.e(TAG,"onActivityResult = result0 = "+result0);
//            String dataString = data.getDataString();
//            Log.e(TAG,"onActivityResult = dataString = "+dataString);
//            Uri result = data.getData();
//            String path = PhotoUtils.getPath(getActivity(),result);
//            Log.e(TAG,"onActivityResult = path = "+path);
//            if (path == null) {
//                chromeClient.onReceiveValue(null);
//            } else {
////                chromeClient.onReceiveValue(new Uri[]{Uri.parse(path)});
//                chromeClient.onReceiveValue(new Uri[]{Uri.parse(dataString)});
////                chromeClient.onReceiveValue(null);
//            }
//        }else
            if(requestCode == PhotoUtils.FILE_SELECTOR_CODE && resultCode == Activity.RESULT_OK){
            Uri uri = data != null ? data.getData() : null;
            if (uri != null) {
                String mimeType = getContentResolver().getType(uri);
                if (mimeType != null && (mimeType.startsWith("image/") || mimeType.startsWith("video/"))) {
                    Toast.makeText(this, "暂不支持该格式", Toast.LENGTH_SHORT).show();
                    chromeClient.onReceiveValue(null);
                    return;
                }
            }

            String dataString = data.getDataString();
            chromeClient.onReceiveValue(new Uri[]{Uri.parse(dataString)});

        }else {
            chromeClient.onReceiveValue(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限获取成功，但这里不能直接调用openAudioFileSelector()
                // 因为我们不知道用户点击的是音频还是视频按钮
//                Toast.makeText(getContext(), "权限获取成功，请重新点击选择文件", Toast.LENGTH_SHORT).show();
                PhotoUtils.openFileSelector(this);
            } else {
                Toast.makeText(this, "需要存储权限才能选择文件", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 1002) {
            // 通知权限请求结果
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 通知权限已授予，后续下载完成时会显示通知
                Timber.tag(TAG).d("通知权限已授予");
            } else {
                // 通知权限被拒绝，下载仍可继续，但不会显示通知
                Timber.tag(TAG).d("通知权限被拒绝，下载完成时将不显示通知");
            }
        }
    }

    private void retryOrShowFallback() {
        if (!hasRetried) {
            hasRetried = true;
            webView.postDelayed(() -> webView.reload(), 2000);
        } else {
            showNetErrorPage();
        }
    }

    private void showNetErrorPage() {
        webView.loadUrl("file:///android_asset/net_error.html");
    }


    /**
     * 设置网络状态监听器
     */
    private void setupNetworkStateListener() {
        networkStateManager.addNetworkStateListener(new NetworkStateManager.NetworkStateListener() {
            @Override
            public void onNetworkAvailable() {
                Timber.tag(TAG).d( "网络连接恢复");
                // 网络恢复时可以重试失败的请求
                runOnUiThread(() -> {
                    hasRetried = false;
//                    webView.reload();
                    forceReloadWebView();
                });

            }

            @Override
            public void onNetworkLost() {
                Timber.tag(TAG).d( "网络连接丢失");
                // 网络丢失时停止重试
//                if (retryManager != null) {
//                    retryManager.stopRetry();
//                }
            }

            @Override
            public void onNetworkChanged(boolean isConnected) {
                Timber.tag(TAG).d( "网络状态变化: " + (isConnected ? "已连接" : "已断开"));
            }
        });
    }
    private void forceReloadWebView() {
        webView.loadUrl(currentUrl);
    }
}
