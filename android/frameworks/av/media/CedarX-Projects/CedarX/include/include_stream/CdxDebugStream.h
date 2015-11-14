#ifndef CDX_DEBUG_H
#define CDX_DEBUG_H
#include <pthread.h>

#ifdef __cplusplus
extern "C"
{
#endif

void CdxDumpThreadStack(pthread_t tid);

void CdxCallStack(void);


#ifdef __cplusplus
}
#endif

#endif
