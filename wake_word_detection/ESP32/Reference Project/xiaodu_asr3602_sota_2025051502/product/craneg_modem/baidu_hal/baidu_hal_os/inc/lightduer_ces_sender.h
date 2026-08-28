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
 */
/**
 * File: lightduer_ces_sender.h
 * Auth: Chen Xihao (chenxihao@baidu.com)
 * Desc: to send ces log
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CES_SENDER_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CES_SENDER_H

#include "lightduer_types.h"

enum duer_ces_importance_e {
    CES_NOMARL,         // if send failed, drop the msg
    CES_ABOVE_NOMARL,   // if send failed, save the msg to flash
    CES_IMPORTANT       // save the msg to flash first, then remove it after send successfully
};

int duer_ces_sender_init(void);

int duer_ces_sender_send(const char *msg, int importance);

int duer_ces_sender_uninit(void);

#endif /* BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CES_SENDER_H */
