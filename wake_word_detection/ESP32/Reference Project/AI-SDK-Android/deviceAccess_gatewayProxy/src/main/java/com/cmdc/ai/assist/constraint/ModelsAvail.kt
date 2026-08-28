import com.cmdc.ai.assist.constraint.ErrorContent

/*
 * 获取可用模型列表
 */

internal data class ModelsPermissionContent(
    val id: String,                       // 权限唯一标识
    val `object`: String,              // 对象类型
    val created: Int,                  // 创建时间戳
    val allow_create_engine: Boolean,  // 是否允许创建引擎
    val allow_sampling: Boolean,       // 是否允许采样
    val allow_logprobs: Boolean,       // 是否允许日志概率
    val allow_search_indices: Boolean, // 是否允许搜索索引
    val allow_view: Boolean,           // 是否允许查看
    val allow_fine_tuning: Boolean,   // 是否允许微调
    val organization: String,           // 组织
    val group: String,                   // 分组
    val is_blocking: Boolean,           // 是否被阻止
)


internal data class ModelsContent(
    val id: String,            // 模型唯一标识
    val `object`: String,    // 对象类型
    val created: Int,        // 创建时间戳
    val owned_by: String,    // 模型所有者
    val permission: Array<ModelsPermissionContent>,    // 模型权限列表
    val root: String,        // 模型根标识
    val parent: String,        // 父模型标识
)

internal data class ModelsAvailRes(
    val data: Array<ModelsContent>,  // 模型内容
    val `object`: String,            // 响应对象类型
    val code: Int,                   // 状态码
    val error: ErrorContent?         // 错误信息内容
)
