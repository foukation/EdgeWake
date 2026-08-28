package com.fxzs.lingxiagent.viewmodel.ppt;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.ppt.dto.ChartData;
import com.fxzs.lingxiagent.model.ppt.dto.PptProject;
import com.fxzs.lingxiagent.model.ppt.dto.PptSlide;
import com.fxzs.lingxiagent.model.ppt.repository.PptRepository;
import com.fxzs.lingxiagent.util.PptStateManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.ppt.PptOutlineEditActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

import timber.log.Timber;

public class VMPptPreview extends BaseViewModel {

    private final ObservableField<String> pptTitle = new ObservableField<>("");
    private final MutableLiveData<List<PptSlide>> slides = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> currentSlideIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> downloadInProgress = new MutableLiveData<>(false);
    private final MutableLiveData<String> webViewUrl = new MutableLiveData<>();
    private final MutableLiveData<String> downloadProgress = new MutableLiveData<>("");
    private final MutableLiveData<String> downloadFinish = new MutableLiveData<>();

    private PptRepository pptRepository;
    private ExecutorService executorService;
    private Handler mainHandler;
    private String currentPptUrl;
    private int sessionId;
    private String taskId;
    private String wpsFileId;
    int progress = 0;
    public VMPptPreview(@NonNull Application application) {
        super(application);
        pptRepository = PptRepository.getInstance();
        pptRepository.initCache(application);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void loadPptData(String pptId) {
        loadPptData(pptId, null, null, 0, null);
    }

    public void loadPptData(String pptId, String pptUrl, String topic) {
        loadPptData(pptId, pptUrl, topic, 0, null);
    }

    public void loadPptData(String pptId, String pptUrl, String topic, int sessionId, String taskId) {
        this.sessionId = sessionId;
        this.taskId = taskId;
        setLoading(true);

        Timber.tag("VMPptPreview").d( "加载PPT数据 - pptId: " + pptId + ", pptUrl: " + pptUrl + ", topic: " + topic);

        if (pptUrl != null && !pptUrl.isEmpty()) {
            // 使用真实的PPT URL，切换到WebView预览模式
            currentPptUrl = pptUrl;
            if (topic != null && !topic.isEmpty()) {
                pptTitle.set(topic);
            } else {
                pptTitle.set("PPT预览");
            }

            Timber.tag("VMPptPreview").d( "使用真实PPT URL，切换到WebView模式: " + pptUrl);

            // 直接切换到WebView预览模式
            loadPptFromUrl(pptUrl, topic);
        } else {
            // 没有PPT URL，显示提示信息或使用模拟数据
            Timber.tag("VMPptPreview").d( "没有PPT URL，显示提示信息");

            if (topic != null && !topic.isEmpty()) {
                pptTitle.set(topic);
            } else {
                pptTitle.set("PPT预览");
            }

            // 显示PPT未生成的提示信息
            loadPptNotAvailable(topic);
        }
    }

    private void loadPptFromUrl(String pptUrl, String topic) {
        // 通知Activity切换到WebView预览模式
        webViewUrl.postValue(pptUrl);
        setLoading(false);
    }

    /**
     * 处理PPT未生成的情况
     */
    private void loadPptNotAvailable(String topic) {
        // 创建提示幻灯片
        List<PptSlide> slideList = new ArrayList<>();

        // 创建提示幻灯片
        PptSlide notAvailableSlide = new PptSlide(
            topic != null ? topic : "PPT项目",
            "PPT文档尚未生成完成\n\n• 请等待PPT生成完成后再查看\n• 或返回继续编辑PPT内容",
            PptSlide.SlideType.CONTENT,
            false
        );
        slideList.add(notAvailableSlide);

        slides.postValue(slideList);
        setLoading(false);
    }
    
    private void loadMockPptData() {
        pptTitle.set("运营年终工作总结");
        currentPptUrl = "https://example.com/ppt/sample.pptx"; // Mock URL
        
        List<PptSlide> slideList = new ArrayList<>();
        
        // Cover slide
        slideList.add(new PptSlide(
            "运营年终工作总结",
            "2024年度汇报",
            PptSlide.SlideType.COVER,
            false
        ));
        
        // Section slide
        slideList.add(new PptSlide(
            "01 年度工作回顾",
            null,
            PptSlide.SlideType.SECTION,
            false
        ));
        
        // Content slide with chart
        PptSlide chartSlide = new PptSlide(
            "业绩数据展示",
            "全年完成销售额增长20%，客户满意度提升30%",
            PptSlide.SlideType.CONTENT,
            true
        );
        
        // Create chart data
        ChartData chartData = new ChartData(ChartData.ChartType.PIE, "业绩数据");
        List<ChartData.DataSeries> seriesList = new ArrayList<>();
        
        List<ChartData.DataPoint> dataPoints = new ArrayList<>();
        dataPoints.add(new ChartData.DataPoint("销售增长", 20, "销售增长"));
        dataPoints.add(new ChartData.DataPoint("客户满意度", 30, "客户满意度"));
        
        ChartData.DataSeries series = new ChartData.DataSeries("业绩指标", dataPoints);
        seriesList.add(series);
        chartData.setDataSeries(seriesList);
        
        chartSlide.setChartData(chartData);
        slideList.add(chartSlide);
        
        // Content slide
        slideList.add(new PptSlide(
            "团队建设与培训",
            "• 组织新员工培训5场\n• 完成技能提升培训10次\n• 团队建设活动8次\n• 员工满意度达到95%",
            PptSlide.SlideType.CONTENT,
            false
        ));
        
        // Additional content slides
        PptSlide marketSlide = new PptSlide(
            "市场拓展成果",
            "• 新增客户200+\n• 市场份额提升15%\n• 品牌知名度显著提升\n• 合作伙伴关系稳固",
            PptSlide.SlideType.CONTENT,
            false
        );
        // Add sample image URL for demonstration
        marketSlide.setImageUrl("https://via.placeholder.com/800x600/4CAF50/FFFFFF?text=Market+Growth");
        slideList.add(marketSlide);
        
        slideList.add(new PptSlide(
            "02 未来规划展望",
            null,
            PptSlide.SlideType.SECTION,
            false
        ));
        
        slideList.add(new PptSlide(
            "2025年目标设定",
            "• 销售额增长目标30%\n• 团队规模扩大至50人\n• 新产品线开发3条\n• 客户满意度保持98%以上",
            PptSlide.SlideType.CONTENT,
            false
        ));
        
        slides.postValue(slideList);
        setLoading(false);
    }
    
    private void onPptDataLoaded(PptProject pptProject) {
        pptTitle.set(pptProject.getTitle());
        currentPptUrl = pptProject.getPptUrl();

        if (pptProject.getSlides() != null && !pptProject.getSlides().isEmpty()) {
            slides.postValue(pptProject.getSlides());
        } else {
            // 如果没有幻灯片数据，使用现有的loadPptFromUrl方法
            loadPptFromUrl(pptProject.getPptUrl(), pptProject.getTitle());
            return; // 避免重复设置loading状态
        }
        setLoading(false);
    }


    
    public void setCurrentSlideIndex(int index) {
        currentSlideIndex.postValue(index);
    }
    
    public void downloadPpt(Context context) {
        if (currentPptUrl == null || currentPptUrl.isEmpty()) {
            if (context instanceof android.app.Activity) {
                GlobalToast.show((android.app.Activity) context, "PPT链接不可用", GlobalToast.Type.ERROR);
            }
            return;
        }

        downloadInProgress.postValue(true);

        // 使用全局Toast显示下载中状态
        mainHandler.post(() -> {
            downloadProgress.postValue("下载中...");
            if (context instanceof android.app.Activity) {
                GlobalToast.show((android.app.Activity) context, "正在下载PPT文件...", GlobalToast.Type.NORMAL);
            }
        });

        executorService.execute(() -> {
            try {
                String downloadUrl = fetchDownloadUrlByWpsFileId();
                downloadPptFile(context, downloadUrl);
            } catch (Exception e) {
                mainHandler.post(() -> {
                    downloadInProgress.postValue(false);
                    if (context instanceof android.app.Activity) {
                        GlobalToast.show((android.app.Activity) context, "下载失败: " + e.getMessage(), GlobalToast.Type.ERROR);
                    }
                });
            }
        });
    }
    
    private void downloadPptFile(Context context, String pptUrl) throws IOException {
        URL url = new URL(pptUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("下载地址请求失败，code=" + responseCode);
        }

        int fileLength = connection.getContentLength();
        String fileName = pptTitle.get() + ".pptx";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用MediaStore API
            downloadWithMediaStore(context, connection, fileName, fileLength);
        } else {
            // Android 9及以下使用传统方式
            downloadWithLegacyStorage(context, connection, fileName, fileLength);
        }
    }

    public void fetchWpsFileId(String pptUrl) {
        if (pptUrl == null || pptUrl.isEmpty()) {
            return;
        }

        executorService.execute(() -> {
            try {
                String fileId = requestWpsFileId(pptUrl);
                wpsFileId = fileId;
                Timber.tag("VMPptPreview").d("获取WPS文件ID成功: " + fileId);
            } catch (Exception e) {
                Timber.tag("VMPptPreview").e(e, "获取WPS文件ID失败");
            }
        });
    }

    private String fetchDownloadUrlByWpsFileId() throws Exception {
        String fileId = wpsFileId;
        if (fileId == null || fileId.isEmpty()) {
            if (currentPptUrl == null || currentPptUrl.isEmpty()) {
                throw new IOException("PPT链接不可用");
            }
            fileId = requestWpsFileId(currentPptUrl);
            wpsFileId = fileId;
        }

        return requestWpsFileInfoUrl(fileId);
    }

    private String requestWpsFileId(String pptUrl) throws Exception {
        final String requestUrl = "https://wps.shanghaijimu.com/console/getFileId";
        URL url = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);
        connection.setRequestProperty("accept", "application/json, text/plain, */*");
        connection.setRequestProperty("content-type", "application/json");
        connection.setRequestProperty("authorization", "Bearer " + SharedPreferencesUtil.getToken());

        JSONObject body = new JSONObject();
        body.put("fileUrl", pptUrl);
        String requestBody = body.toString();
        Timber.tag("VMPptPreview").d("[WPS] getFileId request url=%s, body=%s", requestUrl, requestBody);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int code = connection.getResponseCode();
        String response = readHttpResponse(connection, code);
        Timber.tag("VMPptPreview").d("[WPS] getFileId response code=%d, body=%s", code, truncateLog(response));
        if (code < 200 || code >= 300) {
            throw new IOException("获取文件ID失败，code=" + code + ", msg=" + response);
        }

        JSONObject jsonObject = new JSONObject(response);
        String fileId = jsonObject.optString("id");
        if (fileId == null || fileId.isEmpty()) {
            throw new IOException("获取文件ID失败，返回数据无id");
        }

        return fileId;
    }

    private String requestWpsFileInfoUrl(String fileId) throws Exception {
        final String requestUrl = "https://wps.shanghaijimu.com/console/getFileInfo/" + fileId;
        URL url = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("accept", "application/json, text/plain, */*");
        connection.setRequestProperty("authorization", "Bearer " + SharedPreferencesUtil.getToken());

        Timber.tag("VMPptPreview").d("[WPS] getFileInfo request url=%s", requestUrl);

        int code = connection.getResponseCode();
        String response = readHttpResponse(connection, code);
        Timber.tag("VMPptPreview").d("[WPS] getFileInfo response code=%d, body=%s", code, truncateLog(response));
        if (code < 200 || code >= 300) {
            throw new IOException("获取文件下载链接失败，code=" + code + ", msg=" + response);
        }

        JSONObject jsonObject = new JSONObject(response);
        String downloadUrl = jsonObject.optString("url");
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            throw new IOException("获取文件下载链接失败，返回数据无url");
        }

        return downloadUrl;
    }

    private String readHttpResponse(HttpURLConnection connection, int responseCode) throws IOException {
        InputStream inputStream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (inputStream == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String truncateLog(String content) {
        if (content == null) {
            return "null";
        }
        if (content.length() <= 1000) {
            return content;
        }
        return content.substring(0, 1000) + "...<truncated>";
    }
    
    /**
     * Android 10+ 使用MediaStore API下载
     */
    private void downloadWithMediaStore(Context context, HttpURLConnection connection, 
                                       String fileName, int fileLength) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LingXi_PPT");
        
        Timber.tag("VMPptPreview").d( "使用MediaStore下载文件: " + fileName);
        
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            Timber.tag("VMPptPreview").d( "MediaStore URI创建成功: " + uri);
            try (InputStream input = connection.getInputStream();
                 OutputStream output = resolver.openOutputStream(uri)) {
                
                if (output != null) {
                    copyStreamSimple(input, output);
                    
                    Timber.tag("VMPptPreview").d( "MediaStore文件下载完成");
                    // Android 10+使用MediaStore，实际文件路径不可直接访问
                    // 传递特殊标识符表示使用MediaStore下载
                    notifyDownloadComplete(context, "MEDIASTORE:/LingXi_PPT");
                } else {
                    throw new IOException("无法打开MediaStore输出流");
                }
            }
        } else {
            throw new IOException("无法创建MediaStore条目");
        }
    }
    
    /**
     * Android 9及以下使用传统存储方式
     */
    private void downloadWithLegacyStorage(Context context, HttpURLConnection connection,
                                          String fileName, int fileLength) throws IOException {
        // Create download directory
        File downloadDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS), "LingXi_PPT");
        if (!downloadDir.exists()) {
            boolean created = downloadDir.mkdirs();
            Timber.tag("VMPptPreview").d( "创建目录: " + downloadDir.getAbsolutePath() + ", 成功: " + created);
        }
        
        // Create file
        File outputFile = new File(downloadDir, fileName);
        Timber.tag("VMPptPreview").d( "创建文件: " + outputFile.getAbsolutePath());
        
        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(outputFile)) {
            
            copyStreamSimple(input, output);
            
            // 验证文件是否成功创建
            if (outputFile.exists() && outputFile.length() > 0) {
                Timber.tag("VMPptPreview").d( "文件下载成功: " + outputFile.getAbsolutePath() + ", 大小: " + outputFile.length());
                notifyDownloadComplete(context, outputFile.getParent());
            } else {
                Timber.tag("VMPptPreview").e( "文件下载失败或文件为空: " + outputFile.getAbsolutePath());
                throw new IOException("文件下载失败或文件为空");
            }
        }
    }
    
    /**
     * 简化的流复制（不显示实时进度）
     */
    private void copyStreamSimple(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192]; // 增大缓冲区提高性能
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
    }
    
    /**
     * 统一的下载完成通知
     */
    private void notifyDownloadComplete(Context context, String downloadPath) {
        Timber.tag("VMPptPreview").d( "下载完成，路径: " + downloadPath);
        mainHandler.post(() -> {
            downloadInProgress.postValue(false);
            downloadProgress.postValue("下载完成");
            downloadFinish.postValue(downloadPath);
            
            // 使用全局Toast显示完成提示
//            Toast.makeText(context, "PPT下载完成", Toast.LENGTH_SHORT).show();
        });
    }
    
    public void sharePpt(Context context) {
        if (currentPptUrl == null || currentPptUrl.isEmpty()) {
            Toast.makeText(context, "PPT链接不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, pptTitle.get());
        shareIntent.putExtra(Intent.EXTRA_TEXT, 
            "分享PPT: " + pptTitle.get() + "\n链接: " + currentPptUrl);
        
        Intent chooser = Intent.createChooser(shareIntent, "分享PPT");
        if (chooser.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(chooser);
        } else {
            if (context instanceof android.app.Activity) {
                GlobalToast.show((android.app.Activity) context, "没有找到可用的分享应用", GlobalToast.Type.ERROR);
            }
        }
    }
    
    public void openPptForEdit(Context context, String pptId) {
        // Navigate back to outline edit activity
        Intent intent = new Intent(context,
            PptOutlineEditActivity.class);
        intent.putExtra(PptStateManager.EXTRA_PPT_ID, pptId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
    
    public ObservableField<String> getPptTitle() {
        return pptTitle;
    }
    
    public MutableLiveData<List<PptSlide>> getSlides() {
        return slides;
    }
    
    public MutableLiveData<Integer> getCurrentSlideIndex() {
        return currentSlideIndex;
    }
    
    public MutableLiveData<Boolean> getDownloadInProgress() {
        return downloadInProgress;
    }
    
    public MutableLiveData<String> getDownloadProgress() {
        return downloadProgress;
    }

    public MutableLiveData<String> getDownloadFinish() {
        return downloadFinish;
    }


    public MutableLiveData<String> getWebViewUrl() {
        return webViewUrl;
    }
    
    public int getSessionId() {
        return sessionId;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}