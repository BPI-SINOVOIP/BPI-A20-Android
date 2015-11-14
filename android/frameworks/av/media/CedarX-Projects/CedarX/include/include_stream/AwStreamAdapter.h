#ifndef AW_STREAM_ADAPTER_H
#define AW_STREAM_ADAPTER_H

#include <CDX_Common.h>
#include <cedarx_stream.h>
//#include <cedarx_demux.h>
#ifdef __cplusplus
extern "C"
{
#endif

void AwStreamForceStop(void * handle);

int AwStreamCreateHandle(CedarXDataSourceDesc *ds, struct cdx_stream_info *stream);

void AwStreamDestroyHandle(struct cdx_stream_info *stream);

#ifdef __cplusplus
}
#endif

#endif
