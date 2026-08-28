package com.fxzs.lingxiagent.receiver;

import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.network.ZNet.bean.GUIWidgetBean;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.google.gson.Gson;

import java.util.List;


public class WidgetDataHelper {


    // ==============================================
    // 🔥 🔥 🔥 【兜底默认数据】你给的完整JSON写死在这里
    // ==============================================
    public static final String DEFAULT_WIDGET_JSON = "[\n" +
            "    {\n" +
            "      \"categoryKey\": \"office\",\n" +
            "      \"categoryTitle\": \"电脑办公\",\n" +
            "      \"displayText\": \"切换电脑办公\",\n" +
            "      \"actionCommands\": [\n" +
            "        \"打开云电脑\",\n" +
            "        \"切换到电脑办公\",\n" +
            "        \"进入Windows\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"categoryKey\": \"watch_tv\",\n" +
            "      \"categoryTitle\": \"看电视刷剧\",\n" +
            "      \"displayText\": \"帮我播放电视剧\",\n" +
            "      \"actionCommands\": [\n" +
            "        \"打开移动高清，看电视\",\n" +
            "        \"打开移动高清，播放CCTV1\",\n" +
            "        \"打开移动高清，播放电视剧红高粱\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"categoryKey\": \"takeaway_restaurant\",\n" +
            "      \"categoryTitle\": \"餐饮外卖\",\n" +
            "      \"displayText\": \"帮我订外卖/餐厅\",\n" +
            "      \"actionCommands\": [\n" +
            "        \"打开美团点一杯瑞幸咖啡的生椰拿铁，要热的、微甜\",\n" +
            "        \"点一杯星巴克的抹茶星冰乐，大杯\",\n" +
            "        \"用美团点一份板烧鸡腿堡套餐，换成中薯条和无糖可乐\",\n" +
            "        \"预订海底捞火锅，明天晚上5点半，6位\",\n" +
            "        \"帮我在大众点评上给萝岗万达的怂火锅5星好评，写30个字以上的点评\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"categoryKey\": \"transport_ticket\",\n" +
            "      \"categoryTitle\": \"交通出行\",\n" +
            "      \"displayText\": \"帮我打车/订票\",\n" +
            "      \"actionCommands\": [\n" +
            "        \"用携程订一张票从广州飞北京的南航大飞机，明天下午3点后第一班\",\n" +
            "        \"上携程买一张明天从广州到成都的高铁票，10点左右出发一等座\",\n" +
            "        \"帮我导航去长隆野生动物园，不走高速\",\n" +
            "        \"打开滴滴叫一辆快车，现在出发去公司\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"categoryKey\": \"hotel_booking\",\n" +
            "      \"categoryTitle\": \"订酒店\",\n" +
            "      \"displayText\": \"帮我订酒店\",\n" +
            "      \"actionCommands\": [\n" +
            "        \"打开携程帮我预订明天离广东省博物馆最近的舒适型酒店\",\n" +
            "        \"在携程帮我订明晚北京鸟巢附近全季酒店大床房，含2份早餐\",\n" +
            "        \"帮我用携程订一张广州飞北京的机票，今晚8点左右起飞,再帮我订一个北京鸟巢附近的全季酒店大床房，含早餐\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"categoryKey\": \"shopping\",\n" +
            "      \"categoryTitle\": \"电商购物\",\n" +
            "      \"displayText\": \"帮我购买商品\",\n" +
            "      \"actionCommands\": [\n" +
            "        \"在淘宝京东拼多多，搜索大疆pockect3，对比下单最便宜的\",\n" +
            "        \"帮我去淘宝一键价保\",\n" +
            "        \"打开拼多多拍照搜索同款\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"categoryKey\": \"ai_education\",\n" +
            "      \"categoryTitle\": \"教育学习\",\n" +
            "      \"displayText\": \"帮我总结知识点\",\n" +
            "      \"actionCommands\": [\n" +
            "        \"打开豆包，总结五年级数学第一单元内容并出20道练习题\",\n" +
            "        \"打开元宝，出50道三年级数学口算题\",\n" +
            "        \"帮我在豆包进行拍照答疑\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"categoryKey\": \"video_music_book\",\n" +
            "      \"categoryTitle\": \"影音娱乐\",\n" +
            "      \"displayText\": \"帮我播放电影/音乐\",\n" +
            "      \"actionCommands\": [\n" +
            "        \"打开酷狗音乐，播放如愿\",\n" +
            "        \"打开优酷视频下载电影萌宠特工队\",\n" +
            "        \"打开喜马拉雅，播放有声书百年孤独\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ]";

    // 防止实例化
    private WidgetDataHelper() {
        throw new UnsupportedOperationException("Static utility class");
    }

    /**
     * 数据回调接口
     */
    public interface DataCallback {
        /**
         * 数据加载成功回调
         * @param menuBeans 解析后的列表数据
         */
        void onSuccess(List<GUIWidgetBean> menuBeans);

        /**
         * 数据加载失败回调
         * @param e 异常信息
         */
        void onError(Throwable e);
    }

    /**
     * 获取分类列表并更新 Widget 视图
     */
    public static void loadAndBindWidgetData(DataCallback callback) {

        com.fxzs.lingxiagent.network.ZNet.HttpRequest request = new HttpRequest();
        request.getCategoryList(new Observer<ApiResponse<List<GUIWidgetBean>>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<List<GUIWidgetBean>> res) {
                if (res != null && res.getCode() == 0 && res.getData() != null) {
                    List<GUIWidgetBean> menuBeans = res.getData();
                    if (menuBeans != null && !menuBeans.isEmpty()) {
                        SharedPreferencesUtil.saveWidgetData(new Gson().toJson(menuBeans));
                        if (callback != null) {
                            callback.onSuccess(menuBeans);
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                // 记录日志
            }

            @Override
            public void onComplete() {
            }
        });
    }
}