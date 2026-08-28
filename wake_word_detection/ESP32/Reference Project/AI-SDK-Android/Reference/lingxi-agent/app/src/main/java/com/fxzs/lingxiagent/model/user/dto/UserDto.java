package com.fxzs.lingxiagent.model.user.dto;

public class UserDto {
    private Long id;
    private String nickname;
    private String mobile;
    private String avatar;
    private String email;
    private Integer sex;
    private Integer point;           // 积分
    private Integer experience;       // 经验值
    private Integer level;           // 会员等级
    private Boolean brokerageEnabled;// 是否成为推广员
    private String mark;             // 会员备注/个性签名
    private String registerIp;
    private String loginIp;
    private String loginDate;
    private String createTime;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getMobile() {
        return mobile;
    }
    
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Integer getSex() {
        return sex;
    }
    
    public void setSex(Integer sex) {
        this.sex = sex;
    }
    
    public String getRegisterIp() {
        return registerIp;
    }
    
    public void setRegisterIp(String registerIp) {
        this.registerIp = registerIp;
    }
    
    public String getLoginIp() {
        return loginIp;
    }
    
    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }
    
    public String getLoginDate() {
        return loginDate;
    }
    
    public void setLoginDate(String loginDate) {
        this.loginDate = loginDate;
    }
    
    public String getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    
    public Integer getPoint() {
        return point;
    }
    
    public void setPoint(Integer point) {
        this.point = point;
    }
    
    public Integer getExperience() {
        return experience;
    }
    
    public void setExperience(Integer experience) {
        this.experience = experience;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public Boolean getBrokerageEnabled() {
        return brokerageEnabled;
    }
    
    public void setBrokerageEnabled(Boolean brokerageEnabled) {
        this.brokerageEnabled = brokerageEnabled;
    }
    
    public String getMark() {
        return mark;
    }
    
    public void setMark(String mark) {
        this.mark = mark;
    }
}