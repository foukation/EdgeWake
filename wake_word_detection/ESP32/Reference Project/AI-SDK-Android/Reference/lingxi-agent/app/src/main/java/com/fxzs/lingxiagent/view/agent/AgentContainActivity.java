package com.fxzs.lingxiagent.view.agent;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fxzs.lingxiagent.JumpParameterManager;
import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.model.chat.dto.EventBusShareCancelModel;
import com.fxzs.lingxiagent.model.chat.dto.EventBusShareNotifyModel;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;
import com.fxzs.lingxiagent.util.GMapHelper;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.view.auth.OneClickLoginActivity;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.chat.SuperChatFragment;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.chat.VMChat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import timber.log.Timber;

/**
 * 智能体界面
 * 修改说明：
 * 1. 新增 onNewIntent 以支持 singleTask 模式下的参数刷新。
 * 2. init() 内部增加防御性逻辑，防止 Fragment 重复添加。
 */
public class AgentContainActivity extends BaseActivity {
    public static final int TYPE_HOME = 1;//首页
    public static final int TYPE_AGENT = 2;//智能体
    public static final int TYPE_DRAWING = 3;//绘画-对话界面
    private int type = TYPE_AGENT;//跳转类型
    SuperChatFragment superChatFragment;
    private View back;

    @Override
    protected int getLayoutResource() {
        return R.layout.act_super_chat_container;
    }

    @Override
    protected Class getViewModelClass() {
        return VMChat.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    public void nestedScrollBottom() {
        // 增加空指针保护，防止崩溃
        if (superChatFragment != null && superChatFragment.getView() != null) {
            NestedScrollView svChatList = superChatFragment.getView().findViewById(R.id.sv_chat_list);
            if (svChatList != null && svChatList.getChildAt(0) != null) {
                svChatList.smoothScrollTo(0, svChatList.getChildAt(0).getBottom());
            }
        }
    }

    @Override
    protected void initializeViews() {
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 只有首次创建且没有保存状态时才初始化
        if (savedInstanceState == null) {
            init();
        }
    }

    /**
     * 【新增核心方法】
     * 当 Activity 配置为 singleTask 且实例已存在时，系统会回调此方法。
     * 这里必须更新 Intent 并重新调用 init()。
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Timber.d("AgentContain", "onNewIntent called - 收到新参数，准备刷新");

        // 【关键步骤 1】更新当前 Activity 持有的 Intent
        // 如果不执行这步，init() 里调用的 getIntent() 拿到的还是旧数据
        setIntent(intent);

        // 【关键步骤 2】复用原有的 init 逻辑
        // 注意：因为 init 里会再次 add Fragment，所以需要在 init 内部先移除旧的
        init();
    }

    public void init() {
        // 【关键修改】防御性检查：如果 Fragment 已存在，先移除它
        // 这是为了兼容 onNewIntent 再次调用 init() 的场景，防止界面重叠
        if (superChatFragment != null) {
            Timber.d("AgentContain"+"检测到旧 Fragment，执行移除操作");
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(superChatFragment);
            ft.commitNow();
            superChatFragment = null; // 重置引用
        }

        // --- 以下为原逻辑，保持不变 ---

        back = findViewById(R.id.back);
        // 增加空指针保护，防止 findViewById 失败
        if (back != null) {
            back.setOnClickListener(view -> {
                // 增加安全检查
                if (superChatFragment == null || superChatFragment.getVMChat() == null) {
                    backToMain();
                    return;
                }

                long conversationId = getIntent().getLongExtra(Constant.INTENT_ID, 0);
                List<ChatMessage> chatMessages = superChatFragment.getVMChat().getChatMessages().getValue();

                if (chatMessages != null && !chatMessages.isEmpty() && conversationId != 0) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("result_conversationId", conversationId);
                    resultIntent.putExtra("type", type);
                    resultIntent.putExtra("result_title", extractConversationTitle(chatMessages));
                    resultIntent.putExtra("result_last_message", extractLastMessage(chatMessages));
                    setResult(1, resultIntent);
                }
                backToMain();
            });
        }

        if (getIntent() != null) {
            // 智能体
            // 增加类型安全检查，防止 ClassCastException
            Object obj = getIntent().getSerializableExtra(Constant.INTENT_DATA2);
            getCatDetailListBean bean = null;
            if (obj instanceof getCatDetailListBean) {
                bean = (getCatDetailListBean) obj;
            } else {
                Timber.e("AgentContain"+"Data2 is not getCatDetailListBean: " + (obj != null ? obj.getClass() : "null"));
            }

            String query = getIntent().getStringExtra(Constant.INTENT_DATA_GUI_QUERY);
            long id = getIntent().getLongExtra(Constant.INTENT_ID, 0);

            // 同步更新 type (如果 Intent 中传递了新的 type)
            type = getIntent().getIntExtra(Constant.INTENT_TYPE, TYPE_AGENT);

            superChatFragment = new SuperChatFragment(type, id, bean, query);

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.fragment_container, superChatFragment);
            // 使用 commitNow() 同步提交，避免异步渲染导致的闪烁
            transaction.commitNow();
        }
    }
    private void backToMain() {
        if (JumpParameterManager.INSTANCE.isMainActivityInStack(this)) {
            // 存在 → 直接 finish，系统自动返回动画
            finish();
        } else {
            // 不存在 → 跳 Main，用系统返回动画
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            finish();
        }
    }

    @Override
    protected void setupObservers() {
        // 监听 ViewModel 的 LiveData
    }

    private boolean isShareEditor = false;

    @Subscribe
    public void echoShareEditState(EventBusShareNotifyModel shareNotifyModel) {
        isShareEditor = shareNotifyModel.isShow();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isShareEditor) {
            EventBus.getDefault().post(new EventBusShareCancelModel(true, AgentContainActivity.class.getSimpleName()));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Timber.tag("SuperChatContainActivity").d("onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (superChatFragment != null) {
            superChatFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 添加屏幕常亮标记，保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    /**
     * 处理权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MainActivity.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.tag("GMapHelper").i("定位权限申请成功");
                GMapHelper.getInstance().initLocation(this);
                GMapHelper.getInstance().getLocation();
            }
        }
    }

    private String extractConversationTitle(List<ChatMessage> chatMessages) {
        if (chatMessages == null || chatMessages.isEmpty()) {
            return null;
        }

        for (ChatMessage message : chatMessages) {
            if (message == null) {
                continue;
            }
            if (message.getMsgType() == ChatAdapter.TYPE_USER_HEAD_AGENT || message.getMsgType() == ChatAdapter.TYPE_USER) {
                String text = safeTrim(message.getMessage());
                if (!TextUtils.isEmpty(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private String extractLastMessage(List<ChatMessage> chatMessages) {
        if (chatMessages == null || chatMessages.isEmpty()) {
            return null;
        }

        for (int i = chatMessages.size() - 1; i >= 0; i--) {
            ChatMessage message = chatMessages.get(i);
            if (message == null) {
                continue;
            }
            if (message.getMsgType() == ChatAdapter.TYPE_AI || message.getMsgType() == ChatAdapter.TYPE_USER) {
                String text = safeTrim(message.getMessage());
                if (!TextUtils.isEmpty(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Timber.tag("SuperChatContainActivity").d("activity = onConfigurationChanged " );
    }

    public void onReceiveFloatContent(String formFloatContent) {
        // 处理传过来的数据
        runOnUiThread(() -> {
            // 更新 UI 或执行业务逻辑
            if (!TextUtils.isEmpty(formFloatContent) ) {
              if (superChatFragment != null){
                  superChatFragment.onAgentFloatContent(formFloatContent);
              }
            }
        });
    }


}