package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.cmdc.ai.assist.AIAssistantManager;
import com.cmdc.ai.assist.constraint.TtsConfig;
import com.fxzs.lingxiagent.model.chat.dto.VoiceSettingBean;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.user.repository.VoiceRepository;
import com.fxzs.lingxiagent.model.user.repository.VoiceRepositoryImpl;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.google.gson.Gson;

import java.util.List;

public class VMVoiceSettings extends BaseViewModel {

    private final MutableLiveData<Boolean> voiceChanged = new MutableLiveData<>();
    private final MutableLiveData<List<VoiceSettingBean>> voiceList = new MutableLiveData<>();
    private VoiceSettingBean currentVoiceOption;
    private final VoiceRepository voiceRepository;

    public VMVoiceSettings(@NonNull Application application) {
        super(application);
        voiceRepository = new VoiceRepositoryImpl();
        // 加载声音列表
        loadDefaultVoiceOptions();
        currentVoiceOption = getVoiceOption();
        voiceChanged.setValue(true);
    }

    public VoiceSettingBean getCurrentVoiceOption() {
        return currentVoiceOption;
    }

    public MutableLiveData<Boolean> getVoiceChanged() {
        return voiceChanged;
    }

    public MutableLiveData<List<VoiceSettingBean>> getVoiceList() {
        return voiceList;
    }

    public void selectVoice(VoiceSettingBean voiceSettingBean) {
        this.currentVoiceOption = voiceSettingBean;
        voiceChanged.setValue(true);
    }

    public void saveVoice() {
        Gson gson = new Gson();
        SharedPreferencesUtil.saveVoiceOption(gson.toJson(currentVoiceOption));
        AIAssistantManager.Companion.getInstance().changeTtsConfig(new TtsConfig(currentVoiceOption.getPer(),
                currentVoiceOption.getSpd(), currentVoiceOption.getPit(), currentVoiceOption.getVol()));
    }

    public VoiceSettingBean getVoiceOption() {
        String voiceOption = SharedPreferencesUtil.getVoiceOption();
        if (TextUtils.isEmpty(voiceOption)) return getDefaultVoiceOption();
        return new Gson().fromJson(voiceOption, VoiceSettingBean.class);
    }

    private VoiceSettingBean getDefaultVoiceOption() {
        List<VoiceSettingBean> voiceOptions = voiceRepository.getDefaultOptions();
        if (voiceOptions == null || voiceOptions.isEmpty())
            return new VoiceSettingBean("小雨-活力女主播", "新闻播报", 4100, false);
        // 使用 Stream API 筛选 per 为 4100 的 VoiceSettingBean
        return voiceOptions.stream()
                .filter(voice -> voice.getPer() == 4100)
                .findFirst()
                .orElse(new VoiceSettingBean("小雨-活力女主播", "新闻播报", 4100, false));
    }

    private void loadDefaultVoiceOptions() {
        setLoading(true);
        voiceRepository.loadDefaultVoiceOptions();
        voiceList.setValue(voiceRepository.getDefaultOptions());
        setLoading(false);
    }

}