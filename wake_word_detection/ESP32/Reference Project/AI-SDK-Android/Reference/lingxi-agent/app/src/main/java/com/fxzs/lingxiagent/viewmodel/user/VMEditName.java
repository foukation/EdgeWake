package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.user.dto.UserDto;
import com.fxzs.lingxiagent.model.user.dto.UserUpdateReqDto;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepositoryImpl;

public class VMEditName extends BaseViewModel {
    
    // 双向绑定字段
    private final ObservableField<String> nickname = new ObservableField<>("");
    private final ObservableField<Boolean> saveEnabled = new ObservableField<>(false);
    
    // 业务状态
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    
    // Repository
    private final UserRepository userRepository;
    
    // 原始昵称
    private String originalNickname = "";
    
    public VMEditName(@NonNull Application application) {
        super(application);
        userRepository = new UserRepositoryImpl();
        
        // 监听昵称变化，更新保存按钮状态
        nickname.observeForever(this::validateNickname);
        
        // 加载当前用户昵称
        loadCurrentNickname();
    }
    
    // Getters
    public ObservableField<String> getNickname() {
        return nickname;
    }
    
    public ObservableField<Boolean> getSaveEnabled() {
        return saveEnabled;
    }
    
    public MutableLiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }
    
    // 业务方法
    public void saveNickname() {
        String newNickname = nickname.get();
        if (newNickname == null || newNickname.trim().isEmpty()) {
            setError("昵称不能为空");
            return;
        }
        
        if (newNickname.equals(originalNickname)) {
            // 昵称未改变，直接返回成功
            saveSuccess.setValue(true);
            return;
        }
        
        setLoading(true);
        
        // 创建用户更新请求对象，只更新昵称
        UserUpdateReqDto userUpdate = new UserUpdateReqDto();
        userUpdate.setNickname(newNickname.trim());
        
        userRepository.updateUserProfile(userUpdate, new UserRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                setLoading(false);
                setSuccess("昵称修改成功");
                saveSuccess.postValue(true);
            }
            
            @Override
            public void onError(String error) {
                setLoading(false);
                setError("修改失败: " + error);
            }
        });
    }
    
    // 私有方法
    private void loadCurrentNickname() {
        setLoading(true);
        
        userRepository.getUserProfile(new UserRepository.Callback<UserDto>() {
            @Override
            public void onSuccess(UserDto user) {
                setLoading(false);
                if (user != null && user.getNickname() != null) {
                    originalNickname = user.getNickname();
                    nickname.set(originalNickname);
                }
            }
            
            @Override
            public void onError(String error) {
                setError("获取用户信息失败: " + error);
            }
        });
    }
    
    private void validateNickname(String value) {
        if (value == null || value.trim().isEmpty()) {
            saveEnabled.set(false);
        } else {
            // 昵称不为空且与原始昵称不同时才启用保存按钮
            saveEnabled.set(!value.equals(originalNickname));
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        nickname.removeObserver(this::validateNickname);
    }
}