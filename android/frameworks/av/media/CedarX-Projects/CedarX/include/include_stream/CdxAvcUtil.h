#ifndef CDX_AVC_UTIL_H
#define CDX_AVC_UTIL_H
#include <CdxTypes.h>
#include <CdxBuffer.h>

#ifdef __cplusplus
extern "C"
{
#endif

cdx_void CdxFindAVCDimensions(CdxBufferT *seqParamSet,
                                cdx_int32 *pWidth, cdx_int32 *pHeight);

#ifdef __cplusplus
}
#endif

#endif
