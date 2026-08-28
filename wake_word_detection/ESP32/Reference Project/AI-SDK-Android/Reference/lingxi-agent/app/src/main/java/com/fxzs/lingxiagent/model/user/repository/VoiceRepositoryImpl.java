package com.fxzs.lingxiagent.model.user.repository;

import com.fxzs.lingxiagent.model.chat.dto.VoiceSettingBean;

import java.util.ArrayList;
import java.util.List;

/**
 * VoiceRepositoryImpl
 * 该类是 VoiceRepository 接口的实现类，用于处理语音相关的数据访问操作
 *
 * @author 于海生
 */
public class VoiceRepositoryImpl implements VoiceRepository {
    private static final String TAG = VoiceRepositoryImpl.class.getSimpleName();

    private final List<VoiceSettingBean> options = new ArrayList<>();

    public VoiceRepositoryImpl() {
    }

    public List<VoiceSettingBean> getDefaultOptions() {
        return options;
    }

    public void loadDefaultVoiceOptions() {
        // 免费版本
        options.add(new VoiceSettingBean("小美-标准女主播", "资讯", 0, false));
        options.add(new VoiceSettingBean("小宇-亲切男声", "对话助手", 1, false));
        options.add(new VoiceSettingBean("逍遥-情感男声", "小说", 3, false));
        options.add(new VoiceSettingBean("丫丫-童声", "小说", 4, false));

        // 付费版本
        options.add(new VoiceSettingBean("逍遥-情感男声", "小说", 5003, false));
        options.add(new VoiceSettingBean("小鹿-甜美女声", "对话助手", 5118, false));
        options.add(new VoiceSettingBean("博文-专业男主播", "资讯", 106, false));
        options.add(new VoiceSettingBean("米朵-可爱童声", "对话助手", 103, false));
        options.add(new VoiceSettingBean("小童-童声主播", "资讯", 110, false));
        options.add(new VoiceSettingBean("小萌-软萌妹子", "小说", 111, false));
        options.add(new VoiceSettingBean("小娇-成熟女主播", "资讯", 5, false));
        options.add(new VoiceSettingBean("逍遥-情感男声", "小说", 4003, false));
        options.add(new VoiceSettingBean("博文-专业男主播", "资讯", 4106, false));
        options.add(new VoiceSettingBean("小贤-电台男主播", "资讯", 4115, false));
        options.add(new VoiceSettingBean("常盈-电台女主播", "资讯", 5147, false));
        options.add(new VoiceSettingBean("小皮-萌娃童声", "资讯", 5976, false));
        options.add(new VoiceSettingBean("皮特-老外男声", "资讯", 5971, false));
        options.add(new VoiceSettingBean("阿肯-主播男声", "资讯", 4164, false));
        options.add(new VoiceSettingBean("有为-磁性男声", "资讯", 4176, false));
        options.add(new VoiceSettingBean("小新-播音女声", "资讯", 4259, false));
        options.add(new VoiceSettingBean("小鹿-甜美女声", "对话助手", 4119, false));
        options.add(new VoiceSettingBean("灵儿-清激女声", "对话助手", 4105, false));
        options.add(new VoiceSettingBean("小乔-活泼女声", "对话助手", 4117, false));
        options.add(new VoiceSettingBean("晴岚-甜美女声", "对话助手", 4288, false));
        options.add(new VoiceSettingBean("青川-温柔男声", "对话助手", 4192, false));
        options.add(new VoiceSettingBean("小雯-活力女主播", "资讯", 4100, false));
        options.add(new VoiceSettingBean("米朵-可爱女声", "对话助手", 4103, false));
        options.add(new VoiceSettingBean("姗姗-娱乐女声", "配音", 4144, false));
        options.add(new VoiceSettingBean("小贝-知识女主播", "资讯", 4278, false));
        options.add(new VoiceSettingBean("清风-配音男声", "配音", 4143, false));
        options.add(new VoiceSettingBean("小新-专业女主播", "资讯", 4140, false));
        options.add(new VoiceSettingBean("小彦-知识男主播", "资讯", 4129, false));
        options.add(new VoiceSettingBean("星河-广告男声", "配音", 4149, false));
        options.add(new VoiceSettingBean("小清-广告女声", "配音", 4254, false));
        options.add(new VoiceSettingBean("博文-综艺男声", "配音", 4206, false));
        options.add(new VoiceSettingBean("云朵-可爱童声", "配音", 4147, false));
        options.add(new VoiceSettingBean("婉婉-甜美女声", "配音", 4141, false));
        options.add(new VoiceSettingBean("南方-电台女主播", "资讯", 4226, false));
        options.add(new VoiceSettingBean("悠然-旁白男声", "小说", 6205, false));
        options.add(new VoiceSettingBean("云萱-旁白女声", "小说", 6221, false));
        options.add(new VoiceSettingBean("清豪-逍遥侠客", "小说", 6546, false));
        options.add(new VoiceSettingBean("清柔-温柔男神", "小说", 6602, false));
        options.add(new VoiceSettingBean("雨楠-元气少女", "小说", 6562, false));
        options.add(new VoiceSettingBean("雨萌-邻家女孩", "小说", 6543, false));
        options.add(new VoiceSettingBean("书古-情感男声", "小说", 6747, false));
        options.add(new VoiceSettingBean("书严-沉稳男声", "小说", 6748, false));
        options.add(new VoiceSettingBean("书道-沉稳男声", "小说", 6746, false));
        options.add(new VoiceSettingBean("书宁-亲和女声", "小说", 6644, false));
        options.add(new VoiceSettingBean("小夏-甜美女声", "小说", 4148, false));
        options.add(new VoiceSettingBean("西贝-脱口秀女声", "配音", 4277, false));
        options.add(new VoiceSettingBean("阿龙-说书男声", "配音", 4114, false));
        options.add(new VoiceSettingBean("常悦-民生女主播", "资讯", 5153, false));
        options.add(new VoiceSettingBean("小乐-可爱童声", "对话助手", 6561, false));

        // 定制版本
        options.add(new VoiceSettingBean("泽言-温暖男声", "超拟人单情感", 4179, false));
        options.add(new VoiceSettingBean("禧禧-阳光女声", "超拟人单情感", 4146, false));
        options.add(new VoiceSettingBean("小柔-温柔女声", "超拟人单情感", 6567, false));
        options.add(new VoiceSettingBean("言浩-年轻男声", "超拟人单情感", 4156, false));
        options.add(new VoiceSettingBean("涵竹-开朗女声", "超拟人多情感", 4189, false));
        options.add(new VoiceSettingBean("嫣然-活泼女声", "超拟人多情感", 4194, false));
        options.add(new VoiceSettingBean("泽言-开朗男声", "超拟人多情感", 4193, false));
        options.add(new VoiceSettingBean("怀安-磁性男声", "超拟人多情感", 4195, false));
        options.add(new VoiceSettingBean("清影-甜美女声", "超拟人多情感", 4196, false));
        options.add(new VoiceSettingBean("沁遥-知性女声", "超拟人多情感", 4197, false));
        options.add(new VoiceSettingBean("小粤-粤语女声", "方言", 20100, false));
        options.add(new VoiceSettingBean("晓芸-粤语女声", "方言", 20101, false));
        options.add(new VoiceSettingBean("四川小哥-四川男声", "方言", 4257, false));
        options.add(new VoiceSettingBean("阿闽-闽南男声", "方言", 4132, false));
        options.add(new VoiceSettingBean("小蓉-四川女声", "方言", 4139, false));
        options.add(new VoiceSettingBean("台媒女声-台湾女声", "方言", 5977, false));
        options.add(new VoiceSettingBean("小台-台湾女声", "方言", 4007, false));
        options.add(new VoiceSettingBean("湘玉-陕西女声", "方言", 4150, false));
        options.add(new VoiceSettingBean("阿锦-东北女声", "方言", 4134, false));
        options.add(new VoiceSettingBean("筱林-天津女声", "方言", 4172, false));
        options.add(new VoiceSettingBean("阿花-上海女声", "方言", 5980, false));
        options.add(new VoiceSettingBean("老崔-北京男声", "方言", 4154, false));
    }

    public List<VoiceSettingBean> loadDefaultVoiceOptions_() {
        options.add(new VoiceSettingBean("阳光甜妹", "女 | 青年｜中国台湾口音｜温柔", "", true));
        options.add(new VoiceSettingBean("磁性俊男", "男 | 青年｜帅气", "", false));
        options.add(new VoiceSettingBean("轻熟御姐", "女 | 青年｜自信", "", false));
        options.add(new VoiceSettingBean("霸道总裁粤语", "男 | 青年｜霸道", "", false));
        options.add(new VoiceSettingBean("撒娇学妹", "女 | 青年｜娇气", "", false));
        options.add(new VoiceSettingBean("谦谦君子", "男 | 青年｜谦虚", "", false));
        options.add(new VoiceSettingBean("魅力苏菲", "女 | 青年｜温柔", "", false));
        options.add(new VoiceSettingBean("邻家男孩", "男 | 青年｜自信", "", false));
        options.add(new VoiceSettingBean("邻家女孩", "女 | 青年｜耐心", "", false));
        return options;
    }

}