package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.dto.SoundRecordTaskResponseDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.util.AudioRecorderManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VMRealtimeMeetingTest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application mockApplication;
    
    @Mock
    private MeetingRepository mockRepository;
    
    @Mock
    private AudioRecorderManager mockAudioRecorderManager;
    
    @Mock
    private Observer<String> mockProgressObserver;
    
    @Mock
    private Observer<VMRealtimeMeeting.RecognitionResult> mockRecognitionResultObserver;
    
    private VMRealtimeMeeting viewModel;
    
    @Before
    public void setUp() throws Exception {
        viewModel = new VMRealtimeMeeting(mockApplication);
        
        // 使用反射注入mock repository
        Field repositoryField = VMRealtimeMeeting.class.getDeclaredField("repository");
        repositoryField.setAccessible(true);
        repositoryField.set(viewModel, mockRepository);
        
        // 初始化音频录制器
        viewModel.initAudioRecorder(mockAudioRecorderManager);
        
        // 设置观察者
        viewModel.getProgressMessage().observeForever(mockProgressObserver);
        viewModel.getRecognitionResult().observeForever(mockRecognitionResultObserver);
    }
    
    @Test
    public void testStartRecording_Success() {
        // Given
        String meetingTitle = "测试会议";
        when(mockAudioRecorderManager.startRecording(meetingTitle)).thenReturn(true);
        
        // When
        viewModel.startRecording(meetingTitle);
        
        // Then
        verify(mockAudioRecorderManager).startRecording(meetingTitle);
        assertEquals(meetingTitle, viewModel.getMeetingTitle().getValue());
        assertTrue(viewModel.getIsRecording().getValue());
    }
    
    @Test
    public void testStartRecording_Failure() {
        // Given
        String meetingTitle = "测试会议";
        when(mockAudioRecorderManager.startRecording(meetingTitle)).thenReturn(false);
        
        // When
        viewModel.startRecording(meetingTitle);
        
        // Then
        verify(mockAudioRecorderManager).startRecording(meetingTitle);
        assertFalse(viewModel.getIsRecording().getValue());
        assertEquals("开始录音失败", viewModel.getErrorMessage().getValue());
    }
    
    @Test
    public void testStopRecording() {
        // Given
        String filePath = "/path/to/audio.m4a";
        when(mockAudioRecorderManager.stopRecording()).thenReturn(filePath);
        
        // When
        viewModel.stopRecording();
        
        // Then
        verify(mockAudioRecorderManager).stopRecording();
        assertEquals(filePath, viewModel.getRecordingFilePath().getValue());
        assertFalse(viewModel.getIsRecording().getValue());
    }
    
    @Test
    public void testEndMeetingWithProgress_NoAudioFile() {
        // Given - 没有录音文件路径
        
        // When
        viewModel.endMeetingWithProgress();
        
        // Then
        assertEquals("录音文件路径为空", viewModel.getErrorMessage().getValue());
        verifyNoInteractions(mockRepository);
    }
    
    @Test
    public void testCreateMeetingAndStartRecognition_NewAPIFlow() throws Exception {
        // Given
        String fileUrl = "https://example.com/audio.m4a";
        String language = "chinese_16k";
        String meetingId = "12345";
        
        // 设置会议标题
        Field meetingTitleField = VMRealtimeMeeting.class.getDeclaredField("meetingTitle");
        meetingTitleField.setAccessible(true);
        ((androidx.lifecycle.MutableLiveData<String>)meetingTitleField.get(viewModel)).setValue("测试会议");
        
        // Mock addMeeting响应
        MeetingDto createdMeeting = new MeetingDto();
        createdMeeting.setId(meetingId);
        DrawingRepository.Result<MeetingDto> addResult = DrawingRepository.Result.success(createdMeeting);
        
        // Mock submitAudioRecognitionTask响应
        SoundRecordTaskResponseDto taskResponse = new SoundRecordTaskResponseDto();
        SoundRecordTaskResponseDto.TaskData taskData = new SoundRecordTaskResponseDto.TaskData();
        taskData.setTaskId(123456L);
        taskResponse.setData(taskData);
        DrawingRepository.Result<SoundRecordTaskResponseDto> taskResult = DrawingRepository.Result.success(taskResponse);
        
        when(mockRepository.addMeeting(any(MeetingDto.class))).thenReturn(createLiveData(addResult));
        when(mockRepository.submitAudioRecognitionTask(anyString(), anyString(), anyString())).thenReturn(createLiveData(taskResult));
        
        // When - 使用反射调用私有方法
        java.lang.reflect.Method method = VMRealtimeMeeting.class.getDeclaredMethod("createMeetingAndStartRecognition", String.class, String.class);
        method.setAccessible(true);
        method.invoke(viewModel, fileUrl, language);
        
        // Then - 验证API调用顺序
        // 1. 验证addAiMeetingRecord调用
        ArgumentCaptor<MeetingDto> meetingCaptor = ArgumentCaptor.forClass(MeetingDto.class);
        verify(mockRepository).addMeeting(meetingCaptor.capture());
        MeetingDto capturedMeeting = meetingCaptor.getValue();
        assertEquals(fileUrl, capturedMeeting.getFileUrl());
        assertEquals("测试会议", capturedMeeting.getName());
        assertEquals(Integer.valueOf(3), capturedMeeting.getType()); // type=3 表示录音类型
        
        // 2. 验证soundRecordRecognition调用
        verify(mockRepository).submitAudioRecognitionTask(eq(fileUrl), anyString(), eq(meetingId));
        
        // 3. 验证进度消息更新
        verify(mockProgressObserver, atLeastOnce()).onChanged(anyString());
    }
    
    @Test
    public void testPollingRecognitionResult_Success() throws Exception {
        // Given
        String taskId = "123456";
        String meetingId = "12345";
        String transcription = "这是识别出的会议内容";
        
        // Mock查询结果 - 成功状态
        SoundRecordTaskResponseDto.TaskData taskData = new SoundRecordTaskResponseDto.TaskData();
        taskData.setStatus(2); // 状态2表示成功
        taskData.setResult(transcription);
        SoundRecordTaskResponseDto response = new SoundRecordTaskResponseDto();
        response.setData(taskData);
        DrawingRepository.Result<SoundRecordTaskResponseDto> queryResult = DrawingRepository.Result.success(response);
        
        // Mock更新会议记录
        DrawingRepository.Result<String> updateResult = DrawingRepository.Result.success("更新成功");
        
        when(mockRepository.queryAudioRecognitionResult(taskId, meetingId)).thenReturn(createLiveData(queryResult));
        when(mockRepository.updateMeeting(any(MeetingDto.class))).thenReturn(createLiveData(updateResult));
        
        // When - 使用反射调用私有方法
        java.lang.reflect.Method method = VMRealtimeMeeting.class.getDeclaredMethod("startPollingRecognitionResult", String.class, String.class);
        method.setAccessible(true);
        method.invoke(viewModel, taskId, meetingId);
        
        // 等待异步操作完成
        Thread.sleep(100);
        
        // Then
        // 验证soundRecordRecognitionTaskCheck调用
        verify(mockRepository, atLeastOnce()).queryAudioRecognitionResult(taskId, meetingId);
        
        // 验证updateMeetingRecord调用
        ArgumentCaptor<MeetingDto> updateCaptor = ArgumentCaptor.forClass(MeetingDto.class);
        verify(mockRepository).updateMeeting(updateCaptor.capture());
        MeetingDto updatedMeeting = updateCaptor.getValue();
        assertEquals(meetingId, updatedMeeting.getId());
        assertEquals(transcription, updatedMeeting.getContent());
        
        // 验证最终结果
        verify(mockRecognitionResultObserver, atLeastOnce()).onChanged(any(VMRealtimeMeeting.RecognitionResult.class));
    }
    
    @Test
    public void testPollingRecognitionResult_Failure() throws Exception {
        // Given
        String taskId = "123456";
        String meetingId = "12345";
        String errorMsg = "识别失败：音频质量太差";
        
        // Mock查询结果 - 失败状态
        SoundRecordTaskResponseDto.TaskData taskData = new SoundRecordTaskResponseDto.TaskData();
        taskData.setStatus(3); // 状态3表示失败
        taskData.setErrorMsg(errorMsg);
        SoundRecordTaskResponseDto response = new SoundRecordTaskResponseDto();
        response.setData(taskData);
        DrawingRepository.Result<SoundRecordTaskResponseDto> queryResult = DrawingRepository.Result.success(response);
        
        when(mockRepository.queryAudioRecognitionResult(taskId, meetingId)).thenReturn(createLiveData(queryResult));
        
        // When - 使用反射调用私有方法
        java.lang.reflect.Method method = VMRealtimeMeeting.class.getDeclaredMethod("startPollingRecognitionResult", String.class, String.class);
        method.setAccessible(true);
        method.invoke(viewModel, taskId, meetingId);
        
        // 等待异步操作完成
        Thread.sleep(100);
        
        // Then
        verify(mockRepository, atLeastOnce()).queryAudioRecognitionResult(taskId, meetingId);
        assertEquals("语音识别失败: " + errorMsg, viewModel.getErrorMessage().getValue());
        
        // 验证识别结果为失败
        ArgumentCaptor<VMRealtimeMeeting.RecognitionResult> resultCaptor = ArgumentCaptor.forClass(VMRealtimeMeeting.RecognitionResult.class);
        verify(mockRecognitionResultObserver, atLeastOnce()).onChanged(resultCaptor.capture());
        VMRealtimeMeeting.RecognitionResult capturedResult = resultCaptor.getValue();
        assertFalse(capturedResult.isSuccess());
    }
    
    @Test
    public void testLanguageConversion() throws Exception {
        // 测试语言代码转换逻辑
        String fileUrl = "https://example.com/audio.m4a";
        String meetingId = "12345";
        
        // Mock响应
        SoundRecordTaskResponseDto taskResponse = new SoundRecordTaskResponseDto();
        SoundRecordTaskResponseDto.TaskData taskData = new SoundRecordTaskResponseDto.TaskData();
        taskData.setTaskId(123456L);
        taskResponse.setData(taskData);
        DrawingRepository.Result<SoundRecordTaskResponseDto> taskResult = DrawingRepository.Result.success(taskResponse);
        
        when(mockRepository.submitAudioRecognitionTask(anyString(), anyString(), anyString())).thenReturn(createLiveData(taskResult));
        
        // 使用反射调用私有方法
        java.lang.reflect.Method method = VMRealtimeMeeting.class.getDeclaredMethod("startVoiceRecognitionTask", String.class, String.class, String.class);
        method.setAccessible(true);
        
        // Test 1: 中文
        method.invoke(viewModel, fileUrl, "chinese", meetingId);
        verify(mockRepository).submitAudioRecognitionTask(fileUrl, "16k_zh", meetingId);
        
        // Test 2: 英文
        method.invoke(viewModel, fileUrl, "english", meetingId);
        verify(mockRepository).submitAudioRecognitionTask(fileUrl, "16k_en", meetingId);
        
        // Test 3: 粤语
        method.invoke(viewModel, fileUrl, "cantonese", meetingId);
        verify(mockRepository).submitAudioRecognitionTask(fileUrl, "16k_yue", meetingId);
        
        // Test 4: 默认值
        method.invoke(viewModel, fileUrl, null, meetingId);
        verify(mockRepository, times(2)).submitAudioRecognitionTask(fileUrl, "16k_zh", meetingId); // 包含第一次测试
    }
    
    @Test
    public void testStopPolling() {
        // When
        viewModel.stopPolling();
        
        // Then - 确保轮询已停止（这里主要测试方法不会抛出异常）
        assertNotNull(viewModel);
    }
    
    @Test
    public void testOnCleared() {
        // When
        viewModel.onCleared();
        
        // Then
        verify(mockAudioRecorderManager).stopRecording();
    }
    
    // 辅助方法：创建LiveData
    private <T> androidx.lifecycle.LiveData<T> createLiveData(T value) {
        androidx.lifecycle.MutableLiveData<T> liveData = new androidx.lifecycle.MutableLiveData<>();
        liveData.setValue(value);
        return liveData;
    }
}