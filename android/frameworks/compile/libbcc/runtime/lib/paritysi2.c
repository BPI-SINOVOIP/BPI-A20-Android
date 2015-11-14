/* ===-- paritysi2.c - Implement __paritysi2 -------------------------------===
 *
 *                     The LLVM Compiler Infrastructure
 *
 * This file is distributed under the University of Illinois Open Source
 * License. See LICENSE.TXT for details.
 *
 * ===----------------------------------------------------------------------===
 *
 * This file implements __paritysi2 for the compiler_rt library.
 *
 * ===----------------------------------------------------------------------===
 */

#include "int_lib.h"

/* Returns: 1 if number of bits is odd else returns 0 */

si_int
__paritysi2(si_int a)
{
    su_int x = (su_int)a;
    x ^= x >> 16;
    x ^= x >> 8;
    x ^= x >> 4;
    return (0x6996 >> (x & 0xF)) & 1;
}
