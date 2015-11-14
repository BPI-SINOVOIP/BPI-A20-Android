
#ifndef __DE_TYPE_H__
#define __DE_TYPE_H__


//**************************************************
//display struct typedef
typedef struct {unsigned char  alpha;unsigned char red;unsigned char green; unsigned char blue; }__color_t;         /* 32-bit (ARGB) color                  */
typedef struct {signed int x; signed int y; unsigned int width; unsigned int height;}__rect_t;          /* rect attrib                          */
typedef struct {signed int x; signed int y;                           }__pos_t;        /* coordinate (x, y)                    */
typedef struct {unsigned int width;      unsigned int height;             }__rectsz_t;          /* rect size                            */



typedef enum
{
    BT601 = 0,
	BT709,
	YCC,
	VXYCC
}__cs_mode_t;

typedef enum __PIXEL_RGBFMT                         /* pixel format(rgb)                                            */
{ 
    PIXEL_MONO_1BPP=0,                              /* only used in normal framebuffer                              */
    PIXEL_MONO_2BPP,
    PIXEL_MONO_4BPP,
    PIXEL_MONO_8BPP,
    PIXEL_COLOR_RGB655,
    PIXEL_COLOR_RGB565,
    PIXEL_COLOR_RGB556,
    PIXEL_COLOR_ARGB1555,
    PIXEL_COLOR_RGBA5551,
    PIXEL_COLOR_RGB0888,                            /* used in normal framebuffer and scaler framebuffer            */
    PIXEL_COLOR_ARGB8888,
}__pixel_rgbfmt_t;
typedef enum __PIXEL_YUVFMT                         /* pixel format(yuv)                                            */
{ 
    PIXEL_YUV444 = 0x10,                            /* only used in scaler framebuffer                              */
    PIXEL_YUV422,
    PIXEL_YUV420,
    PIXEL_YUV411,
    PIXEL_CSIRGB,
    PIXEL_OTHERFMT
}__pixel_yuvfmt_t;
typedef enum __YUV_MODE                             /* Frame buffer data mode definition                            */
{
    YUV_MOD_INTERLEAVED=0,
    YUV_MOD_NON_MB_PLANAR,                          /* �޺��ƽ��ģʽ                                               */
    YUV_MOD_MB_PLANAR,                              /* ���ƽ��ģʽ                                                 */
    YUV_MOD_UV_NON_MB_COMBINED,                     /* �޺��UV���ģʽ                                             */
    YUV_MOD_UV_MB_COMBINED                          /* ���UV���ģʽ                                               */
}__yuv_mod_t;
typedef enum                                        /* yuv seq                                                      */
{
    YUV_SEQ_UYVY=0,                                 /* ����4��ָ�ʺ���yuv422 �� interleaved����Ϸ�ʽ               */
    YUV_SEQ_YUYV,
    YUV_SEQ_VYUY,
    YUV_SEQ_YVYU,
    YUV_SEQ_AYUV=0x10,                              /* ����2��ֻ�ʺ���yuv444 �� interleaved����Ϸ�ʽ               */
    YUV_SEQ_VUYA,
    YUV_SEQ_UVUV=0x20,                              /* ����2��ֻ�ʺ���yuv420 �� uv_combined����Ϸ�ʽ               */
    YUV_SEQ_VUVU,
    YUV_SEQ_OTHRS=0xff,                             /* ������pixelfmt��mod����Ϸ�ʽ                                */
}__yuv_seq_t;
typedef enum
{
    FB_TYPE_RGB=0,
    FB_TYPE_YUV=1
}__fb_type_t;

typedef struct
{
    __fb_type_t                 type;               /* 0 rgb, 1 yuv                                                 */
    union
    {
        struct
        {
            __pixel_rgbfmt_t    pixelfmt;           /* ���صĸ�ʽ                                                   */
            unsigned char               br_swap;            /* blue red color swap flag                                     */
            unsigned char                pixseq;             /* ͼ�����Ĵ洢˳��                                             */
            struct                                  /* ��ɫ��                                                       */
            {       
                void          * addr;               /* ���pixelΪ��bpp��ʽ����ɫ��ָ��Ϊ0                          */
                unsigned int           size;               
            }palette;
        }rgb;
        struct
        {
            __pixel_yuvfmt_t    pixelfmt;           /* ���صĸ�ʽ                                                   */
            __yuv_mod_t         mod;                /* ͼ��ĸ�ʽ                                                   */
            __yuv_seq_t         yuvseq;             /* yuv������˳��                                                */
        }yuv;
    }fmt;
    __cs_mode_t					cs_mode;
}__fb_format_t;

typedef struct __FB                                 /* frame buffer                                                 */
{
    __rectsz_t size;               /* frame buffer�ĳ���                                           */
    void * addr[3];            /* frame buffer�����ݵ�ַ������rgb���ͣ�ֻ��addr[0]��Ч         */
    __fb_format_t fmt;
}FB;



#endif
