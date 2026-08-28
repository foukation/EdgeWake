package com.fxzs.lingxiagent.view.drawing;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.drawing.VMDrawingSuccess;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

/**
 * 图片生成成功界面
 */
public class DrawingSuccessActivity extends BaseActivity<VMDrawingSuccess> {
    
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    
    private ImageView ivBack;
    private TextView tvDescription;
    private TextView tvStatus;
    private ImageView ivGeneratedImage;
    private androidx.cardview.widget.CardView cvGeneratedImage;
    private LinearLayout llDownload;
    private LinearLayout llContinueEdit;
    private TextView tvShare;
    private ImageView ivSound;
    private ImageView ivBookmark;
    private ImageView ivRefresh;
    private ImageView ivMore;
    
    private String imageUrl;
    private String prompt;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_drawing_success;
    }
    
    @Override
    protected Class<VMDrawingSuccess> getViewModelClass() {
        return VMDrawingSuccess.class;
    }
    
    @Override
    protected void initializeViews() {
        // 初始化控件
        ivBack = findViewById(R.id.iv_back);
        tvDescription = findViewById(R.id.tv_description);
        tvStatus = findViewById(R.id.tv_status);
        ivGeneratedImage = findViewById(R.id.iv_generated_image);
        cvGeneratedImage = findViewById(R.id.cv_generated_image);
        llDownload = findViewById(R.id.ll_download);
        llContinueEdit = findViewById(R.id.ll_continue_edit);
        tvShare = findViewById(R.id.tv_share);
        ivSound = findViewById(R.id.iv_sound);
        ivBookmark = findViewById(R.id.iv_bookmark);
        ivRefresh = findViewById(R.id.iv_refresh);
        ivMore = findViewById(R.id.iv_more);
        
        // 获取传递的数据
        Intent intent = getIntent();
        imageUrl = intent.getStringExtra("image_url");
        prompt = intent.getStringExtra("prompt");
        
        // 设置数据
        if (prompt != null) {
            tvDescription.setText("帮我生成图片：" + prompt);
        }
        
        // 加载图片并根据比例调整尺寸
        if (imageUrl != null) {
            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                    @Override
                    public void onResourceReady(@androidx.annotation.NonNull android.graphics.Bitmap resource,
                            @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                        // 设置图片
                        ivGeneratedImage.setImageBitmap(resource);

                        // 根据图片比例调整ImageView尺寸
                        adjustImageViewSize(resource.getWidth(), resource.getHeight());
                    }

                    @Override
                    public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {
                        ivGeneratedImage.setImageDrawable(placeholder);
                    }

                    @Override
                    public void onLoadFailed(@androidx.annotation.Nullable android.graphics.drawable.Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        ivGeneratedImage.setImageDrawable(errorDrawable);
                    }
                });
        }
        
        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
        llDownload.setOnClickListener(v -> downloadImage());
        llContinueEdit.setOnClickListener(v -> continueEdit());
        tvShare.setOnClickListener(v -> shareImage());
        ivSound.setOnClickListener(v -> toggleSound());
        ivBookmark.setOnClickListener(v -> toggleBookmark());
        ivRefresh.setOnClickListener(v -> refreshImage());
        ivMore.setOnClickListener(v -> showMoreOptions());
        
        // 设置图片点击查看大图
        ivGeneratedImage.setOnClickListener(v -> {
            Intent viewerIntent = new Intent(this, DrawingImageViewerActivity.class);
            viewerIntent.putExtra("image_url", imageUrl);
            viewerIntent.putExtra("prompt", prompt);
            startActivity(viewerIntent);
        });
    }
    
    @Override
    protected void setupDataBinding() {
        // 这个页面没有需要双向绑定的数据
    }
    
    @Override
    protected void setupObservers() {
        // 观察状态变化
    }

    /**
     * 根据图片实际比例调整ImageView尺寸
     */
    private void adjustImageViewSize(int imageWidth, int imageHeight) {
        if (imageWidth <= 0 || imageHeight <= 0) return;

        // 计算图片比例
        float aspectRatio = (float) imageWidth / imageHeight;

        // 设置最大尺寸限制（dp转px）
        float density = getResources().getDisplayMetrics().density;
        int maxWidth = (int) (300 * density); // 300dp
        int maxHeight = (int) (400 * density); // 400dp
        int minWidth = (int) (150 * density); // 150dp

        // 根据比例计算合适的尺寸
        final int targetWidth, targetHeight;

        if (aspectRatio > 1) {
            // 横图：宽度优先
            int tempWidth = Math.min(maxWidth, Math.max(minWidth, maxWidth));
            int tempHeight = (int) (tempWidth / aspectRatio);
            if (tempHeight > maxHeight) {
                tempHeight = maxHeight;
                tempWidth = (int) (tempHeight * aspectRatio);
            }
            targetWidth = tempWidth;
            targetHeight = tempHeight;
        } else {
            // 竖图：高度优先
            int tempHeight = Math.min(maxHeight, (int) (minWidth / aspectRatio));
            int tempWidth = (int) (tempHeight * aspectRatio);
            if (tempWidth > maxWidth) {
                tempWidth = maxWidth;
                tempHeight = (int) (tempWidth / aspectRatio);
            }
            targetWidth = tempWidth;
            targetHeight = tempHeight;
        }

        // 在主线程中更新UI
        runOnUiThread(() -> {
            // 直接调整CardView尺寸
            if (cvGeneratedImage != null) {
                android.view.ViewGroup.LayoutParams cardParams = cvGeneratedImage.getLayoutParams();
                if (cardParams != null) {
                    cardParams.width = targetWidth;
                    cardParams.height = targetHeight;
                    cvGeneratedImage.setLayoutParams(cardParams);

                    Timber.tag("DrawingSuccess").d( "Adjusted CardView size: " + targetWidth + "x" + targetHeight +
                        " (aspect ratio: " + aspectRatio + ", original: " + imageWidth + "x" + imageHeight + ")");
                }
            }
        });
    }
    
    // 下载图片
    private void downloadImage() {
        if (imageUrl == null) {
            Toast.makeText(this, "没有可下载的图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上不需要存储权限
            performDownload();
        } else {
            // Android 9及以下需要存储权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                AppPermissionRequestManager.requestExternalStoragePermission(this, REQUEST_STORAGE_PERMISSION,"请授权手机存储，以便下载图片功能正常使用");
            } else {
                performDownload();
            }
        }
    }
    
    // 执行下载
    private void performDownload() {
        Toast.makeText(this, "正在下载图片...", Toast.LENGTH_SHORT).show();
        
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    saveImageToGallery(resource);
                }
                
                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                }
                
                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    super.onLoadFailed(errorDrawable);
                    Toast.makeText(DrawingSuccessActivity.this, "图片下载失败", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    // 保存图片到相册
    private void saveImageToGallery(Bitmap bitmap) {
        String fileName = "IYA_Drawing_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用MediaStore
            ContentValues values = new ContentValues();
	        String folderName = this.getPackageName();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.DESCRIPTION, "通通助手AI绘画");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES +  "/" + folderName);
            
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // Android 9及以下使用传统方式
	        String folderName = this.getPackageName();
            File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),  "/" + folderName);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            File imageFile = new File(storageDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                
                // 通知系统相册更新
                MediaStore.Images.Media.insertImage(getContentResolver(), imageFile.getAbsolutePath(), fileName, "通通助手AI绘画");
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imageFile)));
                
                Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // 继续编辑
    private void continueEdit() {
        Intent intent = new Intent(this, DrawingActivity.class);
        // 不再传递prompt，只传递继续编辑标记和参考图片
        intent.putExtra("continue_edit", true);
        // 传递当前图片URL作为参考图片
        if (imageUrl != null && !imageUrl.isEmpty()) {
            intent.putExtra("reference_image_url", imageUrl);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
    
    // 分享图片
    private void shareImage() {
        if (imageUrl == null) {
            Toast.makeText(this, "没有可分享的图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "正在准备分享...", Toast.LENGTH_SHORT).show();
        
        // 下载图片并分享
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    shareImageBitmap(resource);
                }
                
                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                }
                
                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    super.onLoadFailed(errorDrawable);
                    Toast.makeText(DrawingSuccessActivity.this, "图片加载失败", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    // 分享位图
    private void shareImageBitmap(Bitmap bitmap) {
        try {
            // 保存图片到缓存目录
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "shared_image.jpg");
            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
            
            // 获取图片URI
            Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", imageFile);
            
            if (contentUri != null) {
                // 创建分享Intent
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                
                // 添加文字描述
                String shareText = "看看我用通通助手AI创作的画作";
                if (prompt != null && !prompt.isEmpty()) {
                    shareText += "：" + prompt;
                }
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                
                startActivity(Intent.createChooser(shareIntent, "分享图片到"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    // 切换声音
    private void toggleSound() {
        // TODO: 实现声音播放功能
        Toast.makeText(this, "声音功能暂未实现", Toast.LENGTH_SHORT).show();
    }
    
    // 切换收藏
    private void toggleBookmark() {
        // TODO: 实现收藏功能
        Toast.makeText(this, "收藏功能暂未实现", Toast.LENGTH_SHORT).show();
    }
    
    // 刷新图片
    private void refreshImage() {
        // TODO: 实现刷新功能
        Toast.makeText(this, "刷新功能暂未实现", Toast.LENGTH_SHORT).show();
    }
    
    // 显示更多选项
    private void showMoreOptions() {
        // TODO: 实现更多选项功能
        Toast.makeText(this, "更多功能暂未实现", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performDownload();
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
            }
        }
    }
}