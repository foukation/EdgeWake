package com.fxzs.lingxiagent.viewmodel.drawing;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.drawing.api.PageResult;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepositoryImpl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * 绘画历史记录ViewModel
 */
public class VMDrawingHistory extends BaseViewModel {
    
    // 双向绑定字段
    private final ObservableField<String> searchQuery = new ObservableField<>("");
    private final ObservableField<Boolean> hasMoreData = new ObservableField<>(true);
    private final ObservableField<Boolean> isRefreshing = new ObservableField<>(false);
    private final ObservableField<Boolean> isLoadingMore = new ObservableField<>(false);
    private final ObservableField<String> selectedFilter = new ObservableField<>("all"); // all, today, week, month
    
    // 业务状态
    private final MutableLiveData<List<DrawingImageDto>> historyImages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<DrawingSessionDto>> sessions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<DrawingImageDto> selectedImage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showEmptyView = new MutableLiveData<>(false);
    private final ObservableField<String> emptyMessage = new ObservableField<>("暂无绘画记录");
    
    // 分页参数
    private int currentPage = 1;
    private final int pageSize = 10;
    private boolean isLastPage = false;
    
    // Handler for posting delayed tasks
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // Repository
    private final DrawingRepository repository;
    
    public VMDrawingHistory(@NonNull Application application) {
        super(application);
        repository = DrawingRepositoryImpl.getInstance();
        
        // 设置搜索框监听
        searchQuery.observeForever(this::onSearchQueryChanged);
        
        // 初始加载数据
        loadHistory();
        loadSessions();
    }
    
    // Getter方法
    public ObservableField<String> getSearchQuery() {
        return searchQuery;
    }
    
    public ObservableField<Boolean> getHasMoreData() {
        return hasMoreData;
    }
    
    public ObservableField<Boolean> getIsRefreshing() {
        return isRefreshing;
    }
    
    public ObservableField<Boolean> getIsLoadingMore() {
        return isLoadingMore;
    }
    
    public ObservableField<String> getSelectedFilter() {
        return selectedFilter;
    }
    
    public MutableLiveData<List<DrawingImageDto>> getHistoryImages() {
        return historyImages;
    }
    
    public MutableLiveData<List<DrawingSessionDto>> getSessions() {
        return sessions;
    }
    
    public MutableLiveData<DrawingImageDto> getSelectedImage() {
        return selectedImage;
    }
    
    public MutableLiveData<Boolean> getShowEmptyView() {
        return showEmptyView;
    }
    
    public ObservableField<String> getEmptyMessage() {
        return emptyMessage;
    }
    
    /**
     * 加载历史记录
     */
    public void loadHistory() {
        if (isLoadingMore.get() || isRefreshing.get()) {
            return;
        }
        
        setLoading(true);
        
        Map<String, Object> params = new HashMap<>();
        params.put("pageNo", currentPage);
        params.put("pageSize", pageSize);
        if (searchQuery.get() != null && !searchQuery.get().isEmpty()) {
            params.put("keyword", searchQuery.get());
        }
        // 可以根据filter添加时间过滤
        
        repository.getMyImages(params).observeForever(result -> {
            setLoading(false);
            isRefreshing.set(false);
            
            if (result.isSuccess() && result.getData() != null) {
                PageResult<DrawingImageDto> pageResult = result.getData();
                
                if (currentPage == 1) {
                    // 第一页，替换数据
                    historyImages.setValue(pageResult.getRecords());
                } else {
                    // 加载更多，追加数据
                    List<DrawingImageDto> currentList = historyImages.getValue();
                    if (currentList == null) {
                        currentList = new ArrayList<>();
                    }
                    currentList.addAll(pageResult.getRecords());
                    historyImages.setValue(currentList);
                }
                
                // 更新分页状态
                boolean hasMore = pageResult.getRecords().size() >= pageSize;
                hasMoreData.set(hasMore);
                isLastPage = !hasMore;
                
                // 更新空视图状态
                List<DrawingImageDto> allImages = historyImages.getValue();
                showEmptyView.setValue(allImages == null || allImages.isEmpty());
            } else {
                // 加载失败，使用模拟数据
                List<DrawingImageDto> mockImages = generateMockHistory();
                if (currentPage == 1) {
                    historyImages.setValue(mockImages);
                } else {
                    List<DrawingImageDto> currentList = historyImages.getValue();
                    if (currentList == null) {
                        currentList = new ArrayList<>();
                    }
                    currentList.addAll(mockImages);
                    historyImages.setValue(currentList);
                }
                showEmptyView.setValue(mockImages.isEmpty());
                
                setError(result.getError() != null ? result.getError() : "加载失败");
            }
            
            isLoadingMore.set(false);
        });
    }
    
    /**
     * 刷新
     */
    public void refresh() {
        currentPage = 1;
        isLastPage = false;
        isRefreshing.set(true);
        historyImages.setValue(new ArrayList<>());
        loadHistory();
    }
    
    /**
     * 加载更多
     */
    public void loadMore() {
        if (isLastPage || isLoadingMore.get()) {
            return;
        }
        
        currentPage++;
        isLoadingMore.set(true);
        
        // 模拟加载更多
        handler.postDelayed(() -> {
            List<DrawingImageDto> currentList = historyImages.getValue();
            if (currentList == null) {
                currentList = new ArrayList<>();
            }
            
            List<DrawingImageDto> moreImages = generateMockHistory();
            if (moreImages.isEmpty() || currentPage >= 3) { // 模拟只有3页数据
                isLastPage = true;
                hasMoreData.set(false);
            } else {
                currentList.addAll(moreImages);
                historyImages.setValue(currentList);
            }
            
            isLoadingMore.set(false);
        }, 1000);
    }
    
    /**
     * 搜索变化处理
     */
    private void onSearchQueryChanged(String query) {
        // 延迟搜索，避免频繁请求
        handler.postDelayed(() -> {
            if (query.equals(searchQuery.get())) {
                refresh();
            }
        }, 500);
    }
    
    /**
     * 设置过滤条件
     */
    public void setFilter(String filter) {
        if (!filter.equals(selectedFilter.get())) {
            selectedFilter.set(filter);
            refresh();
        }
    }
    
    /**
     * 删除图片
     */
    public void deleteImage(DrawingImageDto image) {
        if (image == null || image.getId() == null) {
            return;
        }
        
        setLoading(true);
        
        repository.deleteImage(image.getId()).observeForever(result -> {
            setLoading(false);
            
            if (result.isSuccess()) {
                // 从列表中移除
                List<DrawingImageDto> currentList = historyImages.getValue();
                if (currentList != null) {
                    currentList.remove(image);
                    historyImages.setValue(currentList);
                    showEmptyView.setValue(currentList.isEmpty());
                }
                setSuccess("删除成功");
            } else {
                setError(result.getError() != null ? result.getError() : "删除失败");
            }
        });
    }
    
    /**
     * 查看图片详情
     */
    public void viewImageDetail(DrawingImageDto image) {
        selectedImage.setValue(image);
    }
    
    /**
     * 生成模拟历史数据
     */
    private List<DrawingImageDto> generateMockHistory() {
        List<DrawingImageDto> images = new ArrayList<>();
        Random random = new Random();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        String[] prompts = {
            "一只橘猫坐在上海东方明珠前自拍",
            "赛博朋克风格的未来城市",
            "梵高风格的星空下的向日葵田",
            "中国水墨画风格的山水景色",
            "可爱的卡通独角兽在彩虹上奔跑",
            "现代简约风格的室内设计",
            "古风仙侠场景，云雾缭绕的仙山",
            "蒸汽朋克风格的机械龙"
        };
        
        String[] styles = {"写实", "动漫", "油画", "水彩", "古风仙侠", "赛博朋克", "蒸汽朋克", "极简主义"};
        
        int itemCount = currentPage == 1 ? 10 : 5; // 第一页10条，后续每页5条
        
        for (int i = 0; i < itemCount; i++) {
            DrawingImageDto image = new DrawingImageDto();
            image.setId((long) (currentPage * 100 + i));
            image.setPrompt(prompts[random.nextInt(prompts.length)]);
            image.setStyle(styles[random.nextInt(styles.length)]);
            image.setAspectRatio(random.nextBoolean() ? "1:1" : "16:9");
            image.setStatus(1); // 1 表示完成状态
            
            // 使用占位图片URL
            image.setImageUrl("https://picsum.photos/400/400?random=" + System.currentTimeMillis() + i);
            image.setThumbnailUrl("https://picsum.photos/200/200?random=" + System.currentTimeMillis() + i);
            
            // 随机生成日期（最近30天内）
            long timestamp = System.currentTimeMillis() - (random.nextInt(30) * 24 * 60 * 60 * 1000L);
            image.setCreateTime(dateFormat.format(new Date(timestamp)));
            
            images.add(image);
        }
        
        return images;
    }
    
    /**
     * 加载会话列表
     */
    private void loadSessions() {
        Map<String, Object> params = new HashMap<>();
        params.put("pageNo", 1);
        params.put("pageSize", 10); // 加载更多会话
        
        repository.getMySessions(params).observeForever(result -> {
            if (result.isSuccess() && result.getData() != null) {
                PageResult<DrawingSessionDto> pageResult = result.getData();
                if (pageResult.getRecords() != null) {
                    sessions.postValue(pageResult.getRecords());
                }
            }
        });
    }
    
    /**
     * 清空会话
     */
    public void clearSession(DrawingSessionDto session) {
        if (session == null || session.getId() == null) {
            return;
        }
        
        setLoading(true);
        
        repository.clearSession(session.getId()).observeForever(result -> {
            setLoading(false);
            
            if (result.isSuccess()) {
                setSuccess("已清空会话");
                // 重新加载数据
                refresh();
            } else {
                setError(result.getError() != null ? result.getError() : "清空失败");
            }
        });
    }
    
    /**
     * 删除所有会话
     */
    public void deleteAllSessions() {
        setLoading(true);
        
        repository.deleteAllSessions().observeForever(result -> {
            setLoading(false);
            
            if (result.isSuccess()) {
                setSuccess("已删除所有会话");
                // 清空本地数据
                historyImages.setValue(new ArrayList<>());
                sessions.setValue(new ArrayList<>());
                showEmptyView.setValue(true);
            } else {
                setError(result.getError() != null ? result.getError() : "删除失败");
            }
        });
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        searchQuery.removeObserver(this::onSearchQueryChanged);
    }
}