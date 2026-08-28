package com.fxzs.lingxiagent.viewmodel.drawing;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSampleDto;

import java.util.ArrayList;
import java.util.List;

/**
 * AI绘画样本画廊ViewModel
 */
public class VMDrawingSamples extends BaseViewModel {
    
    // 样本列表
    private final MutableLiveData<List<DrawingSampleDto>> samples = new MutableLiveData<>(new ArrayList<>());
    
    // 当前选中的分类
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("selected");
    
    public VMDrawingSamples(@NonNull Application application) {
        super(application);
        // 初始化时加载精选样本
        loadSamplesByCategory("selected");
    }
    
    // Getter方法
    public MutableLiveData<List<DrawingSampleDto>> getSamples() {
        return samples;
    }
    
    public MutableLiveData<String> getSelectedCategory() {
        return selectedCategory;
    }
    
    /**
     * 根据分类加载样本
     */
    public void loadSamplesByCategory(String category) {
        // TODO: 实际应该从后端获取数据
        // 这里模拟一些数据
        List<DrawingSampleDto> sampleList = new ArrayList<>();
        
        switch (category) {
            case "selected":
                // 精选样本
                sampleList.add(createSample("戴着墨镜的猫咪在游艇上喝橙汁", "精选", "sample_cat1"));
                sampleList.add(createSample("橘猫在上海外滩夜景前自拍", "精选", "sample_cat2"));
                sampleList.add(createSample("日系校园风格的短发女生笑容", "精选", "sample_girl1"));
                sampleList.add(createSample("唯美人像摄影风格的女生侧脸", "精选", "sample_girl2"));
                break;
                
            case "portrait":
                // 人像摄影样本
                sampleList.add(createSample("专业人像摄影", "人像摄影", "sample_portrait1"));
                sampleList.add(createSample("街拍风格人像", "人像摄影", "sample_portrait2"));
                break;
                
            case "art":
                // 艺术样本
                sampleList.add(createSample("油画风格艺术", "艺术", "sample_art1"));
                sampleList.add(createSample("水彩画风格", "艺术", "sample_art2"));
                break;
                
            case "chinese_comic":
                // 国风漫画样本
                sampleList.add(createSample("古风仙侠人物", "国风漫画", "sample_chinese1"));
                sampleList.add(createSample("国风山水背景", "国风漫画", "sample_chinese2"));
                break;
                
            case "anime":
                // 动漫样本
                sampleList.add(createSample("日系动漫风格", "动漫", "sample_anime1"));
                sampleList.add(createSample("二次元萌系角色", "动漫", "sample_anime2"));
                break;
        }
        
        samples.setValue(sampleList);
    }
    
    /**
     * 创建样本对象
     */
    private DrawingSampleDto createSample(String prompt, String style, String imageRes) {
        DrawingSampleDto sample = new DrawingSampleDto();
        sample.setPrompt(prompt);
        sample.setStyle(style);
        sample.setImageResource(imageRes);
        sample.setImageUrl(""); // 实际应该是网络URL
        return sample;
    }
}