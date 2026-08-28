package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.user.dto.FeedbackReqDto;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepositoryImpl;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class VMFeedback extends BaseViewModel {
    
    private static final String TAG = "VMFeedback";
    
    // 双向绑定字段
    private final ObservableField<String> contactInfo = new ObservableField<>("");
    private final ObservableField<String> feedbackContent = new ObservableField<>("");
    private final ObservableField<Boolean> submitEnabled = new ObservableField<>(false);
    private final ObservableField<String> imageCountText = new ObservableField<>("0/4");
    
    // 业务状态
    private final MutableLiveData<Boolean> submitResult = new MutableLiveData<>();
    private final MutableLiveData<List<String>> imageList = new MutableLiveData<>(new ArrayList<>());

    // 内部状态标志
    private boolean hasSubmittedSuccessfully = false;

    // Repository
    private final UserRepository userRepository;
    
    // 常量
    private static final int MAX_IMAGES = 4;
    private static final int MIN_CONTENT_LENGTH = 1;  // 只要有内容就可以提交
    
    public VMFeedback(@NonNull Application application) {
        super(application);
        userRepository = new UserRepositoryImpl();
        
        // 监听内容变化，更新提交按钮状态
        feedbackContent.observeForever(this::validateForm);
        contactInfo.observeForever(this::validateForm);
    }
    
    // Getters
    public ObservableField<String> getContactInfo() {
        return contactInfo;
    }
    
    public ObservableField<String> getFeedbackContent() {
        return feedbackContent;
    }
    
    public ObservableField<Boolean> getSubmitEnabled() {
        return submitEnabled;
    }
    
    public ObservableField<String> getImageCountText() {
        return imageCountText;
    }
    
    public MutableLiveData<Boolean> getSubmitResult() {
        return submitResult;
    }
    
    public MutableLiveData<List<String>> getImageList() {
        return imageList;
    }
    
    // 业务方法-预提交
    public void preSubmitFeedback(String versionName) {
        Timber.tag(TAG).d( "submitFeedback called, submitEnabled: " + submitEnabled.get());
        
        String contact = contactInfo.get();
        String content = feedbackContent.get();
        
        // 校验联系方式
        if (contact == null || contact.trim().isEmpty()) {
            setError("请输入联系方式（邮箱或微信）");
            return;
        }
        
        // 校验反馈内容
        if (content == null || content.trim().isEmpty()) {
            setError("请输入反馈内容");
            return;
        }
        
        if (!submitEnabled.get()) {
            Timber.tag(TAG).w( "Submit is not enabled");
            return;
        }
        
        setLoading(true);
        
        List<String> images = imageList.getValue();
        
        Timber.tag(TAG).d( "Feedback content: " + content);
        Timber.tag(TAG).d( "Contact: " + contact);
        Timber.tag(TAG).d( "Images count: " + (images != null ? images.size() : 0));
        
        // 如果有图片，先上传图片
        if (images != null && !images.isEmpty()) {
            uploadImagesAndSubmit(versionName, images, content, contact);
        } else {
            // 没有图片，直接提交
            submitFeedback(versionName, content, contact, null);
        }
    }
    
    private void uploadImagesAndSubmit(String versionName, List<String> images, String content, String contact) {
        List<String> uploadedUrls = new ArrayList<>();
        final int totalImages = images.size();
        
        for (String imagePath : images) {
            // 跳过已经是URL的图片（避免重复上传）
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                uploadedUrls.add(imagePath);
                if (uploadedUrls.size() == totalImages) {
                    submitFeedback(versionName, content, contact, uploadedUrls);
                }
                continue;
            }
            
            // 上传本地图片
            java.io.File file = new java.io.File(imagePath);
            okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/*"), file);
            okhttp3.MultipartBody.Part body = okhttp3.MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            
            userRepository.uploadFile(body, new UserRepository.Callback<String>() {
                @Override
                public void onSuccess(String url) {
                    Timber.tag(TAG).d( "Image uploaded successfully: " + url);
                    uploadedUrls.add(url);
                    
                    // 所有图片都上传完成，提交反馈
                    if (uploadedUrls.size() == totalImages) {
                        submitFeedback(versionName, content, contact, uploadedUrls);
                    }
                }
                
                @Override
                public void onError(String error) {
                    Timber.tag(TAG).e( "Image upload failed: " + error);
                    setLoading(false);
                    setError("图片上传失败: " + error);
                    submitResult.postValue(false);
                }
            });
        }
    }
    
    private void submitFeedback(String versionName, String content, String contact, List<String> imageUrls) {
        FeedbackReqDto feedbackReq = new FeedbackReqDto();
        feedbackReq.setType(0); // 默认类型
        feedbackReq.setTitle("用户反馈"); // 设置默认标题
        feedbackReq.setContent(content);
        feedbackReq.setContact(contact);
        if (imageUrls != null && !imageUrls.isEmpty()) {
            feedbackReq.setImageUrls(String.join(",", imageUrls));
        }
        feedbackReq.setAppVersion(versionName);

        Timber.tag(TAG).d( "Submitting feedback to server...");
        
        userRepository.submitFeedback(feedbackReq, new UserRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Timber.tag(TAG).d( "Feedback submitted successfully");
                setLoading(false);
                setSuccess("反馈提交成功，我们会尽快处理");

                // 设置成功提交标志
                hasSubmittedSuccessfully = true;

                // 清空表单数据
                clearFormData();

                submitResult.postValue(true);
            }
            
            @Override
            public void onError(String error) {
                Timber.tag(TAG).e( "Feedback submission failed: " + error);
                setLoading(false);
                setError("提交失败: " + error);
                submitResult.postValue(false);
            }
        });
    }
    
    public boolean hasUnsavedContent() {
        String content = feedbackContent.get();
        String contact = contactInfo.get();
        List<String> images = imageList.getValue();
        
        return (content != null && !content.isEmpty()) || 
               (contact != null && !contact.isEmpty()) ||
               (images != null && !images.isEmpty());
    }
    
    public boolean canAddMoreImages() {
        List<String> images = imageList.getValue();
        return images == null || images.size() < MAX_IMAGES;
    }
    
    public void addImage(String imagePath) {
        List<String> images = imageList.getValue();
        if (images == null) {
            images = new ArrayList<>();
        }
        
        if (images.size() < MAX_IMAGES) {
            images.add(imagePath);
            imageList.setValue(images);
            updateImageCount();
        }
    }
    
    public void removeImage(int position) {
        List<String> images = imageList.getValue();
        if (images != null && position >= 0 && position < images.size()) {
            images.remove(position);
            imageList.setValue(images);
            updateImageCount();
        }
    }

    /**
     * 清空表单数据
     */
    public void clearFormData() {
        Timber.tag(TAG).d( "Clearing form data");

        // 清空文本字段
        contactInfo.set("");
        feedbackContent.set("");

        // 清空图片列表
        imageList.setValue(new ArrayList<>());

        // 更新图片计数
        updateImageCount();

        // 重置提交结果状态和成功标志
        submitResult.setValue(null);
        hasSubmittedSuccessfully = false;

        Timber.tag(TAG).d( "Form data cleared successfully");
    }

    /**
     * 检查是否已成功提交过反馈
     * @return true如果已成功提交，false否则
     */
    public boolean hasSubmittedSuccessfully() {
        return hasSubmittedSuccessfully;
    }

    // 私有方法
    private void validateForm(String value) {
        String content = feedbackContent.get();
        String contact = contactInfo.get();
        
        // 需要联系方式和反馈内容都不为空才能提交
        boolean isContentValid = content != null && content.trim().length() >= MIN_CONTENT_LENGTH;
        boolean isContactValid = contact != null && !contact.trim().isEmpty();
        boolean isValid = isContentValid && isContactValid;
        
        Timber.tag(TAG).d( "validateForm - value: " + value);
        Timber.tag(TAG).d( "validateForm - content: " + content);
        Timber.tag(TAG).d( "validateForm - contact: " + contact);
        Timber.tag(TAG).d( "validateForm - content valid: " + isContentValid + ", contact valid: " + isContactValid);
        Timber.tag(TAG).d( "validateForm - overall valid: " + isValid);
        Timber.tag(TAG).d( "validateForm - submitEnabled before: " + submitEnabled.get());
        submitEnabled.set(isValid);
        Timber.tag(TAG).d( "validateForm - submitEnabled after: " + submitEnabled.get());
    }
    
    private void updateImageCount() {
        List<String> images = imageList.getValue();
        int count = images != null ? images.size() : 0;
        imageCountText.set(count + "/" + MAX_IMAGES);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        feedbackContent.removeObserver(this::validateForm);
        contactInfo.removeObserver(this::validateForm);
    }
}