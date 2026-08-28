package com.fxzs.lingxiagent.viewmodel.chat.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.fxzs.lingxiagent.model.chat.callback.CreateMyCallback;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Observer;

@RunWith(MockitoJUnitRunner.class)
public class ConversationServiceTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    @Mock Application app;
    @Mock HttpRequest httpRequest;

    private ObservableField<Long> conversationId;
    private ConversationService service;

    @Before
    public void setUp() {
        conversationId = new ObservableField<>(0L);
        service = new ConversationService(app, httpRequest, conversationId);
    }

    @Test
    public void createMy_setsConversationId_andCallback() {
        CreateMyCallback cb = mock(CreateMyCallback.class);

        service.createMy("model-1", "title", cb);

        ArgumentCaptor<Observer<ApiResponse<Integer>>> captor = ArgumentCaptor.forClass((Class)Observer.class);
        verify(httpRequest).createMy(eq("model-1"), eq("title"), captor.capture());

        Observer<ApiResponse<Integer>> observer = captor.getValue();
        ApiResponse<Integer> res = new ApiResponse<>();
        res.setCode(0);
        res.setData(123);

        try (MockedStatic<SharedPreferencesUtil> mocked = mockStatic(SharedPreferencesUtil.class)) {
            mocked.when(() -> SharedPreferencesUtil.saveString(anyString(), anyString())).thenAnswer(inv -> null);
            observer.onNext(res);
        }

        assertEquals(123L, (long)conversationId.get());
        verify(cb).back();
    }
}

