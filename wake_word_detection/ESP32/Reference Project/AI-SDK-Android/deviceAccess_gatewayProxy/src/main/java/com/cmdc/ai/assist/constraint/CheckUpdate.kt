/*
 * 新版本APK升级检测。
 */

internal data class CheckUpdateReq(
    val brand: String,        // 系统，安卓的检测升级传Android，iOS的传iOS（Android iOS）
    val model: String,        // 手机型号，以系统获取为准。（RedMi K60、Huawei Mate 60、ViVO x100）
    val osVersion: String,    // 系统版本，以系统获取为准。（Android 13、Android 12）
    val androidId: String,    // 设备唯一标识
    val versionCode: String,  // 版本号
    val versionName: String,  // 版本名称
    val packageName: String,  // APP包名
)

internal data class CheckUpdateRes(
    val updateMode: Int,	    // 升级模式  0 实时升级(提醒用户升级) 、1 强制升级(必须升级APP，否则无法使用)
    val updateContent: String,	// 更新内容说明
    val versionCode: Long,	    // 新版本号
    val versionName: String,	// 新版本名称
    val size: Long,	            // 升级包大小，字节
    val updateTime: Long,	    // 升级utc时间戳(10位)，精确到秒。为空表示不指定时间
    val downloadUrl: String,	// 安装包下载地址
    val sha256: String,	        // 安装包sha256值（大写）
    val updatePrivacy: Boolean,	// 是否涉及更新隐私协议  true 涉及隐私协议更新 、false 不涉及隐私协议更新
)