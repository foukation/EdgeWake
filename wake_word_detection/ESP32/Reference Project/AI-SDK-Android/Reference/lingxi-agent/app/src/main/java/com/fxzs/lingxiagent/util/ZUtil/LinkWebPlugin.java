package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.MarkwonConfiguration;

/**
 * link插件
 */
public class LinkWebPlugin extends AbstractMarkwonPlugin {

    private final Context context;

    public LinkWebPlugin(Context context) {
        this.context = context;
    }


    @Override
    public void configureConfiguration(@NonNull MarkwonConfiguration.Builder builder) {
        builder.linkResolver((view, link) -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
            }
        });
    }

    /**
     * 创建link插件实例
     *
     * @param context 上下文
     * @return TextAlignmentPlugin实例
     */
    public static LinkWebPlugin create(Context context) {
        return new LinkWebPlugin(context);
    }
}
