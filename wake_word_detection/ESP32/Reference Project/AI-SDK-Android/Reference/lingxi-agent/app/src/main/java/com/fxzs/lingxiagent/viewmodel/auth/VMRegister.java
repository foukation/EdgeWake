package com.fxzs.lingxiagent.viewmodel.auth;

import android.app.Application;
import android.os.CountDownTimer;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.model.auth.dto.LoginMode;
import com.fxzs.lingxiagent.model.auth.dto.LoginResponse;
import com.fxzs.lingxiagent.model.auth.repository.AuthRepository;
import com.fxzs.lingxiagent.model.auth.repository.AuthRepositoryImpl;
import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.user.UserUtil;

public class VMRegister extends BaseViewModel {

    // 双向绑定字段
    private final ObservableField<String> phone = new ObservableField<>("");
    private final ObservableField<String> verificationCode = new ObservableField<>("");
    private final ObservableField<String> password = new ObservableField<>("");
    private final ObservableField<Boolean> registerEnabled = new ObservableField<>(false);
    private final ObservableField<Boolean> agreementChecked = new ObservableField<>(false);
    private final ObservableField<Boolean> passwordVisible = new ObservableField<>(false);
    private final ObservableField<String> countdownText = new ObservableField<>("获取验证码");
    private final ObservableField<String> dialogMessage = new ObservableField<>("");
    private final ObservableField<Boolean> canGetCode = new ObservableField<>(true);
    private final ObservableField<LoginMode> loginMode = new ObservableField<>(LoginMode.OneClickLogin);

    // 业务状态
    private final MutableLiveData<Boolean> registerResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> sendSmsResult = new MutableLiveData<>();
    private final MutableLiveData<LoginResponse> loginBySmsResult = new MutableLiveData<>();

    // 倒计时器
    private CountDownTimer countDownTimer;
    private final AuthRepository authRepository;

    public VMRegister(@NonNull Application application) {
        super(application);
        // 初始化Repository
        authRepository = new AuthRepositoryImpl();

        // 设置字段监听
        phone.observeForever(value -> validateForm());
        verificationCode.observeForever(value -> validateForm());
        password.observeForever(value -> validateForm());
    }

    // Getters
    public ObservableField<String> getPhone() {
        return phone;
    }

    public ObservableField<String> getVerificationCode() {
        return verificationCode;
    }

    public ObservableField<String> getPassword() {
        return password;
    }

    public ObservableField<Boolean> getRegisterEnabled() {
        return registerEnabled;
    }

    public ObservableField<Boolean> getAgreementChecked() {
        return agreementChecked;
    }

    public ObservableField<Boolean> getPasswordVisible() {
        return passwordVisible;
    }

    public ObservableField<String> getCountdownText() {
        return countdownText;
    }

    public ObservableField<String> getDialogMessage() {
        return dialogMessage;
    }

    public ObservableField<Boolean> getCanGetCode() {
        return canGetCode;
    }

    public ObservableField<LoginMode> getLoginMode() {
        return loginMode;
    }

    public MutableLiveData<Boolean> getRegisterResult() {
        return registerResult;
    }

    public MutableLiveData<Boolean> getSendSmsResult() {
        return sendSmsResult;
    }

    public MutableLiveData<LoginResponse> getLoginBySmsResult() {
        return loginBySmsResult;
    }

    // 验证码登录
    public void loginBySms() {
        setLoading(true);
        String phoneNumber = phone.get();
        String code = verificationCode.get();
        // 仅使用验证码登录
        authRepository.loginBySms(phoneNumber, code).observeForever(new Observer<LoginResponse>() {
            @Override
            public void onChanged(LoginResponse response) {
                setLoading(false);
                if (null != response) {
                    if (TextUtils.isEmpty(response.getToken())) {
                        // setError(response.getMessage());
                        setError("登录失败，请检查账号或验证码是否正确");
                    } else {
                        setSuccess("登录成功");
                    }
                    loginBySmsResult.postValue(response);
                }
            }
        });
    }

    // 账号密码登录
    public void loginByPassword() {
        setLoading(true);

        String phoneNumber = phone.get();
        String pwd = password.get();
        authRepository.loginByPassword(phoneNumber, pwd).observeForever(new Observer<BaseResponse<
                LoginResponse>>() {
            @Override
            public void onChanged(BaseResponse<LoginResponse> response) {
                setLoading(false);
                if (response != null) {
                    LoginResponse data = response.getData();
                    if (response.isSuccess() && data != null) {
                        if (TextUtils.isEmpty(data.getAccessToken())){
                            setError("登录失败，请检查账号或密码是否正确");
                            return;
                        }
                        setSuccess("登录成功");
                        registerResult.postValue(true);
                    } else {
                        String message = "登录失败，请检查账号或密码是否正确";
                        if (data != null) {
                            if (response.getCode() == Constants.ERROR_CODE_LOGIN_FAIL_PWD ||
                                    response.getCode() == Constants.ERROR_CODE_LOGIN_FAIL_PWD2) {
                                if (data.getRemainFailCount() <= 2 && data.getRemainFailCount() >= 1) {
                                    message = String.format("账号或密码错误，您还有%s次尝试机会。", data.getRemainFailCount());
                                    dialogMessage.postValue(message);
                                    return;
                                }else if (data.getRemainFailCount() == 0){
                                    message = "账号或密码错误次数已达上限，建议24小时后再尝试 。";
                                    dialogMessage.postValue(message);
                                    return;
                                }
                            } else {
                                message = "账号或密码错误次数已达上限，建议24小时后再尝试 。";
                                dialogMessage.postValue(message);
                                return;
                            }
                        }
                        // setError(response.getMessage());
                        setError(message);
                    }
                } else {
                    setError("登录失败，请检查账号或密码是否正确");
                }
            }
        });
    }

    // 使用验证码+密码进行注册
    public void performRegister() {
        setLoading(true);

        String phoneNumber = phone.get();
        String code = verificationCode.get();
        String pwd = password.get();
        authRepository.register(phoneNumber, code, pwd).observeForever(new Observer<BaseResponse<LoginResponse>>() {
            @Override
            public void onChanged(BaseResponse<LoginResponse> response) {
                setLoading(false);
                if (response != null) {
                    if (response.isSuccess() && response.getData() != null) {
                        setSuccess("注册成功");
                        registerResult.postValue(true);
                    } else {
                        // setError(response.getMessage());
                        setError("注册失败，请检查信息是否正确");
                    }
                } else {
                    setError("注册失败，请检查信息是否正确");
                }
            }
        });
    }

    public void performOneClickLogin(String loginToken) {
        setLoading(true);

        authRepository.oneClickLogin(loginToken).observeForever(new Observer<LoginResponse>() {
            @Override
            public void onChanged(LoginResponse response) {
                setLoading(false);
                if (response != null && !TextUtils.isEmpty(response.getToken())) {
                    setSuccess("登录成功");
                    registerResult.postValue(true);
                } else {
                    if (response != null && response.getCode() == Constants.AUTH_ERROR) {
                        // setError(response.getMessage());
                        setError("一键登录失败，请尝试其他登录方式");
                    } else {
                        setError("一键登录失败，请尝试其他登录方式");
                    }
                }
            }
        });
    }

    public void setLoginMode(LoginMode loginMode) {
        this.loginMode.set(loginMode);
        validateForm();
    }

    public void sendVerificationCode(int scene) {
        if (!canGetCode.get()) {
            return;
        }

        // 验证手机号码
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

        authRepository.sendSmsCode(phoneNumber, scene).observeForever(new Observer<BaseResponse<Boolean>>() {
            @Override
            public void onChanged(BaseResponse<Boolean> response) {
                setLoading(false);

                if (null != response) {
                    Boolean result = response.getData();
                    if (response.isSuccess() && result != null && result) {
                        // setSuccess("验证码已发送，5分钟内有效");
                        sendSmsResult.setValue(true);
                        startCountdown();
                    } else {
                        if (response.getCode() == Constants.ERROR_CODE_LOGIN_FAIL_VCODE) {
                            setError("今日账户验证码发送已达上限");
                            return;
                        }
//                        String msg = response.getMsg();
                        // setError(TextUtils.isEmpty(msg) ? "发送验证码失败，请稍后再试" : msg);
                        // setError("发送验证码失败，请稍后再试");
                        startCountdown();
                    }
                }
            }
        });
    }

    public void togglePasswordVisibility() {
        passwordVisible.set(!passwordVisible.get());
    }

    // 私有方法
    private void validateForm() {
        boolean phoneValid = isPhoneValid();

        if (loginMode.get() == LoginMode.Password) {// 密码登录模式
            boolean passwordValid = isPasswordValid();
            registerEnabled.set(phoneValid && passwordValid);
        } else if (loginMode.get() == LoginMode.VerificationMode) {// 验证码登录模式
            boolean codeValid = verificationCode.isNotEmpty() &&
                    verificationCode.get().length() >= Constants.VERIFICATION_CODE_LEN;
            registerEnabled.set(phoneValid && codeValid);
        } else if (loginMode.get() == LoginMode.Register) {// 注册模式
            boolean codeValid = verificationCode.isNotEmpty() &&
                    verificationCode.get().length() >= Constants.VERIFICATION_CODE_LEN;
            registerEnabled.set(phoneValid && codeValid);
        }
    }

    private boolean isPhoneValid() {
        String phoneNumber = phone.get();
        return phoneNumber != null && phoneNumber.matches("^1[3-9]\\d{9}$");
    }

    private boolean isPasswordValid() {
        String pass = password.get();
        // 密码新校验
        return UserUtil.verifyPassword(pass) == null;
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

    @Override
    protected void onCleared() {
        super.onCleared();
        phone.removeObserver(value -> validateForm());
        verificationCode.removeObserver(value -> validateForm());
        password.removeObserver(value -> validateForm());
        // 取消倒计时
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}