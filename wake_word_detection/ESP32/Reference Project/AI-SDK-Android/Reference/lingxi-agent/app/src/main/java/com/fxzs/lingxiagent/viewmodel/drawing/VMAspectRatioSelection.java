package com.fxzs.lingxiagent.viewmodel.drawing;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.drawing.dto.AspectRatioDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingStyleDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepositoryImpl;

import java.util.ArrayList;
import java.util.List;

public class VMAspectRatioSelection extends BaseViewModel {
    
    private final ObservableField<String> prompt = new ObservableField<>("");
    private final ObservableField<Boolean> generateEnabled = new ObservableField<>(false);
    
    private final MutableLiveData<List<DrawingStyleDto>> styleList = new MutableLiveData<>();
    private final MutableLiveData<List<AspectRatioDto>> ratioList = new MutableLiveData<>();
    private final MutableLiveData<DrawingStyleDto> selectedStyle = new MutableLiveData<>();
    private final MutableLiveData<AspectRatioDto> selectedRatio = new MutableLiveData<>();
    private final MutableLiveData<Boolean> generateResult = new MutableLiveData<>();
    
    private final DrawingRepository repository;
    
    public VMAspectRatioSelection(@NonNull Application application) {
        super(application);
        repository = DrawingRepositoryImpl.getInstance();
        
        prompt.observeForever(this::validateForm);
        
        loadStyles();
        loadRatios();
    }
    
    private void loadStyles() {
        repository.getStyles().observeForever(result -> {
            if (result.isSuccess() && result.getData() != null) {
                List<DrawingStyleDto> styles = result.getData();
                styleList.postValue(styles);
                
                // 设置默认选中第一个
                if (!styles.isEmpty() && selectedStyle.getValue() == null) {
                    setSelectedStyle(styles.get(0));
                }
            }
        });
    }
    
    private void loadRatios() {
        // 创建比例选项
        List<AspectRatioDto> ratios = new ArrayList<>();
        ratios.add(new AspectRatioDto("9:16", "竖屏", 576, 1024, false));
        ratios.add(new AspectRatioDto("16:9", "横屏", 1024, 576, false));
        ratios.add(new AspectRatioDto("4:3", "标准", 768, 576, false));
        ratios.add(new AspectRatioDto("2:3", "竖版", 512, 768, false));
        ratios.add(new AspectRatioDto("1:1", "正方形", 768, 768, true));
        
        ratioList.setValue(ratios);
        
        // 设置默认选中1:1
        for (AspectRatioDto ratio : ratios) {
            if (ratio.getIsDefault()) {
                setSelectedRatio(ratio);
                break;
            }
        }
    }
    
    public void setInitialStyle(String styleName) {
        List<DrawingStyleDto> styles = styleList.getValue();
        if (styles != null) {
            for (DrawingStyleDto style : styles) {
                if (style.getName().equals(styleName)) {
                    setSelectedStyle(style);
                    break;
                }
            }
        }
    }
    
    public void setSelectedStyle(DrawingStyleDto style) {
        selectedStyle.setValue(style);
        validateForm(prompt.get());
    }
    
    public void setSelectedRatio(AspectRatioDto ratio) {
        selectedRatio.setValue(ratio);
        validateForm(prompt.get());
    }
    
    private void validateForm(String promptText) {
        boolean isValid = promptText != null && !promptText.trim().isEmpty()
                && selectedStyle.getValue() != null
                && selectedRatio.getValue() != null;
        generateEnabled.set(isValid);
    }
    

    // Getters
    public ObservableField<String> getPrompt() {
        return prompt;
    }
    
    public ObservableField<Boolean> getGenerateEnabled() {
        return generateEnabled;
    }
    
    public MutableLiveData<List<DrawingStyleDto>> getStyleList() {
        return styleList;
    }
    
    public MutableLiveData<List<AspectRatioDto>> getRatioList() {
        return ratioList;
    }
    
    public MutableLiveData<DrawingStyleDto> getSelectedStyle() {
        return selectedStyle;
    }
    
    public MutableLiveData<AspectRatioDto> getSelectedRatio() {
        return selectedRatio;
    }
    
    public MutableLiveData<Boolean> getGenerateResult() {
        return generateResult;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        prompt.removeObserver(this::validateForm);
    }
}