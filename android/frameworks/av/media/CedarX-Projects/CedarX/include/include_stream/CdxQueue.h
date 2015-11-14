#ifndef CDX_QUEUE_H
#define CDX_QUEUE_H
#include <CdxTypes.h>
#include <CdxLog.h>
#include <AwPAlloc.h>

typedef struct CdxQueueS CdxQueueT;
typedef cdx_void * CdxQueueDataT;

struct CdxQueueOpsS
{
    CdxQueueDataT (*pop)(CdxQueueT *);
    cdx_err (*push)(CdxQueueT *, CdxQueueDataT);
    cdx_bool (*empty)(CdxQueueT *);
    
};

struct CdxQueueS
{
    struct CdxQueueOpsS *ops;
};

CDX_INTERFACE CdxQueueDataT CdxQueuePop(CdxQueueT *queue)
{
    CDX_CHECK(queue);
    CDX_CHECK(queue->ops);
    CDX_CHECK(queue->ops->pop);
    return queue->ops->pop(queue);
}

CDX_INTERFACE cdx_err CdxQueuePush(CdxQueueT *queue, CdxQueueDataT data)
{
    CDX_CHECK(queue);
    CDX_CHECK(queue->ops);
    CDX_CHECK(queue->ops->push);
    return queue->ops->push(queue, data);
}


CDX_INTERFACE cdx_bool CdxQueueEmpty(CdxQueueT *queue)
{
    CDX_CHECK(queue);
    CDX_CHECK(queue->ops);
    CDX_CHECK(queue->ops->empty);
    return queue->ops->empty(queue);
}


#ifdef __cplusplus
extern "C"
{
#endif
/*this is a look free queue*/
CdxQueueT *CdxQueueCreate(ngx_pool_t *pool);

cdx_void CdxQueueDestroy(CdxQueueT *queue);

#ifdef __cplusplus
}
#endif

#endif
