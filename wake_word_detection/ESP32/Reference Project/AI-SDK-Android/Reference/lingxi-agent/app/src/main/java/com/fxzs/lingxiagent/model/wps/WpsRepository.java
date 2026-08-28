package com.fxzs.lingxiagent.model.wps;

import android.content.Context;
import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.model.preview.FilePreviewRepository;
import com.fxzs.lingxiagent.view.aifile.AiFileToolTypes;
import com.fxzs.lingxiagent.util.ZUtil.SessionUpload;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class WpsRepository {

    private static final String TAG = "WpsRepository";

    public interface WpsCallback {
        void onUploadProgress(long percent);
        void onTaskCreated(String taskId);
        void onTaskStatusChanged(String status);
        void onSuccess(ArrayList<String> urls,boolean isImage);
//        void onSuccess(WpsTaskResponse.Result result);
        void onError(String message);
    }

    public interface PreviewCallback {
        void onProgress(String status);
        void onSuccess(String localPdfPath);
        void onError(String message);
    }

    private final CompositeDisposable disposables = new CompositeDisposable();
    private final FilePreviewRepository previewRepository = new FilePreviewRepository();
    private final OkHttpClient httpClient = new OkHttpClient();

    public void startConversion(Context context, String filePath, int toolType, @Nullable List<Integer> pages, WpsCallback callback) {
        Timber.tag(TAG).d("Starting conversion: filePath=%s, toolType=%d", filePath, toolType);

        SessionUpload.upload(context, filePath, new com.fxzs.lingxiagent.model.chat.callback.StsCallback() {
            @Override
            public void progress(long percent) {
                callback.onUploadProgress(percent);
            }

            @Override
            public void callback(String url) {
                if (url == null || url.isEmpty()) {
                    Timber.tag(TAG).e("Upload finished but URL is empty.");
                    callback.onError("文件上传失败，URL为空");
                    return;
                }
                Timber.tag(TAG).d("Upload success: %s", url);
                submitWpsTask(context, url, filePath, toolType, pages, callback);
            }

            @Override
            public void error(@Nullable CosXmlClientException clientException, @Nullable CosXmlServiceException serviceException) {
                String errorMsg = "文件上传失败";
                if (clientException != null) {
                    errorMsg += ": " + clientException.getMessage();
                }
                if (serviceException != null) {
                    errorMsg += ": " + serviceException.getMessage();
                }
                Timber.tag(TAG).e(errorMsg);
                callback.onError(errorMsg);
            }
        });
    }

    public void generatePdfForPreview(Context context, String filePath, PreviewCallback callback) {
        int convertToPdfType;
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".doc") || lowerPath.endsWith(".docx")) {
            convertToPdfType = AiFileToolTypes.TOOL_WORD_TO_PDF;
        } else if (lowerPath.endsWith(".ppt") || lowerPath.endsWith(".pptx")) {
            convertToPdfType = AiFileToolTypes.TOOL_PPT_TO_PDF;
        } else {
            callback.onError("不支持的文件类型");
            return;
        }
        SessionUpload.upload(context, filePath, new com.fxzs.lingxiagent.model.chat.callback.StsCallback() {
            @Override
            public void progress(long percent) {
//                callback.onUploadProgress(percent);
                callback.onProgress(percent+"%");
            }

            @Override
            public void callback(String url) {
                if (url == null || url.isEmpty()) {
                    Timber.tag(TAG).e("Upload finished but URL is empty.");
                    callback.onError("文件上传失败，URL为空");
                    return;
                }
                Timber.tag(TAG).d("Upload success: %s", url);
//                submitWpsTask(context, url, filePath, toolType, pages, callback);
                WpsConvertRequest request = new WpsConvertRequest();

//                if (toolType == AiFileToolTypes.TOOL_IMG_TO_WORD || toolType == AiFileToolTypes.TOOL_IMG_TO_PPT) {
//                    request.imgUrls = Collections.singletonList(url);
//                } else {
                request.url = url;
                request.filename = new File(filePath).getName();
//                }

//                if (pages != null && !pages.isEmpty()) {
//                    request.pages = pages.stream().map(String::valueOf).collect(Collectors.joining(","));
//                }

                Observable<WpsTaskResponse> apiCall = getApiCall(convertToPdfType, request);
                if (apiCall == null) {
                    callback.onError("不支持的转换类型: " + convertToPdfType);
                    return;
                }

                disposables.add(apiCall
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            if (response != null && response.data != null && response.data.taskId != null) {
                                String taskId = response.data.taskId;
                                Timber.tag(TAG).d("Task created: %s", taskId);
//                                callback.onTaskCreated(taskId);
                                pollTaskStatusPdf(context, taskId, convertToPdfType, callback);
                            } else {
                                callback.onError("提交转换任务失败");
                            }
                        }, throwable -> {
                            Timber.tag(TAG).e(throwable, "Submit task failed");
                            callback.onError("提交转换任务失败: " + throwable.getMessage());
                        }));
            }

            @Override
            public void error(@Nullable CosXmlClientException clientException, @Nullable CosXmlServiceException serviceException) {
                String errorMsg = "文件上传失败";
                if (clientException != null) {
                    errorMsg += ": " + clientException.getMessage();
                }
                if (serviceException != null) {
                    errorMsg += ": " + serviceException.getMessage();
                }
                Timber.tag(TAG).e(errorMsg);
                callback.onError(errorMsg);
            }
        });

//        startConversion(context, filePath, convertToPdfType, null, new WpsCallback() {
//            @Override
//            public void onUploadProgress(long percent) {
//                callback.onProgress("上传中: " + percent + "%");
//            }
//
//            @Override
//            public void onTaskCreated(String taskId) {
//                callback.onProgress("已提交转换任务");
//            }
//
//            @Override
//            public void onTaskStatusChanged(String status) {
//                callback.onProgress("转换中: " + status);
//            }
//
//            @Override
//            public void onSuccess(ArrayList<String> urls,boolean isImage) {
//                String downloadUrl = null;
//                if (urls != null && !urls.isEmpty()) {
//                    downloadUrl = urls.get(0);
//                }
//
//                if (downloadUrl == null || downloadUrl.isEmpty()) {
//                    callback.onError("生成预览失败: 未获取到PDF链接");
//                    return;
//                }
//
//                String finalDownloadUrl = downloadUrl;
//                new Thread(() -> {
//                    File downloadedPdf = downloadFile(context, finalDownloadUrl);
//                    if (downloadedPdf != null) {
//                        callback.onSuccess(downloadedPdf.getAbsolutePath());
//                    } else {
//                        callback.onError("生成预览失败: 下载PDF失败");
//                    }
//                }).start();
//            }
//
//            @Override
//            public void onError(String message) {
//                callback.onError(message);
//            }
//        });
    }

    private File downloadFile(Context context, String url) {
        try {
            Request request = new Request.Builder().url(url).build();
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful() || response.body() == null) {
                Timber.tag(TAG).e("Download failed: %s", response.message());
                return null;
            }

            File dir = new File(context.getCacheDir(), "preview_cache");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName;
            try {
                int lastSlash = url.lastIndexOf('/');
                int question = url.indexOf('?');
                if (lastSlash >= 0) {
                    if (question > lastSlash) {
                        fileName = url.substring(lastSlash + 1, question);
                    } else {
                        fileName = url.substring(lastSlash + 1);
                    }
                } else {
                    fileName = url;
                }
            } catch (Exception e) {
                Timber.tag(TAG).e(e, "parse fileName failed, fallback to default name");
                fileName = "downloaded_file";
            }
            File file = new File(dir, fileName);

            try (InputStream in = response.body().byteStream();
                 OutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
            return file;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Download exception");
            return null;
        }
    }

    private void submitWpsTask(Context context, String fileUrl, String filePath, int toolType, @Nullable List<Integer> pages, WpsCallback callback) {
        WpsConvertRequest request = new WpsConvertRequest();

        if (toolType == AiFileToolTypes.TOOL_IMG_TO_WORD || toolType == AiFileToolTypes.TOOL_IMG_TO_PPT) {
            request.imgUrls = Collections.singletonList(fileUrl);
        } else {
            request.url = fileUrl;
            request.filename = new File(filePath).getName();
        }

        // 如果是 OCR 且源文件不是图片（例如 PDF -> WORD/PPT），并且指定了页码，
        // 走一条“先转图片，再用 ocrImgToDocs”的链路，这样就可以利用 pages 字段做自定义页码。
        if (
            // isOcrTask(toolType)
            //     && 
//                toolType != AiFileToolTypes.TOOL_IMG_TO_WORD
//                && toolType != AiFileToolTypes.TOOL_IMG_TO_PPT
//                && toolType != AiFileToolTypes.TOOL_WORD_TO_IMG
//                && toolType != AiFileToolTypes.TOOL_PPT_TO_IMG
//                && toolType != AiFileToolTypes.TOOL_PDF_TO_IMG
                (toolType == AiFileToolTypes.TOOL_WORD_TO_PDF ||
                        toolType == AiFileToolTypes.TOOL_PPT_TO_PDF||
                        toolType == AiFileToolTypes.TOOL_PDF_TO_WORD||
                        toolType == AiFileToolTypes.TOOL_PDF_TO_PPT )
                && pages != null && !pages.isEmpty()) {
            startOcrViaImageChain(context, fileUrl, filePath, toolType, pages, callback);
            return;
        }else if (toolType == AiFileToolTypes.TOOL_WORD_TO_IMG ||
                toolType == AiFileToolTypes.TOOL_PPT_TO_IMG ||
                toolType == AiFileToolTypes.TOOL_PDF_TO_IMG) {
            request.pages = pages.stream().map(String::valueOf).collect(Collectors.joining(","));
        }

//        if (pages != null && !pages.isEmpty()) {
//            if (isOcrTask(toolType)) {
//                request.pageNumBegin = pages.get(0);
//                request.pageNumEnd = pages.get(pages.size() - 1);
//            } else if (toolType == AiFileToolTypes.TOOL_WORD_TO_IMG ||
//                       toolType == AiFileToolTypes.TOOL_PPT_TO_IMG ||
//                       toolType == AiFileToolTypes.TOOL_PDF_TO_IMG) {
//                request.pages = pages.stream().map(String::valueOf).collect(Collectors.joining(","));
//            } else {
//                request.fromPage = pages.get(0);
//                request.toPage = pages.get(pages.size() - 1);
//            }
//        }

        Observable<WpsTaskResponse> apiCall = getApiCall(toolType, request);
        if (apiCall == null) {
            callback.onError("不支持的转换类型: " + toolType);
            return;
        }

        disposables.add(apiCall
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response != null && response.data != null && response.data.taskId != null) {
                        String taskId = response.data.taskId;
                        Timber.tag(TAG).d("Task created: %s", taskId);
                        callback.onTaskCreated(taskId);
                        pollTaskStatus(context, taskId, toolType, callback,2);
                    } else {
                        callback.onError("提交转换任务失败");
                    }
                }, throwable -> {
                    Timber.tag(TAG).e(throwable, "Submit task failed");
                    callback.onError("提交转换任务失败: " + throwable.getMessage());
                }));
    }

    /**
     * 针对 PDF 等非图片的 OCR：先按指定 pages 转成图片，再走 ocrImgToDocs。
     */
    private void startOcrViaImageChain(Context context,
                                       String fileUrl,
                                       String filePath,
                                       int toolType,
                                       List<Integer> pages,
                                       WpsCallback callback) {
        Timber.tag(TAG).d("startOcrViaImageChain: fileUrl=%s, toolType=%d, pages=%s",
                fileUrl, toolType, pages);

        // 第一步：按页把 PDF/Office 转成 PNG 图片
        WpsConvertRequest imgRequest = new WpsConvertRequest();
        imgRequest.url = fileUrl;
        imgRequest.filename = new File(filePath).getName();
        imgRequest.pages = pages.stream().map(String::valueOf).collect(Collectors.joining(","));

        WpsApiService service = WpsApiClient.getApiService();

        disposables.add(service.convertToPng(imgRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response != null && response.data != null && response.data.taskId != null) {
                        String imgTaskId = response.data.taskId;
                        Timber.tag(TAG).d("Image convert task created: %s", imgTaskId);
                        callback.onTaskCreated(imgTaskId);
                        pollImageTaskForOcr(context, imgTaskId, filePath, toolType, callback);
                    } else {
                        callback.onError("提交图片转换任务失败");
                    }
                }, throwable -> {
                    Timber.tag(TAG).e(throwable, "Submit image convert task failed");
                    callback.onError("提交图片转换任务失败: " + throwable.getMessage());
                }));
    }

    /**
     * 轮询图片转换任务，完成后拿到 imagesURL，再发起 ocrImgToDocs。
     */
    private void pollImageTaskForOcr(Context context,
                                     String imgTaskId,
                                     String filePath,
                                     int toolType,
                                     WpsCallback callback) {
        Timber.tag(TAG).d("Polling image task for OCR: %s", imgTaskId);

        Observable<WpsTaskResponse> apiCall =
                WpsApiClient.getApiService().getConvertTaskStatus(imgTaskId);

        disposables.add(apiCall
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen(completed -> completed.delay(3, TimeUnit.SECONDS))
                .filter(response -> isTaskFinished(response.data))
                .take(1)
                .subscribe(response -> {
                    if (!isTaskSuccessful(response.data)) {
                        String errorMessage = response.data != null ? response.data.message : "未知错误";
                        Timber.tag(TAG).e("Image task failed: %s, message: %s", imgTaskId, errorMessage);
                        callback.onError("图片转换任务失败: " + errorMessage);
                        return;
                    }

                    // 拿到图片 URL 列表
                    List<String> imgUrls = new ArrayList<>();
                    if (response.data != null &&
                            response.data.result != null &&
                            response.data.result.images != null) {
                        for (WpsTaskResponse.UrlItem item : response.data.result.images) {
                            if (item != null && item.url != null && !item.url.isEmpty()) {
                                imgUrls.add(item.url);
                            }
                        }
                    }

                    if (imgUrls.isEmpty()) {
                        callback.onError("图片转换任务成功，但未获取到图片链接");
                        return;
                    }

                    // 第二步：根据不同 toolType 走不同链路
                    // 1）如果是 Word/PPT 转 PDF：图片 -> Word/PPT -> PDF
                    if (toolType == AiFileToolTypes.TOOL_WORD_TO_PDF ||
                            toolType == AiFileToolTypes.TOOL_PPT_TO_PDF) {
                        startImgToOfficeThenPdf(context, imgUrls, toolType, callback);
                        return;
                    }

                    // 2）其它情况：图片 -> 文档（ocrImgToDocs），结果由原有 pollTaskStatus + handleSuccessfulConversion 处理
                    String officeType = getOfficeTypeForImageOcr(toolType);
                    if (officeType == null) {
                        callback.onError("不支持的 OCR 目标类型");
                        return;
                    }

                    WpsConvertRequest ocrRequest = new WpsConvertRequest();
                    ocrRequest.imgUrls = imgUrls;
                    ocrRequest.filename = new File(filePath).getName();

                    Timber.tag(TAG).d("Start ocrImgToDocs with %d images, officeType=%s",
                            imgUrls.size(), officeType);

                    disposables.add(WpsApiClient.getApiService()
                                    .ocrImgToDocs(ocrRequest, officeType)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ocrResp -> {
                                if (ocrResp != null && ocrResp.data != null && ocrResp.data.taskId != null) {
                                    String ocrTaskId = ocrResp.data.taskId;
                                    Timber.tag(TAG).d("OCR task created from images: %s", ocrTaskId);
                                    callback.onTaskCreated(ocrTaskId);
                                    // 后续 OCR 结果查询仍然复用通用轮询逻辑
                                    pollTaskStatus(context, ocrTaskId, toolType, callback,0);
                                } else {
                                    callback.onError("提交 OCR 任务失败");
                                }
                            }, throwable -> {
                                Timber.tag(TAG).e(throwable, "Submit OCR task failed");
                                callback.onError("提交 OCR 任务失败: " + throwable.getMessage());
                            }));
                }, throwable -> {
                    Timber.tag(TAG).e(throwable, "Polling image task failed");
                    callback.onError("查询图片转换任务状态失败: " + throwable.getMessage());
                }));
    }

    /**
     * Word/PPT 转 PDF 的特殊链路：
     * images -> office/img/convert/to/{docx|pptx} -> /office/convert/to/pdf
     */
    private void startImgToOfficeThenPdf(Context context,
                                         List<String> imgUrls,
                                         int toolType,
                                         WpsCallback callback) {
        String officeType = (toolType == AiFileToolTypes.TOOL_WORD_TO_PDF) ? "docx" : "pptx";

        WpsConvertRequest ocrRequest = new WpsConvertRequest();
        ocrRequest.imgUrls = imgUrls;
        // 这里 filename 主要用于 WPS 生成目标文件名，可简单给个占位
        ocrRequest.filename = "image_to_office." + officeType;

        Timber.tag(TAG).d("startImgToOfficeThenPdf: images=%d, officeType=%s",
                imgUrls.size(), officeType);

        WpsApiService service = WpsApiClient.getApiService();

        // 第一步：images -> Word/PPT
        disposables.add(service.ocrImgToDocs(ocrRequest, officeType)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ocrResp -> {
                    if (ocrResp != null && ocrResp.data != null && ocrResp.data.taskId != null) {
                        String officeTaskId = ocrResp.data.taskId;
                        Timber.tag(TAG).d("Img->Office task created: %s", officeTaskId);
                        callback.onTaskCreated(officeTaskId);

                        // 轮询 Img->Office 任务（用 getOcrTaskStatus）
                        disposables.add(service.getOcrTaskStatus(officeType, officeTaskId)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .repeatWhen(done -> done.delay(3, TimeUnit.SECONDS))
                                .filter(r -> isTaskFinished(r.data))
                                .take(1)
                                .subscribe(r -> {
                                    if (!isTaskSuccessful(r.data)) {
                                        String err = r.data != null ? r.data.message : "未知错误";
                                        Timber.tag(TAG).e("Img->Office task failed: %s, msg=%s",
                                                officeTaskId, err);
                                        callback.onError("图片转文档失败: " + err);
                                        return;
                                    }

                                    String officeUrl = extractDownloadUrl(r.data);
                                    if (officeUrl == null || officeUrl.isEmpty()) {
                                        callback.onError("图片转文档成功，但未获取到文档链接");
                                        return;
                                    }

                                    Timber.tag(TAG).d("Img->Office success, officeUrl=%s", officeUrl);

                                    // 第二步：文档 -> PDF
                                    WpsConvertRequest pdfReq = new WpsConvertRequest();
                                    pdfReq.url = officeUrl;
                                    pdfReq.filename = "office_to_pdf." +
                                            (toolType == AiFileToolTypes.TOOL_WORD_TO_PDF ? "docx" : "pptx");

                                    disposables.add(service.convertToPdf(pdfReq)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(pdfResp -> {
                                                if (pdfResp != null && pdfResp.data != null && pdfResp.data.taskId != null) {
                                                    String pdfTaskId = pdfResp.data.taskId;
                                                    Timber.tag(TAG).d("Office->PDF task created: %s", pdfTaskId);
                                                    callback.onTaskCreated(pdfTaskId);
                                                    // 这里直接复用通用轮询 + 结果处理逻辑
                                                    pollTaskStatus(context, pdfTaskId, toolType, callback,1);
                                                } else {
                                                    callback.onError("提交 PDF 转换任务失败");
                                                }
                                            }, throwable -> {
                                                Timber.tag(TAG).e(throwable, "Submit Office->PDF task failed");
                                                callback.onError("提交 PDF 转换任务失败: " + throwable.getMessage());
                                            }));
                                }, throwable -> {
                                    Timber.tag(TAG).e(throwable, "Polling Img->Office task failed");
                                    callback.onError("查询图片转文档任务状态失败: " + throwable.getMessage());
                                }));
                    } else {
                        callback.onError("提交图片转文档任务失败");
                    }
                }, throwable -> {
                    Timber.tag(TAG).e(throwable, "Submit Img->Office task failed");
                    callback.onError("提交图片转文档任务失败: " + throwable.getMessage());
                }));
    }

    private void pollTaskStatus(Context context, String taskId, int toolType, WpsCallback callback,int index) {
        callback.onTaskStatusChanged("polling");
        Timber.tag(TAG).d("Polling task: %s", taskId);

        String officeType = getOfficeTypeForOcr(toolType);
        Observable<WpsTaskResponse> apiCall = isImageTask(toolType) && officeType != null
                ? WpsApiClient.getApiService().getOcrTaskStatus(officeType, taskId)
                : WpsApiClient.getApiService().getConvertTaskStatus(taskId);

        disposables.add(apiCall
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen(completed -> completed.delay(3, TimeUnit.SECONDS))
                .filter(response -> isTaskFinished(response.data))
                .take(1)
                .subscribe(response -> {
                    if (isTaskSuccessful(response.data)) {
                        Timber.tag(TAG).d("1 => Task finished: %s   %s", taskId,index);
                        if(index == 0 || index == 1 || AiFileToolTypes.isImageFlow(toolType)){
                            handleSuccessfulConversion(context, response.data,toolType, callback);
                        }else{

                            if (response.data.result != null && response.data.result.images != null && !response.data.result.images.isEmpty()) {
                                ArrayList<String> urls = new ArrayList<>();
                                for (WpsTaskResponse.UrlItem item : response.data.result.images) {
                                    urls.add(item.url);
                                }
                                callback.onSuccess(urls, true);

                            }
//                            if (toolType == AiFileToolTypes.TOOL_IMG_TO_WORD || toolType == AiFileToolTypes.TOOL_IMG_TO_PPT) {
//                if (hasUrlFileSuffix(downloadUrl)) {
//                    ArrayList<String> urls = new ArrayList<>();
//                    urls.add(downloadUrl);
//                    callback.onSuccess(urls, true);
                        }
                    } else {
                        String errorMessage = response.data != null ? response.data.message : "未知错误";
                        Timber.tag(TAG).e("Task failed: %s, message: %s", taskId, errorMessage);
                        callback.onError("转换任务失败: " + errorMessage);
                    }
                }, throwable -> {
                    Timber.tag(TAG).e(throwable, "Polling failed");
                    callback.onError("查询任务状态失败: " + throwable.getMessage());
                }));
    }
    private void pollTaskStatusPdf(Context context, String taskId, int toolType, PreviewCallback callback) {
        callback.onProgress("polling");
        Timber.tag(TAG).d("Polling task: %s", taskId);

        String officeType = getOfficeTypeForOcr(toolType);
        Observable<WpsTaskResponse> apiCall = isOcrTask(toolType) && officeType != null
                ? WpsApiClient.getApiService().getOcrTaskStatus(officeType, taskId)
                : WpsApiClient.getApiService().getConvertTaskStatus(taskId);

        disposables.add(apiCall
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen(completed -> completed.delay(3, TimeUnit.SECONDS))
                .filter(response -> isTaskFinished(response.data))
                .take(1)
                .subscribe(response -> {
                    if (isTaskSuccessful(response.data)) {
                        Timber.tag(TAG).d("Task finished: %s", taskId);
                        callback.onProgress("正在下载文件...");

                        String downloadUrl = extractDownloadUrl(response.data);
                        disposables.add(Observable.fromCallable(() -> downloadFile(context, downloadUrl))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(downloadedFile -> {
                                    if (downloadedFile == null) {
                                        callback.onError("下载转换结果失败");
                                        return;
                                    }
                                    Timber.tag(TAG).d("File downloaded to: %s", downloadedFile.getAbsolutePath());
                                    callback.onSuccess(downloadedFile.getAbsolutePath());
                                }, throwable -> {
                                    Timber.tag(TAG).e(throwable, "Download failed");
                                    callback.onError("下载转换结果失败: " + throwable.getMessage());
                                }));

                    } else {
                        String errorMessage = response.data != null ? response.data.message : "未知错误";
                        Timber.tag(TAG).e("Task failed: %s, message: %s", taskId, errorMessage);
                        callback.onError("转换任务失败: " + errorMessage);
                    }
                }, throwable -> {
                    Timber.tag(TAG).e(throwable, "Polling failed");
                    callback.onError("查询任务状态失败: " + throwable.getMessage());
                }));
    }

    private void handleSuccessfulConversion(Context context, WpsTaskResponse.Data data, int toolType,WpsCallback callback) {
        String downloadUrl = extractDownloadUrl(data);

        Timber.tag(TAG).d( "extractDownloadUrl ="+downloadUrl);
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            callback.onError("转换成功，但未找到下载链接");
            return;
        }
//            if (data.result != null && data.result.images != null && !data.result.images.isEmpty()) {
            if (toolType == AiFileToolTypes.TOOL_IMG_TO_WORD || toolType == AiFileToolTypes.TOOL_IMG_TO_PPT) {
//                if (hasUrlFileSuffix(downloadUrl)) {
//                    ArrayList<String> urls = new ArrayList<>();
//                    urls.add(downloadUrl);
//                    callback.onSuccess(urls, true);
//                } else {
                    callback.onTaskStatusChanged("正在处理结果文件...");
                    reUploadDownloadUrl(context, downloadUrl, callback);
//                toolType == AiFileToolTypes.TOOL_PDF_TO_WORD||
//                        toolType == AiFileToolTypes.TOOL_PDF_TO_PPT
//                }
            } else {

                previewRepository.generatePreview(context, downloadUrl, new FilePreviewRepository.PreviewCallback() {
                    @Override
                    public void onProgress(String status) {
                        callback.onTaskStatusChanged(status);
                    }

                    @Override
                    public void onSuccess(String url) {
                        ArrayList<String> urls = new ArrayList<>();
                        urls.add(url);
                        callback.onSuccess(urls,false);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }

    }

    private void reUploadDownloadUrl(Context context, String downloadUrl, WpsCallback callback) {
        disposables.add(Observable.fromCallable(() -> downloadFile(context, downloadUrl))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(file -> {
                    if (file == null || !file.exists()) {
                        callback.onError("下载结果文件失败");
                        return;
                    }
                    Timber.tag(TAG).e("reUploadDownloadUrl downloadUrl: %s", downloadUrl);

                    SessionUpload.upload(context, file.getAbsolutePath(), new com.fxzs.lingxiagent.model.chat.callback.StsCallback() {
                        @Override
                        public void progress(long percent) {
                            callback.onTaskStatusChanged("正在上传结果文件: " + percent + "%");
                        }

                        @Override
                        public void callback(String url) {
                            if (url == null || url.isEmpty()) {
                                callback.onError("上传结果文件失败，URL为空");
                                return;
                            }
                            ArrayList<String> urls = new ArrayList<>();
                            urls.add(url);
                            Timber.tag(TAG).e("reUploadDownloadUrl url: %s", url);
                            callback.onSuccess(urls, true);
                        }

                        @Override
                        public void error(@Nullable CosXmlClientException clientException, @Nullable CosXmlServiceException serviceException) {
                            String errorMsg = "上传结果文件失败";
                            if (clientException != null) {
                                errorMsg += ": " + clientException.getMessage();
                            }
                            if (serviceException != null) {
                                errorMsg += ": " + serviceException.getMessage();
                            }
                            callback.onError(errorMsg);
                        }
                    });
                }, throwable -> callback.onError("下载结果文件失败: " + throwable.getMessage())));
    }

    private boolean hasUrlFileSuffix(String url) {
        boolean hasSuffix = false;
        if (url == null || url.isEmpty()) {
            hasSuffix = false;
        } else {

            try {
                String pure = url;
                int q = pure.indexOf('?');
                if (q >= 0) {
                    pure = pure.substring(0, q);
                }
                int slash = pure.lastIndexOf('/');
                String name = slash >= 0 ? pure.substring(slash + 1) : pure;
                Timber.tag(TAG).e("reUploadDownloadUrl name: %s", name);
                int dot = name.lastIndexOf('.');
                hasSuffix = dot > 0 && dot < name.length() - 1;

            } catch (Exception e) {
                hasSuffix = false;
            }
        }


        Timber.tag(TAG).e("reUploadDownloadUrl url: %s", url);
        Timber.tag(TAG).e("reUploadDownloadUrl hasSuffix: %s", hasSuffix);
        return hasSuffix;
    }

    @Nullable
    private String getUrlFileSuffix(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            String pure = url;
            int q = pure.indexOf('?');
            if (q >= 0) {
                pure = pure.substring(0, q);
            }
            int slash = pure.lastIndexOf('/');
            String name = slash >= 0 ? pure.substring(slash + 1) : pure;
            int dot = name.lastIndexOf('.');
            if (dot > 0 && dot < name.length() - 1) {
                return name.substring(dot + 1);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private String extractDownloadUrl(WpsTaskResponse.Data data) {
        if (data == null) return null;
        if (data.downloadUrl != null && !data.downloadUrl.isEmpty()) {
            return data.downloadUrl;
        }
        if (data.result != null) {
            if (data.result.pdfs != null && !data.result.pdfs.isEmpty()) {
                return data.result.pdfs.get(0).url;
            }
            if (data.result.images != null && !data.result.images.isEmpty()) {
                return data.result.images.get(0).url;
            }
            if (data.result.txts != null && !data.result.txts.isEmpty()) {
                return data.result.txts.get(0).url;
            }
        }
        return null;
    }

    private boolean isTaskFinished(WpsTaskResponse.Data data) {
        if (data == null || data.status == null) return false;
        if (data.status instanceof String) {
            String statusStr = (String) data.status;
            return "success".equalsIgnoreCase(statusStr) || "failure".equalsIgnoreCase(statusStr);
        }
        if (data.status instanceof Number) {
            int statusInt = ((Number) data.status).intValue();
            return statusInt == 1 || statusInt < 0; // Assuming 1 is success, negative is failure
        }
        return false;
    }

    private boolean isTaskSuccessful(WpsTaskResponse.Data data) {
        if (data == null || data.status == null) return false;
        if (data.status instanceof String) {
            return "success".equalsIgnoreCase((String) data.status);
        }
        if (data.status instanceof Number) {
            return ((Number) data.status).intValue() == 1;
        }
        return false;
    }

    private boolean isOcrTask(int toolType) {
        return toolType == AiFileToolTypes.TOOL_PDF_TO_WORD ||
                toolType == AiFileToolTypes.TOOL_PDF_TO_PPT ||
                toolType == AiFileToolTypes.TOOL_IMG_TO_WORD ||
                toolType == AiFileToolTypes.TOOL_IMG_TO_PPT;
    }
    private boolean isImageTask(int toolType) {
        return toolType == AiFileToolTypes.TOOL_WORD_TO_PDF ||
                toolType == AiFileToolTypes.TOOL_PPT_TO_PDF ||
//                toolType == AiFileToolTypes.TOOL_WORD_TO_IMG ||
//                toolType == AiFileToolTypes.TOOL_PPT_TO_IMG||
//                toolType == AiFileToolTypes.TOOL_PDF_TO_IMG||
                toolType == AiFileToolTypes.TOOL_PDF_TO_WORD||
                toolType == AiFileToolTypes.TOOL_PDF_TO_PPT||
                toolType == AiFileToolTypes.TOOL_IMG_TO_WORD ||
                toolType == AiFileToolTypes.TOOL_IMG_TO_PPT;


//        public static final int TOOL_WORD_TO_PDF = 1;
//        public static final int TOOL_PPT_TO_PDF = 2;
//        public static final int TOOL_WORD_TO_IMG = 3;
//        public static final int TOOL_PPT_TO_IMG = 4;
//        public static final int TOOL_PDF_TO_IMG = 5;
//
//        public static final int TOOL_PDF_TO_WORD = 6;
//        public static final int TOOL_PDF_TO_PPT = 7;
//        public static final int TOOL_IMG_TO_WORD = 8;
//        public static final int TOOL_IMG_TO_PPT = 9;
    }

    @Nullable
    private String getOfficeTypeForOcr(int toolType) {
        switch (toolType) {
            case AiFileToolTypes.TOOL_PDF_TO_WORD:
            case AiFileToolTypes.TOOL_IMG_TO_WORD:
                return "docx";
            case AiFileToolTypes.TOOL_PDF_TO_PPT:
            case AiFileToolTypes.TOOL_IMG_TO_PPT:
                return "pptx";


//            return toolType == AiFileToolTypes.TOOL_WORD_TO_PDF ||
//                    toolType == AiFileToolTypes.TOOL_PPT_TO_PDF ||
////                toolType == AiFileToolTypes.TOOL_WORD_TO_IMG ||
////                toolType == AiFileToolTypes.TOOL_PPT_TO_IMG||
////                toolType == AiFileToolTypes.TOOL_PDF_TO_IMG||
//                    toolType == AiFileToolTypes.TOOL_PDF_TO_WORD||
//                    toolType == AiFileToolTypes.TOOL_PDF_TO_PPT||
//                    toolType == AiFileToolTypes.TOOL_IMG_TO_WORD ||
//                    toolType == AiFileToolTypes.TOOL_IMG_TO_PPT;
            default:
                return null;
        }
    }

    /**
     * 针对“先转图片再 OCR”的链路使用的 office_type 映射：
     * 除了 TOOL_IMG_TO_WORD / TOOL_IMG_TO_PPT 以外，其它工具类型都要支持。
     * 可选值：docx, xlsx, pptx, json, table
     */
    @Nullable
    private String getOfficeTypeForImageOcr(int toolType) {
        switch (toolType) {
            // Word 相关、以及 PDF 转 Word，都归为 docx
            case AiFileToolTypes.TOOL_WORD_TO_PDF:
            case AiFileToolTypes.TOOL_WORD_TO_IMG:
            case AiFileToolTypes.TOOL_PDF_TO_WORD:
                return "docx";

            // PPT 相关、以及 PDF 转 PPT，都归为 pptx
            case AiFileToolTypes.TOOL_PPT_TO_PDF:
            case AiFileToolTypes.TOOL_PPT_TO_IMG:
            case AiFileToolTypes.TOOL_PDF_TO_PPT:
                return "pptx";

            // PDF 转图片，目前也按 docx 处理（如果后续有专门的表格/JSON类型，再细分为 xlsx/json/table）
            case AiFileToolTypes.TOOL_PDF_TO_IMG:
                return "docx";

            // 图片转文档本身直接用原有 getOfficeTypeForOcr，不走这里
            case AiFileToolTypes.TOOL_IMG_TO_WORD:
            case AiFileToolTypes.TOOL_IMG_TO_PPT:
            default:
                return null;
        }
    }

    @Nullable
    private Observable<WpsTaskResponse> getApiCall(int toolType, WpsConvertRequest request) {
        WpsApiService service = WpsApiClient.getApiService();
        String officeType = getOfficeTypeForOcr(toolType);

        switch (toolType) {
            case AiFileToolTypes.TOOL_WORD_TO_PDF:
            case AiFileToolTypes.TOOL_PPT_TO_PDF:
                return service.convertToPdf(request);

            case AiFileToolTypes.TOOL_WORD_TO_IMG:
            case AiFileToolTypes.TOOL_PPT_TO_IMG:
            case AiFileToolTypes.TOOL_PDF_TO_IMG:
                return service.convertToPng(request);

            case AiFileToolTypes.TOOL_PDF_TO_WORD:
            case AiFileToolTypes.TOOL_PDF_TO_PPT:
                if (officeType != null) {
                    return service.ocrPdfToDocs(request, officeType);
                }
                break;

            case AiFileToolTypes.TOOL_IMG_TO_WORD:
            case AiFileToolTypes.TOOL_IMG_TO_PPT:
                if (officeType != null) {
                    return service.ocrImgToDocs(request, officeType);
                }
                break;
        }
        return null;
    }

    public void cancel() {
        disposables.clear();
        if (previewRepository != null) {
            previewRepository.cancel();
        }
        Timber.tag(TAG).d("All tasks cancelled.");
    }
}
