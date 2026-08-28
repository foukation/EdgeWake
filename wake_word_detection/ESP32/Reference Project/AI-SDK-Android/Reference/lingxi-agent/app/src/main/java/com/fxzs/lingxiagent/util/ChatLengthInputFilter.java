package com.fxzs.lingxiagent.util;

import android.app.Activity;
import android.text.InputFilter;
import android.text.Spanned;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.GlobalToast;

public class ChatLengthInputFilter implements InputFilter {

    private final int maxLength;
    private final Activity context;
    private boolean hasShownToast = false;

    public ChatLengthInputFilter(Activity context, int maxLength) {
        this.context = context;
        this.maxLength = maxLength;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {

        int keep = maxLength - (dest.length() - (dend - dstart));
        if (keep <= 0) {
            // 已经满了，完全不允许输入
            showToastOnce();
            return "";
        }

        int sourceLen = end - start;

        if (keep >= sourceLen) {
            // 还能完整输入
            resetToast();
            return null;
        }
        return source.subSequence(start, start + keep);
    }

    private void showToastOnce() {
            GlobalToast.show(context, context.getString(R.string.dialog_input_content_hint), GlobalToast.Type.ERROR);
            hasShownToast = true;
    }

    private void resetToast() {
        hasShownToast = false;
    }
}

