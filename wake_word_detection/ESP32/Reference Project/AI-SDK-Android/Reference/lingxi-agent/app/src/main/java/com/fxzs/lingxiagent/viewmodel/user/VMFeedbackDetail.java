package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.user.dto.FeedbackDto;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepositoryImpl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VMFeedbackDetail extends BaseViewModel {
    
    // 双向绑定字段
    private final ObservableField<String> content = new ObservableField<>("");
    private final ObservableField<String> contact = new ObservableField<>("");
    private final ObservableField<String> time = new ObservableField<>("");
    private final ObservableField<String> status = new ObservableField<>("");
    private final ObservableField<String> replyContent = new ObservableField<>("");
    private final ObservableField<String> replyTime = new ObservableField<>("");
    
    // 业务状态
    private final MutableLiveData<List<String>> imageUrls = new MutableLiveData<>(new ArrayList<>());
    private final ObservableField<Boolean> hasImages = new ObservableField<>(false);
    private final ObservableField<Boolean> hasContact = new ObservableField<>(false);
    private final ObservableField<Boolean> hasReply = new ObservableField<>(false);

    private Long feedbackId;
    private final UserRepository userRepository;

    public VMFeedbackDetail(@NonNull Application application) {
        super(application);
        userRepository = new UserRepositoryImpl();
    }
    
    // Getters
    public ObservableField<String> getContent() {
        return content;
    }
    
    public ObservableField<String> getContact() {
        return contact;
    }
    
    public ObservableField<String> getTime() {
        return time;
    }
    
    public ObservableField<String> getStatus() {
        return status;
    }
    
    public ObservableField<String> getReplyContent() {
        return replyContent;
    }
    
    public ObservableField<String> getReplyTime() {
        return replyTime;
    }
    
    public MutableLiveData<List<String>> getImageUrls() {
        return imageUrls;
    }
    
    public ObservableField<Boolean> getHasImages() {
        return hasImages;
    }
    
    public ObservableField<Boolean> getHasContact() {
        return hasContact;
    }
    
    public ObservableField<Boolean> getHasReply() {
        return hasReply;
    }
    
    // 设置反馈ID
    public void setFeedbackId(Long id) {
        this.feedbackId = id;
        loadFeedbackDetail();
    }
    
    // 加载反馈详情
    private void loadFeedbackDetail() {
        if (feedbackId == null) {
            setError("反馈ID不能为空");
            return;
        }

        setLoading(true);

        userRepository.getFeedbackDetail(feedbackId, new UserRepository.Callback<FeedbackDto>() {
            @Override
            public void onSuccess(FeedbackDto feedback) {
                displayFeedback(feedback);
                setLoading(false);
            }

            @Override
            public void onError(String error) {
                setError("获取反馈详情失败: " + error);
                setLoading(false);
            }
        });
    }
    
    // 显示反馈数据
    private void displayFeedback(FeedbackDto feedback) {
        if (feedback == null) {
            setError("反馈详情不存在");
            return;
        }
        
        content.set(feedback.getContent());

        // 格式化创建时间
        if (feedback.getCreateTime() != null) {
            time.set(formatTime(feedback.getCreateTime()));
        }

        // 设置联系方式 - 优先使用contact字段
        String contactInfo = feedback.getContact();
        if (contactInfo == null || contactInfo.isEmpty()) {
            contactInfo = feedback.getContactInfo();
        }
        if (contactInfo != null && !contactInfo.isEmpty()) {
            contact.set(contactInfo);
            hasContact.set(true);
        } else {
            hasContact.set(false);
        }

        // 设置图片 - 处理API返回的images字段
        List<String> images = getImagesFromFeedback(feedback);
        if (images != null && !images.isEmpty()) {
            imageUrls.postValue(images);
            hasImages.set(true);
        } else {
            hasImages.set(false);
        }
        
        // 设置状态
        if (feedback.getStatus() != null) {
            switch (feedback.getStatus()) {
                case 0:
                    status.set("待处理");
                    break;
                case 1:
                    status.set("已处理");
                    break;
                case 2:
                    status.set("已回复");
                    break;
                default:
                    status.set("待处理");
            }
        } else {
            status.set("待处理");
        }

        // 设置回复
        if (feedback.getReply() != null && !feedback.getReply().isEmpty()) {
            replyContent.set(feedback.getReply());
            // 格式化回复时间
            if (feedback.getReplyTime() != null) {
                replyTime.set(formatTime(feedback.getReplyTime()));
            }
            hasReply.set(true);
        } else {
            hasReply.set(false);
        }
    }
    
    // 从反馈对象中获取图片列表
    private List<String> getImagesFromFeedback(FeedbackDto feedback) {
        // 优先使用imageUrls字段
        if (feedback.getImageUrls() != null && !feedback.getImageUrls().isEmpty()) {
            return feedback.getImageUrls();
        }

        // 如果imageUrls为空，尝试使用images字段
        if (feedback.getImages() != null && !feedback.getImages().isEmpty()) {
            return feedback.getImages();
        }

        // 如果images字段是字符串格式，需要解析
        // 根据API返回的格式，可能需要进一步处理
        return new ArrayList<>();
    }

    // 格式化时间戳为可读格式
    private String formatTime(Long timestamp) {
        if (timestamp == null) {
            return "";
        }

        try {
            Date date = new Date(timestamp);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return formatter.format(date);
        } catch (Exception e) {
            return "";
        }
    }
}