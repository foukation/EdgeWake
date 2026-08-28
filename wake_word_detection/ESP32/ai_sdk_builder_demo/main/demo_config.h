/**
 * @file demo_config.h
 * @brief AI SDK Demo 配置文件
 *
 * 接入方只需修改此文件中的配置项即可运行 Demo。
 * 所有凭证信息均需从云端平台获取。
 */
#pragma once

// ============================================================================
// WiFi 配置
// ============================================================================

/** WiFi SSID（路由器名称） */
#define DEMO_WIFI_SSID          "CMCC-Yjxc"
/* #define DEMO_WIFI_SSID          "TP-LINK_551A" */
/** WiFi 密码 */
#define DEMO_WIFI_PASSWORD      "9fad3f78"
/* #define DEMO_WIFI_PASSWORD      "12345678901234567890" */

// ============================================================================
// 产品凭证配置（从云端平台获取）
// ============================================================================

/**
 * 设备号
 * 产品内唯一标识设备的序列号
 * 示例："AIXL2026010001"
 */
#define DEMO_DEVICE_NO          "AIXL2026010001"

/**
 * 设备号类型
 * 支持 "MAC"、"SN"、"IMEI"
 */
#define DEMO_DEVICE_NO_TYPE     "SN"

/**
 * 产品 ID
 * 平台创建产品时生成的唯一标识
 */
#define DEMO_PRODUCT_ID         "2031262928870518785"

/**
 * 产品密钥
 * 平台创建产品时生成的认证密钥
 */
#define DEMO_PRODUCT_KEY        "SPVdcFklfsGPLmDp"

/**
 * 中控配置版本号
 * 需邮件申请
 */
#define DEMO_CENTRAL_CONFIG_VER "3"

/**
 * 认证 Token
 * 平台分配的认证令牌
 */
#define DEMO_TOKEN              "sk-crwLQ3MEel44LsGW1273601f7e6b472584634f4b27C35414"