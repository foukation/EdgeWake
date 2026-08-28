package com.example.service_api.map

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import timber.log.Timber

object GMapHelper {
    private const val TAG = "GMapHelper"
    lateinit var locationClient: AMapLocationClient
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var lastUpdateTime: Long = 0L

    private fun createLocationListener(callback: LocationCallback? = null): AMapLocationListener {
        return object : AMapLocationListener {
            override fun onLocationChanged(amapLocation: AMapLocation?) {
                amapLocation?.let {
                    var currentUpdateTime: Long = System.currentTimeMillis()
                    if (it.errorCode == 0) {
                        if (lastUpdateTime == 0L || currentUpdateTime - lastUpdateTime > 5 * 1000L) {
                            lastUpdateTime = currentUpdateTime
                            Timber.tag(TAG).i("定位成功 - 纬度: ${it.latitude}, 经度: ${it.longitude}, 地址: $it")
                            latitude = it.latitude
                            longitude = it.longitude
                            callback?.onLocationResult(it, 0, null)
                            stopLocation()
                        } else {
                            Timber.tag(TAG).i("定位结果未更新，忽略此次回调")
                        }
                    } else {
                        Timber.tag(TAG).e("定位失败 - 错误码: ${it.errorCode}, 错误信息: ${it.errorInfo}")
                        callback?.onLocationResult(null, it.errorCode, it.errorInfo)
                    }
                }
            }
        }
    }

    interface LocationCallback {
        fun onLocationResult(location: AMapLocation?, errorCode: Int, errorInfo: String?)
    }

    /**
     * 初始化定位服务
     * @param context 应用上下文
     */
    fun initLocation(context: Context) {
        Timber.tag(TAG).i("初始化定位服务")
        AMapLocationClient.updatePrivacyShow(context.applicationContext, true, true)
        AMapLocationClient.updatePrivacyAgree(context.applicationContext, true)
        locationClient = AMapLocationClient(context.applicationContext)
        val locationOption = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        }
//        locationOption.isNeedAddress = true
//        locationOption.interval = 10 * 60 * 1000L //10分钟定位一次
        locationClient.setLocationOption(locationOption)
//        getLocation()
    }

    /**
     * 开始定位（无回调版本）
     * 定位结果将存储在latitude和longitude属性中
     */
    fun getLocation() {
        Timber.tag(TAG).i("开始定位(无回调版本)")
        if (::locationClient.isInitialized) {
            locationClient.setLocationListener(createLocationListener())
            locationClient.startLocation()
        }
    }


  /*  for java 版本
  public void startLocation(){
        // 开始定位并设置回调
        GMapHelper.INSTANCE.startLocation(new GMapHelper.LocationCallback() {
            @Override
            public void onLocationResult(AMapLocation location, int errorCode, String errorInfo) {
                if (errorCode == 0 && location != null) {
                    // 处理定位成功
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String address = location.getAddress();
                    // 使用定位信息，例如更新地图位置等
//                    Timber.tag("Location").i("latitude :" + latitude + " longitude :" + longitude+ " address :" + address);
//                    GMapHelper.INSTANCE.stopLocation();
                } else {
                    // 处理定位失败
                    Timber.tag("Location").i("定位失败");
                }
            }
        });
    }*/

/*    for kotlin 版本
fun startLocation() {
        // 开始定位并设置回调
        GMapHelper.INSTANCE.startLocation(object : LocationCallback() {
            public override fun onLocationResult(
                location: AMapLocation?,
                errorCode: Int,
                errorInfo: String?
            ) {
                if (errorCode == 0 && location != null) {
                    // 处理定位成功
                    val latitude = location.getLatitude()
                    val longitude = location.getLongitude()
                    val address = location.getAddress()
                    // 使用定位信息，例如更新地图位置等
//                    Timber.tag("Location").i("latitude :" + latitude + " longitude :" + longitude+ " address :" + address);
//                    GMapHelper.INSTANCE.stopLocation();
                } else {
                    // 处理定位失败
                    Timber.tag("Location").i("定位失败")
                }
            }
        })
    }*/

    /**
     * 开始定位并设置定位结果回调
     * @param callback 定位结果回调
     */
    fun getLocation(callback: LocationCallback) {
        Timber.tag(TAG).i("开始定位")
        if (::locationClient.isInitialized) {
            locationClient.setLocationListener(createLocationListener(callback))
            locationClient.startLocation()
        }
    }

    /**
     * 停止定位
     */
    fun stopLocation() {
        if (::locationClient.isInitialized) {
            locationClient.stopLocation()
        }
    }

    /**
     * 释放定位资源
     */
    fun destroyLocation() {
        if (::locationClient.isInitialized) {
            locationClient.onDestroy()
        }
    }

    // 自定义方法：检查用户是否已同意隐私政策
    private fun checkUserAgreedPrivacy(): Boolean {
        // 这里应该实现检查用户是否已经同意隐私政策的逻辑
        // 可以使用SharedPreferences存储用户的选择
        // 示例代码：
        // SharedPreferences sp = getSharedPreferences("PrivacySettings", MODE_PRIVATE);
        // return sp.getBoolean("hasAgreed", false);

        // 为了演示，这里假设用户已经同意
        return true
    }
    // 自定义方法：展示隐私政策对话框
    private fun showPrivacyDialog() {
        // 这里应该实现展示隐私政策的对话框
        // 当用户点击同意后，调用initLocation()方法初始化定位
        // 示例代码：
        // new AlertDialog.Builder(this)
        //     .setTitle("隐私政策")
        //     .setMessage("请阅读我们的隐私政策...")
        //     .setPositiveButton("同意", (dialog, which) -> {
        //         // 保存用户同意状态
        //         SharedPreferences sp = getSharedPreferences("PrivacySettings", MODE_PRIVATE);
        //         sp.edit().putBoolean("hasAgreed", true).apply();
        //         // 初始化定位
        //         initLocation();
        //     })
        //     .setNegativeButton("拒绝", (dialog, which) -> {
        //         dialog.dismiss();
        //         // 可以选择关闭应用或者采取其他措施
        //         finish();
        //     })
        //     .show();
    }
}