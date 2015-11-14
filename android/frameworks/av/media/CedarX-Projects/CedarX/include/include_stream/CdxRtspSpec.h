#ifndef CDX_RTSP_SPEC_H
#define CDX_RTSP_SPEC_H

/*
  *  +--------+-----------+---------+------------+----------+
  *  +  header   + video region + extra data +  audio region  + extra data   +
  *  +--------+-----------+---------+------------+----------+
  */
struct CedarXRtspHeaderS
{
    cdx_char tag[8];        /* 'cdxrtsp\0' */
    cdx_uint64 duration;
    cdx_int32 demuxType;      /*asf, ts, rtsp*/
    cdx_int32 disableSeek;
    cdx_void *sftRtspSource;
    cdx_int32 vidStrmNum;
    cdx_int32 hasVideo;
    cdx_int32 audStrmNum;
    cdx_int32 hasAudio;
    cdx_int32 streamNum;
};

struct CedarXRtspVideoRegionS
{
    cdx_int32 codingType; /*enum OMX_VIDEO_CODINGTYPE*/
    cdx_int32 codingSubType;
    cdx_int32 width;
    cdx_int32 height;
    cdx_int32 ptsCorrect;
    cdx_int32 thirdParaOverride;
    cdx_uint32 extraDataLen;
};

struct CedarXRtspAudioRegionS
{
    cdx_int32 codingType;
    cdx_int32 subCodingType;
    cdx_int32 channels;
    cdx_int32 samplePerSecond;
    cdx_uint32 extraDataLen;
};

struct CedarXRtspMetaS
{
    struct CedarXRtspHeaderS header;
    struct CedarXRtspVideoRegionS videoRegion;
    cdx_char *videoExtraData;
    struct CedarXRtspAudioRegionS audioRegion;
    cdx_char *audioExtraData;
};

struct CedarXRtspPktHeadS
{
    cdx_int32 type;
    cdx_uint32 lenght;
    cdx_int32 pts;
};
#define CEDARX_PKT_HEAD_SIZE sizeof(struct CedarXRtspPktHeadS)

#endif
