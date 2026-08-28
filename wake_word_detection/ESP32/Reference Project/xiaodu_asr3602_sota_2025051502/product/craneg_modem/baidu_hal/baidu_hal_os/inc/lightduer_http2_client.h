/**
 * Copyright (2019) Baidu Inc. All rights reserved.
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
 *
 * File: lightduer_http2_client.h
 * Author: Chen Chi (chenchi01@baidu.com)
 * Desc: http2 client. This is a simple wrapper around nghttp2.
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_CLIENT_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_CLIENT_H

#include "nghttp2/nghttp2.h"
#include "lightduer_lib.h"
#include "lightduer_net_transport.h"

/**
 * @brief Handle for working with http2 APIs
 */
typedef void *duer_http2_handle;

/**
 * @brief Function Prototype for data receive callback
 * The function may get called multiple times as long as data is received on the stream.
 *
 * @param[in] handle     http2 api handle.
 * @param[in] data       Pointer to a buffer that contains the data received.
 * @param[in] len        The length of valid data stored at the 'data' pointer.
 * @param[in] user_ctx   Pointer to registered custom data.
 */
typedef int (*duer_http2_data_recv_cb_t)(
        duer_http2_handle handle, const char *data, size_t len, void *user_ctx);
/**
 * @brief Function Prototype for header receive callback
 * The function will get called once for each key-value of a header.
 */
typedef int (*duer_http2_header_cb_t)(
        duer_http2_handle handle, const char *key, const char *value, void *user_ctx);
/**
 * @brief Function Prototype when a DATA frame is fully received.
 */
typedef int (*duer_http2_frame_complete_cb_t)(
        duer_http2_handle handle, void *user_ctx);
/**
 * @brief Function Prototype when a stream is about to be closed.
 */
typedef int (*duer_http2_stream_close_cb_t)(
        duer_http2_handle handle, void *user_ctx);

/**
 * @brief Function Prototype for callback to send data in PUT/POST
 *
 * This function gets called whenever nghttp2 wishes to send data, like for
 * PUT/POST requests to the server. The function keeps getting called until this
 * function sets the flag NGHTTP2_DATA_FLAG_EOF to indicate end of data.
 *
 * @param[in] handle       http2 api handle.
 * @param[out] data        Pointer to a buffer that should contain the data to send.
 * @param[in] len          The maximum length of data that can be sent out by this function.
 * @param[out] data_flags  Pointer to the data flags. The NGHTTP2_DATA_FLAG_EOF
 *                         should be set in the data flags to indicate end of new data.
 * @param[in] user_ctx     Pointer to registered custom data.
 *
 * @return The function should return the number of valid bytes stored in the
 * data pointer
 */
typedef int (*duer_http2_data_send_cb_t)(
        duer_http2_handle handle, char *data, size_t len, uint32_t *data_flags, void *user_ctx);

typedef struct {
    duer_http2_header_cb_t header_recv_cb;
    duer_http2_data_recv_cb_t data_recv_cb;
    duer_http2_frame_complete_cb_t frame_complete_cb;
    duer_http2_data_send_cb_t data_send_cb;
    duer_http2_stream_close_cb_t stream_close_cb;
    void *user_ctx;
} duer_http2_stream_cb_info_t;


/**
 * @brief Allocate and init duer_http2_handle.
 */
duer_http2_handle duer_http2_acquire();

/**
 * @brief Connect to a URI using HTTP/2
 *
 * This API opens an HTTP/2 connection with the provided URI. If successful, the
 * hd pointer is populated with a valid handle for subsequent communication.
 *
 * Only 'https' URIs are supported.
 *
 * @param[in/out] hd        Pointer to a variable of the type 'struct duer_http2_handle'.
 * @param[in]  uri          Pointer to the URI that should be connected to.
 * @param[in] timeout_ms    connect timeout in milliseconds.
 *
 * @return DUER_OK if the connection was successful; otherwise if failed.
 */
int duer_http2_connect(duer_http2_handle hd, const char *uri, duer_u32_t timeout_ms);

/**
 * @brief Invoke nghttp2_session_terminate_session to send GOAWAY frame and
 * gracefully terminate http2 session.
 */
int duer_http2_close_session(duer_http2_handle hd);

/**
 * @brief Free a duer_http2_handle
 */
void duer_http2_free(duer_http2_handle hd);

/**
 * @brief Get the remote hostname of current http2 connection.
 */
const char *duer_http2_get_hostname(duer_http2_handle hd);

/**
 * @brief Setup an HTTP GET request stream
 *
 * This API sets up an HTTP GET request to be sent out to the server. A new
 * stream is created for handling the request. Once the request is setup, the
 * API duer_http2_execute() must be called to actually perform the socket I/O with
 * the server.
 *
 * @param[in] hd        Pointer to a variable of the type 'struct duer_http2_handle'.
 * @param[in] path      Pointer to the string that contains the resource to
 *                      perform the HTTP GET operation on (for example, /users).
 * @param[in] cb        The registered callbacks. "cb" must be valid during the whole lifetime
 *                      of this http2 stream.
 *
 * @return DUER_OK if successful; otherwise failed.
 */
int duer_http2_do_get(duer_http2_handle hd, const char *path,
        duer_http2_stream_cb_info_t *cb);

/**
 * @brief Setup an HTTP POST request stream
 *
 * This API sets up an HTTP POST request to be sent out to the server. A new
 * stream is created for handling the request. Once the request is setup, the
 * API sh2lib_execute() must be called to actually perform the socket I/O with
 * the server.
 *
 * @param[in] hd        Pointer to a variable of the type 'struct duer_http2_handle'.
 * @param[in] path      Pointer to the string that contains the resource to
 *                      perform the HTTP POST operation on (for example, /users).
 * @param[in] cb        The registered callbacks. "cb" must be valid during the whole lifetime
 *                      of this http2 stream.
 *
 * @return DUER_OK if successful; otherwise failed.
 */
int duer_http2_do_post(duer_http2_handle hd, const char *path,
        duer_http2_stream_cb_info_t *cb);


/**
 * @brief Execute send/receive on an HTTP/2 connection
 *
 * While the API sh2lib_do_get(), sh2lib_do_post() setup the requests to be
 * initiated with the server, this API performs the actual data send/receive
 * operations on the HTTP/2 connection. The callback functions are accordingly
 * called during the processing of these requests.
 *
 * @param[in] hd      Pointer to a variable of the type 'struct duer_http2_handle'
 *
 * @return DUER_OK if successful; otherwise failed.
 */
int duer_http2_execute(duer_http2_handle hd);

#define DUER_HTTP2_MAKE_NV(NAME, VALUE)                                \
  {                                                                    \
    (uint8_t *)NAME, (uint8_t *)VALUE, strlen(NAME), strlen(VALUE),    \
        NGHTTP2_NV_FLAG_NONE                                           \
  }

#define DUER_HTTP2_MAKE_DEFAULT_NV(HANDLE, METHOD, PATH)                 \
      DUER_HTTP2_MAKE_NV(":method", METHOD),                             \
      DUER_HTTP2_MAKE_NV(":scheme", "https"),                            \
      DUER_HTTP2_MAKE_NV(":authority", duer_http2_get_hostname(HANDLE)), \
      DUER_HTTP2_MAKE_NV(":path", PATH)

#define DUER_HTTP2_MAKE_DEFAULT_NV_FOR_GET(HANDLE, PATH)                 \
      DUER_HTTP2_MAKE_DEFAULT_NV(HANDLE, "GET", PATH)

#define DUER_HTTP2_MAKE_DEFAULT_NV_FOR_POST(HANDLE, PATH)                \
      DUER_HTTP2_MAKE_DEFAULT_NV(HANDLE, "POST", PATH)

#define DUER_HTTP2_MAKE_DEFAULT_NV_FOR_PUT(HANDLE, PATH)                 \
      DUER_HTTP2_MAKE_DEFAULT_NV(HANDLE, "PUT", PATH)

int duer_http2_do_get_with_common_nv(duer_http2_handle hd, const char *path, const char *token,
        duer_http2_stream_cb_info_t *cb);

/**
 * @brief Setup an HTTP GET request stream with custom name-value pairs
 * Note that "nva" must contain entries generated by DUER_HTTP2_MAKE_DEFAULT_NV_FOR_GET.
 *
 * @param[in] hd        Pointer to a variable of the type 'struct duer_http2_handle'.
 * @param[in] nva       An array of name-value pairs that should be part of the request.
 * @param[in] nvlen     The number of elements in the array pointed to by 'nva'.
 * @param[in] cb        The registered callbacks. "cb" must be valid during the whole lifetime
 *                      of this http2 stream.
 *
 * @return DUER_OK if successful; otherwise failed.
 */
int duer_http2_do_get_with_nv(duer_http2_handle hd, const nghttp2_nv *nva, size_t nvlen,
        duer_http2_stream_cb_info_t *cb);

/**
 * @brief Setup an HTTP PUT/POST request stream with custom name-value pairs
 * Note that "nva" must contain entries generated by DUER_HTTP2_MAKE_DEFAULT_NV_FOR_POST
 * or DUER_HTTP2_MAKE_DEFAULT_NV_FOR_PUT.
 *
 * @param[in] hd        Pointer to a variable of the type 'struct duer_http2_handle'.
 * @param[in] nva       An array of name-value pairs that should be part of the request.
 * @param[in] nvlen     The number of elements in the array pointed to by 'nva'.
 * @param[in] cb        The registered callbacks. "cb" must be valid during the whole lifetime
 *                      of this http2 stream.
 *
 * @return DUER_OK if successful; otherwise failed.
 */
int duer_http2_do_putpost_with_nv(duer_http2_handle hd, const nghttp2_nv *nva, size_t nvlen,
        duer_http2_stream_cb_info_t *cb);

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_CLIENT_H
