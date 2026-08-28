package com.fxzs.lingxiagent.util.ZUtil;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.model.chat.callback.DialogEditCallback;
import com.fxzs.lingxiagent.model.chat.callback.SoftCallback;
import com.fxzs.lingxiagent.model.chat.callback.SuperEditCallback;
import com.fxzs.lingxiagent.model.chat.dto.OptionModel;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.ChatLengthInputFilter;
import com.fxzs.lingxiagent.util.NetworkUtils;
import com.fxzs.lingxiagent.util.ShadowUtils;
import com.fxzs.lingxiagent.util.VoiceButtonSafetyUtil;
import com.fxzs.lingxiagent.util.ZInputMethod;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.auth.OneClickLoginActivity;
import com.fxzs.lingxiagent.view.common.GlobalToast;

import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;


public class SuperAgentUtil {

    Context context;
    LinearLayout root_view;
    View ll_bottom_edit;
    private LinearLayout ll_edit;
    private LinearLayout ll_bottom_voice;

    private EditText ed;
    ImageView iv_voice;
    ImageView iv_keyboard;
    ImageView iv_send;
    TextView tv_press;
    TextView tv_voice_hint;
    View rl_voice;
    View iv_logo;
    private SuperEditCallback callback;
    List<OptionModel> listModel;
    OptionModel selectOptionModel;

    boolean isVoice = false;
    boolean isRecord = false;
    private boolean isInArea = true;

    private int viewLeft;
    private int viewRight;
    private int viewTop;
    private int viewBottom;
    private boolean coordinatesCalculated = false; // 标记坐标是否已计算
    View iv_edit_open;
    View iv_edit_open_expand;
    private LinearLayout ll_expand;
    private long startTime = 0L;
    private long MIN_DURATION_MS = 1000L;
    private int[] location = new int[2];

    private long modelId = -1;
    public SuperAgentUtil(Context context, LinearLayout root_view, getCatDetailListBean bean) {
        this.context = context;
        this.root_view = root_view;
        if (bean != null) {
            this.modelId = bean.getModelId();
        }
        setUI();
//        if (bean != null && AGENT_GUI.equals(bean.getModelName())) {//GUI 默认先申请录音权限 application 无法获取权限
//            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
//                    != PackageManager.PERMISSION_GRANTED) {
//                AppPermissionRequestManager.requestAudioPermission((Activity) context, 1, AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR);
//            }
//        }
    }

    private void setUI() {
//        try {
            // 使用安全的findViewById
            ll_edit = VoiceButtonSafetyUtil.findViewByIdSafely(root_view, R.id.ll_edit, LinearLayout.class);
            ll_bottom_edit = root_view.findViewById(R.id.ll_bottom_edit);
            ll_bottom_voice = root_view.findViewById(R.id.ll_bottom_voice);
            ed = VoiceButtonSafetyUtil.findViewByIdSafely(root_view, R.id.ed, EditText.class);
            iv_voice = VoiceButtonSafetyUtil.findViewByIdSafely(root_view, R.id.iv_voice, ImageView.class);
            iv_keyboard = VoiceButtonSafetyUtil.findViewByIdSafely(root_view, R.id.iv_keyboard, ImageView.class);
            iv_send = VoiceButtonSafetyUtil.findViewByIdSafely(root_view, R.id.iv_send, ImageView.class);
            tv_press = VoiceButtonSafetyUtil.findViewByIdSafely(root_view, R.id.tv_press, TextView.class);
            tv_voice_hint = VoiceButtonSafetyUtil.findViewByIdSafely(root_view, R.id.tv_voice_hint, TextView.class);
            rl_voice = root_view.findViewById(R.id.rl_voice);
            iv_logo = root_view.findViewById(R.id.iv_logo);
            iv_edit_open = root_view.findViewById(R.id.iv_edit_open);
            iv_edit_open_expand = root_view.findViewById(R.id.iv_edit_open_expand);
            ll_expand = root_view.findViewById(R.id.ll_expand);


            // 检查必要的视图是否都找到了
            if (!VoiceButtonSafetyUtil.validateRequiredViews(ll_bottom_edit, ed, iv_voice, iv_send)) {
                Timber.tag("SuperAgentUtil").e( "Some required views are missing!");
                return;
            }
            
            // 应用阴影效果
            if (ll_bottom_edit != null) {
                ShadowUtils.applyDefaultShadow(ll_bottom_edit, context);
            }

            // 设置点击事件
            setupClickListeners();
            
            // 设置长按和触摸事件
            setupTouchListeners();
            
            // 设置文本变化监听器
            setupTextWatcher();
            
//        } catch (Exception e) {
//            Timber.tag("SuperAgentUtil").e( "Error in setUI%s", e.getMessage());
//        }
    }
    
    private void setupClickListeners() {
        // 发送按钮
        if (iv_send != null) {
            iv_send.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ZUtils.print("点击发送");
                    sendText();
                }
            });
        }
        
        // 键盘按钮
        if (iv_keyboard != null) {
            iv_keyboard.setOnClickListener(view -> {
                try {
                    switchMode(0);
                } catch (Exception e) {
                    Timber.tag("SuperAgentUtil").e( "Error switching to keyboard mode%s", e.getMessage());
                }
            });
        }
        
        // 语音按钮
        if (iv_voice != null) {
            iv_voice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 使用安全工具类处理语音按钮点击
                    boolean success = VoiceButtonSafetyUtil.handleVoiceButtonClick(
                        context,
                        () -> {
                            // 获取视图在屏幕中的位置（参考SuperEditUtil的实现）
                            if (ll_bottom_edit != null) {
                                ll_bottom_edit.getLocationOnScreen(location);
                                viewLeft = location[0];
                                viewTop = location[1];
                                viewRight = viewLeft + ll_bottom_edit.getWidth();
                                viewBottom = viewTop + ll_bottom_edit.getHeight();
                                coordinatesCalculated = true; // 标记坐标已计算
                            }
                        },
                        () -> {
                            // 模式切换
                            switchMode(1);
                        }
                    );
                    
                    if (!success) {
                        Timber.tag("SuperAgentUtil").w( "Voice button click handling failed");
                    }
                }
            });
        }
        iv_edit_open.setOnClickListener(view -> {
            //TODO 点击弹窗
            DialogUtils.showInputDialog(context, ed.getEditableText().toString(), new DialogEditCallback() {
                @Override
                public void callback(String result) {
                    sendText(result);
                }

                @Override
                public void onCancel(String result) {
                    DialogEditCallback.super.onCancel(result);
                    ed.setText(result);
                }
            });
        });
    }
    
    private void setupTouchListeners() {
        // 设置长按事件
        if (ll_bottom_edit != null) {
            ll_bottom_edit.setOnLongClickListener(view -> {
                Timber.tag("TouchEvent").d( "onLongClick ====== >");
                return isVoice;
            });
            
            // 设置触摸事件
            ll_bottom_edit.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (!isVoice || callback == null) {
                        return false;
                    }
                    
                    try {
                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                Timber.tag("TouchEvent").d( "手指按下 TextView");
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    AppPermissionRequestManager.requestAudioPermission((Activity) context, 1, AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR);
                                    return false;
                                }
                                isInArea = true;
                                
                                boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(context);
                                if(!isNetworkAvailable){
                                    isInArea = false;
                                    GlobalToast.show((Activity) context,"网络错误，请检查网络连接", GlobalToast.Type.ERROR);
                                    return false;
                                }
                                callback.pressDown();
                                startTime = SystemClock.elapsedRealtime();
                                break;

                            case MotionEvent.ACTION_UP:
                                ll_bottom_voice.setVisibility(View.GONE);
                                ll_bottom_edit.setVisibility(View.VISIBLE);
                                callback.pressUp(isInArea);
                                long duration = SystemClock.elapsedRealtime() - startTime;
                                if (duration < MIN_DURATION_MS && isInArea) {
                                    GlobalToast.show((Activity) context, context.getString(R.string.record_toast), GlobalToast.Type.ERROR);
                                }
                                break;

                            case MotionEvent.ACTION_MOVE:
                                // 获取屏幕坐标
                                float rawX = motionEvent.getRawX();
                                float rawY = motionEvent.getRawY();

                                // 获取视图在屏幕中的位置
                                ll_bottom_edit.getLocationOnScreen(location);
                                int viewLeft = location[0];
                                int viewTop = location[1];
                                int viewRight = viewLeft + ll_bottom_edit.getWidth();
                                int viewBottom = viewTop + ll_bottom_edit.getHeight();

                                // 判断是否在视图范围内
                                if (rawX < viewLeft || rawX > viewRight || rawY < viewTop || rawY > viewBottom) {
                                    isInArea = false;
                                } else {
                                    isInArea = true;
                                }
                                callback.voiceMove(isInArea);
                                break;

                            case MotionEvent.ACTION_CANCEL:
                                Timber.tag("TouchEvent").d( "触摸取消");
                                isInArea = false;
                                VoiceButtonSafetyUtil.setViewVisibilitySafely(ll_bottom_voice, View.GONE);
                                VoiceButtonSafetyUtil.setViewVisibilitySafely(ll_bottom_edit, View.VISIBLE);
                                break;
                        }
                    } catch (Exception e) {
                        Timber.tag("SuperAgentUtil").e( "Error in touch event%s", e.getMessage());
                    }
                    
                    return false;
                }
            });
        }
    }
    
    private void setupTextWatcher() {
        if (ed != null) {
            if (modelId == 156){//深度研究
                ed.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.DIALOG_INPUT_NUMBER),new ChatLengthInputFilter((Activity) context, Constants.DIALOG_INPUT_NUMBER)});
            }

            ed.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // 文本改变前
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // 文本改变中
                }

                @Override
                public void afterTextChanged(Editable s) {

                    String input = s.toString();
                    Timber.tag("SuperAgentUtil").e( "Error in text watcher%s", input);


                    try {
                        if (!input.isEmpty()) {
                            Timber.tag("SuperAgentUtil").e( "Error in text watcher%s", input);
                            VoiceButtonSafetyUtil.setViewVisibilitySafely(iv_voice, View.GONE);
                            VoiceButtonSafetyUtil.setViewVisibilitySafely(iv_send, View.VISIBLE);
                        } else {
                            VoiceButtonSafetyUtil.setViewVisibilitySafely(iv_voice, View.VISIBLE);
                            VoiceButtonSafetyUtil.setViewVisibilitySafely(iv_send, View.GONE);
                        }
                    } catch (Exception e) {
                        Timber.tag("SuperAgentUtil").e( "Error in text watcher%s", e.getMessage());
                    }

                    int lineCount = ed.getLineCount();
                    if (lineCount > 1) {
                        iv_edit_open_expand.setVisibility(View.VISIBLE);
                        setLayoutGravity(ll_expand, Gravity.BOTTOM);
                        if (lineCount > 2){
                            iv_edit_open.setVisibility(View.VISIBLE);
                        }else {
                            iv_edit_open.setVisibility(View.GONE);
                        }
                    } else {
                        iv_edit_open_expand.setVisibility(View.GONE);
                        iv_edit_open.setVisibility(View.GONE);
                        setLayoutGravity(ll_expand, Gravity.CENTER);
                    }
                    if (modelId == 156 && input.length() >= Constants.DIALOG_INPUT_NUMBER) {
                        GlobalToast.show((Activity) context, context.getString(R.string.dialog_input_content_hint), GlobalToast.Type.ERROR);
                    }

                }
            });
        }


        if (ed != null) {
            ed.setOnKeyListener((v, keyCode, event) -> {

                if (event.getAction() == KeyEvent.ACTION_DOWN) {

                    if (keyCode == KeyEvent.KEYCODE_ENTER
                            || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {

                        sendText();
                        return true;
                    }

                }

                return false;
            });
        }
    }

    private void setLayoutGravity(View view, int gravity) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.gravity = gravity;
        view.setLayoutParams(params);
    }
    private void sendText() {

        String content = ed.getText().toString();
        sendText(content);
    }
    private void sendText(String content) {
        if (callback != null) {
            // 检查内容是否为空或只包含空格、换行符等空白字符
            if (content.isEmpty() || content.trim().isEmpty()) {
                ZUtils.showToast("请输入有效内容");
                return;
            }
            ed.setText("");
            callback.send(content,selectOptionModel);
        }
    }

    public void voiceSendText(String content){
        if (callback == null){
            return;
        }
        callback.send(content,selectOptionModel);
    }

    public  void setOnListenSoft(View root_view, SoftCallback callback){


        root_view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect rect = new Rect();
                root_view.getWindowVisibleDisplayFrame(rect);
                int screenHeight = root_view.getRootView().getHeight();

                int keyHeight = screenHeight - rect.bottom;
                if(keyHeight < screenHeight*0.15){
                    Timber.tag("HomeFragment").e( "隐藏了.");
                    callback.hide();
                } else {
                    Timber.tag("HomeFragment").e( "弹出.");
                    NestedScrollView svChatList = root_view.findViewById(R.id.sv_chat_list);
                    if(svChatList != null){
                        svChatList.smoothScrollTo(0, svChatList.getChildAt(0).getBottom());
                    }
                    callback.show();

                }
            }
        });
    }

    public void setCallback(SuperEditCallback callback) {
        // 设置回调接口
        this.callback = callback;
    }

    public void getModel(){
        HttpRequest request = new HttpRequest();
        request.getModelTypeList(new Observer<ApiResponse<List<OptionModel>>>(){

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<List<OptionModel>> res) {
                if (res.getCode() == 0){
                    List<OptionModel> list = res.getData();
                    listModel = list;
//                    SpUtils.saveDataList(context,listModel);
                    Timber.tag("TAG").d( "初始化 Model: " + selectOptionModel);
                    if (selectOptionModel == null) {
                        selectOptionModel = listModel.get(0); // 默认选中第一个模型
                        Timber.tag("TAG").d( "初始化 Model: " + selectOptionModel.getName() + ", ID: " + selectOptionModel.getId());
//                        tv_mode.setText(selectOptionModel.getName());
                    }
                    if (list != null && list.size() > 0) {
                        for (OptionModel model : list) {
                            Timber.tag("TAG").d( "Model: " + model.getName() + ", ID: " + model.getId());
                        }
                    }
                }

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }


    public void setSelectOptionModel(OptionModel option) {
        this.selectOptionModel = option;
    }

    public void switchMode(int mode) {
        try {
            //mode:0-文字输入，1-语音模式
            if(mode == 0){
                isVoice = false;

                if (callback != null) {
                    callback.keyboard();
                }

                VoiceButtonSafetyUtil.setViewVisibilitySafely(iv_keyboard, View.GONE);
                VoiceButtonSafetyUtil.setViewVisibilitySafely(tv_press, View.GONE);
                VoiceButtonSafetyUtil.setViewVisibilitySafely(iv_logo, View.GONE);
                VoiceButtonSafetyUtil.setViewVisibilitySafely(iv_voice, View.VISIBLE);
                VoiceButtonSafetyUtil.setViewVisibilitySafely(ll_edit, View.VISIBLE);
                
                // 重置坐标计算标志位
                coordinatesCalculated = false;
                
                if (ed != null) {
                    ZInputMethod.openInputMethod(ed);
                }
            } else if(mode == 1){
                if ( !AuthHelper.getInstance().isLogin()) {
                    // 未登录，跳转到一键登录页面
                    Intent intent = new Intent(context, OneClickLoginActivity.class);
                    context.startActivity(intent);
                    return;
                }
                
                isVoice = true;
                if (callback != null) {
                    callback.voice();
                }
                
                VoiceButtonSafetyUtil.setViewVisibilitySafely(iv_keyboard, View.VISIBLE);
                VoiceButtonSafetyUtil.setViewVisibilitySafely(tv_press, View.VISIBLE);
                VoiceButtonSafetyUtil.setViewVisibilitySafely(iv_logo, View.VISIBLE);
                VoiceButtonSafetyUtil.setViewVisibilitySafely(iv_voice, View.GONE);
                VoiceButtonSafetyUtil.setViewVisibilitySafely(ll_edit, View.GONE);
                
                // 重置坐标计算标志位，下次点击语音按钮时会重新计算
                coordinatesCalculated = false;
                
                if (root_view != null) {
                    ZInputMethod.hideKeyboard(context, root_view.getWindowToken());
                }
            }
        } catch (Exception e) {
            Timber.tag("SuperAgentUtil").e( "Error in switchMode%s", e.getMessage());
            VoiceButtonSafetyUtil.showGenericError(context);
        }
    }
}
