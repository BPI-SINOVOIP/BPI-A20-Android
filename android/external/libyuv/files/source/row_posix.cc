/*
 *  Copyright (c) 2011 The LibYuv project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#include "row.h"

extern "C" {

#ifdef HAS_ARGBTOYROW_SSSE3

// Constant multiplication table for converting ARGB to I400.
extern "C" TALIGN16(const uint8, kMultiplyMaskARGBToI400[16]) = {
  13u, 64u, 33u, 0u, 13u, 64u, 33u, 0u, 13u, 64u, 33u, 0u, 13u, 64u, 33u, 0u
};

extern "C" TALIGN16(const uint8, kAdd16[16]) = {
  1u, 1u, 1u, 1u, 1u, 1u, 1u, 1u, 1u, 1u, 1u, 1u, 1u, 1u, 1u, 1u
};

// Shuffle table for converting BG24 to ARGB.
extern "C" TALIGN16(const uint8, kShuffleMaskBG24ToARGB[16]) = {
  0u, 1u, 2u, 12u, 3u, 4u, 5u, 13u, 6u, 7u, 8u, 14u, 9u, 10u, 11u, 15u
};

// Shuffle table for converting RAW to ARGB.
extern "C" TALIGN16(const uint8, kShuffleMaskRAWToARGB[16]) = {
  2u, 1u, 0u, 12u, 5u, 4u, 3u, 13u, 8u, 7u, 6u, 14u, 11u, 10u, 9u, 15u
};

void ARGBToYRow_SSSE3(const uint8* src_argb, uint8* dst_y, int pix) {
  asm volatile(
  "movdqa     (%3),%%xmm7\n"
  "movdqa     (%4),%%xmm6\n"
  "movdqa     %%xmm6,%%xmm5\n"
  "psllw      $0x4,%%xmm5\n"  // Generate a mask of 0x10 on each byte.
"1:"
  "movdqa     (%0),%%xmm0\n"
  "pmaddubsw  %%xmm7,%%xmm0\n"
  "movdqa     0x10(%0),%%xmm1\n"
  "psrlw      $0x7,%%xmm0\n"
  "pmaddubsw  %%xmm7,%%xmm1\n"
  "lea        0x20(%0),%0\n"
  "psrlw      $0x7,%%xmm1\n"
  "packuswb   %%xmm1,%%xmm0\n"
  "pmaddubsw  %%xmm6,%%xmm0\n"
  "packuswb   %%xmm0,%%xmm0\n"
  "paddb      %%xmm5,%%xmm0\n"
  "movq       %%xmm0,(%1)\n"
  "lea        0x8(%1),%1\n"
  "sub        $0x8,%2\n"
  "ja         1b\n"
  : "+r"(src_argb),   // %0
    "+r"(dst_y),      // %1
    "+r"(pix)         // %2
  : "r"(kMultiplyMaskARGBToI400),    // %3
    "r"(kAdd16)   // %4
  : "memory"
);
}
#endif

#ifdef  HAS_BG24TOARGBROW_SSSE3
void BG24ToARGBRow_SSSE3(const uint8* src_bg24, uint8* dst_argb, int pix) {
  asm volatile(
  "pcmpeqb    %%xmm7,%%xmm7\n"  // generate mask 0xff000000
  "pslld      $0x18,%%xmm7\n"
  "movdqa     (%3),%%xmm6\n"
"1:"
  "movdqa     (%0),%%xmm0\n"
  "movdqa     0x10(%0),%%xmm1\n"
  "movdqa     0x20(%0),%%xmm3\n"
  "lea        0x30(%0),%0\n"
  "movdqa     %%xmm3,%%xmm2\n"
  "palignr    $0x8,%%xmm1,%%xmm2\n"  // xmm2 = { xmm3[0:3] xmm1[8:15] }
  "pshufb     %%xmm6,%%xmm2\n"
  "por        %%xmm7,%%xmm2\n"
  "palignr    $0xc,%%xmm0,%%xmm1\n"  // xmm1 = { xmm3[0:7] xmm0[12:15] }
  "pshufb     %%xmm6,%%xmm0\n"
  "movdqa     %%xmm2,0x20(%1)\n"
  "por        %%xmm7,%%xmm0\n"
  "pshufb     %%xmm6,%%xmm1\n"
  "movdqa     %%xmm0,(%1)\n"
  "por        %%xmm7,%%xmm1\n"
  "palignr    $0x4,%%xmm3,%%xmm3\n"  // xmm3 = { xmm3[4:15] }
  "pshufb     %%xmm6,%%xmm3\n"
  "movdqa     %%xmm1,0x10(%1)\n"
  "por        %%xmm7,%%xmm3\n"
  "movdqa     %%xmm3,0x30(%1)\n"
  "lea        0x40(%1),%1\n"
  "sub        $0x10,%2\n"
  "ja         1b\n"
  : "+r"(src_bg24),  // %0
    "+r"(dst_argb),  // %1
    "+r"(pix)        // %2
  : "r"(kShuffleMaskBG24ToARGB)  // %3
  : "memory"
);
}

void RAWToARGBRow_SSSE3(const uint8* src_raw, uint8* dst_argb, int pix) {
  asm volatile(
  "pcmpeqb    %%xmm7,%%xmm7\n"  // generate mask 0xff000000
  "pslld      $0x18,%%xmm7\n"
  "movdqa     (%3),%%xmm6\n"
"1:"
  "movdqa     (%0),%%xmm0\n"
  "movdqa     0x10(%0),%%xmm1\n"
  "movdqa     0x20(%0),%%xmm3\n"
  "lea        0x30(%0),%0\n"
  "movdqa     %%xmm3,%%xmm2\n"
  "palignr    $0x8,%%xmm1,%%xmm2\n"  // xmm2 = { xmm3[0:3] xmm1[8:15] }
  "pshufb     %%xmm6,%%xmm2\n"
  "por        %%xmm7,%%xmm2\n"
  "palignr    $0xc,%%xmm0,%%xmm1\n"  // xmm1 = { xmm3[0:7] xmm0[12:15] }
  "pshufb     %%xmm6,%%xmm0\n"
  "movdqa     %%xmm2,0x20(%1)\n"
  "por        %%xmm7,%%xmm0\n"
  "pshufb     %%xmm6,%%xmm1\n"
  "movdqa     %%xmm0,(%1)\n"
  "por        %%xmm7,%%xmm1\n"
  "palignr    $0x4,%%xmm3,%%xmm3\n"  // xmm3 = { xmm3[4:15] }
  "pshufb     %%xmm6,%%xmm3\n"
  "movdqa     %%xmm1,0x10(%1)\n"
  "por        %%xmm7,%%xmm3\n"
  "movdqa     %%xmm3,0x30(%1)\n"
  "lea        0x40(%1),%1\n"
  "sub        $0x10,%2\n"
  "ja         1b\n"
  : "+r"(src_raw),   // %0
    "+r"(dst_argb),  // %1
    "+r"(pix)        // %2
  : "r"(kShuffleMaskRAWToARGB)  // %3
  : "memory"
);
}
#endif

#if defined(__x86_64__)

// 64 bit linux gcc version

void FastConvertYUVToRGB32Row(const uint8* y_buf,  // rdi
                              const uint8* u_buf,  // rsi
                              const uint8* v_buf,  // rdx
                              uint8* rgb_buf,      // rcx
                              int width) {         // r8
  asm volatile(
"1:"
  "movzb  (%1),%%r10\n"
  "lea    1(%1),%1\n"
  "movzb  (%2),%%r11\n"
  "lea    1(%2),%2\n"
  "movq   2048(%5,%%r10,8),%%xmm0\n"
  "movzb  (%0),%%r10\n"
  "movq   4096(%5,%%r11,8),%%xmm1\n"
  "movzb  0x1(%0),%%r11\n"
  "paddsw %%xmm1,%%xmm0\n"
  "movq   (%5,%%r10,8),%%xmm2\n"
  "lea    2(%0),%0\n"
  "movq   (%5,%%r11,8),%%xmm3\n"
  "paddsw %%xmm0,%%xmm2\n"
  "paddsw %%xmm0,%%xmm3\n"
  "shufps $0x44,%%xmm3,%%xmm2\n"
  "psraw  $0x6,%%xmm2\n"
  "packuswb %%xmm2,%%xmm2\n"
  "movq   %%xmm2,0x0(%3)\n"
  "lea    8(%3),%3\n"
  "sub    $0x2,%4\n"
  "ja     1b\n"
  : "+r"(y_buf),    // %0
    "+r"(u_buf),    // %1
    "+r"(v_buf),    // %2
    "+r"(rgb_buf),  // %3
    "+r"(width)     // %4
  : "r" (_kCoefficientsRgbY)  // %5
  : "memory", "r10", "r11", "xmm0", "xmm1", "xmm2", "xmm3"
);
}

void FastConvertYUVToBGRARow(const uint8* y_buf,  // rdi
                             const uint8* u_buf,  // rsi
                             const uint8* v_buf,  // rdx
                             uint8* rgb_buf,      // rcx
                             int width) {         // r8
  asm volatile(
"1:"
  "movzb  (%1),%%r10\n"
  "lea    1(%1),%1\n"
  "movzb  (%2),%%r11\n"
  "lea    1(%2),%2\n"
  "movq   2048(%5,%%r10,8),%%xmm0\n"
  "movzb  (%0),%%r10\n"
  "movq   4096(%5,%%r11,8),%%xmm1\n"
  "movzb  0x1(%0),%%r11\n"
  "paddsw %%xmm1,%%xmm0\n"
  "movq   (%5,%%r10,8),%%xmm2\n"
  "lea    2(%0),%0\n"
  "movq   (%5,%%r11,8),%%xmm3\n"
  "paddsw %%xmm0,%%xmm2\n"
  "paddsw %%xmm0,%%xmm3\n"
  "shufps $0x44,%%xmm3,%%xmm2\n"
  "psraw  $0x6,%%xmm2\n"
  "packuswb %%xmm2,%%xmm2\n"
  "movq   %%xmm2,0x0(%3)\n"
  "lea    8(%3),%3\n"
  "sub    $0x2,%4\n"
  "ja     1b\n"
  : "+r"(y_buf),    // %0
    "+r"(u_buf),    // %1
    "+r"(v_buf),    // %2
    "+r"(rgb_buf),  // %3
    "+r"(width)     // %4
  : "r" (_kCoefficientsBgraY)  // %5
  : "memory", "r10", "r11", "xmm0", "xmm1", "xmm2", "xmm3"
);
}

void FastConvertYUVToABGRRow(const uint8* y_buf,  // rdi
                             const uint8* u_buf,  // rsi
                             const uint8* v_buf,  // rdx
                             uint8* rgb_buf,      // rcx
                             int width) {         // r8
  asm volatile(
"1:"
  "movzb  (%1),%%r10\n"
  "lea    1(%1),%1\n"
  "movzb  (%2),%%r11\n"
  "lea    1(%2),%2\n"
  "movq   2048(%5,%%r10,8),%%xmm0\n"
  "movzb  (%0),%%r10\n"
  "movq   4096(%5,%%r11,8),%%xmm1\n"
  "movzb  0x1(%0),%%r11\n"
  "paddsw %%xmm1,%%xmm0\n"
  "movq   (%5,%%r10,8),%%xmm2\n"
  "lea    2(%0),%0\n"
  "movq   (%5,%%r11,8),%%xmm3\n"
  "paddsw %%xmm0,%%xmm2\n"
  "paddsw %%xmm0,%%xmm3\n"
  "shufps $0x44,%%xmm3,%%xmm2\n"
  "psraw  $0x6,%%xmm2\n"
  "packuswb %%xmm2,%%xmm2\n"
  "movq   %%xmm2,0x0(%3)\n"
  "lea    8(%3),%3\n"
  "sub    $0x2,%4\n"
  "ja     1b\n"
  : "+r"(y_buf),    // %0
    "+r"(u_buf),    // %1
    "+r"(v_buf),    // %2
    "+r"(rgb_buf),  // %3
    "+r"(width)     // %4
  : "r" (_kCoefficientsAbgrY)  // %5
  : "memory", "r10", "r11", "xmm0", "xmm1", "xmm2", "xmm3"
);
}

void FastConvertYUV444ToRGB32Row(const uint8* y_buf,  // rdi
                                 const uint8* u_buf,  // rsi
                                 const uint8* v_buf,  // rdx
                                 uint8* rgb_buf,      // rcx
                                 int width) {         // r8
  asm volatile(
"1:"
  "movzb  (%1),%%r10\n"
  "lea    1(%1),%1\n"
  "movzb  (%2),%%r11\n"
  "lea    1(%2),%2\n"
  "movq   2048(%5,%%r10,8),%%xmm0\n"
  "movzb  (%0),%%r10\n"
  "movq   4096(%5,%%r11,8),%%xmm1\n"
  "paddsw %%xmm1,%%xmm0\n"
  "movq   (%5,%%r10,8),%%xmm2\n"
  "lea    1(%0),%0\n"
  "paddsw %%xmm0,%%xmm2\n"
  "shufps $0x44,%%xmm2,%%xmm2\n"
  "psraw  $0x6,%%xmm2\n"
  "packuswb %%xmm2,%%xmm2\n"
  "movd   %%xmm2,0x0(%3)\n"
  "lea    4(%3),%3\n"
  "sub    $0x1,%4\n"
  "ja     1b\n"
  : "+r"(y_buf),    // %0
    "+r"(u_buf),    // %1
    "+r"(v_buf),    // %2
    "+r"(rgb_buf),  // %3
    "+r"(width)     // %4
  : "r" (_kCoefficientsRgbY)  // %5
  : "memory", "r10", "r11", "xmm0", "xmm1", "xmm2"
);
}

void FastConvertYToRGB32Row(const uint8* y_buf,  // rdi
                            uint8* rgb_buf,      // rcx
                            int width) {         // r8
  asm volatile(
"1:"
  "movzb  (%0),%%r10\n"
  "movzb  0x1(%0),%%r11\n"
  "movq   (%3,%%r10,8),%%xmm2\n"
  "lea    2(%0),%0\n"
  "movq   (%3,%%r11,8),%%xmm3\n"
  "shufps $0x44,%%xmm3,%%xmm2\n"
  "psraw  $0x6,%%xmm2\n"
  "packuswb %%xmm2,%%xmm2\n"
  "movq   %%xmm2,0x0(%1)\n"
  "lea    8(%1),%1\n"
  "sub    $0x2,%2\n"
  "ja     1b\n"
  : "+r"(y_buf),    // %0
    "+r"(rgb_buf),  // %1
    "+r"(width)     // %2
  : "r" (_kCoefficientsRgbY)  // %3
  : "memory", "r10", "r11", "xmm0", "xmm1", "xmm2", "xmm3"
);
}

#elif defined(__i386__)
// 32 bit gcc version

void FastConvertYUVToRGB32Row(const uint8* y_buf,
                              const uint8* u_buf,
                              const uint8* v_buf,
                              uint8* rgb_buf,
                              int width);
  asm(
  ".text\n"
#if defined(OSX) || defined(IOS)
  ".globl _FastConvertYUVToRGB32Row\n"
"_FastConvertYUVToRGB32Row:\n"
#else
  ".global FastConvertYUVToRGB32Row\n"
"FastConvertYUVToRGB32Row:\n"
#endif
  "pusha\n"
  "mov    0x24(%esp),%edx\n"
  "mov    0x28(%esp),%edi\n"
  "mov    0x2c(%esp),%esi\n"
  "mov    0x30(%esp),%ebp\n"
  "mov    0x34(%esp),%ecx\n"

"1:"
  "movzbl (%edi),%eax\n"
  "lea    1(%edi),%edi\n"
  "movzbl (%esi),%ebx\n"
  "lea    1(%esi),%esi\n"
  "movq   _kCoefficientsRgbY+2048(,%eax,8),%mm0\n"
  "movzbl (%edx),%eax\n"
  "paddsw _kCoefficientsRgbY+4096(,%ebx,8),%mm0\n"
  "movzbl 0x1(%edx),%ebx\n"
  "movq   _kCoefficientsRgbY(,%eax,8),%mm1\n"
  "lea    2(%edx),%edx\n"
  "movq   _kCoefficientsRgbY(,%ebx,8),%mm2\n"
  "paddsw %mm0,%mm1\n"
  "paddsw %mm0,%mm2\n"
  "psraw  $0x6,%mm1\n"
  "psraw  $0x6,%mm2\n"
  "packuswb %mm2,%mm1\n"
  "movntq %mm1,0x0(%ebp)\n"
  "lea    8(%ebp),%ebp\n"
  "sub    $0x2,%ecx\n"
  "ja     1b\n"
  "popa\n"
  "ret\n"
);

void FastConvertYUVToBGRARow(const uint8* y_buf,
                              const uint8* u_buf,
                              const uint8* v_buf,
                              uint8* rgb_buf,
                              int width);
  asm(
  ".text\n"
#if defined(OSX) || defined(IOS)
  ".globl _FastConvertYUVToBGRARow\n"
"_FastConvertYUVToBGRARow:\n"
#else
  ".global FastConvertYUVToBGRARow\n"
"FastConvertYUVToBGRARow:\n"
#endif
  "pusha\n"
  "mov    0x24(%esp),%edx\n"
  "mov    0x28(%esp),%edi\n"
  "mov    0x2c(%esp),%esi\n"
  "mov    0x30(%esp),%ebp\n"
  "mov    0x34(%esp),%ecx\n"

"1:"
  "movzbl (%edi),%eax\n"
  "lea    1(%edi),%edi\n"
  "movzbl (%esi),%ebx\n"
  "lea    1(%esi),%esi\n"
  "movq   _kCoefficientsBgraY+2048(,%eax,8),%mm0\n"
  "movzbl (%edx),%eax\n"
  "paddsw _kCoefficientsBgraY+4096(,%ebx,8),%mm0\n"
  "movzbl 0x1(%edx),%ebx\n"
  "movq   _kCoefficientsBgraY(,%eax,8),%mm1\n"
  "lea    2(%edx),%edx\n"
  "movq   _kCoefficientsBgraY(,%ebx,8),%mm2\n"
  "paddsw %mm0,%mm1\n"
  "paddsw %mm0,%mm2\n"
  "psraw  $0x6,%mm1\n"
  "psraw  $0x6,%mm2\n"
  "packuswb %mm2,%mm1\n"
  "movntq %mm1,0x0(%ebp)\n"
  "lea    8(%ebp),%ebp\n"
  "sub    $0x2,%ecx\n"
  "ja     1b\n"
  "popa\n"
  "ret\n"
);

void FastConvertYUVToABGRRow(const uint8* y_buf,
                              const uint8* u_buf,
                              const uint8* v_buf,
                              uint8* rgb_buf,
                              int width);
  asm(
  ".text\n"
#if defined(OSX) || defined(IOS)
  ".globl _FastConvertYUVToABGRRow\n"
"_FastConvertYUVToABGRRow:\n"
#else
  ".global FastConvertYUVToABGRRow\n"
"FastConvertYUVToABGRRow:\n"
#endif
  "pusha\n"
  "mov    0x24(%esp),%edx\n"
  "mov    0x28(%esp),%edi\n"
  "mov    0x2c(%esp),%esi\n"
  "mov    0x30(%esp),%ebp\n"
  "mov    0x34(%esp),%ecx\n"

"1:"
  "movzbl (%edi),%eax\n"
  "lea    1(%edi),%edi\n"
  "movzbl (%esi),%ebx\n"
  "lea    1(%esi),%esi\n"
  "movq   _kCoefficientsAbgrY+2048(,%eax,8),%mm0\n"
  "movzbl (%edx),%eax\n"
  "paddsw _kCoefficientsAbgrY+4096(,%ebx,8),%mm0\n"
  "movzbl 0x1(%edx),%ebx\n"
  "movq   _kCoefficientsAbgrY(,%eax,8),%mm1\n"
  "lea    2(%edx),%edx\n"
  "movq   _kCoefficientsAbgrY(,%ebx,8),%mm2\n"
  "paddsw %mm0,%mm1\n"
  "paddsw %mm0,%mm2\n"
  "psraw  $0x6,%mm1\n"
  "psraw  $0x6,%mm2\n"
  "packuswb %mm2,%mm1\n"
  "movntq %mm1,0x0(%ebp)\n"
  "lea    8(%ebp),%ebp\n"
  "sub    $0x2,%ecx\n"
  "ja     1b\n"
  "popa\n"
  "ret\n"
);

void FastConvertYUV444ToRGB32Row(const uint8* y_buf,
                                 const uint8* u_buf,
                                 const uint8* v_buf,
                                 uint8* rgb_buf,
                                 int width);
  asm(
  ".text\n"
#if defined(OSX) || defined(IOS)
  ".globl _FastConvertYUV444ToRGB32Row\n"
"_FastConvertYUV444ToRGB32Row:\n"
#else
  ".global FastConvertYUV444ToRGB32Row\n"
"FastConvertYUV444ToRGB32Row:\n"
#endif
  "pusha\n"
  "mov    0x24(%esp),%edx\n"
  "mov    0x28(%esp),%edi\n"
  "mov    0x2c(%esp),%esi\n"
  "mov    0x30(%esp),%ebp\n"
  "mov    0x34(%esp),%ecx\n"

"1:"
  "movzbl (%edi),%eax\n"
  "lea    1(%edi),%edi\n"
  "movzbl (%esi),%ebx\n"
  "lea    1(%esi),%esi\n"
  "movq   _kCoefficientsRgbY+2048(,%eax,8),%mm0\n"
  "movzbl (%edx),%eax\n"
  "paddsw _kCoefficientsRgbY+4096(,%ebx,8),%mm0\n"
  "lea    1(%edx),%edx\n"
  "paddsw _kCoefficientsRgbY(,%eax,8),%mm0\n"
  "psraw  $0x6,%mm0\n"
  "packuswb %mm0,%mm0\n"
  "movd   %mm0,0x0(%ebp)\n"
  "lea    4(%ebp),%ebp\n"
  "sub    $0x1,%ecx\n"
  "ja     1b\n"
  "popa\n"
  "ret\n"
);

void FastConvertYToRGB32Row(const uint8* y_buf,
                            uint8* rgb_buf,
                            int width);
  asm(
  ".text\n"
#if defined(OSX) || defined(IOS)
  ".globl _FastConvertYToRGB32Row\n"
"_FastConvertYToRGB32Row:\n"
#else
  ".global FastConvertYToRGB32Row\n"
"FastConvertYToRGB32Row:\n"
#endif
  "push   %ebx\n"
  "mov    0x8(%esp),%eax\n"
  "mov    0xc(%esp),%edx\n"
  "mov    0x10(%esp),%ecx\n"

"1:"
  "movzbl (%eax),%ebx\n"
  "movq   _kCoefficientsRgbY(,%ebx,8),%mm0\n"
  "psraw  $0x6,%mm0\n"
  "movzbl 0x1(%eax),%ebx\n"
  "movq   _kCoefficientsRgbY(,%ebx,8),%mm1\n"
  "psraw  $0x6,%mm1\n"
  "packuswb %mm1,%mm0\n"
  "lea    0x2(%eax),%eax\n"
  "movq   %mm0,(%edx)\n"
  "lea    0x8(%edx),%edx\n"
  "sub    $0x2,%ecx\n"
  "ja     1b\n"
  "pop    %ebx\n"
  "ret\n"
);

#else
// C reference code that mimic the YUV assembly.
#define packuswb(x) ((x) < 0 ? 0 : ((x) > 255 ? 255 : (x)))
#define paddsw(x, y) (((x) + (y)) < -32768 ? -32768 : \
    (((x) + (y)) > 32767 ? 32767 : ((x) + (y))))

static inline void YuvPixel(uint8 y,
                            uint8 u,
                            uint8 v,
                            uint8* rgb_buf,
                            int ashift,
                            int rshift,
                            int gshift,
                            int bshift) {

  int b = _kCoefficientsRgbY[256+u][0];
  int g = _kCoefficientsRgbY[256+u][1];
  int r = _kCoefficientsRgbY[256+u][2];
  int a = _kCoefficientsRgbY[256+u][3];

  b = paddsw(b, _kCoefficientsRgbY[512+v][0]);
  g = paddsw(g, _kCoefficientsRgbY[512+v][1]);
  r = paddsw(r, _kCoefficientsRgbY[512+v][2]);
  a = paddsw(a, _kCoefficientsRgbY[512+v][3]);

  b = paddsw(b, _kCoefficientsRgbY[y][0]);
  g = paddsw(g, _kCoefficientsRgbY[y][1]);
  r = paddsw(r, _kCoefficientsRgbY[y][2]);
  a = paddsw(a, _kCoefficientsRgbY[y][3]);

  b >>= 6;
  g >>= 6;
  r >>= 6;
  a >>= 6;

  *reinterpret_cast<uint32*>(rgb_buf) = (packuswb(b) << bshift) |
                                        (packuswb(g) << gshift) |
                                        (packuswb(r) << rshift) |
                                        (packuswb(a) << ashift);
}

void FastConvertYUVToRGB32Row(const uint8* y_buf,
                              const uint8* u_buf,
                              const uint8* v_buf,
                              uint8* rgb_buf,
                              int width) {
  for (int x = 0; x < width; x += 2) {
    uint8 u = u_buf[x >> 1];
    uint8 v = v_buf[x >> 1];
    uint8 y0 = y_buf[x];
    YuvPixel(y0, u, v, rgb_buf, 24, 16, 8, 0);
    if ((x + 1) < width) {
      uint8 y1 = y_buf[x + 1];
      YuvPixel(y1, u, v, rgb_buf + 4, 24, 16, 8, 0);
    }
    rgb_buf += 8;  // Advance 2 pixels.
  }
}

void FastConvertYUVToBGRARow(const uint8* y_buf,
                             const uint8* u_buf,
                             const uint8* v_buf,
                             uint8* rgb_buf,
                             int width) {
  for (int x = 0; x < width; x += 2) {
    uint8 u = u_buf[x >> 1];
    uint8 v = v_buf[x >> 1];
    uint8 y0 = y_buf[x];
    YuvPixel(y0, u, v, rgb_buf, 0, 8, 16, 24);
    if ((x + 1) < width) {
      uint8 y1 = y_buf[x + 1];
      YuvPixel(y1, u, v, rgb_buf + 4, 0, 8, 16, 24);
    }
    rgb_buf += 8;  // Advance 2 pixels.
  }
}

void FastConvertYUVToABGRRow(const uint8* y_buf,
                             const uint8* u_buf,
                             const uint8* v_buf,
                             uint8* rgb_buf,
                             int width) {
  for (int x = 0; x < width; x += 2) {
    uint8 u = u_buf[x >> 1];
    uint8 v = v_buf[x >> 1];
    uint8 y0 = y_buf[x];
    YuvPixel(y0, u, v, rgb_buf, 24, 0, 8, 16);
    if ((x + 1) < width) {
      uint8 y1 = y_buf[x + 1];
      YuvPixel(y1, u, v, rgb_buf + 4, 24, 0, 8, 16);
    }
    rgb_buf += 8;  // Advance 2 pixels.
  }
}

void FastConvertYUV444ToRGB32Row(const uint8* y_buf,
                                 const uint8* u_buf,
                                 const uint8* v_buf,
                                 uint8* rgb_buf,
                                 int width) {
  for (int x = 0; x < width; ++x) {
    uint8 u = u_buf[x];
    uint8 v = v_buf[x];
    uint8 y = y_buf[x];
    YuvPixel(y, u, v, rgb_buf, 24, 16, 8, 0);
    rgb_buf += 4;  // Advance 1 pixel.
  }
}

void FastConvertYToRGB32Row(const uint8* y_buf,
                            uint8* rgb_buf,
                            int width) {
  for (int x = 0; x < width; ++x) {
    uint8 y = y_buf[x];
    YuvPixel(y, 128, 128, rgb_buf, 24, 16, 8, 0);
    rgb_buf += 4;  // Advance 1 pixel.
  }
}

#endif

}  // extern "C"
