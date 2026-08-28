package com.fxzs.lingxiagent.model.auth.api;

import static org.junit.Assert.*;

import com.fxzs.lingxiagent.model.auth.dto.LoginRequest;
import com.fxzs.lingxiagent.model.auth.dto.LoginResponse;
import com.fxzs.lingxiagent.model.auth.dto.RegisterRequest;
import com.fxzs.lingxiagent.model.auth.dto.SendSmsRequest;
import com.fxzs.lingxiagent.model.auth.dto.SmsLoginRequest;
import com.fxzs.lingxiagent.model.auth.dto.UserDto;
import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.user.dto.ResetPasswordReqDto;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * AuthApiService的API测试
 * 使用MockWebServer模拟真实的HTTP响应
 */
public class AuthApiServiceTest {
    
    private MockWebServer mockWebServer;
    private AuthApiService apiService;
    private Gson gson;
    
    @Before
    public void setUp() throws IOException {
        // 初始化MockWebServer
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        // 初始化Gson
        gson = new Gson();
        
        // 创建Retrofit实例，指向MockWebServer
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.SECONDS)
                .build();
                
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
                
        // 创建API服务
        apiService = retrofit.create(AuthApiService.class);
    }
    
    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    /**
     * 测试密码登录接口
     */
    @Test
    public void testLoginByPassword() throws Exception {
        // 准备响应数据
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken("test_access_token");
        loginResponse.setRefreshToken("test_refresh_token");
        loginResponse.setUserId(12345L);
        
        BaseResponse<LoginResponse> baseResponse = new BaseResponse<>();
        baseResponse.setCode(0);
        baseResponse.setData(loginResponse);
        baseResponse.setMessage("登录成功");
        
        // 设置MockWebServer响应
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(baseResponse)));
        
        // 执行请求
        LoginRequest request = LoginRequest.passwordLogin("13800138000", "password123");
        Call<BaseResponse<LoginResponse>> call = apiService.loginByPassword(request);
        Response<BaseResponse<LoginResponse>> response = call.execute();
        
        // 验证响应
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(0, response.body().getCode());
        assertNotNull(response.body().getData());
        assertEquals("test_access_token", response.body().getData().getAccessToken());
        
        // 验证请求
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/app-api/member/auth/login", recordedRequest.getPath());
        
        // 验证请求体
        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("13800138000"));
        assertTrue(requestBody.contains("password123"));
    }
    
    /**
     * 测试短信验证码登录接口
     */
    @Test
    public void testLoginBySms() throws Exception {
        // 准备响应数据
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken("sms_access_token");
        loginResponse.setRefreshToken("sms_refresh_token");
        
        BaseResponse<LoginResponse> baseResponse = new BaseResponse<>();
        baseResponse.setCode(0);
        baseResponse.setData(loginResponse);
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(baseResponse)));
        
        // 执行请求
        SmsLoginRequest request = new SmsLoginRequest("13800138000", "123456");
        Call<BaseResponse<LoginResponse>> call = apiService.loginBySms(request);
        Response<BaseResponse<LoginResponse>> response = call.execute();
        
        // 验证响应
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(0, response.body().getCode());
        assertEquals("sms_access_token", response.body().getData().getAccessToken());
        
        // 验证请求
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/app-api/member/auth/sms-login", recordedRequest.getPath());
    }
    
    /**
     * 测试发送验证码接口
     */
    @Test
    public void testSendSmsCode() throws Exception {
        // 准备响应
        BaseResponse<Boolean> baseResponse = new BaseResponse<>();
        baseResponse.setCode(0);
        baseResponse.setData(true);
        baseResponse.setMessage("验证码发送成功");
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(baseResponse)));
        
        // 执行请求
        SendSmsRequest request = new SendSmsRequest("13800138000", 1);
        Call<BaseResponse<Boolean>> call = apiService.sendSmsCode(request);
        Response<BaseResponse<Boolean>> response = call.execute();
        
        // 验证响应
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(0, response.body().getCode());
        assertTrue(response.body().getData());
        
        // 验证请求
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/app-api/member/auth/send-sms-code", recordedRequest.getPath());
    }
    
    /**
     * 测试注册接口
     */
    @Test
    public void testRegister() throws Exception {
        // 准备响应
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken("register_token");
        loginResponse.setRefreshToken("register_refresh_token");
        
        UserDto userDto = new UserDto();
        userDto.setId(12345L);
        userDto.setMobile("13800138000");
        userDto.setNickname("新用户");
        loginResponse.setUser(userDto);
        
        BaseResponse<LoginResponse> baseResponse = new BaseResponse<>();
        baseResponse.setCode(0);
        baseResponse.setData(loginResponse);
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(baseResponse)));
        
        // 执行请求
        RegisterRequest request = new RegisterRequest("13800138000", "123456", "password123");
        Call<BaseResponse<LoginResponse>> call = apiService.register(request);
        Response<BaseResponse<LoginResponse>> response = call.execute();
        
        // 验证响应
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(0, response.body().getCode());
        assertNotNull(response.body().getData().getUser());
        assertEquals("新用户", response.body().getData().getUser().getNickname());
        
        // 验证请求
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/app-api/member/auth/register", recordedRequest.getPath());
    }
    
    /**
     * 测试刷新Token接口
     */
    @Test
    public void testRefreshToken() throws Exception {
        // 准备响应
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken("new_access_token");
        loginResponse.setRefreshToken("new_refresh_token");
        
        BaseResponse<LoginResponse> baseResponse = new BaseResponse<>();
        baseResponse.setCode(0);
        baseResponse.setData(loginResponse);
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(baseResponse)));
        
        // 执行请求
        Call<BaseResponse<LoginResponse>> call = apiService.refreshToken("old_refresh_token");
        Response<BaseResponse<LoginResponse>> response = call.execute();
        
        // 验证响应
        assertTrue(response.isSuccessful());
        assertEquals("new_access_token", response.body().getData().getAccessToken());
        
        // 验证请求
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/app-api/member/auth/refresh-token?refreshToken=old_refresh_token", 
                recordedRequest.getPath());
    }
    
    /**
     * 测试退出登录接口
     */
    @Test
    public void testLogout() throws Exception {
        // 准备响应
        BaseResponse<Void> baseResponse = new BaseResponse<>();
        baseResponse.setCode(0);
        baseResponse.setMessage("退出成功");
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(baseResponse)));
        
        // 执行请求
        Call<BaseResponse<Void>> call = apiService.logout();
        Response<BaseResponse<Void>> response = call.execute();
        
        // 验证响应
        assertTrue(response.isSuccessful());
        assertEquals(0, response.body().getCode());
        
        // 验证请求
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/app-api/member/auth/logout", recordedRequest.getPath());
    }
    
    /**
     * 测试获取用户信息接口
     */
    @Test
    public void testGetUserInfo() throws Exception {
        // 准备响应
        UserDto userDto = new UserDto();
        userDto.setId(12345L);
        userDto.setMobile("13800138000");
        userDto.setNickname("测试用户");
        userDto.setAvatar("https://example.com/avatar.jpg");
        userDto.setEmail("test@example.com");
        userDto.setStatus(1);
        
        BaseResponse<UserDto> baseResponse = new BaseResponse<>();
        baseResponse.setCode(0);
        baseResponse.setData(userDto);
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(baseResponse)));
        
        // 执行请求
        Call<BaseResponse<UserDto>> call = apiService.getUserInfo();
        Response<BaseResponse<UserDto>> response = call.execute();
        
        // 验证响应
        assertTrue(response.isSuccessful());
        assertNotNull(response.body().getData());
        assertEquals(12345L, response.body().getData().getId().longValue());
        assertEquals("测试用户", response.body().getData().getNickname());
        
        // 验证请求
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertEquals("/app-api/member/user/get", recordedRequest.getPath());
    }
    
    /**
     * 测试重置密码接口
     */
    @Test
    public void testResetPassword() throws Exception {
        // 准备响应
        BaseResponse<Void> baseResponse = new BaseResponse<>();
        baseResponse.setCode(0);
        baseResponse.setMessage("密码重置成功");
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(baseResponse)));
        
        // 执行请求
        ResetPasswordReqDto req = new ResetPasswordReqDto();
        req.setMobile("13800138000");
        req.setCode("123456");
        req.setPassword("newpassword");
        Call<BaseResponse<Boolean>> call = apiService.resetPassword(req);
        Response<BaseResponse<Boolean>> response = call.execute();

        // 验证响应
        assertTrue(response.isSuccessful());
        assertEquals(0, response.body().getCode());
        
        // 验证请求
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("PUT", recordedRequest.getMethod());
        assertEquals("/app-api/member/user/reset-password?mobile=13800138000&code=123456&password=newpassword", 
                recordedRequest.getPath());
    }
    
    /**
     * 测试错误响应处理
     */
    @Test
    public void testErrorResponse() throws Exception {
        // 准备错误响应
        BaseResponse<LoginResponse> baseResponse = new BaseResponse<>();
        baseResponse.setCode(1001);
        baseResponse.setMessage("用户名或密码错误");
        baseResponse.setData(null);
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(baseResponse)));
        
        // 执行请求
        LoginRequest request = LoginRequest.passwordLogin("13800138000", "wrongpassword");
        Call<BaseResponse<LoginResponse>> call = apiService.loginByPassword(request);
        Response<BaseResponse<LoginResponse>> response = call.execute();
        
        // 验证响应
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(1001, response.body().getCode());
        assertEquals("用户名或密码错误", response.body().getMessage());
        assertNull(response.body().getData());
    }
    
    /**
     * 测试网络错误
     */
    @Test
    public void testNetworkError() throws Exception {
        // 模拟服务器错误
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));
        
        // 执行请求
        Call<BaseResponse<LoginResponse>> call = apiService.loginByPassword(
                LoginRequest.passwordLogin("13800138000", "password"));
        Response<BaseResponse<LoginResponse>> response = call.execute();
        
        // 验证响应
        assertFalse(response.isSuccessful());
        assertEquals(500, response.code());
    }
    
    /**
     * 测试超时
     */
    @Test(expected = IOException.class)
    public void testTimeout() throws Exception {
        // 设置延迟响应，超过客户端超时时间
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .setBodyDelay(2, TimeUnit.SECONDS));
        
        // 执行请求，应该抛出超时异常
        Call<BaseResponse<LoginResponse>> call = apiService.loginByPassword(
                LoginRequest.passwordLogin("13800138000", "password"));
        call.execute();
    }
}