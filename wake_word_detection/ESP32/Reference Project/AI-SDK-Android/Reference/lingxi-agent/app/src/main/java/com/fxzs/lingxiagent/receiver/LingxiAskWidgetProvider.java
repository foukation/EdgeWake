package com.fxzs.lingxiagent.receiver;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.R;

public class LingxiAskWidgetProvider extends AppWidgetProvider {

    /**
     * 【关键新增】静态方法：供外部（如 MainActivity）调用，强制刷新所有该类型的小组件
     * 当用户开启权限返回应用时，必须调用此方法更新组件状态
     */
    public static void refreshAllWidgets(Context context) {
        Intent intent = new Intent(context, LingxiAskWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        // 获取当前桌面上所有该 Widget 的 ID
        ComponentName componentName = new ComponentName(context, LingxiAskWidgetProvider.class);
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(componentName);

        if (appWidgetIds.length > 0) {
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            // 发送广播，系统收到后会回调 onUpdate
            context.sendBroadcast(intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.lingxi_ask_widget);

            // 绑定点击事件：内部会自动检查权限
            setUrlClickWithPermissionCheck(context, views, R.id.iv_ask);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * 绑定点击事件：先检查悬浮窗权限
     * - 有权限：绑定跳转 URL (lingxiawakeup://...?target=wakeupwindow)
     * - 无权限：不绑定任何事件 (点击无反应)
     */
    private void setUrlClickWithPermissionCheck(Context context, RemoteViews views, int viewId) {
        // 1. 检查悬浮窗权限
        String urlStr = "lingxiawakeup://com.fxzs.lingxiagent?target=wakeupwindow&source=widget";
        Uri uri = Uri.parse(urlStr);

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.fxzs.lingxiagent"); // 指定包名，防止歧义
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // 3. 创建 PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 4. 将点击事件绑定到父容器及所有主要子视图
        // 确保点击头像、文字、背景都能触发
        views.setOnClickPendingIntent(viewId, pendingIntent);
        views.setOnClickPendingIntent(R.id.iv_avatar, pendingIntent);
        views.setOnClickPendingIntent(R.id.tv_title, pendingIntent);
        views.setOnClickPendingIntent(R.id.iv_ai_icon, pendingIntent);
        views.setOnClickPendingIntent(R.id.iv_ai_bg, pendingIntent);
    }

}