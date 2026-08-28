/**
 * @file cjson_guard.h
 * @brief cJSON RAII 包装器（内部头文件）
 *
 * ============================================================================
 * ⚠️ 内部头文件 - 请勿在 SDK 外部使用
 * ============================================================================
 *
 * 此文件提供 cJSON 对象的 RAII（资源获取即初始化）包装器，
 * 确保 cJSON 对象在作用域结束时自动释放，防止内存泄漏。
 *
 * 问题背景：
 * cJSON 是 C 风格的库，使用 cJSON_CreateObject() 创建对象后，
 * 必须调用 cJSON_Delete() 释放。如果函数中途 return，可能忘记释放。
 *
 * 解决方案：
 * 使用 RAII 模式，在构造时获取资源，析构时自动释放。
 * 即使发生异常或提前返回，析构函数也会被调用。
 *
 * 使用示例：
 * @code
 * cJSON* root = cJSON_CreateObject();
 * cJSONGuard guard(root);  // 自动管理生命周期
 *
 * // ... 即使这里 return，root 也会被自动释放 ...
 *
 * // 如果需要转移所有权（不让 guard 释放）
 * cJSON* transferred = guard.release();
 * @endcode
 *
 * @copyright Copyright (c) 2024
 */

#pragma once

#include "cJSON.h"

namespace ai_sdk {

/**
 * @class cJSONGuard
 * @brief cJSON 对象的 RAII 包装器
 *
 * 自动管理 cJSON 对象的生命周期，防止内存泄漏。
 *
 * 特性：
 * - 构造时接管 cJSON 对象所有权
 * - 析构时自动调用 cJSON_Delete()
 * - 支持 release() 转移所有权
 * - 禁止拷贝，防止双重释放
 * - 支持移动语义
 */
class cJSONGuard {
public:
    /**
     * @brief 构造函数，接管 cJSON 对象所有权
     * @param json cJSON 对象指针（可以为 nullptr）
     */
    explicit cJSONGuard(cJSON* json = nullptr) noexcept : json_(json) {}

    /**
     * @brief 析构函数，自动释放 cJSON 对象
     */
    ~cJSONGuard() {
        if (json_) {
            cJSON_Delete(json_);
        }
    }

    // 禁止拷贝（防止双重释放）
    cJSONGuard(const cJSONGuard&) = delete;
    cJSONGuard& operator=(const cJSONGuard&) = delete;

    /**
     * @brief 移动构造函数
     * @param other 被移动的对象
     */
    cJSONGuard(cJSONGuard&& other) noexcept : json_(other.json_) {
        other.json_ = nullptr;
    }

    /**
     * @brief 移动赋值运算符
     * @param other 被移动的对象
     * @return 当前对象的引用
     */
    cJSONGuard& operator=(cJSONGuard&& other) noexcept {
        if (this != &other) {
            if (json_) {
                cJSON_Delete(json_);
            }
            json_ = other.json_;
            other.json_ = nullptr;
        }
        return *this;
    }

    /**
     * @brief 获取管理的 cJSON 对象（不转移所有权）
     * @return cJSON 对象指针
     */
    cJSON* get() const noexcept {
        return json_;
    }

    /**
     * @brief 释放所有权，返回 cJSON 对象
     *
     * 调用后，guard 不再管理该对象，调用者负责释放。
     *
     * @return cJSON 对象指针
     */
    cJSON* release() noexcept {
        cJSON* tmp = json_;
        json_ = nullptr;
        return tmp;
    }

    /**
     * @brief 重置，释放当前对象并接管新对象
     * @param json 新的 cJSON 对象（可以为 nullptr）
     */
    void reset(cJSON* json = nullptr) noexcept {
        if (json_) {
            cJSON_Delete(json_);
        }
        json_ = json;
    }

    /**
     * @brief 检查是否持有有效对象
     * @return true 持有有效对象，false 为空
     */
    explicit operator bool() const noexcept {
        return json_ != nullptr;
    }

    /**
     * @brief 箭头运算符，方便访问 cJSON 成员
     * @return cJSON 对象指针
     */
    cJSON* operator->() const noexcept {
        return json_;
    }

private:
    cJSON* json_;  ///< 管理的 cJSON 对象
};

}  // namespace ai_sdk

