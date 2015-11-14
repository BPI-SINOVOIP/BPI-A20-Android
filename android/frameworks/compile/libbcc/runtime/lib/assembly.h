/* ===-- assembly.h - compiler-rt assembler support macros -----------------===
 *
 *                     The LLVM Compiler Infrastructure
 *
 * This file is distributed under the University of Illinois Open Source
 * License. See LICENSE.TXT for details.
 *
 * ===----------------------------------------------------------------------===
 *
 * This file defines macros for use in compiler-rt assembler source.
 * This file is not part of the interface of this library.
 *
 * ===----------------------------------------------------------------------===
 */

#ifndef COMPILERRT_ASSEMBLY_H
#define COMPILERRT_ASSEMBLY_H

#if defined(__POWERPC__) || defined(__powerpc__) || defined(__ppc__)
#define SEPARATOR @
#else
#define SEPARATOR ;
#endif

/* We can't use __USER_LABEL_PREFIX__ here, it isn't possible to concatenate the
   *values* of two macros. This is quite brittle, though. */
#if defined(__APPLE__)
#define SYMBOL_NAME(name) _##name
#else
#define SYMBOL_NAME(name) name
#endif

#ifdef VISIBILITY_HIDDEN
#define DEFINE_COMPILERRT_FUNCTION(name) \
  .globl SYMBOL_NAME(name) SEPARATOR     \
  .private_extern SYMBOL_NAME(name) SEPARATOR    \
  SYMBOL_NAME(name):
#else
#define DEFINE_COMPILERRT_FUNCTION(name) \
  .globl SYMBOL_NAME(name) SEPARATOR     \
  SYMBOL_NAME(name):
#endif

#define DEFINE_COMPILERRT_PRIVATE_FUNCTION(name) \
  .globl SYMBOL_NAME(name) SEPARATOR             \
  .private_extern SYMBOL_NAME(name) SEPARATOR    \
  SYMBOL_NAME(name):

#define DEFINE_COMPILERRT_PRIVATE_FUNCTION_UNMANGLED(name) \
  .globl name SEPARATOR             \
  .private_extern name SEPARATOR    \
  name:

#endif /* COMPILERRT_ASSEMBLY_H */
