package com.fxzs.lingxiagent.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.GlobalToast;

import timber.log.Timber;

public class AppPermissionRequestManager {

    // 抽象公共权限请求逻辑
    private static void requestPermissionWithDialog(
            Activity activity,
            String spKey,
            String permission,
            String[] permissionsToRequest,
            int requestCode,
            String title,
            String message) {

        boolean hasRequestedBefore = SharedPreferencesUtil.getBoolean(spKey, false);

        if (!hasRequestedBefore) {
            CommonDialog.showAgreementDialog(activity, message, title, new CommonDialog.OnDialogClickListener() {
                @Override
                public void onConfirm() {
                    SharedPreferencesUtil.saveBoolean(spKey, true);
                    ActivityCompat.requestPermissions(activity, permissionsToRequest, requestCode);
                }

                @Override
                public void onCancel() {
                    SharedPreferencesUtil.saveBoolean(spKey, true);
                }
            });
        } else {
            if (!TextUtils.isEmpty(getUnauthorizedMessage(permission))) {
                GlobalToast.show(activity, getUnauthorizedMessage(permission), GlobalToast.Type.ERROR);
            }
        }
    }

    private static String getUnauthorizedMessage(String permission) {
        switch (permission) {
            case Manifest.permission.READ_PHONE_STATE:
                return "获取手机状态未授权";
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "";
            case Manifest.permission.RECORD_AUDIO:
                return "录音未授权";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
            case Manifest.permission.READ_MEDIA_IMAGES:
                return "本地存储未授权";
            case Manifest.permission.CAMERA:
                return "访问相机未授权";
            default:
                return "权限未授予";
        }
    }

    public static void requestReadPhoneStatePermission(Activity activity, int requestCode) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_PHONE_STATE, false);
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_PHONE_STATE);
        basePermissionStyle(showRationale, activity, "允许访问电话", "请授权电话权限，以完成拨打电话功能", new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.READ_PHONE_STATE},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
                // nothing to do here
            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_PHONE_STATE, true);
    }

    public static void requestReadContactsPermission(Activity activity, int requestCode) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_READ_CONTACTS, false);
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS);
        basePermissionStyle(showRationale, activity, "允许访问通讯录", "请授权通讯录权限，以完成查找联系人功能", new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.READ_CONTACTS},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
                // nothing to do here
            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_READ_CONTACTS, true);
    }

    public static void requestCallContactsPermission(Activity activity, int requestCode) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_CALL_PHONE, false);
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CALL_PHONE);
        basePermissionStyle(showRationale, activity, "允许访问电话", "请授权电话权限，以完成拨打电话功能", new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.CALL_PHONE},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
                // nothing to do here
            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_CALL_PHONE, true);
    }

    public static void requestSendSmsPermission(Activity activity, int requestCode) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_SEND_SMS, false);
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.SEND_SMS);
        basePermissionStyle(showRationale, activity, "允许访问短信", "请授权短信权限，以完成发送短信功能", new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.SEND_SMS},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
                // nothing to do here
            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_SEND_SMS, true);
    }

    public static void requestLocationPermission(Activity activity, int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Timber.tag("GMapHelper").i("定位权限申请" + activity.getClass().getName());
            requestPermissionWithDialog(
                    activity,
                    Constants.SP_TYPE_PERMISSIONS_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    requestCode,
                    "允许获取位置信息",
                    "请授权位置权限，方便为你提供更精准的规划服务"
            );
        }
    }

    public static final String PERMISSION_AUDIO_MESSAGE_HX = "请授权设备麦克风，以便语音唤醒灵犀";
    public static final String PERMISSION_AUDIO_MESSAGE_ASR = "请授权设备麦克风，以便发送语音消息";
    public static final String PERMISSION_AUDIO_MESSAGE_MEETING = "请授权设备麦克风，以便使用AI会议相关功能";
    public static final String PERMISSION_AUDIO_MESSAGE_SI = "请授权设备麦克风，以便使用同声传译相关功能";
    public static final String PERMISSION_AUDIO_MESSAGE_WAKEUP = "请授权设备麦克风，以便使用唤醒相关功能";

    public static void requestAudioPermission(Activity activity, int requestCode, String msg, SwitchCompat switchWakeup) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_AUDIO, false);
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO);
        basePermissionStyle(showRationale, activity, "允许访问麦克风", msg, new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
                // nothing to do here
                if(switchWakeup != null){
                    switchWakeup.setChecked(false);
                }
            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_AUDIO, true);
    }
    public static void requestAudioPermission(Activity activity, int requestCode, String msg) {
        requestAudioPermission( activity, requestCode, msg,null);
    }

    public static void requestReadMediaAudioPermission(Activity activity, int requestCode,String title,String msg) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_READ_MEDIA_AUDIO, false);
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_MEDIA_AUDIO);
        basePermissionStyle(showRationale, activity, title, msg, new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_READ_MEDIA_AUDIO, true);
    }

    public static void requestExternalStoragePermission(Activity activity, int requestCode,String dec) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_EXTERNAL_STORAGE, false);
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);

        basePermissionStyle(showRationale, activity, "允许访问存储权限", dec, new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{permission, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_EXTERNAL_STORAGE, true);
    }
    public static void requestWriteExternalStoragePermission(Activity activity, int requestCode,String dec) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_EXTERNAL_STORAGE_WRITE, false);
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);

        basePermissionStyle(showRationale, activity, "允许访问存储权限", dec, new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{permission, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_EXTERNAL_STORAGE_WRITE, true);
    }

    public static void requestImagesPermission(Activity activity, int requestCode,String dec) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_EXTERNAL_STORAGE, false);
        boolean isTiramisuOrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
        String permission = isTiramisuOrAbove ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);

        basePermissionStyle(showRationale, activity, "允许访问图片媒体文件", dec, new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{permission},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_EXTERNAL_STORAGE, true);
    }

    public static void requestCameraPermission(Activity activity, int requestCode) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_CAMERA, false);
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);
        basePermissionStyle(showRationale, activity, "允许访问相机", "请授权相机，以确保拍照功能正常使用", new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.CAMERA},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
                // nothing to do here
            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_CAMERA, true);
    }

    public static void requestBluetoothPermission(Activity activity, int requestCode) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_BLUETOOTH_CONNECT, false);
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.BLUETOOTH_CONNECT);
        basePermissionStyle(showRationale, activity, "允许访问蓝牙", "请授权蓝牙权限，以完成打开关闭蓝牙功能", new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
                // nothing to do here
            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_BLUETOOTH_CONNECT, true);
    }

    public static void requestNotificationsPermission(Activity activity, int requestCode,String msg,SwitchCompat switchCompat,CommonDialog.OnDialogClickListener listener) {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_POST_NOTIFICATIONS, false);
        boolean showRationale = !aBoolean || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS);
        basePermissionStyle(showRationale, activity, "允许通知权限", msg, new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (showRationale) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            requestCode);
                } else {
                    openAppSettings(activity);
                }
            }

            @Override
            public void onCancel() {
                // nothing to do here
                if(switchCompat != null){
                    switchCompat.setChecked(false);
                }
                if (listener != null){
                    listener.onCancel();
                }

            }
        });
        SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_POST_NOTIFICATIONS, true);
    }

    public static void requestNotificationsPermission(Activity activity, int requestCode,String msg) {
        requestNotificationsPermission(activity,requestCode,msg,null,null);
     }
    public static void requestNotificationsPermission2(Activity activity, int requestCode,String msg,SwitchCompat switchCompat) {
        requestNotificationsPermission(activity,requestCode,msg,switchCompat,null);
    }

//    public static void requestNotificationsPermission2(Activity activity, int requestCode,String msg,CommonDialog.OnDialogClickListener listener) {
//        requestNotificationsPermission(activity,requestCode,msg,null,listener);
//    }

    public static void openAppSettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivity(intent);
        } catch (Exception e) {
            GlobalToast.show(activity, "无法打开设置，请手动前往系统设置进行权限配置", GlobalToast.Type.ERROR);
        }
    }

    public static void basePermissionStyle(boolean showRationale, Activity activity, String title, String message, CommonDialog.OnDialogClickListener listener) {
        CommonDialog.showConfirmDialog(activity, title, message, showRationale ? "同意并继续" : "去设置", listener);
    }
    public static void requestOverlayPermissionDialog(Activity activity,CommonDialog.OnDialogClickListener listener) {
            basePermissionStyle(true, activity, "允许悬浮窗权限", "为了在后台唤醒时显示快捷入口，请允许“显示在其他应用上层”。", listener);
    }

    /**
     * 判断是否拥有悬浮窗权限 (SYSTEM_ALERT_WINDOW)
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ 必须显式检查
            return Settings.canDrawOverlays(context);
        } else {
            // 6.0 以下默认视为有权限
            return true;
        }
    }

}
