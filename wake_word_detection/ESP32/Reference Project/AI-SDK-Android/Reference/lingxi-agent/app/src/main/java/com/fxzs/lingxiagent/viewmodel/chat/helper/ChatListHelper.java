package com.fxzs.lingxiagent.viewmodel.chat.helper;

import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 轻量级工具：对聊天消息列表进行 copy-on-write 更新，避免并发与引用共享问题。
 */
public final class ChatListHelper {
    private ChatListHelper() {}

    public static List<ChatMessage> copy(List<ChatMessage> current) {
        return current != null ? new ArrayList<>(current) : new ArrayList<>();
    }

    public static List<ChatMessage> add(List<ChatMessage> current, ChatMessage msg) {
        List<ChatMessage> list = copy(current);
        if (msg != null) list.add(msg);
        return list;
    }

    public static List<ChatMessage> removeLast(List<ChatMessage> current, int n) {
        List<ChatMessage> list = copy(current);
        if (n <= 0) return list;
        for (int i = 0; i < n; i++) {
            if (list.isEmpty()) break;
            list.remove(list.size() - 1);
        }
        return list;
    }
}

