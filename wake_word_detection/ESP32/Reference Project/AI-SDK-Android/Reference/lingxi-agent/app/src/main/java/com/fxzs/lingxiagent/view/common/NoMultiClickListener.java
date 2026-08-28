package com.fxzs.lingxiagent.view.common;

import android.os.SystemClock;
import android.view.View;

public abstract class NoMultiClickListener implements View.OnClickListener {
    private static final int MIN_CLICK_DELAY_TIME = 1000;
    private static long lastClickTime;

    public abstract void onNoMultiClick(View v);

    @Override
    public void onClick(View v) {
        long curClickTime = SystemClock.elapsedRealtime();
        if ((curClickTime - lastClickTime) > MIN_CLICK_DELAY_TIME) {
            v.setEnabled(false);
            v.postDelayed(() -> v.setEnabled(true), 1000);
            lastClickTime = curClickTime;
            onNoMultiClick(v);
        }
    }
}
