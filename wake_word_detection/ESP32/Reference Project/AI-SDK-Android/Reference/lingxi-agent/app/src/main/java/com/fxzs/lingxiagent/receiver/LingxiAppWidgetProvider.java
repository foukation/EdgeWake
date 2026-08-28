package com.fxzs.lingxiagent.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.EventConstants;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class LingxiAppWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "LingxiWidget";
    private static final String URI_SCHEME = "lingxiagent://";
    private static final String PARAM_TARGET = "target";
    private static final String PARAM_SALT = "_salt";

    private static final String ACTION_WIDGET_CLICK = "com.fxzs.lingxiagent.ACTION_WIDGET_CLICK";
    private static final String EXTRA_POINT_ID = "extra_point_id";
    private static final String EXTRA_TARGET = "extra_target";
    private static final String EXTRA_SALT = "extra_salt";
    private static final String EXTRA_WIDGET_ID = "extra_widget_id"; // 🔥 新增区分多实例

    private static final int RC_INPUT = 1001;
    private static final int RC_MEETING = 1002;
    private static final int RC_PPT = 1003;
    private static final int RC_TRANSLATE = 1004;
    private static final int RC_DEEP = 1005;

    private static final Gson GSON = new Gson();

    // ============================
    // 接收点击广播
    // ============================
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_WIDGET_CLICK.equals(intent.getAction())) {
            String pointId = intent.getStringExtra(EXTRA_POINT_ID);
            String target = intent.getStringExtra(EXTRA_TARGET);
            long salt = intent.getLongExtra(EXTRA_SALT, 0);
            String pkg = context.getPackageName();

            if (!TextUtils.isEmpty(pointId)) {
                TrackerUtils.trackCommonEvent(pointId);
                Log.d(TAG, "✅ 点击打点成功: " + pointId);
            }

            performJump(context, target, salt, pkg);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) return;
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId); // 🔥 每个实例独立更新
        }
    }

    private void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.lingxi_app_widget);
        long sessionSalt = System.nanoTime();
        bindClicks(context, views, sessionSalt, widgetId); // 🔥 传入 widgetId
        manager.updateAppWidget(widgetId, views);
    }

    /**
     * 绑定点击（支持多实例）
     */
    private void bindClicks(Context context, RemoteViews views, long sessionSalt, int widgetId) {
        String pkg = context.getPackageName();

        bindWidgetClick(context, views, R.id.left_block, RC_INPUT,
                "main", EventConstants.WidgetManagement.AI_OFFICE_ASK_LINGXI, sessionSalt, widgetId);

        bindWidgetClick(context, views, R.id.item_meeting, RC_MEETING,
                "meeting", EventConstants.WidgetManagement.AI_OFFICE_MEETING, sessionSalt, widgetId);

        bindWidgetClick(context, views, R.id.item_ppt, RC_PPT,
                "ppt", EventConstants.WidgetManagement.AI_OFFICE_PPT, sessionSalt, widgetId);

        bindWidgetClick(context, views, R.id.item_translate, RC_TRANSLATE,
                "translate", EventConstants.WidgetManagement.AI_OFFICE_TRANSLATE, sessionSalt, widgetId);

        bindWidgetClick(context, views, R.id.item_deep, RC_DEEP,
                "deepresearch", EventConstants.WidgetManagement.AI_OFFICE_DEEP_RESEARCH, sessionSalt, widgetId);
    }

    /**
     * 🔥 核心修复：每个小组件实例拥有独立的 PendingIntent
     */
    private void bindWidgetClick(
            Context context, RemoteViews views, int viewId, int requestCode,
            String target, String pointId, long salt, int widgetId
    ) {
        Intent intent = new Intent(ACTION_WIDGET_CLICK);
        intent.setComponent(new ComponentName(context, LingxiAppWidgetProvider.class));
        intent.putExtra(EXTRA_POINT_ID, pointId);
        intent.putExtra(EXTRA_TARGET, target);
        intent.putExtra(EXTRA_SALT, salt);
        intent.putExtra(EXTRA_WIDGET_ID, widgetId); // 🔥 区分实例

        // 🔥 唯一请求码：widgetId + baseCode，永远不重复
        int uniqueCode = widgetId * 10000 + requestCode;

        PendingIntent pi = PendingIntent.getBroadcast(
                context, uniqueCode, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(viewId, pi);
    }

    /**
     * 跳转逻辑（完全不变）
     */
    private void performJump(Context context, String target, long sessionSalt, String packageName) {
        Uri uri = new Uri.Builder()
                .scheme(URI_SCHEME.replace("://", ""))
                .authority(packageName)
                .appendQueryParameter(PARAM_TARGET, target)
                .appendQueryParameter(PARAM_SALT, String.valueOf(sessionSalt))
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    // ========== 以下逻辑完全不变 ==========
    private List<getCatDetailListBean> getLocalBeans(Context context) {
        String json = SharedPreferencesUtil.getBeanList(context);
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return GSON.fromJson(json, new TypeToken<List<getCatDetailListBean>>() {}.getType());
        } catch (Exception e) {
            Log.e(TAG, "解析本地缓存失败", e);
            return null;
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "桌面小部件已禁用");
    }

    public static void refreshAllWidgets(Context context) {
        if (context == null) return;
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, LingxiAppWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(provider);
        if (widgetIds == null || widgetIds.length == 0) return;
        LingxiAppWidgetProvider providerInstance = new LingxiAppWidgetProvider();
        long sessionSalt = System.nanoTime();
        for (int id : widgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.lingxi_app_widget);
            providerInstance.bindClicks(context, views, sessionSalt, id); // 🔥 修复刷新
            manager.updateAppWidget(id, views);
        }
        Log.d(TAG, "手动刷新全部小部件成功");
    }
}