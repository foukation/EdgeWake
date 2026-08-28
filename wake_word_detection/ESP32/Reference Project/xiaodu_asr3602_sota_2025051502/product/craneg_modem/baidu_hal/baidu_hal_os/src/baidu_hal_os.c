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
// Author: Gang Chen (chengang12@baidu.com)
//
// Description: Adapter for os APIs

#include "baidu_hal_os.h"
#include "osa_old_api.h"
#include "utilities.h"
#include <stdarg.h>
#include <stdint.h>
#include "pmic_rtc.h"
#include "diag_api.h"

/*
#define CP_PRINT
#ifdef CP_PRINT
#define DUER_LOG_ADP DIAG_FILTER(Diag, DebugScript, xiaodu, DIAG_INFORMATION) diagPrintf
#else
#define DUER_LOG_ADP uart_printf
#endif
*/

void (*duer_print)(const char *fmt, ...) = uart_printf;

void duer_register_print(void (*print_fun)(const char *fmt, ...))
{
    if (print_fun){
        duer_print = print_fun;
    }
    else{
        
    }
}

void duer_sleep_adpt(unsigned long time_ms)
{
     unsigned long tick_count = time_ms / OSAGetClockRate();
	 if (tick_count == 0) {
	     tick_count = 1;
	 }

     OsaTaskSleep(tick_count, NULL);
}

int duer_random_adpt(void)
{
    static int is_first_time = 1;

    if (is_first_time) {
        srand(OSAGetTicks());
        is_first_time = 0;
    }

    return rand();
}

void duer_print_dapt(const char* fmt, ...)
{
    char buf[128];
    va_list arg;
    va_start(arg, fmt);
    vsnprintf(buf, sizeof(buf), fmt, arg);
    va_end(arg);
    duer_print("%s", buf);
}

void *duer_mutex_create_adpt(void)
{
    OSA_STATUS status = OS_SUCCESS;
    OSMutexRef ret = NULL;

    status = OSAMutexCreate(&ret, OSA_PRIORITY);
    if (status != OS_SUCCESS) {
        duer_print("Failed to create mutex: %d", status);
        return NULL;
    }

    return ret;
}

int duer_mutex_delete_adpt(void *mutex)
{
    return OSAMutexDelete((OSMutexRef)mutex);
}

int duer_mutex_lock_adpt(void *mutex)
{
    return OSAMutexLock((OSMutexRef)mutex, OSA_SUSPEND);
}

int duer_mutex_try_lock_adpt(void *mutex, unsigned int timeout)
{
    return OSAMutexLock((OSMutexRef)mutex, timeout);
}

int duer_mutex_unlock_adpt(void *mutex)
{
    return OSAMutexUnlock((OSMutexRef)mutex);
}

void *duer_semaphore_create_adpt(int count)
{
    OSA_STATUS status = OS_SUCCESS;
    OSASemaRef sem = NULL;

    status = OSASemaphoreCreate(&sem, count, OSA_PRIORITY);
    if (status != OS_SUCCESS) {
        duer_print("Failed to create Semaphore: %d", status);
        return NULL;
    }

    return sem;
}

int duer_semaphore_destroy_adpt(void *sem)
{
    return OSASemaphoreDelete(sem);
}

int duer_semaphore_wait_adpt(void *sem, unsigned long wait_time_ms)
{
    return OSASemaphoreAcquire(sem, wait_time_ms / OSAGetClockRate());
}

int duer_semaphore_release_adpt(void *sem)
{
    return OSASemaphoreRelease(sem);
}

extern uint32_t lv_tick_get(void);
unsigned long duer_timestamp_adpt()
{
    // return OSAGetTicks() / OSAGetClockRate();
    return lv_tick_get();
}

void *duer_timer_acquire_adpt()
{
    OsaRefT ret = NULL;
    OSA_STATUS status = OS_SUCCESS;

    status = OSATimerCreate(&ret);
    if (status != OS_SUCCESS) {
        duer_print("Failed to create timer: %d", status);
        return NULL;
    }

    return ret;
}

int duer_timer_start_adpt(void *timer, int is_period, unsigned long time_ms,
                          void (*cb)(unsigned long), unsigned long param)
{
    unsigned long ticks = time_ms / OSAGetClockRate();
    if (ticks == 0) {
        ticks = 1;
    }

    return OSATimerStart(timer, ticks, is_period ? ticks : 0xFFFFFFFF, cb, param);
}

int duer_timer_stop_adpt(void *timer)
{
    return OSATimerStop(timer);
}

int duer_timer_release_adpt(void *timer)
{
    return OSATimerDelete(timer);
}

unsigned int duer_get_thread_id_adpt(void)
{
    OSA_STATUS status = OS_SUCCESS;
    OSATaskRef ret = NULL;

    status = OSATaskGetCurrentRef(&ret);
    if (status != OS_SUCCESS) {
        duer_print("Failed to get thread id: %d", status);
        return 0;
    }

    return (unsigned int)ret;
}

void *duer_thread_create_adpt(char *name, unsigned char priority, void *stack_ptr,
                              unsigned long stask_size, void (*task_start)(void*), void *argv)
{
    OSA_STATUS status = OS_SUCCESS;
    OSATaskRef ret = NULL;

    status = OSATaskCreate(&ret, stack_ptr, stask_size, priority, name, task_start, argv);
    if (status != OS_SUCCESS) {
        duer_print("Failed to create task: %d", status);
        return NULL;
    }

    return ret;
}

int duer_thread_suspend_adpt(void *thread_ref)
{
    return OSATaskSuspend(thread_ref);
}

int duer_thread_start_adpt(void *thread_ref)
{
    return OSATaskResume(thread_ref);
}

int duer_thread_terminate_adpt(void *thread_ref)
{
    return OSATaskTerminate(thread_ref);
}

int duer_thread_delete_adpt(void *thread_ref)
{
    return OSATaskDelete(thread_ref);
}

void *duer_msg_queue_create_adpt(char *name, size_t size, int len)
{
    OSMsgQRef ret = NULL;
    OSA_STATUS status = OS_SUCCESS;

    status = OSAMsgQCreate(&ret,
#if defined (OSA_QUEUE_NAMES)
                           name,
#endif
                           size,
                           len,
                           OSA_NO_SUSPEND
	);
    if (status != OS_SUCCESS) {
        duer_print("Failed to create timer: %d", status);
        return NULL;
    }

    return ret;
}

int duer_msg_queue_delete_adpt(void *queue)
{
    return OSAMsgQDelete(queue);
}

int duer_msg_queue_send_adpt(void *queue, void *msg, size_t size)
{
    return OSAMsgQSend(queue, size, msg, OSA_NO_SUSPEND);
}

int duer_msg_queue_recv_adpt(void *queue, void *data, size_t size)
{
    return OSAMsgQRecv(queue, data, size, OSA_SUSPEND);
}

int duer_msg_queue_recv_no_suspend_adpt(void *queue, void *data, size_t size)
{
    return OSAMsgQRecv(queue, data, size, OSA_NO_SUSPEND);
}

int duer_msg_queue_recv_timeout_adpt(void *queue, void *data, size_t size, unsigned long timeout_ms)
{
    return OSAMsgQRecv(queue, data, size, timeout_ms/OSAGetClockRate());
}



unsigned int duer_rtc_timestamp_adpt()
{
#if 0
    t_rtc real_time;

    PMIC_RTC_GetTime(&real_time, APP_OFFSET);
    return (unsigned int)pmic_rtc_mktime(&real_time);
#endif
    return time(NULL);
}

