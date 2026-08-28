// Copyright (2019) Baidu Inc. All rights reserveed.
/**
 * File: lightduer_bd_timer.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc: Duer broadlink timer functions.
 */
#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BD_TIMER_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BD_TIMER_H
#include "lightduer_timers.h"
#include "lightduer_mutex.h"

typedef struct bd_timer_s bd_timer_t;

struct bd_timer_s {
    /// timer handler
    duer_timer_handler handler;
    /// duer lock
    duer_mutex_t lock;
    /// is active
    duer_bool isActive;
    /// init the broadlink timer
    int (*init)(bd_timer_t *timer_p);
    /// start broadlink timer
    int (*start)(bd_timer_t *timer_p, int expire_time, duer_timer_callback callback, void *param);
    /// stop broadlink timer
    void (*stop)(bd_timer_t *timer_p);
};

/*
 * Register broadlink timer ops
 *
 * @param ops:
 *
 * @return duer_status_t: Success: DUER_OK
 *              Failed:  Other
 */
duer_status_t duer_register_bd_timer_ops(bd_timer_t *timer_p);
#endif /* BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BD_TIMER_H */