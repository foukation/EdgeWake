package com.fxzs.lingxiagent.view.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.DialogEditCallback;
import com.fxzs.smartassist.util.ZUtil.KeyboardUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2025/7/17 下午4:55
 */
public class InputBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public interface OnInputListener {
        void onSend(String text);
        void onCancel(String text);
    }

    private EditText editText;
    private ImageView ivSend, ivCancel;
    private String defaultText = "";
    private OnInputListener inputListener;
    DialogEditCallback callback;

    public void setDefaultText(String text) {
        this.defaultText = text;
    }

    public void setOnInputListener(OnInputListener listener) {
        this.inputListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);

    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.dialog_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        editText = view.findViewById(R.id.dialog_user_input);
        ivSend = view.findViewById(R.id.iv_send);
        ivCancel = view.findViewById(R.id.dialog_iv_enlarge);

        editText.setText(defaultText);
        editText.setSelection(defaultText.length());

        ivSend.setOnClickListener(v -> {
            String text = editText.getText().toString().trim();
            if (!text.isEmpty() && inputListener != null) {
                inputListener.onSend(text);
            }
            if (callback != null){
                callback.callback(text);
            }
            dismiss();
        });

        ivCancel.setOnClickListener(v -> {
            if (inputListener != null) {
                inputListener.onCancel(editText.getText().toString());
            }
            if (callback != null){
                callback.onCancel(editText.getText().toString());
            }
            dismiss();
        });

        // 自动弹出键盘
        editText.postDelayed(() -> KeyboardUtils.showKeyboard(editText), 100);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        View view = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (view != null) {
            view.setBackgroundResource(android.R.color.transparent);
            view.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
//            view.getLayoutParams().height = getContext().getResources().getDisplayMetrics().heightPixels;

            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(view);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setHideable(true);

            behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        if (inputListener != null) {
                            inputListener.onCancel(editText.getText().toString());
                        }
                        if (callback != null){
                            callback.onCancel(editText.getText().toString());
                        }
                        dismiss();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    // 可选：处理滑动进度
                }
            });
        }
    }

    public void setCallback(DialogEditCallback callback) {
        this.callback = callback;
    }
}

