package com.fxzs.lingxiagent.lingxi.lingxi_conversation;

import androidx.annotation.DrawableRes;

public class TabEntity {
    public static TabType agentType =  TabType.CHAT;

    // 对话tab
    public static final String TAB_ENTITY_CHAT_NAME = "对话";
    // 旅行规划tab
    public static final String TAB_ENTITY_TRAVEL_NAME = "旅行规划";
    // 同城聚会tab
    public static final String TAB_ENTITY_TRIP_NAME = "同城聚会";
    // 药品助手tab
    public static final String TAB_ENTITY_MEDICINE_NAME = "药品助手";
    // 翻译tab
    public static final String TAB_ENTITY_TRASLATE_NAME = "翻译";

    /**
     * 应用底部导航栏的标签类型
     */
    public enum TabType {
        /**
         * 聊天界面（默认首页）
         */
        CHAT(0),

        /**
         * 行程规划工具（普通版）
         */
        TRIP_PLANNER(1),

        /**
         * 医疗健康服务
         */
        MEDICINE(2),

        /**
         * 咖啡推荐与购买
         */
        COFFEE(3),

        /**
         * 旅游攻略与景点推荐
         */
        TRAVEL(4),

        /**
         * 实时翻译工具
         */
        TRANSLATE(5),

        /**
         * 荣耀定制版行程规划工具（增强功能）
         */
        TRIP_PLANNER_HONOR(6),

        /**
         * 写作
         */
        TRIP_AI_WRITING(7),

        /**
         * 智能体
         */
        TRIP_AI_AGENT(8);


        private final int value;
        TabType(int value) { this.value = value; }
        public int getValue() { return value; }
    }
    public static LocalModule matchLocalModule(TabType tabType) {
        switch (tabType) {
            case TRIP_PLANNER_HONOR: // 注意：此处无需类名前缀，直接使用枚举常量名
                return LocalModule.TRIP_HONOR;
            case TRIP_PLANNER:
                return LocalModule.TRIP;
            case MEDICINE:
                return LocalModule.MEDICINE;
            case TRANSLATE:
                return LocalModule.TRANSLATE;
            case TRAVEL:
                return LocalModule.TRAVEL;
            // 处理其他TabType枚举值（如CHAT、COFFEE等，避免遗漏）
            case CHAT:
            case COFFEE:
            default: // 必须有default分支（除非所有枚举值都已处理）
                return LocalModule.CHAT;
        }
    }

    private @DrawableRes int iconResId; // 图标资源ID     // 唯一标识符
    private String name;                // 标签名称     // 标签类型（自定义类型常量）

    public static void init() {
        agentType =  TabType.CHAT;
    }

    // Getters and Setters
    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    // 可选：重写toString()方法以便调试
    @Override
    public String toString() {
        return "Tab{" +
                "iconResId=" + iconResId +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
    private TabType type; // 修改为枚举类型

    // 构造函数
    public TabEntity(int iconResId, String name, TabType type) {
        this.iconResId = iconResId;
        this.name = name;
        this.type = type;
    }

    // Getter/Setter
    public TabType getType() { return type; }
    public void setType(TabType type) { this.type = type; }

//    public static List<TabEntity> createTabs() {
//        return Arrays.asList(
//                new TabEntity( R.drawable.dh_icon,TAB_ENTITY_CHAT_NAME, TabType.CHAT),
//                new TabEntity( R.drawable.lx_icon, TAB_ENTITY_TRAVEL_NAME, TabType.TRAVEL),
//                new TabEntity( R.drawable.cx_icon, TAB_ENTITY_TRIP_NAME, TabType.TRIP_PLANNER_HONOR),
//                // new TabEntity( R.drawable.cx_icon, "中兴同城聚会",TabEntity.TabType.TRIP_PLANNER),
////                new TabEntity( R.drawable.yp_icon, TAB_ENTITY_MEDICINE_NAME, TabType.MEDICINE),
//                // new TabEntity( R.drawable.kf_icon,"订咖啡", TabType.CHAT)
//                new TabEntity( R.drawable.fy_icon, TAB_ENTITY_TRASLATE_NAME, TabType.TRANSLATE)
//        );
//    }
}
