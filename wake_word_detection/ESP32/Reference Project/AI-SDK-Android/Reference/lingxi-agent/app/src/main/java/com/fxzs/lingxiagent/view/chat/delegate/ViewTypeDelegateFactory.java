package com.fxzs.lingxiagent.view.chat.delegate;

import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * ViewTypeDelegate 工厂类
 * 使用单例模式管理委托实例，优化内存使用
 * 
 * 由于委托类通常是无状态的或只包含配置信息，
 * 使用单例模式可以减少内存占用并提高性能
 */
public class ViewTypeDelegateFactory {
    
    private static final String TAG = "ViewTypeDelegateFactory";
    
    /**
     * 存储委托单例实例的缓存
     */
    private static final Map<Class<? extends ViewTypeDelegate>, ViewTypeDelegate> delegateCache = new HashMap<>();
    
    /**
     * 获取UserMessageDelegate单例
     */
    public static UserMessageDelegate getUserMessageDelegate() {
        return (UserMessageDelegate) getOrCreateDelegate(UserMessageDelegate.class, UserMessageDelegate::new);
    }
    public static PlanCardDelegate getPlanCardDelegate() {
        return (PlanCardDelegate) getOrCreateDelegate(PlanCardDelegate.class, PlanCardDelegate::new);
    }

    /**
     * 获取AIMessageDelegate单例
     */
    public static AIMessageDelegate getAIMessageDelegate() {
        return (AIMessageDelegate) getOrCreateDelegate(AIMessageDelegate.class, AIMessageDelegate::new);
    }
    
    /**
     * 获取FileMessageDelegate单例
     */
    public static FileMessageDelegate getFileMessageDelegate() {
        return (FileMessageDelegate) getOrCreateDelegate(FileMessageDelegate.class, FileMessageDelegate::new);
    }
    
    /**
     * 获取ImageMessageDelegate单例
     */
    public static ImageMessageDelegate getImageMessageDelegate() {
        return (ImageMessageDelegate) getOrCreateDelegate(ImageMessageDelegate.class, ImageMessageDelegate::new);
    }
    
    /**
     * 获取DrawingMessageDelegate单例
     */
    public static DrawingMessageDelegate getDrawingMessageDelegate() {
        return (DrawingMessageDelegate) getOrCreateDelegate(DrawingMessageDelegate.class, DrawingMessageDelegate::new);
    }
    
    /**
     * 获取AssistantImageDelegate单例
     */
    public static AssistantImageDelegate getAssistantImageDelegate() {
        return (AssistantImageDelegate) getOrCreateDelegate(AssistantImageDelegate.class, AssistantImageDelegate::new);
    }
    
    /**
     * 获取AgentHeadDelegate单例
     */
    public static AgentHeadDelegate getAgentHeadDelegate() {
        return (AgentHeadDelegate) getOrCreateDelegate(AgentHeadDelegate.class, AgentHeadDelegate::new);
    }
    
    /**
     * 获取MeetingHeadDelegate单例
     */
    public static MeetingHeadDelegate getMeetingHeadDelegate() {
        return (MeetingHeadDelegate) getOrCreateDelegate(MeetingHeadDelegate.class, MeetingHeadDelegate::new);
    }
    
    /**
     * 获取HomeHeadDelegate单例
     */
    public static HomeHeadDelegate getHomeHeadDelegate() {
        return (HomeHeadDelegate) getOrCreateDelegate(HomeHeadDelegate.class, HomeHeadDelegate::new);
    }
    
    /**
     * 获取WebViewCardDelegate单例
     */
    public static WebViewCardDelegate getWebViewCardDelegate() {
        return (WebViewCardDelegate) getOrCreateDelegate(WebViewCardDelegate.class, WebViewCardDelegate::new);
    }
    
    /**
     * 获取FoodCardDelegate单例
     */
    public static FoodCardDelegate getFoodCardDelegate() {
        return (FoodCardDelegate) getOrCreateDelegate(FoodCardDelegate.class, FoodCardDelegate::new);
    }
    
    /**
     * 获取HotelCardDelegate单例
     */
    public static HotelCardDelegate getHotelCardDelegate() {
        return (HotelCardDelegate) getOrCreateDelegate(HotelCardDelegate.class, HotelCardDelegate::new);
    }
    
    /**
     * 获取TransportCardDelegate单例（机票）
     */
    public static TransportCardDelegate getPlaneCardDelegate() {
        return new TransportCardDelegate(ChatAdapter.TYPE_ASSISTANT_PLANE_CARD);
    }
    
    /**
     * 获取TransportCardDelegate单例（火车票）
     */
    public static TransportCardDelegate getTrainCardDelegate() {
        return new TransportCardDelegate(ChatAdapter.TYPE_ASSISTANT_TRAIN_CARD);
    }
    
    /**
     * 获取OrderCardDelegate单例
     */
    public static OrderCardDelegate getOrderCardDelegate() {
        return (OrderCardDelegate) getOrCreateDelegate(OrderCardDelegate.class, OrderCardDelegate::new);
    }
    
    /**
     * 获取H5CardDelegate单例
     */
    public static H5CardDelegate getH5CardDelegate() {
        return (H5CardDelegate) getOrCreateDelegate(H5CardDelegate.class, H5CardDelegate::new);
    }
    
    /**
     * 获取FloatPermissionCardDelegate单例
     */
    public static FloatPermissionCardDelegate getFloatPermissionCardDelegate() {
        return (FloatPermissionCardDelegate) getOrCreateDelegate(FloatPermissionCardDelegate.class, FloatPermissionCardDelegate::new);
    }
    
    /**
     * 获取AccessibilityPermissionCardDelegate单例
     */
    public static AccessibilityPermissionCardDelegate getAccessibilityPermissionCardDelegate() {
        return (AccessibilityPermissionCardDelegate) getOrCreateDelegate(AccessibilityPermissionCardDelegate.class, AccessibilityPermissionCardDelegate::new);
    }


    /**
     * 获取HomeHeadDelegate单例
     */
    public static MusicCardDelegate getMusicCardDelegate() {
        return (MusicCardDelegate) getOrCreateDelegate(MusicCardDelegate.class, MusicCardDelegate::new);
    }

    /**
     * 通用的获取或创建委托方法
     * 使用双重检查锁定模式确保线程安全
     */
    @SuppressWarnings("unchecked")
    private static <T extends ViewTypeDelegate> T getOrCreateDelegate(Class<T> delegateClass, DelegateSupplier<T> supplier) {
//        T delegate = (T) delegateCache.get(delegateClass);
//        if (delegate == null) {
//            synchronized (delegateCache) {
//                delegate = (T) delegateCache.get(delegateClass);
//                if (delegate == null) {
//                    delegate = supplier.get();
//                    delegateCache.put(delegateClass, delegate);
//                    android.util.Timber.tag(TAG).d( "Created singleton delegate: " + delegateClass.getSimpleName());
//                }
//            }
//        }
        return supplier.get();
    }
    
    /**
     * 委托供应商函数式接口
     */
    @FunctionalInterface
    private interface DelegateSupplier<T extends ViewTypeDelegate> {
        T get();
    }
    
    /**
     * 清除所有缓存的委托（仅用于测试或内存清理）
     */
    public static void clearCache() {
        synchronized (delegateCache) {
            delegateCache.clear();
            Timber.tag(TAG).d( "Cleared all delegate cache");
        }
    }
    
    /**
     * 获取当前缓存的委托数量（仅用于调试）
     */
    public static int getCacheSize() {
        return delegateCache.size();
    }

}