package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

/**
 * 事件常量定义类 - P0优先级事件
 *
 * 此文件自动生成，包含所有P0优先级的埋点事件
 * 数据来源: 灵犀智能体_2.0_App_埋点需求.xlsx
 * 生成时间: 2025-09-26
 *
 * @author 代码生成器
 */
object EventConstants {

    // 事件ID到事件名称的映射表 - P0优先级事件
    private val eventMap: Map<String, String> by lazy {
        hashMapOf(

            /*=========================P0 优先级事件 ====================================*/

            /*=========================P0 优先级事件 ====================================*/
        )
    }

    /**
     * 获取事件名称
     * @param eventId 事件ID
     * @return 对应的事件名称，如果未找到返回null
     */
    fun getEventName(eventId: String): String? = eventMap[eventId]

    /**
     * 检查指定事件ID是否存在
     * @param eventId 事件ID
     * @return true表示存在，false表示不存在
     */
    fun containsEvent(eventId: String): Boolean = eventMap.containsKey(eventId)

    /**
     * 系统行为 - P0优先级事件常量
     */
    object SystemBehavior {
        /** 启动 app  */
        const val APP_START = "100001"
    }

    /**
     * AI 办公模块 - 优先级事件常量
     */
    object AIOffice {
        /** ai_meeting_enter */
        const val AI_MEETING_ENTER = "200001"

        /** ai_meeting_start */
        const val AI_MEETING_START = "200002"

        /** ai_meeting_transcript */
        const val AI_MEETING_TRANSCRIPT = "200003"

        /** ai_meeting_qa_send */
        const val AI_MEETING_QA_SEND = "200006"

        /** ppt_topic_input */
        const val PPT_TOPIC_INPUT = "200007"

        /** ppt_topic_submit */
        const val PPT_TOPIC_SUBMIT = "200008"

        /** ai_drawing_enter */
        const val AI_DRAWING_ENTER = "200012"

        /** ai_drawing_style_select */
        const val AI_DRAWING_STYLE_SELECT = "200013"

        /** ai_drawing_prompt_input */
        const val AI_DRAWING_PROMPT_INPUT = "200014"

        /** ai_drawing_generate_submit */
        const val AI_DRAWING_GENERATE_SUBMIT = "200017"

        /** ai_drawing_result_view */
        const val AI_DRAWING_RESULT_VIEW = "200018"

        /** translate_mode_select */
        const val TRANSLATE_MODE_SELECT = "200019"

        /** translate_language_select */
        const val TRANSLATE_LANGUAGE_SELECT = "200020"

        /** translate_start */
        const val TRANSLATE_START = "200021"

        /** ppt_generation_complete */
        const val PPT_GENERATION_COMPLETE = "200037"

        /** ai_drawing_new_creation */
        const val AI_DRAWING_NEW_CREATION = "200042"

        /** translate_listen_mode */
        const val TRANSLATE_LISTEN_MODE = "200053"

        /** translate_dialogue_mode */
        const val TRANSLATE_DIALOGUE_MODE = "200054"

        /** translate_text_result */
        const val TRANSLATE_TEXT_RESULT = "200056"

        /** 进入 AI 办公页面 */
        const val ENTER_AI_OFFICE_PAGE = "200079"

    }

    /**
     * 智能体模块 - 优先级事件常量
     */
    object AIAgent {
        /** 火车票查询启动 */
        const val TRAIN_TICKET_QUERY_START = "300001"

        /** 机票查询启动 */
        const val FLIGHT_QUERY_START = "300002"

        /** 酒店预订启动 */
        const val HOTEL_BOOKING_START = "300003"

        /** 旅游计划生成 */
        const val TRAVEL_PLAN_GENERATE = "300004"

        /** 交通卡片点击 */
        const val TRANSPORT_CARD_CLICK = "300006"

        /** 视频摘要生成 */
        const val VIDEO_SUMMARY_GENERATE = "300021"

        /** 视频推荐展示 */
        const val VIDEO_RECOMMEND_SHOW = "300022"

        /** 卡片报告生成 */
        const val CARD_REPORT_GENERATE = "300031"

        /** 金融数据查询 */
        const val FINANCIAL_DATA_QUERY = "300032"

        /** 话费充值启动 */
        const val PHONE_RECHARGE_START = "300041"

        /** 话费余额查询 */
        const val PHONE_BALANCE_QUERY = "300042"

        /** 流量使用查询 */
        const val DATA_USAGE_QUERY = "300043"

        /** 研究任务创建 */
        const val RESEARCH_TASK_CREATE = "300051"

        /** 网络搜索启动 */
        const val WEB_SEARCH_START = "300053"

        /** 研究报告完成 */
        const val RESEARCH_REPORT_COMPLETE = "300055"

        /** 进入智能体页面 */
        const val ENTER_AGENT_PAGE = "300036"

    }

    /**
     * 核心对话模块 - 优先级事件常量
     */
    object CoreDialog {
        /** 语音输入开始 */
        const val VOICE_INPUT_START = "400001"

        /** 语音输入结束 */
        const val VOICE_INPUT_END = "400002"

        /** 文本消息发送 */
        const val TEXT_MESSAGE_SEND = "400008"

        /** TTS播放开始 */
        const val TTS_PLAY_START = "400011"

        /** TTS播放完成 */
        const val TTS_PLAY_COMPLETE = "400012"

        /** 模型切换 */
        const val MODEL_SWITCH = "400016"

        /** 模型性能监控 */
        const val MODEL_PERFORMANCE_MONITOR = "400018"

        /** 会话开始 */
        const val SESSION_START = "400030"

        /** 会话结束 */
        const val SESSION_END = "400031"

        /** 进入主页面  */
        const val ENTER_MAIN_PAGE = "400033"

        /** 胶囊位点击  */
        const val CAPSULE_POSITION_CLICK = "400034"

        /** 底部导航点击 */
        const val BOTTOM_NAVIGATION_CLICK = "400035"

    }

    /**
     * AI翻译 - 优先级事件常量
     */
    object AITranslation {
        /** ai_translate_enter */
        const val AI_TRANSLATE_ENTER = "600001"

        /** ai_translate_interface_show */
        const val AI_TRANSLATE_INTERFACE_SHOW = "600002"

        /** ai_translate_text_request */
        const val AI_TRANSLATE_TEXT_REQUEST = "600006"

        /** ai_translate_text_success */
        const val AI_TRANSLATE_TEXT_SUCCESS = "600007"

        /** ai_translate_text_failed */
        const val AI_TRANSLATE_TEXT_FAILED = "600008"

        /** ai_translate_voice_start */
        const val AI_TRANSLATE_VOICE_START = "600009"

        /** ai_translate_voice_result */
        const val AI_TRANSLATE_VOICE_RESULT = "600012"

    }

    /**
     * 用户登录 - 优先级事件常量
     */
    object UserLogin {
        /** 应用启动登录检查 */
        const val APP_STARTUP_LOGIN_CHECK = "700001"

        /** 一键登录页面展示 */
        const val ONE_CLICK_LOGIN_SHOW = "700002"

        /** 一键登录点击 */
        const val ONE_CLICK_LOGIN_CLICK = "700004"

        /** 一键登录结果 */
        const val ONE_CLICK_LOGIN_RESULT = "700007"

        /** 短信验证码页面展示 */
        const val SMS_CODE_PAGE_SHOW = "700008"

        /** 发送验证码点击 */
        const val SEND_CODE_CLICK = "700009"

        /** 短信登录提交 */
        const val SMS_LOGIN_SUBMIT = "700012"

        /** 短信登录结果 */
        const val SMS_LOGIN_RESULT = "700013"

        /** 密码登录提交 */
        const val PASSWORD_LOGIN_SUBMIT = "700016"

        /** 密码登录结果 */
        const val PASSWORD_LOGIN_RESULT = "700017"

        /** 登录成功跳转 */
        const val LOGIN_SUCCESS_REDIRECT = "700025"

        /** 登录错误处理 */
        const val LOGIN_ERROR_HANDLE = "700032"

    }

    /**
     * 我的 - 优先级事件常量
     */
    object MyProfile {
        /** 我的页面访问 */
        const val MY_PAGE_VISIT = "800001"

        /** 历史记录访问 */
        const val HISTORY_ACCESS = "800005"

        /** 设置页面访问 */
        const val SETTINGS_PAGE_VISIT = "800007"

        /** 大模型切换 */
        const val LLM_SWITCH = "800008"

        /** 注销账号点击 */
        const val LOGOUT_CLICK = "800015"

        /** 检查更新操作 */
        const val CHECK_UPDATE_OPERATION = "800017"

        /** 更新确认 */
        const val UPDATE_CONFIRM = "800018"

        /** 反馈页面访问 */
        const val FEEDBACK_PAGE_VISIT = "800021"

        /** 反馈提交成功 */
        const val FEEDBACK_SUBMIT_SUCCESS = "800022"

    }


    /**
     * 会话管理模块 - 优先级事件常量
     */
    object SessionManagement {
        /** 会话创建 */
        const val SESSION_CREATE = "900001"

        /** 会话异常中断 */
        const val SESSION_ABORT = "900002"

        /** 用户停止会话 */
        const val USER_STOP_SESSION = "900003"

        /** 任务完成 */
        const val TASK_COMPLETE = "900004"
    }

    /**
     * 唤醒按钮
     */
    object WakeUpManagement{
        //语音唤醒
        const val VOICE_WAKE_CREATE = "500001"
        //电源唤醒
        const val POWER_WAKE_CREATE = "500002"
        //键盘唤醒
        const val KEYBOARD_WAKE_CREATE = "500003"
        //唤醒我在
        const val WAKE_UP_CREATE = "500010"

        const val KEYBOARD_WAKE_UP = "500012"

        const val POWER_WAKE_UP = "500013"

        const val WIDGET_QUICK_ENTRY = "500014"
    }

    /**
     * 小组件
     */
    object WidgetManagement{

        const val AUTO_EXEC_PUBLISH_TASK = "1000002" // 左侧“发布任务”
        const val AUTO_EXEC_WATCH_TV = "1000003"      // 小入口-看电视
        const val AUTO_EXEC_PC_OFFICE = "1000004"     // 小入口-电脑办公
        const val AUTO_EXEC_FOOD_DELIVERY = "1000005" // 小入口-餐饮外卖
        const val AUTO_EXEC_SHOPPING_COMPARE = "1000006" // 小入口-购物比价

        const val AI_OFFICE_ASK_LINGXI = "1000007"    // 左侧“问问灵犀”
        const val AI_OFFICE_MEETING = "1000008"       // 小入口-AI会议
        const val AI_OFFICE_PPT = "1000009"           // 小入口-AIPPT
        const val AI_OFFICE_DEEP_RESEARCH = "1000010" // 小入口-深度研究
        const val AI_OFFICE_TRANSLATE = "1000011"     // 小入口-同声传译
    }
}
