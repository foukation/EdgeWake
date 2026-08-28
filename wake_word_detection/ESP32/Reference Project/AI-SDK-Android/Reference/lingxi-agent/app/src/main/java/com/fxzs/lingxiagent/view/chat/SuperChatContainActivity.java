package com.fxzs.lingxiagent.view.chat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.model.chat.dto.DrawingToChatBean;
import com.fxzs.lingxiagent.model.chat.dto.EventBusShareCancelModel;
import com.fxzs.lingxiagent.model.chat.dto.EventBusShareNotifyModel;
import com.fxzs.lingxiagent.model.chat.dto.OptionModel;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingStyleDto;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;
import com.fxzs.lingxiagent.util.GMapHelper;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.chat.VMChat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import timber.log.Timber;


public class SuperChatContainActivity extends BaseActivity {
    public static final int TYPE_HOME = 1;//首页
    public static final int TYPE_AGENT = 2;//智能体
    public static final int TYPE_DRAWING = 3;//绘画-对话界面

    private int type;//跳转类型
    SuperChatFragment superChatFragment;
    private View back;
    private View bottom_padding;


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

    @Override
    protected void initializeViews() {
        EventBus.getDefault().register(this);
        init();
    }

    public void init() {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.act_super_chat);
        back = findViewById(R.id.back);
        bottom_padding = findViewById(R.id.bottom_padding);
        back.setOnClickListener(view -> {
            long conversationId = getIntent().getLongExtra(Constant.INTENT_ID, 0);
            List<ChatMessage> chatMessages = superChatFragment.getVMChat().getChatMessages().getValue();
            if (conversationId != 0) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("result_conversationId", conversationId);
                resultIntent.putExtra("type", type);
                resultIntent.putExtra("result_title", extractConversationTitle(chatMessages));
                resultIntent.putExtra("result_last_message", extractLastMessage(chatMessages));
                setResult(1, resultIntent);
            }
            finish();
        });


        if (getIntent() != null) {
            type = getIntent().getIntExtra(Constant.INTENT_TYPE, SuperChatContainActivity.TYPE_HOME);
            if (type == TYPE_HOME) {//首页
                back.setVisibility(View.GONE);
                long id = getIntent().getLongExtra(Constant.INTENT_ID, 0);

                String input = getIntent().getStringExtra(Constant.INTENT_DATA);
                OptionModel selectOptionModel = (OptionModel) getIntent().getSerializableExtra(Constant.INTENT_DATA1);
                if (id == 0) {
                    List<ChatFileBean> fileList = (List<ChatFileBean>) getIntent().getSerializableExtra(Constant.INTENT_DATA2);
                    superChatFragment = new SuperChatFragment(type,input,selectOptionModel,fileList);
                } else {//历史
                    back.setVisibility(View.VISIBLE);
                    bottom_padding.setVisibility(View.VISIBLE);
                    superChatFragment = new SuperChatFragment(type,input,id,selectOptionModel);
                }
            } else if (type == TYPE_AGENT) {//智能体
                bottom_padding.setVisibility(View.VISIBLE);
                getCatDetailListBean bean = (getCatDetailListBean) getIntent().getSerializableExtra(Constant.INTENT_DATA2);
                long id = getIntent().getLongExtra(Constant.INTENT_ID, 0);
                superChatFragment = new SuperChatFragment(type,id,bean);
            } else if (type == TYPE_DRAWING) {//绘画
                bottom_padding.setVisibility(View.VISIBLE);
                DrawingToChatBean bean = (DrawingToChatBean) getIntent().getSerializableExtra(Constant.INTENT_DATA);
                DrawingStyleDto styleDto = (DrawingStyleDto) getIntent().getSerializableExtra(Constant.INTENT_DATA1);
                superChatFragment = new SuperChatFragment(type,bean,styleDto);
            }

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.fragment_container, superChatFragment);
            transaction.commit();
        }
    }
    @Override
    protected void setupObservers() {
        // 监听 ViewModel 的 LiveData

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
    private boolean isShareEditor = false;

    @Subscribe
    public void echoShareEditState(EventBusShareNotifyModel shareNotifyModel) {
        isShareEditor = shareNotifyModel.isShow();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isShareEditor){
            EventBus.getDefault().post(new EventBusShareCancelModel(true, SuperChatContainActivity.class.getSimpleName()));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Timber.tag("SuperChatContainActivity").d( "onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode);
        if(superChatFragment != null){
            superChatFragment.onActivityResult( requestCode,  resultCode,  data);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        // 切换界面时停止TTS播放
        try {
            TTSManager.Companion.getInstance().stop();
        } catch (Exception e) {
            Timber.tag("SuperChatContainActivity").w( "停止TTS播放失败"+e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 确保销毁时停止TTS播放
        try {
            TTSManager.Companion.getInstance().stop();
        } catch (Exception e) {
            Timber.tag("SuperChatContainActivity").w( "销毁时停止TTS播放失败"+e);
        }

        EventBus.getDefault().unregister(this);
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
}
