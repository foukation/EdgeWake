package com.fxzs.lingxiagent.view.excel;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;

import java.util.List;

public class MyWebChromeClient extends WebChromeClient {
    public static final String TAG = "MyWebChromeClient";
    AiExcelContainActivity context;
    ValueCallback<Uri[]> filePathCallback;

    String[] fileTypes = new String[]{
            ".pdf","audio/*",".bmp","image/png",".gif"
    };
    public MyWebChromeClient(AiExcelContainActivity context) {
        this.context = context;
//        this.filePathCallback = filePathCallback;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        Log.e(TAG,"ConsoleMessage lineNumber = "+consoleMessage.lineNumber());
        Log.e(TAG,"ConsoleMessage = "+consoleMessage.message());
        return true;
//        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public void onPermissionRequest(PermissionRequest request) {

        for (String resource : request.getResources()) {
            Log.e(TAG,"onPermissionRequest resource = "+resource);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (String resource : request.getResources()) {
                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                    request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                    return;
                }
            }
            request.deny();
        }
//        super.onPermissionRequest(request);
//        for (String resource : request.getResources()) {
////
//            Log.e(TAG,"onPermissionRequest resource = "+resource);
////            if (resource.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
////                Log.e(TAG,"onPermissionRequest contains");
////                request.grant(request.getResources());
////            }
//        }
//        Log.e(TAG,"onPermissionRequest resource = "+request.getResources());
//        context.getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                request.grant(request.getResources());
//            }
//        });
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
//        return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
        this.filePathCallback = filePathCallback;
        Log.e(TAG,"onShowFileChooser = 1");
        String[] acceptTypes = fileChooserParams.getAcceptTypes();

        Log.e(TAG,"onShowFileChooser getTitle = "+ fileChooserParams.getTitle());
        Log.e(TAG,"onShowFileChooser isCaptureEnabled = "+ fileChooserParams.isCaptureEnabled());
        Log.e(TAG,"onShowFileChooser getAcceptTypes = "+ fileChooserParams.getAcceptTypes().length);
        for (int i = 0; i < acceptTypes.length; i++) {
            Log.e(TAG,"onShowFileChooser = "+acceptTypes[i]);
        }
//

        if (acceptTypes.length == 1 && acceptTypes[0].contains("image/*") && !fileChooserParams.isCaptureEnabled()) {//相册不带capture

            requestPermission(new String[]{
                    Manifest.permission.CAMERA,
//                    Manifest.permission.READ_EXTERNAL_STORAGE
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            },new Callback() {
                @Override
                public void callback() {

                    PhotoUtils.startAlbum(context);
                }
            });
        }else if (acceptTypes.length == 1 && acceptTypes[0].contains("image/*") && fileChooserParams.isCaptureEnabled()) {//相机
//            Manifest.permission.CAMERA,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE


            requestPermission(new String[]{
                    Manifest.permission.CAMERA,
            },new Callback() {
                @Override
                public void callback() {
                    PhotoUtils.startCamera(context);
                }
            });
        }else if(acceptTypes.length > 1 ){//文件
            boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_EXCEL_FILE, false);
            if(!aBoolean){
                AppPermissionRequestManager.basePermissionStyle(true, (Activity) context, "允许访问文件权限", "请授权文件权限，以便对文件进行上传", new CommonDialog.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        if (true) {
                            PhotoUtils.openFileSelector((Activity) context);
                        } else {
                            AppPermissionRequestManager.openAppSettings((Activity) context);
                        }
                        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_EXCEL_FILE, true);
                    }

                    @Override
                    public void onCancel() {
                        filePathCallback.onReceiveValue(null);
                    }
                });
            }else{
                PhotoUtils.openFileSelector((Activity) context);
            }

//            if(!checkStoragePermission()){
//
//                boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_READ_MEDIA_AUDIO, false);
////            ZUtils.showToast("aBoolean == "+aBoolean);
////                boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_MEDIA_AUDIO);
//                boolean showRationale = true;
////            boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_MEDIA_AUDIO);
//                AppPermissionRequestManager.basePermissionStyle(showRationale, (Activity) context, "允许访问文件权限", "请授权文件权限，以便对文件进行上传", new CommonDialog.OnDialogClickListener() {
//                    @Override
//                    public void onConfirm() {
//                        if (showRationale) {
////            READ_MEDIA_AUDIO
//                            requestStoragePermission();
//                        } else {
//                            AppPermissionRequestManager.openAppSettings((Activity) context);
//                        }
//                    }
//
//                    @Override
//                    public void onCancel() {
//                        filePathCallback.onReceiveValue(null);
//                    }
//                });
//                SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_READ_MEDIA_AUDIO, true);
//            }else{
////                requestStoragePermission();
//                PhotoUtils.openFileSelector((Activity) context);
//            }
//            READ_MEDIA_AUDIO
//            requestStoragePermission();

        }else if(acceptTypes.length == 1){//文件
           boolean hasType = false;
            for (String fileType : fileTypes) {
                if(fileType.contains(acceptTypes[0])){
                    hasType = true;
                    break;
                }
            }
            if (hasType){
                requestPermission(new String[]{
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_AUDIO,

//                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                },new Callback() {
                    @Override
                    public void callback() {
                        PhotoUtils.openFileSelector(context);
                    }
                });
            }else {
                ZUtils.showToast("未知格式");
                filePathCallback.onReceiveValue(null);
            }

        }else {
            ZUtils.showToast("未知格式");
            filePathCallback.onReceiveValue(null);
        }
//                PermissionX.init((FragmentActivity) context)
//                        .permissions( Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                        .request { allGranted, grantedList, deniedList ->
//                    if (allGranted) {
//                        Toast.makeText(this, "All permissions are granted", Toast.LENGTH_LONG).show();
//                    } else {
//                        Toast.makeText(this, "These permissions are denied: $deniedList", Toast.LENGTH_LONG).show();
//                    }
//                }
        Log.e(TAG,"onShowFileChooser = 2");
        return true;
    }

    public void onReceiveValue(Uri[] uris){
        Log.e(TAG,"onReceiveValue = "+uris);

        if (uris != null) {
            for (Uri uri : uris) {
                Log.e(TAG,"onReceiveValue  uri = "+uri.getPath());
            }
        }
        Log.e(TAG,"onReceiveValue = filePathCallback"+filePathCallback.toString());
        filePathCallback.onReceiveValue(uris);
    }

    public void requestPermission(String[] permissionStr ,Callback callback){
        PermissionX.init((AiExcelContainActivity) context)
                .permissions(
                        permissionStr
//                        new String[]{
//                        Manifest.permission.CAMERA,
////                                Manifest.permission.READ_CONTACTS,  Manifest.permission.CALL_PHONE
//                }
                )
                .request(new RequestCallback() {
                    @Override
                    public void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
                        Log.e(TAG,"onShowFileChooser allGranted = "+allGranted);
                        Log.e(TAG,"onShowFileChooser grantedList = "+grantedList);
                        Log.e(TAG,"onShowFileChooser deniedList = "+deniedList);
                        if (allGranted) {
//                            Toast.makeText(context.getContext(), "All permissions are granted", Toast.LENGTH_LONG).show();

                            callback.callback();
                        } else {
                            String showText = "权限";
                            if(deniedList.contains(
                                    Manifest.permission.CAMERA)){
                                showText += "相机权限 ";
                            }
                            if(deniedList.contains(
                                    Manifest.permission.READ_MEDIA_AUDIO)){
                                showText += "录音权限 ";
                            }
                            if(deniedList.contains(
                                    Manifest.permission.READ_MEDIA_IMAGES)){
                                showText += "文件权限 ";
                            }
//                            Toast.makeText(context.getContext(), "These permissions are denied: " + deniedList, Toast.LENGTH_LONG).show();
                            Toast.makeText(context, "请到设置中赋予" + showText, Toast.LENGTH_LONG).show();
                            onReceiveValue(null);
                        }
                    }
                });
    }


    private void requestStoragePermission() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上版本
//            String permission = Manifest.permission.READ_MEDIA_AUDIO;
//            ActivityCompat.requestPermissions(context,
//                    new String[]{permission, Manifest.permission.READ_MEDIA_AUDIO},
//                    1001);
        } else {
            // Android 13以下版本
            String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
            ActivityCompat.requestPermissions(context,
                    new String[]{permission, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1001);
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            // Android 13及以上版本
//            AppPermissionRequestManager.requestReadMediaAudioPermission(context, 1001);
//        } else {
//            // Android 13以下版本
//            AppPermissionRequestManager.requestExternalStoragePermission(context, 1001,"请授权手机存储，以便导入本地文件进行转写");
//        }
    }
    private boolean checkStoragePermission() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上版本使用新的媒体权限
//            return ContextCompat.checkSelfPermission(context,
//                    Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
            return true;
        } else {
            // Android 13以下版本使用传统存储权限
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

}