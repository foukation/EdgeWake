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
 * File: baidu_ca_socket_adp.c
 * Auth: Gang Chen (chengang12@baidu.com)
 * Desc: Adapt the socket function to ASR.
 */

#include "baidu_ca_adapter_internal.h"

#include "../../../pcac/lwipv4v6/src/include/lwip/err.h"
#include "../../../pcac/lwipv4v6/src/include/lwip/sockets.h"
#include "../../../pcac/lwipv4v6/src/include/lwip/sys.h"
#include "../../../pcac/lwipv4v6/src/include/lwip/netdb.h"
#include "../../../pcac/lwipv4v6/src/include/lwip/dns.h"

#include "lightduer_connagent.h"
#include "lightduer_auto_dns.h"
#include "lightduer_log.h"
#include "lightduer_lib.h"
#include "lightduer_memory.h"
#include "lightduer_events.h"
#include "lightduer_timers.h"
#include "lightduer_priority_conf.h"
#include "lightduer_timestamp.h"

#ifndef NON_BLOCKING
#define NON_BLOCKING    (0)
#endif

extern duer_handler my_handler;

#if defined(NON_BLOCKING) && (NON_BLOCKING == 1)
#define MSG_FLAG        (MSG_DONTWAIT)
#else
#define MSG_FLAG        (0)
#endif

#ifndef DUER_SENDTIMEOUT
#define DUER_SENDTIMEOUT    8000 // 8s
#endif

typedef struct _bcasoc_s
{
    volatile int fd;
    volatile int is_send_block;
    volatile int destroy;
    volatile int ref_count;
    duer_u32_t           send_data_block_begin_time; // unit is ms
    duer_timer_handler  _timer;
    duer_transevt_func  _callback;
} bcasoc_t;

static duer_mutex_t         g_mutex = NULL;
static duer_timer_handler   s_finalize_timer = NULL;
static volatile unsigned int socket_open_times = 0U, socket_close_times = 0U;

static void bcasoc_lock() {
    if (g_mutex) {
        duer_mutex_lock(g_mutex);
    }
}

static void bcasoc_unlock() {
    if (g_mutex) {
        duer_mutex_unlock(g_mutex);
    }
}

void bcasoc_initialize(void)
{
    if (g_mutex == NULL) {
        g_mutex = duer_mutex_create();
    }
}

duer_socket_t bcasoc_create(duer_transevt_func func)
{
    bcasoc_t *soc = DUER_MALLOC(sizeof(bcasoc_t));
    if (soc) {
        DUER_MEMSET(soc, 0, sizeof(bcasoc_t));
        soc->fd = -1;
        soc->is_send_block = 0;
        soc->destroy = 0;
        soc->ref_count = 0;
        soc->send_data_block_begin_time = 0;
        soc->_callback = func;
    }
    return soc;
}

static int set_sock_nonblock(int fd)
{
    long arg = 0;
    // Set non-blocking
    if ((arg = fcntl(fd, F_GETFL, NULL)) < 0) {
        DUER_LOGE("Error fcntl(..., F_GETFL) (%s)\n", strerror(errno));
        return DUER_ERR_FAILED;
    }
    arg |= O_NONBLOCK;
    if (fcntl(fd, F_SETFL, arg) < 0) {
        DUER_LOGE("Error fcntl(..., F_SETFL) (%s)\n", strerror(errno));
        return DUER_ERR_FAILED;
    }
    return DUER_OK;
}

static int set_sock_block(int fd)
{
    long arg = 0;
    // Set non-blocking
    if ((arg = fcntl(fd, F_GETFL, NULL)) < 0) {
        DUER_LOGE("Error fcntl(..., F_GETFL) (%s)\n", strerror(errno));
        return DUER_ERR_FAILED;
    }
    arg &= ~O_NONBLOCK;
    if (fcntl(fd, F_SETFL, arg) < 0) {
        DUER_LOGE("Error fcntl(..., F_SETFL) (%s)\n", strerror(errno));
        return DUER_ERR_FAILED;
    }
    return DUER_OK;
}

static void close_sock_fd(bcasoc_t *soc)
{
    bcasoc_lock();
    if (soc->fd != -1) {
        shutdown(soc->fd, SHUT_RDWR);
        close(soc->fd);
        DUER_LOGI("socket: %d close", soc->fd);
        DUER_LOGI("end socket open: %d,close : %d\r\n", socket_open_times, ++socket_close_times);
        soc->fd = -1;
    }
    bcasoc_unlock();
}

__attribute__((weak)) void bd_socket_callback(int s, int evt, u16_t len)
{

}

static duer_status_t bcasoc_connect_ip_timeout(duer_socket_t ctx, const duer_addr_t *addr, duer_u32_t ip,
        duer_u32_t timeout_ms)
{
    int rs = DUER_ERR_FAILED;
    bcasoc_t *soc = (bcasoc_t *)ctx;
    fd_set myset;
    struct timeval tv;
    int opt_val = 0;
    socklen_t opt_len = sizeof(opt_val);
    if (!soc || !addr) {
        return DUER_ERR_FAILED;
    }
    DUER_LOGI("start socket open: %d,close : %d\r\n", socket_open_times, socket_close_times);
    soc->fd = socket_with_callback(AF_INET, addr->type == DUER_PROTO_TCP ? SOCK_STREAM : SOCK_DGRAM,
                     addr->type == DUER_PROTO_TCP ? IPPROTO_TCP : IPPROTO_UDP, bd_socket_callback);
    if (soc->fd < 0) {
        DUER_LOGE("Error socket (%s)\n", strerror(errno));
        return DUER_ERR_FAILED;
    }
    DUER_LOGI("socket: %d open", soc->fd);
    socket_open_times++;
    // Set to nonblock.
    rs = set_sock_nonblock(soc->fd);
    if (rs != DUER_OK) {
        close_sock_fd(soc);
        return rs;
    }
    // Trying to connect.
    struct sockaddr_in addr_in;
    addr_in.sin_family = AF_INET;
    addr_in.sin_port = htons(addr->port);
    DUER_LOGV("Result bcasoc_connect: addr = %u", ip);
    addr_in.sin_addr.s_addr = ip;

    rs = connect(soc->fd, (struct sockaddr *)&addr_in, sizeof(addr_in));
    if (rs < 0) {
        if (errno != EINPROGRESS) {
            DUER_LOGE("Error connecting %d - %s", errno, strerror(errno));
            close_sock_fd(soc);
            return DUER_ERR_FAILED;
        }
        DUER_LOGI("EINPROGRESS in connect() - selecting");
        tv.tv_sec = timeout_ms / 1000;
        tv.tv_usec = (timeout_ms % 1000) * 1000;
        FD_ZERO(&myset);
        FD_SET(soc->fd, &myset);
        rs = select(soc->fd + 1, NULL, &myset, NULL, &tv);
        if (rs < 0 && errno != EINTR) {
            DUER_LOGE("Error connecting %d - %s", errno, strerror(errno));
            close_sock_fd(soc);
            return DUER_ERR_FAILED;
        } else if (rs > 0) {
            // Socket selected for write.
            if (getsockopt(soc->fd, SOL_SOCKET, SO_ERROR, &opt_val, &opt_len) < 0) {
                DUER_LOGE("Error in getsockopt() %d - %s", errno, strerror(errno));
                close_sock_fd(soc);
                return DUER_ERR_FAILED;
            }
            // Check the value returned.
            if (opt_val) {
                DUER_LOGE("Error in delayed connection() %d - %s", opt_val, strerror(opt_val));
                close_sock_fd(soc);
                return DUER_ERR_FAILED;
            }
        } else {
            DUER_LOGE("Connect timeout, select rs: %d", rs);
            close_sock_fd(soc);
            return DUER_ERR_FAILED;
        }
    }
#if !defined(NON_BLOCKING) || (NON_BLOCKING != 1)
    // Set to blocking mode again.
    rs = set_sock_block(soc->fd);
    if (rs != DUER_OK) {
        close_sock_fd(soc);
        return DUER_ERR_FAILED;
    }
#endif  // NON_BLOCKING

    return DUER_OK;
}

static duer_status_t bcasoc_connect_ip(duer_socket_t ctx, const duer_addr_t *addr, duer_u32_t ip)
{
    int rs = DUER_ERR_FAILED;
    bcasoc_t *soc = (bcasoc_t *)ctx;

    DUER_LOGV("Entry bcasoc_connect ctx = %p", ctx);
    {
        char ip_str[DUER_HTTP_DNS_IP_LEN_MAX] = {0};
        if (inet_ntoa_r(ip, ip_str, DUER_HTTP_DNS_IP_LEN_MAX) != NULL) {
            DUER_LOGI("Try to connect: Host=%s, IP=%s", addr->host, ip_str);
        } else {
            DUER_LOGW("inet_ntoa_r failed: Host=%s, IP=%u", addr->host, ip);
        }
    }
    DUER_LOGI("start socket open: %d,close : %d\r\n", socket_open_times, socket_close_times);
    if (soc && addr) {
        soc->fd = socket_with_callback(AF_INET, addr->type == DUER_PROTO_TCP ? SOCK_STREAM : SOCK_DGRAM,
                         addr->type == DUER_PROTO_TCP ? IPPROTO_TCP : IPPROTO_UDP, bd_socket_callback);
        DUER_LOGV("Result bcasoc_connect fd = %d", soc->fd);
        if (soc->fd < 0) {
            DUER_LOGE("socket: %d create failed", soc->fd);
            return soc->fd;
        }

        DUER_LOGI("socket: %d open", soc->fd);

        struct sockaddr_in addr_in;
        addr_in.sin_family = AF_INET;
        addr_in.sin_port = htons(addr->port);
        DUER_LOGV("Result bcasoc_connect: addr = %u", ip);
        addr_in.sin_addr.s_addr = ip;

        rs = connect(soc->fd, (struct sockaddr *)&addr_in, sizeof(addr_in));
        socket_open_times++;
        if (rs >= 0) {

#if defined(NON_BLOCKING) && (NON_BLOCKING == 1)
            int flags = fcntl(soc->fd, F_GETFL, 0);
            fcntl(soc->fd, F_SETFL, flags | O_NONBLOCK);
#endif

        } else {
            bcasoc_lock();
            if (soc->fd != -1) {
                shutdown(soc->fd, SHUT_RDWR);
                close(soc->fd);
                DUER_LOGI("socket: %d close", soc->fd);
                DUER_LOGI("end socket open: %d,close : %d\r\n", socket_open_times, ++socket_close_times);
                soc->fd = -1;
            }
            bcasoc_unlock();
        }
    }

    DUER_LOGV("Result bcasoc_connect rs = %d", rs);

    return rs;
}

duer_status_t bcasoc_connect(duer_socket_t ctx, const duer_addr_t *addr)
{
    return bcasoc_connect_timeout(ctx, addr, 0);
}

duer_status_t bcasoc_connect_timeout(duer_socket_t ctx, const duer_addr_t *addr, duer_u32_t timeout)
{
    duer_ip_list_t *ip_list = NULL;
    if (addr == NULL) {
        return DUER_ERR_INVALID_PARAMETER;
    }

    duer_u32_t ip = addr->ip;
    if (ip == 0) {
        int ret = duer_auto_dns_resolve_uint(addr->host, &ip);
        if (ret < DUER_OK) {
            DUER_LOGE("resolve ip failed: %d", ret);
            return ret;
        }
    }
    if (timeout == 0) {
        return bcasoc_connect_ip(ctx, addr, ip);
    } else {
        return bcasoc_connect_ip_timeout(ctx, addr, ip, timeout);
    }
}

duer_status_t bcasoc_send(duer_socket_t ctx, const void *data, duer_size_t size, const duer_addr_t *addr)
{
    bcasoc_t *soc = (bcasoc_t *)ctx;
    int rs = DUER_ERR_FAILED;
    fd_set fdw;
    struct timeval tv;

    if (!soc || soc->fd == -1) {
        return rs;
    }

    FD_ZERO(&fdw);
    FD_SET(soc->fd, &fdw);

    tv.tv_sec = 5;
    tv.tv_usec = 0;

    rs = select(soc->fd + 1, NULL, &fdw, NULL, &tv);
    if (FD_ISSET(soc->fd, &fdw)) {
        rs = send(soc->fd, data, size, MSG_FLAG);
        // DUER_LOGI("socket: %d send: %d", soc->fd, rs);
    } else if (rs == 0) {
        rs = DUER_ERR_TRANS_WOULD_BLOCK;
        soc->is_send_block = 1;
        soc->send_data_block_begin_time = duer_timestamp();
    } else if (errno != EINTR) {
        rs = DUER_ERR_TRANS_INTERNAL_ERROR;
    }

    if (rs < 0 && rs != DUER_ERR_TRANS_WOULD_BLOCK) {
        DUER_LOGE("write socket error %d:%s", errno, strerror(errno));
    }
    // DUER_LOGI("socket: %d send end: %d", soc->fd, rs);
    return rs > 0 ? size : rs;
}

duer_status_t bcasoc_recv(duer_socket_t ctx, void *data, duer_size_t size, duer_addr_t *addr)
{
    bcasoc_t *soc = (bcasoc_t *)ctx;
    int rs = DUER_ERR_FAILED;
    fd_set fdr;
    struct timeval tv;

    if (!soc || soc->fd == -1) {
        return rs;
    }

    FD_ZERO(&fdr);
    FD_SET(soc->fd, &fdr);

    tv.tv_sec = 3;
    tv.tv_usec = 0;

    rs = select(soc->fd + 1, &fdr, NULL, NULL, &tv);
    if (FD_ISSET(soc->fd, &fdr)) {
        rs = recv(soc->fd, data, size, MSG_FLAG);
        DUER_LOGI("socket: %d recv: %d", soc->fd, rs);
        if (rs <= 0) {
            DUER_LOGE("Can't reach remote host!!! rs = %d", rs);
            rs = DUER_ERR_TRANS_INTERNAL_ERROR;
        }
    } else if (rs == 0) {
        rs = DUER_ERR_TRANS_WOULD_BLOCK;
    } else if (errno != EINTR) {
        rs = DUER_ERR_TRANS_INTERNAL_ERROR;
    }

    if (rs > 0 ) {
        if (soc->fd != -1) {
            //DUER_LOGI("before call bcasoc_timer_expired, soc:%p, fd:%d", soc, soc->fd);
        }
    }

    if (rs < 0 && rs != DUER_ERR_TRANS_WOULD_BLOCK) {
        DUER_LOGE("read socket error %d:%s", errno, strerror(errno));
    }

    DUER_LOGI("socket: %d recv end: %d", soc->fd, rs);
    return rs;
}

duer_status_t bcasoc_recv_timeout(duer_socket_t ctx, void *data, duer_size_t size, duer_u32_t timeout, duer_addr_t *addr)
{
    DUER_LOGV("bcasoc_recv_timeout: ctx = %p, data = %p, size = %d, timeout = %d, addr = %p", ctx, data, size, timeout,
              addr);
    int rs = DUER_ERR_TRANS_INTERNAL_ERROR;
    bcasoc_t *soc = (bcasoc_t *)ctx;
    duer_u32_t ts = 0;
    if (soc && soc->fd != -1) {
        fd_set fdr;
        struct timeval time = {timeout / 1000, (timeout % 1000) * 1000};

        FD_ZERO(&fdr);
        FD_SET(soc->fd, &fdr);

        ts = duer_timestamp();
        rs = select(soc->fd + 1, &fdr, NULL, NULL, &time);
        DUER_LOGV("bcasoc_recv_timeout select: rs = %d", rs);
        if (FD_ISSET(soc->fd, &fdr)) {
            rs = recv(soc->fd, data, size, 0);
            // DUER_LOGI("socket: %d recv: %d", soc->fd, rs);
            if (rs < 0) {
                DUER_LOGE("Can't reach remote host!!! rs = %d", rs);
                rs = DUER_ERR_TRANS_INTERNAL_ERROR;
            } else {
                duer_u32_t elapsed = duer_timestamp() - ts;
                if (timeout > 0 && elapsed > timeout) {
                    DUER_LOGE("elapsed %u ms more than timeout %u, rs %d", elapsed, timeout, rs);
                }
            }
        } else if (rs == 0) {
            if (timeout == 0) {
                rs = DUER_ERR_TRANS_WOULD_BLOCK;
            } else {
                rs = DUER_ERR_TRANS_TIMEOUT;
                DUER_LOGW("recv timeout!!!");
            }
        } else {
            rs = DUER_ERR_TRANS_INTERNAL_ERROR;
            DUER_LOGW("recv failed: rs = %d", rs);
        }
    }
    // DUER_LOGI("socket: %d recv end: %d", soc->fd, rs);
    DUER_LOGV("bcasoc_recv_timeout: rs = %d, fd = %d", rs, soc ? soc->fd : -99);
    return rs;
}

duer_status_t bcasoc_close(duer_socket_t ctx)
{
    bcasoc_t *soc = (bcasoc_t *)ctx;
    if (soc) {
        if (soc->_timer) {
            duer_timer_stop(soc->_timer);
            DUER_LOGI("stop timer:%p", soc->_timer);
        }
        bcasoc_lock();
        if (soc->fd != -1) {
            shutdown(soc->fd, SHUT_RDWR);
            close(soc->fd);
            DUER_LOGI("socket: %d close", soc->fd);
            DUER_LOGI("end socket open: %d,close : %d\r\n", socket_open_times, ++socket_close_times);
            soc->fd = -1;
        }

        bcasoc_unlock();
    }
    return DUER_OK;
}

duer_status_t bcasoc_destroy(duer_socket_t ctx)
{
    bcasoc_t *soc = (bcasoc_t *)ctx;
    bcasoc_lock();
    soc->destroy = 1;
    DUER_LOGI("start last timer:%p, soc:%p", soc->_timer, soc);
    if (soc->_timer) {
        duer_timer_start(soc->_timer, 200);
    } else {
        DUER_FREE(soc);
    }
    bcasoc_unlock();
    return DUER_OK;
}

static void bcasoc_destroy_events(int what, void *object)
{
    DUER_LOGI("bcasoc_destroy_events");
    delete_mutex_lock(g_mutex);
    g_mutex = NULL;
    duer_timer_release(s_finalize_timer);
    s_finalize_timer = NULL;
}

static void bcasoc_finalize_callback(void *param)
{
    // todo: empty?
    //bcasoc_events_call(bcasoc_destroy_events, 0, NULL);
}

void bcasoc_finalize(void)
{
    s_finalize_timer = duer_timer_acquire(bcasoc_finalize_callback, NULL, DUER_TIMER_ONCE);
    if (s_finalize_timer) {
        duer_timer_start(s_finalize_timer, 200);
    }
}

