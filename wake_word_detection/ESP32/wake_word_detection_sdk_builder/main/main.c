/**
 * @file main.c
 * @brief Wake Word Detection SDK 独立编译项目 - 空入口函数
 *
 * 该文件提供 ESP-IDF 项目必需的 app_main() 入口函数。
 *
 * 说明：
 * - 这是一个编译专用项目，不需要实际运行
 * - app_main() 函数为空，仅用于满足 ESP-IDF 构建系统要求
 * - 编译完成后，SDK 静态库会自动保存到
 *   components/wake_word_detection_sdk/lib/${IDF_TARGET}/libwake_word_detection_sdk.a
 */

void app_main(void)
{
    // 唤醒 SDK 静态库编译项目，无需实际运行
}
