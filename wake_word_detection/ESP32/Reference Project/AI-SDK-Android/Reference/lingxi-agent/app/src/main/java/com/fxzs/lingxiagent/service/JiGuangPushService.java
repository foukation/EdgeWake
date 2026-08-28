//package com.fxzs.lingxiagent.service;
//
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Build;
//import android.text.TextUtils;
//import android.util.Log;
//
//import androidx.core.app.NotificationCompat;
//
//import com.fxzs.lingxiagent.R;
//import com.fxzs.lingxiagent.MainActivity;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import cn.jpush.android.api.CmdMessage;
//import cn.jpush.android.api.CustomMessage;
//import cn.jpush.android.api.JPushMessage;
//import cn.jpush.android.api.NotificationMessage;
//import cn.jpush.android.service.JPushMessageReceiver;
//
///**
// * 极光推送自定义服务
// * 继承自JPushMessageService，用于处理推送相关的各种回调
// */
//public class JiGuangPushService extends JPushMessageReceiver {
//    private static final String TAG = "JiGuangPushService";
//    private static final String CHANNEL_ID = "lingxiagent_push_channel";
//    private static final String CHANNEL_NAME = "通通助手推送通知";
//
//    /**
//     * 收到推送通知时回调
//     */
//    @Override
//    public Notification getNotification(Context context, NotificationMessage notificationMessage) {
//        Timber.tag(TAG).d( "[getNotification] 收到推送通知: " + notificationMessage.toString());
//
//        // 可以自定义通知样式
//        return buildNotification(context, notificationMessage);
//    }
//
//    /**
//     * 收到自定义消息时回调
//     */
//    @Override
//    public void onMessage(Context context, CustomMessage customMessage) {
//        Timber.tag(TAG).d( "[onMessage] 收到自定义消息: " + customMessage.toString());
//
//        // 处理自定义消息
//        processCustomMessage(context, customMessage);
//    }
//
//    /**
//     * 点击通知时回调
//     */
//    @Override
//    public void onNotifyMessageOpened(Context context, NotificationMessage message) {
//        Timber.tag(TAG).d( "[onNotifyMessageOpened] 用户点击了通知: " + message.toString());
//
//        // 处理通知点击事件
//        handleNotificationClick(context, message);
//    }
//
//    /**
//     * 收到通知时回调（仅在前台时调用）
//     */
//    @Override
//    public void onNotifyMessageArrived(Context context, NotificationMessage message) {
//        Timber.tag(TAG).d( "[onNotifyMessageArrived] 收到通知（前台）: " + message.toString());
//
//        // 可以在这里处理前台收到通知的逻辑
//        // 比如：更新应用内的消息数量、显示应用内通知等
//    }
//
//    /**
//     * 通知被清除时回调
//     */
//    @Override
//    public void onNotifyMessageDismiss(Context context, NotificationMessage message) {
//        Timber.tag(TAG).d( "[onNotifyMessageDismiss] 通知被清除: " + message.toString());
//    }
//
//    /**
//     * 注册成功时回调
//     */
//    @Override
//    public void onRegister(Context context, String registrationId) {
//        Timber.tag(TAG).d( "[onRegister] 注册成功，registrationId: " + registrationId);
//
//        // 保存Registration ID
//        // 保存Registration ID到SharedPreferences
//        // TODO: 需要添加通用的putString方法到SharedPreferencesUtil
//
//        // TODO: 将Registration ID上报到服务器
//        uploadRegistrationId(context, registrationId);
//    }
//
//    /**
//     * 长连接状态变化时回调
//     */
//    @Override
//    public void onConnected(Context context, boolean isConnected) {
//        Timber.tag(TAG).d( "[onConnected] 长连接状态: " + (isConnected ? "已连接" : "已断开"));
//    }
//
//    /**
//     * 交互事件回调
//     */
//    @Override
//    public void onCommandResult(Context context, CmdMessage cmdMessage) {
//        Timber.tag(TAG).d( "[onCommandResult] " + cmdMessage.toString());
//    }
//
//    /**
//     * tag 增删查改的操作会在此方法中回调结果
//     */
//    @Override
//    public void onTagOperatorResult(Context context, JPushMessage jPushMessage) {
//        Timber.tag(TAG).d( "[onTagOperatorResult] " + jPushMessage.toString());
//        super.onTagOperatorResult(context, jPushMessage);
//    }
//
//    /**
//     * 查询某个 tag 与当前用户的绑定状态的操作会在此方法中回调结果
//     */
//    @Override
//    public void onCheckTagOperatorResult(Context context, JPushMessage jPushMessage) {
//        Timber.tag(TAG).d( "[onCheckTagOperatorResult] " + jPushMessage.toString());
//        super.onCheckTagOperatorResult(context, jPushMessage);
//    }
//
//    /**
//     * alias 相关的操作会在此方法中回调结果
//     */
//    @Override
//    public void onAliasOperatorResult(Context context, JPushMessage jPushMessage) {
//        Timber.tag(TAG).d( "[onAliasOperatorResult] " + jPushMessage.toString());
//        super.onAliasOperatorResult(context, jPushMessage);
//    }
//
//    /**
//     * 设置手机号码会在此方法中回调结果
//     */
//    @Override
//    public void onMobileNumberOperatorResult(Context context, JPushMessage jPushMessage) {
//        Timber.tag(TAG).d( "[onMobileNumberOperatorResult] " + jPushMessage.toString());
//        super.onMobileNumberOperatorResult(context, jPushMessage);
//    }
//
//    /**
//     * 构建自定义通知
//     */
//    private Notification buildNotification(Context context, NotificationMessage message) {
//        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // 创建通知渠道（Android O及以上版本需要）
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID,
//                    CHANNEL_NAME,
//                    NotificationManager.IMPORTANCE_DEFAULT
//            );
//            channel.setDescription("通通助手推送通知渠道");
//            channel.enableLights(true);
//            channel.enableVibration(true);
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        // 构建点击通知的Intent
//        Intent intent = new Intent(context, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        intent.putExtra("notification_message", message.toString());
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                context,
//                0,
//                intent,
//                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
//                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//                    : PendingIntent.FLAG_UPDATE_CURRENT
//        );
//
//        // 构建通知
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle(message.notificationTitle)
//                .setContentText(message.notificationContent)
//                .setStyle(new NotificationCompat.BigTextStyle()
//                        .bigText(message.notificationContent))
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setContentIntent(pendingIntent)
//                .setAutoCancel(true)
//                .setDefaults(NotificationCompat.DEFAULT_ALL);
//
//        // 设置大图标（如果有）
//        if (!TextUtils.isEmpty(message.notificationLargeIcon)) {
//            // TODO: 加载网络图片作为大图标
//        }
//
//        return builder.build();
//    }
//
//    /**
//     * 处理自定义消息
//     */
//    private void processCustomMessage(Context context, CustomMessage customMessage) {
//        String message = customMessage.message;
//        String contentType = customMessage.contentType;
//        String title = customMessage.title;
//        String extra = customMessage.extra;
//
//        Timber.tag(TAG).i(, "处理自定义消息: title=" + title + ", message=" + message + ", extra=" + extra);
//
//        // 根据消息类型进行不同处理
//        try {
//            if (!TextUtils.isEmpty(extra)) {
//                JSONObject extraJson = new JSONObject(extra);
//                String type = extraJson.optString("type");
//
//                switch (type) {
//                    case "chat":
//                        // 聊天消息，可能需要更新聊天界面
//                        handleChatMessage(context, extraJson);
//                        break;
//                    case "system":
//                        // 系统消息
//                        handleSystemMessage(context, extraJson);
//                        break;
//                    case "update":
//                        // 更新提醒
//                        handleUpdateMessage(context, extraJson);
//                        break;
//                    default:
//                        // 默认显示通知
//                        showCustomNotification(context, title, message, extra);
//                        break;
//                }
//            } else {
//                // 默认显示通知
//                showCustomNotification(context, title, message, extra);
//            }
//        } catch (JSONException e) {
//            Timber.tag(TAG).e( "解析自定义消息extra失败", e);
//            showCustomNotification(context, title, message, extra);
//        }
//    }
//
//    /**
//     * 处理通知点击事件
//     */
//    private void handleNotificationClick(Context context, NotificationMessage message) {
//        try {
//            Intent intent = new Intent(context, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//            // 解析附加字段
//            if (!TextUtils.isEmpty(message.notificationExtras)) {
//                JSONObject extrasJson = new JSONObject(message.notificationExtras);
//                String type = extrasJson.optString("type");
//                String targetId = extrasJson.optString("targetId");
//
//                intent.putExtra("push_type", type);
//                intent.putExtra("push_target_id", targetId);
//
//                Timber.tag(TAG).i(, "通知点击跳转参数: type=" + type + ", targetId=" + targetId);
//            }
//
//            context.startActivity(intent);
//        } catch (JSONException e) {
//            Timber.tag(TAG).e( "解析通知extras失败", e);
//            // 出错时默认打开主页
//            Intent intent = new Intent(context, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            context.startActivity(intent);
//        }
//    }
//
//    /**
//     * 处理聊天消息
//     */
//    private void handleChatMessage(Context context, JSONObject extraJson) {
//        String chatId = extraJson.optString("chatId");
//        String senderId = extraJson.optString("senderId");
//        String content = extraJson.optString("content");
//
//        Timber.tag(TAG).i(, "收到聊天消息: chatId=" + chatId + ", senderId=" + senderId);
//
//        // TODO: 更新本地数据库、发送广播通知聊天界面更新等
//    }
//
//    /**
//     * 处理系统消息
//     */
//    private void handleSystemMessage(Context context, JSONObject extraJson) {
//        String msgId = extraJson.optString("msgId");
//        String content = extraJson.optString("content");
//
//        Timber.tag(TAG).i(, "收到系统消息: msgId=" + msgId);
//
//        // TODO: 保存系统消息到本地等
//    }
//
//    /**
//     * 处理更新消息
//     */
//    private void handleUpdateMessage(Context context, JSONObject extraJson) {
//        String version = extraJson.optString("version");
//        String updateUrl = extraJson.optString("updateUrl");
//        boolean forceUpdate = extraJson.optBoolean("forceUpdate", false);
//
//        Timber.tag(TAG).i(, "收到更新消息: version=" + version + ", forceUpdate=" + forceUpdate);
//
//        // TODO: 显示更新对话框等
//    }
//
//    /**
//     * 显示自定义通知
//     */
//    private void showCustomNotification(Context context, String title, String message, String extras) {
//        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // 创建通知渠道
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID,
//                    CHANNEL_NAME,
//                    NotificationManager.IMPORTANCE_DEFAULT
//            );
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        // 构建Intent
//        Intent intent = new Intent(context, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        if (!TextUtils.isEmpty(extras)) {
//            intent.putExtra("push_extras", extras);
//        }
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                context,
//                0,
//                intent,
//                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
//                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//                    : PendingIntent.FLAG_UPDATE_CURRENT
//        );
//
//        // 构建通知
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle(TextUtils.isEmpty(title) ? "通通助手" : title)
//                .setContentText(message)
//                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setContentIntent(pendingIntent)
//                .setAutoCancel(true)
//                .setDefaults(NotificationCompat.DEFAULT_ALL);
//
//        // 显示通知
//        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
//    }
//
//    /**
//     * 上报Registration ID到服务器
//     */
//    private void uploadRegistrationId(Context context, String registrationId) {
//        // TODO: 实现上报逻辑
//        Timber.tag(TAG).i(, "准备上报Registration ID到服务器: " + registrationId);
//    }
//}