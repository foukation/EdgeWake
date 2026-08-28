package com.example.service_api.data

data class MatchConditions(
    val way: Int = 1,         // 控件匹配方法
    val target: String = "",  // 控件匹配标识
)

data class ConditionsContent(
    var conditions: ArrayList<MatchConditions>?,  // 匹配条件
)

data class ActionsContent(
    var actions: ArrayList<Action>,  // 匹配条件
)

data class Action(
    val actionId: String = "",         // 动作ID
    val actionDesc: String = "",       // 描述
    val type: Int = 1,                 // 类型
    var state: Int = 0,                // 状态

    var isFirst: Boolean? = false,     // 是否第一项
    var isLast: Boolean? = false,      // 是否最后一项
    var preExecuteId: String? = null,  // 上一节点ID
    var nextExecuteId: String? = null, // 下一节点ID

    var deeplinkUrl: String? = null,     //
    var deeplinkUrlArgs: String? = null, //

    var watchResult: Any? = null,      // 监控结果
    val firstExecTimer: Int = 1500,    // 首次监控延迟时间
    val interval: Int = 500,           // 监控间隔时间
    val maxExcNum: Int = 10,           // 监控最大执行次数
    var curExuNum: Int = 0,            // 监控当前执行次数
    val execDelayTimer: Int = 0,       // 执行延迟时间
    val appId: Int? = null,                      // app信息

    val execWay: Int = 1,                        // 执行方式
    val matchOcrTarget: String = "",             // OCR匹配文本
    val matchNodeType: Int = 1,                  // 控件匹配方式
    val matchConditions: String = "",            // 匹配条件
    var matchNodeWithActive: Int = 0,            // startNode使用激活节点
    var matchNodeInvisible: Int = 0,             // 控件节点是否不可见（webView）
    val matchNodeMaxDepth: Int = 30,             // 递归最大深度

    val slotType: Int? = null,                   // 槽位类型
    val slotKeyList: String = "",                // 槽位匹配
    val slotDefaultValue: String = "Agent",      // 槽位默认值

    var matchConditionsFormat: ArrayList<MatchConditions>?,  // 匹配条件
    var isOcrAddition: Int? = 0,    // 是否检验选择
    var isOcrDate: Int? = 0         // 是否点击日期
)

data class ClientApiActionsRes(
    val code: Int,
    val msg: String,
    var status: String,
    val data: AgentTaskData,
)

data class IntentData (
    var intent: String,
    var slots: SlotsData,
)

data class SlotsData (
    val product: String?,
    val item: String?,
    val recipient: String?,
    val location: String?,
    val messageContent: String?,
    val destination: String?,
    val drinkName: String?,
    val shop: String?,
    val receiver: String?,
    val content: String?,
    var store: String?,
    var times: String?,
    var hotel: String?,
    var name: String?,
    var direction: String?,
    var device: String?,
    var `object`: String?,
    var application: String?,
    var departure: String?,
    var transportation: String?,
    var date: String?,
    var departure_date: String?,
    var arrival_city: String?,
    var departure_city: String?,
    var drink: String?,
    var sugar: String?,
    var temperature: String?,
    var check_in_date: String?,
    var check_out_date: String?,
    var city: String?,
    var hotel_name: String?
)

data class AgentTaskData(
    val id: Int,
    val desc: String,
    val appId: String,
    val typeId: Int,
    val appName: String,
    val queryIntent: String,
    val actionList: String,
    var execNum: Int,
    val intents: ArrayList<IntentData>?
)
