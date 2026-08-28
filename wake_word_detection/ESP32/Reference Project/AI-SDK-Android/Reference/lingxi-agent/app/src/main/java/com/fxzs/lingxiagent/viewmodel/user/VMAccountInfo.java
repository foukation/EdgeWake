package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.model.auth.repository.AuthRepository;
import com.fxzs.lingxiagent.model.auth.repository.AuthRepositoryImpl;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.network.SignVerifier;
import com.fxzs.lingxiagent.model.user.dto.UserUpdateReqDto;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepositoryImpl;
import com.fxzs.lingxiagent.util.AesUtil;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import java.io.File;

public class VMAccountInfo extends BaseViewModel {
    
    // 双向绑定字段
    private final ObservableField<String> nickname = new ObservableField<>("");
    private final MutableLiveData<String> usernameMulti = new MutableLiveData<>("");
    private final ObservableField<String> phone = new ObservableField<>("");
    private final ObservableField<Boolean> saveEnabled = new ObservableField<>(false);
    
    // 业务状态
    private final MutableLiveData<String> avatarUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> avatarUploadResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutResult = new MutableLiveData<>();
    
    // Repository
    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    // 原始昵称，用于判断是否有修改
    private String originalNickname = "";
    
    public VMAccountInfo(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepositoryImpl();
        userRepository = new UserRepositoryImpl();
        
        // 监听昵称变化
        nickname.observeForever(this::onNicknameChanged);
        
        // 加载用户信息
        loadLocalUserInfo();
    }
    
    // Getters
    public ObservableField<String> getNickname() {
        return nickname;
    }
    public MutableLiveData<String> getUsernameMulti() {
        return usernameMulti;
    }
    
    public ObservableField<String> getPhone() {
        return phone;
    }
    
    public ObservableField<Boolean> getSaveEnabled() {
        return saveEnabled;
    }
    
    public MutableLiveData<String> getAvatarUrl() {
        return avatarUrl;
    }
    
    public MutableLiveData<Boolean> getSaveResult() {
        return saveResult;
    }
    
    public MutableLiveData<Boolean> getAvatarUploadResult() {
        return avatarUploadResult;
    }
    
    public MutableLiveData<Boolean> getLogoutResult() {
        return logoutResult;
    }
    
    public void saveUserInfo() {
        if (!saveEnabled.get()) {
            return;
        }
        
        setLoading(true);

        UserUpdateReqDto userUpdate = new UserUpdateReqDto();
        userUpdate.setNickname(nickname.get());
        
        userRepository.updateUserProfile(userUpdate, new UserRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                setLoading(false);
                setSuccess("保存成功");
                saveResult.postValue(true);
            }
            
            @Override
            public void onError(String error) {
                setLoading(false);
                setError("保存失败: " + error);
                saveResult.postValue(false);
            }
        });
    }

    private void loadLocalUserInfo() {
        String userId = SharedPreferencesUtil.getUserIdStr();
        String nickName = SharedPreferencesUtil.getUserName();
        nickname.postValue(nickName.isEmpty() ? "用户" + userId : nickName);

        String phoneNum = SharedPreferencesUtil.getUserPhone();
        phone.postValue(formatPhone(AesUtil.decrypt(phoneNum, SignVerifier.getClientId())));

        String avatar = SharedPreferencesUtil.getUserAvatar();
        if (!avatar.isEmpty()) {
            avatarUrl.postValue(avatar);
        }
    }

    private void onNicknameChanged(String newNickname) {
        // 判断是否有修改
        boolean hasChanged = newNickname != null && !newNickname.equals(originalNickname) && !newNickname.trim().isEmpty();
        saveEnabled.set(hasChanged);
    }
    
    private String formatPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    // 更新昵称
    public void updateNickname(String newNickname) {
        if (newNickname == null || newNickname.trim().isEmpty()) {
            setError("昵称不能为空");
            return;
        }
        
        setLoading(true);

        UserUpdateReqDto userUpdate = new UserUpdateReqDto();
        userUpdate.setNickname(newNickname);
        
        userRepository.updateUserProfile(userUpdate, new UserRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                setLoading(false);
                nickname.postValue(newNickname);
                usernameMulti.postValue(newNickname);
                originalNickname = newNickname;
                setSuccess("昵称修改成功");
                // 保存用户名称
                SharedPreferencesUtil.saveUserName(newNickname);
            }
            
            @Override
            public void onError(String error) {
                setLoading(false);
                setError("昵称修改失败: " + error);
            }
        });
    }
    
    // 上传头像
    public void uploadAvatar(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            setError("请选择图片");
            return;
        }
        
        File file = new File(imagePath);
        if (!file.exists()) {
            setError("图片文件不存在");
            return;
        }
        
        setLoading(true);
        
        userRepository.uploadAvatar(imagePath, new UserRepository.Callback<String>() {
            @Override
            public void onSuccess(String fileUrl) {
                // 上传成功后，更新用户头像
                updateUserAvatar(fileUrl);
            }
            
            @Override
            public void onError(String error) {
                setError("头像上传失败: " + error);
                avatarUploadResult.postValue(false);
            }
        });
    }
    
    private void updateUserAvatar(String avatarUrl) {
        UserUpdateReqDto userUpdate = new UserUpdateReqDto();
        userUpdate.setAvatar(avatarUrl);
        
        userRepository.updateUserProfile(userUpdate, new UserRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                setLoading(false);
                setSuccess("头像更新成功");
                VMAccountInfo.this.avatarUrl.postValue(avatarUrl);
                
                // 保存头像路径到本地
                SharedPreferencesUtil.saveUserAvatar(avatarUrl);
                
                avatarUploadResult.postValue(true);
            }
            
            @Override
            public void onError(String error) {
                setLoading(false);
                setError("头像更新失败: " + error);
                avatarUploadResult.postValue(false);
            }
        });
    }
    
    public void logout() {
        // 退出登录时不显示头像loading，避免影响头像显示
        // setLoading(true);

        authRepository.logout().observeForever(new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                // setLoading(false);
                if (null != success && success) {
                    setSuccess("退出登录成功");
                    logoutResult.postValue(true);
                } else {
                    setError("退出登录失败");
                    logoutResult.postValue(false);
                }
            }
        });
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        nickname.removeObserver(this::onNicknameChanged);
    }
}