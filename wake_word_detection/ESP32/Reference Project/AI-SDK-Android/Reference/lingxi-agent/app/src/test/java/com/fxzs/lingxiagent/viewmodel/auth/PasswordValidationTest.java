package com.fxzs.lingxiagent.viewmodel.auth;

import android.app.Application;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * 密码校验功能的单元测试
 * 测试新的大小写字母校验要求
 */
@RunWith(MockitoJUnitRunner.class)
public class PasswordValidationTest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application mockApplication;
    
    private VMRegister viewModel;
    
    @Before
    public void setUp() {
        viewModel = new VMRegister(mockApplication);
    }
    
    /**
     * 测试密码校验 - 有效密码
     */
    @Test
    public void testValidPasswords() {
        String[] validPasswords = {
            "Abc123!",     // 最短有效密码：大写+小写+数字+特殊字符
            "Password1@",  // 常见格式
            "MyPass123#",  // 混合格式
            "Test@123A",   // 包含特殊字符
            "AbCdEf123456789012!@"  // 最长有效密码（20位）
        };

        for (String password : validPasswords) {
            viewModel.getPassword().set(password);
            String error = viewModel.getPasswordValidationError();
            assertNull("密码 '" + password + "' 应该是有效的，但返回错误: " + error, error);
        }
    }
    
    /**
     * 测试密码校验 - 无效密码：缺少大写字母
     */
    @Test
    public void testPasswordsWithoutUppercase() {
        String[] invalidPasswords = {
            "abc123!",     // 只有小写+数字+特殊字符
            "password1@",  // 只有小写+数字+特殊字符
            "test@123#",   // 只有小写+数字+特殊字符
        };

        for (String password : invalidPasswords) {
            viewModel.getPassword().set(password);
            String error = viewModel.getPasswordValidationError();
            assertNotNull("密码 '" + password + "' 应该无效（缺少大写字母）", error);
            assertTrue("错误信息应该提到大写字母", error.contains("大写字母"));
        }
    }
    
    /**
     * 测试密码校验 - 无效密码：缺少小写字母
     */
    @Test
    public void testPasswordsWithoutLowercase() {
        String[] invalidPasswords = {
            "ABC123!",     // 只有大写+数字+特殊字符
            "PASSWORD1@",  // 只有大写+数字+特殊字符
            "TEST@123#",   // 只有大写+数字+特殊字符
        };

        for (String password : invalidPasswords) {
            viewModel.getPassword().set(password);
            String error = viewModel.getPasswordValidationError();
            assertNotNull("密码 '" + password + "' 应该无效（缺少小写字母）", error);
            assertTrue("错误信息应该提到小写字母", error.contains("小写字母"));
        }
    }

    /**
     * 测试密码校验 - 无效密码：缺少特殊字符
     */
    @Test
    public void testPasswordsWithoutSpecialChar() {
        String[] invalidPasswords = {
            "Abc123",      // 只有大写+小写+数字
            "Password1",   // 只有大写+小写+数字
            "MyPass123",   // 只有大写+小写+数字
        };

        for (String password : invalidPasswords) {
            viewModel.getPassword().set(password);
            String error = viewModel.getPasswordValidationError();
            assertNotNull("密码 '" + password + "' 应该无效（缺少特殊字符）", error);
            assertTrue("错误信息应该提到特殊字符", error.contains("特殊字符"));
        }
    }

    /**
     * 测试密码校验 - 无效密码：长度不足
     */
    @Test
    public void testPasswordsTooShort() {
        String[] invalidPasswords = {
            "Ab1!",        // 4位
            "Abc1@",       // 5位
        };

        for (String password : invalidPasswords) {
            viewModel.getPassword().set(password);
            String error = viewModel.getPasswordValidationError();
            assertNotNull("密码 '" + password + "' 应该无效（长度不足）", error);
            assertTrue("错误信息应该提到长度", error.contains("长度"));
        }
    }
    
    /**
     * 测试密码校验 - 无效密码：长度过长
     */
    @Test
    public void testPasswordsTooLong() {
        String password = "AbCdEf1234567890123@#$"; // 22位，包含所有必需字符类型
        viewModel.getPassword().set(password);
        String error = viewModel.getPasswordValidationError();
        assertNotNull("密码长度超过20位应该无效", error);
        assertTrue("错误信息应该提到长度", error.contains("长度"));
    }
    
    /**
     * 测试密码校验 - 空密码
     */
    @Test
    public void testEmptyPassword() {
        viewModel.getPassword().set("");
        String error = viewModel.getPasswordValidationError();
        assertNotNull("空密码应该无效", error);
        assertTrue("错误信息应该提示输入密码", error.contains("请输入密码"));
    }
}
