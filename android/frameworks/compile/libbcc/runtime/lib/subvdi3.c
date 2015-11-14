/* ===-- subvdi3.c - Implement __subvdi3 -----------------------------------===
 *
 *      	       The LLVM Compiler Infrastructure
 *
 * This file is distributed under the University of Illinois Open Source
 * License. See LICENSE.TXT for details.
 *
 * ===----------------------------------------------------------------------===
 *
 * This file implements __subvdi3 for the compiler_rt library.
 *
 * ===----------------------------------------------------------------------===
 */

#include "int_lib.h"
#include <stdlib.h>

/* Returns: a - b */

/* Effects: aborts if a - b overflows */

di_int
__subvdi3(di_int a, di_int b)
{
    di_int s = a - b;
    if (b >= 0)
    {
        if (s > a)
            abort();
    }
    else
    {
        if (s <= a)
            abort();
    }
    return s;
}
