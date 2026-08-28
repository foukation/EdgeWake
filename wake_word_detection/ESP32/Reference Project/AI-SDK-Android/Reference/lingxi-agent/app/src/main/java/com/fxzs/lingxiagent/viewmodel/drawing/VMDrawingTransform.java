package com.fxzs.lingxiagent.viewmodel.drawing;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.drawing.api.GenerateImageRequest;
import com.fxzs.lingxiagent.model.drawing.api.ImagineImg2ImgRequest;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepositoryImpl;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepositoryImpl;
import com.fxzs.lingxiagent.view.drawing.DrawingTransformStyleItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import android.os.Handler;
import android.os.Looper;
import timber.log.Timber;

/**
 * AI绘画风格转绘ViewModel
 */
public class VMDrawingTransform extends BaseViewModel {
    
    private final DrawingRepository repository;
    private final UserRepository userRepository;
    
    // 风格列表
    private final MutableLiveData<List<DrawingTransformStyleItem>> styleItems = new MutableLiveData<>(new ArrayList<>());
    
    // 生成进度
    private final MutableLiveData<Integer> generateProgress = new MutableLiveData<>(0);
    
    // 生成结果
    private final MutableLiveData<String> generatedImageUrl = new MutableLiveData<>();
    
    // 生成错误
    private final MutableLiveData<String> generateError = new MutableLiveData<>();
    
    private Timer pollingTimer;
    private Long currentTaskId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    Long sessionId;
    public VMDrawingTransform(@NonNull Application application) {
        super(application);
        repository = DrawingRepositoryImpl.getInstance();
        userRepository = new UserRepositoryImpl();
        // 初始化时加载风格列表
        loadStyleList();
    }
    
    /**
     * 获取风格列表
     */
    public MutableLiveData<List<DrawingTransformStyleItem>> getStyleItems() {
        return styleItems;
    }
    
    /**
     * 获取生成进度
     */
    public MutableLiveData<Integer> getGenerateProgress() {
        return generateProgress;
    }
    
    /**
     * 获取生成的图片URL
     */
    public MutableLiveData<String> getGeneratedImageUrl() {
        return generatedImageUrl;
    }
    
    /**
     * 获取生成错误
     */
    public MutableLiveData<String> getGenerateError() {
        return generateError;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setCurrentTaskId(Long currentTaskId) {
        this.currentTaskId = currentTaskId;
    }

    /**
     * 加载风格列表
     */
    public void loadStyleList() {
        setLoading(true);
        repository.getStyleListV2().observeForever(result -> {
            setLoading(false);
            if (result.isSuccess() && result.getData() != null) {
                List<DrawingTransformStyleItem> items = result.getData();
                styleItems.postValue(items);
            } else {
                setError(result.getError() != null ? result.getError() : "获取风格列表失败");
                styleItems.postValue(new ArrayList<>());
            }
        });
    }
    
    /**
     * 生成图片（风格转绘）
     */
    public void generateTransformImage(Uri imageUri, DrawingTransformStyleItem styleItem, String styleDescription) {
        if (imageUri == null) {
            generateError.postValue("请先选择一张图片");
            return;
        }
        
        setLoading(true);
        generateProgress.postValue(0);
        generateError.postValue(null);
        generatedImageUrl.postValue(null);

        if(!imageUri.toString().startsWith("http")){
            // 第一步：上传图片
            uploadImageAndGenerate(imageUri, styleItem, styleDescription);
        }else {

            callGenerateAPI(imageUri.toString(), styleItem, styleDescription);
        }
    }
    
    /**
     * 上传图片并生成
     */
    private void uploadImageAndGenerate(Uri imageUri, DrawingTransformStyleItem styleItem, String styleDescription) {
        // 将 URI 转换为文件
        File imageFile = uriToFile(imageUri);
        if (imageFile == null || !imageFile.exists()) {
            setLoading(false);
            generateError.postValue("图片文件读取失败");
            return;
        }
        
        // 创建 MultipartBody.Part
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);
        
        // 上传图片
        userRepository.uploadFile(body, new UserRepository.Callback<String>() {
            @Override
            public void onSuccess(String imageUrl) {
                // 上传成功，调用生成API
                callGenerateAPI(imageUrl, styleItem, styleDescription);
            }
            
            @Override
            public void onError(String error) {
                setLoading(false);
                generateError.postValue("图片上传失败: " + error);
            }
        });
    }
    
    /**
     * 调用生成API
     */
    private void callGenerateAPI(String referenceImageUrl, DrawingTransformStyleItem styleItem, String styleDescription) {
        // 组装提示词
        String realPrompt = styleDescription != null && !styleDescription.trim().isEmpty()
                ? styleDescription.trim()
                : "";

        String stylePrompt = "";
        if (styleItem != null && styleItem.getPrompt() != null) {
            stylePrompt = styleItem.getPrompt();
        }

        String prompt = "帮我生成图片：" + stylePrompt + "" + realPrompt;

        if(sessionId != null&& sessionId != 0){
            imagineImg2Img(referenceImageUrl,styleItem,prompt,realPrompt,sessionId);
        }else {
            repository.createImageSession(prompt,1).observeForever(sessionResult -> {
                if (sessionResult.isSuccess() && sessionResult.getData() != null) {
                    sessionId = sessionResult.getData();
                    imagineImg2Img(referenceImageUrl,styleItem,prompt,realPrompt,sessionId);
                } else {
                    setLoading(false);
                    generateError.postValue(sessionResult.getError() != null ? sessionResult.getError() : "创建会话失败");
                }
            });
        }

    }

    public void imagineImg2Img(String referenceImageUrl, DrawingTransformStyleItem styleItem,String prompt,String realPrompt,Long sessionId ){

        String styleName = "";
        if (styleItem != null && styleItem.getPrompt() != null) {
            styleName = styleItem.getName();
        }
        ImagineImg2ImgRequest request = new ImagineImg2ImgRequest();
        request.setPrompt(prompt);
        request.setReferenceImageUrl(referenceImageUrl);
        request.setSize("2k");
        if (styleItem != null) {
            request.setStyleId(styleItem.getId());
        }
        request.setStylePrompt(styleName);
        request.setRealPrompt(realPrompt);
        request.setWatermark(false);
        request.setSequentialImageGeneration("disabled");

        java.util.Map<String, Object> options = new java.util.HashMap<>();
        options.put("realPrompt", realPrompt);
        request.setOptions(options);
        request.setSessionId(String.valueOf(sessionId));

        repository.imagineImg2Img(request).observeForever(result -> {
            if (result.isSuccess() && result.getData() != null) {
                currentTaskId = result.getData();
                startPollingTaskStatus();
            } else {
                setLoading(false);
                generateError.postValue(result.getError() != null ? result.getError() : "生成失败");
            }
        });
    }
    /**
     * 轮询任务状态
     */
    public void startPollingTaskStatus() {
        if (currentTaskId == null) {
            return;
        }
        
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
        
        pollingTimer = new Timer();
        final int[] pollCount = {0};
        final int maxPolls = 60;
        final int[] uiProgress = {0};
        
        pollingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                pollCount[0]++;
                if (pollCount[0] > maxPolls) {
                    pollingTimer.cancel();
                    mainHandler.post(() -> {
                        generateError.postValue("图片生成超时，请重试");
                        setLoading(false);
                    });
                    return;
                }
                
                // 查询任务状态（需要在主线程中调用 observeForever）
                mainHandler.post(() -> {
                    DrawingImageDto queryDto = new DrawingImageDto();
                    queryDto.setId(currentTaskId);
                    repository.getImageDetail(queryDto).observeForever(result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            DrawingImageDto image = result.getData();
                            Integer status = image.getStatus();
                            
                            if (status != null && status == 20) {
                                // 生成成功
                                pollingTimer.cancel();
                                String finalUrl = image.getImageUrl();
                                if (finalUrl != null && !finalUrl.isEmpty()) {
                                    generateProgress.postValue(100);
                                    generatedImageUrl.postValue(finalUrl);
                                    setLoading(false);
                                } else {
                                    generateError.postValue("图片生成失败：未返回图片地址");
                                    setLoading(false);
                                }
                            } else if (status != null && status == 30) {
                                // 生成失败
                                pollingTimer.cancel();
                                generateError.postValue(image.getErrorMsg() != null ? image.getErrorMsg() : "图片生成失败");
                                setLoading(false);
                            } else if (status != null && status == 10) {
                                // 进行中：缓慢推进进度，不超过90%
                                if (uiProgress[0] < 90) {
                                    uiProgress[0] = Math.min(90, uiProgress[0] + 5);
                                }
                                generateProgress.postValue(uiProgress[0]);
                            }
                        } else {
                            if (pollCount[0] > 10 && !result.isSuccess()) {
                                pollingTimer.cancel();
                                generateError.postValue("查询图片状态失败，请重试");
                                setLoading(false);
                            }
                        }
                    });
                });
            }
        }, 1000, 2000);
    }
    
    /**
     * 将 URI 转换为 File
     */
    private File uriToFile(Uri uri) {
        try {
            ContentResolver contentResolver = getApplication().getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {
                return null;
            }
            
            // 创建临时文件
            File directory = getApplication().getCacheDir();
            String fileName = "transform_image_" + System.currentTimeMillis() + ".jpg";
            File file = new File(directory, fileName);
            
            // 复制内容
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            
            return file;
        } catch (Exception e) {
            Timber.e(e, "Failed to convert URI to file");
            return null;
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (pollingTimer != null) {
            pollingTimer.cancel();
            pollingTimer = null;
        }
    }
}

