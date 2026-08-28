package com.fxzs.lingxiagent.view.aifile;

import android.graphics.Bitmap;

public class AiFilePageItem {
    public final int pageIndex; // 1-based
    public Bitmap thumbnail;
    public boolean selected;

    public AiFilePageItem(int pageIndex, Bitmap thumbnail, boolean selected) {
        this.pageIndex = pageIndex;
        this.thumbnail = thumbnail;
        this.selected = selected;
    }
}
