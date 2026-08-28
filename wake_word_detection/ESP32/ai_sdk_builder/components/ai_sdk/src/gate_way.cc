/**
 * @file gate_way.cc
 * @brief GateWay 类实现文件
 *
 * 该文件实现了 GateWay 类的所有方法。
 * GateWay 提供网关配置、设备信息获取和数据上报的统一入口。
 *
 * 设计说明：
 * - GateWay 是一个薄薄的转发层
 * - 实际的 HTTP 逻辑、重试机制、错误处理都在 Client 类中实现
 * - 如果未来需要，可以在 GateWay 层添加统一的重试、缓存、监控等功能
 */

#include "ai_sdk/gate_way.h"
#include "ai_sdk/ai_assistant_manager.h"  // 用于获取 AIAssistantManager::getInstance()
#include "esp_log.h"
#include <string>

// 内部头文件（位于 src/include/）
// GateWay 的核心职责是转发调用到这些已经实现并测试过的 Client 类
// - GatewayClient: 处理网关/代理配置相关的网络请求
// - DeviceClient: 处理设备注册认证相关的网络请求
// - ReportClient: 处理数据上报和心跳相关的网络请求
#include "gateway_client.h"   // 网关客户端，提供 getGateWay() 实现
#include "device_client.h"    // 设备客户端，提供 obtainDeviceInformation() 实现
#include "report_client.h"    // 上报客户端，提供 dataReport() 实现

namespace ai_sdk {

/**
 * @brief 日志标签
 */
static const char* TAG = "GateWay";

/**
 * @brief 获取网关信息
 *
 * 从云端获取网关配置，包括代理服务器地址和认证令牌。
 *
 * 转发流程：
 * 1. 创建 GatewayClient 实例（shared_ptr 管理生命周期）
 * 2. 直接调用 client->getGateWay() 并透传回调函数
 * 3. Client 处理所有网络请求、解析、错误重试等复杂逻辑
 * 4. 获得结果后，Client 会调用传入的 onSuccess/onError 回调通知 GateWay
 * 5. GateWay 不做任何干预，让结果直接返回给调用者
 *
 * 应用场景：
 * - 设备首次启动时获取代理配置
 * - 代理令牌过期时重新获取
 * - 根据网络环境选择合适的代理服务器
 *
 * 成功后，SDK 会自动更新全局 ApiConfig 配置，后续请求会自动使用代理。
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 * 
 * 问题：原实现使用栈上对象，函数返回后 Client 被销毁，但异步任务还在运行
 * 
 * 原代码：
 *   GatewayClient client;  // 栈上分配
 *   client.getGateWay(...);
 *   // 函数返回，client 销毁，但 HTTP 任务还在运行！
 *   // 回调执行时访问已销毁的对象 → 崩溃
 * 
 * 修复方案：使用 shared_ptr 延长 Client 生命周期
 *   - 在 lambda 中捕获 shared_ptr，确保回调执行时 Client 仍有效
 *   - 当回调执行完成，lambda 销毁，shared_ptr 引用计数归零，Client 被正确销毁
 *
 * @param onSuccess 成功回调，返回网关信息（代理地址、令牌、有效期）
 * @param onError 错误回调，返回错误信息
 */
void GateWay::getGateWay(
    GatewaySuccessCallback onSuccess,
    GatewayErrorCallback onError
) {
    ESP_LOGD(TAG, "Getting gateway configuration...");

    // ========================================================================
    // 使用 shared_ptr 管理 GatewayClient 生命周期
    // ========================================================================
    // 改用 shared_ptr 替代栈上对象，确保在异步回调执行期间 Client 对象有效
    //
    // 生命周期说明：
    // 1. 创建 shared_ptr，引用计数 = 1
    // 2. lambda 捕获 client，引用计数 = 2
    // 3. 函数返回，局部 client 销毁，引用计数 = 1（lambda 仍持有）
    // 4. 异步回调执行完成，lambda 销毁，引用计数 = 0
    // 5. GatewayClient 被正确销毁
    auto client = std::make_shared<GatewayClient>();

    // 调用 getGateWay()，在 lambda 中捕获 client 以延长生命周期
    // 注意：这里捕获 client 是关键，确保回调执行时 GatewayClient 仍然有效
    client->getGateWay(
        [client, onSuccess](const GatewayInfo& info, const std::string& message) {
            // client 捕获确保 GatewayClient 在回调期间有效
            if (onSuccess) {
                onSuccess(info, message);
            }
        },
        [client, onError](const std::string& error) {
            // client 捕获确保 GatewayClient 在回调期间有效
            if (onError) {
                onError(error);
            }
        }
    );

    ESP_LOGD(TAG, "GatewayRequest forwarded to GatewayClient");

    // 注意：这里没有错误处理，因为所有错误都通过 onError 回调返回
    // 这是异步编程的最佳实践：错误不通过返回值，而是通过回调通知
}

/**
 * @brief 获取设备信息
 *
 * 设备通过产品信息及设备号从云端获取设备信息，前提是设备号已录入到云端平台。
 *
 * 转发流程：
 * 1. 从 AIAssistantManager::getInstance().config() 获取配置（每次调用都获取）
 * 2. 从配置中提取设备信息和产品信息
 * 3. 构建 DeviceInfoRequest 对象（栈上分配）
 * 4. 创建 DeviceClient 实例（shared_ptr 管理生命周期）
 * 5. 调用 client->obtainDeviceInformation(request, onSuccess, onError)
 * 6. Client 处理所有网络请求、解析响应、错误重试等复杂逻辑
 * 7. 结果通过 onSuccess/onError 回调返回给调用者
 *
 * 使用回调函数来处理成功和错误的情况
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 * 
 * 使用 shared_ptr 管理 DeviceClient 生命周期，确保异步回调执行时对象有效
 * 详见 getGateWay() 的注释说明
 *
 * @param onSuccess 成功回调，返回设备信息（deviceId 和 deviceSecret）
 * @param onError 错误回调，返回错误信息
 */
void GateWay::obtainDeviceInformation(
    DeviceInfoSuccessCallback onSuccess,
    DeviceInfoErrorCallback onError
) {
    ESP_LOGD(TAG, "Obtaining device information...");

    // 从 AIAssistantManager 获取配置
    // 每次调用都从 AIAssistantManager 获取，不存储成员变量
    // 这样做的好处：GateWay 完全无状态，职责单一（只做转发）
    const auto& config = AIAssistantManager::getInstance().config();

    // 从配置构建请求参数
    DeviceInfoRequest request;
    request.deviceNoType = config.deviceNoType;  // 设备号类型（如 "SN"）
    request.deviceNo = config.deviceNo;          // 设备序列号
    request.productId = config.productId;        // 产品 ID（从云端平台获得）
    request.productKey = config.productKey;      // 产品密钥（从云端平台获得）

    ESP_LOGD(TAG, "Building DeviceInfoRequest from AIAssistantManager::config():");
    ESP_LOGD(TAG, "  deviceNoType: %s", request.deviceNoType.c_str());
    ESP_LOGD(TAG, "  deviceNo: %s", request.deviceNo.c_str());
    ESP_LOGD(TAG, "  productId: %s", request.productId.c_str());
    // 注意：productKey 不打印到日志，避免泄露敏感信息

    // ========================================================================
    // 使用 shared_ptr 管理 DeviceClient 生命周期
    // ========================================================================
    // 改用 shared_ptr 替代栈上对象，确保在异步回调执行期间 Client 对象有效
    auto client = std::make_shared<DeviceClient>();

    // 调用 DeviceClient 的 obtainDeviceInformation()
    // 在 lambda 中捕获 client 以延长生命周期
    client->obtainDeviceInformation(request,
        [client, onSuccess](const DeviceInfoResponse& response) {
            // client 捕获确保 DeviceClient 在回调期间有效
            if (onSuccess) {
                onSuccess(response);
            }
        },
        [client, onError](const std::string& error) {
            // client 捕获确保 DeviceClient 在回调期间有效
            if (onError) {
                onError(error);
            }
        }
    );

    ESP_LOGD(TAG, "DeviceInfoRequest forwarded to DeviceClient");

    // 注意：这里没有错误处理，因为所有错误（包括网络错误、认证错误等）
    // 都通过 onError 回调返回。这是异步编程的最佳实践。
}

/**
 * @brief 数据上报
 *
 * 心跳接口/上报接口（定时向云端发送消息）
 * 设备（24小时至少上报一次）向云端上报信息，更新最后活动时间。
 *
 * 转发流程：
 * 1. 调用者构建 DeviceReportRequest（包含 eventType、params 等）
 * 2. 接收调用者传入的 deviceReportRequest 参数（无需 GateWay 自己构建）
 * 3. 创建 ReportClient 实例（shared_ptr 管理生命周期）
 * 4. 调用 client->dataReport(deviceReportRequest, onSuccess, onError)
 * 5. Client 处理所有网络请求、JSON 序列化、错误处理、重试等复杂逻辑
 * 6. 结果通过 onSuccess/onError 回调返回给调用者
 *
 * 请求策略（参考）：
 * 设备应该每隔12小时向平台上报设备数据信息。
 * 实施步骤：
 * 1. 设备初始化：设备首次启动时，向平台上报数据并记录上报时间 T_current。
 * 2. 计算下次上报时间：设备每次上报数据后，记录当前时间 T_current，并计算下次上报时间 T_next：
 *    T_next = T_current + 12 小时 + 随机偏移量
 *    其中，随机偏移量可以是在-15分钟到+15分钟之间的一个随机值。
 * 3. 调度上报任务：设备根据计算出的 T_next 设置定时任务，确保在该时间点上报数据。
 * 注意：避免固定时间集中上报，造成服务器压力过大。
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 * 
 * 使用 shared_ptr 管理 ReportClient 生命周期，确保异步回调执行时对象有效
 * 详见 getGateWay() 的注释说明
 *
 * @param deviceReportRequest 包含设备报告请求的数据对象（由调用者构建）
 * @param onSuccess 成功回调，当数据上报成功时调用
 * @param onError 错误回调，当数据上报失败时调用
 */
void GateWay::dataReport(
    const DeviceReportRequest& deviceReportRequest,
    ReportSuccessCallback onSuccess,
    ReportErrorCallback onError
) {
    ESP_LOGD(TAG, "Sending data report...");

    // ========================================================================
    // 使用 shared_ptr 管理 ReportClient 生命周期
    // ========================================================================
    // 改用 shared_ptr 替代栈上对象，确保在异步回调执行期间 Client 对象有效
    auto client = std::make_shared<ReportClient>();

    // 调用 dataReport()，在 lambda 中捕获 client 以延长生命周期
    client->dataReport(deviceReportRequest,
        [client, onSuccess](const DeviceReportResponse& response) {
            // client 捕获确保 ReportClient 在回调期间有效
            if (onSuccess) {
                onSuccess(response);
            }
        },
        [client, onError](const std::string& error) {
            // client 捕获确保 ReportClient 在回调期间有效
            if (onError) {
                onError(error);
            }
        }
    );

    ESP_LOGD(TAG, "DeviceReportRequest forwarded to ReportClient");

    // 注意：这里没有错误处理，因为所有错误都通过 onError 回调返回
    // 这是异步编程的最佳实践：错误不通过返回值，而是通过回调通知
}

/**
 * @brief HTTP响应处理回调 - 设备信息
 */
void GateWay::onDeviceInfoResponse(const std::string& response,
                                   DeviceInfoSuccessCallback onSuccess,
                                   DeviceInfoErrorCallback onError) {
    // TODO: 实现响应解析
    if (onError) {
        onError("Not implemented");
    }
}

/**
 * @brief HTTP响应处理回调 - 网关信息
 */
void GateWay::onGatewayResponse(const std::string& response,
                                GatewaySuccessCallback onSuccess,
                                GatewayErrorCallback onError) {
    // TODO: 实现响应解析
    if (onError) {
        onError("Not implemented");
    }
}

/**
 * @brief HTTP响应处理回调 - 上报
 */
void GateWay::onReportResponse(const std::string& response,
                               ReportSuccessCallback onSuccess,
                               ReportErrorCallback onError) {
    // TODO: 实现响应解析
    if (onError) {
        onError("Not implemented");
    }
}

} // namespace ai_sdk
