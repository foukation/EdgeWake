package com.fxzs.lingxiagent.view.excel;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtils;


public class AndroidtoJs extends Object {

    Context context;
    JSCallback callback;
    private long lastDownloadTime = 0; // 记录上次下载的时间戳
    private long lastDownloadToastTime = 0; // 记录上次提示的时间戳
    private static final int PERMISSION_REQUEST_POST_NOTIFICATIONS = 1002;
    private static long request_NOTIFICATIONS_time= 0; // 请求通知栏权限最多两次

    public AndroidtoJs(Context context, JSCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    // 定义JS需要调用的方法
    // 被JS调用的方法必须加入@JavascriptInterface注解
    @JavascriptInterface
    public void hello(String msg) {
        System.out.println("JS调用了Android的hello方法");
    }
    @JavascriptInterface
    public void getToken(String msg) {
        System.out.println("JS调用了Android的getToken方法");

        String str= msg.replace("\"", "");
        System.out.println("msg = "+msg);
        System.out.println("str = "+str);
//        Utils.token = str;
    }
    @JavascriptInterface
    public void getOrder(String msg) {
        System.out.println("JS调用了Android的getOrder方法");

        String str= msg.replace("\"", "");
        System.out.println("str = "+str);
         str= str.replace("<order>", "");
        System.out.println("str = "+str);
         str= str.replace("</order>", "");
        System.out.println("msg = "+msg);
        str= str.replace("打开", "");
        System.out.println("str = "+str);
//        Utils.token = str;
//        AppJump.parserAndJump(context,str);
    }
    @JavascriptInterface
    public void setDownload(String msg) {
        System.out.println("JS调用了Android的===.setDownload方法");

        // 防抖处理：4秒内不允许重复点击
        long currentTime = System.currentTimeMillis();
        System.out.println("currentTime = "+currentTime);
        System.out.println("lastDownloadTime = "+lastDownloadTime);
        if (currentTime - lastDownloadTime < 4000) {
            System.out.println("下载操作过于频繁，请稍后再试");
            if (currentTime - lastDownloadToastTime >= 4000) {
                lastDownloadToastTime = currentTime;
                Toast.makeText(context, "下载操作过于频繁，请稍后再试", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        lastDownloadTime = currentTime;

        // 检查并请求通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && request_NOTIFICATIONS_time < 2) {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                        != PackageManager.PERMISSION_GRANTED) {
                    boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_POST_NOTIFICATIONS, false);
                    boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS);
                    if (showRationale) {
                        request_NOTIFICATIONS_time++;
                    } else {
                        request_NOTIFICATIONS_time = 1;
                        request_NOTIFICATIONS_time++;
                    }
                    AppPermissionRequestManager.requestNotificationsPermission((Activity) context, 1002,"需要通知权限以显示下载完成通知");


                    // 请求通知权限
//                    ActivityCompat.requestPermissions(activity,
//                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
//                            PERMISSION_REQUEST_POST_NOTIFICATIONS);
//                    Toast.makeText(context, "需要通知权限以显示下载完成通知", Toast.LENGTH_SHORT).show();
                   
//                    return;
                }
            }
        }

        String str= msg.replace("\"", "");
        System.out.println("str = "+str);
        //TODO 去下载
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 不需要申请存储权限，直接下载
            goToDownload(str);
        } else {
            // Android 9及以下需要检查存储权限
            if (hasStoragePermission()) {
                goToDownload(str);
            } else {
                AppPermissionRequestManager.requestExternalStoragePermission((Activity) context, 1001,"请授权手机存储，以便下载功能正常使用");
            }
        }
//            if (hasStoragePermission()) {
//                goToDownload(str);
//            } else {
//                AppPermissionRequestManager.requestExternalStoragePermission((Activity) context, 1001,"请授权手机存储，以便下载功能正常使用");
//            }
    }

    /**
     * 检查是否有存储权限
     */
    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
//        return ContextCompat.checkSelfPermission(context,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }


    private void goToDownload(String str) {

        if(str.startsWith("data:image")){
            Base64ImageSaver.saveBase64ImageToAlbum(context, str);
        }else {
            ZUtils.downloadFile(context,str);
        }
    }

    @JavascriptInterface
    public void getVersion(String msg) {
        System.out.println("JS调用了Android的===.getVersion方法");

        String str= msg.replace("\"", "");
        System.out.println("str = "+str);
      
//        Utils.checkUpdate(context,str);
        if(callback != null){
            callback.callback("getVersion");
        }
    }
    @JavascriptInterface
    public void getAndroidVersion(String msg) {
        System.out.println("JS调用了Android的===.getAndroidVersion方法");

        String str= msg.replace("\"", "");
        System.out.println("str = "+str);

        if(callback != null){
            callback.callback("getAndroidVersion");
        }
    }

}
