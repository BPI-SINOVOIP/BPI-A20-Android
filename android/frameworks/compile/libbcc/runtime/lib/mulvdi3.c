/*===-- mulvdi3.c - Implement __mulvdi3 -----------------------------------===
 *
 *                     The LLVM Compiler Infrastructure
 *
 * This file is distributed under the University of Illinois Open Source
 * License. See LICENSE.TXT for details.
 *
 * ===----------------------------------------------------------------------===
 *
 * This file implements __mulvdi3 for the compiler_rt library.
 *
 * ===----------------------------------------------------------------------===
 */

#include "int_lib.h"
#include <stdlib.h>

/* Returns: a * b */

/* Effects: aborts if a * b overflows */

di_int
__mulvdi3(di_int a, di_int b)
{
    const int N = (int)(sizeof(di_int) * CHAR_BIT);
    const di_int MIN = (di_int)1 << (N-1);
    const di_int MAX = ~MIN;
    if (a == MIN)
    {
        if (b == 0 || b == 1)
            return a * b;
        abort();
    }
    if (b == MIN)
    {
        if (a == 0 || a == 1)
            return a * b;
        abort();
    }
    di_int sa = a >> (N - 1);
    di_int abs_a = (a ^ sa) - sa;
    di_int sb = b >> (N - 1);
    di_int abs_b = (b ^ sb) - sb;
    if (abs_a < 2 || abs_b < 2)
        return a * b;
    if (sa == sb)
    {
        if (abs_a > MAX / abs_b)
            abort();
    }
    else
    {
        if (abs_a > MIN / -abs_b)
            abort();
    }
    return a * b;
}
