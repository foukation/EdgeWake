package com.fxzs.lingxiagent.viewmodel.chat.service;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MessageRenderServiceTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    @Test
    public void addAIImages_appendsMsg_andRemovesPlaceholder() {
        MessageRenderService.ChatListProvider provider = mock(MessageRenderService.ChatListProvider.class);
        List<ChatMessage> list = new java.util.ArrayList<>();
        // 占位消息，类型用任意已存在常量
        list.add(new ChatMessage("placeholder", ChatAdapter.TYPE_AI));
        when(provider.getMessages()).thenReturn(list);

        MessageRenderService svc = new MessageRenderService(provider);
        ArrayList<String> imgs = new ArrayList<>();
        imgs.add("a"); imgs.add("b");
        svc.addAIImages(imgs);

        verify(provider).postMessages(argThat(arg -> arg.size() == 1 && arg.get(0).getMsgType() == ChatAdapter.TYPE_ASSISTANT_IMG));
    }
}

