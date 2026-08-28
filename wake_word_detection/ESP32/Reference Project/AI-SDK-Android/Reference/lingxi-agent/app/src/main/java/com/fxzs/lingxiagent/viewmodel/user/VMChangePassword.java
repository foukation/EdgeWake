package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;
import android.os.CountDownTimer;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.model.auth.dto.LoginResponse;
import com.fxzs.lingxiagent.model.auth.repository.AuthRepository;
import com.fxzs.lingxiagent.model.auth.repository.AuthRepositoryImpl;
import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.user.UserUtil;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepositoryImpl;
public class VMChangePassword extends BaseViewModel {
    
    // 双向绑定字段
    private final ObservableField<String> phone = new ObservableField<>("");
    private final ObservableField<String> verificationCode = new ObservableField<>("");
    private final ObservableField<Boolean> oldPasswordVisible = new ObservableField<>(false);
    private final ObservableField<String> newPassword = new ObservableField<>("");
    private final ObservableField<Boolean> newPasswordVisible = new ObservableField<>(false);
    private final ObservableField<String> confirmPassword = new ObservableField<>("");
    private final ObservableField<Boolean> confirmPasswordVisible = new ObservableField<>(false);
    private final ObservableField<Boolean> confirmEnabled = new ObservableField<>(false);
    private final ObservableField<Boolean> canGetCode = new ObservableField<>(true);
    private final ObservableField<String> countdownText = new ObservableField<>("获取验证码");
    
    // 业务状态
    private final MutableLiveData<Boolean> changeSuccess = new MutableLiveData<>();
    
    // Repository
    private final UserRepository userRepository;
    private final AuthRepository authRepository;

    public VMChangePassword(@NonNull Application application) {
        super(application);
        userRepository = new UserRepositoryImpl();
        authRepository = new AuthRepositoryImpl();

        // 表单联动：手机号与验证码变化后，更新确认按钮状态
        phone.observeForever(this::validateForm);
        verificationCode.observeForever(this::validateForm);
    }
    
    // Getters
    public ObservableField<String> getPhone() {
        return phone;
    }

    public ObservableField<String> getVerificationCode() {
        return verificationCode;
    }
    public ObservableField<Boolean> getOldPasswordVisible() {
        return oldPasswordVisible;
    }
    
    public ObservableField<String> getNewPassword() {
        return newPassword;
    }

    public ObservableField<Boolean> getNewPasswordVisible() {
        return newPasswordVisible;
    }
    
    public ObservableField<String> getConfirmPassword() {
        return confirmPassword;
    }

    public ObservableField<Boolean> getConfirmPasswordVisible() {
        return confirmPasswordVisible;
    }
    
    public ObservableField<Boolean> getConfirmEnabled() {
        return confirmEnabled;
    }
    
    public MutableLiveData<Boolean> getChangeSuccess() {
        return changeSuccess;
    }

    public ObservableField<Boolean> getCanGetCode() { return canGetCode; }

    public ObservableField<String> getCountdownText() { return countdownText; }

    /**
     * 切换旧密码可见性
     */
    public void toggleOldPasswordVisibility() {
        oldPasswordVisible.set(!oldPasswordVisible.get());
    }

    /**
     * 切换新密码可见性
     */
    public void toggleNewPasswordVisibility() {
        newPasswordVisible.set(!newPasswordVisible.get());
    }

    /**
     * 切换确认密码可见性
     */
    public void toggleConfirmPasswordVisibility() {
        confirmPasswordVisible.set(!confirmPasswordVisible.get());
    }

    // 业务方法（已改为通过短信验证码重置密码）
    public void changePassword() {
        performPasswordReset();
    }
    
    // 私有方法
    private void validateForm(String value) {
        boolean phoneValid = phone.isNotEmpty() && phone.get().length() == 11;
        boolean codeValid = verificationCode.isNotEmpty() && verificationCode.get().length() >= Constants.VERIFICATION_CODE_LEN;
        confirmEnabled.set(phoneValid && codeValid);
    }
    
    private boolean validatePassword(String oldPwd, String newPwd, String confirmPwd) {
        if (oldPwd == null || oldPwd.isEmpty()) {
            setError("请输入原密码");
            return false;
        }

        // 密码新校验
        String passwordError = UserUtil.verifyPassword(newPwd);
        if (passwordError != null) {
            setError(passwordError);
            return false;
        }

        if (!newPwd.equals(confirmPwd)) {
            setError("两次输入的密码不一致");
            return false;
        }

        if (oldPwd.equals(newPwd)) {
            setError("新密码不能与原密码相同");
            return false;
        }

        return true;
    }
    
    private CountDownTimer countDownTimer;

    @Override
    protected void onCleared() {
        super.onCleared();
        phone.removeObserver(this::validateForm);
        verificationCode.removeObserver(this::validateForm);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
    /**
     * 发送验证码
     */
    public void sendVerificationCode() {
        String phoneNumber = phone.get();
        /* if (TextUtils.isEmpty(phoneNumber)) {
            setError("请输入手机号");
            return;
        }
        if (!UserUtil.isValidMobile(phoneNumber)) {
            setError("请输入正确的手机号");
            return;
        } */

        // 验证手机号码
        if (TextUtils.isEmpty(phoneNumber) || !UserUtil.isValidMobile(phoneNumber)) {
            setError("请输入有效的手机号");
            return;
        }

        setLoading(true);
        authRepository.sendSmsCode(phoneNumber, Constants.SCENE_RESET_PWD)
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
                        }
                    }
                });
    }

    /**
     * 执行密码重置
     */
    public void performPasswordReset() {
        String phoneNumber = phone.get();
        String code = verificationCode.get();
        String password = newPassword.get();
        String confirmPwd = confirmPassword.get();

        // 密码校验
        String passwordError = UserUtil.verifyPassword(password);
        if (passwordError != null) {
            setError(passwordError);
            return;
        }

        if (!password.equals(confirmPwd)) {
            setError("两次输入的密码不一致");
            return;
        }

        setLoading(true);
        authRepository.resetPassword(phoneNumber, code, password).observeForever(new Observer<BaseResponse<LoginResponse>>() {
            @Override
            public void onChanged(BaseResponse<LoginResponse> resp) {
                setLoading(false);
                if (resp != null) {
                    if ( resp.getCode() == 0 && resp.getData() != null) {
                        setSuccess("密码重置成功");
                        changeSuccess.postValue(true);
                    } else {
                        // setError(TextUtils.isEmpty(resp.getMsg()) ? "密码重置失败" : resp.getMsg());
                        setError("密码重置失败");
                    }
                } else {
                    setError("密码重置失败");
                }
            }
        });
    }

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

}