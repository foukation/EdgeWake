// Copyright 2017 Baidu Inc. All Rights Reserved.
// Author: Chang Li (changli@baidu.com)
//
// Description: Implementation of recorder

#include "duerapp_recorder.h"
#include "lightduer_log.h"
#include "lightduer_dcs.h"
#include "lightduer_asr.h"
#include "lightduer_timestamp.h"

void duer_record_start(bool is_positive)
{
    duer_asrparam_t param;
    baidu_json *event;

    DUER_LOGI("duer_record_start");

    duer_ext_stop_speak();
    event = duer_dcs_listen_start_prepare();

    memset(&param, 0, sizeof(param));
    if (!is_positive) {
        param.wakeup_status = 200;
    }

#ifdef DUER_CES_ENABLE
    duer_ces_dpvs_set_request_type(CES_REQUEST_TYPE_VOICE);
    duer_ces_dpvs_set_asr_type(CES_ASR_TYPE_AUTO_LISTEN);
#endif
    DUER_LOGI("wakeup_backtime=%d.", param.wakeup_status);
    //duer_ds_e2e_event(DUER_E2E_1ST_WAKEUP);
    duer_dcs_listen_start(&param, event);
}

