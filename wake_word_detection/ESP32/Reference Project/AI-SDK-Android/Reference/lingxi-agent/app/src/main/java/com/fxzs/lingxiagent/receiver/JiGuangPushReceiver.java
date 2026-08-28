package com.fxzs.lingxiagent.receiver;

//import cn.jpush.android.api.JPushInterface;

/**
 * 极光推送自定义接收器
 * 用于接收处理极光推送的各种消息和通知
 */
//public class JiGuangPushReceiver extends BroadcastReceiver {
//    private static final String TAG = "JiGuangPushReceiver";
//    private static final String CHANNEL_ID = "lingxiagent_push_channel";
//    private static final String CHANNEL_NAME = "通通助手推送通知";
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        try {
//            Bundle bundle = intent.getExtras();
//            Timber.tag(TAG).d( "[JiGuangPushReceiver] onReceive - " + intent.getAction() + ", extras: " + printBundle(bundle));
//
//            if (JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
//                // 极光推送注册成功，获取Registration ID
//                String regId = bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID);
//                Timber.tag(TAG).d( "[JiGuangPushReceiver] 极光推送注册成功，Registration ID: " + regId);
//                // 可以将Registration ID发送到服务器
//                processRegistrationId(context, regId);
//
//            } else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
//                // 接收到推送下来的自定义消息
//                Timber.tag(TAG).d( "[JiGuangPushReceiver] 接收到推送下来的自定义消息: " + bundle.getString(JPushInterface.EXTRA_MESSAGE));
//                processCustomMessage(context, bundle);
//
//            } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
//                // 接收到推送下来的通知
//                Timber.tag(TAG).d( "[JiGuangPushReceiver] 接收到推送下来的通知");
//                int notifactionId = bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID);
//                Timber.tag(TAG).d( "[JiGuangPushReceiver] 接收到推送下来的通知的ID: " + notifactionId);
//                processNotification(context, bundle);
//
//            } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
//                // 用户点击打开了通知
//                Timber.tag(TAG).d( "[JiGuangPushReceiver] 用户点击打开了通知");
//                processNotificationOpened(context, bundle);
//
//            } else if (JPushInterface.ACTION_RICHPUSH_CALLBACK.equals(intent.getAction())) {
//                // 用户收到到RICH PUSH CALLBACK
//                Timber.tag(TAG).d( "[JiGuangPushReceiver] 用户收到到RICH PUSH CALLBACK: " + bundle.getString(JPushInterface.EXTRA_EXTRA));
//                // 在这里根据 JPushInterface.EXTRA_EXTRA 的内容处理代码，比如打开新的Activity， 打开一个网页等..
//
//            } else if (JPushInterface.ACTION_CONNECTION_CHANGE.equals(intent.getAction())) {
//                // 极光推送连接状态变更
//                boolean connected = intent.getBooleanExtra(JPushInterface.EXTRA_CONNECTION_CHANGE, false);
//                Timber.tag(TAG).d( "[JiGuangPushReceiver] 极光推送连接状态变更，connected state change to " + connected);
//
//            } else {
//                Timber.tag(TAG).d( "[JiGuangPushReceiver] Unhandled intent - " + intent.getAction());
//            }
//        } catch (Exception e) {
//            Timber.tag(TAG).e( "[JiGuangPushReceiver] 处理推送消息异常", e);
//        }
//    }
//
//    /**
//     * 打印所有的 intent extra 数据
//     */
//    private static String printBundle(Bundle bundle) {
//        StringBuilder sb = new StringBuilder();
//        for (String key : bundle.keySet()) {
//            if (key.equals(JPushInterface.EXTRA_NOTIFICATION_ID)) {
//                sb.append("\nkey:").append(key).append(", value:").append(bundle.getInt(key));
//            } else if (key.equals(JPushInterface.EXTRA_CONNECTION_CHANGE)) {
//                sb.append("\nkey:").append(key).append(", value:").append(bundle.getBoolean(key));
//            } else if (key.equals(JPushInterface.EXTRA_EXTRA)) {
//                if (TextUtils.isEmpty(bundle.getString(JPushInterface.EXTRA_EXTRA))) {
//                    Timber.tag(TAG).i(, "This message has no Extra data");
//                    continue;
//                }
//
//                try {
//                    JSONObject json = new JSONObject(bundle.getString(JPushInterface.EXTRA_EXTRA));
//                    Iterator<String> it = json.keys();
//
//                    while (it.hasNext()) {
//                        String myKey = it.next();
//                        sb.append("\nkey:").append(key).append(", value: [").append(myKey).append(" - ").append(json.optString(myKey)).append("]");
//                    }
//                } catch (JSONException e) {
//                    Timber.tag(TAG).e( "Get message extra JSON error!");
//                }
//            } else {
//                sb.append("\nkey:").append(key).append(", value:").append(bundle.get(key));
//            }
//        }
//        return sb.toString();
//    }
//
//    /**
//     * 处理Registration ID
//     */
//    private void processRegistrationId(Context context, String regId) {
//        // 将Registration ID保存到SharedPreferences或发送到服务器
//        Timber.tag(TAG).i(, "processRegistrationId: " + regId);
//        // TODO: 根据业务需求处理Registration ID
//    }
//
//    /**
//     * 处理自定义消息
//     */
//    private void processCustomMessage(Context context, Bundle bundle) {
//        String message = bundle.getString(JPushInterface.EXTRA_MESSAGE);
//        String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
//        String contentType = bundle.getString(JPushInterface.EXTRA_CONTENT_TYPE);
//        String title = bundle.getString(JPushInterface.EXTRA_TITLE);
//        String msgId = bundle.getString(JPushInterface.EXTRA_MSG_ID);
//
//        Timber.tag(TAG).i(, "收到自定义消息: title=" + title + ", message=" + message + ", extras=" + extras + ", contentType=" + contentType);
//
//        // 创建本地通知显示自定义消息
//        showNotification(context, title, message, extras);
//    }
//
//    /**
//     * 处理通知消息
//     */
//    private void processNotification(Context context, Bundle bundle) {
//        String title = bundle.getString(JPushInterface.EXTRA_NOTIFICATION_TITLE);
//        String alert = bundle.getString(JPushInterface.EXTRA_ALERT);
//        String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
//        int notificationId = bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID);
//
//        Timber.tag(TAG).i(, "收到通知: title=" + title + ", alert=" + alert + ", extras=" + extras + ", notificationId=" + notificationId);
//
//        // 这里可以根据需要对通知进行额外处理
//        // 比如：保存到数据库、更新未读消息数等
//    }
//
//    /**
//     * 处理通知点击事件
//     */
//    private void processNotificationOpened(Context context, Bundle bundle) {
//        String title = bundle.getString(JPushInterface.EXTRA_NOTIFICATION_TITLE);
//        String alert = bundle.getString(JPushInterface.EXTRA_ALERT);
//        String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
//
//        Timber.tag(TAG).i(, "用户点击通知: title=" + title + ", alert=" + alert + ", extras=" + extras);
//
//        // 解析extras中的自定义参数
//        try {
//            Intent intent = new Intent(context, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//            if (!TextUtils.isEmpty(extras)) {
//                JSONObject extrasJson = new JSONObject(extras);
//                // 根据自定义参数决定跳转逻辑
//                String type = extrasJson.optString("type");
//                String targetId = extrasJson.optString("targetId");
//
//                intent.putExtra("push_type", type);
//                intent.putExtra("push_target_id", targetId);
//
//                Timber.tag(TAG).i(, "推送跳转参数: type=" + type + ", targetId=" + targetId);
//            }
//
//            context.startActivity(intent);
//        } catch (JSONException e) {
//            Timber.tag(TAG).e( "解析推送extras失败", e);
//            // 出错时默认打开主页
//            Intent intent = new Intent(context, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            context.startActivity(intent);
//        }
//    }
//
//    /**
//     * 显示本地通知
//     */
//    private void showNotification(Context context, String title, String message, String extras) {
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
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setContentIntent(pendingIntent)
//                .setAutoCancel(true)
//                .setDefaults(NotificationCompat.DEFAULT_ALL);
//
//        // 显示通知
//        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
//    }
//}