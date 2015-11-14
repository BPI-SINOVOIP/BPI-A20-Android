/* ===---------- eprintf.c - Implements __eprintf --------------------------===
 *
 *                     The LLVM Compiler Infrastructure
 *
 * This file is distributed under the University of Illinois Open Source
 * License. See LICENSE.TXT for details.
 *
 * ===----------------------------------------------------------------------===
 */



#include <stdio.h>
#include <stdlib.h>


/*
 * __eprintf() was used in an old version of <assert.h>.
 * It can eventually go away, but it is needed when linking
 * .o files built with the old <assert.h>.
 *
 * It should never be exported from a dylib, so it is marked
 * visibility hidden.
 */
__attribute__((visibility("hidden")))
void __eprintf(const char* format, const char* assertion_expression,
				const char* line, const char* file)
{
	fprintf(stderr, format, assertion_expression, line, file);
	fflush(stderr);
	abort();
}
