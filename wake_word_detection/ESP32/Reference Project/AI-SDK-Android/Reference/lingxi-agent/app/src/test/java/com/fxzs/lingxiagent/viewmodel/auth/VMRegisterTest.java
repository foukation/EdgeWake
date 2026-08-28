package com.fxzs.lingxiagent.viewmodel.auth;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.model.auth.dto.LoginResponse;
import com.fxzs.lingxiagent.model.auth.repository.AuthRepository;
import com.fxzs.lingxiagent.model.common.Constants;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

/**
 * VMRegister的单元测试
 * 测试覆盖所有公开方法和双向绑定逻辑
 */
@RunWith(MockitoJUnitRunner.class)
public class VMRegisterTest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application mockApplication;
    
    @Mock
    private AuthRepository mockAuthRepository;
    
    @Mock
    private Observer<Boolean> mockBooleanObserver;
    
    @Mock
    private Observer<String> mockStringObserver;
    
    private VMRegister viewModel;
    
    @Before
    public void setUp() throws Exception {
        viewModel = new VMRegister(mockApplication);
        
        // 使用反射替换authRepository
        Field authRepositoryField = VMRegister.class.getDeclaredField("authRepository");
        authRepositoryField.setAccessible(true);
        authRepositoryField.set(viewModel, mockAuthRepository);
    }
    
    /**
     * 测试初始状态
     */
    @Test
    public void testInitialState() {
        // Assert initial values
        assertEquals("", viewModel.getPhone().get());
        assertEquals("", viewModel.getVerificationCode().get());
        assertEquals("", viewModel.getPassword().get());
        assertFalse(viewModel.getRegisterEnabled().get());
        assertFalse(viewModel.getAgreementChecked().get());
        assertFalse(viewModel.getPasswordVisible().get());
        assertEquals("获取验证码", viewModel.getCountdownText().get());
        assertTrue(viewModel.getCanGetCode().get());
    }
    
    /**
     * 测试表单验证 - 所有字段有效
     */
    @Test
    public void testFormValidationAllFieldsValid() {
        // Initially register should be disabled
        assertFalse(viewModel.getRegisterEnabled().get());
        
        // Set valid phone
        viewModel.getPhone().set("13800138000");
        assertFalse(viewModel.getRegisterEnabled().get());
        
        // Set verification code
        viewModel.getVerificationCode().set("123456");
        assertFalse(viewModel.getRegisterEnabled().get());
        
        // Set password
        viewModel.getPassword().set("password123");
        assertFalse(viewModel.getRegisterEnabled().get());
        
        // Check agreement
        viewModel.getAgreementChecked().set(true);
        assertTrue(viewModel.getRegisterEnabled().get());
    }
    
    /**
     * 测试手机号验证
     */
    @Test
    public void testPhoneValidation() {
        // Set other fields valid
        viewModel.getVerificationCode().set("123456");
        viewModel.getPassword().set("password123");
        viewModel.getAgreementChecked().set(true);
        
        // Test invalid phone numbers
        String[] invalidPhones = {"", "1234567890", "12345678901", "23800138000", "1380013800a"};
        for (String phone : invalidPhones) {
            viewModel.getPhone().set(phone);
            assertFalse("Phone " + phone + " should be invalid", viewModel.getRegisterEnabled().get());
        }
        
        // Test valid phone numbers
        String[] validPhones = {"13800138000", "15912345678", "18687654321", "19988776655"};
        for (String phone : validPhones) {
            viewModel.getPhone().set(phone);
            assertTrue("Phone " + phone + " should be valid", viewModel.getRegisterEnabled().get());
        }
    }
    
    /**
     * 测试验证码验证
     */
    @Test
    public void testVerificationCodeValidation() {
        // Set other fields valid
        viewModel.getPhone().set("13800138000");
        viewModel.getPassword().set("password123");
        viewModel.getAgreementChecked().set(true);
        
        // Test invalid codes
        String[] invalidCodes = {"", "123"};
        for (String code : invalidCodes) {
            viewModel.getVerificationCode().set(code);
            assertFalse("Code " + code + " should be invalid", viewModel.getRegisterEnabled().get());
        }
        
        // Test valid codes
        String[] validCodes = {"1234", "123456", "999999"};
        for (String code : validCodes) {
            viewModel.getVerificationCode().set(code);
            assertTrue("Code " + code + " should be valid", viewModel.getRegisterEnabled().get());
        }
    }
    
    /**
     * 测试密码验证
     */
    @Test
    public void testPasswordValidation() {
        // Set other fields valid
        viewModel.getPhone().set("13800138000");
        viewModel.getVerificationCode().set("123456");
        viewModel.getAgreementChecked().set(true);
        
        // Test invalid passwords
        String[] invalidPasswords = {"", "12345"};
        for (String password : invalidPasswords) {
            viewModel.getPassword().set(password);
            assertFalse("Password with length " + password.length() + " should be invalid", 
                    viewModel.getRegisterEnabled().get());
        }
        
        // Test valid passwords
        String[] validPasswords = {"123456", "password", "verylongpassword123"};
        for (String password : validPasswords) {
            viewModel.getPassword().set(password);
            assertTrue("Password with length " + password.length() + " should be valid", 
                    viewModel.getRegisterEnabled().get());
        }
    }
    
    /**
     * 测试密码可见性切换
     */
    @Test
    public void testTogglePasswordVisibility() {
        // Initially password should be hidden
        assertFalse(viewModel.getPasswordVisible().get());
        
        // Toggle visibility
        viewModel.togglePasswordVisibility();
        assertTrue(viewModel.getPasswordVisible().get());
        
        // Toggle again
        viewModel.togglePasswordVisibility();
        assertFalse(viewModel.getPasswordVisible().get());
    }
    
    /**
     * 测试注册成功
     */
    @Test
    public void testPerformRegisterSuccess() {
        // Setup
        LoginResponse successResponse = new LoginResponse();
        successResponse.setAccessToken("test_token");
        
        MutableLiveData<LoginResponse> responseLiveData = new MutableLiveData<>();
        when(mockAuthRepository.register(anyString(), anyString(), anyString())).thenReturn(responseLiveData);
        
        // Set valid registration info
        viewModel.getPhone().set("13800138000");
        viewModel.getVerificationCode().set("123456");
        viewModel.getPassword().set("password123");
        viewModel.getAgreementChecked().set(true);
        
        // Observe register result
        viewModel.getRegisterResult().observeForever(mockBooleanObserver);
        
        // Perform register
        viewModel.performRegister();
        
        // Simulate success response
        responseLiveData.setValue(successResponse);
        
        // Verify success
        verify(mockBooleanObserver).onChanged(true);
    }
    
    /**
     * 测试注册失败
     */
    @Test
    public void testPerformRegisterFailure() {
        // Setup
        MutableLiveData<LoginResponse> responseLiveData = new MutableLiveData<>();
        when(mockAuthRepository.register(anyString(), anyString(), anyString())).thenReturn(responseLiveData);
        
        // Set valid registration info
        viewModel.getPhone().set("13800138000");
        viewModel.getVerificationCode().set("123456");
        viewModel.getPassword().set("password123");
        viewModel.getAgreementChecked().set(true);
        
        // Observe register result
        viewModel.getRegisterResult().observeForever(mockBooleanObserver);
        
        // Perform register
        viewModel.performRegister();
        
        // Simulate failure response
        responseLiveData.setValue(null);
        
        // Verify no success callback
        verify(mockBooleanObserver, never()).onChanged(any());
    }
    
    /**
     * 测试注册按钮在未满足条件时不响应
     */
    @Test
    public void testPerformRegisterWhenDisabled() {
        // Register is disabled initially
        assertFalse(viewModel.getRegisterEnabled().get());
        
        // Try to perform register
        viewModel.performRegister();
        
        // Verify no API call was made
        verify(mockAuthRepository, never()).register(anyString(), anyString(), anyString());
    }
    
    /**
     * 测试发送验证码成功
     */
    @Test
    public void testSendVerificationCodeSuccess() throws InterruptedException {
        // Setup
        MutableLiveData<Boolean> responseLiveData = new MutableLiveData<>();
        when(mockAuthRepository.sendSmsCode(anyString(), eq(Constants.SCENE_REGISTER))).thenReturn(responseLiveData);
        
        // Set valid phone
        viewModel.getPhone().set("13800138000");
        
        // Send verification code
        viewModel.sendVerificationCode();
        
        // Simulate success response
        responseLiveData.setValue(true);
        
        // Wait a bit for the countdown to start
        Thread.sleep(100);
        
        // Verify countdown started
        assertFalse(viewModel.getCanGetCode().get());
        assertNotEquals("获取验证码", viewModel.getCountdownText().get());
    }
    
    /**
     * 测试发送验证码失败
     */
    @Test
    public void testSendVerificationCodeFailure() {
        // Setup
        MutableLiveData<Boolean> responseLiveData = new MutableLiveData<>();
        when(mockAuthRepository.sendSmsCode(anyString(), eq(Constants.SCENE_REGISTER))).thenReturn(responseLiveData);
        
        // Set valid phone
        viewModel.getPhone().set("13800138000");
        
        // Send verification code
        viewModel.sendVerificationCode();
        
        // Simulate failure response
        responseLiveData.setValue(false);
        
        // Verify countdown not started
        assertTrue(viewModel.getCanGetCode().get());
        assertEquals("获取验证码", viewModel.getCountdownText().get());
    }
    
    /**
     * 测试发送验证码时手机号无效
     */
    @Test
    public void testSendVerificationCodeInvalidPhone() {
        // Set invalid phone
        viewModel.getPhone().set("12345");
        
        // Try to send verification code
        viewModel.sendVerificationCode();
        
        // Verify no API call was made
        verify(mockAuthRepository, never()).sendSmsCode(anyString(), anyInt());
        
        // Verify countdown not started
        assertTrue(viewModel.getCanGetCode().get());
        assertEquals("获取验证码", viewModel.getCountdownText().get());
    }
    
    /**
     * 测试发送验证码时倒计时未结束
     */
    @Test
    public void testSendVerificationCodeDuringCountdown() {
        // Disable getting code
        viewModel.getCanGetCode().set(false);
        
        // Set valid phone
        viewModel.getPhone().set("13800138000");
        
        // Try to send verification code
        viewModel.sendVerificationCode();
        
        // Verify no API call was made
        verify(mockAuthRepository, never()).sendSmsCode(anyString(), anyInt());
    }
    
    /**
     * 测试协议勾选对表单验证的影响
     */
    @Test
    public void testAgreementCheckboxValidation() {
        // Set all fields valid except agreement
        viewModel.getPhone().set("13800138000");
        viewModel.getVerificationCode().set("123456");
        viewModel.getPassword().set("password123");
        viewModel.getAgreementChecked().set(false);
        
        // Register should be disabled
        assertFalse(viewModel.getRegisterEnabled().get());
        
        // Check agreement
        viewModel.getAgreementChecked().set(true);
        
        // Register should be enabled
        assertTrue(viewModel.getRegisterEnabled().get());
        
        // Uncheck agreement
        viewModel.getAgreementChecked().set(false);
        
        // Register should be disabled again
        assertFalse(viewModel.getRegisterEnabled().get());
    }
    
    /**
     * 测试清理资源
     */
    @Test
    public void testOnCleared() {
        // This test ensures onCleared doesn't throw exceptions
        viewModel.onCleared();
        
        // Try to modify fields after clearing
        viewModel.getPhone().set("13800138000");
        viewModel.getPassword().set("password");
        viewModel.getVerificationCode().set("123456");
        
        // Should not crash
        assertTrue(true);
    }
}