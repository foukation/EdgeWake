package com.fxzs.lingxiagent.view.drawing;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AsrManager;
import com.fxzs.lingxiagent.model.chat.dto.DrawingToChatBean;
import com.fxzs.lingxiagent.model.drawing.dto.AspectRatioDto;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.NetworkUtils;
import com.fxzs.lingxiagent.util.ShadowUtils;
import com.fxzs.lingxiagent.util.ZInputMethod;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.chat.SuperChatContainActivity;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.common.VoiceInputDialog;
import com.fxzs.lingxiagent.view.common.VoiceRecordView;
import com.fxzs.lingxiagent.viewmodel.drawing.VMDrawing;
import com.fxzs.smartassist.util.ZUtil.SizeUtils;

import java.util.List;

import timber.log.Timber;

/**
 * AI绘画参数配置与入口界面
 */
public class DrawingActivity extends BaseActivity<VMDrawing> {


    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_AUDIO_PERMISSION = 1002;
    private static final int REQUEST_CONTINUE_EDIT = 1003;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1004;

    private RecyclerView rvStyles;
    private LinearLayout llRatioButtons;
    private EditText etPrompt;
    private ImageView btnVoiceInput;

    private DrawingStyleAdapter styleAdapter;
    private VoiceInputDialog voiceInputDialog;


    private ImageView ivReferenceImage;
    private String referenceImageUrl;
    private boolean isContinueEditMode = false;  // 是否是继续编辑模式
    private boolean isInitializing = true;  // 是否正在初始化
    private float aspectRatio;
    private CardView cv_reference_image;
    private View rl_top_bar;
    private View ll_input_container;
    private View ll_voice;
    private View iv_keyboard;

    TextView tv_press;
    TextView tv_voice_hint;
    View rl_voice;
    View iv_logo;
    private VoiceRecordView voiceRecordView;
    private boolean isInArea = true;
    boolean isVoice = false;
    private View styleRatio;

    private final int PRESS_DOWN = 1;
    private final int PRESS_UP = 2;
    private final int PRESS_MOVE = 3;

    private static final String TAG = "DrawingActivity";

    private long startTime = 0L;
    private long MIN_DURATION_MS = 1000L;
    private AsrManager asrManager;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_drawing_continue_edit;
    }

    @Override
    protected Class<VMDrawing> getViewModelClass() {
        return VMDrawing.class;
    }

    @Override
    protected void initializeViews() {
        Timber.tag("DrawingActivity").d("initializeViews() started");
        try {
            Intent intent = getIntent();
            if (intent != null) {
                isContinueEditMode = intent.getBooleanExtra("continue_edit", false);
                isVoice = intent.getBooleanExtra("isVoice", false);
            }
            Timber.tag("DrawingActivity").d("isContinueEdit: " + isContinueEditMode);

            // 初始化UI控件
            rvStyles = findViewById(R.id.rv_style_real);
            llRatioButtons = findViewById(R.id.ll_ratio_container_real);
            etPrompt = findViewById(R.id.et_prompt);
            btnVoiceInput = findViewById(R.id.iv_voice_input_or_send);
            ivReferenceImage = findViewById(R.id.iv_reference_image);
            cv_reference_image = findViewById(R.id.cv_reference_image);
            rl_top_bar = findViewById(R.id.rl_top_bar);
            ll_input_container = findViewById(R.id.ll_input_container);
            ll_voice = findViewById(R.id.ll_voice);
            iv_keyboard = findViewById(R.id.iv_keyboard);
            tv_press = findViewById(R.id.tv_press);
            tv_voice_hint = findViewById(R.id.tv_voice_hint);
            rl_voice = findViewById(R.id.rl_voice);
            iv_logo = findViewById(R.id.iv_logo);
            voiceRecordView = findViewById(R.id.voiceRecordView);
            voiceRecordView.setBottomPadding(SizeUtils.dpToPx(24));
            styleRatio = findViewById(R.id.ll_style_ratio_container);
            ShadowUtils.applyDefaultShadow(styleRatio, this);

            // 设置参考图
            setupReferenceImage(intent);

            // 设置返回按钮
            View backBtn = findViewById(R.id.iv_back);
            if (backBtn != null) {
                backBtn.setOnClickListener(v -> finish());
                findViewById(R.id.iv_back2).setOnClickListener(v -> finish());
            }

            // 初始化列表和按钮
            setupStyleRecyclerView();
            setupRatioButtons();

            // 设置输入框
            if (etPrompt != null && !isVoice) {
                etPrompt.postDelayed(() -> {
                    etPrompt.requestFocus();
                    etPrompt.setSelection(etPrompt.getText().length());
                    ZInputMethod.openInputMethod(etPrompt);
                    isInitializing = false;
                }, 500);
            }

            handleIntentData();
            setupInputStateToggle();

            Timber.tag("DrawingActivity").d("initializeViews() completed successfully");
        } catch (Exception e) {
            Timber.tag("DrawingActivity").e("Error in initializeViews()" + e);
            throw e;
        }

        setupKeyboardListener();

        initAsrManger();
    }

    private void setupReferenceImage(Intent intent) {
        if (ivReferenceImage != null && intent != null) {
            Timber.tag("DrawingActivity").d("ivReferenceImage found, setting up image");
            try {
                ivReferenceImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                ivReferenceImage.setAdjustViewBounds(true);
                String referenceImageUrlValue = intent.getStringExtra("reference_image_url");
                Timber.tag("DrawingActivity").d("Reference image URL: " + referenceImageUrlValue);
                if (referenceImageUrlValue != null && !referenceImageUrlValue.isEmpty()) {
                    this.referenceImageUrl = referenceImageUrlValue; // 保存到成员变量
                    Glide.with(this)
                            .asBitmap()
                            .load(referenceImageUrlValue)
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                    int width = resource.getWidth();
                                    int height = resource.getHeight();
                                    aspectRatio = (float) width / height;
                                    Timber.tag("DrawingActivity").d("Glide aspectRatio = " + aspectRatio);
                                    setImagRatio(false);
                                    ivReferenceImage.setImageBitmap(resource);
                                }
                            });
                }
            } catch (Exception e) {
                Timber.tag("DrawingActivity").e("Error setting up reference image" + e);
            }
        } else {
            Timber.tag(TAG).w("ivReferenceImage is null!");
        }
    }


    private void setImagRatio(boolean isSoftwareShow) {
        if (aspectRatio > 1) {//横条型
            ViewGroup.LayoutParams params = cv_reference_image.getLayoutParams();
            if (isSoftwareShow) {
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            }
            cv_reference_image.setLayoutParams(params);
        } else {//竖型
            ViewGroup.LayoutParams params = cv_reference_image.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            cv_reference_image.setLayoutParams(params);
        }
        rl_top_bar.setVisibility(isSoftwareShow ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void setupDataBinding() {
        // 绑定输入框
        DataBindingUtils.bindEditText(etPrompt, viewModel.getPrompt(), this);

        // 添加文本变化监听器，用于动态切换图标
        etPrompt.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateVoiceSendIcon(s.toString().trim());
                if (s.length() > 0) {
                    logCurrentSelections();
                }
            }
        });

        etPrompt.setOnKeyListener((v, keyCode, event) -> {

            if (event.getAction() == KeyEvent.ACTION_DOWN) {

                if (keyCode == KeyEvent.KEYCODE_ENTER
                        || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    String prompt = viewModel.getPrompt().get();
                    if (prompt != null && !prompt.isEmpty()) {
                        sendAgentJump(v);
                    } else {
                        ZInputMethod.hideKeyboard(DrawingActivity.this, v.getWindowToken());
                        // 没有文本输入，执行语音输入功能
                        onVoiceInputClick();
                        isVoice = true;
                        Timber.tag("DrawingActivity").d("No prompt text, showing voice input message");
                        ll_input_container.setVisibility(View.GONE);
                        ll_voice.setVisibility(View.VISIBLE);
                    }
                    return true;
                }

            }

            return false;
        });

        // 初始化图标状态
        updateVoiceSendIcon(viewModel.getPrompt().get() != null ? viewModel.getPrompt().get() : "");
    }


    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            String prompt = intent.getStringExtra("prompt");
            String style = intent.getStringExtra("style");
            String ratio = intent.getStringExtra("ratio");
            String referenceImageUrlFromIntent = intent.getStringExtra("reference_image_url");
            boolean isContinueEdit = intent.getBooleanExtra("continue_edit", false);
            boolean isVoiceIntent = intent.getBooleanExtra("isVoice", false);

            Timber.tag("DrawingActivity").d("handleIntent - referenceImageUrl: " + referenceImageUrlFromIntent);
            Timber.tag("DrawingActivity").d("handleIntent - isContinueEdit: " + isContinueEdit);

            if (isContinueEdit) {
                this.isContinueEditMode = true;
                if (referenceImageUrlFromIntent != null) {
                    this.referenceImageUrl = referenceImageUrlFromIntent;
                    viewModel.setReferenceImageUrl(referenceImageUrlFromIntent);
                }
            }

            if (prompt != null && !prompt.isEmpty() && !isContinueEdit) {
                viewModel.getPrompt().set(prompt);
            }

            if (style != null && !style.isEmpty()) {
                viewModel.setInitialStyle(style);
            }
            if (ratio != null && !ratio.isEmpty()) {
                viewModel.getSelectedRatio().setValue(ratio);
            }
            if (isVoiceIntent) {
                isVoice = isVoiceIntent;
                if (isVoice) {
                    ZInputMethod.hideKeyboard(DrawingActivity.this, btnVoiceInput.getWindowToken());
                    // 没有文本输入，执行语音输入功能
                    onVoiceInputClick();
                    Timber.tag("DrawingActivity").d("No prompt text, showing voice input message");
                    ll_input_container.setVisibility(View.GONE);
                    ll_voice.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void setupObservers() {
        // 观察风格列表
        viewModel.getStyles().observe(this, styles -> {
            if (styleAdapter != null && styles != null) {
                styleAdapter.setStyles(styles);

                String styleId = getIntent().getStringExtra("styleId");
                Timber.tag("DrawingActivity").d("styleId======= " + styleId);
                if (styleId != null) {
                    for (int i = 0; i < styles.size(); i++) {
                        if ((styles.get(i).getId() + "").equals(styleId)) {
                            // 同款：根据示例图片风格ID默认选中并同步到ViewModel
                            if (viewModel != null) {
                                viewModel.setSelectedStyle(styles.get(i));
                            }
                            styleAdapter.setSelectedPosition(i);
                            Timber.tag("DrawingActivity").d("Synced selected style to UI: " +
                                    styles.get(i).getName() + " (ID: " + styles.get(i).getId() + ") at position " + i);
                            rvStyles.scrollToPosition(i);
                            break;
                        }
                    }
                }
                updateInputHint();
            }
        });

        // 观察宽高比列表
        viewModel.getAspectRatios().observe(this, ratios -> {

            String selectedRatio = getIntent().getStringExtra("ratio");
            ZUtils.print("getRatioFromPrompt selectedRatio: " + selectedRatio);
            if (!TextUtils.isEmpty(selectedRatio)) {
                viewModel.getSelectedRatio().set(selectedRatio);
            }
            if (ratios != null) {
                for (int i = 0; i < ratios.size(); i++) {
                    if (ratios.get(i).getRatio().equals(selectedRatio)) {
                        ZUtils.print("selectedRatio: " + selectedRatio);
                        ZUtils.print("setSelectedPosition: " + i);
                        updateRatioButtonStates(selectedRatio);
                        break;
                    }
                }
            }

            updateRatioButtons(ratios);
        });
    }

    // 设置输入状态切换
    private void setupInputStateToggle() {
        btnVoiceInput.setOnClickListener(v -> {
            String prompt = viewModel.getPrompt().get();
            if (prompt != null && !prompt.isEmpty()) {
                sendAgentJump(v);
            } else {
                ZInputMethod.hideKeyboard(DrawingActivity.this, v.getWindowToken());
                // 没有文本输入，执行语音输入功能
                onVoiceInputClick();
                isVoice = true;
                Timber.tag("DrawingActivity").d("No prompt text, showing voice input message");
                ll_input_container.setVisibility(View.GONE);
                ll_voice.setVisibility(View.VISIBLE);
            }
        });
        iv_keyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isVoice = false;
                ll_input_container.setVisibility(View.VISIBLE);
                ll_voice.setVisibility(View.GONE);
                ZInputMethod.openInputMethod(etPrompt);
            }
        });

        ll_voice.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Timber.tag("TouchEvent").d("onLongClick ====== >");
                return isVoice;
            }
        });


        ll_voice.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (!isVoice) {
                    return false;
                }
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (ActivityCompat.checkSelfPermission(DrawingActivity.this, Manifest.permission.RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {
                            AppPermissionRequestManager.requestAudioPermission(DrawingActivity.this, REQUEST_AUDIO_PERMISSION,AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR);
                            return false;
                        }
                        isInArea = true;
                        boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(DrawingActivity.this);
                        if (!isNetworkAvailable) {
                            isInArea = false;
                            GlobalToast.show(DrawingActivity.this, "网络错误，请检查网络连接", GlobalToast.Type.ERROR);
                            return false;
                        }
                        voiceStatusHandle(PRESS_DOWN, false, false);
                        startTime = SystemClock.elapsedRealtime();
                        break;

                    case MotionEvent.ACTION_UP:
                        voiceStatusHandle(PRESS_UP, isInArea, false);
                        long duration = SystemClock.elapsedRealtime() - startTime;
                        if (duration < MIN_DURATION_MS) {
                            GlobalToast.show(DrawingActivity.this, getString(R.string.record_toast), GlobalToast.Type.ERROR);
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float rawX = motionEvent.getRawX();
                        float rawY = motionEvent.getRawY();

                        int[] location = new int[2];
                        ll_voice.getLocationOnScreen(location);
                        int viewLeft = location[0];
                        int viewTop = location[1];
                        int viewRight = viewLeft + ll_voice.getWidth();
                        int viewBottom = viewTop + ll_voice.getHeight();

                        isInArea = !(rawX < viewLeft || rawX > viewRight || rawY < viewTop || rawY > viewBottom);
                        voiceStatusHandle(PRESS_MOVE, false, isInArea);

                        break;

                    case MotionEvent.ACTION_CANCEL:
                        isInArea = false;
                        voiceRecordView.setVisibility(View.GONE);
                        break;
                }
                return false;
            }
        });
    }

    /**
     * 处理首次生成（跳转到对话页面）
     */
    private void handleFirstGeneration(String prompt) {

        Intent intent = getIntent();
        boolean from_chat_send = intent != null && intent.getBooleanExtra("from_chat_send", false);
        handleFirstGeneration(prompt, from_chat_send);
    }

    private void handleFirstGeneration(String prompt, boolean isSetResult) {

        Timber.tag("DrawingActivity").d("=== handleFirstGeneration START ===");

        if (TextUtils.isEmpty(prompt)) {
            Toast.makeText(this, "请输入或说出你想创作的内容", Toast.LENGTH_SHORT).show();
            return;
        }

//        verifyViewModelState();

        Intent intent = new Intent(this, SuperChatContainActivity.class);
        intent.putExtra(Constant.INTENT_TYPE, SuperChatContainActivity.TYPE_DRAWING);


        DrawingToChatBean drawingToChatBean = new DrawingToChatBean();
        drawingToChatBean.setPrompt(prompt);

        if (referenceImageUrl != null && !referenceImageUrl.isEmpty()) {
            drawingToChatBean.setReference_image_url(referenceImageUrl);
        }

        if (viewModel.getSelectedStyle() != null) {
            drawingToChatBean.setStyle(viewModel.getSelectedStyle().getName());
            drawingToChatBean.setStyle_id(String.valueOf(viewModel.getSelectedStyle().getId()));
            Timber.tag("DrawingActivity").d("Passing style: " + viewModel.getSelectedStyle().getName() +
                    " (ID: " + viewModel.getSelectedStyle().getId() + ")");
        }

        String selectedRatio = viewModel.getSelectedRatio().get();
        if (selectedRatio != null && !selectedRatio.isEmpty()) {
            drawingToChatBean.setRatio(selectedRatio);
            Timber.tag("DrawingActivity").d("Passing ratio: " + selectedRatio);
        }

        if (viewModel.getSelectAspectRatios() != null) {
            drawingToChatBean.setAspectRatioDto(viewModel.getSelectAspectRatios());
            Timber.tag("DrawingActivity").d("传比例宽高 Passing ratio: getRatio" + viewModel.getSelectAspectRatios().getRatio());
        }


        Timber.tag("DrawingActivity").d("=== handleFirstGeneration END ====");
        intent.putExtra(Constant.INTENT_DATA, drawingToChatBean);
        intent.putExtra(Constant.INTENT_DATA1, viewModel.getSelectedStyle());
        if (isSetResult) {
            setResult(RESULT_OK, intent);
        } else {
            startActivity(intent);
        }

        finish(); // 跳转后关闭当前页面
    }

    // 根据输入框内容动态切换语音/发送图标
    private void updateVoiceSendIcon(String text) {
        if (btnVoiceInput != null) {
            btnVoiceInput.setSelected(!text.isEmpty());
        }
    }

    // 设置风格选择RecyclerView
    private void setupStyleRecyclerView() {
        styleAdapter = new DrawingStyleAdapter();
        styleAdapter.setOnStyleClickListener((style, position) -> {
            Timber.tag("DrawingActivity").d("=== STYLE CLICK EVENT ====");
            Timber.tag("DrawingActivity").d("Style clicked: " + style.getName() +
                    " (ID: " + style.getId() + ") at position " + position);
            try {
                if (viewModel != null && style != null) {
                    viewModel.setSelectedStyle(style);
                    styleAdapter.setSelectedPosition(position);
                    updateInputHint();
                } else {
                    Timber.tag("DrawingActivity").e("ViewModel or style is null!");
                }
            } catch (Exception e) {
                Timber.tag("DrawingActivity").e("Error setting style: " + e.getMessage(), e);
            }
            Timber.tag("DrawingActivity").d("=== STYLE CLICK END ====");
        });

        rvStyles.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvStyles.setAdapter(styleAdapter);
    }

    // 设置宽高比按钮
    private void setupRatioButtons() {
        int[] buttonIds = {R.id.tv_ratio_9_16_real, R.id.tv_ratio_16_9_real, R.id.tv_ratio_4_3_real, R.id.tv_ratio_2_3_real, R.id.tv_ratio_1_1_real};
        int[] containerIds = {R.id.ll_ratio_9_16_real, R.id.ll_ratio_16_9_real, R.id.ll_ratio_4_3_real, R.id.ll_ratio_2_3_real, R.id.ll_ratio_1_1_real};
        String[] ratios = {"9:16", "16:9", "4:3", "3:4", "1:1"};

        for (int i = 0; i < buttonIds.length; i++) {
            LinearLayout container = findViewById(containerIds[i]);
            String ratio = ratios[i];

            if (container == null) {
                Timber.tag("DrawingActivity").e("Container not found for ratio: " + ratio);
                continue;
            }

            container.setTag(i);
            container.setOnClickListener(v -> {
                Timber.tag("DrawingActivity").d("=== RATIO CLICK EVENT ====");
                int position = (int) v.getTag();
                viewModel.setSelectAspectRatios(position);
                try {
                    if (viewModel != null && viewModel.getSelectedRatio() != null) {
                        viewModel.getSelectedRatio().set(ratio);
                        updateRatioButtonStates(ratio);
                        updateInputHint();
                        logCurrentSelections();
                    } else {
                        Timber.tag("DrawingActivity").e("ViewModel or selectedRatio is null!");
                    }
                } catch (Exception e) {
                    Timber.tag("DrawingActivity").e("Error setting ratio: " + e.getMessage(), e);
                }
                Timber.tag("DrawingActivity").d("=== RATIO CLICK END ====");
            });
        }

        viewModel.getSelectedRatio().set("9:16");
        updateRatioButtonStates("9:16");
        updateInputHint();

        etPrompt.postDelayed(() -> {
            Timber.tag("DrawingActivity").d("=== Initial Setup Complete ====");
            verifyControlsInitialization();
            logCurrentSelections();
        }, 1000);
    }

    private void updateRatioButtons(List<AspectRatioDto> ratios) {
        // 可以根据后端返回的数据动态更新按钮
    }

    private void updateRatioButtonStates(String selectedRatio) {
        int[] buttonIds = {R.id.tv_ratio_9_16_real, R.id.tv_ratio_16_9_real, R.id.tv_ratio_4_3_real, R.id.tv_ratio_2_3_real, R.id.tv_ratio_1_1_real};
        int[] containerIds = {R.id.ll_ratio_9_16_real, R.id.ll_ratio_16_9_real, R.id.ll_ratio_4_3_real, R.id.ll_ratio_2_3_real, R.id.ll_ratio_1_1_real};
        int[] rectIds = {R.id.view_ratio_9_16_real, R.id.view_ratio_16_9_real, R.id.view_ratio_4_3_real, R.id.view_ratio_2_3_real, R.id.view_ratio_1_1_real};
        String[] ratios = {"9:16", "16:9", "4:3", "3:4", "1:1"};

        for (int i = 0; i < buttonIds.length; i++) {
            TextView button = findViewById(buttonIds[i]);
            LinearLayout container = findViewById(containerIds[i]);
            View rect = findViewById(rectIds[i]);
            boolean isSelected = ratios[i].equals(selectedRatio);
            button.setSelected(isSelected);
            container.setSelected(isSelected);
            if (isSelected) {
                ViewCompat.setBackgroundTintList(rect, ColorStateList.valueOf(Color.parseColor("#1C77FF")));
            } else {
                ViewCompat.setBackgroundTintList(rect, ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
            }
        }
    }

    private void updateInputHint() {
        if (etPrompt == null) return;
        // Hint logic can be simplified or removed as it's a dedicated screen now
    }

    private void logCurrentSelections() {
        Timber.tag("DrawingActivity").d("=== Current Selections ====");
        if (viewModel.getSelectedStyle() != null) {
            Timber.tag("DrawingActivity").d("Selected Style: " + viewModel.getSelectedStyle().getName());
        }
        String selectedRatio = viewModel.getSelectedRatio().get();
        if (selectedRatio != null && !selectedRatio.isEmpty()) {
            Timber.tag("DrawingActivity").d("Selected Ratio: " + selectedRatio);
        }
        Timber.tag("DrawingActivity").d("=========================");
    }

    private int calculateHeightFromRatio(String ratioStr, int width) {
        if (ratioStr == null || ratioStr.isEmpty()) return width;
        try {
            String[] parts = ratioStr.split(":");
            if (parts.length == 2) {
                double widthRatio = Double.parseDouble(parts[0]);
                double heightRatio = Double.parseDouble(parts[1]);
                return (int) Math.round(width * heightRatio / widthRatio);
            }
        } catch (NumberFormatException e) {
            Timber.tag("DrawingActivity").e("Error parsing ratio: " + ratioStr, e);
        }
        return width;
    }

//    private void verifyViewModelState() {
//        Timber.tag("DrawingActivity").d( "=== Pre-Generation Verification ====");
//        if (viewModel == null) {
//            Timber.tag("DrawingActivity").e( "ViewModel is null!");
//            return;
//        }
//        Timber.tag("DrawingActivity").d( "ViewModel prompt: " + viewModel.getPrompt().get());
//        Timber.tag("DrawingActivity").d( "===============================");
//    }

    private void verifyControlsInitialization() {
        Timber.tag("DrawingActivity").d("=== Controls Verification ====");
        Timber.tag("DrawingActivity").d("ViewModel: " + (viewModel != null ? "OK" : "NULL"));
        Timber.tag("DrawingActivity").d("StyleAdapter: " + (styleAdapter != null ? "OK" : "NULL"));
        Timber.tag("DrawingActivity").d("RecyclerView: " + (rvStyles != null ? "OK" : "NULL"));
        Timber.tag("DrawingActivity").d("=============================");
    }

    private void onVoiceInputClick() {
        if (!checkAudioPermission()) {
//            startVoiceInput();
//        } else {
            requestAudioPermission();
        }
    }

    private boolean checkAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        AppPermissionRequestManager.requestAudioPermission(this, REQUEST_AUDIO_PERMISSION,AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.tag("DrawingActivity").d("onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (requestCode == REQUEST_CONTINUE_EDIT && resultCode == RESULT_OK && data != null) {
            // This activity is started for result by SuperChatFragment.
            // We just need to pass the result back.
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "存储权限已获取，请重试下载操作", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "语音权限已授权，请再次点击语音按钮", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要录音权限才能使用语音输入", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        isContinueEditMode = intent.getBooleanExtra("continue_edit", false);
        String style = intent.getStringExtra("style");
        String ratio = intent.getStringExtra("ratio");

        if (isContinueEditMode) {
            if (etPrompt != null) {
                etPrompt.setText("");
                etPrompt.setHint("请输入您的修改要求");
            }
            if (style != null && !style.isEmpty() && viewModel != null) {
                viewModel.setInitialStyle(style);
            }
            if (ratio != null && !ratio.isEmpty() && viewModel != null && viewModel.getSelectedRatio() != null) {
                viewModel.getSelectedRatio().set(ratio);
            }
        } else {
            handleIntent();
        }

        String styleId = intent.getStringExtra("styleId");
        int width = intent.getIntExtra("width", 0);
        int height = intent.getIntExtra("height", 0);
        viewModel.setAspectRatio(ratio, width, height);
        if (width != 0 && height != 0) {
            AspectRatioDto aspectRatioDto = new AspectRatioDto();
            aspectRatioDto.setRatio(ratio);
            aspectRatioDto.setWidth(width);
            aspectRatioDto.setHeight(height);
            viewModel.setSelectAspectRatios(aspectRatioDto);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceInputDialog != null && voiceInputDialog.isShowing()) {
            voiceInputDialog.dismiss();
        }
        if (asrManager != null) {
            asrManager.onDestroy();
            asrManager = null;
        }
    }

    private void initAsrManger() {
        asrManager = new AsrManager();
        asrManager.setResultListener(new AsrManager.AsrResultListener() {
            @Override
            public void onFinalResult(String text) {
                handleFirstGeneration(text);
            }

            @Override
            public void onPartialResult(String text) {
            }

            @Override
            public void onError(String errorMsg) {
                Timber.e("ASR error: %s", errorMsg);
                GlobalToast.show(DrawingActivity.this, "未识别到文字", GlobalToast.Type.ERROR);
            }

            @Override
            public void onCloseError(String text) {
                boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(DrawingActivity.this);
                if (!isNetworkAvailable) {
                    GlobalToast.show(DrawingActivity.this, "网络错误，请检查网络连接", GlobalToast.Type.ERROR);
                } else {
                    GlobalToast.show(DrawingActivity.this, "未识别到文字", GlobalToast.Type.ERROR);
                }
            }
        });
    }

    private void voiceStatusHandle(int type, boolean isInArea, boolean status) {
        if (type == PRESS_DOWN) {
            if (voiceRecordView != null && voiceRecordView.startRecording()) {
                voiceRecordView.show();
            }
            TTSManager.Companion.getInstance().stop();
            toggleAsrRecognition();
        } else if (type == PRESS_MOVE) {
            if (voiceRecordView != null) {
                voiceRecordView.switchVoiceStatus(status);
            }
        } else if (type == PRESS_UP) {
            if (voiceRecordView != null) {
                voiceRecordView.stopRecording();
            }
            if (!isInArea) {
                closeAsr();
            } else {
                cancelAsr();
            }
        }
    }

    private void toggleAsrRecognition() {
        if (asrManager == null) {
            Timber.tag(TAG).d("asrManager = %s", asrManager);
            initAsrManger();
        }
        asrManager.toggleRecognition();
    }

    public void closeAsr() {
        if (asrManager == null) {
            Timber.tag(TAG).d("asrManager = %s", asrManager);
            return;
        }
        asrManager.stopRecognition();
    }

    public void cancelAsr() {
        if (asrManager == null) {
            Timber.tag(TAG).d("asrManager = %s", asrManager);
            return;
        }
        asrManager.cancelRecognition();
    }

    private void sendAgentJump(View view) {
        Intent currentIntent = getIntent();
        boolean fromChat = currentIntent != null && currentIntent.getBooleanExtra("from_chat", false);

        if (fromChat) {
            Intent resultIntent = new Intent();
            if (etPrompt != null) {
                resultIntent.putExtra("edit_prompt", etPrompt.getText().toString().trim());
            }
            if (viewModel.getSelectedStyle() != null) {
                resultIntent.putExtra("style", viewModel.getSelectedStyle().getName());
                resultIntent.putExtra("DrawingStyleDto", viewModel.getSelectedStyle());
            }
            if (referenceImageUrl != null && !referenceImageUrl.isEmpty()) {
                resultIntent.putExtra("reference_image_url", referenceImageUrl);
            }

            if (viewModel.getSelectedRatio() != null && viewModel.getSelectedRatio().get() != null) {
                resultIntent.putExtra("ratio", viewModel.getSelectedRatio().get());
            }
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            handleFirstGeneration(etPrompt.getText().toString().trim());
        }
    }

    private void setupKeyboardListener() {
        Timber.tag("DrawingActivity").d("setupKeyboardListener() started");
        try {
            // 获取根视图
            View rootView = findViewById(android.R.id.content);

            if (rootView != null) {
                Timber.tag("DrawingActivity").d("Root view found, adding global layout listener");
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    private int previousHeight = 0;

                    @Override
                    public void onGlobalLayout() {
                        android.graphics.Rect rect = new android.graphics.Rect();
                        rootView.getWindowVisibleDisplayFrame(rect);
                        int currentHeight = rect.height();

                        if (previousHeight == 0) {
                            previousHeight = currentHeight;
                            return;
                        }

                        // 计算高度差
                        int heightDiff = previousHeight - currentHeight;

                        // 如果高度差大于200dp，认为键盘弹起
                        int threshold = (int) (200 * getResources().getDisplayMetrics().density);

                        if (heightDiff > threshold) {
                            // 键盘弹起，调整输入框位置
//                            adjustInputContainerForKeyboard(true, heightDiff);
                            // 计算图片最大可用高度
                            View topBar = findViewById(R.id.rl_top_bar);
                            View styleRatio = findViewById(R.id.ll_style_ratio_container);
                            View input = findViewById(R.id.ll_input_container);
                            int topBarHeight = topBar != null ? topBar.getHeight() : 0;
                            int styleRatioHeight = styleRatio != null ? styleRatio.getHeight() : 0;
                            int inputHeight = input != null ? input.getHeight() : 0;
                            int margin = dpToPx(16); // 适当留白
                            int availableHeight = currentHeight - topBarHeight - styleRatioHeight - inputHeight - margin;
//                        adjustImageCardForKeyboard(true, availableHeight);
                            setImagRatio(true);
                        } else if (heightDiff < -threshold) {
                            // 键盘收起，恢复输入框位置
//                            adjustInputContainerForKeyboard(false, 0);
//                        adjustImageCardForKeyboard(false, 0);
                            setImagRatio(false);
                        }

                        previousHeight = currentHeight;
                    }
                });
            }
        } catch (Exception e) {
            Timber.tag("DrawingActivity").e("Error in setupKeyboardListener()" + e);
        }
    }

//    private void adjustInputContainerForKeyboard(boolean keyboardVisible, int keyboardHeight) {
//        View inputContainer = findViewById(R.id.ll_input_container);
//
//
//        if (inputContainer != null) {
//            ViewGroup.LayoutParams layoutParams = inputContainer.getLayoutParams();
//
//            // 检查布局参数类型
//            if (layoutParams instanceof RelativeLayout.LayoutParams) {
//                RelativeLayout.LayoutParams params =
//                        (RelativeLayout.LayoutParams) layoutParams;
//
//                if (keyboardVisible) {
//                    // 键盘弹起时，设置输入框为自适应高度，最大180dp
//                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
//                    int marginBottom = (int) (20 * getResources().getDisplayMetrics().density);
//                    params.bottomMargin = marginBottom;
//                } else {
//                    // 键盘收起时，恢复固定高度54dp
//                    int fixedHeight = (int) (54 * getResources().getDisplayMetrics().density);
//                    params.height = fixedHeight;
//                    int marginBottom = (int) (20 * getResources().getDisplayMetrics().density);
//                    params.bottomMargin = marginBottom;
//                }
//
//                inputContainer.setLayoutParams(params);
//            }
//        }
//
//
//        // 对于继续编辑页面，还需要调整ScrollView的padding来确保内容可见
//        View referenceImage = findViewById(R.id.cv_reference_image);
//        if (referenceImage != null && keyboardVisible) {
//            // 为引用图片容器添加额外的底部padding，确保图片完全可见
//            int extraPadding = (int) (150 * getResources().getDisplayMetrics().density);
//            referenceImage.setPadding(
//                    referenceImage.getPaddingLeft(),
//                    referenceImage.getPaddingTop(),
//                    referenceImage.getPaddingRight(),
//                    extraPadding
//            );
//        } else if (referenceImage != null && !keyboardVisible) {
//            // 键盘收起时，恢复原始padding
//            int originalPadding = (int) (12 * getResources().getDisplayMetrics().density);
//            referenceImage.setPadding(
//                    originalPadding,
//                    originalPadding,
//                    originalPadding,
//                    originalPadding
//            );
//        }
//    }


    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}