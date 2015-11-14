#ifndef __MMC_DEF__
#define __MMC_DEF__

//#define SUNXI_MMCDBG

#ifdef SUNXI_MMCDBG
#define MMCINFO(fmt...)	tick_printf("[mmc]: "fmt)//err and info
#define MMCDBG(fmt...)	tick_printf("[mmc]: "fmt)//dbg
#define MMCMSG(fmt...)	tick_printf(fmt)//data or register and so on
#else
#define MMCINFO(fmt...)	tick_printf("[mmc]: "fmt)
#define MMCDBG(fmt...)
#define MMCMSG(fmt...)
#endif


#define DRIVER_VER  "2014-5-23 10:07:00"


#endif
