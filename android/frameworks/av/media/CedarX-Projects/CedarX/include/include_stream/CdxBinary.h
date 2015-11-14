#ifndef CDX_BINARY_H
#define CDX_BINARY_H
#include <CdxTypes.h>

static inline cdx_uint32 GetU8(cdx_uint8 *data)
{
    return (cdx_uint32)(*data);
}

static inline cdx_uint32 GetLE16(cdx_uint8 *data)
{
    return GetU8(data) | (GetU8(data + 1) << 8);
}

static inline cdx_uint32 GetLE32(cdx_uint8 *data)
{
    return GetLE16(data) | (GetLE16(data + 2) << 16);
}

static inline cdx_uint64 GetLE64(cdx_uint8 *data)
{
    return ((cdx_uint64)GetLE32(data)) | (((cdx_uint64)GetLE32(data + 4)) << 32);
}

static inline cdx_uint16 GetBE16(const cdx_uint8 *data)
{
    return data[0] << 8 | data[1];
}

static inline cdx_uint32 GetBE24(const cdx_uint8 *data)
{
    return data[0] << 16 | GetBE16(&data[1]);
}

static inline cdx_uint32 GetBE32(const cdx_uint8 *data)
{
    return GetBE16(data) << 16 | GetBE16(&data[2]);
}

static inline cdx_uint64 GetBE64(const cdx_uint8 *data)
{
    return (cdx_uint64)(GetBE32(data)) << 32 | GetBE32(&data[4]);
}

#endif
