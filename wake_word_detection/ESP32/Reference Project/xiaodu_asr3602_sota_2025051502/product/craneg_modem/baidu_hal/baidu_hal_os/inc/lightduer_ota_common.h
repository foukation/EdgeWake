/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @file lightduer_ota_common.h
 * @author zhangguopeng01@baidu.com
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_OTA_COMMON_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_OTA_COMMON_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/** 下次ota使用的ota分区，从ota-1分区启动 */
#define DUER_OTA_BOOT_PART_1 0

/** 下次ota使用的ota分区，从ota-2分区启动 */
#define DUER_OTA_BOOT_PART_2 1

/** 分区文件名最大长度 */
#define DUER_OTA_PARTITION_NAME_MAX_LENGTH 32

/** 版本信息最大长度 */
#define DUER_OTA_VERSION_MAX_LENGTH 32

/** Release notes最大长度 */
#define DUER_OTA_RELEASE_NOTES_MAX_LENGTH 2048

/** device id最大长度 */
#define DUER_OTA_DEVICE_ID_MAX_LENGTH 32

/** hash value max size: md5 string length + 4 */
#define DUER_OTA_HASH_VALUE_MAX_LENGTH 36

/** bduss max length */
#define DUER_OTA_BDUSS_VALUE_MAX_LENGTH 256

/** YES */
#define DUER_OTA_YES 1

/** NO */
#define DUER_OTA_NO 0

/** @brief 下一启动分区 */
typedef enum _duer_ota_boot_part_t {
    /** 从DuerOS分区启动 */
    DUER_OTA_BOOT_PART_DUEROS,

    /** 从OTA分区1启动 */
    DUER_OTA_BOOT_PART_OTA_1,

    /** 从OTA分区2启动 */
    DUER_OTA_BOOT_PART_OTA_2,
} duer_ota_boot_part_t;

/** @brief 升级模式 */
typedef enum _duer_ota_upgrade_mode_t {
    /** 用户通过手机APP触发升级 */
    DUER_OTA_UPGRADE_MODE_APP,

    /** 云端推送升级 */
    DUER_OTA_UPGRADE_MODE_CLOUD,

    /** 静默升级 */
    DUER_OTA_UPGRADE_MODE_SILENCE,
} duer_ota_upgrade_mode_t;

/** @brief flash中存放的ota相关的key信息 */
typedef enum _duer_ota_flash_key_t {
    /** 当前设置的ota启动分区 */
    DUER_OTA_FLASH_KEY_OTA_BOOT_PART,

    /** 当前的ota升级mode */
    DUER_OTA_FLASH_KEY_UPGRADE_MODE,

    /** 当前固件版本 */
    DUER_OTA_FLASH_KEY_CURRENT_VERSION,

    /** 当前dueros分区的hash值 */
    DUER_OTA_FLASH_KEY_DUEROS_HASH_VALUE,

    /** 当前ota-1分区的hash值 */
    DUER_OTA_FLASH_KEY_OTA_1_HASH_VALUE,

    /** 当前ota-2分区的hash值 */
    DUER_OTA_FLASH_KEY_OTA_2_HASH_VALUE,

    /** 当前dsp分区的hash值 */
    DUER_OTA_FLASH_KEY_DSP_HASH_VALUE,

    /** 当前ring分区的hash值 */
    DUER_OTA_FLASH_KEY_RING_HASH_VALUE,

    /** ota written flag */
    DUER_OTA_FLASH_KEY_WRITTEN_FLAG,

    /** ota flag */
    DUER_OTA_FLASH_KEY_OTA_FLAG,

    /** ota flag for exception */
    DUER_OTA_FLASH_KEY_OTA_EXCEPTION_FLAG,

    /** flash key数目 */
    DUER_OTA_FLASH_KEY_MAX,
} duer_ota_flash_key_t;

/** @brief ota分区启动模式 */
typedef enum _duer_ota_boot_mode_t {
    /** 冷启动 */
    DUER_OTA_BOOT_MODE_COLD_BOOT,

    /** 热启动 */
    DUER_OTA_BOOT_MODE_WARM_BOOT,
} duer_ota_boot_mode_t;

/** @brief OTA调用结果 */
typedef enum _duer_ota_result_t {
    /** OK */
    DUER_OTA_RESULT_OK,

    /** 通用错误 */
    DUER_OTA_RESULT_FAILED,

    /** 内存不足 */
    DUER_OTA_RESULT_NO_ENOUGH_MEMORY,

    /** 参数错误 */
    DUER_OTA_RESULT_INVALID_PARAMETERS,

    /** xbu sdk wrapper init failed */
    DUER_OTA_RESULT_WRAPPER_INITIALIZE_FAILED,

    /** clear ota flag failed */
    DUER_OTA_RESULT_CLEAR_OTA_FLAG_FAILED,

    /** invalid prompt music */
    DUER_OTA_RESULT_INVALID_PROMPT_MUSIC,

    /** no more data */
    DUER_OTA_RESULT_NO_MORE_DATA,

    /** inquire device id failed */
    DUER_OTA_RESULT_INQUIRE_DEVICE_ID_FAILED,

    /** inquire current version failed */
    DUER_OTA_RESULT_INQUIRE_CURRENT_VERSION_FAILED,

    /** inquire new version failed */
    DUER_OTA_RESULT_INQUIRE_NEW_VERSION_FAILED,

    /** no new version available */
    DUER_OTA_RESULT_NO_NEW_VERISON_AVAILABLE,

    /** set ota flag failed */
    DUER_OTA_RESULT_SET_OTA_FLAG_FAILED,

    /** get ota boot part */
    DUER_OTA_RESULT_GET_OTA_BOOT_PART_FAILED,

    /** set ota boot part failed */
    DUER_OTA_RESULT_SET_OTA_BOOT_PART_FAILED,

    /** read ota flash info failed */
    DUER_OTA_RESULT_GET_OTA_FLASH_INFO_FAILED,

    /** ota write flash failed */
    DUER_OTA_RESULT_WRITE_FLASH_FAILED,

    /** OTA SDK未初始化 */
    DUER_OTA_RESULT_SDK_UNINITIALIZED,

    /** OTA SDK初始化失败 */
    DUER_OTA_RESULT_SDK_INITIALIZE_FAILED,

    /** get device id failed */
    DUER_OTA_RESULT_GET_DEVICE_ID_FAILED,

    /** 参数设置失败 */
    DUER_OTA_RESULT_SET_QUERY_PARAMETER_FAILED,

    /** UPDATE参数设置失败 */
    DUER_OTA_RESULT_SET_UPDATE_PARAMETER_FAILED,

    /** 查询版本失败 */
    DUER_OTA_RESULT_INQUIRE_VERSION_FAILED,

    /** 获取版本信息失败 */
    DUER_OTA_RESULT_GET_VERSION_FAILED,

    /** OTA SDK已经初始化完成 */
    DUER_OTA_RESULT_SDK_ALREADY_INITIALIZED,

    /** create ota manager failed */
    DUER_OTA_RESULT_CREATE_OTA_MANAGER_FAILED,

    /** 设置回调函数失败 */
    DUER_OTA_RESULT_SET_CALLBACK_FUNC_FAILED,

    /** WIFI模块已经初始化 */
    DUER_OTA_RESULT_WIFI_TASK_ALREADY_INITIALZED,

    /** WIFI模块未初始化 */
    DUER_OTA_RESULT_WIFI_TASK_NOT_INITIALIZED,

    /** WIFI模块calback函数注册失败 */
    DUER_OTA_RESULT_WIFI_REGISTER_CALLBACK_FAILED,

    /** read flash failed */
    DUER_OTA_RESULT_READ_FLASH_FAILED,

    /** 读取SN失败 */
    DUER_OTA_RESULT_READ_DEVICE_ID_FAILED,

    /** 读取current version失败 */
    DUER_OTA_RESULT_READ_CURRENT_VERSION_FAILED,

    /** 读取current version失败 */
    DUER_OTA_RESULT_WRITE_FALSH_FAILED,

    /** SDK运行中 */
    DUER_OTA_RESULT_SDK_IS_RUNNING,

    /** 启动分区校验失败 */
    DUER_OTA_RESULT_VALIDATE_IMAGE_FAIELD,

    /** 检测静默升级版本失败 */
    DUER_OTA_RESULT_CHECK_SILENCE_VERSION_FAILED,

    DUER_OTA_RESULT_MAX,
} duer_ota_result_t;

/** @brief upgradable partitions */
typedef enum _duer_ota_upgradable_partition_t {
    /** dueros分区： 对等于esp中的ota 0分区（factory分区），存放dueros主应用逻辑 */
    DUER_OTA_PARTITION_DUEROS,

    /** ota-1分区： 对等于esp中的ota 1分，存放ota逻辑 */
    DUER_OTA_PARTITION_OTA_1,

    /** ota-2分区： 对等于esp中的ota 2分区，存ota逻辑 */
    DUER_OTA_PARTITION_OTA_2,

    /** dsp分区：存放语音dsp image */
    DUER_OTA_PARTITION_DSP,

    /** ring分区：存放ring数据 */
    DUER_OTA_PARTITION_RING,

    DUER_OTA_PARTITION_MAX,
} duer_ota_upgradable_partition_t;

#ifdef __cplusplus
}
#endif

#endif /* BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_OTA_COMMON_H */
