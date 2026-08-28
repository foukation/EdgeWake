package com.fxzs.lingxiagent.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

/**
 * 网络监听
 *
 * 功能：
 * 1. 监听网络恢复
 * 2. 监听断网
 * 3. 判断真正联网
 * 4. 防止重复回调
 */
public class NetworkMonitor {

    private static final String TAG = "NetworkMonitor";

    private final ConnectivityManager connectivityManager;

    private ConnectivityManager.NetworkCallback networkCallback;

    /**
     * 上一次网络状态
     */
    private boolean lastConnected = false;

    /**
     * 防抖
     */
    private long lastCallbackTime = 0;

    public interface Listener {

        /**
         * 网络恢复
         */
        void onNetworkAvailable();

        /**
         * 网络断开
         */
        void onNetworkLost();
    }

    public NetworkMonitor(Context context) {

        connectivityManager =
                (ConnectivityManager)
                        context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * 注册监听
     */
    public void register(Listener listener) {

        if (networkCallback != null) {
            return;
        }

        // 初始化当前网络状态
        lastConnected = isNetworkConnected();

        Log.e(TAG, "初始化网络状态: " + lastConnected);

        networkCallback =
                new ConnectivityManager.NetworkCallback() {

                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);

                        Log.e(TAG, "onAvailable");

                        checkNetworkState(listener);
                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);

                        Log.e(TAG, "onLost");

                        checkNetworkState(listener);
                    }

                    @Override
                    public void onCapabilitiesChanged(
                            Network network,
                            NetworkCapabilities networkCapabilities
                    ) {
                        super.onCapabilitiesChanged(
                                network,
                                networkCapabilities
                        );

                        Log.e(TAG, "onCapabilitiesChanged");

                        checkNetworkState(listener);
                    }
                };

        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    /**
     * 注销监听
     */
    public void unregister() {

        if (networkCallback != null) {

            connectivityManager.unregisterNetworkCallback(networkCallback);

            networkCallback = null;
        }
    }

    /**
     * 检查网络状态变化
     */
    private void checkNetworkState(Listener listener) {

        // 防抖
        if (!canCallback()) {
            return;
        }

        boolean connected = isNetworkConnected();

        Log.e(TAG, "当前网络状态: " + connected);

        // 无网 -> 有网
        if (!lastConnected && connected) {

            lastConnected = true;

            Log.e(TAG, "网络恢复");

            if (listener != null) {
                listener.onNetworkAvailable();
            }

            return;
        }

        // 有网 -> 无网
        if (lastConnected && !connected) {

            lastConnected = false;

            Log.e(TAG, "网络断开");

            if (listener != null) {
                listener.onNetworkLost();
            }
        }
    }

    /**
     * 是否真正联网
     */
    public boolean isNetworkConnected() {

        try {

            Network network = connectivityManager.getActiveNetwork();

            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(network);

            if (capabilities == null) {
                return false;
            }

            // 是否具备联网能力
            return capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET);

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 防止短时间重复回调
     */
    private boolean canCallback() {

        long now = System.currentTimeMillis();

        if (now - lastCallbackTime < 1500) {
            return false;
        }

        lastCallbackTime = now;

        return true;
    }
}