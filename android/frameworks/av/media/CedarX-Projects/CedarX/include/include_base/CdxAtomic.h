#ifndef CDX_ATOMIC_H
#define CDX_ATOMIC_H

//#include <CdxTypes.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    volatile long counter;
} cdx_atomic_t;

/*���ü���+ 1 �����ؽ������ֵ*/
static inline long CdxAtomicInc(cdx_atomic_t *ref) 
{
    return __sync_add_and_fetch(&ref->counter, 1);
}

/*���ü���- 1 �����ؽ������ֵ*/
static inline long CdxAtomicDec(cdx_atomic_t *ref)
{
    return __sync_sub_and_fetch(&ref->counter, 1);
}

/*�������ü���Ϊval����������ǰ��ֵ*/
static inline long CdxAtomicSet(cdx_atomic_t *ref, long val)
{
    return __sync_lock_test_and_set(&ref->counter, val);
}

/*��ȡ���ü�����ֵ*/
static inline long CdxAtomicRead(cdx_atomic_t *ref)
{
    return __sync_or_and_fetch(&ref->counter, 0);
}

#ifdef __cplusplus
}
#endif

#endif
