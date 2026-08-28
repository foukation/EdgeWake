package com.fxzs.lingxiagent.view.user;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.user.UserUtil;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.common.LoadingProgressDialog;
import com.fxzs.lingxiagent.viewmodel.user.VMFeedback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class FeedbackActivity extends BaseActivity<VMFeedback> {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_STORAGE_PERMISSION2 = 102;

    private ImageView ivClose;
    private TextView tvHistory;
    private EditText etContact;
    private EditText etFeedback;
    private TextView tvCount;
    private RecyclerView rvImages;
    private FrameLayout flAddImage;
    private TextView tvImageCount;
    private Button btnSubmit;
    private FeedbackImageAdapter imageAdapter;

    // Loading对话框
    private LoadingProgressDialog loadingDialog;

    // 图片选择相关
    private Uri currentPhotoUri;
    private String currentPhotoPath;

    // Activity Result Launchers
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;
    private ActivityResultLauncher<Intent> feedbackSuccessLauncher;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_feedback;
    }

    @Override
    protected Class<VMFeedback> getViewModelClass() {
        return VMFeedback.class;
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        // 必须在super.onCreate()之前注册ActivityResultLauncher
        registerActivityResultLaunchers();
        super.onCreate(savedInstanceState);
    }

    private void registerActivityResultLaunchers() {
        // 图库选择
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleSelectedImage(uri);
                    }
                }
        );

        // 相机拍照
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && currentPhotoUri != null) {
                        handleSelectedImage(currentPhotoUri);
                    }
                }
        );

        // 权限请求
        multiplePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Boolean granted : permissions.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        showImageSourceDialog();
                    } else {
                        showPermissionDeniedDialog();
                    }
                }
        );

        // 反馈成功页面返回
        feedbackSuccessLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // 成功页面返回，确保表单数据已清空，然后关闭当前页面
                        viewModel.clearFormData();
                        finish();
                    }
                }
        );
    }

    @Override
    protected void initializeViews() {
        // 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));
            }, 100);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        // 初始化控件
        ivClose = findViewById(R.id.iv_close);
        tvHistory = findViewById(R.id.tv_history);
        etContact = findViewById(R.id.et_contact);
        etFeedback = findViewById(R.id.et_feedback);
        tvCount = findViewById(R.id.tv_count);
        rvImages = findViewById(R.id.rv_images);
        flAddImage = findViewById(R.id.fl_add_image);
        tvImageCount = findViewById(R.id.tv_image_count);
        btnSubmit = findViewById(R.id.btn_submit);

        // 设置图片列表
        rvImages.setLayoutManager(new GridLayoutManager(this, 4));
        imageAdapter = new FeedbackImageAdapter(this);
        imageAdapter.setOnImageDeleteListener(position -> viewModel.removeImage(position));
        imageAdapter.setOnImageClickListener((position, imageUrl) -> {
            // 显示图片预览
            showImagePreview(imageUrl);
        });
        rvImages.setAdapter(imageAdapter);

        // 设置点击事件
        ivClose.setOnClickListener(v -> {
            // 检查是否有未保存的内容
            if (viewModel.hasUnsavedContent()) {
                showCloseConfirmDialog();
            } else {
                finish();
            }
        });

        tvHistory.setOnClickListener(v -> {
            // 跳转到反馈历史页面
            Intent intent = new Intent(this, FeedbackHistoryActivity.class);
            startActivity(intent);
        });

        flAddImage.setOnClickListener(v -> {
            if (viewModel.canAddMoreImages()) {
                showImagePicker();
            } else {
                showToast("最多只能上传4张图片");
            }
        });

        btnSubmit.setOnClickListener(v -> {
            Timber.tag("FeedbackActivity").d("Submit button clicked");

            // 获取输入内容
            String contact = etContact.getText().toString().trim();
            String content = etFeedback.getText().toString().trim();

            Timber.tag("FeedbackActivity").d("Contact: " + contact);
            Timber.tag("FeedbackActivity").d("Content: " + content);

            // 校验联系方式
            if (contact.isEmpty()) {
                showToast("请输入联系方式（邮箱或微信）");
                return;
            }

            // 校验反馈内容
            if (content.isEmpty()) {
                showToast("请输入反馈内容");
                return;
            }

            String versionName = UserUtil.getAppVersionName(this);
            // 校验通过，调用ViewModel提交
            viewModel.preSubmitFeedback(versionName);
        });

        // 添加文本变化监听 - 只用于更新字数显示
        etFeedback.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCount.setText(s.length() + "/100");

                if (s.length() >= 100) {
                    GlobalToast.show(FeedbackActivity.this, "文字已超出100长度", GlobalToast.Type.NORMAL);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void setupDataBinding() {
        // 绑定EditText的双向绑定
        DataBindingUtils.bindEditText(etFeedback, viewModel.getFeedbackContent(), this);
        DataBindingUtils.bindEditText(etContact, viewModel.getContactInfo(), this);

        // 移除按钮状态绑定，让按钮始终可点击，在点击时进行校验
        // DataBindingUtils.bindEnabled(btnSubmit, viewModel.getSubmitEnabled(), this);

        // 绑定图片数量
        DataBindingUtils.bindTextView(tvImageCount, viewModel.getImageCountText(), this);
    }

    @Override
    protected void setupObservers() {
        // 观察提交结果
        viewModel.getSubmitResult().observe(this, success -> {
            if (success != null && success) {
                // 跳转到成功页面
                Intent intent = new Intent(this, FeedbackSuccessActivity.class);
                feedbackSuccessLauncher.launch(intent);
            }
        });

        // 观察图片列表变化
        viewModel.getImageList().observe(this, images -> {
            if (imageAdapter != null) {
                imageAdapter.setImages(images);
            }
        });
    }

    @Override
    protected void handleLoadingState(boolean loading) {
        Timber.tag("FeedbackActivity").d("Loading state changed: " + loading);

        if (loading) {
            // 显示loading对话框
            showLoadingDialog();
            // 禁用提交按钮防止重复提交
            btnSubmit.setEnabled(false);
            btnSubmit.setAlpha(0.6f);
        } else {
            // 隐藏loading对话框
            hideLoadingDialog();
            // 恢复提交按钮
            btnSubmit.setEnabled(true);
            btnSubmit.setAlpha(1.0f);
        }
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new LoadingProgressDialog(this);
        }

        if (!loadingDialog.isShowing()) {
            loadingDialog.setMessage("反馈提交中...")
                    .setCancelable(false)
                    .show();
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 确保对话框被正确清理
        hideLoadingDialog();
        loadingDialog = null;
    }

    private void showCloseConfirmDialog() {
        // 使用全局统一的警告弹窗样式
        com.fxzs.lingxiagent.view.common.CommonDialog.showConfirmDialog(
                this,
                "确定要退出吗？",
                "已编辑的内容将不会保存",
                "确认",
                new CommonDialog.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        finish();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void showImagePicker() {
        showImageSourceDialog();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                showToast("需要相机权限才能拍照");
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                showToast("需要存储权限才能选择图片");
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileManager();
            } else {
                showToast("需要存储权限才能选择文件");
            }
        }
    }

    private void showImageSourceDialog() {
        // 使用PopupWindow弹窗，调整合适的尺寸
        View popupView = getLayoutInflater().inflate(R.layout.popup_avatar_select, null);
        final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(popupView, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dp2px(12));

        popupView.findViewById(R.id.item_camera).setOnClickListener(v -> {
            popupWindow.dismiss();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                AppPermissionRequestManager.requestCameraPermission(this, REQUEST_CAMERA_PERMISSION);
            } else {
                openCamera();
            }
        });
        popupView.findViewById(R.id.item_gallery).setOnClickListener(v -> {
            popupWindow.dismiss();
            String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                    Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                AppPermissionRequestManager.requestImagesPermission(this, REQUEST_STORAGE_PERMISSION,"请授权图片媒体文件权限，以完成图片上传功能");
            } else {
                openGallery();
            }
        });
        popupView.findViewById(R.id.item_file).setOnClickListener(v -> {
            popupWindow.dismiss();
            String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                    Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                AppPermissionRequestManager.requestImagesPermission(this, REQUEST_STORAGE_PERMISSION2,"请授权图片媒体文件权限，以完成本地文件上传功能");
            } else {
                openFileManager();
            }
        });

        // 锚定在添加图片按钮上方，向上偏移8dp
        View anchor = findViewById(R.id.fl_add_image);
        popupWindow.showAsDropDown(anchor, 0, -anchor.getHeight() - dp2px(144) - dp2px(8));
    }

    private int dp2px(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            currentPhotoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(currentPhotoUri);
        } catch (IOException e) {
            e.printStackTrace();
            showToast("创建图片文件失败");
        }
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void openFileManager() {
        // 暂时使用图库选择，后续可以实现文件管理器
        openGallery();
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "FEEDBACK_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void handleSelectedImage(Uri uri) {
        Timber.tag("FeedbackActivity").d("Handling selected image URI: " + uri);

        // 将content URI复制为本地文件
        String imagePath = copyUriToFile(uri);
        if (imagePath != null) {
            Timber.tag("FeedbackActivity").d("Image copied to: " + imagePath);
            // 验证文件是否存在
            java.io.File file = new java.io.File(imagePath);
            if (file.exists()) {
                Timber.tag("FeedbackActivity").d("File exists, size: " + file.length() + " bytes");
                viewModel.addImage(imagePath);
            } else {
                Timber.tag("FeedbackActivity").e("File does not exist after copy: " + imagePath);
                showToast("图片文件创建失败");
            }
        } else {
            Timber.tag("FeedbackActivity").e("Failed to copy image file");
            showToast("获取图片失败");
        }
    }

    private String copyUriToFile(Uri uri) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            Timber.tag("FeedbackActivity").d("Starting to copy URI to file: " + uri);

            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Timber.tag("FeedbackActivity").e("Failed to open input stream for URI: " + uri);
                return null;
            }

            File outputFile = createImageFile();
            Timber.tag("FeedbackActivity").d("Created output file: " + outputFile.getAbsolutePath());

            outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[8192]; // 增大缓冲区
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            outputStream.flush();
            Timber.tag("FeedbackActivity").d("Successfully copied " + totalBytes + " bytes to: " + outputFile.getAbsolutePath());

            // 验证文件是否正确创建
            if (outputFile.exists() && outputFile.length() > 0) {
                Timber.tag("FeedbackActivity").d("File verification passed, size: " + outputFile.length());
                return outputFile.getAbsolutePath();
            } else {
                Timber.tag("FeedbackActivity").e("File verification failed, exists: " + outputFile.exists() + ", size: " + outputFile.length());
                return null;
            }

        } catch (IOException e) {
            Timber.tag("FeedbackActivity").e("IOException while copying file", e);
            return null;
        } catch (SecurityException e) {
            Timber.tag("FeedbackActivity").e("SecurityException while copying file", e);
            return null;
        } finally {
            // 确保流被正确关闭
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Timber.tag("FeedbackActivity").e("Error closing streams", e);
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("权限提示")
                .setMessage("需要相机和存储权限才能选择图片")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showImagePreview(String imageUrl) {
        // 创建一个对话框显示大图
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);

        ImageView ivPreview = dialogView.findViewById(R.id.iv_preview);
        ImageView ivClose = dialogView.findViewById(R.id.iv_close);

        // 改进图片加载，与适配器保持一致
        Object imageSource;
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            // 网络图片
            imageSource = imageUrl;
        } else if (imageUrl.startsWith("file://")) {
            // 文件URI
            imageSource = Uri.parse(imageUrl);
        } else {
            // 本地文件路径
            imageSource = new java.io.File(imageUrl);
        }

        // 加载图片
        com.bumptech.glide.Glide.with(this)
                .load(imageSource)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(ivPreview);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        ivClose.setOnClickListener(v -> dialog.dismiss());
        ivPreview.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }, 100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // 当Activity重新启动时（比如从成功页面返回），检查是否需要清空表单
        // 如果上次提交成功，确保表单数据已清空
        if (viewModel.hasSubmittedSuccessfully()) {
            Timber.tag("FeedbackActivity").d("Activity restarted after successful submission, ensuring form is cleared");
            viewModel.clearFormData();
        }
    }
}