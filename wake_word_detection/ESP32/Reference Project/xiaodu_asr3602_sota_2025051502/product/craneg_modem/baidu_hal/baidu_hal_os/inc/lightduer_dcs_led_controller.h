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
 * File: lightduer_dcs_led_controller.h
 * Auth: Ning Wang (wangning23@baidu.com)
 * Desc: apply APIs to support DCS led_controller Interface.
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_DCS_LED_CONTROLLER_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_DCS_LED_CONTROLLER_H

#include "lightduer_dcs.h"
#include "lightduer_dcs_local.h"

/**
 * DESC:
 * Initialize led interface.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_led_controller_init(void);

#endif