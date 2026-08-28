package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.chat.dto.ModelTypeResponse;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepository;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepositoryImpl;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.util.GlobalSettings;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VMModelSelection extends BaseViewModel {
    
    // 业务状态
    private final MutableLiveData<Boolean> selectionSuccess = new MutableLiveData<>();
    private final MutableLiveData<List<ModelTypeResponse.ModelItem>> modelList = new MutableLiveData<>();
    
    // 当前选中的模型代码
    private String currentModelCode;
    
    // 模型选中状态映射
    private final Map<String, Boolean> modelSelectionMap = new HashMap<>();
    
    // Repository
    private final ChatRepository chatRepository;
    
    // 模型列表
    private List<ModelTypeResponse.ModelItem> models = new ArrayList<>();
    
    public VMModelSelection(@NonNull Application application) {
        super(application);
        chatRepository = new ChatRepositoryImpl();
        
        // 加载当前选中的模型
        loadCurrentModel();
        
        // 加载模型列表
        loadModelList();
    }
    
    // Getters
    public MutableLiveData<Boolean> getSelectionSuccess() {
        return selectionSuccess;
    }
    
    public MutableLiveData<List<ModelTypeResponse.ModelItem>> getModelList() {
        return modelList;
    }
    
    public boolean isModelSelected(String modelCode) {
        Boolean selected = modelSelectionMap.get(modelCode);
        return selected != null && selected;
    }
    
    // 业务方法
    public void selectModel(String modelCode, String modelName) {
        if (modelCode == null || modelCode.equals(currentModelCode)) {
            return;
        }
        
        // 保存选中的模型
        GlobalSettings.getInstance().setSelectedModel(getApplication(), modelCode, modelName);
        currentModelCode = modelCode;
        
        // 更新UI状态
        updateSelectionState(modelCode);
        
        // 显示成功提示
        setSuccess("已切换到 " + modelName + " 模型");
        
        // 通知选择成功
        selectionSuccess.postValue(true);
    }
    
    // 私有方法
    private void loadCurrentModel() {
        // 从全局设置获取当前选中的模型代码
        currentModelCode = GlobalSettings.getInstance().getSelectedModelCode();
        if (currentModelCode == null) {
            currentModelCode = SharedPreferencesUtil.getSelectedModel(getApplication());
        }
    }
    
    private void loadModelList() {
        setLoading(true);
        chatRepository.getModelTypeList(8, new ChatRepository.Callback<ModelTypeResponse>() {
            @Override
            public void onSuccess(ModelTypeResponse data) {
                setLoading(false);
                if (data != null && !data.isEmpty()) {
                    models = new ArrayList<>(data);
                    modelList.setValue(models);
                    
                    // 如果当前没有选中的模型，默认选择第一个
                    if (currentModelCode == null || currentModelCode.isEmpty()) {
                        ModelTypeResponse.ModelItem firstModel = models.get(0);
                        currentModelCode = firstModel.getModel();
                        GlobalSettings.getInstance().setSelectedModel(getApplication(), firstModel.getModel(), firstModel.getName());
                    }
                    
                    // 更新选中状态
                    updateSelectionState(currentModelCode);
                } else {
                    setError("获取模型列表为空");
                    // 使用默认模型列表
                    createDefaultModelList();
                }
            }
            
            @Override
            public void onError(String error) {
                setLoading(false);
                setError("获取模型列表失败: " + error);
                
                // 使用默认模型列表
                createDefaultModelList();
            }
        });
    }
    
    private void createDefaultModelList() {
        // 创建默认模型列表
        models = new ArrayList<>();
        
        ModelTypeResponse.ModelItem deepseekR1 = new ModelTypeResponse.ModelItem();
        deepseekR1.setModel("bot-20250715144730-xcr9z");
        deepseekR1.setName("DeepSeek-r1");
        deepseekR1.setModelDesc("深度思考推理");
        models.add(deepseekR1);
        
        ModelTypeResponse.ModelItem doubao = new ModelTypeResponse.ModelItem();
        doubao.setModel("bot-20250715145055-hks84");
        doubao.setName("豆包大模型");
        doubao.setModelDesc("全能思考");
        models.add(doubao);
        
        ModelTypeResponse.ModelItem jiutian = new ModelTypeResponse.ModelItem();
        jiutian.setModel("jiutian-lan");
        jiutian.setName("九天大模型");
        jiutian.setModelDesc("强大的通用能力");
        models.add(jiutian);
        
        ModelTypeResponse.ModelItem tencentHunyuan = new ModelTypeResponse.ModelItem();
        tencentHunyuan.setModel("hunyuan-t1-vision");
        tencentHunyuan.setName("腾讯混元");
        tencentHunyuan.setModelDesc("适合大部分任务");
        models.add(tencentHunyuan);
        
        ModelTypeResponse.ModelItem lingxi = new ModelTypeResponse.ModelItem();
        lingxi.setModel("10086");
        lingxi.setName("灵犀");
        lingxi.setModelDesc("灵犀智能助手");
        models.add(lingxi);
        
        modelList.setValue(models);
        
        // 如果当前没有选中的模型，默认选择第一个
        if (currentModelCode == null || currentModelCode.isEmpty()) {
            currentModelCode = "10086";
            GlobalSettings.getInstance().setSelectedModel(getApplication(), "10086", "灵犀");
        }
        
        // 更新选中状态
        updateSelectionState(currentModelCode);
    }
    
    private void updateSelectionState(String modelCode) {
        // 清空选中状态
        modelSelectionMap.clear();
        
        // 设置当前选中状态
        for (ModelTypeResponse.ModelItem model : models) {
            modelSelectionMap.put(model.getModel(), model.getModel().equals(modelCode));
        }
        
        // 通知UI更新
        modelList.setValue(models);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // 清理资源
    }
}