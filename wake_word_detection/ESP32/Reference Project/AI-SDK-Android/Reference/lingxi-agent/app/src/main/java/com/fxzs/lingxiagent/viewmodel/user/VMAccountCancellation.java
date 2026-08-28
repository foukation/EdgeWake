package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.model.auth.repository.AuthRepository;
import com.fxzs.lingxiagent.model.auth.repository.AuthRepositoryImpl;
import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.network.RetrofitClient;
import com.fxzs.lingxiagent.model.network.SignVerifier;
import com.fxzs.lingxiagent.model.user.UserUtil;
import com.fxzs.lingxiagent.model.user.api.UserApiService;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepositoryImpl;
import com.fxzs.lingxiagent.util.AesUtil;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VMAccountCancellation extends BaseViewModel {
    // 双向绑定字段
    private final ObservableField<String> phone = new ObservableField<>("");
    private final ObservableField<Boolean> canGetCode = new ObservableField<>(true);
    private final ObservableField<String> verificationCode = new ObservableField<>("");
    private final ObservableField<String> countdownText = new ObservableField<>("获取验证码");
    private final ObservableField<Boolean> nextEnabled = new ObservableField<>(false);
    private final ObservableField<Boolean> agreementChecked = new ObservableField<>(false);
    // 业务状态
    private final MutableLiveData<Boolean> cancellationResult = new MutableLiveData<>();
    // 倒计时器
    private CountDownTimer countDownTimer;
    private AuthRepository authRepository;

    private UserRepository userRepository;
    private UserApiService userApiService;
    private Application application;
    private String userPhone;
    public VMAccountCancellation(@NonNull Application application) {
        super(application);
        this.application = application;
        this.authRepository = new AuthRepositoryImpl();
        this.userRepository = new UserRepositoryImpl();
        this.userApiService = RetrofitClient.getInstance().createService(UserApiService.class);

        // 设置字段联动逻辑
        verificationCode.observeForever(this::validateForm);
        agreementChecked.observeForever(this::validateForm);
    }

    // Getter方法
    public ObservableField<String> getPhone() {
        return phone;
    }

    public ObservableField<String> getVerificationCode() {
        return verificationCode;
    }

    public ObservableField<Boolean> getCanGetCode() {
        return canGetCode;
    }

    public ObservableField<Boolean> getNextEnabled() {
        return nextEnabled;
    }

    public ObservableField<String> getCountdownText() {
        return countdownText;
    }
    
    public ObservableField<Boolean> getAgreementChecked() {
        return agreementChecked;
    }
    
    public MutableLiveData<Boolean> getCancellationResult() {
        return cancellationResult;
    }

    // 业务方法
    public void loadUserPhone() {
        setLoading(true);
        userPhone = SharedPreferencesUtil.getUserPhone();
        phone.postValue(UserUtil.formatPhone(userPhone));
        setLoading(false);
    }

    /**
     * 发送验证码
     */
    public void sendVerificationCode() {
        // 检查是否在倒计时中，防止重复发送
        if (!canGetCode.get()) {
            return;
        }
        
        setLoading(true);

        String phoneNumber = AesUtil.decrypt(userPhone, SignVerifier.getClientId());
        authRepository.sendSmsCode(phoneNumber, Constants.SCENE_DELETE_USER)
                .observeForever(new Observer<BaseResponse<Boolean>>() {
                    @Override
                    public void onChanged(BaseResponse<Boolean> response) {
                        setLoading(false);

                        if (null != response) {
                            Boolean result = response.getData();
                            if (response.isSuccess() && result != null && result) {
                                // setSuccess("验证码已发送，5分钟内有效");
                                startCountdown();
                            } else {
                                if (response.getCode() == Constants.ERROR_CODE_LOGIN_FAIL_VCODE) {
                                    setError("今日账户验证码发送已达上限");
                                    return;
                                }
                                String msg = response.getMsg();
                                // setError(TextUtils.isEmpty(msg) ? "发送验证码失败，请稍后再试" : msg);
                                // setError("发送验证码失败，请稍后再试");
                                startCountdown();
                            }
                        } else {
                           // setError("网络请求失败，请检查网络连接");
                        }
                    }
                });
    }

    /**
     * 表单验证
     */
    private void validateForm(Object obj) {
        boolean codeValid = verificationCode.isNotEmpty() && verificationCode.get().length() >= Constants.VERIFICATION_CODE_LEN;
        nextEnabled.set(codeValid && agreementChecked.get());
    }

    /**
     * 开始倒计时
     */
    private void startCountdown() {
        canGetCode.set(false);

        countDownTimer = new CountDownTimer(Constants.SMS_COUNTDOWN * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                countdownText.set(seconds + "s后重新获取");
            }

            @Override
            public void onFinish() {
                canGetCode.set(true);
                countdownText.set("获取验证码");
            }
        }.start();
    }

    public void cancelAccount() {
        setLoading(true);

        // 获取用户ID
        long userId = SharedPreferencesUtil.getUserId();
        if (userId <= 0) {
            setError("用户信息不存在");
            setLoading(false);
            return;
        }

        // 调用注销账号接口
        Map<String, Object> params = new HashMap<>();
        params.put("code", AesUtil.encrypt(verificationCode.get(), SignVerifier.getClientId()));
        userApiService.deleteAccount(params).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Boolean> baseResponse = response.body();
                    if (baseResponse.isSuccess() && baseResponse.getData()) {
                        setSuccess("账号注销成功");
                        cancellationResult.postValue(true);
                    } else {
                        setError(baseResponse.getMsg() != null ? baseResponse.getMsg() : "注销失败，请稍后重试");
                    }
                } else {
                    setError("注销失败，请稍后重试");
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                setLoading(false);
                setError("网络请求失败：" + t.getMessage());
            }
        });
    }
    
    public void clearUserData() {
        // 清除所有用户数据
        SharedPreferencesUtil.clearAllData();
        
        // 清除应用缓存
        SharedPreferences sharedPreferences = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        
        // 清除数据库中的用户数据（如果有）
        // userRepository.clearAllUserData();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // 清理观察者
        verificationCode.removeObserver(this::validateForm);
        agreementChecked.removeObserver(null);

        // 取消倒计时
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}