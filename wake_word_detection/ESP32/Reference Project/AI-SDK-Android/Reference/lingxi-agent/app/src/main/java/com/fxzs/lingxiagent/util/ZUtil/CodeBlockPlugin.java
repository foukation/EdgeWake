package com.fxzs.lingxiagent.util.ZUtil;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import io.noties.markwon.recycler.MarkwonAdapter;

/**
 * 代码块工具类，提供代码块相关的工具方法
 */
public class CodeBlockPlugin {

    /**
     * 创建代码块的MarkwonAdapter.Entry
     */
    public static MarkwonAdapter.Entry createCodeBlockEntry(Context context) {
        return CodeBlockEntry.create(context);
    }

    /**
     * 创建高级表格的MarkwonAdapter.Entry
     */
    public static MarkwonAdapter.Entry createAdvancedTableEntry(Context context, int tableLayoutRes, int tableLayoutId, int cellLayoutRes) {
        return AdvancedTableEntry.create(context, tableLayoutRes, tableLayoutId, cellLayoutRes);
    }

    /**
     * 复制代码到剪贴板
     */
    public static void copyCodeToClipboard(Context context, String codeContent) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Code", codeContent);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "代码已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }
}
