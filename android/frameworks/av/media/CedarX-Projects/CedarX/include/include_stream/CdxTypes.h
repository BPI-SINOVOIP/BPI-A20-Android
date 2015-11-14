#ifndef CDX_TYPES_H
#define CDX_TYPES_H

#ifdef NULL
#undef NULL
#endif
#define NULL 0

#define CDX_SUCCESS 0
#define CDX_FAILURE (-1)

typedef long long cdx_int64;
typedef unsigned long long cdx_uint64;

typedef int cdx_int32;
typedef unsigned int cdx_uint32;

typedef short cdx_int16;
typedef unsigned short cdx_uint16;

typedef unsigned char cdx_uint8;
typedef char cdx_int8;

typedef unsigned long cdx_ulong;
typedef long cdx_long;

typedef char cdx_char;
typedef int cdx_bool;
typedef void cdx_void;
typedef unsigned long cdx_size;
typedef signed long cdx_ssize;

typedef float cdx_float;

#define CDX_INTERFACE static inline

typedef cdx_int32 cdx_err;

#define CDX_TRUE 1
#define CDX_FALSE 0

#define CedarXMin(a, b) ((a) < (b) ? (a) : (b))

#define CdxOffsetof(TYPE, MEMBER) ((size_t) &((TYPE *)0)->MEMBER) 

#define CdxContainerOf(ptr, type, member) ({ \
    const typeof(((type *)0)->member) *__mptr = (ptr); \
    (type *)((char *)__mptr - CdxOffsetof(type,member) ); })

#endif
