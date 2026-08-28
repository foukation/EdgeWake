package com.fxzs.lingxiagent.receiver;

import android.app.AlarmManager;
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
import com.fxzs.lingxiagent.lingxi.main.utils.GsonUtils;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.EventConstants;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.network.ZNet.bean.GUIWidgetBean;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LingxiGUIWidgetProvider extends AppWidgetProvider {

    // 静态常量
    public static final String ACTION_REFRESH_WIDGET = "com.fxzs.lingxiagent.action.REFRESH_WIDGET";
    public static final String ACTION_BANNER_UPDATE = "com.fxzs.lingxiagent.action.BANNER_UPDATE";
    public static final String ACTION_WIDGET_BTN_CLICK = "com.fxzs.lingxiagent.action.WIDGET_BTN_CLICK";
    private static final String TAG = "LingxiGUIWidget";

    private static final int BANNER_INTERVAL = 4000;
    private static final String URI_SCHEME = "lingxiagent";
    private static final String PARAM_TARGET = "target";
    private static final String PARAM_CONTENT = "itemContent";
    private static final String PARAM_TIME = "t";

    // 点击额外参数
    private static final String EXTRA_POINT_ID = "extra_point_id";
    private static final String EXTRA_EXPECT_KEY = "extra_expect_key";
    private static final String EXTRA_VIEW_ID = "extra_view_id";
    // 新增：组件ID参数（区分多实例）
    private static final String EXTRA_WIDGET_ID = "extra_widget_id";

    private static final Gson GSON = new Gson();

    // 按钮映射
    private static final Map<Integer, String> BTN_ACTION_MAP;
    static {
        BTN_ACTION_MAP = new HashMap<>();
        BTN_ACTION_MAP.put(R.id.id_rl_publish, "MAIN_BTN");
        BTN_ACTION_MAP.put(R.id.id_tv_kds, "watch_tv");
        BTN_ACTION_MAP.put(R.id.id_tv_dnbg, "office");
        BTN_ACTION_MAP.put(R.id.id_tv_cy, "takeaway_restaurant");
        BTN_ACTION_MAP.put(R.id.id_tv_gw, "shopping");
    }

    // ============================
    // 🔥 核心：点击在这里接收 + 打点
    // ============================
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            super.onReceive(context, intent);
            String action = intent.getAction();
            if (action == null) return;

            if (ACTION_REFRESH_WIDGET.equals(action)) {
                refreshAllWidgets(context);
            } else if (ACTION_BANNER_UPDATE.equals(action)) {
                handleBannerUpdate(context);
            } else if (ACTION_WIDGET_BTN_CLICK.equals(action)) {
                // 🔥 按钮点击 → 在这里打点！！！
                String pointId = intent.getStringExtra(EXTRA_POINT_ID);
                String expectKey = intent.getStringExtra(EXTRA_EXPECT_KEY);
                int viewId = intent.getIntExtra(EXTRA_VIEW_ID, 0);

                // 上报打点
                if (!TextUtils.isEmpty(pointId)) {
                    TrackerUtils.trackCommonEvent(pointId);
                    Log.d(TAG, "✅ 点击打点成功: " + pointId);
                }

                // 执行原跳转逻辑
                performClickJump(context, viewId, expectKey);
            }
        } catch (Exception e) {
            Log.e(TAG, "小部件广播异常", e);
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "小部件已禁用，停止轮询");
        stopBannerAlarm(context);
    }

    // ==============================================
    // 🔥 核心：一创建就显示文字，绝不空白 + 多实例兼容
    // ==============================================
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) return;
        boolean isLogin = AuthHelper.getInstance().isLogin();

        // 遍历每个组件实例，独立绑定
        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.lingxi_gui_widget);
            String bannerText = getFinalBannerText(context);
            views.setTextViewText(R.id.id_tv_banner_text, bannerText);
            if (isLogin) {
                loadWidgetData(context, views);
            }

            // 🔥 修复：传入 widgetId，每个实例独立绑定点击
            bindAllButtons(context, views, widgetId);
            bindBannerClick(context, views, widgetId);
            manager.updateAppWidget(widgetId, views);
        }
    }

    // ==============================================
    // 最终安全获取文案
    // ==============================================
    private String getFinalBannerText(Context context) {
        List<GUIWidgetBean> list = getSafeWidgetList(context);
        if (list == null || list.isEmpty()) {
            return "\"欢迎使用灵犀智能助手\"";
        }

        int index = SharedPreferencesUtil.getWidgetIndex();
        if (index < 0 || index >= list.size()) {
            index = 0;
            SharedPreferencesUtil.saveWidgetIndex(0);
        }

        GUIWidgetBean bean = list.get(index);
        if (bean == null || TextUtils.isEmpty(bean.getDisplayText())) {
            return "\"灵犀AI为您服务\"";
        }

        return "\"" + bean.getDisplayText() + "\"";
    }

    // ==============================================
    // 安全获取列表
    // ==============================================
    private List<GUIWidgetBean> getSafeWidgetList(Context context) {
        String cacheJson = SharedPreferencesUtil.getWidgetIData();
        if (TextUtils.isEmpty(cacheJson)) {
            cacheJson = WidgetDataHelper.DEFAULT_WIDGET_JSON;
        }
        try {
            return GSON.fromJson(cacheJson, new TypeToken<List<GUIWidgetBean>>() {}.getType());
        } catch (Exception e) {
            Log.e(TAG, "解析数据失败", e);
            return null;
        }
    }

    // ==============================================
    // Banner 轮询逻辑
    // ==============================================
    private void handleBannerUpdate(Context context) {
        List<GUIWidgetBean> list = getSafeWidgetList(context);
        if (list == null || list.isEmpty()) return;

        int oldIndex = SharedPreferencesUtil.getWidgetIndex();
        int newIndex = oldIndex + 1;
        if (newIndex >= list.size()) newIndex = 0;

        SharedPreferencesUtil.saveWidgetIndex(newIndex);
        updateBannerUi(context, "\"" + list.get(newIndex).getDisplayText() + "\"");
    }

    // ==============================================
    // 网络加载
    // ==============================================
    private void loadWidgetData(Context context, RemoteViews views) {
        WidgetDataHelper.loadAndBindWidgetData(new WidgetDataHelper.DataCallback() {
            @Override
            public void onSuccess(List<GUIWidgetBean> menuBeans) {
                if (menuBeans == null || menuBeans.isEmpty()) return;
                startBannerAlarm(context);
                handleBannerUpdate(context);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "Banner 数据加载失败", e);
                startBannerAlarm(context);
            }
        });
    }

    // ==============================================
    // 🔥 修复：多实例绑定点击，每个组件独立ID
    // ==============================================
    private void bindAllButtons(Context context, RemoteViews views, int widgetId) {
        for (Map.Entry<Integer, String> entry : BTN_ACTION_MAP.entrySet()) {
            int viewId = entry.getKey();
            String expectKey = entry.getValue();
            String pointId = null;

            // 匹配打点ID
            switch (expectKey) {
                case "MAIN_BTN":
                    pointId = EventConstants.WidgetManagement.AUTO_EXEC_PUBLISH_TASK;
                    break;
                case "watch_tv":
                    pointId = EventConstants.WidgetManagement.AUTO_EXEC_WATCH_TV;
                    break;
                case "office":
                    pointId = EventConstants.WidgetManagement.AUTO_EXEC_PC_OFFICE;
                    break;
                case "takeaway_restaurant":
                    pointId = EventConstants.WidgetManagement.AUTO_EXEC_FOOD_DELIVERY;
                    break;
                case "shopping":
                    pointId = EventConstants.WidgetManagement.AUTO_EXEC_SHOPPING_COMPARE;
                    break;
            }

            // 🔥 核心修复：每个组件实例唯一标识
            Intent intent = new Intent(ACTION_WIDGET_BTN_CLICK);
            intent.setComponent(new ComponentName(context, LingxiGUIWidgetProvider.class));
            intent.putExtra(EXTRA_VIEW_ID, viewId);
            intent.putExtra(EXTRA_EXPECT_KEY, expectKey);
            intent.putExtra(EXTRA_POINT_ID, pointId);
            intent.putExtra(EXTRA_WIDGET_ID, widgetId);

            // 🔥 唯一请求码：widgetId + viewId，永不重复
            int requestCode = widgetId * 1000 + viewId;
            PendingIntent pi = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(viewId, pi);
        }
    }

    // ==============================================
    // 🔥 点击后真正执行跳转（完全保留原有逻辑）
    // ==============================================
    private void performClickJump(Context context, int viewId, String expectKey) {
        String packageName = context.getPackageName();
        List<GUIWidgetBean> allItems = getSafeWidgetList(context);
        int currentIndex = SharedPreferencesUtil.getWidgetIndex();

        // 云电脑逻辑（完全不变）
        if (expectKey.equals("office")) {
            try {
                Intent cloudIntent = context.getPackageManager().getLaunchIntentForPackage("cm.komect.aqb.android.cloudcomputerpad");
                if (cloudIntent != null) {
                    cloudIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(cloudIntent);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 原有跳转逻辑（完全不变）
        String actionJson = findActionJson(allItems, currentIndex, viewId, expectKey);
        Uri uri = new Uri.Builder()
                .scheme(URI_SCHEME)
                .authority(packageName)
                .appendQueryParameter(PARAM_TARGET, "guiagent")
                .appendQueryParameter(PARAM_CONTENT, actionJson)
                .appendQueryParameter(PARAM_TIME, String.valueOf(System.currentTimeMillis()))
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    private String findActionJson(List<GUIWidgetBean> items, int index, int viewId, String key) {
        if (items == null || items.isEmpty()) return "";

        if (viewId == R.id.id_rl_publish) {
            if (index < items.size()) {
                return GsonUtils.toJson(items.get(index).getActionCommands());
            }
        } else {
            for (GUIWidgetBean item : items) {
                if (key.equals(item.getCategoryKey())) {
                    return GsonUtils.toJson(item.getActionCommands());
                }
            }
        }
        return "";
    }

    // ==============================================
    // 🔥 修复：Banner 点击多实例兼容
    // ==============================================
    private void bindBannerClick(Context context, RemoteViews views, int widgetId) {
        Intent intent = new Intent(context, LingxiGUIWidgetProvider.class);
        intent.setAction(ACTION_BANNER_UPDATE);
        intent.putExtra(EXTRA_WIDGET_ID, widgetId);

        // 唯一请求码
        PendingIntent pi = PendingIntent.getBroadcast(
                context, 7777 + widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.id_tv_banner_text, pi);
    }

    // ==============================================
    // 轮询开关
    // ==============================================
    private void startBannerAlarm(Context context) {
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        if (alarmManager == null) return;
//
//        Intent intent = new Intent(context, LingxiGUIWidgetProvider.class);
//        intent.setAction(ACTION_BANNER_UPDATE);
//        PendingIntent pi = PendingIntent.getBroadcast(context, 8888, intent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//        alarmManager.cancel(pi);
//        alarmManager.setRepeating(AlarmManager.RTC,
//                System.currentTimeMillis() + BANNER_INTERVAL, BANNER_INTERVAL, pi);
    }

    private void stopBannerAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, LingxiGUIWidgetProvider.class);
        intent.setAction(ACTION_BANNER_UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 8888, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pi);
    }

    // ==============================================
    // 刷新工具
    // ==============================================
    private void refreshAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, LingxiGUIWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(cn);
        if (ids != null && ids.length > 0) {
            onUpdate(context, manager, ids);
        }
    }

    private void updateBannerUi(Context context, String text) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, LingxiGUIWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(cn);

        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.lingxi_gui_widget);
            views.setTextViewText(R.id.id_tv_banner_text, text);
            bindAllButtons(context, views, id);
            bindBannerClick(context, views, id);
            manager.updateAppWidget(id, views);
        }
    }
}