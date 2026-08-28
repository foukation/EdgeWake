package com.fxzs.lingxiagent.model.user.repository;

import com.fxzs.lingxiagent.model.chat.dto.VoiceSettingBean;

import java.util.List;

/**
 * VoiceRepository 接口
 * 该接口用于定义语音相关的数据访问操作
 *
 * @author 于海生
 */
public interface VoiceRepository {

    void loadDefaultVoiceOptions();

    List<VoiceSettingBean> getDefaultOptions();
}