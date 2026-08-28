package com.fxzs.lingxiagent.view.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.GlobalDataHolder;
import com.fxzs.smartassist.util.CustomImageButton;
import com.fxzs.smartassist.util.VoiceEnergyDetector;
import com.fxzs.smartassist.util.ZUtil.ScreenUtils;
import com.fxzs.smartassist.util.ZUtil.SizeUtils;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;


public class ChatInputLayout extends ConstraintLayout {
    private EditOperateView etUserInput;
    private ImageView btIcon, btImage;
    private CustomImageButton btSend;
    private boolean isVoiceMode = false;
    private OnInputListener inputListener;
    private VoiceEnergyDetector mVoiceEnergyDetector;

    private RelativeLayout mFyRelativeLayout;
    private ImageView fySpinnerClose;
    private CustomSpinner fyTagSpinner, fyTagresSpinner;
    private View bottomView;
    private ImageView ivEnlarge;

    private boolean isEditStatus;
    private LinearLayout llEdit;

    private int keyBoardHeight = 0;
    private boolean isOpenKeyBoard = false;
    private int[] locationVoice = new int[2];
    private int screenHeight = 0;

    private final int dp_16 = SizeUtils.dpToPx(16);
    private final int dp_10 = SizeUtils.dpToPx(10);
    public ChatInputLayout(Context context) {
        super(context);
        init(context);
    }

    public ChatInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChatInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.layout_chat_input, this, true);
        mVoiceEnergyDetector = new VoiceEnergyDetector();
        // 初始化组件
        etUserInput = findViewById(R.id.et_user_input);
        btIcon = findViewById(R.id.bt_icon);
        btImage = findViewById(R.id.bt_image);
        btSend = findViewById(R.id.bt_send);
        bottomView = findViewById(R.id.bottomView);
        ivEnlarge = findViewById(R.id.iv_enlarge);
        llEdit = findViewById(R.id.ll_edit);
        LinearLayout llChatInput = findViewById(R.id.ll_chat_input);
        setShadow(llChatInput,getContext(),SizeUtils.dpToPx(16), Color.GRAY,false,Color.WHITE,21);
        mFyRelativeLayout = findViewById(R.id.fy_select);
        fySpinnerClose = findViewById(R.id.spinner_close);
        fyTagSpinner = findViewById(R.id.fy_target_spinner);
        fyTagresSpinner = findViewById(R.id.fy_result_spinner);
        fyTagSpinner.setSelectPosition(GlobalDataHolder.getTranslateFromTab());
        fyTagresSpinner.setSelectPosition(GlobalDataHolder.getTranslateToTab());
        fyTagSpinner.setItemSelectListener(GlobalDataHolder::saveTranslateFromTab);
        fyTagresSpinner.setItemSelectListener(GlobalDataHolder::saveTranslateToTab);
        ImageView imageView = findViewById(R.id.fy_convert_button);
        imageView.setOnClickListener((View view) -> {
            int tag = fyTagSpinner.getSelectedItemPosition();
            int res = fyTagresSpinner.getSelectedItemPosition();
            fyTagSpinner.setSelectPosition(res);
            fyTagresSpinner.setSelectPosition(tag);
        });
        fySpinnerClose.setOnClickListener(view -> {
            mFyRelativeLayout.setVisibility(GONE);
            if(inputListener != null) {
                inputListener.onChatBoxSpinnerHide();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(bottomView, (v, insets) -> {
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, navInsets.bottom);
//            LogUtils.INSTANCE.d("底部高度"+navInsets.bottom);
//            etUserInput.setBottomHeight(navInsets.bottom);
            return insets;
        });
        screenHeight = ScreenUtils.getScreenHeight(getContext());


//        post(() -> {
//            llEdit.getLocationOnScreen(locationVoice);
//            int lastHeight =screenHeight - llEdit.getHeight()-locationVoice[1];
//            Log.d("ChatInputLayout","底部剩余高度"+lastHeight  +" screenHeight = "+screenHeight  + "llEdit.getHeight() = "+llEdit.getHeight() + "  locationVoice[1]= "+locationVoice[1]);
//            etUserInput.setBottomHeight(lastHeight);
//
//        });

        // 设置输入框监听
        etUserInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (inputListener != null) {
                    inputListener.onTextChanged(s.toString());
                }
                isEditStatus = etUserInput.isEditStatus();
//                LogUtils.INSTANCE.d("updateSendButtonState" + isVoiceMode + "  s.toString()=" + s.toString() + "  isEditStatus=" + isEditStatus + " getLineCount=" + etUserInput.getLineCount());
                int lineCount = etUserInput.getLineCount();
                if (!TextUtils.isEmpty(s.toString()) && isEditStatus) {
                    setImageResource(R.drawable.home_send);
                    btImage.setVisibility(GONE);
                } else if (isEditStatus) {
                    setImageResource(R.drawable.home_voice);
                    btImage.setVisibility(VISIBLE);
                } else {
                    updateSendButtonState(s.length() > 0);
                }

                if (lineCount > 1) {
                    setLayoutGravity(btSend, Gravity.BOTTOM);
                } else {
                    setLayoutGravity(btSend, Gravity.CENTER);
                }
                ivEnlarge.setVisibility(lineCount > 2 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        etUserInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {  // 检测 "发送" 动作
                String text = etUserInput.getText().toString().trim();
                if (!text.isEmpty() && inputListener != null) {
                    inputListener.onSendButtonClicked(text);
                    etUserInput.setText("");
                }
                return true;  // 消费事件
            }
            return false;  // 其他情况交给系统处理
        });
        // 设置语音/发送按钮点击事件
        btSend.setOnClickListener(v -> {
            int currentImageResourceId = btSend.getCurrentImageResourceId();
            if (currentImageResourceId != R.drawable.home_send) {
                toggleVoiceTextMode(!isVoiceMode);

                // TODO: 2025/7/30 接入几目注释，记得修改
//                if (!isVoiceMode && AIConversationManager.Companion.getCurrentState() == AIConversationManager.ConversationState.IDLE) {
                if (!isVoiceMode) {
                    // 显示软键盘
                    showSoftKeyboard(getContext(), etUserInput);
                } else {
                    hideSoftKeyboard(getContext(), etUserInput);
                }
            } else {
                String text = etUserInput.getText().toString().trim();
                if (!text.isEmpty() && inputListener != null) {
                    inputListener.onSendButtonClicked(text);
                    etUserInput.setText("");
                }
            }
        });


        // 设置图片按钮点击事件
        btImage.setOnClickListener(v -> {
            if (inputListener != null) {
                inputListener.onImageButtonClicked();
            }
        });

        etUserInput.setOnFinishedRecordListener(new EditOperateView.OnFinishedRecordListener() {
            @Override
            public void onStartRecord() {
//                LogUtils.INSTANCE.d("开始录音");
                if (inputListener != null){
                    inputListener.onStartRecording();
                }

            }


            @Override
            public void onStopRecord() {
            }

            @Override
            public void onCancelRecord() {
                if (inputListener != null){
                    inputListener.onCancelRecording();
                }
            }

            @Override
            public void onCompleteRecord() {
                if (inputListener != null){
                    inputListener.onStopRecording();
                }

            }

            @Override
            public void onChangeSoftKeyboard(boolean open, int height) {
                keyBoardHeight = height;
                isOpenKeyBoard = open;
                if (isOpenKeyBoard){
                    btIcon.setAlpha(1f);
                    btIcon.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                        btIcon.setVisibility(View.GONE);
                        etUserInput.setPadding(dp_16,dp_10,0,dp_10);
                    }).start();
                }else if (TextUtils.isEmpty(etUserInput.getText())){
                    btIcon.setVisibility(View.VISIBLE);
                    btIcon.setAlpha(0f);
                    btIcon.animate().alpha(1f).setDuration(50).withEndAction(()->{
                        etUserInput.setPadding(0,dp_10,0,dp_10);
                    }).start();

                }
            }
        });

        ivEnlarge.setOnClickListener(view -> showInputDialog());
        defaultState();
    }

    public String getString() {
        if (etUserInput != null) {
            return etUserInput.getText().toString();
        }
        return null;
    }

    public void setText(String text) {
        if (etUserInput != null) {
            etUserInput.setText(text);
        }
    }

    public void changeTagSpinnerStatus(boolean show){
        if(show) {
            mFyRelativeLayout.setVisibility(VISIBLE);
        } else {
            mFyRelativeLayout.setVisibility(GONE);
        }
    }

    public String getFySpinnerText(boolean getRes){
        if(getRes) {
            return fyTagresSpinner.getSelectedItem();
        } else {
            return fyTagSpinner.getSelectedItem();
        }
    }

    // 切换语音/文本输入模式
    public void toggleVoiceTextMode(boolean voiceMode) {
        isVoiceMode = voiceMode;
        etUserInput.setEditStatus();
        btSend.setImageResource(etUserInput.isEditStatus() ? R.drawable.home_voice : R.drawable.home_keyboard);
    }

    // 更新发送按钮状态
    private void updateSendButtonState(boolean hasText) {
//        Log.d("updateSendButtonState", "updateSendButtonState" + etUserInput.isEditStatus() + "hasText = " + hasText);
        btSend.setImageResource(etUserInput.isEditStatus() ? R.drawable.home_send : R.drawable.home_voice);

    }

    // 设置输入监听器
    public void setOnInputListener(OnInputListener listener) {
        this.inputListener = listener;
    }

    // 输入监听器接口
    public interface OnInputListener {
        void onTextChanged(String text);

        void onSendButtonClicked(String text);

        void onVoiceButtonClicked();

        void onImageButtonClicked();

        void onStartRecording();

        void onStopRecording();

        void onCancelRecording();

        void onChatBoxSpinnerHide();
    }

    public void recordingState() {
;
    }

    public void defaultState() {
    }

    @SuppressLint("ClickableViewAccessibility")
    private void voiceState() {
        etUserInput.setHintTextColor(Color.parseColor("#B3B3B3"));
        etUserInput.setText("");
        etUserInput.setHint(R.string.text_voice_hint);
        btSend.setImageResource(R.drawable.home_keyboard);
        etUserInput.clearFocus();
        etUserInput.setCursorVisible(false);
        etUserInput.setFocusable(false);
        etUserInput.setFocusableInTouchMode(false);
        etUserInput.setClickable(true);
        hideSoftKeyboard(getContext(), etUserInput);
    }

    /**
     * 发送状态
     */
    public void sendState() {
        hideSoftKeyboard(getContext(), etUserInput);
        if (isEditStatus) {
            etUserInput.setText("");
        }
    }

    public void sendState_stopListening() {
        sendState();
        mVoiceEnergyDetector.stopListening();
    }

    /**
     * 键盘状态
     */
    @SuppressLint("ClickableViewAccessibility")
    public void keyBoardState() {
//        LogUtils.INSTANCE.d("当前键盘状态" + isVoiceMode);
        etUserInput.setEditStatus();
    }

    /**
     * 收音中
     */
    public void setRecording() {
        // TODO: 2025/7/30  接入几目注释，记得修改
//        TtsXiaDuMediaPlayer.getInstance().stop();
//        TtsMediaPlayer.getInstance().stop();
//        //进入就自动收音
//        recordingState();
//        // mVoiceEnergyDetector.startListening(dB -> setAnimationSpeed(mVoiceEnergyDetector.getVoiceState(dB)));
//
//        if (TabEntity.agentType == TabEntity.TabType.TRANSLATE) {
//            Intent broadcastIntent = new Intent(BroadcastConstants.ACTION_TRANSLATION_START);
//            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcastIntent);
//            return;
//        }
//
//        Intent broadcastIntent = new Intent(BroadcastConstants.ACTION_SPEECH_START);
//        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcastIntent);

    }

    public boolean getVoiceMode() {
        return isVoiceMode;
    }

    public void setImageResource(int resId) {
        btSend.setImageResource(resId);
    }

    protected void onDestroy() {
    }

    public static void showSoftKeyboard(Context context, View view) {
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            view.requestFocus(); // 确保输入框获取焦点
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * 隐藏软键盘（针对当前窗口）
     *
     * @param context 上下文
     * @param view    任意视图（用于获取窗口令牌）
     */
    public static void hideSoftKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0); // 通过窗口令牌隐藏
        }
    }

    public void showPhotoIcon(int visibility) {
        if (btImage != null) {
            btImage.setVisibility(visibility);
        }

    }

    private void setLayoutGravity(View view, int gravity) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.gravity = gravity;  // 如：靠右对齐
        view.setLayoutParams(params);
    }

    private FragmentManager fragmentManager;



    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    private void showInputDialog() {
        InputBottomSheetDialogFragment dialogFragment = new InputBottomSheetDialogFragment();
        dialogFragment.setDefaultText(etUserInput.getText().toString());

        dialogFragment.setOnInputListener(new InputBottomSheetDialogFragment.OnInputListener() {
            @Override
            public void onSend(String text) {
                // 发送按钮回调逻辑，比如回传给外部监听器
                if (!text.isEmpty() && inputListener != null) {
                    inputListener.onSendButtonClicked(text);
                    etUserInput.setText("");
                }
            }

            @Override
            public void onCancel(String text) {
                etUserInput.setText(text);
            }
        });

        dialogFragment.show(fragmentManager, "InputDialogFragment");
    }

    public void setShadow(View view, Context context, float elevationWidth, int elevationColor, boolean hasStroke, int strokeColor, float strokeWidth) {

        float cornerRadius = SizeUtils.dpToPx(16);
        float elevation = elevationWidth > 0 ? elevationWidth : 20;
        float stroke = strokeWidth > 0 ? strokeWidth : 2;

        ShapeAppearanceModel shapeAppearanceModel = new ShapeAppearanceModel.Builder()
                .setAllCorners(CornerFamily.ROUNDED, cornerRadius)
                .build();

        MaterialShapeDrawable materialShapeDrawable = new MaterialShapeDrawable(shapeAppearanceModel);
        materialShapeDrawable.setFillColor(ColorStateList.valueOf(Color.WHITE));

        if (hasStroke) {
            int padding = (int) (stroke / 2);
            materialShapeDrawable.setStroke(stroke, ColorStateList.valueOf(strokeColor));
            materialShapeDrawable.setPadding(padding, padding, padding, padding);
        }

        materialShapeDrawable.setShadowCompatibilityMode(MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS);
        materialShapeDrawable.initializeElevationOverlay(context);
        materialShapeDrawable.setElevation(elevation);
        materialShapeDrawable.setShadowColor(context.getColor(R.color.color_606F8B));

        // 关闭父布局的裁剪，才能看到阴影
        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).setClipChildren(false);
        }

        view.setBackground(materialShapeDrawable);
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (etUserInput != null){
            etUserInput.detach();
        }
    }
}
