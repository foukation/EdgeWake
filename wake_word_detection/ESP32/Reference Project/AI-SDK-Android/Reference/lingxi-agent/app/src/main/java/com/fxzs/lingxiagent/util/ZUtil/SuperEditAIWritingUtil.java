package com.fxzs.lingxiagent.util.ZUtil;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.model.chat.callback.AIMeetingEditCallback;
import com.fxzs.lingxiagent.model.chat.callback.OnItemClickAncor;
import com.fxzs.lingxiagent.model.chat.dto.AiWritingTypeBean;
import com.fxzs.lingxiagent.model.chat.dto.OptionModel;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.ChatLengthInputFilter;
import com.fxzs.lingxiagent.util.KeyboardUtils;
import com.fxzs.lingxiagent.util.NetworkUtils;
import com.fxzs.lingxiagent.util.ShadowUtils;
import com.fxzs.lingxiagent.util.ZInputMethod;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.auth.OneClickLoginActivity;
import com.fxzs.lingxiagent.view.chat.AiWritingTypeAdapter;
import com.fxzs.lingxiagent.view.chat.OptionAiMeetingAdapter;
import com.fxzs.lingxiagent.view.common.GlobalToast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

public class SuperEditAIWritingUtil {

    Context context;
    LinearLayout root_view;
    View ll_bottom_edit;
    View ll_voice_edit;


    ImageView iv_keyboard;
    ImageView iv_add;
    TextView tv_press;
    View iv_logo;

    ImageView iv_voice;

    TextView tv_voice_hint;
    View rl_voice;
    private View ll_edit;

    private EditText ed;
    ImageView iv_send;
    private AIMeetingEditCallback callback;
    List<AiWritingTypeBean> listModel;
    OptionModel selectOptionModel;
    private RecyclerView rv_type;
    private RecyclerView rv_type2;
    private AiWritingTypeAdapter typeAdapter;
    private List<AiWritingTypeBean> l2;
    private AiWritingTypeAdapter type2Adapter;
    private ImageView iv_close,ivControl;
    private LinearLayout llTypeContent,llControl;
    HashMap<String,AiWritingTypeBean> selectHash = new HashMap<>();

    boolean isVoice = false;
    boolean isRecord = false;
    private boolean isInArea = true;
    private int[] location = new int[2];

    private long startTime = 0L;
    private long MIN_DURATION_MS = 1000L;
    private boolean isControlOpen = true;
    public SuperEditAIWritingUtil(Context context, LinearLayout root_view) {
        this.context = context;
        this.root_view = root_view;
        getModel();
        setUI();
    }

    private void setUI() {
        rv_type = root_view.findViewById(R.id.rv_type);
        rv_type2 = root_view.findViewById(R.id.rv_type2);
        ed = root_view.findViewById(R.id.ed);
        iv_send = root_view.findViewById(R.id.iv_send);
        iv_close = root_view.findViewById(R.id.iv_close);
        llTypeContent = root_view.findViewById(R.id.ll_type_content);
        ivControl = root_view.findViewById(R.id.iv_control);
        llControl = root_view.findViewById(R.id.ll_control);

        ll_bottom_edit = root_view.findViewById(R.id.ll_bottom_edit);
        ll_voice_edit = root_view.findViewById(R.id.ll_voice_edit);
        ll_edit = root_view.findViewById(R.id.ll_edit);

        iv_keyboard = root_view.findViewById(R.id.iv_keyboard);
        iv_add = root_view.findViewById(R.id.iv_add);
        tv_voice_hint = root_view.findViewById(R.id.tv_voice_hint);
        rl_voice = root_view.findViewById(R.id.rl_voice);
        iv_voice = root_view.findViewById(R.id.iv_voice);
        tv_press = root_view.findViewById(R.id.tv_press);
        tv_voice_hint = root_view.findViewById(R.id.tv_voice_hint);
        rl_voice = root_view.findViewById(R.id.rl_voice);
        iv_logo = root_view.findViewById(R.id.iv_logo);
        ShadowUtils.applyDefaultShadowTransparent(ll_bottom_edit,context);
        root_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


            }
        });
        ZInputMethod.openInputMethod(ed);


        setTypeRv();
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
        iv_close.setOnClickListener(view -> {
            isControlOpen = true;
            ivControl.setImageResource(R.drawable.home_close_dialogue);
            llTypeContent.setVisibility(View.VISIBLE);
            changeSoftkey(0,null);
            if (callback != null) {
                callback.close();
            }
        });
        ed.setFilters(new InputFilter[]{
                new ChatLengthInputFilter((Activity) context,Constants.DIALOG_INPUT_NUMBER)
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
                }else {
                    iv_voice.setVisibility(View.VISIBLE);
//                    iv_add.setVisibility(View.VISIBLE);
                    iv_send.setVisibility(View.GONE);
                }
            }
        });

        iv_voice.setOnClickListener(view -> {
            if ( !AuthHelper.getInstance().isLogin()) {
                // 未登录，跳转到一键登录页面
                Intent intent = new Intent(context, OneClickLoginActivity.class);
                context.startActivity(intent);
                return;
            }
            changeSoftkey(1,view);


        });

        iv_keyboard.setOnClickListener(view -> changeSoftkey(0,view));

        ll_voice_edit.setOnLongClickListener(view -> {
            Timber.tag("TouchEvent").d( "onLongClick ====== >");
            if (isVoice){
                return true;
            }
            return false;
        });



        ll_voice_edit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (!isVoice){
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
                        ll_voice_edit.setVisibility(View.VISIBLE);
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

//                        // 获取视图在屏幕中的位置
                        ll_voice_edit.getLocationOnScreen(location);
                        int viewLeft = location[0];
                        int viewTop = location[1];
                        int viewRight = viewLeft + ll_voice_edit.getWidth();
                        int viewBottom = viewTop + ll_voice_edit.getHeight();

                        Timber.tag("TouchEvent").d( "rawX: " + rawX + ", rawY: " + rawY);
                        Timber.tag("TouchEvent").d( "viewLeft: " + viewLeft + ", viewTop: " + viewTop+ ", viewRight: " + viewRight+ ", viewBottom: " + viewBottom);
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
                        Timber.tag("TouchEvent").d( "触摸取消");
                        isInArea = false;
                        ll_voice_edit.setVisibility(View.VISIBLE);
                        break;
                }
                return false; // 返回 true 表示消费事件
            }
        });

        llControl.setOnClickListener(v -> {
            isControlOpen =!isControlOpen;
            if (isControlOpen) {
                ivControl.setImageResource(R.drawable.home_close_dialogue);
                llTypeContent.setVisibility(View.VISIBLE);
            }else {
                ivControl.setImageResource(R.drawable.home_open_dialogue);
                llTypeContent.setVisibility(View.GONE);
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


    private void changeSoftkey(int state,View view) {
        if(state == 0){//键盘模式
            isVoice = false;

            if (callback != null) {
                callback.keyboard();
            }

            iv_keyboard.setVisibility(View.GONE);
            tv_press.setVisibility(View.GONE);
            iv_logo.setVisibility(View.GONE);
            iv_voice.setVisibility(View.VISIBLE);
            ll_edit.setVisibility(View.VISIBLE);
            if(view !=null){
                ZInputMethod.openInputMethod(ed);
            }
        }else if(state == 1){//语音模式
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
            ZInputMethod.hideKeyboard(context,view.getWindowToken());
        }
    }

    public void sendMsg(String content) {
        // 检查内容是否为空或只包含空格、换行符等空白字符
        if(TextUtils.isEmpty(content) || content.trim().isEmpty()){
            ZUtils.showToast("请输入有效内容");
            return;
        }
        if (content.length() > Constants.DIALOG_INPUT_NUMBER) {
            GlobalToast.show((Activity) context, context.getString(R.string.dialog_input_content_hint), GlobalToast.Type.ERROR);
            return;
        }
        changeSoftkey(0,null);

        String sendContent = "";
        if(typeAdapter.getSelectedPosition() == -1){
            sendContent = "帮我写作"+ "，"+content;
        }else {
            sendContent = "帮我写"+listModel.get(typeAdapter.getSelectedPosition()).getName() + "，"+"主题是"+content;
        }
        for (int i = 0; i < l2.size(); i++) {
            String name =  l2.get(i).getName();
            String subName =  l2.get(i).getSubName();
            if(!TextUtils.isEmpty(subName)){
//                if(i != l2.size()-1){
                    sendContent+="，";
//                }
                sendContent+= name + "是" + subName ;
            }
        }
        ed.setText("");
        isControlOpen = false;
        ivControl.setImageResource(R.drawable.home_open_dialogue);
        llTypeContent.setVisibility(View.GONE);
        callback.send(sendContent);
    }

    private void setTypeRv() {
        StaggeredGridLayoutManager manager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.HORIZONTAL);

        rv_type.setLayoutManager(manager);
        typeAdapter = new AiWritingTypeAdapter(context,listModel, AiWritingTypeAdapter.TYPE_ONE,new OnItemClickAncor() {
            @Override
            public void onItemClick(int position, View view) {

                List<AiWritingTypeBean> list = listModel.get(position).getListModel();
                String name = listModel.get(position).getName();
                setType2Rv(list);
                ed.setHint("输入要写的"+name+"主题和要求...");
            }
        });
        rv_type.setAdapter(typeAdapter);

        List<AiWritingTypeBean> list = listModel.get(0).getListModel();
        setType2Rv(list);
    }

    private void setType2Rv(List<AiWritingTypeBean> list) {
        l2 = list;
        rv_type2.setLayoutManager(new GridLayoutManager(context, 1, GridLayoutManager.HORIZONTAL, false));


        type2Adapter = new AiWritingTypeAdapter(context,l2,AiWritingTypeAdapter.TYPE_TWO, new OnItemClickAncor() {
            @Override
            public void onItemClick(int position, View view) {
                List<AiWritingTypeBean> l3 = l2.get(position).getListModel();
                String name = l2.get(position).getName();
                AiWritingTypeBean selectOption = selectHash.get(name);
                ZUtils.showAIMeetingPopup(context,view,l3,selectOption,new OptionAiMeetingAdapter.OnOptionSelectedListener() {
                    @Override
                    public void onOptionSelected(AiWritingTypeBean option) {
                       selectHash.put(name,option);

                        list.get(position).setSubName(option.getName());
                        type2Adapter.notifyDataSetChanged();
                    }
                });
            }
        });
        rv_type2.setAdapter(type2Adapter);
        type2Adapter.clearAllSubName();
        selectHash = new HashMap<>();
    }

    public void hideBottomView(){
        isControlOpen = true;
        ivControl.setImageResource(R.drawable.home_close_dialogue);
        llTypeContent.setVisibility(View.VISIBLE);
        changeSoftkey(0,null);
        if (callback != null) {
            callback.close();
        }
    }

    public void setCallback(AIMeetingEditCallback callback) {
        // 设置回调接口
        this.callback = callback;
    }

    public void getModel(){
        List<AiWritingTypeBean> l3list1 = new ArrayList<>();
        l3list1.add(new AiWritingTypeBean("短"));
        l3list1.add(new AiWritingTypeBean("中"));
        l3list1.add(new AiWritingTypeBean("长"));
        AiWritingTypeBean l2b1 = new AiWritingTypeBean("长度",l3list1);


        List<AiWritingTypeBean> l3list2 = new ArrayList<>();
        l3list2.add(new AiWritingTypeBean("中文"));
        l3list2.add(new AiWritingTypeBean("英文"));
        AiWritingTypeBean l2b2 = new AiWritingTypeBean("语言",l3list2);


        List<AiWritingTypeBean> l3list3 = new ArrayList<>();
        l3list3.add(new AiWritingTypeBean("记叙文"));
        l3list3.add(new AiWritingTypeBean("议论文"));
        l3list3.add(new AiWritingTypeBean("散文"));
        l3list3.add(new AiWritingTypeBean("说明文"));
        l3list3.add(new AiWritingTypeBean("应用文"));
        AiWritingTypeBean l2b3 = new AiWritingTypeBean("文体",l3list3);

        List<AiWritingTypeBean> l3list4 = new ArrayList<>();
        l3list4.add(new AiWritingTypeBean("小学"));
        l3list4.add(new AiWritingTypeBean("初中"));
        l3list4.add(new AiWritingTypeBean("高中"));
        AiWritingTypeBean l2b4 = new AiWritingTypeBean("教育阶段",l3list4);

        List<AiWritingTypeBean> l3list5 = new ArrayList<>();
        l3list5.add(new AiWritingTypeBean("100"));
        l3list5.add(new AiWritingTypeBean("200"));
        l3list5.add(new AiWritingTypeBean("300"));
        l3list5.add(new AiWritingTypeBean("400"));
        l3list5.add(new AiWritingTypeBean("500"));
        l3list5.add(new AiWritingTypeBean("600"));
        l3list5.add(new AiWritingTypeBean("800"));
        AiWritingTypeBean l2b5 = new AiWritingTypeBean("字数",l3list5);

        List<AiWritingTypeBean> l3list6 = new ArrayList<>();
        l3list6.add(new AiWritingTypeBean("简洁"));
        l3list6.add(new AiWritingTypeBean("正式"));
        l3list6.add(new AiWritingTypeBean("生动"));
        l3list6.add(new AiWritingTypeBean("幽默"));
        l3list6.add(new AiWritingTypeBean("高情商"));
        l3list6.add(new AiWritingTypeBean("口语化"));
        AiWritingTypeBean l2b6 = new AiWritingTypeBean("写作风格",l3list6);

        List<AiWritingTypeBean> l3list7 = new ArrayList<>();
        l3list7.add(new AiWritingTypeBean("润色"));
        l3list7.add(new AiWritingTypeBean("扩写"));
        l3list7.add(new AiWritingTypeBean("续写"));
        l3list7.add(new AiWritingTypeBean("缩写"));
        AiWritingTypeBean l2b7 = new AiWritingTypeBean("改写方式",l3list7);



        List<AiWritingTypeBean> l3list8 = new ArrayList<>();
        l3list8.add(new AiWritingTypeBean("10"));
        l3list8.add(new AiWritingTypeBean("15"));
        l3list8.add(new AiWritingTypeBean("20"));
        l3list8.add(new AiWritingTypeBean("25"));
        l3list8.add(new AiWritingTypeBean("30"));
        AiWritingTypeBean l2b8 = new AiWritingTypeBean("页数",l3list8);

        List<AiWritingTypeBean> l3list9 = new ArrayList<>();
        l3list9.add(new AiWritingTypeBean("日报"));
        l3list9.add(new AiWritingTypeBean("周报"));
        l3list9.add(new AiWritingTypeBean("月报"));
        l3list9.add(new AiWritingTypeBean("季度报"));
        l3list9.add(new AiWritingTypeBean("年报"));
        AiWritingTypeBean l2b9 = new AiWritingTypeBean("汇报类型",l3list9);

        List<AiWritingTypeBean> l3list10 = new ArrayList<>();
        l3list10.add(new AiWritingTypeBean("童话"));
        l3list10.add(new AiWritingTypeBean("历史"));
        l3list10.add(new AiWritingTypeBean("科幻"));
        l3list10.add(new AiWritingTypeBean("武侠"));
        l3list10.add(new AiWritingTypeBean("言情"));
        l3list10.add(new AiWritingTypeBean("侦探"));
        l3list10.add(new AiWritingTypeBean("职场"));
        AiWritingTypeBean l2b10 = new AiWritingTypeBean("类型",l3list10);

        List<AiWritingTypeBean> l3list11 = new ArrayList<>();
        l3list11.add(new AiWritingTypeBean("微信"));
        l3list11.add(new AiWritingTypeBean("小红书"));
        l3list11.add(new AiWritingTypeBean("微博"));
        l3list11.add(new AiWritingTypeBean("淘宝"));
        AiWritingTypeBean l2b11 = new AiWritingTypeBean("发布平台",l3list11);


//        List<AiWritingTypeBean> l3list12 = new ArrayList<>();
//        l3list12.add(new AiWritingTypeBean("历史"));
//        l3list12.add(new AiWritingTypeBean("科幻"));
//        l3list12.add(new AiWritingTypeBean("武侠"));
//        l3list12.add(new AiWritingTypeBean("言情"));
//        l3list12.add(new AiWritingTypeBean("侦探"));
//        l3list12.add(new AiWritingTypeBean("职场"));
//        AiWritingTypeBean l2b12 = new AiWritingTypeBean("写作风格",l3list12);

        List<AiWritingTypeBean> l3list13 = new ArrayList<>();
        l3list13.add(new AiWritingTypeBean("喜悦的"));
        l3list13.add(new AiWritingTypeBean("悲伤的"));
        l3list13.add(new AiWritingTypeBean("思念的"));
        l3list13.add(new AiWritingTypeBean("无奈的"));
        l3list13.add(new AiWritingTypeBean("积极的"));
        AiWritingTypeBean l2b13 = new AiWritingTypeBean("表达感情",l3list13);

        List<AiWritingTypeBean> l3list14 = new ArrayList<>();
        l3list14.add(new AiWritingTypeBean("聊天消息"));
        l3list14.add(new AiWritingTypeBean("朋友圈"));
        l3list14.add(new AiWritingTypeBean("评论"));
        l3list14.add(new AiWritingTypeBean("邮件"));
        l3list14.add(new AiWritingTypeBean("投诉"));
        l3list14.add(new AiWritingTypeBean("咨询"));
        AiWritingTypeBean l2b14 = new AiWritingTypeBean("消息类型",l3list14);

        List<AiWritingTypeBean> l3list15 = new ArrayList<>();
        l3list15.add(new AiWritingTypeBean("语气"));
        l3list15.add(new AiWritingTypeBean("礼貌"));
        l3list15.add(new AiWritingTypeBean("热情"));
        l3list15.add(new AiWritingTypeBean("简洁"));
        l3list15.add(new AiWritingTypeBean("口语化"));
        l3list15.add(new AiWritingTypeBean("高情商"));
        AiWritingTypeBean l2b15 = new AiWritingTypeBean("语气",l3list15);

        List<AiWritingTypeBean> l3list16 = new ArrayList<>();
        l3list16.add(new AiWritingTypeBean("朋友"));
        l3list16.add(new AiWritingTypeBean("恋人"));
        l3list16.add(new AiWritingTypeBean("同学"));
        l3list16.add(new AiWritingTypeBean("家人"));
        l3list16.add(new AiWritingTypeBean("领导"));
        l3list16.add(new AiWritingTypeBean("客户"));
        AiWritingTypeBean l2b16 = new AiWritingTypeBean("回复对象",l3list16);

        List<AiWritingTypeBean> l3list17 = new ArrayList<>();
        l3list17.add(new AiWritingTypeBean("儿童"));
        l3list17.add(new AiWritingTypeBean("青少年"));
        l3list17.add(new AiWritingTypeBean("成年读者"));
        AiWritingTypeBean l2b17 = new AiWritingTypeBean("教育阶段",l3list17);


        List<AiWritingTypeBean> l3list18 = new ArrayList<>();
        l3list18.add(new AiWritingTypeBean("儿童"));
        l3list18.add(new AiWritingTypeBean("青少年"));
        l3list18.add(new AiWritingTypeBean("成年读者"));
        AiWritingTypeBean l2b18 = new AiWritingTypeBean("受众",l3list18);

        List<AiWritingTypeBean> l3list19 = new ArrayList<>();
        l3list19.add(new AiWritingTypeBean("本科"));
        l3list19.add(new AiWritingTypeBean("硕士"));
        l3list19.add(new AiWritingTypeBean("博士"));
        AiWritingTypeBean l2b19 = new AiWritingTypeBean("教育阶段",l3list19);


//        List<AiWritingTypeBean> l1list1 = new ArrayList<>();
        listModel = new ArrayList<>();
        listModel.add(new AiWritingTypeBean("文章",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b6);
                add(l2b5);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("回复",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b14);
                add(l2b15);
                add(l2b16);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("作文",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b3);
                add(l2b4);
                add(l2b5);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("改写",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b7);
//                add(l2b1);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("宣传文案",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b6);
                add(l2b11);
                add(l2b5);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("PPT 大纲",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b8);
                add(l2b2);
            }
        }));
//        listModel.add(new AiWritingTypeBean("论文",new ArrayList<AiWritingTypeBean>(){
//            {
//                add(l2b19);
//                add(l2b1);
//                add(l2b2);
//            }
//        }));
        listModel.add(new AiWritingTypeBean("总结汇报",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b9);
                add(l2b5);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("小说",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b10);
                add(l2b5);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("故事",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b10);
                add(l2b17);
                add(l2b5);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("脚本",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b6);
                add(l2b5);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("朋友圈",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b6);
//                add(l2b1);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("小红书",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b6);
//                add(l2b1);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("微博",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b6);
//                add(l2b1);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("诗歌",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b13);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("教案",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b18);
                add(l2b5);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("评语",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b6);
                add(l2b5);
                add(l2b2);
            }
        }));
//        listModel.add(new AiWritingTypeBean("研究报告",new ArrayList<AiWritingTypeBean>(){
//            {
//                add(l2b1);
//                add(l2b2);
//            }
//        }));
        listModel.add(new AiWritingTypeBean("方案策划",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b5);
                add(l2b2);
            }
        }));

        listModel.add(new AiWritingTypeBean("会议纪要",new ArrayList<AiWritingTypeBean>(){
            {
//                add(l2b1);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("申请",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b5);
                add(l2b2);
            }
        }));

        listModel.add(new AiWritingTypeBean("邮件",new ArrayList<AiWritingTypeBean>(){
            {
//                add(l2b1);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("日记",new ArrayList<AiWritingTypeBean>(){
            {
                add(l2b5);
                add(l2b2);
            }
        }));
        listModel.add(new AiWritingTypeBean("计划",new ArrayList<AiWritingTypeBean>(){
            {
//                add(l2b1);
                add(l2b2);
            }
        }));
    }

    public ImageView getIv_close() {
        return iv_close;
    }
}
