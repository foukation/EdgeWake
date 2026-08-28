package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.fxzs.lingxiagent.model.chat.callback.DialogEditCallback;
import com.fxzs.lingxiagent.view.common.InputBottomSheetDialogFragment;
import com.fxzs.lingxiagent.view.dialog.ChatEditDialog;

public class DialogUtils {
    public static void showChatEditDialog(Context context, String text, DialogEditCallback callback){
        FragmentTransaction ft = ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction();
        ChatEditDialog dialogFragment = new ChatEditDialog();
        dialogFragment.setText(text);
        dialogFragment.setCallback(callback);
        dialogFragment.show(ft,"dialog");
    }

    public static void showInputDialog(Context context, String text, DialogEditCallback callback) {
        FragmentTransaction ft = ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction();
        InputBottomSheetDialogFragment dialogFragment = new InputBottomSheetDialogFragment();
        dialogFragment.setDefaultText(text);
        dialogFragment.setCallback(callback);
        dialogFragment.show(ft, "InputDialogFragment");
    }
}
