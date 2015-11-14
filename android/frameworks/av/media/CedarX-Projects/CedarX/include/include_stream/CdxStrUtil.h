#ifndef CDX_STR_UTIL_H
#define CDX_STR_UTIL_H

#include <CdxTypes.h>
#include <CdxList.h>

struct CdxStrItemS
{
    CdxListNodeT node;
    cdx_char *val;
};

#ifdef __cplusplus
extern "C"
{
#endif

cdx_void CdxStrTrimTail(cdx_char *str);

cdx_void CdxStrTrimHead(cdx_char *str);

cdx_void CdxStrTrim(cdx_char *str);

cdx_void CdxStrTolower(cdx_char *str);

cdx_char *CdxStrAttributeOfKey(const cdx_char *str, cdx_char *key, cdx_char sepa);

cdx_uint32 CdxStrSplit(cdx_char *str, char sepa, CdxListT *itemList);

#ifdef __cplusplus
}
#endif

#endif
