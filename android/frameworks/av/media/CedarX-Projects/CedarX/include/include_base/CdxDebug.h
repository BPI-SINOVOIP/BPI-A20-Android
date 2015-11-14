#ifndef CDX_DEBUG_H
#define CDX_DEBUG_H
#include <sys/types.h>
#include <cutils/log.h>

#define CDX_LOG(level, fmt, arg...) \
    LOG_PRI(level, LOG_TAG,             \
        "<%s:%u>"fmt, strrchr(__FILE__, '/') + 1, __LINE__, ##arg)

#define CDX_LOGV(fmt, arg...)               \
    CDX_LOG(ANDROID_LOG_VERBOSE, fmt, ##arg)

#define CDX_LOGE(fmt, arg...)               \
    CDX_LOG(ANDROID_LOG_ERROR, fmt, ##arg)

#define CDX_LOGI(fmt, arg...)               \
    CDX_LOG(ANDROID_LOG_INFO, fmt, ##arg)

#define CDX_TRACE() \
    CDX_LOGI("<%s:%u> tid(%d)", strrchr(__FILE__, '/') + 1, __LINE__, gettid())

#define CDX_CHECK(e) \
    LOG_ALWAYS_FATAL_IF(                        \
            !(e),                               \
            "<%s:%d>CDX_CHECK(%s) failed.",     \
            strrchr(__FILE__, '/') + 1, __LINE__, #e)             \

#define CDX_BUF_DUMP(buf, len) \
    do { \
        char *_buf = (char *)buf;\
        char str[1024] = {0};\
        unsigned int index = 0, _len;\
        _len = (unsigned int)len;\
        snprintf(str, 1024, ":%d:[", _len);\
        for (index = 0; index < _len; index++)\
        {\
            snprintf(str + strlen(str), 1024, "0x%02hhx ", _buf[index]);\
        }\
        str[strlen(str) - 1] = ']';\
        CDX_LOGI("%s", str);\
    }while (0)

#ifdef __cplusplus
extern "C"
{
#endif

void CdxCallStack(void);


#ifdef __cplusplus
}
#endif

#endif
