package com.fxzs.lingxiagent.view.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.DialogEditCallback;


public class ChatEditDialog extends BaseDialogFragment{

    View rl_view;
    View iv_edit_open;
    View iv_send;
    View iv_edit_close;
    private EditText ed;
    String text;
    DialogEditCallback callback;

    @Override
    public int getLayout() {
        return R.layout.dialog_chat_edit;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 获取 Window
        Window window = getActivity().getWindow();
        if (window != null) {
            // 设置软键盘调整模式为 adjustPan
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        rl_view = view.findViewById(R.id.rl_view);
        ed = view.findViewById(R.id.ed);
        iv_send = view.findViewById(R.id.iv_send);
        iv_edit_close = view.findViewById(R.id.iv_edit_close);

//        rl_view.setVisibility(View.GONE);
        ed = view.findViewById(R.id.ed);
        iv_send = view.findViewById(R.id.iv_send);
        iv_edit_close = view.findViewById(R.id.iv_edit_close);

//        rl_view.setVisibility(View.GONE);
//mContainer.setLayoutParams(new LinearLayout.LayoutParams(
//        LinearLayout.LayoutParams.MATCH_PARENT,
//        LinearLayout.LayoutParams.MATCH_PARENT));
        iv_edit_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        iv_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String result = ed.getEditableText().toString();
                callback.callback(result);
                dismiss();
            }
        });
        iv_edit_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dismiss();

            }
        });

        if(!TextUtils.isEmpty(text)){
            ed.setText(text);
        }

        iv_edit_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        iv_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String result = ed.getEditableText().toString();
                callback.callback(result);
                dismiss();
            }
        });
        iv_edit_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dismiss();

            }
        });

        if(!TextUtils.isEmpty(text)){
            ed.setText(text);
        }


        getData();
    }

    private void getData() {

    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public DialogEditCallback getCallback() {
        return callback;
    }

    public void setCallback(DialogEditCallback callback) {
        this.callback = callback;
    }
}
