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
// Author: Chen Chi (chenchi01@baidu.com)
//
// Description: cert info.

#ifndef LIBDUER_DEVICE_FRAMEWORK_CORE_LIGHTDUER_CA_CERT_H
#define LIBDUER_DEVICE_FRAMEWORK_CORE_LIGHTDUER_CA_CERT_H

#ifdef __cplusplus
extern "C" {
#endif

#ifdef CONFIG_CACERT_ENABLED
const char *duer_get_ca_root_cert();
#endif

#ifdef __cplusplus
}
#endif

#endif  // LIBDUER_DEVICE_FRAMEWORK_CORE_LIGHTDUER_CA_CERT_H
