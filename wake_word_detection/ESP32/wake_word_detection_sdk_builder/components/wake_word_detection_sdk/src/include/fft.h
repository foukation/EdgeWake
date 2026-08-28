/* Copyright (c) 2016 HR, adapted for ESP-IDF
 * FFT routines ported from wekws/runtime/core/frontend/fft
 */

#ifndef FFT_H_
#define FFT_H_

#ifdef __cplusplus
extern "C" {
#endif

#ifndef M_PI
#define M_PI 3.1415926535897932384626433832795
#endif
#ifndef M_2PI
#define M_2PI 6.283185307179586476925286766559005
#endif

void fft_make_sintbl(int n, float* sintbl);
void fft_make_bitrev(int n, int* bitrev);
int fft_compute(const int* bitrev, const float* sintbl, float* x, float* y, int n);

#ifdef __cplusplus
}
#endif

#endif  // FFT_H_
