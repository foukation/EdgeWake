package com.fxzs.lingxiagent.view.drawing;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.drawing.VMDrawingTransform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * AI绘画 - 图片风格转换/图生图入口页
 */
public class DrawingTransformActivity extends BaseActivity<VMDrawingTransform> {

    public static final String EXTRA_URL = "EXTRA_URL";
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_DES = "EXTRA_DES";
    public static final String EXTRA_TYPE = "EXTRA_TYPE";//1重新编辑，2继续创作

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private static final int REQUEST_CODE_SELECT_STYLE = 3;
    private static final int REQUEST_GENERATE = 4;//去生成页返回
    private static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024L; // 10MB
    private static final int MAX_STYLE_DESCRIPTION_LENGTH = 500;

    private LinearLayout layoutUploadPlaceholder;
    private ImageView ivSelectedImage;
    private View cardSelectedImage;
    private DrawingTransformStyleAdapter styleAdapter;
    private Uri selectedImageUri;
    private EditText etStyleDescription;
    Long sessionId;

    @Override
    protected int getLayoutResource() {
        return R.layout.act_drawing_transform;
    }

    @Override
    protected Class<VMDrawingTransform> getViewModelClass() {
        return VMDrawingTransform.class;
    }

    @Override
    protected void setupDataBinding() {
        // no-op
    }

    @Override
    protected void initializeViews() {
        etStyleDescription = findViewById(R.id.et_style_description);
        etStyleDescription.setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                int keep = MAX_STYLE_DESCRIPTION_LENGTH - (dest.length() - (dend - dstart));
                if (keep <= 0) {
                    Toast.makeText(DrawingTransformActivity.this, "最多输入500字", Toast.LENGTH_SHORT).show();
                    return "";
                }
                int incomingCount = end - start;
                if (keep >= incomingCount) {
                    return null;
                }
                Toast.makeText(DrawingTransformActivity.this, "最多输入500字", Toast.LENGTH_SHORT).show();
                return source.subSequence(start, start + keep);
            }
        }});

        // 设置返回按钮点击事件
        findViewById(R.id.back).setOnClickListener(v -> finish());
        setupStyleRecyclerView();
        setupImageSelection();
        setupGenerateButton();
        setupSeeAllStyles();


        // 获取传递的数据
        Intent intent = getIntent();
        if(intent != null){
            String url = intent.getStringExtra(EXTRA_URL);
            if(url != null){
                selectedImageUri = Uri.parse(url);
            }
            sessionId = intent.getLongExtra(EXTRA_SESSION_ID,0);
            String des = intent.getStringExtra(EXTRA_DES);
            if(des != null){
                etStyleDescription.setText(des);
            }
//            styleAdapter.setSelectedPosition(0);
            updateImageViews();
        }
    }

    private void setupStyleRecyclerView() {
        RecyclerView rvStyleSelection = findViewById(R.id.rv_style_selection);
        rvStyleSelection.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        // 初始化适配器，使用空列表，数据将通过 ViewModel 加载
        List<DrawingTransformStyleItem> styleItems = new ArrayList<>();
        styleAdapter = new DrawingTransformStyleAdapter(this, styleItems);
        rvStyleSelection.setAdapter(styleAdapter);
    }

    private void setupImageSelection() {
        View imageUploadContainer = findViewById(R.id.image_upload_container);
        layoutUploadPlaceholder = findViewById(R.id.layout_upload_placeholder);
        cardSelectedImage = findViewById(R.id.card_selected_image);
        ivSelectedImage = findViewById(R.id.iv_selected_image);
        
        // 点击上传区域：未选择图片时打开相册，已选择图片时查看大图
        imageUploadContainer.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                Intent intent = new Intent(this, DrawingImageViewerActivity.class);
                intent.putExtra("image_url", selectedImageUri.toString());
                intent.putExtra("hide_bottom_bar", true);
                startActivity(intent);
            } else {
                checkPermissionAndOpenGallery();
            }
        });
        
        // 替换图片
        View btnReplaceImage = findViewById(R.id.btn_replace_image);
        btnReplaceImage.setOnClickListener(v -> checkPermissionAndOpenGallery());
        
        // 删除图片
        View btnDeleteImage = findViewById(R.id.btn_delete_image);
        btnDeleteImage.setOnClickListener(v -> {
            selectedImageUri = null;
            updateImageViews();
        });
    }

    private void setupGenerateButton() {
        findViewById(R.id.btn_generate).setOnClickListener(v -> {
            if (selectedImageUri == null) {
                Toast.makeText(this, "请先选择一张图片", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 获取用户输入的风格描述
            String styleDescription = etStyleDescription.getText().toString().trim();
            
            // TODO: 调用AI生成图片的API
            generateImage(selectedImageUri, styleDescription);
        });
    }
    
    private void setupSeeAllStyles() {
        findViewById(R.id.tv_see_all_styles).setOnClickListener(v -> {
            // 跳转到选择风格页面
            Intent intent = new Intent(this, DrawingStyleSelectionActivity.class);
            // 传递当前所有风格和已选中的风格
            List<DrawingTransformStyleItem> allStyles = viewModel.getStyleItems().getValue();
            if (allStyles != null && !allStyles.isEmpty()) {
                ArrayList<DrawingTransformStyleItem> allStylesList = new ArrayList<>(allStyles);
                intent.putExtra(DrawingStyleSelectionActivity.EXTRA_ALL_STYLES, allStylesList);
            }
            // 传递当前已选中的风格（从适配器获取）
            if (styleAdapter != null) {
                DrawingTransformStyleItem selectedStyle = getSelectedStyle();
                if (selectedStyle != null) {
                    intent.putExtra(DrawingStyleSelectionActivity.EXTRA_SELECTED_STYLE, selectedStyle);
                }
            }
            startActivityForResult(intent, REQUEST_CODE_SELECT_STYLE);
        });
    }
    
    /**
     * 获取当前选中的风格
     */
    private DrawingTransformStyleItem getSelectedStyle() {
        if (styleAdapter == null) {
            return null;
        }
        List<DrawingTransformStyleItem> allItems = viewModel.getStyleItems().getValue();
        if (allItems != null && styleAdapter.getSelectedPosition() >= 0 && 
            styleAdapter.getSelectedPosition() < allItems.size()) {
            return allItems.get(styleAdapter.getSelectedPosition());
        }
        return null;
    }

    private void checkPermissionAndOpenGallery() {

        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            AppPermissionRequestManager.requestImagesPermission(this, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE,"请授权设备相册权限，以便使用风格转绘相关功能");
        } else {
            openImagePicker();
        }

//        String permission;
//        // Android 13+ (API 33+) 使用 READ_MEDIA_IMAGES，否则使用 READ_EXTERNAL_STORAGE
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            permission = Manifest.permission.READ_MEDIA_IMAGES;
//        } else {
//            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
//        }
//
//        if (ContextCompat.checkSelfPermission(this, permission)
//                != PackageManager.PERMISSION_GRANTED) {
//            // 检查是否应该显示权限说明
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
//                // 用户之前拒绝过，但未选择"不再询问"，显示说明后请求权限
//                Toast.makeText(this, "需要图片访问权限以选择图片", Toast.LENGTH_SHORT).show();
//                ActivityCompat.requestPermissions(this,
//                        new String[]{permission},
//                        PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
//            } else {
//                // 首次请求或已被永久拒绝
//                ActivityCompat.requestPermissions(this,
//                        new String[]{permission},
//                        PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
//            }
//        } else {
//            openImagePicker();
//        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/jpg", "image/png"});
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                // 权限被拒绝，检查是否被永久拒绝
                String permission = permissions.length > 0 ? permissions[0] : "";
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    // 被永久拒绝，引导用户到设置页面
                    Toast.makeText(this, "请在设置中开启图片访问权限", Toast.LENGTH_LONG).show();
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "需要图片访问权限以选择图片", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri pickedImageUri = data.getData();
            if (!isAllowedImageType(pickedImageUri)) {
                Toast.makeText(this, "仅支持 jpg/jpeg/png 格式图片", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isImageSizeValid(pickedImageUri)) {
                Toast.makeText(this, "图片大小不能超过 10MB", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedImageUri = pickedImageUri;
            updateImageViews();
        } else if (requestCode == REQUEST_CODE_SELECT_STYLE && resultCode == RESULT_OK && data != null) {
            // 接收选中的风格（单选）
            Object selectedStyleObj = data.getSerializableExtra(DrawingStyleSelectionActivity.EXTRA_SELECTED_STYLE);
            if (selectedStyleObj instanceof DrawingTransformStyleItem) {
                DrawingTransformStyleItem selectedStyle = (DrawingTransformStyleItem) selectedStyleObj;
                if (selectedStyle != null) {
                    // 更新风格列表，将选中的风格显示在列表前面
                    updateStyleListWithSelected(selectedStyle);
                }
            }
        } else if (requestCode == REQUEST_GENERATE && resultCode == RESULT_OK && data != null) {

            int type = data.getIntExtra(DrawingTransformActivity.EXTRA_TYPE,0);
            if(type == 1){
                String url = data.getStringExtra(DrawingTransformActivity.EXTRA_URL);
                sessionId = data.getLongExtra(DrawingImageGenerateActivity.EXTRA_SESSION_ID,0);

            }else if(type == 2){
                //继续创作清空
//                String url = data.getStringExtra(DrawingTransformActivity.EXTRA_URL);
                sessionId = data.getLongExtra(DrawingImageGenerateActivity.EXTRA_SESSION_ID,0);
//                selectedImageUri = Uri.parse(url);
                selectedImageUri = null;
                etStyleDescription.setText("");
                styleAdapter.setSelectedPosition(-1);
                updateImageViews();
            }
        }
    }
    
    /**
     * 更新风格列表，将选中的风格显示在前面
     */
    private void updateStyleListWithSelected(DrawingTransformStyleItem selectedStyle) {
        List<DrawingTransformStyleItem> allStyles = viewModel.getStyleItems().getValue();
        if (allStyles == null || allStyles.isEmpty()) {
            return;
        }
        
        // 创建新的列表，将选中的风格放在前面
        List<DrawingTransformStyleItem> newList = new ArrayList<>();
        
        // 先添加选中的风格
        for (DrawingTransformStyleItem item : allStyles) {
            if (item.getId() == selectedStyle.getId()) {
                newList.add(item);
                break;
            }
        }
        
        // 再添加未选中的风格
        for (DrawingTransformStyleItem item : allStyles) {
            if (item.getId() != selectedStyle.getId()) {
                newList.add(item);
            }
        }
        
        // 更新 ViewModel 和适配器
        viewModel.getStyleItems().setValue(newList);
        if (styleAdapter != null) {
            styleAdapter.updateData(newList);
            // 设置第一个为选中状态（选中的风格已经在第一位）
            styleAdapter.setSelectedPosition(0);
        }
    }

    private boolean isAllowedImageType(Uri imageUri) {
        if (imageUri == null) {
            return false;
        }

        String mimeType = getContentResolver().getType(imageUri);
        if (mimeType != null) {
            mimeType = mimeType.toLowerCase();
            if ("image/jpeg".equals(mimeType) || "image/jpg".equals(mimeType) || "image/png".equals(mimeType)) {
                return true;
            }
        }

        String path = imageUri.getPath();
        if (path != null) {
            String lowerPath = path.toLowerCase();
            return lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".png");
        }

        return false;
    }

    private boolean isImageSizeValid(Uri imageUri) {
        if (imageUri == null) {
            return false;
        }

        long fileSize = -1L;
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(imageUri, new String[]{OpenableColumns.SIZE}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (fileSize <= 0) {
            try {
                android.content.res.AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(imageUri, "r");
                if (afd != null) {
                    fileSize = afd.getLength();
                    afd.close();
                }
            } catch (Exception ignored) {
            }
        }

        return fileSize > 0 && fileSize <= MAX_IMAGE_SIZE_BYTES;
    }

    private void updateImageViews() {
        if (selectedImageUri != null) {
            layoutUploadPlaceholder.setVisibility(View.GONE);
            cardSelectedImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(ivSelectedImage);
        } else {
            layoutUploadPlaceholder.setVisibility(View.VISIBLE);
            cardSelectedImage.setVisibility(View.GONE);
        }
    }

    private void generateImage(Uri imageUri, String styleDescription) {
        // 获取选中的风格
        DrawingTransformStyleItem selectedStyle = getSelectedStyle();
        if (selectedStyle == null) {
            showToast("请选择风格");
            return;
        }
        // 跳转到图片生成页面
        Intent intent = new Intent(this, DrawingImageGenerateActivity.class);
        intent.putExtra(DrawingImageGenerateActivity.EXTRA_ORIGINAL_IMAGE_URI, imageUri.toString());
        intent.putExtra(DrawingImageGenerateActivity.EXTRA_STYLE_DESCRIPTION, styleDescription);
        if (selectedStyle != null) {
            intent.putExtra(DrawingImageGenerateActivity.EXTRA_STYLE_ITEM, selectedStyle);
        }
        intent.putExtra(DrawingImageGenerateActivity.EXTRA_SESSION_ID, sessionId);
//        startActivity(intent);
        startActivityForResult(intent, REQUEST_GENERATE);
        
        // TODO: 实际应该调用API生成图片，然后在生成页面中轮询状态
        // viewModel.generateTransformImage(imageUri, selectedStyle, styleDescription);
    }

    @Override
    protected void setupObservers() {
        // 监听风格列表数据
        viewModel.getStyleItems().observe(this, styleItems -> {
            if (styleItems != null) {
                // 更新适配器数据
                if (styleAdapter != null) {
                    styleAdapter.updateData(styleItems);
                } else {
                    styleAdapter = new DrawingTransformStyleAdapter(this, styleItems);
                    RecyclerView rvStyleSelection = findViewById(R.id.rv_style_selection);
                    rvStyleSelection.setAdapter(styleAdapter);
                }
            }
        });
        
        // 监听错误信息
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}