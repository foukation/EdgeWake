/*
 * just for test
 *
 */

#ifndef BAIDU_HAL_OS_H
#define BAIDU_HAL_OS_H

#include "stdio.h"

void duer_sleep_adpt(unsigned long time_ms);

int duer_random_adpt(void);

void duer_print_dapt(const char* fmt, ...);

void *duer_mutex_create_adpt(void);

int duer_mutex_delete_adpt(void *mutex);

int duer_mutex_lock_adpt(void *mutex);

int duer_mutex_unlock_adpt(void *mutex);

int duer_mutex_try_lock_adpt(void *mutex, unsigned int timeout);

void *duer_semaphore_create_adpt(int count);

int duer_semaphore_destroy_adpt(void *sem);

int duer_semaphore_wait_adpt(void *sem, unsigned long wait_time_ms);

int duer_semaphore_release_adpt(void *sem);

unsigned long duer_timestamp_adpt();

void *duer_timer_acquire_adpt();

int duer_timer_start_adpt(void *timer, int is_period, unsigned long time_ms,
                          void (*cb)(unsigned long), unsigned long param);

int duer_timer_stop_adpt(void *timer);

int duer_timer_release_adpt(void *timer);

unsigned int duer_get_thread_id_adpt(void);

void *duer_thread_create_adpt(char *name, unsigned char priority, void *stack_ptr,
                              unsigned long stask_size, void (*task_start)(void*), void *argv);


int duer_thread_start_adpt(void *thread_ref);

int duer_thread_terminate_adpt(void *thread_ref);

int duer_thread_delete_adpt(void *thread_ref);

int duer_thread_suspend_adpt(void *thread_ref);

void *duer_msg_queue_create_adpt(char *name, size_t size, int len);

int duer_msg_queue_delete_adpt(void *queue);

int duer_msg_queue_send_adpt(void *queue, void *msg, size_t size);

int duer_msg_queue_recv_adpt(void *queue, void *data, size_t size);

int duer_msg_queue_recv_no_suspend_adpt(void *queue, void *data, size_t size);

int duer_msg_queue_recv_timeout_adpt(void *queue, void *data, size_t size, unsigned long timeout_ms);



unsigned int duer_rtc_timestamp_adpt();

#endif   /* BAIDU_HAL_OS_H */
