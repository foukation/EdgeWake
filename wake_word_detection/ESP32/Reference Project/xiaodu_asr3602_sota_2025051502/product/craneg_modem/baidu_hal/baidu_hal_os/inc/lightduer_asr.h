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
 * File: lightduer_connagent.h
 * Auth: Su Hao (suhao@baidu.com)
 * Desc: Light duer APIS.
 */

#ifndef BAIDU_DUER_LIGHTDUER_MODULE_CONNAGENT_BDSPEECH_LIGHTDUER_ASR_H
#define BAIDU_DUER_LIGHTDUER_MODULE_CONNAGENT_BDSPEECH_LIGHTDUER_ASR_H

#ifdef __cplusplus
extern "C" {
#endif

#include "baidu_json.h"

typedef struct _duer_asr_pam_s {
    int     wakeup_status;
    size_t  param_length;
    char    *param;
    int     print_voice;
} duer_asrparam_t;

typedef enum _duer_asr_mode_t {
    DUER_ASR_AUTO = 0,
    DUER_ASR_OPUS,
    DUER_ASR_FEATURE
} duer_asr_mode_t;

char *duer_asr_generate_pam(baidu_json *value);

void duer_asr_destroy_pam(char *pam);

duer_asrparam_t *duer_asr_param_create(int wakeup_status,
                                       char *pam,
                                       size_t pam_len,
                                       int recursive,
                                       int print_voice);

duer_asrparam_t *duer_asr_param_duplicate(const duer_asrparam_t *other);

void duer_asr_param_destroy(duer_asrparam_t *param);

int duer_asr_start(const duer_asrparam_t *param);

int duer_asr_stop(void);

int duer_asr_cancel(void);

int duer_wakeup_start(void);

int duer_wakeup_stop(void);

void duer_bds_boot(int launch_mode);

int duer_asr_set_mode(duer_asr_mode_t mode);

void duer_wakeup_restrain();

void duer_bds_link_start();

void duer_bds_link_stop();

#ifdef __cplusplus
}
#endif

#endif/*BAIDU_DUER_LIGHTDUER_MODULE_CONNAGENT_BDSPEECH_LIGHTDUER_ASR_H*/
