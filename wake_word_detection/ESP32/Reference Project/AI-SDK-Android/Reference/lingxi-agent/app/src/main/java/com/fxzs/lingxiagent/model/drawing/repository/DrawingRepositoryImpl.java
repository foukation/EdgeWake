package com.fxzs.lingxiagent.model.drawing.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.IYAApplication;
import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.drawing.api.CreateImageSessionRequest;
import com.fxzs.lingxiagent.model.drawing.api.DrawingApiService;
import com.fxzs.lingxiagent.model.drawing.api.GenerateImageRequest;
import com.fxzs.lingxiagent.model.drawing.api.ImagineImg2ImgRequest;
import com.fxzs.lingxiagent.model.drawing.api.PageResult;
import com.fxzs.lingxiagent.model.drawing.api.SampleListRequest;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSampleDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingStyleDto;
import com.fxzs.lingxiagent.model.honor.repository.BenefitCode;
import com.fxzs.lingxiagent.util.BillDialogHelper;
import com.fxzs.lingxiagent.view.drawing.DrawingTransformStyleItem;
import com.fxzs.lingxiagent.model.network.RetrofitClient;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * AI绘画仓库实现
 */
public class DrawingRepositoryImpl implements DrawingRepository {
    
    private static DrawingRepositoryImpl instance;
    private final DrawingApiService apiService;
    
    private DrawingRepositoryImpl() {
        apiService = RetrofitClient.getInstance().createService(DrawingApiService.class);
    }
    
    public static synchronized DrawingRepositoryImpl getInstance() {
        if (instance == null) {
            instance = new DrawingRepositoryImpl();
        }
        return instance;
    }
    
    @Override
    public LiveData<Result<Long>> createImageSession(String sessionName,Integer type) {
        MutableLiveData<Result<Long>> result = new MutableLiveData<>();
        CreateImageSessionRequest request;
        if(type!=null){
             request = new CreateImageSessionRequest(sessionName, type);
        }else {
            request = new CreateImageSessionRequest(sessionName);
        }
//        CreateImageSessionRequest
        apiService.createImageSession(request).enqueue(new Callback<BaseResponse<Long>>() {
            @Override
            public void onResponse(Call<BaseResponse<Long>> call, 
                                 Response<BaseResponse<Long>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Long> baseResponse = response.body();
                    if (baseResponse.getCode() == 0) {
                        Timber.tag("DrawingRepo").d( "Session created successfully, sessionId: " + baseResponse.getData());
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg()));
                    }
                } else {
                    result.postValue(Result.error("创建会话失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Long>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<List<DrawingStyleDto>>> getStyles() {
        MutableLiveData<Result<List<DrawingStyleDto>>> result = new MutableLiveData<>();
        
        apiService.getStyles().enqueue(new Callback<BaseResponse<List<DrawingStyleDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<DrawingStyleDto>>> call, 
                                 Response<BaseResponse<List<DrawingStyleDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<DrawingStyleDto>> baseResponse = response.body();
                    if (baseResponse.getCode() == 0) { // Volc API成功码是0
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg()));
                    }
                } else {
                    result.postValue(Result.error("获取风格列表失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<List<DrawingStyleDto>>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<List<DrawingTransformStyleItem>>> getStyleListV2() {
        MutableLiveData<Result<List<DrawingTransformStyleItem>>> result = new MutableLiveData<>();
        
        apiService.getStyleListV2().enqueue(new Callback<BaseResponse<List<DrawingTransformStyleItem>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<DrawingTransformStyleItem>>> call, 
                                 Response<BaseResponse<List<DrawingTransformStyleItem>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<DrawingTransformStyleItem>> baseResponse = response.body();
                    if (baseResponse.getCode() == 0) { // Volc API成功码是0
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg()));
                    }
                } else {
                    result.postValue(Result.error("获取风格列表失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<List<DrawingTransformStyleItem>>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<DrawingImageDto>> generateImage(GenerateImageRequest request) {
        MutableLiveData<Result<DrawingImageDto>> result = new MutableLiveData<>();
        
        // Volc接口返回的是任务ID(Long类型)
        apiService.generateImage(request).enqueue(new Callback<BaseResponse<Long>>() {
            @Override
            public void onResponse(Call<BaseResponse<Long>> call,
                                 Response<BaseResponse<Long>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Long> baseResponse = response.body();
                    if (baseResponse.getCode() == 0) { // Volc API成功码是0
                        // 返回一个包含任务ID的DrawingImageDto
                        DrawingImageDto imageDto = new DrawingImageDto();
                        imageDto.setId(baseResponse.getData());
                        imageDto.setStatus(0); // 生成中
                        result.postValue(Result.success(imageDto));
                    } else if(BenefitCode.isBenefitError(String.valueOf(baseResponse.getCode()))) {
                        result.postValue(Result.billError(String.valueOf(baseResponse.getCode()), baseResponse.getMessage()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg()));
                    }
                } else {
                    result.postValue(Result.error("生成图片失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Long>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<Long>> imagineImg2Img(ImagineImg2ImgRequest request) {
        MutableLiveData<Result<Long>> result = new MutableLiveData<>();

        apiService.imagineImg2Img(request).enqueue(new Callback<BaseResponse<Long>>() {
            @Override
            public void onResponse(Call<BaseResponse<Long>> call, Response<BaseResponse<Long>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Long> baseResponse = response.body();
                    if (baseResponse.getCode() == 0) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg()));
                    }
                } else {
                    result.postValue(Result.error("生成图片失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Long>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });

        return result;
    }

    @Override
    public LiveData<Result<DrawingImageDto>> generateImageSync(GenerateImageRequest request) {
        MutableLiveData<Result<DrawingImageDto>> result = new MutableLiveData<>();
        
        apiService.generateImageSync(request).enqueue(new Callback<BaseResponse<Object>>() {
            @Override
            public void onResponse(Call<BaseResponse<Object>> call,
                                 Response<BaseResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Object> baseResponse = response.body();
                    Timber.tag("DrawingRepo").d( "Response code: " + baseResponse.getCode());
                    Timber.tag("DrawingRepo").d( "Response data type: " + (baseResponse.getData() != null ? baseResponse.getData().getClass().getName() : "null"));
                    Timber.tag("DrawingRepo").d( "Response data: " + baseResponse.getData());
                    
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        // 同步生成返回的是图片URL字符串
                        Object data = baseResponse.getData();
                        DrawingImageDto imageDto = new DrawingImageDto();
                        
                        if (data instanceof String) {
                            // 如果返回的是URL字符串
                            String imageUrl = (String) data;
                            imageDto.setImageUrl(imageUrl);
                            imageDto.setThumbnailUrl(imageUrl);
                            imageDto.setPrompt(request.getPrompt());
                            imageDto.setWidth(request.getWidth());
                            imageDto.setHeight(request.getHeight());
                            imageDto.setStatus(1); // 生成成功
                            imageDto.setCreateTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                            
                            // 设置其他信息
                            if (request.getStyleId() != null) {
                                imageDto.setStyle("风格" + request.getStyleId());
                            }
                            imageDto.setAspectRatio(request.getWidth() + ":" + request.getHeight());
                        } else if (data instanceof java.util.Map) {
                            // 如果返回的是Map对象
                            try {
                                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
                                // 尝试从不同的字段获取图片URL
                                String imageUrl = null;
                                if (dataMap.containsKey("url")) {
                                    imageUrl = String.valueOf(dataMap.get("url"));
                                } else if (dataMap.containsKey("imageUrl")) {
                                    imageUrl = String.valueOf(dataMap.get("imageUrl"));
                                } else if (dataMap.containsKey("image_url")) {
                                    imageUrl = String.valueOf(dataMap.get("image_url"));
                                } else if (dataMap.containsKey("result")) {
                                    imageUrl = String.valueOf(dataMap.get("result"));
                                }
                                
                                if (imageUrl != null && !imageUrl.equals("null")) {
                                    imageDto.setImageUrl(imageUrl);
                                    imageDto.setThumbnailUrl(imageUrl);
                                    imageDto.setPrompt(request.getPrompt());
                                    imageDto.setWidth(request.getWidth());
                                    imageDto.setHeight(request.getHeight());
                                    imageDto.setStatus(1); // 生成成功
                                    imageDto.setCreateTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                                    
                                    if (request.getStyleId() != null) {
                                        imageDto.setStyle("风格" + request.getStyleId());
                                    }
                                    imageDto.setAspectRatio(request.getWidth() + ":" + request.getHeight());
                                } else {
                                    // 如果没有找到图片URL，返回错误
                                    Timber.tag("DrawingRepo").e( "No image URL found in response");
                                    result.postValue(Result.error("生成失败：未返回图片地址"));
                                    return;
                                }
                            } catch (Exception e) {
                                Timber.tag("DrawingRepo").e( "Error parsing response data: " + e.getMessage());
                                result.postValue(Result.error("生成失败：响应数据解析错误"));
                                return;
                            }
                        } else {
                            // 其他类型，返回错误
                            Timber.tag("DrawingRepo").e( "Unknown response data type: " + (data != null ? data.getClass().getName() : "null"));
                            result.postValue(Result.error("生成失败：未知的响应数据类型"));
                            return;
                        }
                        
                        result.postValue(Result.success(imageDto));
                    } else {
                        result.postValue(Result.error(baseResponse.getMessage() != null ? baseResponse.getMessage() : "生成失败"));
                    }
                } else {
                    result.postValue(Result.error("生成图片失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Object>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<DrawingImageDto>> getImageDetail(DrawingImageDto imageDto) {
        MutableLiveData<Result<DrawingImageDto>> result = new MutableLiveData<>();
        
        apiService.getImageDetail(imageDto).enqueue(new Callback<BaseResponse<DrawingImageDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<DrawingImageDto>> call,
                                 Response<BaseResponse<DrawingImageDto>> response) {
                Timber.tag("DrawingRepo").d( "=== getImageDetail Response ===");
                Timber.tag("DrawingRepo").d( "Request URL: " + response.raw().request().url());
                
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<DrawingImageDto> baseResponse = response.body();
                    Timber.tag("DrawingRepo").d( "Response code: " + baseResponse.getCode());
                    Timber.tag("DrawingRepo").d( "Response message: " + baseResponse.getMessage());
                    
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        DrawingImageDto imageData = baseResponse.getData();
                        if (imageData != null) {
                            Timber.tag("DrawingRepo").d( "=== 服务器返回的图片详情 ===");
                            Timber.tag("DrawingRepo").d( "ID: " + imageData.getId() + " <-- 查询的ID");
                            Timber.tag("DrawingRepo").d( "Prompt: " + imageData.getPrompt());
                            Timber.tag("DrawingRepo").d( "ImageUrl字段: " + imageData.getImageUrl());
                            Timber.tag("DrawingRepo").d( "PicUrl字段: " + imageData.getPicUrl());
                            Timber.tag("DrawingRepo").d( "SampleUrl字段: " + imageData.getSampleUrl());
                            Timber.tag("DrawingRepo").d( "最终URL (getImageUrl): " + imageData.getImageUrl());
                            Timber.tag("DrawingRepo").d( "ThumbnailUrl: " + imageData.getThumbnailUrl());
                            Timber.tag("DrawingRepo").d( "Style: " + imageData.getStyle());
                            Timber.tag("DrawingRepo").d( "AspectRatio: " + imageData.getAspectRatio());
                            Timber.tag("DrawingRepo").d( "Width: " + imageData.getWidth());
                            Timber.tag("DrawingRepo").d( "Height: " + imageData.getHeight());
                            Timber.tag("DrawingRepo").d( "Status: " + imageData.getStatus());
                            Timber.tag("DrawingRepo").d( "CreateTime: " + imageData.getCreateTime());
                        } else {
                            Timber.tag("DrawingRepo").d( "返回的图片详情为null");
                        }
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        // 处理错误情况，包括404等
                        String errorMsg = baseResponse.getMsg() != null ? baseResponse.getMsg() : 
                                         baseResponse.getMessage() != null ? baseResponse.getMessage() : 
                                         "获取图片详情失败";
                        Timber.tag("DrawingRepo").e( "API错误: " + errorMsg);
                        result.postValue(Result.error(errorMsg));
                    }
                } else {
                    Timber.tag("DrawingRepo").e( "HTTP错误: " + response.code() + " - " + response.message());
                    result.postValue(Result.error("获取图片详情失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<DrawingImageDto>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<PageResult<DrawingSessionDto>>> getMySessions(Map<String, Object> params) {
        MutableLiveData<Result<PageResult<DrawingSessionDto>>> result = new MutableLiveData<>();
        
        apiService.getMySessions(params,0).enqueue(new Callback<BaseResponse<PageResult<DrawingSessionDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<PageResult<DrawingSessionDto>>> call,
                                 Response<BaseResponse<PageResult<DrawingSessionDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<PageResult<DrawingSessionDto>> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg()));
                    }
                } else {
                    result.postValue(Result.error("获取会话列表失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<PageResult<DrawingSessionDto>>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<PageResult<DrawingSampleDto>>> getSampleList(SampleListRequest request) {
        MutableLiveData<Result<PageResult<DrawingSampleDto>>> result = new MutableLiveData<>();
        
        apiService.getSampleList(request).enqueue(new Callback<BaseResponse<PageResult<DrawingSampleDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<PageResult<DrawingSampleDto>>> call,
                                 Response<BaseResponse<PageResult<DrawingSampleDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<PageResult<DrawingSampleDto>> baseResponse = response.body();
                    
                    // 打印完整的响应
                    Timber.tag("DrawingRepo").d( "=== getSampleList API Response ===");
                    Timber.tag("DrawingRepo").d( "Response code: " + baseResponse.getCode());
                    Timber.tag("DrawingRepo").d( "Response message: " + baseResponse.getMessage());
                    Timber.tag("DrawingRepo").d( "Response data: " + baseResponse.getData());
                    
                    // 打印请求信息
                    Timber.tag("DrawingRepo").d( "Request URL: " + response.raw().request().url());
                    Timber.tag("DrawingRepo").d( "Request body: " + request.toString());
                    
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        PageResult<DrawingSampleDto> pageResult = baseResponse.getData();
                        if (pageResult != null) {
                            Timber.tag("DrawingRepo").d( "PageResult - total: " + pageResult.getTotal() + 
                                ", pages: " + pageResult.getPages() + 
                                ", current: " + pageResult.getCurrent() + 
                                ", size: " + pageResult.getSize() +
                                ", records(getRecords): " + (pageResult.getRecords() != null ? pageResult.getRecords().size() : "null") +
                                ", list: " + (pageResult.getList() != null ? pageResult.getList().size() : "null") +
                                ", data: " + (pageResult.getData() != null ? pageResult.getData().size() : "null"));
                            
                            // 打印完整的样本数据
                            List<DrawingSampleDto> samples = pageResult.getList();
                            if (samples != null && !samples.isEmpty()) {
                                Timber.tag("DrawingRepo").d( "=== 服务器返回的完整样本数据 ===");
                                for (int i = 0; i < samples.size(); i++) {
                                    DrawingSampleDto sample = samples.get(i);
                                    Timber.tag("DrawingRepo").d( "样本[" + i + "]: {" +
                                        "id=" + sample.getId() + " <-- 服务器返回的ID, " +
                                        "prompt='" + sample.getPrompt() + "', " +
                                        "imageUrl='" + sample.getImageUrl() + "', " +
                                        "catId=" + sample.getCatId() + ", " +
                                        "catName='" + sample.getCatName() + "', " +
                                        "styleId=" + sample.getStyleId() + ", " +
                                        "styleName='" + sample.getStyleName() + "'}");
                                }
                            }
                        } else {
                            Timber.tag("DrawingRepo").d( "PageResult is null");
                        }
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        Timber.tag("DrawingRepo").e( "API error code: " + baseResponse.getCode() + ", message: " + baseResponse.getMessage());
                        result.postValue(Result.error(baseResponse.getMessage()));
                    }
                } else {
                    Timber.tag("DrawingRepo").e( "Response not successful: " + response.code());
                    result.postValue(Result.error("获取示例列表失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<PageResult<DrawingSampleDto>>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    
    @Override
    public LiveData<Result<String>> updateSession(DrawingSessionDto sessionDto) {
        MutableLiveData<Result<String>> result = new MutableLiveData<>();
        
        apiService.updateSession(sessionDto).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call,
                                 Response<BaseResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMessage()));
                    }
                } else {
                    result.postValue(Result.error("更新会话失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<PageResult<DrawingImageDto>>> getMyImages(Map<String, Object> params) {
        MutableLiveData<Result<PageResult<DrawingImageDto>>> result = new MutableLiveData<>();
        
        apiService.getMyImages(params).enqueue(new Callback<BaseResponse<PageResult<DrawingImageDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<PageResult<DrawingImageDto>>> call,
                                 Response<BaseResponse<PageResult<DrawingImageDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<PageResult<DrawingImageDto>> baseResponse = response.body();
                    if (baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMessage()));
                    }
                } else {
                    result.postValue(Result.error("获取图片列表失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<PageResult<DrawingImageDto>>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<Boolean>> deleteImage(Long imageId) {
        MutableLiveData<Result<Boolean>> result = new MutableLiveData<>();
        
        apiService.deleteImage(imageId).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call,
                                 Response<BaseResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Boolean> baseResponse = response.body();
                    if (baseResponse.getCode() == 0) {
                        result.postValue(Result.success(true));
                    } else {
                        result.postValue(Result.error(baseResponse.getMessage()));
                    }
                } else {
                    result.postValue(Result.error("删除图片失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<Void>> clearSession(Long sessionId) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        
        apiService.clearSession(sessionId).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call,
                                 Response<BaseResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Void> baseResponse = response.body();
                    if (baseResponse.getCode() == 200) {
                        result.postValue(Result.success(null));
                    } else {
                        result.postValue(Result.error(baseResponse.getMessage()));
                    }
                } else {
                    result.postValue(Result.error("清空会话失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<Void>> deleteAllSessions() {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        
        apiService.deleteAllSessions().enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call,
                                 Response<BaseResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Void> baseResponse = response.body();
                    if (baseResponse.getCode() == 200) {
                        result.postValue(Result.success(null));
                    } else {
                        result.postValue(Result.error(baseResponse.getMessage()));
                    }
                } else {
                    result.postValue(Result.error("删除所有会话失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<List<DrawingSampleDto>>> getImageCatList() {
        MutableLiveData<Result<List<DrawingSampleDto>>> result = new MutableLiveData<>();
        
        apiService.getImageCatList().enqueue(new Callback<BaseResponse<List<DrawingSampleDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<DrawingSampleDto>>> call,
                                 Response<BaseResponse<List<DrawingSampleDto>>> response) {
                // 打印完整的请求信息
                Timber.tag("DrawingRepo").d( "=== getImageCatList Request Info ===");
                Timber.tag("DrawingRepo").d( "Request URL: " + response.raw().request().url());
                Timber.tag("DrawingRepo").d( "Request Method: " + response.raw().request().method());
                Timber.tag("DrawingRepo").d( "Request Headers: " + response.raw().request().headers());
                
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<DrawingSampleDto>> baseResponse = response.body();
                    
                    // 打印完整的响应
                    Timber.tag("DrawingRepo").d( "=== getImageCatList API Response ===");
                    Timber.tag("DrawingRepo").d( "Response code: " + baseResponse.getCode());
                    Timber.tag("DrawingRepo").d( "Response message: " + baseResponse.getMessage());
                    Timber.tag("DrawingRepo").d( "Response msg: " + baseResponse.getMsg());
                    
                    List<DrawingSampleDto> data = baseResponse.getData();
                    Timber.tag("DrawingRepo").d( "Response data size: " + (data != null ? data.size() : "null"));
                    
                    if (data != null && !data.isEmpty()) {
                        Timber.tag("DrawingRepo").d( "Total samples: " + data.size());
                        // 打印前3个样本的详细信息
                        int count = Math.min(3, data.size());
                        for (int i = 0; i < count; i++) {
                            DrawingSampleDto sample = data.get(i);
                            Timber.tag("DrawingRepo").d( "Sample " + (i+1) + ":");
                            Timber.tag("DrawingRepo").d( "  - ID: " + sample.getId());
                            Timber.tag("DrawingRepo").d( "  - Prompt: " + sample.getPrompt());
                            Timber.tag("DrawingRepo").d( "  - ImageUrl: " + sample.getImageUrl());
                            Timber.tag("DrawingRepo").d( "  - CatId: " + sample.getCatId());
                            Timber.tag("DrawingRepo").d( "  - CatName: " + sample.getCatName());
                            Timber.tag("DrawingRepo").d( "  - StyleId: " + sample.getStyleId());
                            Timber.tag("DrawingRepo").d( "  - StyleName: " + sample.getStyleName());
                        }
                        if (data.size() > 3) {
                            Timber.tag("DrawingRepo").d( "... and " + (data.size() - 3) + " more samples");
                        }
                    } else {
                        Timber.tag("DrawingRepo").d( "No sample data returned");
                    }
                    
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        Timber.tag("DrawingRepo").e( "API error code: " + baseResponse.getCode() + ", message: " + baseResponse.getMessage());
                        result.postValue(Result.error(baseResponse.getMessage()));
                    }
                } else {
                    Timber.tag("DrawingRepo").e( "Response not successful: " + response.code() + " - " + response.message());
                    Timber.tag("DrawingRepo").e( "Response error body: " + (response.errorBody() != null ? response.errorBody().toString() : "null"));
                    result.postValue(Result.error("获取示例图片失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<List<DrawingSampleDto>>> call, Throwable t) {
                Timber.tag("DrawingRepo").e( "Network request failed: " + t.getMessage());
                Timber.tag("DrawingRepo").e( "Error type: " + t.getClass().getName());
                t.printStackTrace();
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    

    @Override
    public LiveData<Result<PageResult<DrawingImageDto>>> getSessionList(Map<String, Object> params) {
        MutableLiveData<Result<PageResult<DrawingImageDto>>> result = new MutableLiveData<>();
        
        // 将API改为使用getMySessions，返回DrawingSessionDto
        apiService.getMySessions(params, (Integer) (params.get("type")==null?0:params.get("type"))).enqueue(new Callback<BaseResponse<PageResult<DrawingSessionDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<PageResult<DrawingSessionDto>>> call,
                                 Response<BaseResponse<PageResult<DrawingSessionDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<PageResult<DrawingSessionDto>> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        // 转换DrawingSessionDto为DrawingImageDto
                        PageResult<DrawingSessionDto> sessionPageResult = baseResponse.getData();
                        if (sessionPageResult != null) {
                            PageResult<DrawingImageDto> imagePageResult = new PageResult<DrawingImageDto>();
                            imagePageResult.setTotal(sessionPageResult.getTotal());
                            imagePageResult.setPages(sessionPageResult.getPages());
                            imagePageResult.setCurrent(sessionPageResult.getCurrent());
                            imagePageResult.setSize(sessionPageResult.getSize());
                            
                            // 转换会话列表为图片列表
                            List<DrawingImageDto> images = new ArrayList<>();
                            if (sessionPageResult.getRecords() != null) {
                                for (DrawingSessionDto session : sessionPageResult.getRecords()) {
                                    DrawingImageDto image = new DrawingImageDto();
                                    image.setId(session.getId());
                                    image.setSessionId(session.getId());
                                    image.setPrompt(session.getFirstPrompt() != null ? session.getFirstPrompt() : session.getName());
                                    image.setImageUrl(session.getLastImageUrl());
                                    image.setThumbnailUrl(session.getLastImageUrl());
                                    image.setCreateTime(session.getCreateTime());
                                    if(session.getLatestImage() != null){
                                        image.setPicUrl(session.getLatestImage().getPicUrl());
                                        image.setWidth(session.getLatestImage().getWidth());
                                        image.setHeight(session.getLatestImage().getHeight());
                                    }
                                    images.add(image);
                                }
                            }
                            imagePageResult.setRecords(images);
                            result.postValue(Result.success(imagePageResult));
                        } else {
                            result.postValue(Result.success(null));
                        }
                    } else {
                        result.postValue(Result.error(baseResponse.getMessage()));
                    }
                } else {
                    result.postValue(Result.error("获取会话列表失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<PageResult<DrawingSessionDto>>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<DrawingSessionDto>> getSessionDetailById(Long sessionId) {
        MutableLiveData<Result<DrawingSessionDto>> result = new MutableLiveData<>();
        
        apiService.getSessionDetailById(sessionId).enqueue(new Callback<BaseResponse<DrawingSessionDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<DrawingSessionDto>> call,
                                 Response<BaseResponse<DrawingSessionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<DrawingSessionDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMessage()));
                    }
                } else {
                    result.postValue(Result.error("获取会话详情失败"));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<DrawingSessionDto>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<String>> deleteAllSessions(Long sessionId) {
        MutableLiveData<Result<String>> result = new MutableLiveData<>();
         Map<String,Object> map = new HashMap<>();
         map.put("id",sessionId);
        apiService.deleteAllSessions(map).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call,
                                   Response<BaseResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMessage()));
                    }
                } else {
                    result.postValue(Result.error("获取会话详情失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });

        return result;
    }
}