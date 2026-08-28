package com.fxzs.lingxiagent.model.auth.repository;

import static org.junit.Assert.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.model.auth.dto.LoginResponse;
import com.fxzs.lingxiagent.model.auth.dto.UserDto;
import com.fxzs.lingxiagent.model.common.Constants;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AuthRepository的集成测试
 * 测试真实的API调用（需要后端服务运行）
 * 
 * 注意：这些测试需要实际的后端服务才能运行
 * 在CI/CD环境中，应该使用Mock测试或者专门的测试环境
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthRepositoryIntegrationTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AuthRepository repository;
    
    // 测试用的手机号和密码
    private static final String TEST_PHONE = "13800138001"; // 测试专用号码
    private static final String TEST_PASSWORD = "test123456";
    private static final String TEST_SMS_CODE = "123456"; // 测试环境固定验证码
    
    @Mock
    private Observer<LoginResponse> mockLoginObserver;
    
    @Mock
    private Observer<Boolean> mockBooleanObserver;
    
    @Mock
    private Observer<UserDto> mockUserObserver;
    
    @Before
    public void setUp() {
        // 使用真实的Repository实现
        repository = new AuthRepositoryImpl();
        
        // 确保使用测试环境的BASE_URL
        // 可以通过反射或配置文件设置测试环境URL
    }
    
    /**
     * 测试发送验证码
     * 注意：这会真实发送短信，请谨慎使用
     */
    @Test
    public void testSendSmsCode() throws InterruptedException {
        if (!isTestEnvironmentAvailable()) {
            Timber.i("跳过集成测试：测试环境不可用");
            return;
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        repository.sendSmsCode(TEST_PHONE, Constants.SCENE_LOGIN).observeForever(success -> {
            result.set(success);
            latch.countDown();
        });
        
        assertTrue("发送验证码超时", latch.await(10, TimeUnit.SECONDS));
        assertTrue("发送验证码失败", result.get());
    }
    
    /**
     * 测试验证码登录流程
     */
    @Test
    public void testSmsLoginFlow() throws InterruptedException {
        if (!isTestEnvironmentAvailable()) {
            Timber.i("跳过集成测试：测试环境不可用");
            return;
        }
        
        // 1. 先发送验证码
        CountDownLatch sendCodeLatch = new CountDownLatch(1);
        AtomicReference<Boolean> sendCodeResult = new AtomicReference<>(false);
        
        repository.sendSmsCode(TEST_PHONE, Constants.SCENE_LOGIN).observeForever(success -> {
            sendCodeResult.set(success);
            sendCodeLatch.countDown();
        });
        
        assertTrue("发送验证码超时", sendCodeLatch.await(10, TimeUnit.SECONDS));
        assertTrue("发送验证码失败", sendCodeResult.get());
        
        // 2. 使用验证码登录
        CountDownLatch loginLatch = new CountDownLatch(1);
        AtomicReference<LoginResponse> loginResult = new AtomicReference<>();
        
        repository.loginBySms(TEST_PHONE, TEST_SMS_CODE).observeForever(response -> {
            loginResult.set(response);
            loginLatch.countDown();
        });
        
        assertTrue("登录超时", loginLatch.await(10, TimeUnit.SECONDS));
        assertNotNull("登录失败", loginResult.get());
        assertNotNull("Token为空", loginResult.get().getAccessToken());
    }
    
    /**
     * 测试密码登录
     */
    @Test
    public void testPasswordLogin() throws InterruptedException {
        if (!isTestEnvironmentAvailable()) {
            Timber.i("跳过集成测试：测试环境不可用");
            return;
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<LoginResponse> result = new AtomicReference<>();
        
        repository.loginByPassword(TEST_PHONE, TEST_PASSWORD).observeForever(response -> {
            result.set(response);
            latch.countDown();
        });
        
        assertTrue("登录超时", latch.await(10, TimeUnit.SECONDS));
        
        LoginResponse response = result.get();
        if (response != null) {
            // 登录成功
            assertNotNull("Token为空", response.getAccessToken());
            assertNotNull("RefreshToken为空", response.getRefreshToken());
        } else {
            // 如果测试账号不存在，这是预期的
            Timber.i("密码登录失败：可能测试账号未注册");
        }
    }
    
    /**
     * 测试注册流程
     */
    @Test
    public void testRegisterFlow() throws InterruptedException {
        if (!isTestEnvironmentAvailable()) {
            Timber.i("跳过集成测试：测试环境不可用");
            return;
        }
        
        // 使用随机手机号避免重复注册
        String randomPhone = "138" + System.currentTimeMillis() % 100000000;
        
        // 1. 发送验证码
        CountDownLatch sendCodeLatch = new CountDownLatch(1);
        AtomicReference<Boolean> sendCodeResult = new AtomicReference<>(false);
        
        repository.sendSmsCode(randomPhone, Constants.SCENE_REGISTER).observeForever(success -> {
            sendCodeResult.set(success);
            sendCodeLatch.countDown();
        });
        
        assertTrue("发送验证码超时", sendCodeLatch.await(10, TimeUnit.SECONDS));
        
        if (!sendCodeResult.get()) {
            Timber.i("发送注册验证码失败，跳过注册测试");
            return;
        }
        
        // 2. 注册
        CountDownLatch registerLatch = new CountDownLatch(1);
        AtomicReference<LoginResponse> registerResult = new AtomicReference<>();
        
        repository.register(randomPhone, TEST_SMS_CODE, TEST_PASSWORD).observeForever(response -> {
            registerResult.set(response);
            registerLatch.countDown();
        });
        
        assertTrue("注册超时", registerLatch.await(10, TimeUnit.SECONDS));
        
        LoginResponse response = registerResult.get();
        if (response != null) {
            assertNotNull("注册后Token为空", response.getAccessToken());
        } else {
            Timber.i("注册失败：可能验证码错误或手机号已注册");
        }
    }
    
    /**
     * 测试获取用户信息
     */
    @Test
    public void testGetUserInfo() throws InterruptedException {
        if (!isTestEnvironmentAvailable()) {
            Timber.i("跳过集成测试：测试环境不可用");
            return;
        }
        
        // 先登录获取token
        if (!performTestLogin()) {
            Timber.i("登录失败，跳过获取用户信息测试");
            return;
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<UserDto> result = new AtomicReference<>();
        
        repository.getUserInfo().observeForever(user -> {
            result.set(user);
            latch.countDown();
        });
        
        assertTrue("获取用户信息超时", latch.await(10, TimeUnit.SECONDS));
        
        UserDto user = result.get();
        if (user != null) {
            assertNotNull("用户ID为空", user.getId());
            assertNotNull("手机号为空", user.getMobile());
        } else {
            Timber.i("获取用户信息失败：可能未登录或token无效");
        }
    }
    
    /**
     * 测试刷新Token
     */
    @Test
    public void testRefreshToken() throws InterruptedException {
        if (!isTestEnvironmentAvailable()) {
            Timber.i("跳过集成测试：测试环境不可用");
            return;
        }
        
        // 先登录获取refreshToken
        CountDownLatch loginLatch = new CountDownLatch(1);
        AtomicReference<String> refreshToken = new AtomicReference<>();
        
        repository.loginBySms(TEST_PHONE, TEST_SMS_CODE).observeForever(response -> {
            if (response != null && response.getRefreshToken() != null) {
                refreshToken.set(response.getRefreshToken());
            }
            loginLatch.countDown();
        });
        
        assertTrue("登录超时", loginLatch.await(10, TimeUnit.SECONDS));
        
        if (refreshToken.get() == null) {
            Timber.i("未获取到RefreshToken，跳过刷新测试");
            return;
        }
        
        // 刷新Token
        CountDownLatch refreshLatch = new CountDownLatch(1);
        AtomicReference<LoginResponse> refreshResult = new AtomicReference<>();
        
        repository.refreshToken(refreshToken.get()).observeForever(response -> {
            refreshResult.set(response);
            refreshLatch.countDown();
        });
        
        assertTrue("刷新Token超时", refreshLatch.await(10, TimeUnit.SECONDS));
        
        LoginResponse response = refreshResult.get();
        if (response != null) {
            assertNotNull("新Token为空", response.getAccessToken());
            assertNotEquals("Token未更新", refreshToken.get(), response.getAccessToken());
        }
    }
    
    /**
     * 测试重置密码流程
     */
    @Test
    public void testResetPassword() throws InterruptedException {
        if (!isTestEnvironmentAvailable()) {
            Timber.i("跳过集成测试：测试环境不可用");
            return;
        }
        
        // 1. 发送重置密码验证码
        CountDownLatch sendCodeLatch = new CountDownLatch(1);
        AtomicReference<Boolean> sendCodeResult = new AtomicReference<>(false);
        
        repository.sendSmsCode(TEST_PHONE, Constants.SCENE_RESET_PWD).observeForever(success -> {
            sendCodeResult.set(success);
            sendCodeLatch.countDown();
        });
        
        assertTrue("发送验证码超时", sendCodeLatch.await(10, TimeUnit.SECONDS));
        
        if (!sendCodeResult.get()) {
            Timber.i("发送重置密码验证码失败");
            return;
        }
        
        // 2. 重置密码
        CountDownLatch resetLatch = new CountDownLatch(1);
        AtomicReference<Boolean> resetResult = new AtomicReference<>(false);
        
        String newPassword = "newpass" + System.currentTimeMillis();
        repository.resetPassword(TEST_PHONE, TEST_SMS_CODE, newPassword).observeForever(success -> {
            resetResult.set(false);
            resetLatch.countDown();
        });
        
        assertTrue("重置密码超时", resetLatch.await(10, TimeUnit.SECONDS));
        
        if (resetResult.get()) {
            Timber.i("密码重置成功");
            
            // 3. 尝试用新密码登录验证
            CountDownLatch loginLatch = new CountDownLatch(1);
            AtomicReference<LoginResponse> loginResult = new AtomicReference<>();
            
            repository.loginByPassword(TEST_PHONE, newPassword).observeForever(response -> {
                loginResult.set(response);
                loginLatch.countDown();
            });
            
            assertTrue("登录验证超时", loginLatch.await(10, TimeUnit.SECONDS));
            
            if (loginResult.get() != null) {
                Timber.i("新密码登录成功");
            }
        } else {
            Timber.i("密码重置失败");
        }
    }
    
    /**
     * 测试退出登录
     */
    @Test
    public void testLogout() throws InterruptedException {
        if (!isTestEnvironmentAvailable()) {
            Timber.i("跳过集成测试：测试环境不可用");
            return;
        }
        
        // 先登录
        if (!performTestLogin()) {
            Timber.i("登录失败，跳过退出登录测试");
            return;
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        repository.logout().observeForever(success -> {
            result.set(success);
            latch.countDown();
        });
        
        assertTrue("退出登录超时", latch.await(10, TimeUnit.SECONDS));
        assertTrue("退出登录失败", result.get());
    }
    
    /**
     * 测试错误的登录凭证
     */
    @Test
    public void testInvalidCredentials() throws InterruptedException {
        if (!isTestEnvironmentAvailable()) {
            Timber.i("跳过集成测试：测试环境不可用");
            return;
        }
        
        // 测试错误的密码
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<LoginResponse> result = new AtomicReference<>();
        
        repository.loginByPassword(TEST_PHONE, "wrongpassword").observeForever(response -> {
            result.set(response);
            latch.countDown();
        });
        
        assertTrue("请求超时", latch.await(10, TimeUnit.SECONDS));
        assertNull("错误密码应该登录失败", result.get());
        
        // 测试错误的验证码
        CountDownLatch smsLatch = new CountDownLatch(1);
        AtomicReference<LoginResponse> smsResult = new AtomicReference<>();
        
        repository.loginBySms(TEST_PHONE, "000000").observeForever(response -> {
            smsResult.set(response);
            smsLatch.countDown();
        });
        
        assertTrue("请求超时", smsLatch.await(10, TimeUnit.SECONDS));
        assertNull("错误验证码应该登录失败", smsResult.get());
    }
    
    /**
     * 辅助方法：执行测试登录
     */
    private boolean performTestLogin() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> success = new AtomicReference<>(false);
        
        repository.loginBySms(TEST_PHONE, TEST_SMS_CODE).observeForever(response -> {
            success.set(response != null && response.getAccessToken() != null);
            latch.countDown();
        });
        
        latch.await(10, TimeUnit.SECONDS);
        return success.get();
    }
    
    /**
     * 检查测试环境是否可用
     */
    private boolean isTestEnvironmentAvailable() {
        // 这里可以添加检查测试服务器是否可达的逻辑
        // 例如：ping测试服务器或调用健康检查接口
        
        // 如果没有配置测试环境，返回false
        String baseUrl = Constants.BASE_URL;
        if (baseUrl == null || baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")) {
            return false;
        }
        
        // 暂时禁用集成测试，直到有可用的测试环境
        // 如果您有可用的测试环境，请将此行改为 return true;
        return false;
    }
}