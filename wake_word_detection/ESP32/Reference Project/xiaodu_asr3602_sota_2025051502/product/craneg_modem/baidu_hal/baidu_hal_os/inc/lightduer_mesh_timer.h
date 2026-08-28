// Copyright (2019) Baidu Inc. All rights reserveed.
/**
 * File: lightduer_mesh_timer.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc: Duer mesh timer functions.
 */
#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_TIMER_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_TIMER_H
#include "lightduer_timers.h"
/*
 * duer mesh timer init
 *
 * @param none
 *
 * @return none
 */
void init_mesh_timer();
/*
 * duer mesh timer start
 *
 * @param expire_time : int
 *        callback : duer_timer_callback
 *        param : void *
 *
 * @return duer_status_t
 *         SUCCESS : DUER_OK
 *         OTHER :
 */
duer_status_t start_mesh_timer(int expire_time, duer_timer_callback callback, void *param);

/*
 * duer mesh timer stop
 *
 * @param none
 *
 * @return none:
 */
void stop_mesh_timer();
#endif /* BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_TIMER_H */