#ifndef CDX_LOG_H
#define CDX_LOG_H
#include <CdxTypes.h>
#include <CdxDebugStream.h>

#ifdef __OS_ANDROID
#include <cutils/log.h>

#if defined(LOG_TAG)
#undef LOG_TAG
#endif
#define LOG_TAG "AwPlayer"

#define CDX_LOG(level, fmt, arg...) \
    LOG_PRI(level, LOG_TAG,             \
        "<%s:%u>"fmt, strrchr(__FILE__, '/') + 1, __LINE__, ##arg)


#ifndef LOG_NDEBUG
#define LOG_NDEBUG 1
#endif
#if LOG_NDEBUG
#define CDX_LOGV(fmt, arg...)
#else
#define CDX_LOGV(fmt, arg...)               \
    CDX_LOG(ANDROID_LOG_VERBOSE, fmt, ##arg)

#endif


#define CDX_LOGD(fmt, arg...)               \
    CDX_LOG(ANDROID_LOG_DEBUG, fmt, ##arg)

#define CDX_LOGI(fmt, arg...)               \
    CDX_LOG(ANDROID_LOG_INFO, fmt, ##arg)

#define CDX_LOGW(fmt, arg...)               \
    CDX_LOG(ANDROID_LOG_WARN, fmt, ##arg)

#define CDX_LOGE(fmt, arg...) \
    CDX_LOG(ANDROID_LOG_ERROR, fmt, ##arg);   \

#define CDX_TRACE() \
    CDX_LOGI("<%s:%u> tid(%d)", strrchr(__FILE__, '/') + 1, __LINE__, gettid())
 
/*check when realease version*/
#define CDX_FORCE_CHECK(e) \
        LOG_ALWAYS_FATAL_IF(                        \
                !(e),                               \
                "<%s:%d>CDX_CHECK(%s) failed.",     \
                strrchr(__FILE__, '/') + 1, __LINE__, #e)      \

#define CDX_TRESPASS() \
        LOG_ALWAYS_FATAL("Should not be here.")

#define CDX_LOG_FATAL(fmt, arg...)                          \
        LOG_ALWAYS_FATAL("<%s:%d>"fmt,                      \
            strrchr(__FILE__, '/') + 1, __LINE__, ##arg)    \
      
#define CDX_ITF_CHECK(base, itf)    \
    CDX_CHECK(base);                \
    CDX_CHECK(base->ops);           \
    CDX_CHECK(base->ops->itf)

#define CDX_LOG_CHECK(e, fmt, arg...)                           \
    LOG_ALWAYS_FATAL_IF(                                        \
            !(e),                                               \
            "<%s:%d>check (%s) failed:"fmt,                     \
            strrchr(__FILE__, '/') + 1, __LINE__, #e, ##arg)    \
 
#ifdef CDX_DEBUG
#define CDX_CHECK(e)                                            \
    LOG_ALWAYS_FATAL_IF(                                        \
            !(e),                                               \
            "<%s:%d>CDX_CHECK(%s) failed.",                     \
            strrchr(__FILE__, '/') + 1, __LINE__, #e)           \

#else
#define CDX_CHECK(condition)
#endif

#else
#include <stdio.h>
#include <assert.h>
#include <string.h>

#define CDX_LOGV(fmt, arg...) \
    printf("V/<%s:%u>"fmt"\n", strrchr(__FILE__, '/') + 1, __LINE__, ##arg)
    
#define CDX_LOGD(fmt, arg...) \
    printf("D/<%s:%u>"fmt"\n", strrchr(__FILE__, '/') + 1, __LINE__, ##arg)

#define CDX_LOGI(fmt, arg...) \
    printf("I/<%s:%u>"fmt"\n", strrchr(__FILE__, '/') + 1, __LINE__, ##arg)

#define CDX_LOGW(fmt, arg...) \
    printf("W/<%s:%u>"fmt"\n", strrchr(__FILE__, '/') + 1, __LINE__, ##arg)

#define CDX_LOGE(fmt, arg...) \
    do { \
        printf("E/<%s:%u>"fmt"\n", strrchr(__FILE__, '/') + 1, __LINE__, ##arg);\
        }while (0)

#define CDX_CHECK(e)                                            \
    do {                                                        \
        if (!(e))                                                 \
        {                                                       \
            CDX_LOGE("check (%s) failed.", #e);                 \
            assert(0);                                          \
        }                                                       \
    } while (0)

#define CDX_LOG_CHECK(e, fmt, arg...)                           \
    do {                                                        \
        if (!(e))                                                 \
        {                                                       \
            CDX_LOGE("check (%s) failed:"fmt, #e, ##arg);       \
            assert(0);                                          \
        }                                                       \
    } while (0)

#define CDX_FORCE_CHECK(e) CDX_CHECK(e)


#endif

#define CDX_BUF_DUMP(buf, len) \
    do { \
        char *_buf = (char *)buf;\
        char str[1024] = {0};\
        unsigned int index = 0, _len;\
        _len = (unsigned int)len;\
        snprintf(str, 1024, ":%d:[", _len);\
        for (index = 0; index < _len; index++)\
        {\
            snprintf(str + strlen(str), 1024 - strlen(str), "0x%02hhx ", _buf[index]);\
        }\
        str[strlen(str) - 1] = ']';\
        CDX_LOGV("%s", str);\
    }while (0)

#endif
