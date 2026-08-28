package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.user.dto.FAQItem;

import java.util.ArrayList;
import java.util.List;

public class VMHelpCenter extends BaseViewModel {
    
    // 业务状态
    private final MutableLiveData<List<FAQItem>> faqList = new MutableLiveData<>();
    private final MutableLiveData<FAQItem> navigationEvent = new MutableLiveData<>();
    
    public VMHelpCenter(@NonNull Application application) {
        super(application);
        loadFAQList();
    }
    
    // Getters
    public MutableLiveData<List<FAQItem>> getFaqList() {
        return faqList;
    }
    
    public MutableLiveData<FAQItem> getNavigationEvent() {
        return navigationEvent;
    }
    
    // 业务方法
    public void onFAQClicked(FAQItem faq) {
        navigationEvent.setValue(faq);
    }
    
    private void loadFAQList() {
        // 模拟加载FAQ数据
        List<FAQItem> faqs = new ArrayList<>();
        
        faqs.add(new FAQItem(
            "1",
            "如何使用AI聊天功能？",
            "点击主页的聊天按钮，即可开始与AI对话。您可以问各种问题，AI会为您提供帮助。",
            "chat"
        ));
        
        faqs.add(new FAQItem(
            "2",
            "如何生成AI绘画？",
            "进入绘画模块，输入您想要绘制的内容描述，选择风格，点击生成即可。",
            "drawing"
        ));
        
        faqs.add(new FAQItem(
            "3",
            "如何使用PPT生成功能？",
            "输入PPT主题和大纲，AI会自动生成专业的PPT文稿，您可以下载使用。",
            "ppt"
        ));
        
        faqs.add(new FAQItem(
            "4",
            "会议记录功能怎么用？",
            "开启会议记录后，AI会实时转写会议内容，并自动生成会议摘要。",
            "meeting"
        ));
        
        faqs.add(new FAQItem(
            "5",
            "翻译功能支持哪些语言？",
            "目前支持中文、英文、日文、韩文、法文、德文、西班牙文等多种语言互译。",
            "translate"
        ));
        
        faqs.add(new FAQItem(
            "6",
            "如何更换AI模型？",
            "在设置中选择“首选大模型”，可以切换不同的AI模型。",
            "settings"
        ));
        
        faqs.add(new FAQItem(
            "7",
            "如何联系客服？",
            "您可以通过意见反馈功能提交问题，或者通过官网联系我们。",
            "feedback"
        ));
        
        faqs.add(new FAQItem(
            "8",
            "会员有什么特权？",
            "会员可以享受更高的使用限额、更快的响应速度、专属功能等特权。",
            "vip"
        ));
        
        faqList.postValue(faqs);
    }
}