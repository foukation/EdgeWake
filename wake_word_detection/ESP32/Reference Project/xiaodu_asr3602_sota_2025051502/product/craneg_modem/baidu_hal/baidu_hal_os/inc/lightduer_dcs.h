/**
 * Copyright (2017) Baidu Inc. All rights reserved.
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
 * File: lightduer_dcs.h
 * Auth: Gang Chen (chengang12@baidu.com)
 * Desc: Light duer DCS APIS.
 */
#ifndef BAIDU_DUER_LIGHTDUER_DCS_H
#define BAIDU_DUER_LIGHTDUER_DCS_H

#include "baidu_json.h"
#include "lightduer_types.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    DCS_PLAY_CMD,
    DCS_PAUSE_CMD,
    DCS_PREVIOUS_CMD,
    DCS_NEXT_CMD,
    DCS_PLAY_CONTROL_EVENT_NUMBER,
} duer_dcs_play_control_cmd_t;

typedef enum {
    DCS_MEDIA_ERROR_UNKNOWN,               // unknown error
    // server invalid response, such as bad request, forbidden, not found .etc
    DCS_MEDIA_ERROR_INVALID_REQUEST,
    DCS_MEDIA_ERROR_SERVICE_UNAVAILABLE,   // device cannot connect to server
    DCS_MEDIA_ERROR_INTERNAL_SERVER_ERROR, // server failed to handle device's request
    DCS_MEDIA_ERROR_INTERNAL_DEVICE_ERROR, // device internal error
    DCS_MEDIA_ERROR_RESOURCE_OFFLINE,      // audio resource offline
    DCS_MEDIA_ERROR_CARTON_ERROR,          // play audio stutter
    DCS_MEDIA_ERROR_LOCAL_FILE_ERROR,      // play local file error
} duer_dcs_audio_error_t;

typedef enum {
    DCS_RECOMMEND_POWER_ON,
    DCS_RECOMMEND_OUT_OF_BOX,
    DCS_RECOMMEND_TIME_NUMBER,
} duer_dcs_recommend_time_t;

typedef struct {
    const char *url;
    int offset;
    const char *audio_item_id;
} duer_dcs_audio_info_t;

enum duer_dcs_device_capability {
    DCS_TTS_HTTPS_PROTOCAL_SUPPORTED = 0x01, // the device support https protocal to playing tts
    DCS_WECHAT_SUPPORTED             = 0x02, // the device support wechat
};

typedef enum {
    DCS_RESTART_LEVEL_UNSPECIFIED, // restart level unspecified
    DCS_RESTART_LEVEL_SYSTEM,      // system level restart
    DCS_RESTART_LEVEL_LAUNCHER,    // restart launcher
} duer_dcs_restart_level_t;

typedef enum {
    DCS_RESTART_MODE_UNSPECIFIED,  // restart mode unspecified
    DCS_RESTART_MODE_NORMAL,       // normal restart
    DCS_RESTART_MODE_SILENCE,      // restart device silently
} duer_dcs_restart_mode_t;

typedef enum {
    DCS_TARGET_DEFAULT,
    DCS_TARGET_BT,
    DCS_TARGET_ALARM,
    DCS_TARGET_HOURLYCHIME,
    DCS_TARGET_CALL_MEDIA, // reserved
    DCS_TARGET_CALL_RING,  // reserved
    DCS_TARGET_COUNT,
} duer_dcs_volume_target_t;

/**
 * Internal used private namespace.
 */
extern const char *DCS_PRIVATE_NAMESPACE;

/**
 * Initialize the dcs framework.
 *
 * @return none.
 */
void duer_dcs_framework_init(void);

/**
 * Uninitialize the DCS module.
 *
 * @return none.
 */
void duer_dcs_uninitialize(void);

/**
 * DESC:
 * Initialize dcs voice input interface.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_voice_input_init(void);

/**
 * DESC:
 * Notify DCS when recorder start to record.
 *
 * PARAM: none
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_on_listen_started(void);

/**
 * DESC:
 * Notify dcs module before listen start(open mic), the ListenStarted
 * event would be return.
 *
 * PARAM: none
 *
 * @RETURN: pointer point to the ListenStarted event json struct.
 */
baidu_json *duer_dcs_listen_start_prepare(void);

/**
 * DESC:
 * Notify dcs module to listen start(open mic)
 *
 * @param[in] parameter: asr parameter
 * @param[in] event: ListenStarted event returned by duer_dcs_listen_start_prepare
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_listen_start(void *parameter, baidu_json *event);

/**
 * DESC:
 * There is no VAD in translate scene,
 * hence developer needs to call this function to notify DCS framework after recording finished.
 *
 * PARAM: none
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_on_listen_stopped(void);

/**
 * DESC:
 * Developer needs to implement this interface to start recording.
 *
 * PARAM: none
 *
 * @RETURN: none.
 */
void duer_dcs_listen_handler(void);

/**
 * DESC:
 * Developer needs to implement this interface to stop recording.
 *
 * PARAM: none
 *
 * @RETURN: none.
 */
void duer_dcs_stop_listen_handler(void);

/**
 * DESC:
 * Initialize dcs voice output interface
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_voice_output_init(void);

/**
 * DESC:
 *
 * It should be called when speech finished, used to notify DCS level.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_speech_on_finished(void);

/**
 * DESC:
 *
 * It should be called when speech stopped, used to notify DCS level.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_speech_on_stopped(void);

/**
 * DESC:
 *
 * Used to play tts data flow.
 *
 * PARAM[in] data: the tts flow data
 * PARAM[in] size:  the data size
 *
 * @RETURN: none
 */
void duer_dcs_tts_flow_handler(const char *data, size_t size);

/**
 * DESC:
 * Developer needs to implement this interface to play speech.
 *
 * PARAM:
 * @param[in] url: the url of the speech need to play
 *
 * @RETURN: none.
 */
void duer_dcs_speak_handler(const char *url);

/**
 * DESC:
 * Developer needs to implement this interface to stop speech.
 *
 * PARAM:
 * @param: none
 *
 * @RETURN: none.
 */
void duer_dcs_stop_speak_handler(void);

/**
 * DESC:
 * Initialize dcs speaker controler interface to enable volume control function.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_speaker_control_init(void);

/**
 * DESC:
 * Notify DCS when volume changed
 *
 * PARAM:
 * @param[in] target: which volume changed
 * @param[in] volume: current target volume
 * @param[in] is_mute: current target mute state
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_on_volume_changed(duer_dcs_volume_target_t target,
                               int volume,
                               duer_bool is_mute);

/**
 * DESC:
 * Notify DCS when mute state changed.
 *
 * PARAM:
 * @param[in] target: which volume mute
 * @param[in] volume: current target volume
 * @param[in] is_mute: current target mute state
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_on_mute(duer_dcs_volume_target_t target,
                     int volume,
                     duer_bool is_mute);

/**
 * DESC:
 * Developer needs to implement this interface, it is used to get volume state.
 * The caller should free the json memory.
 *
 * @RETURN: json of speaker state.
 */
baidu_json *duer_dcs_get_speaker_state(void);

/**
 * DESC:
 * Developer needs to implement this interface to set volume.
 *
 * PARAM:
 * @param[in] volume: the value of the volume need to set
 * @param[in] target: the target volume
 *
 * @RETURN: none.
 */
void duer_dcs_volume_set_handler(int volume, duer_dcs_volume_target_t target);

/**
 * DESC:
 * Developer needs to implement this interface to adjust volume.
 *
 * PARAM:
 * @param[in] volume: the value need to adjusted.
 * @param[in] target: the target volume
 *
 * @RETURN: none.
 */
void duer_dcs_volume_adjust_handler(int volume, duer_dcs_volume_target_t target);

/**
 * DESC:
 * Developer needs to implement this interface to change mute state.
 *
 * PARAM:
 * @param[in] is_mute: set/discard mute.
 * @param[in] target: the target volume
 *
 * @RETURN: none.
 */
void duer_dcs_mute_handler(duer_bool is_mute, duer_dcs_volume_target_t target);

/**
 * DESC:
 * Initialize dcs audio player interface.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_audio_player_init(void);

/**
 * DESC:
 * report a LinkClicked event.
 * User can also use this API to subscribe FM.
 *
 * PARAM[in] url: the url identify the FM.
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_on_link_clicked(const char *url);

/**
 * DESC:
 * report a LinkClicked event with url and initiator.type and params.
 *
 * PARAM[in] url: the clicked url.
 * PARAM[in] type: initiator.type, should be "USER_CLICK" or "AUTO_TRIGGER".
 * PARAM[in] params: json params, params is destroyed after the call.
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_on_link_clicked_detail(const char *url, const char *type, baidu_json *params);
int duer_dcs_only_on_link_clicked_detail(const char *url, const char *type, baidu_json *params);

/**
 * DESC:
 * Notify DCS when an audio is nearly finished.
 *
 * PARAM: none
 *
 * @RETURN: none.
 */
void duer_dcs_audio_on_nearly_finished(void);

/**
 * DESC:
 * Notify DCS when an audio is started.
 *
 * PARAM: none
 *
 * @RETURN: none.
 */
void duer_dcs_audio_on_started(void);

/**
 * DESC:
 * Notify DCS when an audio is finished.
 *
 * PARAM: none
 *
 * @RETURN: none.
 */
void duer_dcs_audio_on_finished(void);

/**
 * DESC:
 * Notify DCS when failed to play audio.
 *
 * PARAM[in] type: the error type
 * PARAM[in] msg: the error message
 * PARAM[in] host: the host name of audio resource
 * PARAM[in] ip: the resolved ip of the host name
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_audio_play_failed(duer_dcs_audio_error_t type, const char *msg,
        const char *host, const char * ip);

/**
 * DESC:
 * Report StreamMetadataExtracted event if metadata is found in the audio.
 *
 * PARAM[in] metadata: the metadata need to report, its layout is:
 *                     "metadata": {
 *                         "{{STRING}}": "{{STRING}}",
 *                         "{{STRING}}": {{BOOLEAN}}
 *                         "{{STRING}}": "{{STRING NUMBER}}"
 *                         ...
 *                     }
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_audio_report_metadata(baidu_json *metadata);

/**
 * DESC:
 * Notify DCS when audio is stuttered.
 *
 * PARAM[in] is_stuttuered: DUER_TRUE when stutter started(download speed lower than play speed),
 *                          DUER_FALSE when stutter finished(the audio resume play).
 * PARAM[in] err_msg: error msg
 * PARAM[in] host: host name of the audio resource
 * PARAM[in] ip: resolved ip of the host name
 */
int duer_dcs_audio_on_stuttered(duer_bool is_stuttered, const char *err_msg,
        const char *host, const char *ip);

/**
 * DESC:
 * Developer needs to implement this interface to pre-download audio.
 * The audio should be download, but don't play it until duer_dcs_audio_play_handler is called.
 *
 * PARAM:
 * @param[in] audio_info: the info of the audio need to pre-download.
 *
 * @RETURN: none.
 */
void duer_dcs_audio_download_handler(const duer_dcs_audio_info_t *audio_info);

/**
 * DESC:
 * Developer needs to implement this interface to play audio.
 *
 * PARAM:
 * @param[in] audio_info: the info of the audio need to play
 *
 * @RETURN: 0 for ok, -1 for failed.
 */
int duer_dcs_audio_play_handler(const duer_dcs_audio_info_t *audio_info);

/**
 * DESC:
 * Developer needs to implement this interface to stop audio player.
 *
 * PARAM: none
 *
 * @RETURN: none.
 */
void duer_dcs_audio_stop_handler(void);

/**
 * DESC:
 * Notify DCS when an audio is stopped not by DCS API.
 *
 * PARAM: none
 *
 * @RETURN: none.
 */
void duer_dcs_audio_on_stopped(void);

/**
 * DESC:
 * Developer needs to implement this interface to resume audio play.
 *
 * PARAM:
 * @param[in] audio_info: the info of the audio need to resumed
 *
 * @RETURN: none.
 */
void duer_dcs_audio_resume_handler(const duer_dcs_audio_info_t *audio_info);

/**
 * DESC:
 * Developer needs to implement this interface to pause audio play.
 *
 * PARAM: none
 *
 * @RETURN: none.
 */
void duer_dcs_audio_pause_handler(void);

/**
 * DESC:
 * Developer needs to implement this interface, it's used to get the audio play progress.
 *
 * PARAM: none
 *
 * @RETURN: the play position of the audio.
 */
int duer_dcs_audio_get_play_progress(void);

/**
 * DESC:
 * Realize play control(play, pause, next/previous audio) by send command to DCS.
 *
 * PARAM[in] cmd: command type.
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_send_play_control_cmd(duer_dcs_play_control_cmd_t cmd);

/**
 * DESC:
 * Report current state after device boot.
 *
 * PARAM: none
 *
 * @RETURN: 0 if succuss, negative if failed.
 */
int duer_dcs_sync_state(void);

/**
 * DESC:
 * Sending an "Exited" event to close the multi dialogue.
 *
 * @RETURN: 0 if succuss, negative if failed.
 */
int duer_dcs_close_multi_dialog(void);

/**
 * DESC:
 * Initialize dcs screen interface.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_screen_init(void);

/**
 * DESC:
 * Developer needs to implement this interface to get the ASR result.
 *
 * PARAM[in] text: the ASR text result.
 * PARAM[in] type: "INTERMEDIATE" or "FINAL".
 *
 * @RETURN: DUER_OK if success,
 *          DUER_MSG_RSP_BAD_REQUEST if the payload is invalid,
 *          DUER_ERR_FAILED if other error happened.
 */
duer_status_t duer_dcs_input_text_handler(const char *text, const char *type);

/**
 * DESC:
 * Developer needs to implement this interface to get render card.
 *
 * PARAM[in] payload: the information of the render card, please reference the DCS document.
 *
 * @RETURN: DUER_OK if success,
 *          DUER_MSG_RSP_BAD_REQUEST if the payload is invalid,
 *          DUER_ERR_FAILED if other error happened.
 */
duer_status_t duer_dcs_render_card_handler(baidu_json *payload);

/**
 * DESC:
 * Developer needs to implement this interface to open/close bluetooth.
 *
 * PARAM[in] is_switch: open bluetooth if is_switch is DUER_TRUE, otherwise close bluetooth.
 * PARAM[in] target: Reserved parameter, currently its value is "default".
 *
 * @RETURN: None
 */
void duer_dcs_bluetooth_set_handler(duer_bool is_switch, const char *target);

/**
 * DESC:
 * Get dcs creen view state, caller should free the state by baidu_json_delete.
 *
 * @RETURN: dcs creen view state.
 */
baidu_json *duer_screen_get_view_state(void);

/**
 * DESC:
 * Developer needs to implement this interface to connect/disconnect bluetooth to device.
 *
 * PARAM[in] is_connect: connect bluetooth if is_connect is DUER_TRUE, otherwise disconnect bluetooth.
 * PARAM[in] target: Reserved parameter, currently its value is "default".
 *
 * @RETURN: None
 */
void duer_dcs_bluetooth_connect_handler(duer_bool is_connect, const char *target);

/**
 * DESC:
 * Developer needs to implement this interface to open/close microphone.
 *
 * PARAM[in] is_switch: open microphone if is_switch is DUER_TRUE, otherwise close microphone.
 * PARAM[in] target: Reserved parameter, currently its value is "default".
 *
 * @RETURN: None
 */
void duer_dcs_microphone_set_handler(duer_bool is_switch, const char *target);

/**
 * DESC:
 * Initialize device control interface, such as control the bluetooth.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_device_control_init(void);

/**
 * DESC:
 * Initialize text input interface.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_text_input_init(void);

/**
 * DESC:
 * Used to declare the device capability, such as: whether https or wechat are supported.
 * It is unnecessary if the device don't have the capability defined in duer_dcs_device_capability.
 * @PARAM[in] capability: the device capability, it's the or value of the member in
 *                        duer_dcs_device_capability.
 * @RETURN:   DUER_OK if success, negative if failed.
 */
duer_status_t duer_dcs_capability_declare(duer_u32_t capability);

/**
 * DESC:
 * Sending RecommendationRequested event, and DCS server will recommend a resource to device.
 * Currently, only the scene of "POWER_ON" is supported, that means this function should be called
 * only one time(when device power_on).
 *
 * @PARAM[in] type: type of the scene.
 * @RETURN: 0 if succuss, negative if failed.
 */
int duer_dcs_recommend_request(duer_dcs_recommend_time_t time);

/**
 * DESC:
 * Similar to duer_dcs_on_listen_started, except not create new dialog id, but get current one.
 *
 * PARAM: none
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_on_listen_started_with_current_dialogid(void);

/**
 * DESC:
 * Developer needs to implement this interface to pause local player.
 *
 * PARAM: none
 *
 * @RETURN: None
 */
void duer_dcs_local_player_pause_handler(void);

/**
 * DESC:
 * Developer needs to implement this interface to resume local player.
 *
 * PARAM: none
 *
 * @RETURN: None
 */
void duer_dcs_local_player_resume_handler(void);

/**
 * DESC:
 * Developer needs to implement this interface to stop local player.
 *
 * PARAM: none
 *
 * @RETURN: None
 */
void duer_dcs_local_player_stop_handler(void);

/**
 * DESC:
 * Notify DCS module when local player playing.
 *
 * PARAM[in] offset: the play offset in milliseconds
 *
 * @RETURN: None
 */
void duer_dcs_local_player_on_played(int offset);

/**
 * DESC:
 * Notify DCS module when local player stopped.
 *
 * PARAM[in] offset: the play offset in milliseconds
 *
 * @RETURN: None
 */
void duer_dcs_local_player_on_stopped(int offset);

/**
 * DESC:
 * Notify DCS module when local player finished.
 *
 * PARAM[in] offset: the play offset in milliseconds
 *
 * @RETURN: None
 */
void duer_dcs_local_player_on_finished(int offset);

/**
 * DESC:
 * Checking whether local player is paused by higher play channel.
 *
 * @RETURN: Reture DUER_TRUE if local player is paused, else returen DUER_FALSE.
 */
duer_bool duer_local_player_is_paused(void);

/**
 * DESC:
 * Get the play offset in millseconds of local audio player.
 *
 * @RETURN: the play offset.
 */
int duer_dcs_local_player_get_play_progress(void);

/**
 * DESC:
 * Checking whether local player is playing.
 *
 * @RETURN: Reture DUER_TRUE if local player is playing, else returen DUER_FALSE.
 */
duer_bool duer_local_player_is_playing(void);

/**
 * DESC:
 * Get the client context.
 *
 * PARAM: none
 *
 * @RETURN: NULL if failed, pointer point to the client context if success.
 */
baidu_json *duer_dcs_get_client_context(void);

/**
 * DESC:
 * Initialize dcs ota interface.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_ota_init(void);

/**
 * DESC:
 * Developer needs to implement this interface to update firmware.
 *
 * PARAM: none
 *
 * @RETURN: None
 */
duer_status_t duer_dcs_ota_update_handler(baidu_json *payload);

/**
 * DESC:
 * Initialize dcs push-service interface.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_push_service_init(void);

void duer_dcs_push_service_deinit(void);

/**
 * DESC:
 * Initialize dcs power interface.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_power_init(void);


void duer_dcs_power_deinit(void);
/**
 * DESC:
 * Developer needs to implement this interface to power off.
 *
 * PARAM: none
 *
 * @RETURN: None
 */
void duer_dcs_power_off_handler(void);

/**
 * DESC:
 * Developer needs to implement this interface to restart device.
 *
 * PARAM[in] level: restart level
 * PARAM[in] mode:  restart mode
 *
 * @RETURN: None
 */
void duer_dcs_restart_handler(duer_dcs_restart_level_t level, duer_dcs_restart_mode_t mode);

/**
 * DESC:
 * Initialize dcs scree extended card interface.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_screen_ext_card_init(void);
void duer_dcs_device_video_player_init(void);

/**
 * DESC:
 * To get render audio list.
 * Caller should free the json after used by baidu_json_delete
 *
 * PARAM: none
 *
 * @RETURN: render audio list.
 */
baidu_json *duer_dcs_audio_get_audio_list();

/**
 * DESC:
 * To get render player info.
 * Caller should free the json after used by baidu_json_delete
 *
 * PARAM: none
 *
 * @RETURN: render player info.
 */
baidu_json *duer_dcs_audio_get_player_info();

/**
 * DESC:
 * To get speaker controller info.
 * Caller should free the json after used by baidu_json_delete
 *
 * PARAM: none
 *
 * @RETURN: speaker controller info.
 */
baidu_json *duer_dcs_speaker_controller_info(void);

/**
 * DESC:
 * To get alert info.
 * Caller should free the json after used by baidu_json_delete
 *
 * PARAM: none
 *
 * @RETURN: alert info.
 */
baidu_json *duer_dcs_alert_info(void);

/**
 * DESC:
 * To get audio player info.
 * Caller should free the json after used by baidu_json_delete
 *
 * PARAM: none
 *
 * @RETURN: audio player info.
 */
baidu_json *duer_dcs_audio_player_info(void);

/**
 * DESC:
 * User should implement this interface to update the location setting.
 *
 * PARAM[in] enable: enable the location state or not
 *
 * @RETURN: none.
 */
void duer_dcs_location_update_setting_handler(duer_bool enable);

/**
 * DESC:
 * Report the location setting changed event.
 *
 * PARAM[in] enable the location state or not
 *
 * @RETURN: 0 when success, negative when fail.
 */
duer_status_t duer_dcs_location_setting_changed_report(duer_bool enable);

/**
 * DESC:
 * Report the child mode changed event.
 *
 * PARAM[in] enable the child mode or not
 *
 * @RETURN: 0 when success, negative when fail.
 */
duer_status_t duer_dcs_child_mode_changed_report(duer_bool enable);

/**
 * DESC:
 * Report sleep protect changed event.
 *
 * PARAM[in] enable sleep protect or not
 *
 * @RETURN: 0 when success, negative when fail.
 */
duer_status_t duer_dcs_sleep_protect_mode_changed_report(duer_bool enable);

/**
 * DESC:
 * get sleep protect assets
 *
 * PARAM[in] none
 *
 * @RETURN: assets
 */
char *duer_dcs_get_sleep_protect_assets();

/**
 * DESC:
 * set sleep protect assets
 *
 * PARAM[in] assets
 *
 * @RETURN: none
 */
void duer_dcs_set_sleep_protect_assets(char *assert);

/**
 * DESC:
 * sleep protect mode handle
 *
 * PARAM[in] enable sleep protect mode or not
 *
 * @RETURN: none
 */
void duer_dcs_sleep_protect_mode_set_handler(duer_bool enable,
                                             char *turn_on_time,
                                             char *turn_off_time,
                                             char *assets);

/**
 * DESC:
 * User should implement this interface to get sleep protect mode.
 *
 * PARAM[in] sleep_protect_mode: get the status of sleep protect mode
 *
 * @RETURN: none.
 */
duer_bool duer_dcs_get_sleep_protect_mode_status();

/**
 * DESC:
 * User should implement this interface to set child mode.
 *
 * PARAM[in] enable: enable the child mode or not
 *
 * @RETURN: none.
 */
void duer_dcs_child_mode_set_handler(duer_bool enable);

/**
 * DESC:
 * User should implement this interface to get child mode.
 *
 * PARAM[in] enable: enable the child mode or not
 *
 * @RETURN: none.
 */
void duer_dcs_get_child_mode_status(duer_bool *child_mode);

/**
 * DESC:
 * User should implement this interface to get led state.
 * @PARAM:  none.
 * @RETURN: DUER_TRUE if displaying, DUER_FALSE if not displaying.
 */
duer_bool duer_dcs_is_led_displaying(void);

/**
 * DESC:
 * User should implement this interface to parse led structure.
 * @PARAM:  none.
 * @RETURN: DUER_TRUE if success, DUER_FALSE if failed.
 */
duer_status_t duer_parse_led_structure(baidu_json *visual_led_controller);

/**
 * DESC:
 * User should implement this interface to clear led structure.
 * @PARAM:  none.
 * @RETURN: DUER_TRUE if success, DUER_FALSE if failed.
 */
duer_status_t duer_clear_led_structure(void);

/**
 * DESC:
 * Used to control whether print DCS directive or not.
 *
 * PARAM[in] enable: print DCS directive or not
 *
 * @RETURN: none.
 */
void duer_dcs_print_directive(duer_bool enable);

/**
 * DESC:
 * Used to get whether print DCS directive or not.
 *
 * PARAM[in] none.
 *
 * @RETURN: true: print DCS directive.
 */
duer_bool is_duer_dcs_print_directive(void);

/**
 * DESC:
 * User should implement this interface to start crab log report for debug.
 * @PARAM:  none.
 * @RETURN: DUER_OK if success, negative if failed.
 */
duer_status_t duer_dcs_start_crab_log_handler(void);

/**
 * DESC:
 * User     User should implement this interface to stop crab log report.
 * @PARAM:  none.
 * @RETURN: DUER_OK if success, negative if failed.
 */
duer_status_t duer_dcs_stop_crab_log_handler(void);

/**
 * DESC:
 * Used to get TTS: cloud will response a TTS whose content is "text"
 *
 * PARAM[in] text: the text need to convert to TTS
 *
 * @RETURN: 0 when success, negative when fail.
 */
int duer_dcs_user_interact(const char *text);

/**
 * DESC:
 * Used to get create a new dialogue request id
 *
 * PARAM: None.
 *
 * @RETURN: the const char of dialogue request id.
 */
const char *duer_create_request_id();

void duer_voice_changed_report(int voice_id);

void duer_dcs_set_voice_hdlr(int voice_id);

void duer_dcs_voice_print_init(void);

duer_status_t duer_dcs_render_player_info_hdl(baidu_json *payload);
duer_status_t duer_dcs_render_audio_list_hdl(baidu_json *payload);
duer_status_t duer_dcs_render_weather_hdl(baidu_json *payload);

void duer_dcs_location_init(void);
baidu_json *duer_dcs_get_gps_state();

void duer_dcs_screen_ext_card_watch_pay_init(void);
duer_status_t duer_dcs_render_watch_pay_hdl(baidu_json *payload);
duer_status_t duer_dcs_render_watch_auth_hdl(baidu_json *payload);

void duer_dcs_authentication_init(void);
duer_status_t duer_dcs_require_login_hdl(baidu_json *payload);
duer_status_t duer_dcs_access_token_invalid_hdl();

void duer_dcs_thirdparty_init(void);
duer_status_t duer_dcs_activity_notify_hdl(baidu_json *payload);
duer_status_t duer_share_bind_cb_hdl(baidu_json *payload);
duer_status_t duer_vip_status_cb_hdl(baidu_json *payload);
duer_status_t duer_cloudapp_status_cb_hdl(baidu_json *payload);
duer_status_t duer_dcs_watch_vip_products_hdl(baidu_json *payload);
duer_status_t duer_dcs_watch_skill_list_hdl(baidu_json *payload);
duer_status_t duer_dcs_sync_skill_cfg_hdl(baidu_json *payload);
duer_status_t duer_dcs_sync_skill_time_hdl(baidu_json *payload);
duer_status_t duer_dcs_watch_disable_skill_hdl(baidu_json *payload);

void duer_dcs_screen_ext_card_watch_confirm_init(void);

/**
 * DESC:
 * Developer needs to implement this interface to get confirm info.
 *
 * PARAM[in] payload: the information of confirm, please reference the DCS document.
 *
 * @RETURN: DUER_OK if success,
 *          DUER_MSG_RSP_BAD_REQUEST if the payload is invalid,
 *          DUER_ERR_FAILED if other error happened.
 */

duer_status_t duer_dcs_render_ask_for_confirm_hdl(baidu_json *payload);

/**
 * DESC:
 * Developer needs to implement this interface to get dumi recharge info.
 *
 * PARAM[in] payload: the information of dumi recharge, please reference the DCS document.
 *
 * @RETURN: DUER_OK if success,
 *          DUER_MSG_RSP_BAD_REQUEST if the payload is invalid,
 *          DUER_ERR_FAILED if other error happened.
 */

duer_status_t duer_dcs_render_dumi_recharge_hdl(baidu_json *payload);

/**
 * DESC:
 * Developer needs to implement this interface to get dumi recharge help info.
 *
 * PARAM[in] payload: the information of dumi recharge help, please reference the DCS document.
 *
 * @RETURN: DUER_OK if success,
 *          DUER_MSG_RSP_BAD_REQUEST if the payload is invalid,
 *          DUER_ERR_FAILED if other error happened.
 */

duer_status_t duer_dcs_render_dumi_recharge_help_hdl(baidu_json *payload);


void duer_dcs_botapp_sdk_init(void);
/**
 * DESC:
 * Developer needs to implement this interface to get buy status.
 *
 * PARAM[in] payload: the information of buy status, please reference the DCS document.
 *
 * @RETURN: DUER_OK if success,
 *          DUER_MSG_RSP_BAD_REQUEST if the payload is invalid,
 *          DUER_ERR_FAILED if other error happened.
 */

duer_status_t duer_dcs_botapp_notify_buy_status_handler(baidu_json *payload);

/**
 * DESC:
 * Developer needs to implement this interface to get intent.
 *
 * PARAM[in] payload: the information of handle intent, please reference the DCS document.
 *
 * @RETURN: DUER_OK if success,
 *          DUER_MSG_RSP_BAD_REQUEST if the payload is invalid,
 *          DUER_ERR_FAILED if other error happened.
 */

duer_status_t duer_dcs_botapp_handle_intent_handler(baidu_json *payload);

/**
 * DESC:
 * Developer needs to implement this interface to get intent.
 *
 * PARAM[in] payload: the information of handle intent, please reference the DCS document.
 *
 * @RETURN: DUER_OK if success,
 *          DUER_MSG_RSP_BAD_REQUEST if the payload is invalid,
 *          DUER_ERR_FAILED if other error happened.
 */

duer_status_t duer_dcs_botapp_open_handler(baidu_json *payload);

duer_status_t duer_dcs_render_resource_list_hdl(const baidu_json *directive);

/**
 * DESC:
 * Developer needs to implement this interface to get intent.
 *
 * PARAM[in] payload: the information of handle intent, please reference the DCS document.
 *
 * @RETURN: DUER_OK if success,
 *          DUER_MSG_RSP_BAD_REQUEST if the payload is invalid,
 *          DUER_ERR_FAILED if other error happened.
 */

duer_status_t duer_dcs_cldapp_render_video_hdl(const baidu_json *directive);

// Add callback function list
// Hemingway
// May,12 2023
typedef int (*duer_dcs_render_watch_pay_hdl_f)(baidu_json *payload);
typedef int (*duer_dcs_render_watch_auth_hdl_f)(baidu_json *payload);
typedef int (*duer_dcs_require_login_hdl_f)(baidu_json *payload);
typedef baidu_json *(*duer_dcs_get_gps_state_f)();
typedef int (*duer_dcs_render_audio_list_hdl_f)(baidu_json *payload);
typedef int (*duer_dcs_render_player_info_hdl_f)(baidu_json *payload);
typedef int (*duer_dcs_render_weather_hdl_f)(baidu_json *payload);
typedef int (*duer_dcs_activity_notify_hdl_f)(baidu_json *payload);
typedef int (*duer_dcs_access_token_invalid_hdl_f)();

typedef duer_status_t (*duer_dcs_tone_alert_set_handler_f)(const baidu_json *directive);
typedef int (*duer_dcs_audio_play_handler_f)(const duer_dcs_audio_info_t *audio_info);
typedef duer_status_t (*duer_cloudapp_status_cb_hdl_f)(baidu_json *payload);
typedef void (*duer_dcs_speak_handler_f)(const char *url);
typedef duer_status_t (*duer_dcs_sync_skill_time_hdl_f)(baidu_json *payload);
typedef void (*duer_dcs_volume_set_handler_f)(int volume, duer_dcs_volume_target_t target);
typedef void (*duer_dcs_tts_flow_handler_f)(const char *data, size_t size);
typedef void (*duer_dcs_stop_speak_handler_f)(void);
typedef void (*duer_dcs_audio_stop_handler_f)(void);
typedef void (*duer_dcs_audio_resume_handler_f)(const duer_dcs_audio_info_t *audio_info);
typedef void (*duer_dcs_audio_pause_handler_f)(void);
typedef int (*duer_dcs_audio_get_play_progress_f)(void);
typedef baidu_json *(*duer_dcs_get_speaker_state_f)(void);
typedef void (*duer_dcs_volume_adjust_handler_f)(int volume, duer_dcs_volume_target_t target);
typedef void (*duer_dcs_mute_handler_f)(duer_bool is_mute, duer_dcs_volume_target_t target);
typedef void (*duer_dcs_listen_handler_f)(void);
typedef void (*duer_dcs_stop_listen_handler_f)(void);
typedef void (*duer_dcs_alert_delete_handler_f)(const char *token);
typedef duer_status_t (*duer_dcs_input_text_handler_f)(const char *text, const char *type);
typedef duer_status_t (*duer_dcs_render_card_handler_f)(baidu_json *payload);
typedef void (*duer_dcs_audio_download_handler_f)(const duer_dcs_audio_info_t *audio_info);
typedef duer_status_t (*duer_share_bind_cb_hdl_f)(baidu_json *payload);
typedef duer_status_t (*duer_vip_status_cb_hdl_f)(baidu_json *payload);
typedef duer_status_t (*duer_dcs_watch_vip_products_hdl_f)(baidu_json *payload);
typedef duer_status_t (*duer_dcs_watch_skill_list_hdl_f)(baidu_json *payload);
typedef duer_status_t (*duer_dcs_sync_skill_cfg_hdl_f)(baidu_json *payload);
typedef duer_status_t (*duer_dcs_watch_disable_skill_hdl_f)(baidu_json *payload);
typedef duer_status_t (*duer_dcs_render_ask_for_confirm_hdl_f)(baidu_json *payload);
typedef duer_status_t (*duer_dcs_render_dumi_recharge_hdl_f)(baidu_json *payload);
typedef duer_status_t (*duer_dcs_render_dumi_recharge_help_hdl_f)(baidu_json *payload);
typedef duer_status_t (*duer_dcs_botapp_notify_buy_status_handler_f)(baidu_json *payload);
typedef duer_status_t (*duer_dcs_botapp_handle_intent_handler_f)(baidu_json *payload);
typedef duer_status_t (*duer_dcs_botapp_open_handler_f)(baidu_json *payload);
typedef duer_status_t (*duer_dcs_cldapp_render_video_hdl_f)(const baidu_json *directive);

int lightduer_dcs_callback_render_watch_pay_hdl(baidu_json *payload);
int lightduer_dcs_callback_render_watch_auth_hdl(baidu_json *payload);
int lightduer_dcs_callback_require_login_hdl(baidu_json *payload);
baidu_json* lightduer_dcs_callback_get_gps_state();
int lightduer_dcs_callback_render_audio_list_hdl(baidu_json *payload);
int lightduer_dcs_callback_render_player_info_hdl(baidu_json *payload);
int lightduer_dcs_callback_render_weather_hdl(baidu_json *payload);
int lightduer_dcs_callback_activity_notify_hdl(baidu_json *payload);
int lightduer_dcs_callback_access_token_invalid_hdl(void);

signed char lightduer_dcs_callback_init(duer_dcs_render_watch_pay_hdl_f f_render_watch_pay,
                duer_dcs_render_watch_auth_hdl_f f_render_watch_auth,
                duer_dcs_require_login_hdl_f f_require_login,
                duer_dcs_get_gps_state_f             f_get_gps_state,
                duer_dcs_render_audio_list_hdl_f     f_render_audio_list,
                duer_dcs_render_player_info_hdl_f    f_render_player_info,
                duer_dcs_render_weather_hdl_f        f_render_weather,
                duer_dcs_activity_notify_hdl_f       f_activity_notify,
                duer_dcs_access_token_invalid_hdl_f  f_access_token_invalid);
signed char lightduer_dcs_callback_deinit(void);

duer_status_t lightduer_dcs_callback_tone_alert_set_handler(const baidu_json *directive);
int lightduer_dcs_callback_audio_play_handler(const duer_dcs_audio_info_t *audio_info);
duer_status_t lightduer_dcs_callback_cloudapp_status_cb_hdl(baidu_json *payload);
void lightduer_dcs_callback_speak_handler(const char *url);
duer_status_t lightduer_dcs_callback_sync_skill_time_hdl(baidu_json *payload);
void lightduer_dcs_callback_volume_set_handler(int volume, duer_dcs_volume_target_t target);
void lightduer_dcs_callback_tts_flow_handler(const char *data, size_t size);
void lightduer_dcs_callback_stop_speak_handler(void);
void lightduer_dcs_callback_audio_stop_handler(void);
void lightduer_dcs_callback_audio_resume_handler(const duer_dcs_audio_info_t *audio_info);
void lightduer_dcs_callback_audio_pause_handler(void);
int lightduer_dcs_callback_audio_get_play_progress(void);
baidu_json* lightduer_dcs_callback_get_speaker_state(void);
void lightduer_dcs_callback_volume_adjust_handler(int volume, duer_dcs_volume_target_t target);
void lightduer_dcs_callback_mute_handler(duer_bool is_mute, duer_dcs_volume_target_t target);
void lightduer_dcs_callback_listen_handler(void);
void lightduer_dcs_callback_stop_listen_handler(void);
void lightduer_dcs_callback_alert_delete_handler(const char *token);
duer_status_t lightduer_dcs_callback_input_text_handler(const char *text, const char *type);
duer_status_t lightduer_dcs_callback_render_card_handler(baidu_json *payload);
void lightduer_dcs_callback_audio_download_handler(const duer_dcs_audio_info_t *audio_info);
duer_status_t lightduer_dcs_callback_share_bind_cb_hdl(baidu_json *payload);
duer_status_t lightduer_dcs_callback_vip_status_cb_hdl(baidu_json *payload);
duer_status_t lightduer_dcs_callback_watch_vip_products_hdl(baidu_json *payload);
duer_status_t lightduer_dcs_callback_watch_skill_list_hdl(baidu_json *payload);
duer_status_t lightduer_dcs_callback_sync_skill_cfg_hdl(baidu_json *payload);
duer_status_t lightduer_dcs_callback_render_ask_for_confirm_hdl(baidu_json *payload);
duer_status_t lightduer_dcs_callback_render_dumi_recharge_hdl(baidu_json *payload);
duer_status_t lightduer_dcs_callback_render_dumi_recharge_help_hdl(baidu_json *payload);
duer_status_t lightduer_dcs_callback_botapp_notify_buy_status_handler(baidu_json *payload);
duer_status_t lightduer_dcs_callback_botapp_handle_intent_handler(baidu_json *payload);
duer_status_t lightduer_dcs_callback_botapp_open_handler(baidu_json *payload);
duer_status_t lightduer_dcs_callback_cldapp_render_video_hdl(const baidu_json *directive);

signed char lightduer_dcs_main_callback_init(duer_dcs_tone_alert_set_handler_f    f_tone_alert_set,
                duer_dcs_audio_play_handler_f        f_audio_play,
                duer_cloudapp_status_cb_hdl_f        f_cloudapp_status_cb,
                duer_dcs_speak_handler_f             f_speak,
                duer_dcs_sync_skill_time_hdl_f       f_sync_skill_time,
                duer_dcs_volume_set_handler_f        f_volume_set,
                duer_dcs_tts_flow_handler_f          f_tts_flow,
                duer_dcs_stop_speak_handler_f        f_stop_speak,
                duer_dcs_audio_stop_handler_f        f_audio_stop,
                duer_dcs_audio_resume_handler_f      f_audio_resume,
                duer_dcs_audio_pause_handler_f       f_audio_pause,
                duer_dcs_audio_get_play_progress_f   f_audio_get_play_progress,
                duer_dcs_get_speaker_state_f         f_get_speaker_state,
                duer_dcs_volume_adjust_handler_f     f_volume_adjust,
                duer_dcs_mute_handler_f              f_mute,
                duer_dcs_listen_handler_f            f_listen,
                duer_dcs_stop_listen_handler_f       f_stop_listen,
                duer_dcs_alert_delete_handler_f      f_alert_delete,
                duer_dcs_input_text_handler_f        f_input_text,
                duer_dcs_render_card_handler_f       f_render_card,
                duer_dcs_audio_download_handler_f    f_audio_download,
                duer_share_bind_cb_hdl_f             f_share_bind_cb,
                duer_vip_status_cb_hdl_f             f_vip_status_cb,
                duer_dcs_watch_vip_products_hdl_f    f_watch_vip_products,
                duer_dcs_watch_skill_list_hdl_f      f_watch_skill_list,
                duer_dcs_sync_skill_cfg_hdl_f             f_sync_skill_cfg,
                duer_dcs_watch_disable_skill_hdl_f        f_watch_disable_skill,
                duer_dcs_render_ask_for_confirm_hdl_f     f_render_ask_for_confirm,
                duer_dcs_render_dumi_recharge_hdl_f           f_render_dumi_recharge,
                duer_dcs_render_dumi_recharge_help_hdl_f      f_render_dumi_recharge_help,
                duer_dcs_botapp_notify_buy_status_handler_f   f_botapp_notify_buy_status,
                duer_dcs_botapp_handle_intent_handler_f       f_botapp_handle_intent,
                duer_dcs_botapp_open_handler_f                f_botapp_open,
                duer_dcs_cldapp_render_video_hdl_f            f_cldapp_render_video);
signed char lightduer_dcs_main_callback_deinit(void);
/**
 * DESC:
 * Developer needs to implement this interface to use the stream chat ticket.
 *
 * PARAM[in] payload: the information of handle intent, please reference the DCS document.
 *
 * @RETURN: DUER_OK if success,
 *          DUER_MSG_RSP_BAD_REQUEST if the payload is invalid,
 *          DUER_ERR_FAILED if other error happened.
 */
duer_status_t duer_dcs_render_stream_chat_ticket_hdl(const baidu_json *directive);
#ifdef __cplusplus
}
#endif

#endif/*BAIDU_DUER_LIGHTDUER_DCS_H*/

