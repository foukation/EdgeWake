package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * 网络环境检测工具类
 * 用于检测当前网络连接类型：WiFi、5G、4G等
 */
object NetworkUtils {

    /**
     * 网络类型枚举
     */
    enum class NetworkType {
        NONE,      // 无网络连接
        WIFI,      // WiFi网络
        MOBILE_5G, // 5G移动网络
        MOBILE_4G, // 4G移动网络
        MOBILE_3G, // 3G移动网络
        MOBILE_2G, // 2G移动网络
        MOBILE_UNKNOWN // 其他移动网络
    }

    /**
     * 获取当前网络类型
     * @param context 上下文
     * @return 网络类型
     */
    private fun getNetworkType(context: Context): NetworkType {
        // 检查网络权限
        if (!hasNetworkPermission(context)) {
            return NetworkType.NONE
        }

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NetworkType.NONE

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getNetworkTypeForApi23AndAbove(connectivityManager, context)
        } else {
            getNetworkTypeForApiBelow23(connectivityManager)
        }
    }

    /**
     * 检查是否有网络连接
     * @param context 上下文
     * @return true表示有网络连接，false表示无网络连接
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        return getNetworkType(context) != NetworkType.NONE
    }

    /**
     * 检查是否是WiFi网络
     * @param context 上下文
     * @return true表示是WiFi网络
     */
    private fun isWiFi(context: Context): Boolean {
        return getNetworkType(context) == NetworkType.WIFI
    }

    /**
     * 检查是否是移动网络
     * @param context 上下文
     * @return true表示是移动网络（包括2G/3G/4G/5G）
     */
    private fun isMobileNetwork(context: Context): Boolean {
        val networkType = getNetworkType(context)
        return networkType == NetworkType.MOBILE_2G ||
                networkType == NetworkType.MOBILE_3G ||
                networkType == NetworkType.MOBILE_4G ||
                networkType == NetworkType.MOBILE_5G ||
                networkType == NetworkType.MOBILE_UNKNOWN
    }

    /**
     * 检查是否是5G网络
     * @param context 上下文
     * @return true表示是5G网络
     */
    private fun is5G(context: Context): Boolean {
        return getNetworkType(context) == NetworkType.MOBILE_5G
    }

    /**
     * 检查是否是4G网络
     * @param context 上下文
     * @return true表示是4G网络
     */
    private fun is4G(context: Context): Boolean {
        return getNetworkType(context) == NetworkType.MOBILE_4G
    }

    /**
     * 获取网络类型的字符串描述
     * @param context 上下文
     * @return 网络类型描述
     */
    fun getNetworkTypeString(context: Context): String {
        return when (getNetworkType(context)) {
            NetworkType.NONE -> "无网络"
            NetworkType.WIFI -> "WiFi"
            NetworkType.MOBILE_5G -> "5G"
            NetworkType.MOBILE_4G -> "4G"
            NetworkType.MOBILE_3G -> "3G"
            NetworkType.MOBILE_2G -> "2G"
            NetworkType.MOBILE_UNKNOWN -> "移动网络"
        }
    }

    /**
     * Android 6.0及以上版本的网络类型检测
     */
    private fun getNetworkTypeForApi23AndAbove(
        connectivityManager: ConnectivityManager,
        context: Context
    ): NetworkType {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                getMobileNetworkType(context)
            }

            else -> NetworkType.NONE
        }
    }

    /**
     * Android 6.0以下版本的网络类型检测
     */
    @Suppress("DEPRECATION")
    private fun getNetworkTypeForApiBelow23(connectivityManager: ConnectivityManager): NetworkType {
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected) {
            return NetworkType.NONE
        }

        return when (activeNetworkInfo.type) {
            ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
            ConnectivityManager.TYPE_MOBILE -> {
                getMobileNetworkTypeFromSubtype(activeNetworkInfo.subtype)
            }

            else -> NetworkType.NONE
        }
    }

    /**
     * 获取移动网络类型（通过TelephonyManager）
     */
    private fun getMobileNetworkType(context: Context): NetworkType {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return NetworkType.MOBILE_UNKNOWN

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11及以上，使用 dataNetworkType
                when (telephonyManager.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_LTE -> NetworkType.MOBILE_4G
                    20 -> NetworkType.MOBILE_5G // NETWORK_TYPE_NR = 20 (API 29+)
                    else -> getMobileNetworkTypeFromSubtype(telephonyManager.dataNetworkType)
                }
            } else {
                @Suppress("DEPRECATION")
                (getMobileNetworkTypeFromSubtype(
        telephonyManager.networkType
    ))
            }
        } catch (e: SecurityException) {
            NetworkType.MOBILE_UNKNOWN
        }
    }

    /**
     * 根据网络子类型判断移动网络类型
     * @param subtype 网络子类型常量值
     * @return 对应的NetworkType枚举值
     */
    private fun getMobileNetworkTypeFromSubtype(subtype: Int): NetworkType {
        return when (subtype) {
            // 5G (只在API 29+可用)
            20 -> { // TelephonyManager.NETWORK_TYPE_NR 的值
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    NetworkType.MOBILE_5G
                } else {
                    NetworkType.MOBILE_UNKNOWN
                }
            }

            // 4G
            TelephonyManager.NETWORK_TYPE_LTE -> NetworkType.MOBILE_4G

            // 3G
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP -> NetworkType.MOBILE_3G

            // 2G
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> NetworkType.MOBILE_2G

            else -> NetworkType.MOBILE_UNKNOWN
        }
    }

    /**
     * 检查是否有网络权限
     */
    private fun hasNetworkPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
