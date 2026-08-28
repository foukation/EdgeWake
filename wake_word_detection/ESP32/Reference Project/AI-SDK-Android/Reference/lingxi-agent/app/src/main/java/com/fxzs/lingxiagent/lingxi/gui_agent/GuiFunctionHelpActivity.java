package com.fxzs.lingxiagent.lingxi.gui_agent;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.CaseCategory;
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.CaseItem;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.network.ZNet.bean.GUIWidgetBean;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;
import com.fxzs.lingxiagent.receiver.WidgetDataHelper;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.view.agent.AgentContainActivity;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.main.VMEmpty;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GuiFunctionHelpActivity extends BaseActivity {
    private LinearLayout llContainer;

    // 保留 bean 用于传递 Intent
    getCatDetailListBean bean = new getCatDetailListBean();

    private static final String TAG = "GuiFunctionHelp";

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_guifunction;
    }

    @Override
    protected Class getViewModelClass() {
        return VMEmpty.class;
    }

    @Override
    protected void setupDataBinding() {
    }

    @Override
    protected void initializeViews() {
        ImageView closeIcon = findViewById(R.id.iv_back);
        closeIcon.setOnClickListener((view) -> finish());

        llContainer = findViewById(R.id.ll_container);

        // 初始化 bean 的基本信息 (这些可能不需要动态变化，或者也可以从配置读取)
        bean.setModelName(Constants.AGENT_GUI);
        bean.setName(Constants.AGENT_GUI);
        bean.setModelId(160);
        bean.setBotId("NexusPilot");
        bean.setPreInput("你好呀！我是自动执行助手，一句指令让我操作各类应用，点咖啡、订机票、订酒店，都能轻松帮你搞定～！");
        bean.setDescription("你好呀！我是自动执行助手，一句指令让我操作各类应用，点咖啡、订机票、订酒店，都能轻松帮你搞定～");
        bean.setRecommendQuestions("[\"帮我订一杯瑞幸的生椰拿铁\",\"帮我在携程订一张北京到广州的飞机票，后天出发，中午12点以后的第一班\",\"帮我订一个广州保利世贸展览馆附近的酒店，大床房带早餐，下个月1号入住\"]");

        // 【核心修改】从 SharedPreferences 获取真实数据并渲染
        loadAndRenderRealData();
    }

    /**
     * 从 SP 读取 JSON 数据，解析为 GUIWidgetBean 列表，转换为 CaseCategory 列表，并渲染到 UI
     */
    private void loadAndRenderRealData() {
        // 1. 获取保存的 JSON 字符串
        String cacheJson = SharedPreferencesUtil.getWidgetIData();
        // 🔥 缓存为空 → 直接使用默认兜底JSON
        if (TextUtils.isEmpty(cacheJson)) {
            cacheJson = WidgetDataHelper.DEFAULT_WIDGET_JSON;
        }

        if (cacheJson == null || cacheJson.isEmpty()) {
            Log.w(TAG, "No widget data found in SharedPreferences. Showing empty or default state.");
            // 可选：显示默认提示或加载占位图
            TextView tvTip = new TextView(this);
            tvTip.setText("暂无功能数据，请先在桌面 Widget 刷新数据");
            tvTip.setPadding(50, 50, 50, 50);
            llContainer.addView(tvTip);
            return;
        }

        try {
            // 2. 使用 Gson 解析为 List<GUIWidgetBean>
            Gson gson = new Gson();
            Type type = new TypeToken<List<GUIWidgetBean>>() {
            }.getType();
            List<GUIWidgetBean> widgetBeans = gson.fromJson(cacheJson, type);

            if (widgetBeans != null && !widgetBeans.isEmpty()) {
                // 3. 转换为 UI 需要的 CaseCategory 列表
                List<CaseCategory> categories = convertToCaseCategories(widgetBeans);

                // 4. 渲染到界面
                addCategoriesToContainer(categories);
            } else {
                Log.w(TAG, "Parsed data is empty.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse widget data JSON", e);
            // 解析失败时的处理
            TextView tvError = new TextView(this);
            tvError.setText("数据加载失败，请重试");
            llContainer.addView(tvError);
        }
    }

    /**
     * 将 GUIWidgetBean 列表转换为 CaseCategory 列表
     */
    private List<CaseCategory> convertToCaseCategories(List<GUIWidgetBean> widgets) {
        List<CaseCategory> categories = new ArrayList<>();

        for (GUIWidgetBean bean : widgets) {
            String title = bean.getCategoryTitle();
            if (title == null) title = "未命名分类";

            List<CaseItem> items = new ArrayList<>();

            // 优先使用 actionCommands
            List<String> commands = bean.getActionCommands();
            if (commands != null && !commands.isEmpty()) {
                for (String cmd : commands) {
                    if (cmd != null && !cmd.trim().isEmpty()) {
                        items.add(new CaseItem(cmd));
                    }
                }
            }
            // 兼容性处理：如果 actionCommands 为空，但数据结构中有其他潜在字段（根据你的 JSON 示例可能有嵌套 items）
            // 注意：你的 GUIWidgetBean 类目前只定义了 actionCommands。
            // 如果后端返回的 JSON 中某些项（如 price_compare_shopping）确实没有 actionCommands 而是嵌套了 items，
            // 则需要修改 GUIWidgetBean 类增加对应字段，或者在这里手动解析 JsonNode。
            // 假设当前 GUIWidgetBean 能覆盖大部分情况，若遇到空列表，可选择跳过或添加提示项。

            if (items.isEmpty()) {
                // 如果某个分类下没有命令，可以选择跳过该分类，或者添加一个默认项
                // 这里选择跳过，避免显示空分类
                continue;
            }

            categories.add(new CaseCategory(title, items));
        }

        return categories;
    }

    /**
     * 动态将分类数据填充到容器中
     *
     * @param categories 分类列表
     */
    private void addCategoriesToContainer(List<CaseCategory> categories) {
        LayoutInflater inflater = LayoutInflater.from(this);

        // 清空容器（防止重复添加，虽然 initializeViews 通常只调用一次，但为了安全）
        llContainer.removeAllViews();

        for (CaseCategory category : categories) {
            // 1. 加载并添加分类标题
            View titleView = inflater.inflate(R.layout.item_category_title, llContainer, false);
            TextView tvTitle = titleView.findViewById(R.id.tv_category_title);
            tvTitle.setText(category.getTitle());
            llContainer.addView(titleView);

            // 2. 加载并添加该分类下的所有条目
            for (CaseItem item : category.getItems()) {
                View itemView = inflater.inflate(R.layout.item_case_entry, llContainer, false);
                TextView tvContent = itemView.findViewById(R.id.tv_content);
                tvContent.setText(item.getContent());

                // 条目点击事件
                itemView.setOnClickListener(v ->
                        handleItemClick(category.getTitle(), item.getContent())
                );
                llContainer.addView(itemView);
            }
        }
    }

    /**
     * 处理条目点击事件
     */
    private void handleItemClick(String categoryTitle, String itemContent) {
        Intent intent = new Intent(this, AgentContainActivity.class);
        intent.putExtra(Constant.INTENT_TYPE, AgentContainActivity.TYPE_AGENT);
        intent.putExtra(Constant.INTENT_DATA2, bean);
        intent.putExtra(Constant.INTENT_DATA_GUI_QUERY, itemContent);

        startActivity(intent);
        // 可选：添加点击动画或日志
        finish();
        Log.d(TAG, "Clicked: [" + categoryTitle + "] " + itemContent);
    }

    @Override
    protected void setupObservers() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 可选：如果希望每次回到页面都重新读取最新数据（防止 Widget 更新后这里没变），可以取消注释下面这行
        // loadAndRenderRealData();
    }
}