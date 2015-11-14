/*
**********************************************************************************************************************
*                                                    ePDK
*                                    the Easy Portable/Player Develop Kits
*                                              eMOD Sub-System
*
*                                   (c) Copyright 2007-2009, Steven.ZGJ.China
*                                             All Rights Reserved
*
* Moudle  : display driver
* File    : drv_display.c
*
* By      : William
* Version : v1.0
* Date    : 2008-1-8 9:46:23
**********************************************************************************************************************
*/

#ifndef _DRV_HDMI_H
#define _DRV_HDMI_H

#include "de_type.h"

typedef struct hdmi_audio
{
	__u8    hw_intf;        /* 0:iis  1:spdif 2:pcm */
	__u16	fs_between;     /* fs */
	__u32   sample_rate;    /*sample rate*/  
	__u8    clk_edge;       /* 0:*/
	__u8    ch0_en;         /* 1 */
	__u8    ch1_en;         /* 0 */
	__u8 	ch2_en;         /* 0 */
	__u8 	ch3_en;         /* 0 */
	__u8	word_length;    /* 32 */
	__u8    shift_ctl;      /* 0 */
	__u8    dir_ctl;        /* 0 */
	__u8    ws_pol;
	__u8    just_pol;
}hdmi_audio_t;


/*
************************************************************
* display driver ioctl cmd definition
*
************************************************************
*/

/*define display driver command*/
typedef enum tag_HDMI_CMD
{
    /* command cache on/off                         */
		HDMI_CMD_SET_VIDEO_MOD,
		HDMI_CMD_GET_VIDEO_MOD,
		HDMI_CMD_SET_AUDIO_PARA,
		HDMI_CMD_AUDIO_RESET_NOTIFY,            /*iis reset finish notify    */
		HDMI_CMD_CLOSE,                         /*iis reset finish notify    */
		HDMI_CMD_MOD_SUPPORT,                   /*�ж�ĳһ��hdmiģʽ�Ƿ�֧��*/
		HDMI_CMD_AUDIO_ENABLE,
		HDMI_CMD_GET_HPD_STATUS,
}__hdmi_cmd_t;



#endif  /* _DRV_DISPLAY_H */
