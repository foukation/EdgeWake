package com.fxzs.lingxiagent.model.auth.dto;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;

/**
 * Auth模块DTO的单元测试
 * 测试数据模型的序列化和反序列化
 */
public class AuthDtoTest {
    
    private Gson gson;
    
    @Before
    public void setUp() {
        gson = new GsonBuilder()
                .setLenient()
                .create();
    }
    
    /**
     * 测试LoginRequest
     */
    @Test
    public void testLoginRequest() {
        // 测试密码登录请求
        LoginRequest passwordRequest = LoginRequest.passwordLogin("13800138000", "password123");
        assertNotNull(passwordRequest);
        assertEquals("13800138000", passwordRequest.getMobile());
        assertEquals("password123", passwordRequest.getPassword());
        
        // 测试JSON序列化
        String json = gson.toJson(passwordRequest);
        assertTrue(json.contains("\"mobile\":\"13800138000\""));
        assertTrue(json.contains("\"password\":\"password123\""));
        
        // 测试JSON反序列化
        LoginRequest deserialized = gson.fromJson(json, LoginRequest.class);
        assertEquals(passwordRequest.getMobile(), deserialized.getMobile());
        assertEquals(passwordRequest.getPassword(), deserialized.getPassword());
    }
    
    /**
     * 测试SmsLoginRequest
     */
    @Test
    public void testSmsLoginRequest() {
        SmsLoginRequest request = new SmsLoginRequest("13800138000", "123456");
        assertEquals("13800138000", request.getMobile());
        assertEquals("123456", request.getCode());
        
        // 测试序列化
        String json = gson.toJson(request);
        assertTrue(json.contains("\"mobile\":\"13800138000\""));
        assertTrue(json.contains("\"code\":\"123456\""));
        
        // 测试反序列化
        SmsLoginRequest deserialized = gson.fromJson(json, SmsLoginRequest.class);
        assertEquals(request.getMobile(), deserialized.getMobile());
        assertEquals(request.getCode(), deserialized.getCode());
    }
    
    /**
     * 测试LoginResponse
     */
    @Test
    public void testLoginResponse() {
        LoginResponse response = new LoginResponse();
        response.setAccessToken("test_access_token");
        response.setRefreshToken("test_refresh_token");
        response.setExpiresTime(System.currentTimeMillis() + 7200000);
        response.setUserId(12345L);
        
        UserDto user = new UserDto();
        user.setId(12345L);
        user.setMobile("13800138000");
        response.setUser(user);
        
        // 验证getter方法
        assertEquals("test_access_token", response.getAccessToken());
        assertEquals("test_refresh_token", response.getRefreshToken());
        assertEquals(12345L, response.getUserId().longValue());
        assertNotNull(response.getUser());
        
        // 测试JSON序列化
        String json = gson.toJson(response);
        assertTrue(json.contains("\"accessToken\":\"test_access_token\""));
        assertTrue(json.contains("\"refreshToken\":\"test_refresh_token\""));
        assertTrue(json.contains("\"userId\":12345"));
        
        // 测试JSON反序列化
        LoginResponse deserialized = gson.fromJson(json, LoginResponse.class);
        assertEquals(response.getAccessToken(), deserialized.getAccessToken());
        assertEquals(response.getRefreshToken(), deserialized.getRefreshToken());
        assertEquals(response.getUserId(), deserialized.getUserId());
    }
    
    /**
     * 测试UserDto
     */
    @Test
    public void testUserDto() {
        UserDto user = new UserDto();
        user.setId(12345L);
        user.setMobile("13800138000");
        user.setNickname("测试用户");
        user.setAvatar("https://example.com/avatar.jpg");
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setStatus(1);
        user.setCreateTime("2024-01-01 12:00:00");
        
        // 验证所有getter方法
        assertEquals(12345L, user.getId().longValue());
        assertEquals("13800138000", user.getMobile());
        assertEquals("测试用户", user.getNickname());
        assertEquals("https://example.com/avatar.jpg", user.getAvatar());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(1, user.getStatus().intValue());
        assertEquals("2024-01-01 12:00:00", user.getCreateTime());
        
        // 测试JSON序列化
        String json = gson.toJson(user);
        assertTrue(json.contains("\"id\":12345"));
        assertTrue(json.contains("\"mobile\":\"13800138000\""));
        assertTrue(json.contains("\"nickname\":\"测试用户\""));
        
        // 测试JSON反序列化
        UserDto deserialized = gson.fromJson(json, UserDto.class);
        assertEquals(user.getId(), deserialized.getId());
        assertEquals(user.getMobile(), deserialized.getMobile());
        assertEquals(user.getNickname(), deserialized.getNickname());
    }
    
    /**
     * 测试SendSmsRequest
     */
    @Test
    public void testSendSmsRequest() {
        SendSmsRequest request = new SendSmsRequest("13800138000", 1);
        assertEquals("13800138000", request.getMobile());
        assertEquals(1, request.getScene().intValue());
        
        // 测试序列化
        String json = gson.toJson(request);
        assertTrue(json.contains("\"mobile\":\"13800138000\""));
        assertTrue(json.contains("\"scene\":1"));
        
        // 测试反序列化
        SendSmsRequest deserialized = gson.fromJson(json, SendSmsRequest.class);
        assertEquals(request.getMobile(), deserialized.getMobile());
        assertEquals(request.getScene(), deserialized.getScene());
    }
    
    /**
     * 测试RegisterRequest
     */
    @Test
    public void testRegisterRequest() {
        RegisterRequest request = new RegisterRequest("13800138000", "123456", "password123");
        assertEquals("13800138000", request.getMobile());
        assertEquals("123456", request.getCode());
        assertEquals("password123", request.getPassword());
        
        // 测试序列化
        String json = gson.toJson(request);
        assertTrue(json.contains("\"mobile\":\"13800138000\""));
        assertTrue(json.contains("\"code\":\"123456\""));
        assertTrue(json.contains("\"password\":\"password123\""));
        
        // 测试反序列化
        RegisterRequest deserialized = gson.fromJson(json, RegisterRequest.class);
        assertEquals(request.getMobile(), deserialized.getMobile());
        assertEquals(request.getCode(), deserialized.getCode());
        assertEquals(request.getPassword(), deserialized.getPassword());
    }
    
    /**
     * 测试空值处理
     */
    @Test
    public void testNullHandling() {
        // 测试LoginResponse空值
        LoginResponse emptyResponse = new LoginResponse();
        assertNull(emptyResponse.getAccessToken());
        assertNull(emptyResponse.getRefreshToken());
        assertNull(emptyResponse.getUserId());
        assertNull(emptyResponse.getUser());
        
        // 测试UserDto空值
        UserDto emptyUser = new UserDto();
        assertNull(emptyUser.getId());
        assertNull(emptyUser.getMobile());
        assertNull(emptyUser.getNickname());
        
        // 测试从空JSON反序列化
        LoginResponse fromEmptyJson = gson.fromJson("{}", LoginResponse.class);
        assertNotNull(fromEmptyJson);
        assertNull(fromEmptyJson.getAccessToken());
    }
    
    /**
     * 测试边界情况
     */
    @Test
    public void testEdgeCases() {
        // 测试空字符串
        LoginRequest emptyRequest = LoginRequest.passwordLogin("", "");
        assertEquals("", emptyRequest.getMobile());
        assertEquals("", emptyRequest.getPassword());
        
        // 测试特殊字符
        UserDto user = new UserDto();
        user.setNickname("测试用户@#$%");
        user.setEmail("test+tag@example.com");
        
        String json = gson.toJson(user);
        UserDto deserialized = gson.fromJson(json, UserDto.class);
        assertEquals(user.getNickname(), deserialized.getNickname());
        assertEquals(user.getEmail(), deserialized.getEmail());
        
        // 测试长字符串
        String longPassword = "a".repeat(100);
        RegisterRequest longRequest = new RegisterRequest("13800138000", "123456", longPassword);
        assertEquals(100, longRequest.getPassword().length());
    }
    
    /**
     * 测试默认构造函数
     */
    @Test
    public void testDefaultConstructors() {
        // 所有DTO都应该有默认构造函数
        assertNotNull(new LoginRequest());
        assertNotNull(new SmsLoginRequest("", ""));
        assertNotNull(new LoginResponse());
        assertNotNull(new UserDto());
        assertNotNull(new SendSmsRequest());
        assertNotNull(new RegisterRequest());
    }
}