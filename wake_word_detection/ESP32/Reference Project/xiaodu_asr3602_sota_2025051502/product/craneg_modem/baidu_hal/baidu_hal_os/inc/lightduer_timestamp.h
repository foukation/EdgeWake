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
// Author: Su Hao (suhao@baidu.com)
//
// Description: The timestamp APIs.

#ifndef BAIDU_IOT_TINYDU_IOT_OS_SRC_IOT_BAIDU_CA_SOURCE_BAIDU_CA_TIMESTAMP_H
#define BAIDU_IOT_TINYDU_IOT_OS_SRC_IOT_BAIDU_CA_SOURCE_BAIDU_CA_TIMESTAMP_H

#include "lightduer_types.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Macros used to compare time values
 *
 *  These inlines deal with timer wrapping correctly. You are
 *  strongly encouraged to use them
 *  1. Because people otherwise forget
 *  2. Because if the timer wrap changes in future you won't have to
 *     alter your code.
 *
 * DUER_TIME_AFTER(a,b) returns true if the time a is after time b.
 */
#define DUER_TIME_AFTER(a, b)              ((duer_s32_t)(b) - (duer_s32_t)(a) < 0)
#define DUER_TIME_BEFORE(a, b)             DUER_TIME_AFTER(b, a)
#define DUER_TIME_AFTER_EQUAL(a, b)        ((duer_s32_t)(a) - (duer_s32_t)(b) >= 0)
#define DUER_TIME_BEFORE_EQUAL(a, b)       DUER_TIME_AFTER_EQUAL(b, a)
#define DUER_TIME_DIFF(old, new) \
                ((duer_u32_t)(new) >= (duer_u32_t)(old) ? \
                 (duer_u32_t)(new) - (duer_u32_t)(old) \
                 : (duer_u32_t)-1 - (duer_u32_t)(old) + (duer_u32_t)(new) + 1)

typedef struct {
    duer_u8_t tm_sec;     // secondsafter the minute - [0,59]
    duer_u8_t tm_min;     // minutesafter the hour - [0,59]
    duer_u8_t tm_hour;    // hours since midnight - [0,23]
    duer_u8_t tm_mday;    // day of the month - [1,31]
    duer_u8_t tm_mon;     // months since January - [1,12]
    int       tm_year;    // years
    duer_u8_t tm_wday;    // days since Sunday - [0,6]
    int       tm_yday;    // days since January 1 - [0,365]
    int       tm_isdst;   // daylight savings time flag
} duer_tm_t;

/*
 * The timestamp callbacks
 * Return the timestamp by milliseconds
 */
typedef duer_u32_t (*duer_timestamp_f)();

/*
 * The real timestamp callbacks
 * Return the timestamp by seconds
 */
typedef duer_u32_t (*duer_real_timestamp_f)();

/*
 * Aquire time tm callbacks
 * Return duer_tm_t struct.
 */
typedef duer_tm_t (*duer_get_tm_f)();

/*
 * Obtain the system timestamp by milliseconds
 *
 * @Return duer_u32_t, the system timestamp by milliseconds
 */
DUER_INT duer_u32_t duer_timestamp(void);

/*
 * Obtain the real timestamp by seconds.
 * Need to be implemented if NO_GET_TIME_OF_DAY defined, as some platform does not have Posix time APIs.
 *
 * @Return duer_u32_t, the real timestamp by seconds since 1970-01-01.
 */
DUER_INT duer_u32_t duer_real_timestamp(void);

/*
 * Aquire time tm struct.
 * Need to be implemented if NO_GET_TIME_OF_DAY defined, as some platform does not have Posix time APIs.
 *
 * @Return duer_tm_t.
 */
duer_tm_t duer_get_tm();

/*
 * Initial the timestamp callbacks for Baidu CA
 *
 * @Param f_timestamp, in, the function obtain the timestamp
 * @Param f_real_timestamp, in, the function obtain the real timestamp
 * @Param f_get_tm, in, the function obtain time tm
 */
DUER_EXT void baidu_ca_timestamp_init(duer_timestamp_f f_timestamp, \
        duer_real_timestamp_f f_real_timestamp, \
        duer_get_tm_f f_get_tm);

#ifdef __cplusplus
}
#endif

#endif // BAIDU_IOT_TINYDU_IOT_OS_SRC_IOT_BAIDU_CA_SOURCE_BAIDU_CA_TIMESTAMP_H
