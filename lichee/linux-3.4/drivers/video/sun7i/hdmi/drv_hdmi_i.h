
#ifndef  _DRV_HDMI_I_H_
#define  _DRV_HDMI_I_H_
#include <linux/module.h>
#include <asm/uaccess.h>
#include <asm/memory.h>
#include <asm/unistd.h>
#include "asm-generic/int-ll64.h"
#include "linux/kernel.h"
#include "linux/mm.h"
#include "linux/semaphore.h"
#include <linux/vmalloc.h>
#include <linux/fs.h>
#include <linux/dma-mapping.h>
#include <linux/sched.h>   //wake_up_process()
#include <linux/kthread.h> //kthread_create()??kthread_run()
#include <linux/err.h> //IS_ERR()??PTR_ERR()
#include <linux/delay.h>
#include <linux/platform_device.h>
#include <linux/errno.h>
#include <linux/slab.h>
#include <linux/delay.h>
#include <linux/init.h>
#include <linux/dma-mapping.h>
#include <linux/interrupt.h>
#include <linux/clk.h>
#include <linux/cdev.h>
#include <linux/types.h>
#include <linux/module.h>
#include <mach/sys_config.h>
//#include <linux/switch.h>
#include <linux/android_switch.h>

#include <linux/drv_display.h>
#include <linux/drv_hdmi.h>

extern __u32 hdmi_print;
extern __u32 rgb_only;
extern __u32 boot_disp_para;
#if 1
#define OSAL_PRINTF(msg...) do{printk(KERN_WARNING "[HDMI] ");printk(msg);}while(0)
#define __inf(msg...)       do{if(hdmi_print){printk(KERN_WARNING "[HDMI] ");printk(msg);}}while(0)
#define __msg(msg...)       do{if(hdmi_print){printk(KERN_WARNING "[HDMI] file:%s,line:%d:",__FILE__,__LINE__);printk(msg);}}while(0)
#define __wrn(msg...)       do{printk(KERN_WARNING "[HDMI WRN] file:%s,line:%d:    ",__FILE__,__LINE__);printk(msg);}while(0)
#define __here__            do{if(hdmi_print){printk(KERN_WARNING "[HDMI] file:%s,line:%d\n",__FILE__,__LINE__);}}while(0)
#else
#define OSAL_PRINTF(msg...) 
#define __inf(msg...)       
#define __msg(msg...)
#define __wrn(msg...)
#define __here__ 
#endif


__s32 Hdmi_init(void);
__s32 Hdmi_exit(void);
__u32 IsHdcpDriver(void);
__u32 Hdmi_hdcp_enable(__u32 hdcp_en);
__s32 Hdmi_open(void);
__s32 Hdmi_close(void);
__s32 Hdmi_set_display_mode(__disp_tv_mode_t mode);
__s32 Hdmi_mode_support(__disp_tv_mode_t mode);
__s32 Hdmi_get_HPD_status(void);
__s32 Hdmi_Audio_Enable(__u8 mode, __u8 channel);
__s32 Hdmi_Set_Audio_Para(hdmi_audio_t * audio_para);


extern __s32 hdmi_i2c_add_driver(void);
extern __s32 hdmi_i2c_del_driver(void);

#define sys_get_wvalue(n)   (*((volatile __u32 *)(n)))          /* word input */
#define sys_put_wvalue(n,c) (*((volatile __u32 *)(n))  = (c))   /* word output */


#endif
