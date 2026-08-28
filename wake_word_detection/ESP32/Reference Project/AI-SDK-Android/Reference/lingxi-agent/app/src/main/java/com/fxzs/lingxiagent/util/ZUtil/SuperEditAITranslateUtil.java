package com.fxzs.lingxiagent.util.ZUtil;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.model.chat.callback.AITranslateEditCallback;
import com.fxzs.lingxiagent.model.chat.dto.AiWritingTypeBean;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.KeyboardUtils;
import com.fxzs.lingxiagent.util.NetworkUtils;
import com.fxzs.lingxiagent.util.ShadowUtils;
import com.fxzs.lingxiagent.util.ZInputMethod;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.auth.OneClickLoginActivity;
import com.fxzs.lingxiagent.view.chat.OptionAiMeetingAdapter;
import com.fxzs.lingxiagent.view.common.GlobalToast;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class SuperEditAITranslateUtil {

    Context context;
    LinearLayout root_view;
    View ll_bottom_edit;
    View ll_voice_edit;


    private EditText ed;
    ImageView iv_send;
    private AITranslateEditCallback callback;
    List<AiWritingTypeBean> listModel;
    List<AiWritingTypeBean> listModel2;
    private ImageView iv_close;
    private LinearLayout ll_from;
    private LinearLayout ll_to;
    private TextView tv_from;
    private TextView tv_to;


    ImageView iv_voice;
    private LinearLayout ll_bottom_voice;
    TextView tv_voice_hint;
    View rl_voice;
    ImageView iv_keyboard;
    ImageView iv_add;
    TextView tv_press;
    View iv_logo;
    private View ll_edit;


    boolean isVoice = false;
    boolean isRecord = false;
    private boolean isInArea = true;

    AiWritingTypeBean selectOption1;
    AiWritingTypeBean selectOption2;

    String sleet1 = "中文";

    String sleet2 = "简体中文";
    private int[] location = new int[2];

    private long startTime = 0L;
    private long MIN_DURATION_MS = 1000L;

    private List<String> selectLanguageModel  = new ArrayList<>();

    public String getSleet1() {
        return sleet1;
    }

    public String getSleet2() {
        return sleet2;
    }

    public SuperEditAITranslateUtil(Context context, LinearLayout root_view) {
        this.context = context;
        this.root_view = root_view;
        setUI();
        getModel();
    }

    private void setUI() {
        ll_from = root_view.findViewById(R.id.ll_from);
        ll_to = root_view.findViewById(R.id.ll_to);
        tv_from = root_view.findViewById(R.id.tv_from);
        tv_to = root_view.findViewById(R.id.tv_to);
        ed = root_view.findViewById(R.id.ed);
        iv_send = root_view.findViewById(R.id.iv_send);
        iv_close = root_view.findViewById(R.id.iv_close);
        ll_bottom_voice = root_view.findViewById(R.id.ll_bottom_voice);
        ll_edit = root_view.findViewById(R.id.ll_edit);
        ll_bottom_edit = root_view.findViewById(R.id.ll_bottom_edit);
        ll_voice_edit = root_view.findViewById(R.id.ll_voice_edit);

        iv_keyboard = root_view.findViewById(R.id.iv_keyboard);
        iv_add = root_view.findViewById(R.id.iv_add);
        iv_send = root_view.findViewById(R.id.iv_send);
        tv_voice_hint = root_view.findViewById(R.id.tv_voice_hint);
        rl_voice = root_view.findViewById(R.id.rl_voice);
        iv_voice = root_view.findViewById(R.id.iv_voice);
        tv_press = root_view.findViewById(R.id.tv_press);
        rl_voice = root_view.findViewById(R.id.rl_voice);
        iv_logo = root_view.findViewById(R.id.iv_logo);
        ShadowUtils.applyDefaultShadow(ll_bottom_edit, context);
        ZInputMethod.openInputMethod(ed);

        ll_from.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ZUtils.showAIMeetingPopup(context, view, listModel, selectOption1, new OptionAiMeetingAdapter.OnOptionSelectedListener() {
                    @Override
                    public void onOptionSelected(AiWritingTypeBean option) {
                        selectOption1 = option;
                        tv_from.setText(option.getName());
                        sleet1 = option.getName();
                    }
                });
            }
        });

        ll_to.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ZUtils.showAIMeetingPopup(context, view, listModel2, selectOption2, new OptionAiMeetingAdapter.OnOptionSelectedListener() {
                    @Override
                    public void onOptionSelected(AiWritingTypeBean option) {
                        selectOption2 = option;
                        tv_to.setText(option.getName());
                        sleet2 = option.getName();
                    }
                });
            }
        });
        iv_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (callback != null) {
                    String content = ed.getText().toString();
                    if (content.isEmpty()) {
                        ZUtils.showToast("请输入内容");
                        return;
                    }
                    KeyboardUtils.hideSoftKeyboard((Activity) context);
                    sendMsg(content);
                }
            }
        });
        iv_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                AsrOneUtils.getInstance().removeCallBack();
                changeSoftkey(0, null);
                if (callback != null) {
                    callback.close();
                }
            }
        });

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
                // 文本改变后，s 是当前文本内容
                String input = s.toString();
                // 比如，实时显示输入内容
                Timber.tag("EditText").d( "当前输入: " + input);
                if (input.length() > 0) {
                    iv_voice.setVisibility(View.GONE);
//                    iv_add.setVisibility(View.GONE);
                    iv_send.setVisibility(View.VISIBLE);
                } else {
                    iv_voice.setVisibility(View.VISIBLE);
//                    iv_add.setVisibility(View.VISIBLE);
                    iv_send.setVisibility(View.GONE);
                }
            }
        });


        iv_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeSoftkey(1, view);

            }
        });

        iv_keyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeSoftkey(0, view);

            }
        });

        ll_voice_edit.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                Timber.tag("TouchEvent").d( "onLongClick ====== >");
                if (isVoice) {
                    return true;
                }
                return false;
            }
        });


        ll_voice_edit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (!isVoice) {
                    return false;
                }
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 手指按下
                        Timber.tag("TouchEvent").d( "手指按下 TextView");
//                        tv_press.setBackgroundColor(Color.LTGRAY); // 示例：改变背景色
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {
                            AppPermissionRequestManager.requestAudioPermission((Activity) context, 1,AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR);
                            return false;
                        }
                        if (!selectLanguageModel.contains(sleet1)){
                            GlobalToast.show((AppCompatActivity)context, "暂不支持该语种语音输入", GlobalToast.Type.ERROR);
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
                        // 手指松开
                        Timber.tag("TouchEvent").d( "手指松开 TextView");
                        ll_bottom_edit.setVisibility(View.VISIBLE);
                        callback.pressUp(isInArea);
                        long duration = SystemClock.elapsedRealtime() - startTime;
//                        if (duration < MIN_DURATION_MS && isInArea) {
//                            GlobalToast.show((Activity) context, context.getString(R.string.record_toast), GlobalToast.Type.ERROR);
//                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        // 获取屏幕坐标
                        float rawX = motionEvent.getRawX();
                        float rawY = motionEvent.getRawY();

//                        // 获取视图在屏幕中的位置
                        ll_voice_edit.getLocationOnScreen(location);
                        int viewLeft = location[0];
                        int viewTop = location[1];
                        int viewRight = viewLeft + ll_voice_edit.getWidth();
                        int viewBottom = viewTop + ll_voice_edit.getHeight();
                        
                        Timber.tag("TouchEvent").d( "rawX: " + rawX + ", rawY: " + rawY);
                        Timber.tag("TouchEvent").d( "viewLeft: " + viewLeft + ", viewTop: " + viewTop + ", viewRight: " + viewRight + ", viewBottom: " + viewBottom);

                        // 判断是否在视图范围内
                        if (rawX < viewLeft || rawX > viewRight || rawY < viewTop || rawY > viewBottom) {
                            Timber.tag("TouchEvent").d( "手指移出 TextView 范围");
                            isInArea = false;
                        } else {
                            Timber.tag("TouchEvent").d( "手指在 TextView 范围内移动");
                            isInArea = true;
                        }
                        callback.voiceMove(isInArea);
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        // 触摸取消（例如被父视图拦截）
                        Timber.tag("TouchEvent").d("触摸取消");
                        isInArea = false;
                        ll_bottom_edit.setVisibility(View.VISIBLE);
                        break;
                }
                return false; // 返回 true 表示消费事件
            }
        });

        if (ed != null) {
            ed.setOnKeyListener((v, keyCode, event) -> {

                if (event.getAction() == KeyEvent.ACTION_DOWN) {

                    if (keyCode == KeyEvent.KEYCODE_ENTER
                            || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {

                        if (callback != null) {
                            String content = ed.getText().toString();
                            if (content.isEmpty()) {
                                ZUtils.showToast("请输入内容");
                                return true;
                            }
                            KeyboardUtils.hideSoftKeyboard((Activity) context);
                            sendMsg(content);
                        }
                        return true;
                    }

                }

                return false;
            });
        }
    }

    private void changeSoftkey(int state, View view) {
        if (state == 0) {//键盘模式
            isVoice = false;

            if (callback != null) {
                callback.keyboard();
            }

            iv_keyboard.setVisibility(View.GONE);
            tv_press.setVisibility(View.GONE);
            iv_logo.setVisibility(View.GONE);
            iv_voice.setVisibility(View.VISIBLE);
            ll_edit.setVisibility(View.VISIBLE);
            if (view != null) {
                ZInputMethod.openInputMethod(ed);
            }
        } else if (state == 1) {//语音模式

            if (!AuthHelper.getInstance().isLogin()) {
                // 未登录，跳转到一键登录页面
                Intent intent = new Intent(context, OneClickLoginActivity.class);
                context.startActivity(intent);
                return;
            }
            isVoice = true;
            if (callback != null) {
                callback.voice();
            }
            ed.setText("");
            iv_keyboard.setVisibility(View.VISIBLE);
            tv_press.setVisibility(View.VISIBLE);
            iv_logo.setVisibility(View.VISIBLE);
            iv_voice.setVisibility(View.GONE);
            ll_edit.setVisibility(View.GONE);
            ZInputMethod.hideKeyboard(context, view.getWindowToken());
        }
    }

    public void sendMsg(String content) {
        // 检查内容是否为空或只包含空格、换行符等空白字符
        if (TextUtils.isEmpty(content) || content.trim().isEmpty()) {
            ZUtils.showToast("请输入有效内容");
            return;
        }
        changeSoftkey(0, null);
        String sendContent = content;
        if (!Constant.isUseLingXiTranslation) {
            sendContent = content + " 翻译为" + sleet2;
            content = "帮我把这段文本翻译成" + sleet2 + ":\"" + content + "\"";
        }

        ed.setText("");
        callback.send(content, sendContent, sleet1, sleet2);
    }


    public void setCallback(AITranslateEditCallback callback) {
        // 设置回调接口
        this.callback = callback;
    }

    public void getModel() {
        listModel = new ArrayList<>();
        if (Constant.isUseLingXiTranslation) {
            listModel.add(new AiWritingTypeBean("中文"));
            listModel.add(new AiWritingTypeBean("英语"));
            listModel.add(new AiWritingTypeBean("日语"));
            listModel.add(new AiWritingTypeBean("韩语"));
            listModel.add(new AiWritingTypeBean("西班牙语"));
            listModel.add(new AiWritingTypeBean("法语"));
            listModel.add(new AiWritingTypeBean("德语"));
            listModel.add(new AiWritingTypeBean("俄语"));
            listModel.add(new AiWritingTypeBean("意大利语"));
        } else {
            listModel.add(new AiWritingTypeBean("自动检测"));
            listModel.add(new AiWritingTypeBean("英语"));
            listModel.add(new AiWritingTypeBean("简体中文"));
            listModel.add(new AiWritingTypeBean("繁体中文"));
        }


        selectOption1 = listModel.get(0);

        listModel2 = new ArrayList<>();
        if (Constant.isUseLingXiTranslation) {
            listModel2.add(new AiWritingTypeBean("中文"));
            listModel2.add(new AiWritingTypeBean("英语"));
            listModel2.add(new AiWritingTypeBean("日语"));
            listModel2.add(new AiWritingTypeBean("韩语"));
            listModel2.add(new AiWritingTypeBean("西班牙语"));
            listModel2.add(new AiWritingTypeBean("法语"));
            listModel2.add(new AiWritingTypeBean("德语"));
            listModel2.add(new AiWritingTypeBean("俄语"));
            listModel2.add(new AiWritingTypeBean("意大利语"));
        } else {
            listModel2.add(new AiWritingTypeBean("英语"));
            listModel2.add(new AiWritingTypeBean("简体中文"));
            listModel2.add(new AiWritingTypeBean("繁体中文"));
        }


        selectOption2 = listModel2.get(1);
        tv_to.setText(selectOption2.getName());
        sleet2 = selectOption2.getName();
        sleet1 = selectOption1.getName();
        tv_from.setText(sleet1);
        selectLanguageModel.add("中文");
        selectLanguageModel.add("英语");

    }

    public ImageView getIv_close() {
        return iv_close;
    }
}
