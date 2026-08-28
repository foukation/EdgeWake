package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.model.chat.dto.ModelTypeResponse;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VMUserSettingsTest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application mockApplication;
    
    @Mock
    private UserRepository mockUserRepository;
    
    @Mock
    private ChatRepository mockChatRepository;
    
    @Mock
    private Observer<Boolean> booleanObserver;
    
    @Mock
    private Observer<String> stringObserver;
    
    private VMUserSettings viewModel;
    
    @Before
    public void setUp() throws Exception {
        viewModel = new VMUserSettings(mockApplication);
        
        // 使用反射注入mock依赖
        Field userRepoField = VMUserSettings.class.getDeclaredField("userRepository");
        userRepoField.setAccessible(true);
        userRepoField.set(viewModel, mockUserRepository);
        
        Field chatRepoField = VMUserSettings.class.getDeclaredField("chatRepository");
        chatRepoField.setAccessible(true);
        chatRepoField.set(viewModel, mockChatRepository);
    }
    
    @Test
    public void testLoadModelList_Success() {
        // 准备测试数据
        ModelTypeResponse response = new ModelTypeResponse();
        List<ModelTypeResponse.ModelItem> modelList = new ArrayList<>();
        
        ModelTypeResponse.ModelItem model1 = new ModelTypeResponse.ModelItem();
        model1.setModel("deepseek_r1");
        model1.setName("DeepSeek R1");
        modelList.add(model1);
        
        ModelTypeResponse.ModelItem model2 = new ModelTypeResponse.ModelItem();
        model2.setModel("doubao");
        model2.setName("豆包");
        modelList.add(model2);
        

        
        // 捕获回调
        ArgumentCaptor<ChatRepository.Callback<ModelTypeResponse>> callbackCaptor = 
                ArgumentCaptor.forClass(ChatRepository.Callback.class);
        
        // 验证调用了getModelTypeList
        verify(mockChatRepository).getModelTypeList(eq(8), callbackCaptor.capture());
        
        // 模拟成功回调
        callbackCaptor.getValue().onSuccess(response);
        
        // 验证可用模型数组已更新
        String[] availableModels = viewModel.getAvailableModels();
        assertEquals(2, availableModels.length);
        assertEquals("DeepSeek R1", availableModels[0]);
        assertEquals("豆包", availableModels[1]);
    }
    
    @Test
    public void testLoadModelList_Error() {
        // 捕获回调
        ArgumentCaptor<ChatRepository.Callback<ModelTypeResponse>> callbackCaptor = 
                ArgumentCaptor.forClass(ChatRepository.Callback.class);
        
        // 验证调用了getModelTypeList
        verify(mockChatRepository).getModelTypeList(eq(8), callbackCaptor.capture());
        
        // 模拟错误回调
        callbackCaptor.getValue().onError("网络错误");
        
        // 验证使用了默认模型列表
        String[] availableModels = viewModel.getAvailableModels();
        assertEquals(4, availableModels.length);
        assertEquals("DeepSeek R1", availableModels[0]);
        assertEquals("豆包", availableModels[1]);
        assertEquals("联通元景", availableModels[2]);
        assertEquals("腾讯混元", availableModels[3]);
    }
    
    @Test
    public void testShowModelSelector() {
        // 观察showModelDialog
        viewModel.getShowModelDialog().observeForever(booleanObserver);
        
        // 调用方法
        viewModel.showModelSelector();
        
        // 验证值被设置为true
        verify(booleanObserver).onChanged(true);
    }
    
    @Test
    public void testNavigateToSecurity() {
        // 调用方法
        viewModel.navigateToSecurity();
        
        // 验证导航目标
        assertEquals(VMUserSettings.NAV_SECURITY, (int) viewModel.getNavigationTarget().getValue());
    }
}