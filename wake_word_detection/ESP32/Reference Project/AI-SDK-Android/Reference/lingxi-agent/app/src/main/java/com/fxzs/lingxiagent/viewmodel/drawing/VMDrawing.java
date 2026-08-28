package com.fxzs.lingxiagent.viewmodel.drawing;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.drawing.api.PageResult;
import com.fxzs.lingxiagent.model.drawing.api.SampleListRequest;
import com.fxzs.lingxiagent.model.drawing.dto.AspectRatioDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSampleDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingStyleDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepositoryImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

/**
 * AI绘画主界面ViewModel
 */
public class VMDrawing extends BaseViewModel {

    // 双向绑定字段
    private final ObservableField<String> prompt = new ObservableField<>("");
    private final ObservableField<String> selectedRatio = new ObservableField<>("1:1");
    private final ObservableField<Boolean> generateEnabled = new ObservableField<>(false);
    private final ObservableField<Boolean> isGenerating = new ObservableField<>(false);
    private final ObservableField<Integer> progress = new ObservableField<>(0);
    private final ObservableField<String> progressText = new ObservableField<>("");

    // 业务状态
    private final MutableLiveData<List<DrawingStyleDto>> styles = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<AspectRatioDto>> aspectRatios = new MutableLiveData<>();
    private final MutableLiveData<DrawingImageDto> generatedImage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showResult = new MutableLiveData<>(false);
    private final MutableLiveData<List<DrawingSampleDto>> samples = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<DrawingSessionDto> currentSession = new MutableLiveData<>();
    private final MutableLiveData<List<DrawingSampleDto>> categories = new MutableLiveData<>(new ArrayList<>());

    private DrawingStyleDto selectedStyle = null;
    private AspectRatioDto selectAspectRatios = null;
    private final DrawingRepository repository;
    private String initialStyle = null;
    private String referenceImageUrl = null; // 参考图片URL
    private String hiddenPrompt = null; // 继续编辑模式下的隐藏prompt，用于关联但不显示
    private boolean isContinueEditMode = false; // 是否是继续编辑模式

    public VMDrawing(@NonNull Application application) {
        super(application);
        repository = DrawingRepositoryImpl.getInstance();

        // 监听输入变化
        prompt.observeForever(this::validateForm);
        // 监听比例选择变化
        selectedRatio.observeForever(this::validateForm);

        // 初始化数据
        initAspectRatios();
        loadStyles();
        // 加载分类
        loadCategories();


        // 暂时不在初始化时创建会话，等用户输入prompt后再创建
    }

    // Getters
    public ObservableField<String> getPrompt() {
        return prompt;
    }

    public DrawingStyleDto getSelectedStyle() {
        return selectedStyle;
    }

    public void setSelectedStyle(DrawingStyleDto style) {
        // 记录调用栈，找出是谁在重置风格
        Timber.tag("VMDrawing").d( "setSelectedStyle called with: " + (style != null ? style.getName() + " (ID: " + style.getId() + ")" : "null"));
//        Timber.tag("VMDrawing").d( "Call stack: " + android.util.Log.getStackTraceString(new Throwable()));

        this.selectedStyle = style;
        Timber.tag("VMDrawing").d( "Style selected: " + (style != null ? style.getName() + " (ID: " + style.getId() + ")" : "null"));
        Timber.tag("VMDrawing").d( "selectedStyle object updated: " + (this.selectedStyle != null ? this.selectedStyle.getName() + " (ID: " + this.selectedStyle.getId() + ")" : "null"));
        validateForm(prompt.get());
    }

    public ObservableField<String> getSelectedRatio() {
        return selectedRatio;
    }

    public ObservableField<Boolean> getGenerateEnabled() {
        return generateEnabled;
    }

    public ObservableField<Boolean> getIsGenerating() {
        return isGenerating;
    }

    public ObservableField<Integer> getProgress() {
        return progress;
    }

    public ObservableField<String> getProgressText() {
        return progressText;
    }

    public MutableLiveData<List<DrawingStyleDto>> getStyles() {
        return styles;
    }

    public MutableLiveData<List<AspectRatioDto>> getAspectRatios() {
        return aspectRatios;
    }

    public MutableLiveData<DrawingImageDto> getGeneratedImage() {
        return generatedImage;
    }

    public MutableLiveData<Boolean> getShowResult() {
        return showResult;
    }

    public MutableLiveData<List<DrawingSampleDto>> getSamples() {
        return samples;
    }

    public MutableLiveData<DrawingSessionDto> getCurrentSession() {
        return currentSession;
    }

    // 初始化宽高比选项（注意：这些数据仅用于显示，实际计算使用512作为基准宽度）
    private void initAspectRatios() {
        List<AspectRatioDto> ratios = new ArrayList<>();
        // 基准宽度512，根据比例计算高度
        ratios.add(new AspectRatioDto("9:16", "9:16", 720, 1280, true));  // 512 * 16/9 ≈ 910
        ratios.add(new AspectRatioDto("16:9", "16:9", 1280, 720, false));  // 512 * 9/16 = 288
        ratios.add(new AspectRatioDto("4:3", "4:3", 1024, 768, false));    // 512 * 3/4 = 384
        ratios.add(new AspectRatioDto("3:4", "3:4", 768, 1024, false));    // 512 * 3/2 = 768
        ratios.add(new AspectRatioDto("1:1", "1:1", 768, 768, false));     // 512 * 1/1 = 512，默认
        aspectRatios.postValue(ratios);

        selectAspectRatios = new AspectRatioDto("9:16", "9:16", 720, 1280,  true);//默认
        Timber.tag("VMDrawing").d( "Initialized aspect ratios with base width 512");
    }

    public void setSelectAspectRatios(int position){
        Timber.tag("VMDrawing").d( "Initialized aspect setSelectAspectRatios position = "+position);
        selectAspectRatios = aspectRatios.getValue().get(position);
    }
    public void setSelectAspectRatios( AspectRatioDto aspectRatios ){
        Timber.tag("VMDrawing").d( "Initialized aspect setSelectAspectRatios aspectRatios = "+aspectRatios.getRatio());
        Timber.tag("VMDrawing").d( "Initialized aspect setSelectAspectRatios getWidth = "+aspectRatios.getWidth());
        Timber.tag("VMDrawing").d( "Initialized aspect setSelectAspectRatios getHeight = "+aspectRatios.getHeight());
        selectAspectRatios = aspectRatios;
    }

    public AspectRatioDto getSelectAspectRatios() {
        return selectAspectRatios;
    }

    // 加载风格列表
    private void loadStyles() {
        setLoading(true);

        repository.getStyles().observeForever(result -> {
            setLoading(false);
            if (result.isSuccess() && result.getData() != null) {
                List<DrawingStyleDto> styleList = result.getData();
                Collections.sort(styleList);
                styles.postValue(styleList);

                // 应用初始风格（如果有）
                if (initialStyle != null) {
                    for (DrawingStyleDto style : styleList) {
                        if (style.getName().equals(initialStyle)) {
                            setSelectedStyle(style);
                            initialStyle = null; // 清空，避免重复设置
                            return;
                        }
                    }
                }

                // 如果没有初始风格或找不到，使用默认第一个（但不覆盖用户已选择的风格）
                if (!styleList.isEmpty() && selectedStyle == null) {
                    setSelectedStyle(styleList.get(0));
                    Timber.tag("VMDrawing").d( "Set default style: " + styleList.get(0).getName());
                } else if (selectedStyle != null) {
                    Timber.tag("VMDrawing").d( "Keeping existing selected style: " + selectedStyle.getName() + " (ID: " + selectedStyle.getId() + ")");
                }
            } else {
                // 如果获取失败，使用默认风格
                List<DrawingStyleDto> mockStyles = createMockStyles();
                styles.postValue(mockStyles);

                // 应用初始风格（如果有）
                if (initialStyle != null) {
                    for (DrawingStyleDto style : mockStyles) {
                        if (style.getName().equals(initialStyle)) {
                            setSelectedStyle(style);
                            initialStyle = null;
                            setError(result.getError() != null ? result.getError() : "获取风格列表失败");
                            return;
                        }
                    }
                }

                if (!mockStyles.isEmpty() && selectedStyle == null) {
                    setSelectedStyle(mockStyles.get(0));
                    Timber.tag("VMDrawing").d( "Set default mock style: " + mockStyles.get(0).getName());
                } else if (selectedStyle != null) {
                    Timber.tag("VMDrawing").d( "Keeping existing selected style (mock): " + selectedStyle.getName() + " (ID: " + selectedStyle.getId() + ")");
                }
                setError(result.getError() != null ? result.getError() : "获取风格列表失败");
            }
        });
    }

    // 创建模拟风格数据
    private List<DrawingStyleDto> createMockStyles() {
        List<DrawingStyleDto> list = new ArrayList<>();

        DrawingStyleDto style1 = new DrawingStyleDto();
        style1.setId(1L);
        style1.setName("写实");
        style1.setPrompt("realistic, photorealistic");
        style1.setIconUrl("https://example.com/style/realistic.jpg");
        list.add(style1);

        DrawingStyleDto style2 = new DrawingStyleDto();
        style2.setId(2L);
        style2.setName("数字艺术写实");
        style2.setPrompt("digital art, realistic style");
        style2.setIconUrl("https://example.com/style/digital_art.jpg");
        list.add(style2);

        DrawingStyleDto style3 = new DrawingStyleDto();
        style3.setId(3L);
        style3.setName("古风仙侠");
        style3.setPrompt("ancient chinese style, fantasy, xianxia");
        style3.setIconUrl("https://example.com/style/ancient.jpg");
        list.add(style3);

        DrawingStyleDto style4 = new DrawingStyleDto();
        style4.setId(4L);
        style4.setName("机甲风");
        style4.setPrompt("mecha, sci-fi, mechanical");
        style4.setIconUrl("https://example.com/style/mecha.jpg");
        list.add(style4);

        DrawingStyleDto style5 = new DrawingStyleDto();
        style5.setId(5L);
        style5.setName("数码漫画（二次元）");
        style5.setPrompt("anime, manga style, 2D");
        style5.setIconUrl("https://example.com/style/anime.jpg");
        list.add(style5);

        DrawingStyleDto style6 = new DrawingStyleDto();
        style6.setId(6L);
        style6.setName("乙女漫画（厚涂）");
        style6.setPrompt("shoujo manga, thick painting style");
        style6.setIconUrl("https://example.com/style/shoujo.jpg");
        list.add(style6);

        return list;
    }

    // 表单验证
    private void validateForm(String value) {
        boolean hasPrompt = prompt.get() != null && !prompt.get().trim().isEmpty();
        boolean hasStyle = selectedStyle != null;
        boolean hasRatio = selectedRatio.get() != null && !selectedRatio.get().trim().isEmpty();
        generateEnabled.set(hasPrompt && hasStyle && hasRatio && !isGenerating.get());

        Timber.tag("VMDrawing").d( "validateForm - hasPrompt: " + hasPrompt +
                ", hasStyle: " + hasStyle + ", hasRatio: " + hasRatio +
                ", generateEnabled: " + generateEnabled.get());
    }

    /**
     * 设置宽高比信息（从比例选择页面传入）
     */
    public void setAspectRatio(String ratio, int width, int height) {
        selectedRatio.set(ratio);

        // 更新比例列表中对应的选项
        List<AspectRatioDto> ratios = aspectRatios.getValue();
        if (ratios != null) {
            boolean found = false;
            for (AspectRatioDto aspectRatio : ratios) {
                if (aspectRatio.getRatio().equals(ratio)) {
                    aspectRatio.setWidth(width);
                    aspectRatio.setHeight(height);
                    found = true;
                    break;
                }
            }

            // 如果不是预设的比例，添加一个自定义比例
            if (!found) {
                AspectRatioDto customRatio = new AspectRatioDto(ratio, ratio, width, height, false);
                ratios.add(customRatio);
                aspectRatios.postValue(ratios);
                Timber.tag("VMDrawing").d( "Added custom aspect ratio: " + ratio + " (" + width + "x" + height + ")");
            }
        }
    }

    /**
     * 设置初始风格（从画廊页面传入）
     */
    public void setInitialStyle(String styleName) {
        this.initialStyle = styleName;
    }

    /**
     * 设置参考图片URL（用于做同款功能）
     */
    public void setReferenceImageUrl(String imageUrl) {
        this.referenceImageUrl = imageUrl;
        Timber.tag("VMDrawing").d( "Reference image URL set: " + imageUrl);
    }

    /**
     * 清除参考图片URL
     */
    public void clearReferenceImageUrl() {
        this.referenceImageUrl = null;
    }

    /**
     * 设置隐藏的prompt（用于继续编辑模式）
     */
    public void setHiddenPrompt(String prompt) {
        this.hiddenPrompt = prompt;
        Timber.tag("VMDrawing").d( "Hidden prompt set: " + prompt);
    }

    /**
     * 获取隐藏的prompt
     */
    public String getHiddenPrompt() {
        return hiddenPrompt;
    }

    /**
     * 设置是否为继续编辑模式
     */
    public void setContinueEditMode(boolean isContinueEditMode) {
        this.isContinueEditMode = isContinueEditMode;
    }

    /**
     * 获取是否为继续编辑模式
     */
    public boolean isContinueEditMode() {
        return isContinueEditMode;
    }

    // 加载绘画示例
    public void loadSamples(Long categoryId) {
        SampleListRequest request = new SampleListRequest();
        request.setCatId(categoryId);
        request.setPageNo(1);
        request.setPageSize(20);

        repository.getSampleList(request).observeForever(result -> {
            if (result.isSuccess() && result.getData() != null) {
                PageResult<DrawingSampleDto> pageResult = result.getData();
                if (pageResult.getRecords() != null) {
                    samples.postValue(pageResult.getRecords());
                }
            } else {
                setError(result.getError() != null ? result.getError() : "获取示例失败");
            }
        });
    }

    // 使用示例作为模板
    public void useSampleAsTemplate(DrawingSampleDto sample) {
        if (sample != null) {
            prompt.set(sample.getPrompt());
            // 设置对应的风格
            if (sample.getStyleId() != null) {
                List<DrawingStyleDto> styleList = styles.getValue();
                if (styleList != null) {
                    for (DrawingStyleDto style : styleList) {
                        if (style.getId().equals(sample.getStyleId())) {
                            setSelectedStyle(style);
                            break;
                        }
                    }
                }
            }
        }
    }

    // 更新会话名称
    public void updateSessionName(String name) {
        DrawingSessionDto session = currentSession.getValue();
        if (session != null) {
            session.setName(name);
            repository.updateSession(session).observeForever(result -> {
                if (result.isSuccess()) {
                    // 更新成功
                } else {
                    setError("更新会话失败");
                }
            });
        }
    }

    // 类别列表
    public MutableLiveData<List<DrawingSampleDto>> getCategories() {
        return categories;
    }



    // 拉取类别
    private void loadCategories() {
        repository.getImageCatList().observeForever(result -> {
            if (result.isSuccess() && result.getData() != null) {
                categories.postValue(result.getData());
            } else {
                setError(result.getError() != null ? result.getError() : "获取分类失败");
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        prompt.removeObserver(this::validateForm);
        selectedRatio.removeObserver(this::validateForm);
    }
}