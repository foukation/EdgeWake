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
 * File: duerapp.c
 * Auth: Su Hao(suhao@baidu.com)
 * Desc: Duer Application Wi-Fi configuration.
 */

#include <string.h>

#include "duerapp_config.h"

#define EXAMPLE_WIFI_SSID "iottest"
#define EXAMPLE_WIFI_PASS "12345678"

/* FreeRTOS event group to signal when we are connected & ready to make a request */

/* The event group allows multiple bits for each event,
   but we only care about one event - are we connected
   to the AP with an IP? */

void duer_test_start();


void initialize_wifi(void)
{
    
}
