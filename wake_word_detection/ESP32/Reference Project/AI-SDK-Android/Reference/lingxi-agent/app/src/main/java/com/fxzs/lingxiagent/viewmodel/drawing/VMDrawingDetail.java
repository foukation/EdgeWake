package com.fxzs.lingxiagent.viewmodel.drawing;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepositoryImpl;

import timber.log.Timber;

/**
 * AI绘画图片详情页ViewModel
 */
public class VMDrawingDetail extends BaseViewModel {
    
    // Repository
    private final DrawingRepository repository = DrawingRepositoryImpl.getInstance();
    
    // 双向绑定字段
    private final ObservableField<String> promptText = new ObservableField<>("");
    private final ObservableField<Boolean> isLoading = new ObservableField<>(false);
    
    // 业务状态
    private final MutableLiveData<DrawingImageDto> imageDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteResult = new MutableLiveData<>();
    
    private String currentImageId;
    
    public VMDrawingDetail(@NonNull Application application) {
        super(application);
        // 观察图片详情变化，更新UI显示
        imageDetail.observeForever(this::updateImageDisplay);
    }
    
    // Getters
    public ObservableField<String> getPromptText() {
        return promptText;
    }
    
    public ObservableField<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public MutableLiveData<DrawingImageDto> getImageDetail() {
        return imageDetail;
    }
    
    public MutableLiveData<Boolean> getDeleteResult() {
        return deleteResult;
    }
    
    /**
     * 通过示例ID加载详情
     */
    public void loadSampleDetail(String sampleId) {
        isLoading.set(true);
        setLoading(true);
        
        try {
            Long id = Long.parseLong(sampleId);
            Timber.tag("VMDrawingDetail").d( "=== 查询图片详情 ===");
            Timber.tag("VMDrawingDetail").d( "Sample ID (String): " + sampleId);
            Timber.tag("VMDrawingDetail").d( "Sample ID (Long): " + id + " <-- 将发送给API");
            
            DrawingImageDto queryDto = new DrawingImageDto();
            queryDto.setId(id);
            
            // 使用volc/query接口查询详情
            repository.getImageDetail(queryDto).observeForever(result -> {
                isLoading.set(false);
                setLoading(false);
                
                if (result != null && result.isSuccess() && result.getData() != null) {
                    DrawingImageDto detail = result.getData();
                    imageDetail.setValue(detail);
                    // 更新提示词显示
                    promptText.set(detail.getPrompt() != null ? detail.getPrompt() : "");
                } else {
                    setError(result != null && result.getError() != null ? result.getError() : "加载图片详情失败");
                }
            });
        } catch (NumberFormatException e) {
            isLoading.set(false);
            setLoading(false);
            setError("无效的图片ID");
        }
    }
    
    
    /**
     * 更新图片显示信息
     */
    private void updateImageDisplay(DrawingImageDto image) {
        if (image == null) return;
        
        // 更新提示词
        promptText.set(image.getPrompt() != null ? image.getPrompt() : "");
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        imageDetail.removeObserver(this::updateImageDisplay);
    }
}