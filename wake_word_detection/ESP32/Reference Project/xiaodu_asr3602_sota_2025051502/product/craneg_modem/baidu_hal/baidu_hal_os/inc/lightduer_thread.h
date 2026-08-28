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
 * File: lightduer_thread.h
 * Auth: Chang Li (changli@baidu.com)
 * Desc: Provide the thread APIs.
 */

#ifndef LIBDUER_DEVICE_FRAMEWORK_CORE_LIGHTDUER_THREAD_H
#define LIBDUER_DEVICE_FRAMEWORK_CORE_LIGHTDUER_THREAD_H

#include "lightduer_types.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef void * duer_thread_t;

typedef struct {
    duer_thread_t p_thread;
    void *args;
} duer_thread_entry_args_t;

typedef void (*duer_thread_entry_f_t)(duer_u32_t argc, duer_thread_entry_args_t *);

/*
 * @Desc Create the specific thread object.
 *
 * @Param thread name.
 * @Param queue name.
 * @Param thread stack size in bytes.
 * @Param queue number.
 * @Param thread priority.
 *
 * @Ret thread object.
 */
duer_thread_t duer_thread_create(const char *thread_name, const char *queue_name, duer_u32_t stack_size,
        duer_u32_t queue_num, duer_u8_t priority);

/*
 * @Desc Start the specific thread.
 *
 * @Param duer_thread_t object.
 * @Param thread entry function.
 * @Param arguments passed to the entry function.
 *
 * @Ret Success: DUER_OK, Failure: DUER_ERR_FAILED.
 */
duer_status_t duer_thread_start(duer_thread_t thread, duer_thread_entry_f_t entry_func, void *args);

/*
 * @Desc Thread exit function. Note: Currently it only be called by the thread its self!!!
 *
 * @Param duer_thread_entry_args_t object.
 * @Param is_released=true, resources can be released automatically, without
 * calling duer_thread_destroy.
 *
 * @Ret Success: DUER_OK, Failure: DUER_ERR_FAILED.
 */
duer_status_t duer_thread_exit(duer_thread_entry_args_t *entry_args, duer_bool is_released);

/*
 * @Desc Destroy thread object and release resources.
 *
 * @Param duer_thread_t object.
 *
 * @Ret Success: DUER_OK, Failure: DUER_ERR_FAILED.
 */
duer_status_t duer_thread_destroy(duer_thread_t thread);
/*
  terminate thread
  Hemingway2003
  Jun,5 2023
*/
duer_status_t duer_thread_terminate(duer_thread_t thread);
/*
  destroy all thread
  Hemingway2003
  May,17 2023
*/
duer_status_t duer_thread_destroy_all(void);
/*
  List all thread for test
  Hemingway2003
  Jun,6 2023
*/
void duer_thread_destroy_list(void);
/*
 * @Desc Get the thread id.
 *
 * @Param duer_thread_t object.
 *
 * @Ret thread id.
 */
duer_u32_t duer_thread_get_id(duer_thread_t thread);

/*
 * @Desc Dump the stack usage of thread
 *
 * @Param thread, the thread object
 * @Return duer_status_t, the operation result
 */
duer_status_t duer_thread_get_stack_info(duer_thread_t thread);

/*
 * The thread callbacks
 */
typedef duer_thread_t (*duer_thread_create_f_t)(const char *, const char *, duer_u32_t, duer_u32_t, duer_u8_t);
typedef duer_status_t (*duer_thread_start_f_t)(duer_thread_t, duer_thread_entry_f_t, void *);
typedef duer_status_t (*duer_thread_exit_f_t)(duer_thread_entry_args_t *, duer_bool is_released);
typedef duer_status_t (*duer_thread_destroy_f_t)(duer_thread_t);
typedef duer_status_t (*duer_thread_terminate_f_t)(duer_thread_t);
typedef duer_u32_t (*duer_thread_get_id_f_t)(duer_thread_t);
typedef duer_status_t (*duer_thread_get_stack_info_f)(duer_thread_t thread);

/*
 * Initial the thread callbacks for lightduer
 *
 * @Param _f_create implementation.
 * @Param _f_start implementation.
 * @Param _f_exit implementation.
 * @Param _f_destroy implementation.
 * @Param _f_get_id implementation.
 */
DUER_EXT void duer_thread_init(
        duer_thread_create_f_t  _f_create,
        duer_thread_start_f_t   _f_start,
        duer_thread_exit_f_t    _f_exit,
        duer_thread_destroy_f_t _f_destroy,
        duer_thread_terminate_f_t _f_terminate,
        duer_thread_get_id_f_t  _f_get_id,
        duer_thread_get_stack_info_f f_get_stack_info);

#ifdef __cplusplus
}
#endif

#endif // LIBDUER_DEVICE_FRAMEWORK_CORE_LIGHTDUER_THREAD_H
