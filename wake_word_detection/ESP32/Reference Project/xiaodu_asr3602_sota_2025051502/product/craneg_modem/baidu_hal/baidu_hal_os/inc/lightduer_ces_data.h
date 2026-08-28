/**
 * Copyright (2019) Baidu Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * File: lightduer_ces_data.h
 * Auth: Chen Xihao (chenxihao@baidu.com)
 * Desc: Define data type of ces
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CES_DATA_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CES_DATA_H

#include <stdint.h>
#include "baidu_json.h"
#include "lightduer_types.h"
/**
 * message format defined on http://wiki.baidu.com/pages/viewpage.action?pageId=476221534
 */

extern const char *CONTENT_EXTENSION;

typedef struct duer_dcs_player_velocity_stat_s {
    char *msg_id;               // 端消息的id，不可为空
    char *dialog_id;            // 端语音请求的唯一id,语音请求不可为空
    // char *player_type;          // 1：自有播放器，可控。2：第三方
    char *url;                  // 播放资源的url地址
    char *url_ip;               // 播放资源的url对应的ip
    int64_t directive_time;    // 设备端应用层收到播放指令的时间
    int64_t player_start_time; // 设备端应用层开始调用播放器的时间
    int64_t player_ready_time; // 设备端播放器告诉应用层，已下载到资源并开始播放（不等于开始出声）
    int64_t player_get_music_info_start_time; // 设备端应用层调用咪咕SDK音乐ID获取Url接口的开始时间，毫秒级本地系统时间戳
    int64_t player_get_music_info_end_time; // 设备端应用层调用咪咕SDK音乐ID获取Url接口的结束时间，毫秒级本地系统时间戳
    int player_get_music_info_retry_count; // 设备端应用层调用咪咕SDK音乐ID获取Url接口的重试次数
    int player_get_music_info_finish_type; // 设备端应用层调用咪咕SDK音乐:退出原因1：正常退出，2：超时退出，3：打断退出
    int request_type;           // 1:语音请求 2:点击操作请求 3:文本请求 4:播放器请求
    int asr_type;               // 1: 自动听音模式 2：按住说话模式
} duer_dcs_player_velocity_stat_t;

typedef struct duer_dcs_client_velocity_stat_s {
    char *msg_id;                       // 端消息的id，不可为空
    char *dialog_id;                    // 端语音请求的唯一id,语音请求不可为空
    int64_t request_start_time;        // 客户端向服务端发起请求的开始时间
    int64_t button_release_time;       // 按住说话模式（不可为空），用户抬起手的时间, -1表示不存在
    int64_t vad_end_time;              // 开启客户端VAD，客户端收到语音SDK vad end回调的时间，-1表示不存在
    int64_t stop_listen_time;          // 客户端收到StopListen指令的时间
    int64_t audio_send_end_time;       // ACL链路touch模式语音数据全部发送完成如果弱网该时间与button_release_t相差会比较大
    int64_t asr_result_time;       // 客户端收到语音 SDK 回调asr最终识别结果的时间（集成语音 SDK），否则收到云端返回的type 是 final 的RenderVoiceInputText指令时间
    char *asr_result_text;              // ASR 的最终结果，即 query
    int64_t duer_result_parsing_time;  // SDK开始解析指令数据的时间（非ASR上屏指令、非RenderHint指令的第一个渲染指令；对于无屏设备，是第一个Play 指令），此时还没有解析指令
    int64_t duer_result_time;          // SDK解析完指令数据（非ASR上屏指令、非RenderHint 指令的第一个渲染指令；对于无屏设备，是第一个Play 指令）的时间
    int64_t duer_result_enque_time;    // SDK把指令放入指令执行队列（非ASR上屏指令、非 RenderHint指令的第一个渲染指令；对于无屏设备，是第一个Play 指令）的时间
    int64_t duer_result_throw_time;    // SDK把指令数据抛给应用层的时间（非ASR上屏指令、非 RenderHint指令的第一个渲染指令；对于无屏设备，是第一个Play 指令）
    char *duer_directive_name;          // 渲染指令名称；对于无屏设备，是第一个Play 指令名称
    int64_t duer_speak_time;           // SDK解析完第一个speak指令的时间
    int64_t duer_speak_enque_time;     // SDK把speak指令放入指令执行队列的时间
    int64_t duer_speak_pop_time;       // SDK从指令执行队列取出 speak指令的时间
    int64_t tts_data_time;             // 客户端收到tts合成首包数据的时间
    int64_t tts_voice_time;            // 客户端播放出tts声音的时间
    int request_type;                   // 1:语音请求, 2:事件请求
    int asr_type;                       // 1:自动听音模式, 2：按住说话模式
} duer_dcs_client_velocity_stat_t;

typedef struct duer_ces_flow_data_s {
    int64_t start_time;
    int64_t end_time;
    baidu_json *event_list;
} duer_ces_flow_data_t;

baidu_json *duer_ces_json_device_status(void);

baidu_json *duer_ces_json_connent_data(const char* bussiness_type, const char* event_type,
                                       const char* event_flag, const char* event_value,
                                       const char* description, int error_type);

baidu_json *duer_ces_json_connent_data_mode(const char* bussiness_type, const char* event_type,
                                            const char* event_flag, const char* event_value,
                                            const char* event_mode, const char* description,
                                            int error_type);

baidu_json *duer_ces_json_connent_data_description_size(
        const char* bussiness_type, const char* event_type,
        const char* event_flag, const char* event_value,
        const char* description, int description_size, int error_type);

baidu_json *duer_ces_json_event_data(int type, baidu_json *content);

int duer_ces_add_event_to_list(baidu_json *event_list, const char *event_name,
                               int64_t timestamp, baidu_json* content);

baidu_json *duer_ces_json_flow_data(int type, duer_ces_flow_data_t *data, baidu_json *option);

baidu_json *duer_ces_json_common_data(baidu_json *event, int64_t min_time, int64_t max_time);

char *duer_ces_get_json_msg_with_name(const char *err_msg, const char *name);

char *duer_ces_get_json_err_msg(const char *err_msg);

char *duer_ces_get_json_desc(const char *err_msg);

int64_t duer_ces_timestamp(void);

#endif /* BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CES_DATA_H */
