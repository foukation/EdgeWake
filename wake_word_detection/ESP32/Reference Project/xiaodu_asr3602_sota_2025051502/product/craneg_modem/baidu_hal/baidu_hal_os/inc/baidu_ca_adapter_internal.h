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
 * File: baidu_ca_adapter_internal.h
 * Auth: Gang Chen (chengang12@baidu.com)
 * Desc: Adapt the IoT CA to different platform.
 */

#ifndef BAIDU_DUER_IOT_CA_ADAPTER_BAIDU_CA_ADAPTER_INTERNAL_H
#define BAIDU_DUER_IOT_CA_ADAPTER_BAIDU_CA_ADAPTER_INTERNAL_H

#include "lightduer_types.h"
#include "lightduer_lib.h"
#include "lightduer_log.h"
#include "lightduer_net_transport.h"
#include "lightduer_mutex.h"
#include "lightduer_semaphore.h"
#include "lightduer_thread.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Memory adapter
 */

extern void *bcamem_alloc(duer_context ctx, duer_size_t size);

extern void *bcamem_realloc(duer_context ctx, void *ptr, duer_size_t size);

extern void bcamem_free(duer_context ctx, void *ptr);

/*
 * Socket adapter
 */

extern void bcasoc_initialize(void);

extern duer_socket_t bcasoc_create(duer_transevt_func context);

extern duer_status_t bcasoc_connect(duer_socket_t ctx, const duer_addr_t *addr);

extern duer_status_t bcasoc_connect_timeout(
    duer_socket_t ctx, const duer_addr_t *addr, duer_u32_t timeout);

extern duer_status_t bcasoc_send(
    duer_socket_t ctx, const void *data, duer_size_t size, const duer_addr_t *addr);

extern duer_status_t bcasoc_recv(duer_socket_t ctx, void *data, duer_size_t size, duer_addr_t *addr);

extern duer_status_t bcasoc_recv_timeout(
    duer_socket_t ctx, void *data, duer_size_t size, duer_u32_t timeout, duer_addr_t *addr);

extern duer_status_t bcasoc_close(duer_socket_t ctx);

extern duer_status_t bcasoc_destroy(duer_socket_t ctx);

extern void bcasoc_finalize(void);

/*
 * Debug adapter
 */

extern void bcadbg(duer_context ctx, duer_u32_t level, const char *file, duer_u32_t line, const char *msg);

/*
 * Mutex adapter
 */

extern duer_mutex_t create_mutex(void);

extern duer_status_t lock_mutex(duer_mutex_t mutex);

extern duer_status_t unlock_mutex(duer_mutex_t mutex);

extern duer_status_t delete_mutex_lock(duer_mutex_t mutex);

extern void baidu_fs_adapter_initialize(void);

/*
 * Semaphore adapter
 */
extern duer_semaphore_t semaphore_create(int count);

extern duer_status_t semaphore_wait(duer_semaphore_t semaphore, duer_u32_t millisec);

extern duer_status_t semaphore_release(duer_semaphore_t semaphore);

extern duer_status_t semaphore_destroy(duer_semaphore_t semaphore);

/*
 * Thread adapter
 */

extern duer_thread_t _thread_create(const char *thread_name, const char *queue_name, duer_u32_t stack_size,
                                    duer_u32_t queue_num, duer_u8_t priority);

extern duer_status_t _thread_start(duer_thread_t thread, duer_thread_entry_f_t entry_func,
                                   void *args);

extern duer_status_t _thread_exit(duer_thread_entry_args_t *entry_args, duer_bool is_released);

extern duer_status_t _thread_destroy(duer_thread_t thread);

extern duer_status_t _thread_terminate(duer_thread_t thread);

duer_status_t _thread_adp_init(void);

extern duer_u32_t _thread_get_id(duer_thread_t thread);

extern duer_status_t _thread_get_stack_info(duer_thread_t thread);

/*
 * AP information adapter
 */
extern duer_status_t wifi_status_get(void);

extern duer_s8_t wifi_rssi_get(void);

/*
 * Time API
 */
duer_u32_t duer_timestamp_obtain();

duer_u32_t duer_real_timestamp_obtain();

#ifdef __cplusplus
}
#endif

#endif/*BAIDU_DUER_IOT_CA_ADAPTER_BAIDU_CA_ADAPTER_INTERNAL_H*/
