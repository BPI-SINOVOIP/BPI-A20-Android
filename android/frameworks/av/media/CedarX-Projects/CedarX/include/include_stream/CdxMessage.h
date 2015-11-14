#ifndef CDX_MESSAGE_H
#define CDX_MESSAGE_H
#include <CdxTypes.h>
#include <CdxLog.h>
#include <CdxMeta.h>

typedef struct CdxMessageS CdxMessageT;
typedef struct CdxHandlerS CdxHandlerT;
typedef struct CdxDeliverS CdxDeliverT;

struct CdxDeliverOpsS
{
    cdx_int32 (*postUS)(CdxDeliverT *, CdxMessageT *, cdx_uint64/*timeUs*/);
};

struct CdxDeliverS
{
    struct CdxDeliverOpsS *ops;
};

CdxDeliverT *CdxDeliverCreate(ngx_pool_t *pool);

cdx_void CdxDeliverDestroy(CdxDeliverT *deliver);

CDX_INTERFACE cdx_int32 CdxDeliverPostUS(CdxDeliverT *deliver, CdxMessageT *msg,
                                        cdx_uint64 timeUs)
{
    CDX_CHECK(deliver);
    CDX_CHECK(deliver->ops);
    CDX_CHECK(deliver->ops->postUS);
    return deliver->ops->postUS(deliver, msg, timeUs);
}

/**
 *目前msg的场景都是串行的，后续有串行的需要再
 *考虑加锁
 */
 
/* sizeof(name) < 32 */
struct CdxMessageOpsS
{
    CdxMessageT *(*incRef)(CdxMessageT *);

    cdx_void (*decRef)(CdxMessageT *);

    CdxMetaT *(*getMeta)(CdxMessageT *);

    cdx_int32 (*what)(CdxMessageT *);
    
    cdx_err (*postUS)(CdxMessageT *, cdx_uint64); /* asynchronous */

    cdx_err (*postToGlobalDeliver)(CdxMessageT *, cdx_uint64);
    
    CdxMessageT *(*dup)(CdxMessageT *);

    cdx_void (*deliver)(CdxMessageT *);
};

struct CdxMessageS
{
    struct CdxMessageOpsS *ops;
};

CDX_INTERFACE CdxMessageT *CdxMessageIncRef(CdxMessageT *msg)
{
    CDX_CHECK(msg);
    CDX_CHECK(msg->ops);
    CDX_CHECK(msg->ops->incRef);
    return msg->ops->incRef(msg);
}

CDX_INTERFACE cdx_void CdxMessageDecRef(CdxMessageT *msg)
{
    CDX_CHECK(msg);
    CDX_CHECK(msg->ops);
    CDX_CHECK(msg->ops->decRef);
    return msg->ops->decRef(msg);
}

CDX_INTERFACE CdxMetaT *CdxMessageGetMeta(CdxMessageT *msg)
{
    CDX_CHECK(msg);
    CDX_CHECK(msg->ops);
    CDX_CHECK(msg->ops->getMeta);
    return msg->ops->getMeta(msg);
}

CDX_INTERFACE cdx_void CdxMessageDeliver(CdxMessageT *msg)
{
    CDX_CHECK(msg);
    CDX_CHECK(msg->ops);
    CDX_CHECK(msg->ops->deliver);
    return msg->ops->deliver(msg);
}

#define CdxMessageFastNotify(what, hdr)                             \
    do                                                          \
    {                                                           \
        CdxMessageT *_notifyMsg = CdxMessageCreate(what, hdr);  \
        CDX_FORCE_CHECK(_notifyMsg);                            \
        CdxMessagePost(_notifyMsg);                             \
    }while (0)                                                  

#define CdxMessageSetInt32(msg, name, val) \
    CdxMetaSetInt32(CdxMessageGetMeta(msg), name, val)

#define CdxMessageFindInt32(msg, name, pVal) \
    CdxMetaFindInt32(CdxMessageGetMeta(msg), name, pVal)

#define CdxMessageSetInt64(msg, name, val) \
    CdxMetaSetInt64(CdxMessageGetMeta(msg), name, val)

#define CdxMessageFindInt64(msg, name, pVal) \
    CdxMetaFindInt64(CdxMessageGetMeta(msg), name, pVal)

#define CdxMessageSetString(msg, name, val) \
    CdxMetaSetString(CdxMessageGetMeta(msg), name, val)

#define CdxMessageFindString(msg, name, pVal) \
    CdxMetaFindString(CdxMessageGetMeta(msg), name, pVal)


#define CdxMessageSetObject(msg, name, val, dtor) \
    CdxMetaSetObject(CdxMessageGetMeta(msg), name, val, dtor)
    
#define CdxMessageFindObject(msg, name, pVal) \
    CdxMetaFindObject(CdxMessageGetMeta(msg), name, (cdx_void **)(pVal))

#define CdxMessageRemoveObject(msg, name) \
    CdxMetaRemoveObject(CdxMessageGetMeta(msg), name)

#define CdxMessageClearMeta(msg) \
    CdxMetaClear(CdxMessageGetMeta(msg))

CDX_INTERFACE cdx_int32 CdxMessageWhat(CdxMessageT *msg)
{
    CDX_CHECK(msg);
    CDX_CHECK(msg->ops);
    CDX_CHECK(msg->ops->what);
    return msg->ops->what(msg);
}

CDX_INTERFACE cdx_err CdxMessagePostUS(CdxMessageT *msg, cdx_uint64 timeOut)
{
    CDX_CHECK(msg);
    CDX_CHECK(msg->ops);
    CDX_CHECK(msg->ops->postUS);
    return msg->ops->postUS(msg, timeOut);
}

CDX_INTERFACE cdx_err CdxMessagePostToGlobalDeliver(CdxMessageT *msg, cdx_uint64 timeOut)
{
    CDX_CHECK(msg);
    CDX_CHECK(msg->ops);
    CDX_CHECK(msg->ops->postToGlobalDeliver);
    return msg->ops->postToGlobalDeliver(msg, timeOut);
}

CDX_INTERFACE CdxMessageT *CdxMessageDup(CdxMessageT *msg)
{
    CDX_CHECK(msg);
    CDX_CHECK(msg->ops);
    CDX_CHECK(msg->ops->dup);
    return msg->ops->dup(msg);
}

#define CdxMessagePost(msg) \
    CdxMessagePostUS(msg, 0)

#ifdef __cplusplus
extern "C" {
#endif

CdxMessageT *CdxMessageCreate(cdx_int32 what, CdxHandlerT *handler, ngx_pool_t *pool);

cdx_void CdxMessageDestroy(CdxMessageT  *msg);

struct CdxHandlerOpsS
{
    cdx_void (*msgReceived)(CdxHandlerT *, CdxMessageT *);
    /**
       *return value:
       *CDX_TRUE, we will destroy the msg
       *CDX_FALSE, do nothing
       */
    cdx_int32 (*postUS)(CdxHandlerT *, CdxMessageT *, cdx_uint64);
};

struct CdxHandlerS
{
    struct CdxHandlerOpsS *ops;
};

// TODO:需要考虑handler生命周期的问题
CDX_INTERFACE cdx_void CdxHandlerMsgReceived(CdxHandlerT *hdr, CdxMessageT *msg)
{
    CDX_CHECK(hdr);
    CDX_CHECK(hdr->ops);
    CDX_CHECK(hdr->ops->msgReceived);
    hdr->ops->msgReceived(hdr, msg);
    return ;
}

CDX_INTERFACE cdx_int32 CdxHandlerPostUS(CdxHandlerT *hdr, CdxMessageT *msg,
                                        cdx_uint64 timeUs)
{
    CDX_CHECK(hdr);
    CDX_CHECK(hdr->ops);
    return hdr->ops->postUS ? hdr->ops->postUS(hdr, msg, timeUs)
                            : CdxMessagePostToGlobalDeliver(msg, timeUs);
}

#ifdef __cplusplus
}
#endif

#endif  // A_MESSAGE_H_
