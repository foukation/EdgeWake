package com.fxzs.lingxiagent.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 网络状态管理器
 * 监听网络状态变化并提供回调
 */
public class NetworkStateManager {
    
    /**
     * 网络状态监听器接口
     */
    public interface NetworkStateListener {
        void onNetworkAvailable();
        void onNetworkLost();
        void onNetworkChanged(boolean isConnected);
    }
    
    private Context context;
    private ConnectivityManager connectivityManager;
    private List<NetworkStateListener> listeners;
    private boolean isNetworkAvailable;
    
    // Android N及以上版本使用的网络回调
    private ConnectivityManager.NetworkCallback networkCallback;
    
    // Android N以下版本使用的广播接收器
    private BroadcastReceiver networkReceiver;
    
    public NetworkStateManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listeners = new ArrayList<>();
        this.isNetworkAvailable = isNetworkCurrentlyAvailable();
        
        initNetworkMonitoring();
    }
    
    /**
     * 初始化网络监听
     */
    private void initNetworkMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android N及以上版本使用NetworkCallback
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    handleNetworkChange(true);
                }
                
                @Override
                public void onLost(@NonNull Network network) {
                    handleNetworkChange(false);
                }
                
                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    handleNetworkChange(hasInternet);
                }
            };
            
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
            
        } else {
            // Android N以下版本使用BroadcastReceiver
            networkReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    boolean isConnected = isNetworkCurrentlyAvailable();
                    handleNetworkChange(isConnected);
                }
            };
            
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(networkReceiver, filter);
        }
    }
    
    /**
     * 处理网络状态变化
     */
    private void handleNetworkChange(boolean isConnected) {
        if (isNetworkAvailable != isConnected) {
            isNetworkAvailable = isConnected;
            
            for (NetworkStateListener listener : listeners) {
                if (isConnected) {
                    listener.onNetworkAvailable();
                } else {
                    listener.onNetworkLost();
                }
                listener.onNetworkChanged(isConnected);
            }
        }
    }
    
    /**
     * 检查当前网络是否可用
     */
    private boolean isNetworkCurrentlyAvailable() {
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null && 
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } else {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }
    
    /**
     * 添加网络状态监听器
     */
    public void addNetworkStateListener(NetworkStateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 移除网络状态监听器
     */
    public void removeNetworkStateListener(NetworkStateListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 获取当前网络状态
     */
    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }
    
    /**
     * 获取网络类型
     */
    public String getNetworkType() {
        if (!isNetworkAvailable) {
            return "无网络";
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return "未知";
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities == null) {
                return "未知";
            }
            
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WiFi";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "移动网络";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "以太网";
            } else {
                return "其他";
            }
        } else {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo == null) {
                return "未知";
            }
            
            switch (activeNetworkInfo.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return "WiFi";
                case ConnectivityManager.TYPE_MOBILE:
                    return "移动网络";
                case ConnectivityManager.TYPE_ETHERNET:
                    return "以太网";
                default:
                    return "其他";
            }
        }
    }
    
    /**
     * 销毁网络监听
     */
    public void destroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        } else if (networkReceiver != null) {
            context.unregisterReceiver(networkReceiver);
            networkReceiver = null;
        }
        
        listeners.clear();
        context = null;
        connectivityManager = null;
    }
}