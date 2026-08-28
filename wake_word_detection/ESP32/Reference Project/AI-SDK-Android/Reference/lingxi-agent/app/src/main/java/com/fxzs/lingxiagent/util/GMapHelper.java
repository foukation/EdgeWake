package com.fxzs.lingxiagent.util;

import android.content.Context;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import java.util.Locale;

import timber.log.Timber;

public class GMapHelper {
    private static final String TAG = "GMapHelper";
    private static GMapHelper INSTANCE = new GMapHelper();
    private AMapLocationClient locationClient;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private long lastUpdateTime = 0L;

    public static GMapHelper getInstance() {
        return INSTANCE;
    }

    private GMapHelper() {
        // 私有构造函数防止外部实例化
    }

    public interface LocationCallback {
        void onLocationResult(AMapLocation location, int errorCode, String errorInfo);
    }

    private AMapLocationListener createLocationListener(final LocationCallback callback) {
        return new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation amapLocation) {
                if (amapLocation == null) return;

                long currentUpdateTime = System.currentTimeMillis();
                if (amapLocation.getErrorCode() == 0) {
                    if (lastUpdateTime == 0L || currentUpdateTime - lastUpdateTime > 5 * 1000L) {
                        lastUpdateTime = currentUpdateTime;
                        Timber.tag(TAG).d( String.format(Locale.getDefault(),
                                "定位成功 - 纬度: %s, 经度: %s, 地址: %s",
                                amapLocation.getLatitude(),
                                amapLocation.getLongitude(),
                                amapLocation));

                        latitude = amapLocation.getLatitude();
                        longitude = amapLocation.getLongitude();

                        if (callback != null) {
                            callback.onLocationResult(amapLocation, 0, null);
                        }
                        stopLocation();
                    } else {
                        Timber.tag(TAG).i( "定位结果未更新，忽略此次回调");
                    }
                } else {
                    Timber.tag(TAG).e( String.format(Locale.getDefault(),"定位失败 - 错误码: %s, 错误信息: %s",
                            amapLocation.getErrorCode(),
                            amapLocation.getErrorInfo()));

                    if (callback != null) {
                        callback.onLocationResult(null, amapLocation.getErrorCode(), amapLocation.getErrorInfo());
                    }
                }
            }
        };
    }

    /**
     * 初始化定位服务
     * @param context 应用上下文
     */
    public void initLocation(Context context) {
        Timber.tag(TAG).i( "初始化定位服务");

        // 隐私政策设置
        AMapLocationClient.updatePrivacyShow(context.getApplicationContext(), true, true);
        AMapLocationClient.updatePrivacyAgree(context.getApplicationContext(), true);

        try {
            locationClient = new AMapLocationClient(context.getApplicationContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        AMapLocationClientOption locationOption = new AMapLocationClientOption();
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);

        // 可选设置
        // locationOption.setNeedAddress(true);
        // locationOption.setInterval(10 * 60 * 1000L); // 10分钟定位一次

        locationClient.setLocationOption(locationOption);
    }

    /**
     * 开始定位（无回调版本）
     * 定位结果将存储在latitude和longitude属性中
     */
    public void getLocation() {
        Timber.tag(TAG).i( "开始定位(无回调版本)");
        if (locationClient != null) {
            locationClient.setLocationListener(createLocationListener(null));
            locationClient.startLocation();
        }
    }

    /**
     * 开始定位并设置定位结果回调
     * @param callback 定位结果回调
     */
    public void getLocation(LocationCallback callback) {
        Timber.tag(TAG).i( "开始定位");
        if (locationClient != null) {
            locationClient.setLocationListener(createLocationListener(callback));
            locationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    public void stopLocation() {
        if (locationClient != null) {
            locationClient.stopLocation();
        }
    }

    /**
     * 释放定位资源
     */
    public void destroyLocation() {
        if (locationClient != null) {
            locationClient.onDestroy();
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    // 示例使用代码
    public void startLocation(LocationCallback callback) {
        getLocation(callback);
    }

    // 自定义方法：检查用户是否已同意隐私政策
    private boolean checkUserAgreedPrivacy() {
        // 这里应该实现检查用户是否已经同意隐私政策的逻辑
        // 可以使用SharedPreferences存储用户的选择
        // 示例代码：
        // SharedPreferences sp = getSharedPreferences("PrivacySettings", MODE_PRIVATE);
        // return sp.getBoolean("hasAgreed", false);

        // 为了演示，这里假设用户已经同意
        return true;
    }

    // 自定义方法：展示隐私政策对话框
    private void showPrivacyDialog(Context context) {
        // 这里应该实现展示隐私政策的对话框
        // 当用户点击同意后，调用initLocation()方法初始化定位
        // 示例代码：
        /*
        new AlertDialog.Builder(context)
            .setTitle("隐私政策")
            .setMessage("请阅读我们的隐私政策...")
            .setPositiveButton("同意", (dialog, which) -> {
                // 保存用户同意状态
                SharedPreferences sp = context.getSharedPreferences("PrivacySettings", Context.MODE_PRIVATE);
                sp.edit().putBoolean("hasAgreed", true).apply();
                // 初始化定位
                initLocation(context);
            })
            .setNegativeButton("拒绝", (dialog, which) -> {
                dialog.dismiss();
                // 可以选择关闭应用或者采取其他措施
                ((Activity) context).finish();
            })
            .show();
        */
    }
}

