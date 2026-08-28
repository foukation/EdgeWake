package com.fxzs.lingxiagent.model.chat.dto;

import com.fxzs.lingxiagent.R;

public enum ShareItem {
    COPY_LINK(R.mipmap.share_copy_link, "复制链接"),
    WECHAT(R.mipmap.share_wechat, "微信"),
    WECHAT_MOMENT(R.mipmap.share_wechat_moment, "朋友圈"),
    LONG_PIC(R.mipmap.share_long_pic, "生成长图"),
    COPY_TEXT(R.mipmap.share_copy_text, "复制文本"),
    SHARE_COLLECT(R.mipmap.share_collect, "收藏"),
    SHARE_FILE(R.mipmap.share_file, "导出文件"),
    SHARE_DELETE(R.mipmap.share_delete, "删除"),
    SAVE_PIC(R.mipmap.share_save_pic, "存为图片"),
    SHARE_SETTING(R.mipmap.share_setting, "更多");

    private final int resId;
    private final String text;

    ShareItem(int resId, String text) {
        this.resId = resId;
        this.text = text;
    }

    public int getResId() {
        return resId;
    }

    public String getText() {
        return text;
    }
}
