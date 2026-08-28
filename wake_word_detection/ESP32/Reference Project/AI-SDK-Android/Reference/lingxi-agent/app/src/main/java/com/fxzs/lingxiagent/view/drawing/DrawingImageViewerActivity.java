package com.fxzs.lingxiagent.view.drawing;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 全屏图片查看界面
 */
public class DrawingImageViewerActivity extends AppCompatActivity {
    
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final long SAVE_IMAGE_DEBOUNCE_MS = 3_000L;
    
    private PhotoView photoView;
    private LinearLayout llBottomBar;
    private LinearLayout btnContinueEdit;
    private LinearLayout btnSave;
    private String imageUrl;
    private String prompt;
    private Bitmap currentBitmap;

    private long lastSaveImageClickTime = 0L;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置全屏沉浸式
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }
        
        setContentView(R.layout.activity_drawing_image_viewer);
        
        // 获取传递的数据
        Intent intent = getIntent();
        imageUrl = intent.getStringExtra("image_url");
        prompt = intent.getStringExtra("prompt");
        boolean hideBottomBar = intent.getBooleanExtra("hide_bottom_bar", false);

        if (imageUrl == null) {
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews(hideBottomBar);
    }
    
    private void initViews(boolean hideBottomBar) {
        photoView = findViewById(R.id.photo_view);
        llBottomBar = findViewById(R.id.ll_bottom_bar);
        btnContinueEdit = findViewById(R.id.btn_continue_edit);
        btnSave = findViewById(R.id.btn_save);

        // 根据参数决定是否隐藏底部操作栏
        if (hideBottomBar) {
            llBottomBar.setVisibility(View.GONE);
        }

        // 加载图片
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(photoView);

        // 设置点击事件
        photoView.setOnClickListener(v -> finish());
        btnContinueEdit.setOnClickListener(v -> continueEdit());
        btnSave.setOnClickListener(v -> saveImage());

        // 设置返回按钮
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
    }
    
    private void continueEdit() {
//        Intent intent = new Intent(this, DrawingActivity.class);
        Intent intent = new Intent();
        // 不再传递prompt，只传递继续编辑标记和参考图片
        intent.putExtra("continue_edit", true);  // 标记为继续编辑模式
        intent.putExtra("reference_image_url", imageUrl);  // 传递参考图片URL
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);
        setResult(RESULT_OK,intent);
        finish();
    }
    
    private void saveImage() {
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastSaveImageClickTime < SAVE_IMAGE_DEBOUNCE_MS) {
            return;
        }
        lastSaveImageClickTime = now;

        // 检查存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上不需要存储权限
            performSave();
        } else {
            // Android 9及以下需要存储权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                AppPermissionRequestManager.requestExternalStoragePermission(this, REQUEST_STORAGE_PERMISSION,"请授权手机存储，以便图片可以保持");
            } else {
                performSave();
            }
        }
    }
    
    private void performSave() {
        Toast.makeText(this, "正在保存图片...", Toast.LENGTH_SHORT).show();
        
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        currentBitmap = resource;
                        saveImageToGallery(resource);
                    }
                    
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                    
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        Toast.makeText(DrawingImageViewerActivity.this, "图片保存失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void saveImageToGallery(Bitmap bitmap) {
        String fileName = "IYA_Drawing_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用MediaStore
	        String folderName = this.getPackageName();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.DESCRIPTION, "灵犀智能体AI绘画");
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
                MediaStore.Images.Media.insertImage(getContentResolver(), imageFile.getAbsolutePath(), fileName, "灵犀智能体AI绘画");
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imageFile)));
                
                Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performSave();
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
            }
        }
    }
}