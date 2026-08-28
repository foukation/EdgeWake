package com.fxzs.lingxiagent.lingxi.lingxi_conversation;

import java.util.Arrays;
import java.util.List;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2025/8/21 上午10:38
 */
public class HomeModelEntity {
    public enum ModelType {
        /**
         * 灵犀模型
         */
        LING_XI_MODEL(0),

        /**
         * 其他模型
         */
        OTHER_MODEL(1);

        private final int value;
        ModelType(int value) { this.value = value; }
        public int getValue() { return value; }
    }


    private String name;
    private int type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public HomeModelEntity( String name) {
        this.name = name;
    }
    public static List<HomeModelEntity> createHeadEntity(ModelType modelType){
        if (modelType == ModelType.LING_XI_MODEL){
            return Arrays.asList(new HomeModelEntity("充值话费"),
                    new HomeModelEntity("流量查询"),
                    new HomeModelEntity("话费余额"),
                    new HomeModelEntity("查询全球通权益"));
        }
        return Arrays.asList(new HomeModelEntity("今日热点"),
                new HomeModelEntity("生成一份祝福文案"),
                new HomeModelEntity("查一下这个季节怎么养生"),
                new HomeModelEntity("帮我写一篇我的祖国"));

    }
}
