package com.fxzs.lingxiagent.view.aifile;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.RequestCallback;
import com.fxzs.lingxiagent.model.preview.FilePreviewRepository;
import com.fxzs.lingxiagent.model.wps.WpsRepository;
import com.fxzs.lingxiagent.model.wps.WpsTaskResponse;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.MeetingLoadingProgressDialog;
import com.fxzs.lingxiagent.view.meeting.RealtimeMeetingActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import timber.log.Timber;

public class AiFilePickFileActivity extends BaseActivity<AiFileToolViewModel> {

    public static final String EXTRA_FILE_URI = "extra_file_uri";
    public static final String EXTRA_FILE_PATH = "extra_file_path";

    private static final String TAG = "AiFilePickFile";

    private final List<AiFilePageItem> pages = new ArrayList<>();
    private AiFilePageAdapter adapter;
    private WpsRepository wpsRepository;
    private FilePreviewRepository previewRepository;
    private MeetingLoadingProgressDialog progressDialog;

    private ParcelFileDescriptor pdfPfd;
    private PdfRenderer pdfRenderer;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ai_file_pick_file;
    }

    @Override
    protected Class<AiFileToolViewModel> getViewModelClass() {
        return AiFileToolViewModel.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
        Timber.tag(TAG).d("initializeViews: toolType=%s, filePath=%s, fileUri=%s",
                getToolType(),
                getIntent().getStringExtra(EXTRA_FILE_PATH),
                getIntent().getStringExtra(EXTRA_FILE_URI));

        wpsRepository = new WpsRepository();
        previewRepository = new FilePreviewRepository();

        setupHeader();
        setupList();
        setupConvert();

        loadPages();
        updateRightText();
    }

    private void setupHeader() {
        View back = findViewById(R.id.back);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        TextView title = findViewById(R.id.tv_header_title);
        if (title != null) {
            title.setText("页码选择");
        }

        TextView tvRight = findViewById(R.id.tv_right);
        if (tvRight != null) {
            tvRight.setText("全不选");
            tvRight.setOnClickListener(v -> toggleAll());
        }
    }

    private void setupList() {
        RecyclerView rv = findViewById(R.id.rv_pages);
        if (rv == null) return;

        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            final int space = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 17,
                    AiFilePickFileActivity.this.getResources().getDisplayMetrics());

            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position == RecyclerView.NO_POSITION) return;

                int spanCount = 2;
                int column = position % spanCount;

                outRect.left = column == 0 ? 0 : space / 2;
                outRect.right = column == spanCount - 1 ? 0 : space / 2;
                outRect.top = position < spanCount ? 0 : space;
                outRect.bottom = 0;
            }
        });
        adapter = new AiFilePageAdapter(pages);
        adapter.setListener(position -> {
            if (position < 0 || position >= pages.size()) return;
            pages.get(position).selected = !pages.get(position).selected;
            adapter.notifyItemChanged(position);
            updateRightText();
        });
        rv.setAdapter(adapter);
    }

    private void setupConvert() {
        TextView btn = findViewById(R.id.btn_convert);
        if (btn == null) return;
        btn.setText(getConvertButtonText(getToolType()));
        btn.setOnClickListener(v -> handleConvertClick());
    }

    private void handleConvertClick() {
        List<Integer> selectedPages = pages.stream()
                .filter(p -> p.selected)
                .map(p -> p.pageIndex)
                .collect(Collectors.toList());

        Timber.tag(TAG).d("convert click: selected=%s", selectedPages);
        if (selectedPages.isEmpty()) {
            showToast("请至少选择一页");
            return;
        }

        String filePath = getIntent().getStringExtra(EXTRA_FILE_PATH);
        if (filePath == null) {
            showToast("文件路径为空");
            return;
        }

        showProgress("准备上传...");

        wpsRepository.startConversion(this, filePath, getToolType(), selectedPages, new WpsRepository.WpsCallback() {
            @Override
            public void onUploadProgress(long percent) {
                runOnUiThread(() -> showProgress("正在上传: " + percent + "%"));
            }

            @Override
            public void onTaskCreated(String taskId) {
                runOnUiThread(() -> showProgress("已提交转换任务"));
            }

            @Override
            public void onTaskStatusChanged(String status) {
                runOnUiThread(() -> showProgress("正在转换: " + status));
            }

            @Override
            public void onSuccess(
//                    String result
                     ArrayList<String> urls,boolean isImage
//                    WpsTaskResponse.Result result
            ) {
                Timber.tag(TAG).d("onSuccess: isImage=%s", isImage);
                runOnUiThread(() -> {
                    dismissProgress();
                    if (urls == null) {
                        showToast("转换成功，但结果为空");
                        return;
                    }
//                    ArrayList<String> urls = new ArrayList<>();
//                    urls.add(result);
                    launchResultActivity(urls, isImage);
//                    if (result.pdfs != null && !result.pdfs.isEmpty()) {
//                        ArrayList<String> urls = new ArrayList<>();
//                        for (WpsTaskResponse.UrlItem item : result.pdfs) {
//                            urls.add(item.url);
//                        }
//                        launchResultActivity(urls, false);
//                    } else if (result.images != null && !result.images.isEmpty()) {
//                        ArrayList<String> urls = new ArrayList<>();
//                        for (WpsTaskResponse.UrlItem item : result.images) {
//                            urls.add(item.url);
//                        }
//                        launchResultActivity(urls, true);
//                    } else if (result.txts != null && !result.txts.isEmpty()) {
//                        ArrayList<String> urls = new ArrayList<>();
//                        for (WpsTaskResponse.UrlItem item : result.txts) {
//                            urls.add(item.url);
//                        }
//                        launchResultActivity(urls, false);
//                    } else {
//                        showToast("转换成功，但未获取到预览链接");
//                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    dismissProgress();
                    showToast("转换失败: " + message);
                });
            }
        });
    }

    private void launchResultActivity(ArrayList<String> urls, boolean isImage) {
        Intent intent = new Intent(this, AiFileResultActivity.class);
        intent.putExtra(AiFileResultActivity.EXTRA_TITLE, "转换结果");
        if (isImage) {
            intent.putStringArrayListExtra(AiFileResultActivity.EXTRA_IMAGE_URLS, urls);
        } else {
            if (!urls.isEmpty()) {
                intent.putExtra(AiFileResultActivity.EXTRA_URL, urls.get(0));
            }
        }
        startActivity(intent);
    }

    private void showProgress(String message) {
//        if (progressDialog == null) {
//            progressDialog = new ProgressDialog(this);
//            progressDialog.setCancelable(false);
//        }
//        progressDialog.setMessage(message);
//        if (!progressDialog.isShowing()) {
//            progressDialog.show();
//        }

        if (progressDialog != null && progressDialog.isShowing()) {
            return;
        }

        progressDialog = new MeetingLoadingProgressDialog(this).setMessage(message).setCancelable(true);
        progressDialog.show();
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void toggleAll() {
        boolean allSelected = isAllSelected();
        Timber.tag(TAG).d("toggleAll: allSelected=%s -> set=%s", allSelected, !allSelected);
        for (AiFilePageItem p : pages) {
            p.selected = !allSelected;
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        updateRightText();
    }

    private boolean isAllSelected() {
        if (pages.isEmpty()) return false;
        for (AiFilePageItem p : pages) {
            if (!p.selected) return false;
        }
        return true;
    }

    private void updateRightText() {
        TextView tvRight = findViewById(R.id.tv_right);
        if (tvRight == null) return;
        tvRight.setText(isAllSelected() ? "全不选" : "全选");
    }

    private void loadPages() {
        String filePath = getIntent().getStringExtra(EXTRA_FILE_PATH);
        String fileUri = getIntent().getStringExtra(EXTRA_FILE_URI);

        Timber.tag(TAG).d("loadPages: filePath=%s, fileUri=%s", filePath, fileUri);

        if (filePath != null && filePath.toLowerCase().endsWith(".pdf")) {
            boolean ok = loadPdfPagesFromPath(filePath);
            if (ok) return;
            Timber.tag(TAG).w("loadPages: loadPdfPagesFromPath failed, fallback to uri");
        }

        if (fileUri != null && fileUri.toLowerCase().startsWith("content://")) {
            Uri uri = Uri.parse(fileUri);
            if (isPdfUri(uri)) {
                boolean ok = loadPdfPagesFromUri(uri);
                if (ok) return;
            }
        }
        
        if (filePath != null && (filePath.toLowerCase().endsWith(".doc") || filePath.toLowerCase().endsWith(".docx") || filePath.toLowerCase().endsWith(".ppt") || filePath.toLowerCase().endsWith(".pptx"))) {
            convertFileToPdfForPreview(filePath);
            return;
        }

        Timber.tag(TAG).w("loadPages: fallback placeholder pages");
        createPlaceholderPages(6);
    }

    private boolean isPdfUri(Uri uri) {
        String type = getContentResolver().getType(uri);
        return type != null && type.equals("application/pdf");
    }

    private void convertFileToPdfForPreview(String filePath) {
        showProgress("正在生成预览...");
        int originalToolType = getToolType();
        int convertToPdfType;
        if (originalToolType == AiFileToolTypes.TOOL_WORD_TO_IMG) {
            convertToPdfType = AiFileToolTypes.TOOL_WORD_TO_PDF;
        } else if (originalToolType == AiFileToolTypes.TOOL_PPT_TO_IMG) {
            convertToPdfType = AiFileToolTypes.TOOL_PPT_TO_PDF;
        } else {
            convertToPdfType = originalToolType; // Already a to-pdf type
        }
        wpsRepository.generatePdfForPreview(this, filePath, new WpsRepository.PreviewCallback() {
            @Override
            public void onProgress(String status) {

            }

            @Override
            public void onSuccess(String localPdfPath) {
                dismissProgress();
                loadPdfPagesFromPath(localPdfPath);
            }

            @Override
            public void onError(String message) {

            }
        });
//        wpsRepository.startConversion(this, filePath, convertToPdfType, null, new WpsRepository.WpsCallback() {
//            @Override
//            public void onUploadProgress(long percent) {
//                runOnUiThread(() -> showProgress("生成预览(上传): " + percent + "%"));
//            }
//
//            @Override
//            public void onTaskCreated(String taskId) {
//                runOnUiThread(() -> showProgress("生成预览(转换)..."));
//            }
//
//            @Override
//            public void onTaskStatusChanged(String status) {
//                runOnUiThread(() -> showProgress("生成预览(查询): " + status));
//            }
//
//            @Override
//            public void onSuccess(
//                    ArrayList<String> urls,boolean isImage) {
//                runOnUiThread(() -> {
//                    String downloadUrl = null;
//                    if (urls != null  && !urls.isEmpty()) {
//                        downloadUrl = urls.get(0);
//                    }
//
//                    if (downloadUrl != null && !downloadUrl.isEmpty()) {
//                        new FilePreviewRepository().generatePreviewPage(AiFilePickFileActivity.this, downloadUrl, new FilePreviewRepository.PreviewCallback() {
//                            @Override
//                            public void onProgress(String status) {
//                                showProgress("生成预览(" + status + ")");
//                            }
//
//                            @Override
//                            public void onSuccess(String fileId) {
//                                // This is a file path from our cache
//                                dismissProgress();
//                                loadPdfPagesFromPath(fileId);
//                            }
//
//                            @Override
//                            public void onError(String message) {
//                                dismissProgress();
//                                showToast("生成预览失败: " + message);
//                                createPlaceholderPages(6);
//                            }
//                        });
//                    } else {
//                        dismissProgress();
//                        showToast("生成预览失败: 未获取到PDF链接");
//                        createPlaceholderPages(6);
//                    }
//                });
//            }
//
//            @Override
//            public void onError(String message) {
//                runOnUiThread(() -> {
//                    dismissProgress();
//                    showToast("生成预览失败: " + message);
//                    createPlaceholderPages(6);
//                });
//            }
//        });
    }

    private void createPlaceholderPages(int count) {
        Timber.tag(TAG).d("createPlaceholderPages: count=%s", count);
        pages.clear();
        for (int i = 1; i <= count; i++) {
            pages.add(new AiFilePageItem(i, createPlaceholderBitmap(), true));
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        updateRightText();
    }

    private Bitmap createPlaceholderBitmap() {
        Bitmap bmp = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.parseColor("#F5F7FA"));
        return bmp;
    }

    private boolean loadPdfPagesFromPath(@NonNull String filePath) {
        Timber.tag(TAG).d("loadPdfPagesFromPath: %s", filePath);

        pages.clear();
        if (adapter != null) adapter.notifyDataSetChanged();

        File file = new File(filePath);
        Timber.tag(TAG).d("pdf file exists=%s, len=%s", file.exists(), file.exists() ? file.length() : -1);
        if (!file.exists()) {
            Timber.tag(TAG).e("pdf file not found");
            return false;
        }

        try {
            closePdf();
            pdfPfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(pdfPfd);
            return bindAndRenderPdfPages();
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "pdf open failed (path)");
            closePdf();
            return false;
        }
    }

    private boolean loadPdfPagesFromUri(@NonNull Uri uri) {
        Timber.tag(TAG).d("loadPdfPagesFromUri: %s", uri);
        try {
            closePdf();
            ContentResolver cr = getContentResolver();
            ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
            if (pfd == null) {
                Timber.tag(TAG).e("openFileDescriptor returned null");
                return false;
            }
            pdfPfd = pfd;
            pdfRenderer = new PdfRenderer(pdfPfd);
            return bindAndRenderPdfPages();
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "pdf open failed (uri)");
            closePdf();
            return false;
        }
    }

    private boolean bindAndRenderPdfPages() {
        if (pdfRenderer == null) return false;

        int pageCount = pdfRenderer.getPageCount();
        Timber.tag(TAG).d("pdf pageCount=%s", pageCount);
        if (pageCount <= 0) {
            Timber.tag(TAG).e("pdf pageCount <= 0");
            return false;
        }

        pages.clear();
        for (int i = 0; i < pageCount; i++) {
            pages.add(new AiFilePageItem(i + 1, createPlaceholderBitmap(), true));
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        updateRightText();

        float scale = 0.9f;
        new Thread(() -> {
            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = null;
                try {
                    PdfRenderer r = pdfRenderer;
                    if (r == null) return;
                    Timber.tag(TAG).d("render page %s", i + 1);
                    page = r.openPage(i);
                    int w = Math.max(1, (int) (page.getWidth() * scale));
                    int h = Math.max(1, (int) (page.getHeight() * scale));
                    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(Color.WHITE);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    int pos = i;
                    runOnUiThread(() -> {
                        if (pos < pages.size()) {
                            pages.get(pos).thumbnail = bitmap;
                            if (adapter != null) adapter.notifyItemChanged(pos);
                        }
                    });
                } catch (Exception e) {
                    Timber.tag(TAG).e(e, "render page %s failed", i + 1);
                } finally {
                    if (page != null) {
                        try {
                            page.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }).start();

        return true;
    }

    private void closePdf() {
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
        } catch (Exception ignored) {
        }
        pdfRenderer = null;

        try {
            if (pdfPfd != null) {
                pdfPfd.close();
            }
        } catch (Exception ignored) {
        }
        pdfPfd = null;
    }

    private int getToolType() {
        return getIntent().getIntExtra(AiFileToolTypes.EXTRA_TOOL_TYPE, AiFileToolTypes.TOOL_WORD_TO_PDF);
    }

    private String getConvertButtonText(int toolType) {
        switch (toolType) {
            case AiFileToolTypes.TOOL_WORD_TO_PDF:
            case AiFileToolTypes.TOOL_PPT_TO_PDF:
                return "转换为PDF";
            case AiFileToolTypes.TOOL_WORD_TO_IMG:
            case AiFileToolTypes.TOOL_PPT_TO_IMG:
            case AiFileToolTypes.TOOL_PDF_TO_IMG:
                return "转换为图片";
            case AiFileToolTypes.TOOL_PDF_TO_WORD:
                return "转换为Word";
            case AiFileToolTypes.TOOL_PDF_TO_PPT:
                return "转换为PPT";
            default:
                return "开始转换";
        }
    }

    @Override
    protected void setupObservers() {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWhiteStatusBar();
    }

    @Override
    protected void onDestroy() {
        Timber.tag(TAG).d("onDestroy");
        if (wpsRepository != null) {
            wpsRepository.cancel();
        }
        dismissProgress();
        closePdf();
        super.onDestroy();
    }
}
