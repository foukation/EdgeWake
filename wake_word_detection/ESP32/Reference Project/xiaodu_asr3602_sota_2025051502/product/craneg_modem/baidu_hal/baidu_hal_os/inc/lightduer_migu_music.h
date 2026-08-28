/**
 * Copyright (2020) Baidu Inc. All rights reserved.
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
 * File: lightduer_migu_music.h
 * Auth: Chen Xihao (chenxihao@baidu.com)
 * Desc: Get migu music info APIs.
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MIGU_MUSIC_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MIGU_MUSIC_H

#include <stdbool.h>
#include "baidu_json.h"
#include "lightduer_types.h"

#ifdef __cplusplus
extern "C" {
#endif

#define MIGU_MUSIC_URL_PREFIX "dueros://duer.music.com/migu/play"

enum duer_migu_err_code_e {
    DUER_MIGU_ERR_NONE,
    DUER_MIGU_ERR_FAILED,
    DUER_MIGU_ERR_RESOURCE_OFFLINE,
    DUER_MIGU_ERR_RESOURCE_SYSTEM_BUSY,
    DUER_MIGU_ERR_PARSE_TOKEN_FAILED
};

/**
 * DESC:
 * Get music url from migu server
 *
 * PARAM:
 * dueros_url, the url of dueros starts with MIGU_MUSIC_URL_PREFIX
 * url, output the url of music, should be freed by caller
 *
 * @RETURN: the error code defined in enum duer_migu_err_code_e
 */
int duer_migu_get_music_url(const char *dueros_url, char **url);

/**
 * DESC:
 * Notify to stop getting music url
 *
 * PARAM:none
 *
 * @RETURN: none
 */
void *duer_migu_stop_get_music_url(void);

/**
 * DESC:
 * Get the error message, if the return of duer_migu_get_music_url is not 0,
 * you can call this function to get the detail of error
 *
 * PARAM:none
 *
 * @RETURN: the error message
 */
const char *duer_migu_get_err_msg(void);

#ifdef __cplusplus
}
#endif

#endif /* BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MIGU_MUSIC_H */

