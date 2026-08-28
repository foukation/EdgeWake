enum class AgentUseCode(val alias: Int) {
    USE(1),
    NOT(0),
}

enum class ResCode(val alias: Int) {
    SUC(0),// 成功
    AUTH_ERR(401), // 认证 token 错误
    PARAM_ERR(400),// 参数错误
    SERVE_ERR(500),// 服务内部服务器错误
    IMAGE_ERR(216101),// 图片格式错误、图片未传递
}
