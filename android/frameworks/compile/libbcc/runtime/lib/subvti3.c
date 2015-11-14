/* ===-- subvti3.c - Implement __subvti3 -----------------------------------===
 *
 *      	       The LLVM Compiler Infrastructure
 *
 * This file is distributed under the University of Illinois Open Source
 * License. See LICENSE.TXT for details.
 *
 * ===----------------------------------------------------------------------===
 *
 * This file implements __subvti3 for the compiler_rt library.
 *
 * ===----------------------------------------------------------------------===
 */

#if __x86_64

#include "int_lib.h"
#include <stdlib.h>

/* Returns: a - b */

/* Effects: aborts if a - b overflows */

ti_int
__subvti3(ti_int a, ti_int b)
{
    ti_int s = a - b;
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

#endif /* __x86_64 */
