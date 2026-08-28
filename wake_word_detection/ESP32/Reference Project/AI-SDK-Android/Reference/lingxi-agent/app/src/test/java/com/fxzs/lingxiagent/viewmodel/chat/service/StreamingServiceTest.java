package com.fxzs.lingxiagent.viewmodel.chat.service;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.fxzs.lingxiagent.model.chat.callback.SSECallback;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class StreamingServiceTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    @Mock HttpRequest httpRequest;

    private StreamingService service;

    @Before
    public void setUp() {
        service = new StreamingService(httpRequest);
    }

    @Test
    public void startStandardStream_receivesReasonAndContent_andEnd() {
        StreamingService.Callback cb = mock(StreamingService.Callback.class);
        service.startStandardStream(1L, 2L, "hi", Collections.emptyList(), false, cb);

        ArgumentCaptor<SSECallback> captor = ArgumentCaptor.forClass(SSECallback.class);
        verify(httpRequest).sendStreams(eq(1L), eq(2L), eq("hi"), eq(false), eq(Collections.emptyList()), eq("disabled"), captor.capture(), isNull());

        SSECallback sse = captor.getValue();
        // assistant-reason
        String reasonJson = "{\"code\":0,\"data\":{\"receive\":{\"type\":\"assistant-reason\",\"content\":\"think\"}}}";
        sse.receive(reasonJson);
        verify(cb).onReceive("think", true, null, null);

        // assistant content
        String contentJson = "{\"code\":0,\"data\":{\"receive\":{\"type\":\"assistant\",\"content\":\"hello\"}}}";
        sse.receive(contentJson);
        verify(cb).onReceive("hello", false, null, null);

        sse.end();
        verify(cb).onEnd();
    }
}

