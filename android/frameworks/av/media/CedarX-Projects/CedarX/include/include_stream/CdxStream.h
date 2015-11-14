/*
**********************************************************************
* File Name  : CdxStream.h
* Author       : bzchen@allwinnertech.com
* Version      : 1.0
* Data          : 2013.08.29
* Description: This file define data structure and interface of the stream module.
**********************************************************************/

#ifndef CDX_STREAM_H
#define CDX_STREAM_H
#include "CdxTypes.h"
#include "CdxLog.h"
#include "CdxMemory.h"
#include "CdxBinary.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct CdxStreamProbeDataS CdxStreamProbeDataT;
typedef struct CdxStreamCreatorS CdxStreamCreatorT;

#define STREAM_SEEK_SET 0           //* offset from the file start.
#define STREAM_SEEK_CUR 1           //* offset from current file position.
#define STREAM_SEEK_END 2           //* offset from the file end.

#define CDX_STREAM_FLAG_SEEK    0x01U
#define CDX_STREAM_FLAG_STT     0x02U   /*seek to time*/
#define CDX_STREAM_FLAG_NET     0x04U   /*net work stream*/

typedef struct CdxDataSourceS CdxDataSourceT;
typedef struct CdxStreamS CdxStreamT;

typedef struct CdxHttpHeaderField
{
    const char *key;
    const char *val;
}CdxHttpHeaderFieldT;
typedef struct CdxHttpHeaderFields
{
    int num;
    CdxHttpHeaderFieldT *pHttpHeader;
}CdxHttpHeaderFieldsT;

enum DSExtraDataTypeE
{
    EXTRA_DATA_HTTP_HEADER,
    EXTRA_DATA_UNKNOWN,
};

struct CdxDataSourceS
{
    cdx_char *uri;  /* format : "scheme://..." */
    cdx_void *extraData; /* extra data for some stream, ex: http header for http stream */
    enum DSExtraDataTypeE extraDataType;
};

//***********************************************************//
//* Define IO status.
//***********************************************************//
enum CdxIOStateE
{
    CDX_IO_STATE_OK = 0,  //* nothing wrong of the data accession.
    CDX_IO_STATE_INVALID,
    CDX_IO_STATE_ERROR,   //* unknown error, can't access the media file.
    CDX_IO_STATE_EOS      /*end of stream*/
};

/*for stream->control*/
enum CdxStreamCommandE
{
    STREAM_CMD_FORCE_STOP = 0x100,    
        /* Force stop the stream running job.  */
        
    STREAM_CMD_GET_DURATION = 0x101,    
        /* Get the duration of the media file.
            *param is a pointer to a int64_t variable, duration = *(int64_t*)param.
            *return 0 if OK, return -1 if not supported by the stream handler. 
            */

    STREAM_CMD_READ_NOBLOCK = 0x102,
        /* for sock recv in seek operation, return as soon as possible.
             * return 0 if OK, return -1 if not support.
             */
             
    STREAM_CMD_GET_SOCKRECVBUFLEN = 0x103,
        /* Get the socket recv buf len, *(cdx_int32*)param.
             * return 0 if OK, return -1 if not support.
             */
        /* To Add More commands here.*/

        /* struct StreamCacheStateS */
    STREAM_CMD_GET_CACHESTATE = 0x104,
};

typedef enum CdxIOStateE CdxIOStateT;
typedef enum CdxStreamCommandE CdxStreamCommandT;

struct StreamCacheStateS
{
    cdx_int32 nCacheCapacity;
    cdx_int32 nCacheSize;
    cdx_int32 nBandwidthKbps;
};

struct CdxStreamProbeDataS
{
    cdx_char *buf;
    cdx_uint32 len;
};

struct CdxStreamCreatorS
{
    CdxStreamT *(*open)(CdxDataSourceT *);  
};

struct CdxStreamOpsS
{
    CdxStreamProbeDataT *(*getProbeData)(CdxStreamT * /*stream*/);
    
    cdx_int32 (*read)(CdxStreamT * /*stream*/, void * /*buf*/, cdx_uint32 /*len*/);

    cdx_int32 (*write)(CdxStreamT *, void * /*buf*/, cdx_uint32 /*len*/);

    cdx_int32 (*close)(CdxStreamT * /*stream*/);

    cdx_int32 (*getIOState)(CdxStreamT * /*stream*/);

    cdx_uint32 (*attribute)(CdxStreamT * /*stream*/);

    cdx_int32 (*control)(CdxStreamT * /*stream*/, cdx_int32 /*cmd*/, void * /*param*/);

    /*以上接口必须实现*/

    cdx_int32 (*getMetaData)(CdxStreamT *, const cdx_char * /*key*/, cdx_char ** /*pVal*/);
       
    cdx_int32 (*seek)(CdxStreamT * /*stream*/, cdx_int64 /*offset*/, cdx_int32 /*whence*/);

    cdx_int32 (*seekToTime)(CdxStreamT * /*stream*/, cdx_int64 /*time us*/);

    cdx_bool (*eos)(CdxStreamT * /*stream*/);

    cdx_int64 (*tell)(CdxStreamT * /*stream*/);

    cdx_int64 (*size)(CdxStreamT * /*stream*/);

    cdx_int32 (*forceStop)(CdxStreamT * /*stream*/);
    
    /*cut  Size/CachedSize/SetBufferSize/GetBufferSize */
};

struct CdxStreamS
{
    struct CdxStreamOpsS *ops;
};


CdxStreamT *CdxStreamOpen(CdxDataSourceT *source);

CDX_INTERFACE CdxStreamProbeDataT *CdxStreamGetProbeData(CdxStreamT *stream)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);
    CDX_CHECK(stream->ops->getProbeData);
    return stream->ops->getProbeData(stream);
}

CDX_INTERFACE cdx_int32 CdxStreamRead(CdxStreamT *stream, void *buf, cdx_int32 len)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);
    CDX_CHECK(stream->ops->read);
    return stream->ops->read(stream, buf, len);
}

CDX_INTERFACE cdx_int32 CdxStreamWrite(CdxStreamT *stream, void *buf, cdx_int32 len)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);

    return stream->ops->write ? stream->ops->write(stream, buf, len) : -1;
}

CDX_INTERFACE cdx_int32 CdxStreamClose(CdxStreamT *stream)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);
    CDX_CHECK(stream->ops->close);
    return stream->ops->close(stream);
}

CDX_INTERFACE cdx_int32 CdxStreamGetIoState(CdxStreamT *stream)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);
    CDX_CHECK(stream->ops->getIOState);
    return stream->ops->getIOState(stream);
}

CDX_INTERFACE cdx_uint32 CdxStreamAttribute(CdxStreamT *stream)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);
    CDX_CHECK(stream->ops->attribute);
    return stream->ops->attribute(stream);
}

CDX_INTERFACE cdx_int32 CdxStreamControl(CdxStreamT *stream, 
                                        cdx_int32 cmd, void *param)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);
    CDX_CHECK(stream->ops->control);
    return stream->ops->control(stream, cmd, param);
}

CDX_INTERFACE cdx_int32 CdxStreamGetMetaData(CdxStreamT *stream, 
                                        const cdx_char *key, cdx_char **pVal)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);
//    CDX_CHECK(stream->ops->getMetaData);
    return stream->ops->getMetaData ? 
            stream->ops->getMetaData(stream, key, pVal) :
            -1;
}

CDX_INTERFACE cdx_int32 CdxStreamSeek(CdxStreamT *stream, cdx_int64 offset,
                                    cdx_int32 whence)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);

    return stream->ops->seek ? stream->ops->seek(stream, offset, whence) : -1;
}

CDX_INTERFACE cdx_int32 CdxStreamSeekToTime(CdxStreamT *stream, cdx_int64 timeUs)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);

    return stream->ops->seekToTime ? stream->ops->seekToTime(stream, timeUs) : -1;
}

CDX_INTERFACE cdx_bool CdxStreamEos(CdxStreamT *stream)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);

    return stream->ops->eos ? stream->ops->eos(stream) : CDX_FALSE;
}

CDX_INTERFACE cdx_int64 CdxStreamTell(CdxStreamT *stream)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);

    return stream->ops->tell ? stream->ops->tell(stream) : -1;
}

CDX_INTERFACE cdx_int32 CdxStreamForceStop(CdxStreamT *stream)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);

    return stream->ops->forceStop ? stream->ops->forceStop(stream) : -1;
}

CDX_INTERFACE cdx_int64 CdxStreamSize(CdxStreamT *stream)
{
    CDX_CHECK(stream);
    CDX_CHECK(stream->ops);

    return stream->ops->size ? stream->ops->size(stream) : (-1LL);
}

#define CdxStreamSeekAble(stream) \
    (!!(CdxStreamAttribute(stream) & CDX_STREAM_FLAG_SEEK))

#define CdxStreamIsNetStream(stream) \
    (!!(CdxStreamAttribute(stream) & CDX_STREAM_FLAG_NET))

static inline cdx_int32 CdxStreamSkip(CdxStreamT *stream, cdx_uint32 len) 
{
    if (CdxStreamSeekAble(stream))
    {
        return CdxStreamSeek(stream, (cdx_int64)len, STREAM_SEEK_CUR);
    }
    else
    {
        cdx_int32 ret;
        cdx_void *dummyBuf;
        dummyBuf = CdxMalloc(len);
        CDX_FORCE_CHECK(dummyBuf);
        
        ret = CdxStreamRead(stream, dummyBuf, len);
        CDX_FORCE_CHECK((cdx_uint32)ret == len);
        CdxFree(dummyBuf);
        
        return CDX_SUCCESS;
    }
}

#define CdxStreamGetU8(stream) \
    ({cdx_uint8 data; CdxStreamRead(stream, &data, 1); GetU8(&data);})

#define CdxStreamGetLE16(stream) \
    ({cdx_uint8 data[2]; CdxStreamRead(stream, data, 2); GetLE16(data);})

#define CdxStreamGetLE32(stream) \
        ({cdx_uint8 data[4]; CdxStreamRead(stream, data, 4); GetLE32(data);})

#define CdxStreamGetLE64(stream) \
        ({cdx_uint8 data[8]; CdxStreamRead(stream, data, 8); GetLE64(data);})

#define CdxStreamGetBE16(stream) \
    ({cdx_uint8 data[2]; CdxStreamRead(stream, data, 2); GetBE16(data);})

#define CdxStreamGetBE32(stream) \
    ({cdx_uint8 data[4]; CdxStreamRead(stream, data, 4); GetBE32(data);})

#define CdxStreamGetBE64(stream) \
        ({cdx_uint8 data[8]; CdxStreamRead(stream, data, 8); GetBE64(data);})

#ifdef __cplusplus
}
#endif

#endif

