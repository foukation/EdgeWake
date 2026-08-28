/**
 * @file main.c
 * @brief AI SDK 独立编译项目 - 空入口函数
 *
 * 该文件提供 ESP-IDF 项目必需的 app_main() 入口函数。
 * 
 * 说明：
 * - 这是一个编译专用项目，不需要实际运行
 * - app_main() 函数为空，仅用于满足 ESP-IDF 构建系统要求
 * - 编译完成后，AI SDK 静态库会自动保存到 components/ai_sdk/lib/${IDF_TARGET}/
 *
 * @copyright Copyright (c) 2024
 */

/**
 * @brief 应用程序入口点
 * 
 * ESP-IDF 项目必须提供此函数。
 * 在本编译项目中，此函数为空。
 */
void app_main(void)
{
    // AI SDK 静态库编译项目，无需实际运行
    // 编译完成后，静态库位于：
    // components/ai_sdk/lib/${IDF_TARGET}/libai_sdk.a
}

