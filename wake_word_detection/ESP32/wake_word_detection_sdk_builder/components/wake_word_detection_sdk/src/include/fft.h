/* Copyright (c) 2016 HR, adapted for ESP-IDF
 * FFT routines ported from wekws/runtime/core/frontend/fft
 */

// 内部私有头（非对外 API）：radix-2 就地复数 FFT，供 Fbank 频谱分析使用。

#ifndef FFT_H_
#define FFT_H_

#ifdef __cplusplus
extern "C" {  // C 链接：移植的 C 代码，供 C++ 侧 Fbank 调用
#endif

#ifndef M_PI
#define M_PI 3.1415926535897932384626433832795
#endif
#ifndef M_2PI
#define M_2PI 6.283185307179586476925286766559005  // 当前 fft.cc 未用，保留备用
#endif

// 预算大小为 n 的 FFT 正弦查找表（构造时一次）。
// sintbl 容量须 >= n + n/4（内部最大写到下标 n + n/4 - 1）。
void fft_make_sintbl(int n, float* sintbl);

// 预算大小为 n 的位反转置换表（构造时一次）。bitrev 容量须 >= n。
void fft_make_bitrev(int n, int* bitrev);

// 就地 radix-2 复数 FFT。
//   bitrev/sintbl: 由上面两函数按同尺寸 |n| 预算好的表
//   x/y          : 实部/虚部（各长 n），就地覆盖为变换结果
//   n            : 必须是 2 的幂；正=正变换，负=逆变换(带 1/n 归一化)
// 返回：恒为 0（未使用）。
int fft_compute(const int* bitrev, const float* sintbl, float* x, float* y, int n);

#ifdef __cplusplus
}
#endif

#endif  // FFT_H_
