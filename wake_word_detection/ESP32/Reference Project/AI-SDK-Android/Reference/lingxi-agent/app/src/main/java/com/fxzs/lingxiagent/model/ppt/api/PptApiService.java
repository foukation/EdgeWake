package com.fxzs.lingxiagent.model.ppt.api;

import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.ppt.dto.CoverListRequest;
import com.fxzs.lingxiagent.model.ppt.dto.CoverListResponse;
import com.fxzs.lingxiagent.model.ppt.dto.FileAnalyzeRequest;
import com.fxzs.lingxiagent.model.ppt.dto.OutlineRequest;
import com.fxzs.lingxiagent.model.ppt.dto.PptPageResult;
import com.fxzs.lingxiagent.model.ppt.dto.PptProject;
import com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto;
import com.fxzs.lingxiagent.model.ppt.dto.PptTaskCheckRequest;
import com.fxzs.lingxiagent.model.ppt.dto.PptTaskCommitRequest;
import com.fxzs.lingxiagent.model.ppt.dto.PptTaskStatusResponse;
import com.fxzs.lingxiagent.model.ppt.dto.PptTitleRequest;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * PPT相关API服务接口
 * 基于联通AI PPT接口文档实现
 */
public interface PptApiService {
    
    /**
     * 生成PPT大纲
     * @param request 大纲生成请求
     * @return 大纲生成响应（流式返回）
     */
    @POST("app-api/lt/ai/ppt/generatePptOutline")
    Call<String> generateOutline(@Body OutlineRequest request);
    
    /**
     * 获取PPT封面模板列表
     * @param request 封面查询请求
     * @return 封面模板列表
     */
    @POST("app-api/lt/ai/xf/ppt/getCoverList")
    Call<BaseResponse<CoverListResponse>> getCoverList(@Body CoverListRequest request);
    
    /**
     * 提交PPT生成任务
     * @param request PPT生成请求
     * @return 任务响应
     */
    @POST("app-api/lt/ai/xf/ppt/ppt/task/commit")
    Call<BaseResponse<String>> commitPptTask(@Body PptTaskCommitRequest request);
    
    /**
     * 查询PPT生成任务状态
     * @param request 任务查询请求
     * @return 任务状态
     */
    @POST("app-api/lt/ai/xf/ppt/ppt/task/check")
    Call<BaseResponse<PptTaskStatusResponse>> checkPptTask(@Body PptTaskCheckRequest request);

    /**
     * 创建PPT会话
     * @param request 会话创建请求
     * @return 会话创建响应，data字段直接返回sessionId数字
     */
    @POST("app-api/lt/ai/ppt/createPptSession")
    Call<BaseResponse<Integer>> createPptSession(@Body com.fxzs.lingxiagent.model.ppt.dto.PptSessionCreateRequest request);

    /**
     * 获取PPT会话历史记录
     * @return PPT会话列表
     */
    @POST("app-api/lt/ai/ppt/getPptSessionList")
    Call<BaseResponse<PptPageResult<PptSessionDto>>> getPptSessionList();
    
    /**
     * 文档解析
     * @param request 文档解析请求
     * @return 解析结果（流式返回）
     */
    @POST("app-api/lt/ai/file/analyse")
    Call<String> analyzeFile(@Body FileAnalyzeRequest request);
    
    // 以下是扩展接口，用于完整的PPT管理功能
    
    /**
     * 获取示例主题列表
     * @return 示例主题列表
     */
    @GET("app-api/lt/ai/ppt/sample-topics")
    Call<BaseResponse<List<String>>> getSampleTopics();
    
    /**
     * 获取用户的PPT项目列表
     * @param page 页码
     * @param size 每页大小
     * @return PPT项目列表
     */
    @GET("app-api/lt/ai/ppt/my-projects")
    Call<BaseResponse<List<PptProject>>> getMyPptProjects(
            @Query("page") int page,
            @Query("size") int size
    );
    
    /**
     * 删除PPT项目
     * @param pptId PPT ID
     * @return 删除结果
     */
//    @DELETE("app-api/lt/ai/ppt/{pptId}")
//    Call<BaseResponse<Void>> deletePpt(@Path("pptId") String pptId);
    /**
     * 删除PPT项目
     * @param pptId PPT ID
     * @return 删除结果
     */
    @POST("app-api/lt/ai/ppt/delPptSession")
    Observable<ApiResponse<Integer>> deletePpt(@Body Map<String, Object> body);

    @POST("app-api/lt/ai/ppt/updatePptSession")
    Observable<ApiResponse<String>>  updatePptSession(@Body Map<String, Object> body);

    /**
     * 获取PPT详情
     * @param pptId PPT ID
     * @return PPT项目详情
     */
    @GET("app-api/lt/ai/ppt/{pptId}")
    Observable<ApiResponse<String>> getPptDetails(@Path("pptId") String pptId);

    /**
     * 获取优化后的PPT标题
     * @param request 标题优化请求
     * @return 优化后的标题
     */
    @POST("app-api/lt/ai/ppt/getPptTitle")
    @Headers({
        "accept: application/json, text/plain, */*",
        "content-type: application/json"
    })
    Call<BaseResponse<String>> getPptTitle(@Body PptTitleRequest request);
}