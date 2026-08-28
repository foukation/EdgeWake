package com.fxzs.lingxiagent.view.common;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZInputMethod;

import timber.log.Timber;

public class EditInfoDialog extends Dialog {

    private TextView tvTitle;
    private EditText etInput;
    private TextView tvCancel;
    private TextView tvConfirm;
    
    private OnEditInfoDialogListener listener;
    private int maxLength = 100;
    Context context;

    public interface OnEditInfoDialogListener {
        void onConfirm(String inputText);
        void onCancel();
    }
    
    public EditInfoDialog(@NonNull Context context) {
        super(context, R.style.CustomDialog);
        this.context = context;
        initView();
    }
    
    private void initView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_info, null);
        setContentView(view);
        
        tvTitle = findViewById(R.id.tv_title);
        etInput = findViewById(R.id.et_input);
        tvCancel = findViewById(R.id.tv_cancel);
        tvConfirm = findViewById(R.id.tv_confirm);
        
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        
        setupListeners();
    }
    
    private void setupListeners() {
        tvCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel();
            }

            ZInputMethod.closeInputMethod(getContext(), etInput);
            dismiss();
        });
        
        tvConfirm.setOnClickListener(v -> {
            String inputText = etInput.getText().toString().trim();
            
            // 验证输入不能为空
            if (TextUtils.isEmpty(inputText)) {
                GlobalToast.show((Activity) context, "标题不能为空", GlobalToast.Type.ERROR);
                return;
            }
            
            if (listener != null) {
                inputText = inputText.replace("\n"," ");
                Timber.tag("EditText").d( "处理后: " + inputText);
//                etInput.setText(input.replace("\n","oo"));
                listener.onConfirm(inputText);
            }

            ZInputMethod.closeInputMethod(getContext(), etInput);

            dismiss();
        });
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String input = editable.toString();
                // 比如，实时显示输入内容
                Timber.tag("EditText").d( "当前输入: " + input);
                if(input.length() >= maxLength){
                    GlobalToast.show((Activity) context, "超出限定长度", GlobalToast.Type.NORMAL);
                    return;
                }
            }
        });

        if (etInput != null) {
            etInput.setOnKeyListener((v, keyCode, event) -> {

                if (event.getAction() == KeyEvent.ACTION_DOWN) {

                    if (keyCode == KeyEvent.KEYCODE_ENTER
                            || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {

                        String inputText = etInput.getText().toString().trim();

                        // 验证输入不能为空
                        if (TextUtils.isEmpty(inputText)) {
                            GlobalToast.show((Activity) context, "标题不能为空", GlobalToast.Type.ERROR);
                            return true;
                        }

                        if (listener != null) {
                            inputText = inputText.replace("\n"," ");
                            Timber.tag("EditText").d( "处理后: " + inputText);
//                etInput.setText(input.replace("\n","oo"));
                            listener.onConfirm(inputText);
                        }

                        ZInputMethod.closeInputMethod(getContext(), etInput);

                        dismiss();
                        return true;
                    }

                }

                return false;
            });
        }
    }
    
    public EditInfoDialog setTitle(String title) {
        if (tvTitle != null && !TextUtils.isEmpty(title)) {
            tvTitle.setText(title);
        }
        return this;
    }
    
    public EditInfoDialog setHint(String hint) {
        if (etInput != null && !TextUtils.isEmpty(hint)) {
            etInput.setHint(hint);
        }
        return this;
    }
    
    public EditInfoDialog setText(String text) {
        if (etInput != null && !TextUtils.isEmpty(text)) {
            etInput.setText(text);
            etInput.setSelection(text.length());
        }
        return this;
    }
    
    public EditInfoDialog setCancelText(String cancelText) {
        if (tvCancel != null && !TextUtils.isEmpty(cancelText)) {
            tvCancel.setText(cancelText);
        }
        return this;
    }
    
    public EditInfoDialog setConfirmText(String confirmText) {
        if (tvConfirm != null && !TextUtils.isEmpty(confirmText)) {
            tvConfirm.setText(confirmText);
        }
        return this;
    }

    public EditInfoDialog setConfirmTextColor(int color) {
        if (tvConfirm != null) {
            tvConfirm.setTextColor(color);
        }
        return this;
    }

    public EditInfoDialog setInputType(int inputType) {
        if (etInput != null) {
            etInput.setInputType(inputType);
        }
        return this;
    }
    
    public EditInfoDialog setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        if (etInput != null && maxLength > 0) {
            etInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(maxLength)});
        }
        return this;
    }
    
    public EditInfoDialog setOnEditInfoDialogListener(OnEditInfoDialogListener listener) {
        this.listener = listener;
        return this;
    }
    
    public String getInputText() {
        if (etInput != null) {
            return etInput.getText().toString().trim();
        }
        return "";
    }
    
    @Override
    public void show() {
        try {
            super.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void dismiss() {
        try {
            if (isShowing()) {
                super.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}