package com.fxzs.lingxiagent.model.ppt.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.ppt.api.PptApiService;
import com.fxzs.lingxiagent.model.ppt.callback.PptStreamCallback;
import com.fxzs.lingxiagent.model.ppt.dto.CoverListRequest;
import com.fxzs.lingxiagent.model.ppt.dto.CoverListResponse;
import com.fxzs.lingxiagent.model.ppt.dto.FileAnalyzeRequest;
import com.fxzs.lingxiagent.model.ppt.dto.OutlineItem;
import com.fxzs.lingxiagent.model.ppt.dto.OutlineRequest;
import com.fxzs.lingxiagent.model.ppt.dto.PptPageResult;
import com.fxzs.lingxiagent.model.ppt.dto.PptProject;
import com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto;
import com.fxzs.lingxiagent.model.ppt.dto.PptTaskCheckRequest;
import com.fxzs.lingxiagent.model.ppt.dto.PptTaskCommitRequest;
import com.fxzs.lingxiagent.model.ppt.dto.PptTaskStatusResponse;
import com.fxzs.lingxiagent.model.ppt.dto.PptTitleRequest;
import com.fxzs.lingxiagent.model.ppt.dto.TaskStatus;
import com.fxzs.lingxiagent.network.RetrofitClient;
import com.fxzs.lingxiagent.network.ZNet.SseApi;
import com.fxzs.lingxiagent.util.NetworkStateManager;
import com.fxzs.lingxiagent.util.RetryManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.StreamJsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * PPT数据访问层
 * 负责处理PPT相关的网络请求和本地数据缓存
 */
public class PptRepository {

    private static PptRepository instance;
    private final PptApiService pptApiService; // PPT生成专用API服务
    private final ScheduledExecutorService scheduler;

    // 缓存数据
    private final MutableLiveData<List<String>> sampleTopics = new MutableLiveData<>();
    private final MutableLiveData<List<CoverListResponse.CoverTemplate>> coverTemplates = new MutableLiveData<>();
    private final MutableLiveData<List<PptProject>> myProjects = new MutableLiveData<>();

    private PptCacheManager cacheManager;
    private NetworkStateManager networkStateManager;
    private RetryManager retryManager;

    // 流式请求管理
    private Disposable outlineStreamDisposable;

    // 当前会话ID缓存
    private String currentSessionId;

    public PptRepository() {
        pptApiService = RetrofitClient.getInstance().createPptService(PptApiService.class);
        scheduler = Executors.newScheduledThreadPool(2);
    }

    /**
     * 初始化缓存管理器和网络状态监听
     */
    public void initCache(android.content.Context context) {
        if (cacheManager == null) {
            cacheManager = PptCacheManager.getInstance(context);
        }
        if (networkStateManager == null) {
            networkStateManager = new NetworkStateManager(context);
            setupNetworkStateListener();
        }
        if (retryManager == null) {
            retryManager = RetryManager.createExponentialBackoff(3, 1000);
        }
    }

    /**
     * 设置网络状态监听器
     */
    private void setupNetworkStateListener() {
        networkStateManager.addNetworkStateListener(new NetworkStateManager.NetworkStateListener() {
            @Override
            public void onNetworkAvailable() {
                Timber.tag("PptRepository").d( "网络连接恢复");
                // 网络恢复时可以重试失败的请求
            }

            @Override
            public void onNetworkLost() {
                Timber.tag("PptRepository").d( "网络连接丢失");
                // 网络丢失时停止重试
                if (retryManager != null) {
                    retryManager.stopRetry();
                }
            }

            @Override
            public void onNetworkChanged(boolean isConnected) {
                Timber.tag("PptRepository").d( "网络状态变化: " + (isConnected ? "已连接" : "已断开"));
            }
        });
    }

    public static synchronized PptRepository getInstance() {
        if (instance == null) {
            instance = new PptRepository();
        }
        return instance;
    }

    /**
     * 生成PPT大纲（流式返回）
     */
    public void generateOutlineStream(OutlineRequest request, PptStreamCallback callback) {
        Timber.tag("PptRepository").d( "generateOutlineStream called with theme: " + request.getTheme());

        // 检查网络连接
        if (networkStateManager != null && !networkStateManager.isNetworkAvailable()) {
            callback.onError("网络连接不可用，请检查网络设置");
            return;
        }

        // 取消之前的流式请求
        if (outlineStreamDisposable != null && !outlineStreamDisposable.isDisposed()) {
            Timber.tag("PptRepository").d( "取消之前的流式请求");
            outlineStreamDisposable.dispose();
        } else {
            Timber.tag("PptRepository").d( "没有需要取消的之前请求");
        }

        callback.onStart();

        // 在生成大纲的同时创建PPT会话
        createPptSession(request.getTheme(), new PptCallback<com.fxzs.lingxiagent.model.ppt.dto.PptSessionCreateResponse>() {
            @Override
            public void onSuccess(com.fxzs.lingxiagent.model.ppt.dto.PptSessionCreateResponse sessionResponse) {
                Timber.tag("PptRepository").d( "PPT会话创建成功，sessionId: " + sessionResponse.getSessionId());
                // 保存sessionId供后续使用
                currentSessionId = sessionResponse.getSessionId();
                // 同时保存到SharedPreferences
                SharedPreferencesUtil.saveString("current_ppt_session_id", currentSessionId);
                // 会话创建成功，继续生成大纲
                proceedWithOutlineGeneration(request, callback);
            }

            @Override
            public void onError(String error) {
                Timber.tag("PptRepository").w( "PPT会话创建失败，但继续生成大纲: " + error);
                // 即使会话创建失败，也继续生成大纲，不影响主要功能
                proceedWithOutlineGeneration(request, callback);
            }
        });
    }

    /**
     * 继续执行大纲生成逻辑
     */
    private void proceedWithOutlineGeneration(OutlineRequest request, PptStreamCallback callback) {
        // 构建请求参数Map
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("theme", request.getTheme());
        if (request.hasFileAnalyseResults()) {
            requestMap.put("fileAnalyseResults", request.getFileAnalyseResults());
        }

        Timber.tag("PptRepository").d( "请求参数: " + requestMap.toString());

        // 使用现有的SSE API
        SseApi sseApi = com.fxzs.lingxiagent.network.ZNet.RetrofitClient.createSseApi();
        Observable<String> sseObservable = parseSSEStream(sseApi.generatePptOutlineStream(requestMap));

        outlineStreamDisposable = sseObservable
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> {
                    Timber.tag("PptRepository").d( "SSE流订阅开始");
                })
                .doOnNext(data -> {
                    Timber.tag("PptRepository").d( "SSE流接收到原始数据: " + data);
                })
                .doOnError(throwable -> {
                    Timber.tag("PptRepository").e( "SSE流发生错误: " + throwable.getMessage(), throwable);
                })
                .doOnComplete(() -> {
                    Timber.tag("PptRepository").d( "SSE流正常完成");
                })
                .doOnDispose(() -> {
                    Timber.tag("PptRepository").d( "SSE流被取消");
                })
                .subscribe(
                        data -> {
                            // 处理接收到的SSE数据
                            Timber.tag("PptRepository").d( "处理SSE数据: " + data);
                            callback.onReceive(data);
                        },
                        throwable -> {
                            // 处理错误
                            Timber.tag("PptRepository").e( "SSE流订阅错误: " + throwable.getMessage(), throwable);
                            PptErrorHandler.ErrorInfo errorInfo = PptErrorHandler.handleError(throwable);
                            String userFriendlyMessage = PptErrorHandler.getUserFriendlyMessage(errorInfo);
                            callback.onError(userFriendlyMessage);
                        },
                        () -> {
                            // 流结束
                            Timber.tag("PptRepository").d( "SSE流订阅完成");
                            callback.onComplete();
                        }
                );
    }

    /**
     * 生成PPT大纲（阻塞式返回，保留兼容性）
     */
    public void generateOutline(OutlineRequest request, PptCallback<String> callback) {
        Call<String> call = pptApiService.generateOutline(request);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("大纲生成失败: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }

    /**
     * 获取封面模板列表
     */
    public void getCoverList(CoverListRequest request, PptCallback<CoverListResponse> callback) {
        Timber.tag("PptRepository").d( "发起getCoverList请求: color=" + request.getColor() + ", style=" + request.getStyle() + ", pageNum=" + request.getPageNum() + ", pageSize=" + request.getPageSize());

        // 检查网络连接
        if (networkStateManager != null && !networkStateManager.isNetworkAvailable()) {
            callback.onError("网络连接不可用，请检查网络设置");
            return;
        }

        Call<BaseResponse<CoverListResponse>> call = pptApiService.getCoverList(request);
        executeWithRetry(call, new PptCallback<BaseResponse<CoverListResponse>>() {
            @Override
            public void onSuccess(BaseResponse<CoverListResponse> response) {
                Timber.tag("PptRepository").d( "getCoverList响应成功");

                if (response != null) {
                    Timber.tag("PptRepository").d( "BaseResponse: success=" + response.isSuccess() + ", message=" + response.getMessage());

                    if (response.isSuccess()) {
                        CoverListResponse data = response.getData();
                        if (data != null) {
                            // 处理嵌套的data结构
                            // 接口返回的结构是: {code: 0, data: {flag: true, code: 0, data: {total: 119, records: [...]}}}
                            // 所以我们需要从 data.getData() 中获取实际的列表数据
                            CoverListResponse.CoverListData actualData = data.getData();
                            if (actualData != null && actualData.getRecords() != null) {
                                Timber.tag("PptRepository").d( "获取到模板数据: records=" + actualData.getRecords().size() + ", total=" + actualData.getTotal());

                                // 创建一个简化的CoverListResponse对象返回给调用者
                                CoverListResponse simplifiedResponse = new CoverListResponse();
                                CoverListResponse.CoverListData simplifiedData = new CoverListResponse.CoverListData();
                                simplifiedData.setTotal(actualData.getTotal());
                                simplifiedData.setRecords(actualData.getRecords());
                                simplifiedData.setPageNum(actualData.getPageNum());
                                simplifiedResponse.setData(simplifiedData);
                                simplifiedResponse.setFlag(data.isFlag());
                                simplifiedResponse.setCode(data.getCode());

                                // 缓存模板数据
                                coverTemplates.postValue(actualData.getRecords());
                                callback.onSuccess(simplifiedResponse);
                            } else {
                                Timber.tag("PptRepository").e( "嵌套的封面模板数据为空");
                                callback.onError("封面模板数据为空");
                            }
                        } else {
                            Timber.tag("PptRepository").e( "封面模板数据为空");
                            callback.onError("封面模板数据为空");
                        }
                    } else {
                        Timber.tag("PptRepository").e( "API返回失败: " + response.getMessage());
                        callback.onError("获取封面模板失败: " + response.getMessage());
                    }
                } else {
                    Timber.tag("PptRepository").e( "响应数据为空");
                    callback.onError("获取封面模板失败: 响应数据为空");
                }
            }

            @Override
            public void onError(String error) {
                Timber.tag("PptRepository").e( "获取封面模板失败: " + error);
                callback.onError(error);
            }
        }, PptErrorHandler.RetryConfig.getDefault());
    }

    /**
     * 获取封面模板列表（简化版本）
     */
    public void getCoverList(String color, String style, int pageNum, int pageSize, PptCallback<CoverListResponse> callback) {
        CoverListRequest request = new CoverListRequest();
        request.setColor(color);
        request.setStyle(style);
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        getCoverList(request, callback);
    }

    /**
     * 提交PPT生成任务（简化版本）
     */
    public void submitPptTask(String coverId, java.util.Map<String, Object> customData, PptCallback<String> callback) {
        submitPptTask(coverId, null, null, customData, callback);
    }

    /**
     * 提交PPT生成任务（完整版本）
     */
    public void submitPptTask(String coverId, String coverUrl, String sessionId, java.util.Map<String, Object> customData, PptCallback<String> callback) {
        PptTaskCommitRequest request = new PptTaskCommitRequest();
        request.setCoverId(coverId);
        request.setCoverUrl(coverUrl);
        request.setSessionId(sessionId);
        request.setCustomData(customData);

        Timber.tag("PptRepository").d( "提交PPT任务 - coverId: " + coverId + ", coverUrl: " + coverUrl + ", sessionId: " + sessionId);
        commitPptTask(request, callback);
    }

    /**
     * 获取当前会话ID
     */
    public String getCurrentSessionId() {
        if (currentSessionId == null) {
            // 从SharedPreferences中恢复
            currentSessionId = SharedPreferencesUtil.getString("current_ppt_session_id", null);
        }
        return currentSessionId;
    }

    /**
     * 查询任务状态（简化版本）
     */
    public void checkTaskStatus(String taskId, PptCallback<TaskStatus> callback) {
        PptTaskCheckRequest request = new PptTaskCheckRequest(taskId);
        Call<BaseResponse<PptTaskStatusResponse>> call = pptApiService.checkPptTask(request);
        call.enqueue(new Callback<BaseResponse<PptTaskStatusResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<PptTaskStatusResponse>> call, Response<BaseResponse<PptTaskStatusResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<PptTaskStatusResponse> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        PptTaskStatusResponse data = baseResponse.getData();
                        if (data != null) {
                            // 转换为TaskStatus对象
                            TaskStatus taskStatus = new TaskStatus();
                            taskStatus.setPptStatus(data.getPptStatus());
                            taskStatus.setPptUrl(data.getPptUrl());
                            taskStatus.setErrMsg(data.getErrMsg());
                            taskStatus.setTotalPages(data.getTotalPages());
                            taskStatus.setDonePages(data.getDonePages());
                            callback.onSuccess(taskStatus);
                        } else {
                            callback.onError("任务状态数据为空");
                        }
                    } else {
                        callback.onError("查询任务状态失败: " + baseResponse.getMessage());
                    }
                } else {
                    callback.onError("查询任务状态失败: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<PptTaskStatusResponse>> call, Throwable t) {
                callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }

    /**
     * 提交PPT生成任务
     */
    public void commitPptTask(PptTaskCommitRequest request, PptCallback<String> callback) {
        Timber.tag("PptRepository").d( "提交PPT生成任务: " + request.toString());
        Call<BaseResponse<String>> call = pptApiService.commitPptTask(request);
        call.enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call, Response<BaseResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        String taskId = baseResponse.getData();
                        if (taskId != null && !taskId.isEmpty()) {
                            // 缓存任务ID
                            SharedPreferencesUtil.saveString("current_ppt_task_id", taskId);
                            callback.onSuccess(taskId);
                        } else {
                            callback.onError("任务ID为空");
                        }
                    } else {
                        callback.onError("提交PPT任务失败: " + baseResponse.getMessage());
                    }
                } else {
                    callback.onError("提交PPT任务失败: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }

    /**
     * 查询PPT任务状态
     */
    public void checkPptTask(String taskId, PptCallback<PptTaskStatusResponse> callback) {
        PptTaskCheckRequest request = new PptTaskCheckRequest(taskId);
        Timber.tag("PptRepository").d( "查询PPT任务状态: " + taskId);
        Call<BaseResponse<PptTaskStatusResponse>> call = pptApiService.checkPptTask(request);
        call.enqueue(new Callback<BaseResponse<PptTaskStatusResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<PptTaskStatusResponse>> call, Response<BaseResponse<PptTaskStatusResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<PptTaskStatusResponse> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        PptTaskStatusResponse data = baseResponse.getData();
                        if (data != null) {
                            callback.onSuccess(data);
                        } else {
                            callback.onError("任务状态数据为空");
                        }
                    } else {
                        callback.onError("查询任务状态失败: " + baseResponse.getMessage());
                    }
                } else {
                    callback.onError("查询任务状态失败: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<PptTaskStatusResponse>> call, Throwable t) {
                callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }



    /**
     * 创建PPT会话
     */
    public void createPptSession(String title, PptCallback<com.fxzs.lingxiagent.model.ppt.dto.PptSessionCreateResponse> callback) {
        Timber.tag("PptRepository").d( "创建PPT会话: " + title);

        // 检查网络连接
        if (networkStateManager != null && !networkStateManager.isNetworkAvailable()) {
            callback.onError("网络连接不可用，请检查网络设置");
            return;
        }

        com.fxzs.lingxiagent.model.ppt.dto.PptSessionCreateRequest request =
            new com.fxzs.lingxiagent.model.ppt.dto.PptSessionCreateRequest(title);

        Call<BaseResponse<Integer>> call = pptApiService.createPptSession(request);

        call.enqueue(new Callback<BaseResponse<Integer>>() {
            @Override
            public void onResponse(Call<BaseResponse<Integer>> call,
                                 Response<BaseResponse<Integer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Integer> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        Integer sessionIdInt = baseResponse.getData();
                        if (sessionIdInt != null) {
                            // 将数字转换为字符串并创建响应对象
                            String sessionId = String.valueOf(sessionIdInt);
                            com.fxzs.lingxiagent.model.ppt.dto.PptSessionCreateResponse sessionResponse =
                                new com.fxzs.lingxiagent.model.ppt.dto.PptSessionCreateResponse(sessionId);
                            sessionResponse.setTitle(title);

                            Timber.tag("PptRepository").d( "PPT会话创建成功，sessionId: " + sessionId);
                            callback.onSuccess(sessionResponse);
                        } else {
                            Timber.tag("PptRepository").e( "PPT会话创建失败: sessionId为空");
                            callback.onError("创建会话失败: sessionId为空");
                        }
                    } else {
                        Timber.tag("PptRepository").e( "PPT会话创建失败: " + baseResponse.getMsg());
                        callback.onError("创建会话失败: " + baseResponse.getMsg());
                    }
                } else {
                    Timber.tag("PptRepository").e( "PPT会话创建请求失败: " + response.message());
                    callback.onError("创建会话失败: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Integer>> call, Throwable t) {
                Timber.tag("PptRepository").e( "PPT会话创建网络请求失败"+ t);
                callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }

    /**
     * 获取PPT会话历史记录
     */
    public void getPptSessionList(PptCallback<List<PptSessionDto>> callback) {
        Timber.tag("PptRepository").d( "获取PPT会话历史记录");
        
        // 无网络时，尝试从本地缓存读取
        if (networkStateManager != null && !networkStateManager.isNetworkAvailable()) {
            try {
                String key = com.fxzs.lingxiagent.model.common.Constants.PREF_HISTORY_CACHE_PPT;
                String json = com.fxzs.lingxiagent.util.SharedPreferencesUtil.getString(key, "");
                if (!android.text.TextUtils.isEmpty(json)) {
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto>>(){}.getType();
                    java.util.List<com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto> cachedList = new com.google.gson.Gson().fromJson(json, listType);
                    if (cachedList != null && !cachedList.isEmpty()) {
                        Timber.tag("PptRepository").d( "PPT session list cache hit, items=" + cachedList.size());
                        callback.onSuccess(cachedList);
                        return;
                    }
                }
                Timber.tag("PptRepository").d( "PPT session list cache miss, empty json");
            } catch (Exception e) {
                Timber.tag("PptRepository").e( "PPT session list cache read error: " + e.getMessage());
            }
            callback.onError("当前无网络且本地无缓存");
            return;
        }
        
        Call<BaseResponse<PptPageResult<PptSessionDto>>> call = pptApiService.getPptSessionList();
        call.enqueue(new Callback<BaseResponse<PptPageResult<PptSessionDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<PptPageResult<PptSessionDto>>> call,
                                 Response<BaseResponse<PptPageResult<PptSessionDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<PptPageResult<PptSessionDto>> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        List<PptSessionDto> data = baseResponse.getData().getList();
                        if (data != null) {
                            // 成功后写入本地缓存
                            try {
                                String key = com.fxzs.lingxiagent.model.common.Constants.PREF_HISTORY_CACHE_PPT;
                                String json = new com.google.gson.Gson().toJson(data);
                                com.fxzs.lingxiagent.util.SharedPreferencesUtil.saveString(key, json);
                                Timber.tag("PptRepository").d( "PPT session list cache saved, items=" + data.size() + ", bytes=" + (json!=null?json.length():0));
                            } catch (Exception e) {
                                Timber.tag("PptRepository").e( "PPT session list cache save error: " + e.getMessage());
                            }
                            callback.onSuccess(data);
                        } else {
                            callback.onError("PPT历史记录数据为空");
                        }
                    } else {
                        callback.onError("获取PPT历史记录失败: " + baseResponse.getMessage());
                    }
                } else {
                    callback.onError("获取PPT历史记录失败: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<PptPageResult<PptSessionDto>>> call, Throwable t) {
                callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }

    /**
     * 轮询PPT任务状态直到完成
     */
    public void pollPptTaskStatus(String taskId, PptCallback<PptTaskStatusResponse> callback) {
        scheduler.scheduleWithFixedDelay(new Runnable() {
            private int retryCount = 0;
            private final int maxRetries = 60; // 最多轮询5分钟（每5秒一次）

            @Override
            public void run() {
                if (retryCount >= maxRetries) {
                    callback.onError("任务轮询超时");
                    return;
                }

                checkPptTask(taskId, new PptCallback<PptTaskStatusResponse>() {
                    @Override
                    public void onSuccess(PptTaskStatusResponse result) {
                        if (result.isDone() || result.isFailed()) {
                            // 任务完成或失败，停止轮询
                            callback.onSuccess(result);
                        } else {
                            // 任务仍在进行中，继续轮询
                            retryCount++;
                        }
                    }

                    @Override
                    public void onError(String error) {
                        retryCount++;
                        if (retryCount >= maxRetries) {
                            callback.onError(error);
                        }
                        // 否则继续轮询
                    }
                });
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * 文档解析
     */
    public void analyzeFile(FileAnalyzeRequest request, PptCallback<String> callback) {
        // 自动检测文件类型
        request.autoDetectFileType();

        Call<String> call = pptApiService.analyzeFile(request);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("文档解析失败: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }

    /**
     * 获取示例主题列表（带缓存）
     */
    public LiveData<List<String>> getSampleTopics() {
        // 先尝试从缓存获取
        if (cacheManager != null) {
            List<String> cachedTopics = cacheManager.getCachedSampleTopics();
            if (!cachedTopics.isEmpty()) {
                sampleTopics.postValue(cachedTopics);
                return sampleTopics;
            }
        }

        // 缓存为空，从网络获取
        if (sampleTopics.getValue() == null) {
            loadSampleTopicsFromNetwork();
        }
        return sampleTopics;
    }

    private void loadSampleTopicsFromNetwork() {
        Call<BaseResponse<List<String>>> call = pptApiService.getSampleTopics();
        call.enqueue(new Callback<BaseResponse<List<String>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<String>>> call, Response<BaseResponse<List<String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<String>> baseResponse = response.body();
                    if (baseResponse.isSuccess() && baseResponse.getData() != null) {
                        List<String> topics = baseResponse.getData();
                        sampleTopics.postValue(topics);
                        // 缓存数据
                        if (cacheManager != null) {
                            cacheManager.cacheSampleTopics(topics);
                        }
                    }
                } else {
                    // 网络失败时使用默认示例主题
                    List<String> defaultTopics = getDefaultSampleTopics();
                    sampleTopics.postValue(defaultTopics);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<String>>> call, Throwable t) {
                // 网络失败时使用默认示例主题
                List<String> defaultTopics = getDefaultSampleTopics();
                sampleTopics.postValue(defaultTopics);
            }
        });
    }

    /**
     * 获取默认示例主题
     */
    private List<String> getDefaultSampleTopics() {
        return Arrays.asList(
                "汽车销售活动运营及策划方案",
                "新人入职培训管理方案",
                "工程项目进度工作总结汇报",
                "共享自行车可行性的研究",
                "个人效率提升：掌握时间管理的秘籍"
        );
    }

    /**
     * 获取缓存的封面模板
     */
    public LiveData<List<CoverListResponse.CoverTemplate>> getCoverTemplates() {
        return coverTemplates;
    }

    /**
     * 获取我的PPT项目列表
     */
    public LiveData<List<PptProject>> getMyProjects() {
        return myProjects;
    }

    /**
     * 带重试机制的网络请求
     */
    private <T> void executeWithRetry(Call<T> call, PptCallback<T> callback, PptErrorHandler.RetryConfig retryConfig) {
        executeWithRetry(call, callback, retryConfig, 0);
    }

    private <T> void executeWithRetry(Call<T> call, PptCallback<T> callback, PptErrorHandler.RetryConfig retryConfig, int currentRetry) {
        // 检查网络状态
        if (networkStateManager != null && !networkStateManager.isNetworkAvailable()) {
            callback.onError("网络连接不可用，请检查网络设置");
            return;
        }

        call.clone().enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    PptErrorHandler.ErrorInfo errorInfo = PptErrorHandler.handleError(
                            new retrofit2.HttpException(response)
                    );

                    if (errorInfo.isRetryable() && currentRetry < retryConfig.getMaxRetries()) {
                        // 延迟后重试
                        long delay = retryConfig.getDelayForRetry(currentRetry);
                        Timber.tag("PptRepository").d( "请求失败，" + delay + "ms后进行第" + (currentRetry + 1) + "次重试");
                        scheduler.schedule(() -> {
                            executeWithRetry(call, callback, retryConfig, currentRetry + 1);
                        }, delay, TimeUnit.MILLISECONDS);
                    } else {
                        String errorMessage = PptErrorHandler.getUserFriendlyMessage(errorInfo);
                        Timber.tag("PptRepository").e( "请求最终失败: " + errorMessage);
                        callback.onError(errorMessage);
                    }
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                PptErrorHandler.ErrorInfo errorInfo = PptErrorHandler.handleError(t);

                if (errorInfo.isRetryable() && currentRetry < retryConfig.getMaxRetries()) {
                    // 延迟后重试
                    long delay = retryConfig.getDelayForRetry(currentRetry);
                    Timber.tag("PptRepository").d( "网络请求失败，" + delay + "ms后进行第" + (currentRetry + 1) + "次重试: " + t.getMessage());
                    scheduler.schedule(() -> {
                        executeWithRetry(call, callback, retryConfig, currentRetry + 1);
                    }, delay, TimeUnit.MILLISECONDS);
                } else {
                    String errorMessage = PptErrorHandler.getUserFriendlyMessage(errorInfo);
                    Timber.tag("PptRepository").e( "网络请求最终失败: " + errorMessage);
                    callback.onError(errorMessage);
                }
            }
        });
    }

    /**
     * 检查网络连接状态
     */
    public boolean isNetworkAvailable() {
        return networkStateManager != null && networkStateManager.isNetworkAvailable();
    }

    /**
     * 获取网络类型
     */
    public String getNetworkType() {
        return networkStateManager != null ? networkStateManager.getNetworkType() : "未知";
    }

    /**
     * 保存当前大纲到缓存
     */
    public void saveCurrentOutline(List<OutlineItem> outline) {
        if (cacheManager != null) {
            cacheManager.cacheCurrentOutline(outline);
        }
    }

    /**
     * 从缓存获取当前大纲
     */
    public List<OutlineItem> getCurrentOutline() {
        if (cacheManager != null) {
            return cacheManager.getCachedCurrentOutline();
        }
        return new ArrayList<>();
    }

    /**
     * 保存选中的模板ID
     */
    public void saveSelectedTemplateId(String templateId) {
        if (cacheManager != null) {
            cacheManager.cacheSelectedTemplateId(templateId);
        }
    }

    /**
     * 获取选中的模板ID
     */
    public String getSelectedTemplateId() {
        if (cacheManager != null) {
            return cacheManager.getCachedSelectedTemplateId();
        }
        return null;
    }

    /**
     * 清除会话缓存
     */
    public void clearSessionCache() {
        if (cacheManager != null) {
            cacheManager.clearSessionCache();
        }
    }

    /**
     * 获取优化后的PPT标题
     * @param input 用户输入的原始主题
     * @param callback 回调接口
     */
    public void getPptTitle(String input, PptCallback<String> callback) {
        Timber.tag("PptRepository").d( "获取优化后的PPT标题: " + input);

        // 检查网络连接
        if (networkStateManager != null && !networkStateManager.isNetworkAvailable()) {
            callback.onError("网络连接不可用，请检查网络设置");
            return;
        }

        // 创建请求对象
        PptTitleRequest request = new PptTitleRequest(input);
        
        Call<BaseResponse<String>> call = pptApiService.getPptTitle(request);
        call.enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call, Response<BaseResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        String optimizedTitle = baseResponse.getData();
                        if (optimizedTitle != null && !optimizedTitle.trim().isEmpty()) {
                            Timber.tag("PptRepository").d( "获取优化标题成功: " + optimizedTitle);
                            callback.onSuccess(optimizedTitle);
                        } else {
                           Timber.tag("PptRepository").w("优化标题为空，使用原始输入");
                            // 如果返回的标题为空，使用原始输入
                            callback.onSuccess(input);
                        }
                    } else {
                        Timber.tag("PptRepository").e( "获取优化标题失败: " + baseResponse.getMessage());
                        // API调用失败时，使用原始输入，不影响用户体验
                        callback.onSuccess(input);
                    }
                } else {
                    Timber.tag("PptRepository").e( "获取优化标题请求失败: " + response.message());
                    // 请求失败时，使用原始输入，不影响用户体验
                    callback.onSuccess(input);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                Timber.tag("PptRepository").e( "获取优化标题网络请求失败: " + t.getMessage());
                // 网络请求失败时，使用原始输入，不影响用户体验
                callback.onSuccess(input);
            }
        });
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        if (networkStateManager != null) {
            networkStateManager.destroy();
            networkStateManager = null;
        }

        if (retryManager != null) {
            retryManager.destroy();
            retryManager = null;
        }

        if (outlineStreamDisposable != null && !outlineStreamDisposable.isDisposed()) {
            outlineStreamDisposable.dispose();
        }
    }

    /**
     * 解析SSE流
     */
    private Observable<String> parseSSEStream(Observable<ResponseBody> responseBodyObservable) {
        Timber.tag("PptRepository").d( "parseSSEStream called");
        return responseBodyObservable
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposable -> {
                    Timber.tag("PptRepository").d( "parseSSEStream 订阅开始");
                })
                .doOnNext(responseBody -> {
                    Timber.tag("PptRepository").d( "parseSSEStream 接收到ResponseBody: " + responseBody);
                })
                .flatMap(responseBody -> Observable.create(emitter -> {
                    Timber.tag("PptRepository").d( "开始解析SSE流，ResponseBody: " + responseBody);
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
                        String line;
                        while (!emitter.isDisposed() && (line = reader.readLine()) != null) {
                            Timber.tag("PptRepository").d( "SSE line: " + line);

                            // 解析SSE数据行
                            String data = StreamJsonParser.parseSSEDataLine(line);
                            if (data != null && !data.isEmpty()) {
                                emitter.onNext(data);
                            }
                        }
                        if (!emitter.isDisposed()) {
                            emitter.onComplete();
                        }
                    } catch (Exception e) {
                        if (!emitter.isDisposed()) {
                            emitter.onError(e);
                        }
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Exception e) {
                                Timber.tag("PptRepository").e( "Error closing reader"+e);
                            }
                        }
                    }
                }));
    }

    /**
     * 取消流式请求
     */
    public void cancelOutlineStream() {
        Timber.tag("PptRepository").d( "cancelOutlineStream called");
        if (outlineStreamDisposable != null && !outlineStreamDisposable.isDisposed()) {
            Timber.tag("PptRepository").d( "取消SSE流订阅");
            outlineStreamDisposable.dispose();
        } else {
            Timber.tag("PptRepository").d( "SSE流订阅已经被取消或为null");
        }
    }

    /**
     * PPT操作回调接口
     */
    public interface PptCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}