package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository.Result;
import com.fxzs.lingxiagent.model.meeting.dto.LanguageDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import androidx.lifecycle.MutableLiveData;

@RunWith(MockitoJUnitRunner.class)
public class VMMeetingLanguageTest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application mockApplication;
    
    @Mock
    private MeetingRepository mockRepository;
    
    @Mock
    private Observer<List<LanguageDto>> mockLanguageObserver;
    
    private VMMeeting viewModel;
    
    @Before
    public void setUp() throws Exception {
        viewModel = new VMMeeting(mockApplication);
        
        // Inject mock repository using reflection
        Field repositoryField = VMMeeting.class.getDeclaredField("repository");
        repositoryField.setAccessible(true);
        repositoryField.set(viewModel, mockRepository);
    }
    
    @Test
    public void testLoadLanguages_Success() {
        // Prepare test data
        List<LanguageDto> testLanguages = new ArrayList<>();
        
        LanguageDto chinese = new LanguageDto();
        chinese.setCode("16k_zh_large");
        chinese.setName("中文普通话");
        testLanguages.add(chinese);
        
        LanguageDto english = new LanguageDto();
        english.setCode("16k_en");
        english.setName("英语");
        testLanguages.add(english);
        
        // Mock repository response
        MutableLiveData<Result<List<LanguageDto>>> resultLiveData = new MutableLiveData<>();
        when(mockRepository.getRealTimeLanguages()).thenReturn(resultLiveData);
        
        // Observe languages
        viewModel.getRealTimeLanguages().observeForever(mockLanguageObserver);
        
        // Trigger language loading
        viewModel.loadLanguages();
        
        // Simulate successful response
        resultLiveData.setValue(Result.success(testLanguages));
        
        // Verify the languages are set correctly
        verify(mockLanguageObserver).onChanged(testLanguages);
        
        List<LanguageDto> actualLanguages = viewModel.getRealTimeLanguages().get();
        assertNotNull(actualLanguages);
        assertEquals(2, actualLanguages.size());
        assertEquals("16k_zh_large", actualLanguages.get(0).getCode());
        assertEquals("中文普通话", actualLanguages.get(0).getName());
    }
    
    @Test
    public void testLoadLanguages_EmptyList() {
        // Mock repository response with empty list
        MutableLiveData<Result<List<LanguageDto>>> resultLiveData = new MutableLiveData<>();
        when(mockRepository.getRealTimeLanguages()).thenReturn(resultLiveData);
        
        // Observe languages
        viewModel.getRealTimeLanguages().observeForever(mockLanguageObserver);
        
        // Trigger language loading
        viewModel.loadLanguages();
        
        // Simulate empty response
        resultLiveData.setValue(Result.success(new ArrayList<>()));
        
        // Verify empty list is handled
        verify(mockLanguageObserver).onChanged(new ArrayList<>());
        
        List<LanguageDto> actualLanguages = viewModel.getRealTimeLanguages().get();
        assertNotNull(actualLanguages);
        assertTrue(actualLanguages.isEmpty());
    }
    
    @Test
    public void testLoadLanguages_NetworkError() {
        // Mock repository response with error
        MutableLiveData<Result<List<LanguageDto>>> resultLiveData = new MutableLiveData<>();
        when(mockRepository.getRealTimeLanguages()).thenReturn(resultLiveData);
        
        // Observe languages
        viewModel.getRealTimeLanguages().observeForever(mockLanguageObserver);
        
        // Trigger language loading
        viewModel.loadLanguages();
        
        // Simulate error response
        resultLiveData.setValue(Result.error("Network error"));
        
        // Verify empty list is set on error
        verify(mockLanguageObserver).onChanged(new ArrayList<>());
        
        // Verify error message is set
        assertEquals("网络连接失败，无法获取语音识别语言列表", viewModel.getError().getValue());
    }
    
    @Test
    public void testSelectedLanguageDefault() {
        // Test default selected language
        String defaultLanguage = viewModel.getSelectedLanguage().get();
        assertNotNull(defaultLanguage);
        assertEquals("zh-CN", defaultLanguage);
    }
    
    @Test
    public void testSelectedLanguageUpdate() {
        // Test updating selected language
        viewModel.getSelectedLanguage().set("16k_en");
        assertEquals("16k_en", viewModel.getSelectedLanguage().get());
    }
}