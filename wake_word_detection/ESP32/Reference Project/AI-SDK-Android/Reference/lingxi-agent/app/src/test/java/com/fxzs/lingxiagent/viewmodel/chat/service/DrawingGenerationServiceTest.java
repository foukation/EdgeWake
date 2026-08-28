package com.fxzs.lingxiagent.viewmodel.chat.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.drawing.api.GenerateImageRequest;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;

import org.junit.Rule;
import org.junit.Test;

public class DrawingGenerationServiceTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    @Test
    public void startGeneration_buildsRequest_andReturnsTaskId() {
        DrawingRepository repo = mock(DrawingRepository.class);
        MutableLiveData<DrawingRepository.Result<DrawingImageDto>> live = new MutableLiveData<>();
        // when(repo.generateImage(any(GenerateImageRequest.class))).thenReturn(live);

        DrawingGenerationService service = new DrawingGenerationService(repo);
        DrawingGenerationService.Params p = new DrawingGenerationService.Params();
        p.finalPrompt = "p"; p.userPrompt = "u"; p.referenceImageUrl = "http://img"; p.width = 100; p.height = 200;

        DrawingGenerationService.Callback cb = mock(DrawingGenerationService.Callback.class);
        service.startGeneration(p, cb);

        DrawingImageDto dto = new DrawingImageDto();
        dto.setId(99L);
        live.postValue(DrawingRepository.Result.success(dto));

        verify(cb).onTaskIdReceived(99L);
        verify(cb).onStart(any(DrawingImageDto.class), eq(0), anyString());
    }
}

