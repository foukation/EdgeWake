package com.fxzs.lingxiagent.view.drawing;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.drawing.dto.AspectRatioDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingStyleDto;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.viewmodel.drawing.VMAspectRatioSelection;

import timber.log.Timber;

public class AspectRatioSelectionActivity extends BaseActivity<VMAspectRatioSelection> {
    
    private RecyclerView rvStyles;
    private RecyclerView rvRatios;
    private EditText etPrompt;
    private ImageView ivBack;
    private ImageView ivVoice;
    private ImageView ivSend;
    
    private DrawingStyleAdapter styleAdapter;
    private AspectRatioAdapter ratioAdapter;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_aspect_ratio_selection;
    }
    
    @Override
    protected Class<VMAspectRatioSelection> getViewModelClass() {
        return VMAspectRatioSelection.class;
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置状态栏样式
        setupStatusBar();
    }

    @Override
    protected void initializeViews() {
        Timber.tag("AspectRatioSelection").d( "initializeViews called");
        
        rvStyles = findViewById(R.id.rv_styles);
        rvRatios = findViewById(R.id.rv_ratios);
        etPrompt = findViewById(R.id.et_prompt);
        ivBack = findViewById(R.id.iv_back);
        ivVoice = findViewById(R.id.iv_voice);
        ivSend = findViewById(R.id.iv_send);
        
        Timber.tag("AspectRatioSelection").d( "ivSend found: " + (ivSend != null));
        
        setupRecyclerViews();
        setupClickListeners();
        
        // 从Intent获取初始prompt和style
        Intent intent = getIntent();
        if (intent != null) {
            String prompt = intent.getStringExtra("prompt");
            String styleName = intent.getStringExtra("style");
            
            if (prompt != null) {
                viewModel.getPrompt().set(prompt);
            }
            
            if (styleName != null) {
                viewModel.setInitialStyle(styleName);
            }
        }
    }
    
    private void setupRecyclerViews() {
        // 设置风格列表
        styleAdapter = new DrawingStyleAdapter();
        rvStyles.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvStyles.setAdapter(styleAdapter);
        
        styleAdapter.setOnStyleClickListener((style, position) -> {
            viewModel.setSelectedStyle(style);
            styleAdapter.setSelectedPosition(position);
        });
        
        // 设置比例列表
        ratioAdapter = new AspectRatioAdapter();
        rvRatios.setLayoutManager(new GridLayoutManager(this, 3));
        rvRatios.setAdapter(ratioAdapter);
        
        ratioAdapter.setOnRatioClickListener((ratio, position) -> {
            viewModel.setSelectedRatio(ratio);
            ratioAdapter.setSelectedPosition(position);
        });
    }
    
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        ivSend.setOnClickListener(v -> {
            Timber.tag("AspectRatioSelection").d( "Send button clicked");
            Toast.makeText(this, "点击了发送按钮", Toast.LENGTH_SHORT).show();
            
            // 测试直接跳转
            Intent testIntent = new Intent(this, DrawingActivity.class);
            testIntent.putExtra("prompt", "测试跳转");
            testIntent.putExtra("ratio", "1:1");
            testIntent.putExtra("width", 768);
            testIntent.putExtra("height", 768);
            startActivity(testIntent);
            
            // generateImage();
        });
        
        ivVoice.setOnClickListener(v -> {
            Toast.makeText(this, "语音输入功能开发中", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    protected void setupDataBinding() {
        DataBindingUtils.bindEditText(etPrompt, viewModel.getPrompt(), this);
        DataBindingUtils.bindEnabled(ivSend, viewModel.getGenerateEnabled(), this);
    }
    
    @Override
    protected void setupObservers() {
        // 观察风格列表
        viewModel.getStyleList().observe(this, styles -> {
            if (styles != null) {
                styleAdapter.setStyles(styles);
                
                // 设置默认选中的风格
                DrawingStyleDto selectedStyle = viewModel.getSelectedStyle().getValue();
                if (selectedStyle != null) {
                    for (int i = 0; i < styles.size(); i++) {
                        if (styles.get(i).getName().equals(selectedStyle.getName())) {
                            styleAdapter.setSelectedPosition(i);
                            break;
                        }
                    }
                }
            }
        });
        
        // 观察比例列表
        viewModel.getRatioList().observe(this, ratios -> {
            if (ratios != null) {
                ratioAdapter.setRatios(ratios);
            }
        });
        
        // 观察生成结果
        viewModel.getGenerateResult().observe(this, result -> {
            Timber.tag("AspectRatioSelection").d( "generateResult observed: " + result);
            if (result != null && result) {
                Timber.tag("AspectRatioSelection").d( "Starting DrawingActivity");
                // 跳转到聊天界面
                Intent intent = new Intent(this, DrawingActivity.class);
                intent.putExtra("prompt", viewModel.getPrompt().get());
                intent.putExtra("style", viewModel.getSelectedStyle().getValue().getName());
                intent.putExtra("ratio", viewModel.getSelectedRatio().getValue().getRatio());
                intent.putExtra("width", viewModel.getSelectedRatio().getValue().getWidth());
                intent.putExtra("height", viewModel.getSelectedRatio().getValue().getHeight());
                startActivity(intent);
                finish();
            }
        });
    }
    
    private void generateImage() {
        Timber.tag("AspectRatioSelection").d( "generateImage called");
        Timber.tag("AspectRatioSelection").d( "prompt: " + viewModel.getPrompt().get());
        Timber.tag("AspectRatioSelection").d( "selectedStyle: " + viewModel.getSelectedStyle().getValue());
        Timber.tag("AspectRatioSelection").d( "selectedRatio: " + viewModel.getSelectedRatio().getValue());
        Timber.tag("AspectRatioSelection").d( "generateEnabled: " + viewModel.getGenerateEnabled().get());
        
        // 直接跳转，不依赖观察者
        if (Boolean.TRUE.equals(viewModel.getGenerateEnabled().get())) {
            Intent intent = new Intent(this, DrawingActivity.class);
            intent.putExtra("prompt", viewModel.getPrompt().get());
            
            DrawingStyleDto selectedStyle = viewModel.getSelectedStyle().getValue();
            if (selectedStyle != null) {
                intent.putExtra("style", selectedStyle.getName());
            }
            
            AspectRatioDto selectedRatio = viewModel.getSelectedRatio().getValue();
            if (selectedRatio != null) {
                intent.putExtra("ratio", selectedRatio.getRatio());
                intent.putExtra("width", selectedRatio.getWidth());
                intent.putExtra("height", selectedRatio.getHeight());
            }
            
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "请填写描述并选择风格和比例", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置状态栏样式
     */
    private void setupStatusBar() {
        // 设置状态栏颜色为白色，与背景一致
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }
}