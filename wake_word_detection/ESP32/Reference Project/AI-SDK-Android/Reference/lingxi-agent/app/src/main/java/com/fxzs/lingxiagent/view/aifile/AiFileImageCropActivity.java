package com.fxzs.lingxiagent.view.aifile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.wps.WpsRepository;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.MeetingLoadingProgressDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class AiFileImageCropActivity extends BaseActivity<AiFileToolViewModel> {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_IMAGE_PATH = "extra_image_path";

    private static final int MIN_CROP_SIZE_PX = 120;

    private ImageView iv;
    private FrameLayout cropContainer;
    private View cropOverlay;
    private View cropTouchLayer;
    private WpsRepository wpsRepository;
    private MeetingLoadingProgressDialog progressDialog;

    private enum DragMode {
        MOVE,
        LEFT,
        TOP,
        RIGHT,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private DragMode activeDragMode = DragMode.MOVE;
    private float downRawX;
    private float downRawY;
    private float startX;
    private float startY;
    private int startW;
    private int startH;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ai_file_image_crop;
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
        wpsRepository = new WpsRepository();

        View back = findViewById(R.id.back);
        TextView title = findViewById(R.id.tv_header_title);
        if (title != null) {
            title.setText("选择范围");
        }
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        iv = findViewById(R.id.iv_image);
        cropContainer = findViewById(R.id.crop_container);
        cropOverlay = findViewById(R.id.crop_overlay);
        cropTouchLayer = findViewById(R.id.fl_content);

        bindImage();
        setupCropGestures();

        View btnRotateLeft = findViewById(R.id.btn_rotate_left);
        View btnRotateRight = findViewById(R.id.btn_rotate_right);
        View btnReset = findViewById(R.id.btn_reset);
        View btnAll = findViewById(R.id.btn_all);
        View btnConvert = findViewById(R.id.btn_convert);

        if (btnRotateLeft != null) {
            btnRotateLeft.setOnClickListener(v -> rotate(-90f));
        }
        if (btnRotateRight != null) {
            btnRotateRight.setOnClickListener(v -> rotate(90f));
        }
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                if (iv != null) {
                    iv.setRotation(0f);
                }
                if (cropContainer != null) {
                    cropContainer.post(this::resetCropToImage);
                }
            });
        }
        if (btnAll != null) {
            btnAll.setOnClickListener(v -> setCropToFullImage());
        }
        if (btnConvert != null) {
            btnConvert.setOnClickListener(v -> handleConvertClick());
        }

        if (cropContainer != null) {
            cropContainer.post(this::resetCropToImage);
        }
    }

    private void handleConvertClick() {
        String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        if (imagePath == null || imagePath.isEmpty()) {
            showToast("图片路径为空");
            return;
        }

        File croppedFile = cropSelectedAreaToTempFile(imagePath);
        if (croppedFile == null || !croppedFile.exists()) {
            showToast("裁剪失败，请调整选区后重试");
            return;
        }

        showProgress("准备上传...");

        wpsRepository.startConversion(this, croppedFile.getAbsolutePath(), getToolType(), null, new WpsRepository.WpsCallback() {
            @Override
            public void onUploadProgress(long percent) {
                runOnUiThread(() -> showProgress("正在上传: " + percent + "%"));
            }

            @Override
            public void onTaskCreated(String taskId) {
                runOnUiThread(() -> showProgress("已提交OCR任务"));
            }

            @Override
            public void onTaskStatusChanged(String status) {
                runOnUiThread(() -> showProgress("正在识别: " + status));
            }

            @Override
            public void onSuccess(ArrayList<String> urls, boolean isImage) {
                runOnUiThread(() -> {
                    dismissProgress();
                    if (urls == null) {
                        showToast("识别成功，但结果为空");
                        return;
                    }
                    launchResultActivity(urls, false);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    dismissProgress();
                    showToast("识别失败: " + message);
                });
            }
        });
    }

    private File cropSelectedAreaToTempFile(String imagePath) {
        try {
            if (iv == null || cropContainer == null) return null;

            Bitmap source = loadSourceBitmap(imagePath);
            if (source == null) return null;

            RectF imageBounds = getImageBoundsOnScreen();
            if (imageBounds == null || imageBounds.width() <= 0 || imageBounds.height() <= 0) return null;

            RectF cropRectOnScreen = getCropRectOnScreen();
            if (cropRectOnScreen == null) return null;

            RectF intersect = new RectF(cropRectOnScreen);
            boolean hasIntersect = intersect.intersect(imageBounds);
            if (!hasIntersect || intersect.width() <= 1f || intersect.height() <= 1f) return null;

            float ratioX = source.getWidth() / imageBounds.width();
            float ratioY = source.getHeight() / imageBounds.height();

            int left = Math.max(0, Math.round((intersect.left - imageBounds.left) * ratioX));
            int top = Math.max(0, Math.round((intersect.top - imageBounds.top) * ratioY));
            int right = Math.min(source.getWidth(), Math.round((intersect.right - imageBounds.left) * ratioX));
            int bottom = Math.min(source.getHeight(), Math.round((intersect.bottom - imageBounds.top) * ratioY));

            if (right <= left || bottom <= top) return null;

            Rect srcRect = new Rect(left, top, right, bottom);
            Bitmap cropped = Bitmap.createBitmap(source, srcRect.left, srcRect.top, srcRect.width(), srcRect.height());

            File outFile = new File(getCacheDir(), "crop_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                cropped.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                fos.flush();
            }
            return outFile;
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap loadSourceBitmap(String imagePath) {
        // 优先使用 content uri（通常由系统 picker 授权读取），避免直接读 file path 被拒绝
        String uriStr = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (uriStr != null && !uriStr.isEmpty()) {
            try (InputStream is = getContentResolver().openInputStream(Uri.parse(uriStr))) {
                if (is != null) {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    if (bmp != null) return bmp;
                }
            } catch (Exception ignored) {
            }
        }

        // 兜底：尝试 file path
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                return BitmapFactory.decodeFile(imagePath);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void launchResultActivity(ArrayList<String> urls, boolean isImage) {
        Intent intent = new Intent(this, AiFileResultActivity.class);
        intent.putExtra(AiFileResultActivity.EXTRA_TITLE, "识别结果");
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

    private int getToolType() {
        return getIntent().getIntExtra(AiFileToolTypes.EXTRA_TOOL_TYPE, AiFileToolTypes.TOOL_IMG_TO_WORD);
    }

    private void bindImage() {
        if (iv == null) return;
        String uriStr = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        String path = getIntent().getStringExtra(EXTRA_IMAGE_PATH);

        try {
            if (uriStr != null && !uriStr.isEmpty()) {
                iv.setImageURI(Uri.parse(uriStr));
            } else if (path != null && !path.isEmpty()) {
                iv.setImageURI(Uri.parse("file://" + path));
            }
        } catch (Exception ignored) {
        }

        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    private void setupCropGestures() {
        if (cropContainer == null || cropTouchLayer == null) return;

        // 主触摸层：监听外层容器，保证外扩端点也能命中
        cropTouchLayer.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (!isTouchOnCropArea(event.getRawX(), event.getRawY())) {
                        activeDragMode = DragMode.MOVE;
                        return false;
                    }
                    float localX = event.getRawX() - getCropContainerLeftOnScreen();
                    float localY = event.getRawY() - getCropContainerTopOnScreen();
                    activeDragMode = detectDragMode(localX, localY, cropContainer.getWidth(), cropContainer.getHeight());
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    startX = cropContainer.getX();
                    startY = cropContainer.getY();
                    startW = cropContainer.getWidth();
                    startH = cropContainer.getHeight();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downRawX;
                    float dy = event.getRawY() - downRawY;
                    applyDrag(activeDragMode, startX, startY, startW, startH, dx, dy);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    activeDragMode = DragMode.MOVE;
                    return true;
                default:
                    return false;
            }
        });
    }

    private boolean isTouchOnCropArea(float rawX, float rawY) {
        if (cropContainer == null) return false;
        float padding = dp(16);
        float left = getCropContainerLeftOnScreen() - padding;
        float top = getCropContainerTopOnScreen() - padding;
        float right = left + cropContainer.getWidth() + padding * 2;
        float bottom = top + cropContainer.getHeight() + padding * 2;
        return rawX >= left && rawX <= right && rawY >= top && rawY <= bottom;
    }

    private float getCropContainerLeftOnScreen() {
        if (cropContainer == null) return 0f;
        int[] location = new int[2];
        cropContainer.getLocationOnScreen(location);
        return location[0];
    }

    private float getCropContainerTopOnScreen() {
        if (cropContainer == null) return 0f;
        int[] location = new int[2];
        cropContainer.getLocationOnScreen(location);
        return location[1];
    }

    private DragMode detectDragMode(float x, float y, int w, int h) {
        float cornerHalf = dp(10);
        float edgeHalf = dp(6);
        float touchPadding = dp(6);
        float cornerRadius = cornerHalf + touchPadding;
        float edgeBand = edgeHalf + touchPadding;

        boolean nearLeft = x <= edgeBand;
        boolean nearRight = x >= w - edgeBand;
        boolean nearTop = y <= edgeBand;
        boolean nearBottom = y >= h - edgeBand;

        // 角点优先（大热区）
        if (distance(x, y, 0, 0) <= cornerRadius) return DragMode.TOP_LEFT;
        if (distance(x, y, w, 0) <= cornerRadius) return DragMode.TOP_RIGHT;
        if (distance(x, y, 0, h) <= cornerRadius) return DragMode.BOTTOM_LEFT;
        if (distance(x, y, w, h) <= cornerRadius) return DragMode.BOTTOM_RIGHT;

        if (nearTop && nearLeft) return DragMode.TOP_LEFT;
        if (nearTop && nearRight) return DragMode.TOP_RIGHT;
        if (nearBottom && nearLeft) return DragMode.BOTTOM_LEFT;
        if (nearBottom && nearRight) return DragMode.BOTTOM_RIGHT;

        // 边中点热区（限制在边附近）
        if (nearTop && x >= edgeBand && x <= w - edgeBand) return DragMode.TOP;
        if (nearBottom && x >= edgeBand && x <= w - edgeBand) return DragMode.BOTTOM;
        if (nearLeft && y >= edgeBand && y <= h - edgeBand) return DragMode.LEFT;
        if (nearRight && y >= edgeBand && y <= h - edgeBand) return DragMode.RIGHT;

        return DragMode.MOVE;
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void applyDrag(DragMode mode,
                           float startX,
                           float startY,
                           int startW,
                           int startH,
                           float dx,
                           float dy) {
        RectF bounds = getImageBoundsOnScreen();
        if (bounds == null || cropContainer == null) return;

        float left = startX;
        float top = startY;
        float right = startX + startW;
        float bottom = startY + startH;

        switch (mode) {
            case MOVE:
                left += dx;
                right += dx;
                top += dy;
                bottom += dy;
                break;
            case LEFT:
                left += dx;
                break;
            case TOP:
                top += dy;
                break;
            case RIGHT:
                right += dx;
                break;
            case BOTTOM:
                bottom += dy;
                break;
            case TOP_LEFT:
                top += dy;
                left += dx;
                break;
            case TOP_RIGHT:
                top += dy;
                right += dx;
                break;
            case BOTTOM_LEFT:
                bottom += dy;
                left += dx;
                break;
            case BOTTOM_RIGHT:
                bottom += dy;
                right += dx;
                break;
        }

        if (mode == DragMode.MOVE) {
            float width = right - left;
            float height = bottom - top;
            if (left < bounds.left) {
                left = bounds.left;
                right = left + width;
            }
            if (top < bounds.top) {
                top = bounds.top;
                bottom = top + height;
            }
            if (right > bounds.right) {
                right = bounds.right;
                left = right - width;
            }
            if (bottom > bounds.bottom) {
                bottom = bounds.bottom;
                top = bottom - height;
            }
        } else {
            if (right - left < MIN_CROP_SIZE_PX) {
                if (mode == DragMode.LEFT || mode == DragMode.TOP_LEFT || mode == DragMode.BOTTOM_LEFT) {
                    left = right - MIN_CROP_SIZE_PX;
                } else {
                    right = left + MIN_CROP_SIZE_PX;
                }
            }
            if (bottom - top < MIN_CROP_SIZE_PX) {
                if (mode == DragMode.TOP || mode == DragMode.TOP_LEFT || mode == DragMode.TOP_RIGHT) {
                    top = bottom - MIN_CROP_SIZE_PX;
                } else {
                    bottom = top + MIN_CROP_SIZE_PX;
                }
            }

            left = Math.max(left, bounds.left);
            top = Math.max(top, bounds.top);
            right = Math.min(right, bounds.right);
            bottom = Math.min(bottom, bounds.bottom);

            if (right - left < MIN_CROP_SIZE_PX || bottom - top < MIN_CROP_SIZE_PX) {
                return;
            }
        }

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) cropContainer.getLayoutParams();
        lp.width = Math.round(right - left);
        lp.height = Math.round(bottom - top);
        cropContainer.setLayoutParams(lp);
        cropContainer.setX(left);
        cropContainer.setY(top);
    }

    private void rotate(float degrees) {
        if (iv == null) return;
        iv.setRotation((iv.getRotation() + degrees) % 360f);
        if (cropContainer != null) {
            cropContainer.post(this::resetCropToImage);
        }
    }

    private void resetCropToImage() {
        if (iv == null || cropContainer == null) return;
        RectF bounds = getImageBoundsOnScreen();
        if (bounds == null) return;

        int targetW = (int) (bounds.width() * 0.9f);
        int targetH = (int) (bounds.height() * 0.9f);
        if (targetW <= 0 || targetH <= 0) return;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) cropContainer.getLayoutParams();
        lp.width = targetW;
        lp.height = targetH;
        cropContainer.setLayoutParams(lp);

        cropContainer.post(() -> {
            cropContainer.setX(bounds.centerX() - cropContainer.getWidth() / 2f);
            cropContainer.setY(bounds.centerY() - cropContainer.getHeight() / 2f);
        });
    }

    private void setCropToFullImage() {
        if (iv == null || cropContainer == null) return;
        RectF bounds = getImageBoundsOnScreen();
        if (bounds == null) return;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) cropContainer.getLayoutParams();
        lp.width = Math.round(bounds.width());
        lp.height = Math.round(bounds.height());
        cropContainer.setLayoutParams(lp);
        cropContainer.setX(bounds.left);
        cropContainer.setY(bounds.top);
    }

    private RectF getCropRectOnScreen() {
        if (cropContainer == null) return null;
        return new RectF(
                cropContainer.getX(),
                cropContainer.getY(),
                cropContainer.getX() + cropContainer.getWidth(),
                cropContainer.getY() + cropContainer.getHeight()
        );
    }

    private RectF getImageBoundsOnScreen() {
        if (iv == null) return null;
        Drawable d = iv.getDrawable();
        if (d == null) return null;

        Matrix m = iv.getImageMatrix();
        RectF rect = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        m.mapRect(rect);

        float left = iv.getLeft() + rect.left;
        float top = iv.getTop() + rect.top;
        float right = iv.getLeft() + rect.right;
        float bottom = iv.getTop() + rect.bottom;
        return new RectF(left, top, right, bottom);
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
        if (wpsRepository != null) {
            wpsRepository.cancel();
        }
        dismissProgress();
        super.onDestroy();
    }
}
