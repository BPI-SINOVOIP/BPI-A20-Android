/*
 * sunxi Camera Interface  driver
 * Author: raymonxiu
 */
#include <linux/module.h>
#include <linux/errno.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/sched.h>
#include <linux/slab.h>
#include <linux/version.h>
#include <linux/mutex.h>
#include <linux/videodev2.h>
#include <linux/clk.h>
#include <linux/delay.h>

#if LINUX_VERSION_CODE >= KERNEL_VERSION(2, 6, 20)
#include <linux/freezer.h>
#endif

#include <media/v4l2-device.h>
#include <media/v4l2-ioctl.h>
#include <media/v4l2-common.h>
#include <linux/io.h>
#include <linux/platform_device.h>
#include <linux/interrupt.h>
#include <linux/i2c.h>
#include <media/v4l2-subdev.h>
#include <media/videobuf-dma-contig.h>
#include <linux/moduleparam.h>

#include <linux/gpio.h>

//#include <mach/gpio_v2.h>
//#include <mach/script_v2.h>
#include <mach/sys_config.h>
#include <mach/clock.h>
#include <mach/irqs.h>
#include <linux/regulator/consumer.h>

#include "../include/sunxi_csi_core.h"
#include "../include/sunxi_dev_csi.h"
#include "sunxi_csi_reg.h"

#define CSI_MAJOR_VERSION 1
#define CSI_MINOR_VERSION 0
#define CSI_RELEASE 0
#define CSI_VERSION \
	KERNEL_VERSION(CSI_MAJOR_VERSION, CSI_MINOR_VERSION, CSI_RELEASE)
#define CSI_MODULE_NAME "sunxi_csi0"

//#define USE_DMA_CONTIG
//#define AJUST_DRAM_PRIORITY

#define REGS_pBASE					(0x01C00000)	 	      // register base addr
#define SDRAM_REGS_pBASE    (REGS_pBASE + 0x01000)    // SDRAM Controller

#define NUM_INPUTS 2
#define CSI_OUT_RATE      (24*1000*1000)
#define CSI_ISP_RATE			(80*1000*1000)
#define CSI_MAX_FRAME_MEM (32*1024*1024)
//#define TWI_NO		 (1)

#define MIN_WIDTH  (32)
#define MIN_HEIGHT (32)
#define MAX_WIDTH  (4096)
#define MAX_HEIGHT (4096)
#define CAPTURE_FRAME 1

//add by heyihang.Jan 24, 2013
extern void camera_export_info(char *module_name, int *i2c_addr, int index);

static unsigned video_nr = 0;
static unsigned first_flag = 0;


static char ccm[I2C_NAME_SIZE] = "";
static uint i2c_addr = 0xff;

static char ccm_b[I2C_NAME_SIZE] = "";
static uint i2c_addr_b = 0xff;

long unsigned int sec,usec;
static struct ccm_config ccm_cfg[NUM_INPUTS] = {
	{
		.i2c_addr = 0xff,
	},
	{
		.i2c_addr = 0xff,
	},
};

module_param_string(ccm, ccm, sizeof(ccm), S_IRUGO|S_IWUSR);
module_param(i2c_addr,uint, S_IRUGO|S_IWUSR);
module_param_string(ccm_b, ccm_b, sizeof(ccm_b), S_IRUGO|S_IWUSR);
module_param(i2c_addr_b,uint, S_IRUGO|S_IWUSR);

static struct i2c_board_info  dev_sensor[] =  {
	{
		//I2C_BOARD_INFO(ccm, i2c_addr),
		.platform_data	= NULL,
	},
	{
		//I2C_BOARD_INFO(ccm, i2c_addr),
		.platform_data	= NULL,
	},
};

//ccm support format
static struct csi_fmt formats[] = {
	//8bit
	{
		.name     		= "planar YUV 422",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_YUYV8_2X8,//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YUV422P,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422,
		.output_fmt		= CSI_PLANAR_YUV422,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 3,
	},
	{
		.name     		= "planar YUV 420",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_YUYV8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YUV420,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422,
		.output_fmt		= CSI_PLANAR_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 3,
	},
	{
		.name     		= "planar YVU 420",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_YUYV8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YVU420,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422,
		.output_fmt		= CSI_PLANAR_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 3,
	},
	{
		.name     		= "planar YUV 422 UV combined",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_YUYV8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_NV16,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422,
		.output_fmt		= CSI_UV_CB_YUV422,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 2,
	},
	{
		.name     		= "planar YUV 420 UV combined",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_YUYV8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_NV12,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422,
		.output_fmt		= CSI_UV_CB_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 2,
	},
	{
		.name     		= "planar YUV 422 VU combined",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_YUYV8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_NV61,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422,
		.output_fmt		= CSI_UV_CB_YUV422,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 2,
	},
	{
		.name     		= "planar YUV 420 VU combined",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_YUYV8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_NV21,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422,
		.output_fmt		= CSI_UV_CB_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 2,
	},
	{
		.name     		= "MB YUV420",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_YUYV8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_HM12,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422,
		.output_fmt		= CSI_MB_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 2,
	},
	{
		.name     		= "RAW Bayer",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_SBGGR8_1X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_SBGGR8,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_RAW,
		.output_fmt		= CSI_PASS_THROUTH,
		.csi_field		= CSI_ODD,
		.depth    		= 8,
		.planes_cnt		= 1,
	},
//	{
//		.name     		= "planar RGB242",
//		.csi_if				= CSI_IF_HV8,
//		.ccm_fmt			= V4L2_PIX_FMT_SBGGR8,	
//		.fourcc   		= V4L2_PIX_FMT_RGB32,		//can't find the appropriate format in V4L2 define,use this temporarily
//		.field				= V4L2_FIELD_NONE,
//		.input_fmt		= CSI_BAYER,
//		.output_fmt		= CSI_PLANAR_RGB242,
//		.csi_field		= CSI_ODD,
//		.depth    		= 8,
//		.planes_cnt		= 3,
//	},
	{
		.name     		= "YUV422 YUYV",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_YUYV8_2X8,//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YUYV,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_RAW,
		.output_fmt		= CSI_PASS_THROUTH,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 1,
	},
	{
		.name     		= "YUV422 YVYU",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_YVYU8_2X8,//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YVYU,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_RAW,
		.output_fmt		= CSI_PASS_THROUTH,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 1,
	},
	{
		.name     		= "YUV422 UYVY",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_UYVY,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_RAW,
		.output_fmt		= CSI_PASS_THROUTH,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 1,
	},
	{
		.name     		= "YUV422 VYUY",
		.csi_if				= CSI_IF_HV8,
		.ccm_fmt			= V4L2_MBUS_FMT_VYUY8_2X8,//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_VYUY,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_RAW,
		.output_fmt		= CSI_PASS_THROUTH,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 1,
	},
	//16bit
	{
		.name     		= "planar YUV 422",
		.csi_if				= CSI_IF_CCIR656_16,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YUV422P,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422_16,
		.output_fmt		= CSI_PLANAR_YUV422,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 3,
	},
	{
		.name     		= "planar YUV 420",
		.csi_if				= CSI_IF_CCIR656_16,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YUV420,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422_16,
		.output_fmt		= CSI_PLANAR_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 3,
	},
	{
		.name     		= "planar YVU 420",
		.csi_if				= CSI_IF_CCIR656_16,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YVU420,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422_16,
		.output_fmt		= CSI_PLANAR_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 3,
	},
	{
		.name     		= "planar YUV 422 UV combined",
		.csi_if				= CSI_IF_CCIR656_16,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_NV16,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422_16,
		.output_fmt		= CSI_UV_CB_YUV422,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 2,
	},
	{
		.name     		= "planar YUV 420 UV combined",
		.csi_if				= CSI_IF_CCIR656_16,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_NV12,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422_16,
		.output_fmt		= CSI_UV_CB_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 2,
	},
	{
		.name     		= "MB YUV420",
		.csi_if				= CSI_IF_CCIR656_16,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_HM12,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV422_16,
		.output_fmt		= CSI_MB_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 2,
	},
	//24bit
	{
		.name     		= "planar YUV 422",
		.csi_if				= CSI_IF_HV24,
		.ccm_fmt			= V4L2_MBUS_FMT_YUV8_1X24,//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YUV422P,
		.field				= V4L2_FIELD_NONE,
		.input_fmt		= CSI_YUV444,
		.output_fmt		= CSI_FIELD_UV_CB_YUV444_YUV422,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 3,
	},
	{
		.name     		= "planar YUV 422",
		.csi_if				= CSI_IF_HV24,
		.ccm_fmt			= V4L2_MBUS_FMT_YUV8_1X24,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YUV422P,
		.field				= V4L2_FIELD_INTERLACED,
		.input_fmt		= CSI_YUV444,
		.output_fmt		= CSI_FRAME_UV_CB_YUV444_YUV422,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 3,
	},
	//BT656 8bit
	{
		.name     		= "planar YUV 422",
		.csi_if				= CSI_IF_CCIR656,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YUV422P,
		.field				= V4L2_FIELD_INTERLACED,
		.input_fmt		= CSI_CCIR656,
		.output_fmt		= CSI_FRAME_PLANAR_YUV422,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 3,
	},
	{
		.name     		= "planar YUV 420",
		.csi_if				= CSI_IF_CCIR656,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YUV420,
		.field				= V4L2_FIELD_INTERLACED,
		.input_fmt		= CSI_CCIR656,
		.output_fmt		= CSI_FRAME_PLANAR_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 3,
	},
	{
		.name     		= "planar YVU 420",
		.csi_if				= CSI_IF_CCIR656,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_YVU420,
		.field				= V4L2_FIELD_INTERLACED,
		.input_fmt		= CSI_CCIR656,
		.output_fmt		= CSI_FRAME_PLANAR_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 3,
	},
	{
		.name     		= "planar YUV 422 UV combined",
		.csi_if				= CSI_IF_CCIR656,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_NV16,
		.field				= V4L2_FIELD_INTERLACED,
		.input_fmt		= CSI_CCIR656,
		.output_fmt		= CSI_FRAME_UV_CB_YUV422,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 2,
	},
	{
		.name     		= "planar YUV 420 UV combined",
		.csi_if				= CSI_IF_CCIR656,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_NV12,
		.field				= V4L2_FIELD_INTERLACED,
		.input_fmt		= CSI_CCIR656,
		.output_fmt		= CSI_FRAME_UV_CB_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 2,
	},
	{
		.name     		= "planar YUV 422 VU combined",
		.csi_if				= CSI_IF_CCIR656,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_NV61,
		.field				= V4L2_FIELD_INTERLACED,
		.input_fmt		= CSI_CCIR656,
		.output_fmt		= CSI_FRAME_UV_CB_YUV422,
		.csi_field		= CSI_ODD,
		.depth    		= 16,
		.planes_cnt		= 2,
	},
	{
		.name     		= "planar YUV 420 VU combined",
		.csi_if				= CSI_IF_CCIR656,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_NV21,
		.field				= V4L2_FIELD_INTERLACED,
		.input_fmt		= CSI_CCIR656,
		.output_fmt		= CSI_FRAME_UV_CB_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 2,
	},
	{
		.name     		= "MB YUV420",
		.csi_if				= CSI_IF_CCIR656,
		.ccm_fmt			= V4L2_MBUS_FMT_UYVY8_2X8,	//linux-3.0
		.fourcc   		= V4L2_PIX_FMT_HM12,
		.field				= V4L2_FIELD_INTERLACED,
		.input_fmt		= CSI_CCIR656,
		.output_fmt		= CSI_FRAME_MB_YUV420,
		.csi_field		= CSI_ODD,
		.depth    		= 12,
		.planes_cnt		= 2,
	},	
};

//FPGA
unsigned gpio_request_ex(char *main_name, const char *sub_name) {
	return 0;
}

static struct csi_fmt *get_format(struct csi_dev *dev, struct v4l2_format *f)
{
	struct csi_fmt *fmt;
	struct v4l2_mbus_framefmt ccm_fmt;//linux-3.0
	unsigned int k;
	int ret;
	
	for (k = 0; k < ARRAY_SIZE(formats); k++) {
		fmt = &formats[k];
		if (fmt->csi_if == dev->interface) {
			csi_dbg(0,"interface ok dev->interface = %d!\n",dev->interface);
			if (fmt->fourcc == f->fmt.pix.pixelformat) {
				csi_dbg(0,"fourcc fmt ok!\n");
				ccm_fmt.code = fmt->ccm_fmt;//linux-3.0
				ccm_fmt.field = f->fmt.pix.field;
				ccm_fmt.width = f->fmt.pix.width;//linux-3.0
				ccm_fmt.height = f->fmt.pix.height;//linux-3.0		
				ret = v4l2_subdev_call(dev->sd,video,try_mbus_fmt,&ccm_fmt);//linux-3.0
				if (ret < 0) {
					csi_err("v4l2 sub device try_fmt error!\n");
					return NULL;
				}
				//info got from module
				f->fmt.pix.field = ccm_fmt.field;//linux-3.0
				f->fmt.pix.width = ccm_fmt.width;//linux-3.0
				f->fmt.pix.height = ccm_fmt.height;//linux-3.0
//				f->fmt.pix.bytesperline = ccm_fmt.fmt.pix.bytesperline;//linux-3.0
//				f->fmt.pix.sizeimage = ccm_fmt.fmt.pix.sizeimage;//linux-3.0
				
				if(ccm_fmt.field == fmt->field) {
					csi_dbg(0,"field ok!\n");
					break;
				}
				else
					continue;
			}
		}
	}

	if (k == ARRAY_SIZE(formats)) {
		return NULL;
	}	

	return &formats[k];
};

static inline void csi_set_addr(struct csi_dev *dev,struct csi_buffer *buffer)
{
	
	struct csi_buffer *buf = buffer;	
	dma_addr_t addr_org;
	
	csi_dbg(3,"buf ptr=%p\n",buf);

    //modified by zenglingying.Jan 17, 2013
	addr_org = videobuf_to_dma_contig((struct videobuf_buffer *)buf);
	if( (addr_org&0x40000000)!=0 )
	{
		addr_org-=0x40000000;
		csi_dbg(3,"csi recal mem_addr=%p\n",addr_org);
	}


	if(dev->fmt->input_fmt==CSI_RAW){
		dev->csi_buf_addr.y  = addr_org;
		dev->csi_buf_addr.cb = addr_org;
		dev->csi_buf_addr.cr = addr_org;
	
	}else if(dev->fmt->input_fmt==CSI_BAYER){
		//really rare here
		dev->csi_buf_addr.cb = addr_org;//for G channel
		dev->csi_buf_addr.y  = addr_org + dev->width*dev->height*1/2;//for B channel
		dev->csi_buf_addr.cr = addr_org + dev->width*dev->height*3/4;//for R channel

	}else if(dev->fmt->input_fmt==CSI_CCIR656){
	//TODO:
		switch (dev->fmt->output_fmt) {
			case CSI_FRAME_PLANAR_YUV422:
				dev->csi_buf_addr.y  = addr_org;
				dev->csi_buf_addr.cb = addr_org + dev->width*dev->height;
				dev->csi_buf_addr.cr = addr_org + dev->width*dev->height*3/2;
				break;

			case CSI_FRAME_PLANAR_YUV420:
				dev->csi_buf_addr.y  = addr_org;
				dev->csi_buf_addr.cb = addr_org + dev->width*dev->height;
				dev->csi_buf_addr.cr = addr_org + dev->width*dev->height*5/4;
				break;
				
			case CSI_FRAME_UV_CB_YUV422:
			case CSI_FRAME_UV_CB_YUV420:
			case CSI_FRAME_MB_YUV422:
			case CSI_FRAME_MB_YUV420:
				dev->csi_buf_addr.y  = addr_org;
				dev->csi_buf_addr.cb = addr_org + dev->width*dev->height;
				dev->csi_buf_addr.cr = addr_org + dev->width*dev->height;
				break;

			default:
				break;
		}
	}else if(dev->fmt->input_fmt==CSI_YUV422){
		
		switch (dev->fmt->output_fmt) {
			case CSI_PLANAR_YUV422:
				dev->csi_buf_addr.y  = addr_org;
				dev->csi_buf_addr.cb = addr_org + dev->width*dev->height;
				dev->csi_buf_addr.cr = addr_org + dev->width*dev->height*3/2;
				break;

			case CSI_PLANAR_YUV420:
				dev->csi_buf_addr.y  = addr_org;
				dev->csi_buf_addr.cb = addr_org + dev->width*dev->height;
				dev->csi_buf_addr.cr = addr_org + dev->width*dev->height*5/4;
				break;
				
			case CSI_UV_CB_YUV422:
			case CSI_UV_CB_YUV420:
			case CSI_MB_YUV422:
			case CSI_MB_YUV420:
				dev->csi_buf_addr.y  = addr_org;
				dev->csi_buf_addr.cb = addr_org + dev->width*dev->height;
				dev->csi_buf_addr.cr = addr_org + dev->width*dev->height;
				break;

			default:
				break;
		}
	}else if(dev->fmt->input_fmt==CSI_YUV422_16){
		//TODO
	}
	
	bsp_csi_set_buffer_address(dev, CSI_BUF_0_A, dev->csi_buf_addr.y);
	bsp_csi_set_buffer_address(dev, CSI_BUF_0_B, dev->csi_buf_addr.y);
	bsp_csi_set_buffer_address(dev, CSI_BUF_1_A, dev->csi_buf_addr.cb);
	bsp_csi_set_buffer_address(dev, CSI_BUF_1_B, dev->csi_buf_addr.cb);
	bsp_csi_set_buffer_address(dev, CSI_BUF_2_A, dev->csi_buf_addr.cr);
	bsp_csi_set_buffer_address(dev, CSI_BUF_2_B, dev->csi_buf_addr.cr);

	csi_dbg(3,"csi_buf_addr_y=%x\n",  dev->csi_buf_addr.y);
	csi_dbg(3,"csi_buf_addr_cb=%x\n", dev->csi_buf_addr.cb);
	csi_dbg(3,"csi_buf_addr_cr=%x\n", dev->csi_buf_addr.cr);
	
}

static int csi_clk_get(struct csi_dev *dev)
{
	int ret;

	dev->csi_ahb_clk=clk_get(NULL, CLK_AHB_CSI0);//"ahb_csi0"
	if (dev->csi_ahb_clk == NULL || IS_ERR(dev->csi_ahb_clk)) {
                csi_err("get csi1 ahb clk error!\n");	
		return -1;
    }
	
    // for csi mclk output
	dev->csi_module_clk=clk_get(NULL, CLK_MOD_CSI0); //"csi0"
	if(dev->csi_module_clk == NULL || IS_ERR(dev->csi_module_clk)) {
                csi_err("get csi1 module clk error!\n");	
		return -1;
    }
    
    dev->csi_dram_clk = clk_get(NULL, CLK_DRAM_CSI0);//"dram_csi0"
	if (dev->csi_dram_clk == NULL || IS_ERR(dev->csi_dram_clk)) {
                csi_err("get csi0 dram clk error!\n");
		return -1;
    }
    
    //add by heyihang.Jan 16, 2013
    #if 1
	dev->csi_isp_src_clk=clk_get(NULL,CLK_SYS_PLL3);//"video_pll0"
	if (dev->csi_isp_src_clk == NULL || IS_ERR(dev->csi_isp_src_clk)) {
                csi_err("get csi_isp source clk error!\n");	
		return -1;
    }
    
	dev->csi_isp_clk=clk_get(NULL,CLK_MOD_CSIISP);//"csi_isp"
	if(dev->csi_isp_clk == NULL || IS_ERR(dev->csi_isp_clk)) {
                csi_err("get csi/isp sclk error!\n");	
		return -1;
    }

	ret = clk_set_parent(dev->csi_isp_clk, dev->csi_isp_src_clk);
	if (ret != 0) {
                csi_err(" csi_isp set parent failed \n");
                return -1;
    }
    
    clk_put(dev->csi_isp_src_clk);
	
    ret = clk_set_rate(dev->csi_isp_clk, CSI_ISP_RATE);
	if (ret == -1) {
                csi_err("set csi_isp clock error\n");
		return -1;
   	} 
    #endif
    
	return 0;
}

static int csi_clk_out_set(struct csi_dev *dev)
{
	int ret;
	
	if(dev->ccm_info->mclk==24000000 || dev->ccm_info->mclk==12000000 || dev->ccm_info->mclk==6000000)
	{
		dev->csi_clk_src=clk_get(NULL,CLK_SYS_HOSC);//"hosc"
		if ((dev->csi_clk_src == NULL) || IS_ERR(dev->csi_clk_src)) {
	                csi_err("get csi0 hosc source clk error!\n");	
			return -1;
	  }
	}
	else
	{
		dev->csi_clk_src=clk_get(NULL,CLK_SYS_PLL7);//"video_pll1"
		if ((dev->csi_clk_src == NULL) || IS_ERR(dev->csi_clk_src)) {
	                csi_err("get csi0 video pll1 source clk error!\n");	
			return -1;
	  }
	}  
	
	ret = clk_set_parent(dev->csi_module_clk, dev->csi_clk_src);
	if (ret != 0) {
	  csi_err(" csi set parent failed \n");
	  return -1;
	}
	   
	clk_put(dev->csi_clk_src);
		
	ret = clk_set_rate(dev->csi_module_clk, dev->ccm_info->mclk);
	if (ret != 0) {
		csi_err("set csi0 module clock error\n");
		return -1;
	}

	return 0;
}

static void csi_reset_enable(struct csi_dev *dev)
{
	csi_dbg(2,"csi_reset_enable\n");

	clk_reset(dev->csi_isp_clk, AW_CCU_CLK_RESET);
	clk_reset(dev->csi_module_clk, AW_CCU_CLK_RESET);
}

static void csi_reset_disable(struct csi_dev *dev)
{
	csi_dbg(2,"csi_reset_disable\n");

	clk_reset(dev->csi_isp_clk, AW_CCU_CLK_NRESET);
	clk_reset(dev->csi_module_clk, AW_CCU_CLK_NRESET);
}

static int csi_clk_enable(struct csi_dev *dev)
{
	csi_dbg(2,"csi_clk_enable\n");
    
	clk_enable(dev->csi_ahb_clk);
	clk_enable(dev->csi_dram_clk);
	clk_enable(dev->csi_isp_clk);
	
	return 0;
}

static int csi_clk_disable(struct csi_dev *dev)
{
	csi_dbg(2,"csi_clk_disable\n");
	clk_disable(dev->csi_ahb_clk);
	clk_disable(dev->csi_dram_clk);
    
	clk_disable(dev->csi_isp_clk);
    
	return 0;
}

static int csi_clk_release(struct csi_dev *dev)
{
	csi_dbg(2,"csi_clk_release\n");
	clk_put(dev->csi_ahb_clk);        
    dev->csi_ahb_clk = NULL;

	clk_put(dev->csi_module_clk);        
    dev->csi_module_clk = NULL;

	clk_put(dev->csi_dram_clk);        
    dev->csi_dram_clk = NULL;
    
	clk_put(dev->csi_isp_clk);        
    dev->csi_isp_clk = NULL;

	return 0;
}

static int inline csi_is_generating(struct csi_dev *dev)
{
	return test_bit(0, &dev->generating);
}

static void inline csi_start_generating(struct csi_dev *dev)
{
	 set_bit(0, &dev->generating);
	 return;
}	

static void inline csi_stop_generating(struct csi_dev *dev)
{
	 first_flag = 0;
	 clear_bit(0, &dev->generating);
	 return;
}

static int update_ccm_info(struct csi_dev *dev , struct ccm_config *ccm_cfg)
{
   dev->sd = ccm_cfg->sd;
   dev->ccm_info = &ccm_cfg->ccm_info;
   dev->interface = ccm_cfg->interface;
	 dev->vflip = ccm_cfg->vflip;
	 dev->hflip = ccm_cfg->hflip;
	 dev->flash_pol = ccm_cfg->flash_pol;
	 dev->iovdd = ccm_cfg->iovdd;
	 dev->avdd = ccm_cfg->avdd;
	 dev->dvdd = ccm_cfg->dvdd;
	 dev->vol_iovdd = ccm_cfg->vol_iovdd;
	 dev->vol_avdd =  ccm_cfg->vol_avdd;
	 dev->vol_dvdd =  ccm_cfg->vol_dvdd;
	 dev->reset_io    = ccm_cfg->reset_io;    
	 dev->standby_io  = ccm_cfg->standby_io;  
	 dev->power_io    = ccm_cfg->power_io;    
	 dev->flash_io    = ccm_cfg->flash_io;    
	 dev->af_power_io = ccm_cfg->af_power_io; 
	
	 return v4l2_subdev_call(dev->sd,core,ioctl,CSI_SUBDEV_CMD_SET_INFO,dev->ccm_info);
}

static irqreturn_t csi_isr(int irq, void *priv)
{
	struct csi_buffer *buf;	
	struct csi_dev *dev = (struct csi_dev *)priv;
	struct csi_dmaqueue *dma_q = &dev->vidq;
	__csi_int_status_t status;

	csi_dbg(3,"csi_isr\n");
	
	spin_lock(&dev->slock);
	bsp_csi_int_get_status(dev, &status);
	csi_dbg(3,"status vsync = %d, framedone = %d, capdone = %d\n",status.vsync_trig,status.frame_done,status.capture_done);
	if (dev->capture_mode == V4L2_MODE_IMAGE) {	
		bsp_csi_int_disable(dev,CSI_INT_VSYNC_TRIG);
		bsp_csi_int_disable(dev,CSI_INT_CAPTURE_DONE);
		if (first_flag < CAPTURE_FRAME) {
			csi_print("frame %d\n",first_flag+1);
			if (first_flag == CAPTURE_FRAME-1) {
				csi_print("single capture start!\n");	
				bsp_csi_capture_video_stop(dev);
				bsp_csi_capture_picture(dev);
			}
			first_flag++;
			goto unlock;
		}
		csi_print("capture image mode!\n");	
		buf = list_entry(dma_q->active.next,struct csi_buffer, vb.queue);
		csi_dbg(3,"buf ptr=%p\n",buf);
		list_del(&buf->vb.queue);
		buf->vb.state = VIDEOBUF_DONE;
		wake_up(&buf->vb.done);
		goto unlock;	
	} else {
		bsp_csi_int_disable(dev,CSI_INT_FRAME_DONE);
		if (first_flag == 0) {
			first_flag++;
			csi_print("capture video mode!\n");
			goto set_next_addr;
		}
		
		if (first_flag == 1) {
			first_flag++;
			csi_print("capture video first frame done!\n");
		}
		
		if (list_empty(&dma_q->active)) {		
			csi_err("No active queue to serve\n");		
			goto unlock;	
		}
		
		buf = list_entry(dma_q->active.next,struct csi_buffer, vb.queue);
		csi_dbg(3,"buf ptr=%p\n",buf);
	
		/* Nobody is waiting on this buffer*/	
	
		if (!waitqueue_active(&buf->vb.done)) {
			csi_dbg(1," Nobody is waiting on this buffer,buf = 0x%p\n",buf);					
		}
		
		list_del(&buf->vb.queue);
	
		do_gettimeofday(&buf->vb.ts);
		buf->vb.field_count++;
#if DBG_EN == 1		
		csi_dbg(1,"frame interval = %ld\n",buf->vb.ts.tv_sec*1000000+buf->vb.ts.tv_usec - (sec*1000000+usec));
		sec = buf->vb.ts.tv_sec;
		usec = buf->vb.ts.tv_usec;
#endif
		dev->ms += jiffies_to_msecs(jiffies - dev->jiffies);
		dev->jiffies = jiffies;
	
		buf->vb.state = VIDEOBUF_DONE;
		wake_up(&buf->vb.done);
		
		//judge if the frame queue has been written to the last
		if (list_empty(&dma_q->active)) {		
			csi_dbg(1,"No more free frame\n");		
			goto unlock;	
		}
		
		if ((&dma_q->active) == dma_q->active.next->next) {
			csi_dbg(1,"No more free frame on next time\n");		
			goto unlock;	
		}
	}
	
set_next_addr:	
	buf = list_entry(dma_q->active.next->next,struct csi_buffer, vb.queue);
	csi_set_addr(dev,buf);

unlock:
	bsp_csi_int_get_status(dev, &status);
	if((status.buf_0_overflow) || (status.buf_1_overflow) || (status.buf_2_overflow) || (status.hblank_overflow))
	{
		if((status.buf_0_overflow) || (status.buf_1_overflow) || (status.buf_2_overflow)) {
			bsp_csi_int_clear_status(dev,CSI_INT_BUF_0_OVERFLOW);
			bsp_csi_int_clear_status(dev,CSI_INT_BUF_1_OVERFLOW);
			bsp_csi_int_clear_status(dev,CSI_INT_BUF_2_OVERFLOW);
			csi_err("fifo overflow\n");
		}
		
		if(status.hblank_overflow) {
			bsp_csi_int_clear_status(dev,CSI_INT_HBLANK_OVERFLOW);
			csi_err("hblank overflow\n");
		}
		csi_err("reset csi module\n");
		bsp_csi_close(dev);
		bsp_csi_open(dev);
	}
		
	spin_unlock(&dev->slock);
	
	if (dev->capture_mode == V4L2_MODE_IMAGE) {	
		bsp_csi_int_clear_status(dev,CSI_INT_VSYNC_TRIG);
		bsp_csi_int_clear_status(dev,CSI_INT_CAPTURE_DONE);
		bsp_csi_int_enable(dev,CSI_INT_VSYNC_TRIG);
		bsp_csi_int_enable(dev,CSI_INT_CAPTURE_DONE);
	} else {
		bsp_csi_int_clear_status(dev,CSI_INT_FRAME_DONE);
		bsp_csi_int_enable(dev,CSI_INT_FRAME_DONE);
	}
	
	return IRQ_HANDLED;
}

/*
 * Videobuf operations
 */
static int buffer_setup(struct videobuf_queue *vq, unsigned int *count, unsigned int *size)
{
	struct csi_dev *dev = vq->priv_data;
	int buf_max_flag = 0;
	
	csi_dbg(1,"buffer_setup\n");
	
	if(dev->fmt->input_fmt == CSI_RAW)
	{
		switch(dev->fmt->fourcc) {
			case 	V4L2_PIX_FMT_YUYV:
			case	V4L2_PIX_FMT_YVYU:
			case	V4L2_PIX_FMT_UYVY:
			case	V4L2_PIX_FMT_VYUY:
				*size = dev->width * dev->height * 2;
				break;
			default:
				*size = dev->width * dev->height;
				break;
		}
	}
	else if(dev->fmt->input_fmt == CSI_BAYER)
	{
		*size = dev->width * dev->height;
	}
	else if(dev->fmt->input_fmt == CSI_CCIR656)
	{
		//TODO
		switch (dev->fmt->output_fmt) {
			case CSI_FRAME_PLANAR_YUV422:
			case CSI_FRAME_UV_CB_YUV422:
			case CSI_FRAME_MB_YUV422:
				*size = dev->width * dev->height * 2;
				break;
			case CSI_FRAME_PLANAR_YUV420:
			case CSI_FRAME_UV_CB_YUV420:
			case CSI_FRAME_MB_YUV420:
				*size = dev->width * dev->height * 3/2;
				break;
				break;
			default:
				break;
		}
	}
	else if(dev->fmt->input_fmt == CSI_YUV422)
	{
		switch (dev->fmt->output_fmt) {
			case 	CSI_PLANAR_YUV422:
			case	CSI_UV_CB_YUV422:
			case 	CSI_MB_YUV422:
				*size = dev->width * dev->height * 2;
				break;
				
			case CSI_PLANAR_YUV420:
			case CSI_UV_CB_YUV420:
			case CSI_MB_YUV420:
				*size = dev->width * dev->height * 3/2;
				break;

			default:
				*size = dev->width * dev->height * 2;
				break;
		}
	}
	else if(dev->fmt->input_fmt==CSI_YUV422_16){
		//TODO
	}
	else
	{
		*size = dev->width * dev->height * 2;	
	}
	
	dev->frame_size = *size;
	
	while (*size * *count > CSI_MAX_FRAME_MEM) {
		(*count)--;
		buf_max_flag = 1;
		if(*count == 0)
			csi_err("one buffer size larger than max frame memory! buffer count = %d\n,",*count);
	}	
	
	if(buf_max_flag == 0) {
		if(dev->capture_mode == V4L2_MODE_IMAGE) {
			if (*count != 1) {
				*count = 1;
				csi_err("buffer count is set to 1 in image capture mode\n");
			}
		} else {
			if (*count < 3) {
				*count = 3;
				csi_err("buffer count is invalid, set to 3 in video capture\n");
			}
		}
	}

	csi_print("%s, buffer count=%d, size=%d\n", __func__,*count, *size);

	return 0;
}

static void free_buffer(struct videobuf_queue *vq, struct csi_buffer *buf)
{
	csi_dbg(1,"%s, state: %i\n", __func__, buf->vb.state);

#ifdef USE_DMA_CONTIG	
	videobuf_dma_contig_free(vq, &buf->vb);
#endif
	
	csi_dbg(1,"free_buffer: freed\n");
	
	buf->vb.state = VIDEOBUF_NEEDS_INIT;
}

static int buffer_prepare(struct videobuf_queue *vq, struct videobuf_buffer *vb,
						  enum v4l2_field field)
{
	struct csi_dev *dev = vq->priv_data;
	struct csi_buffer *buf = container_of(vb, struct csi_buffer, vb);
	int rc;

	csi_dbg(1,"buffer_prepare\n");

	BUG_ON(NULL == dev->fmt);

	if (dev->width  < MIN_WIDTH || dev->width  > MAX_WIDTH ||
	    dev->height < MIN_HEIGHT || dev->height > MAX_HEIGHT) {
		return -EINVAL;
	}
	
	buf->vb.size = dev->frame_size;			
	
	if (0 != buf->vb.baddr && buf->vb.bsize < buf->vb.size) {
		return -EINVAL;
	}

	/* These properties only change when queue is idle, see s_fmt */
	buf->fmt       = dev->fmt;
	buf->vb.width  = dev->width;
	buf->vb.height = dev->height;
	buf->vb.field  = field;

	if (VIDEOBUF_NEEDS_INIT == buf->vb.state) {
		rc = videobuf_iolock(vq, &buf->vb, NULL);
		if (rc < 0) {
			goto fail;
		}
	}

	vb->boff= videobuf_to_dma_contig(vb);
	buf->vb.state = VIDEOBUF_PREPARED;

	return 0;

fail:
	free_buffer(vq, buf);
	return rc;
}

static void buffer_queue(struct videobuf_queue *vq, struct videobuf_buffer *vb)
{
	struct csi_dev *dev = vq->priv_data;
	struct csi_buffer *buf = container_of(vb, struct csi_buffer, vb);
	struct csi_dmaqueue *vidq = &dev->vidq;

	csi_dbg(1,"buffer_queue\n");
	buf->vb.state = VIDEOBUF_QUEUED;
	list_add_tail(&buf->vb.queue, &vidq->active);
}

static void buffer_release(struct videobuf_queue *vq,
			   struct videobuf_buffer *vb)
{
	struct csi_buffer *buf  = container_of(vb, struct csi_buffer, vb);

	csi_dbg(1,"buffer_release\n");
	
	free_buffer(vq, buf);
}

static struct videobuf_queue_ops csi_video_qops = {
	.buf_setup    = buffer_setup,
	.buf_prepare  = buffer_prepare,
	.buf_queue    = buffer_queue,
	.buf_release  = buffer_release,
};

/*
 * IOCTL vidioc handling
 */
static int vidioc_querycap(struct file *file, void  *priv,
					struct v4l2_capability *cap)
{
	struct csi_dev *dev = video_drvdata(file);

	strcpy(cap->driver, "sunxi_csi");
	strcpy(cap->card, "sunxi_csi");
	strlcpy(cap->bus_info, dev->v4l2_dev.name, sizeof(cap->bus_info));

	cap->version = CSI_VERSION;
	cap->capabilities = V4L2_CAP_VIDEO_CAPTURE | V4L2_CAP_STREAMING | \
			    V4L2_CAP_READWRITE;
	return 0;
}

static int vidioc_enum_fmt_vid_cap(struct file *file, void  *priv,
					struct v4l2_fmtdesc *f)
{
	struct csi_fmt *fmt;

	csi_dbg(0,"vidioc_enum_fmt_vid_cap\n");

	if (f->index > ARRAY_SIZE(formats)-1) {
		return -EINVAL;
	}	
	fmt = &formats[f->index];

	strlcpy(f->description, fmt->name, sizeof(f->description));
	f->pixelformat = fmt->fourcc;
	return 0;
}

static int vidioc_g_fmt_vid_cap(struct file *file, void *priv,
					struct v4l2_format *f)
{
	struct csi_dev *dev = video_drvdata(file);

	f->fmt.pix.width        = dev->width;
	f->fmt.pix.height       = dev->height;
	f->fmt.pix.field        = dev->vb_vidq.field;
	f->fmt.pix.pixelformat  = dev->fmt->fourcc;
	f->fmt.pix.bytesperline = (f->fmt.pix.width * dev->fmt->depth) >> 3;
	f->fmt.pix.sizeimage    = f->fmt.pix.height * f->fmt.pix.bytesperline;

	return 0;
}

static int vidioc_try_fmt_vid_cap(struct file *file, void *priv,
			struct v4l2_format *f)
{
	struct csi_dev *dev = video_drvdata(file);
	struct csi_fmt *csi_fmt;
	
	csi_dbg(0,"vidioc_try_fmt_vid_cap\n");

	/*judge the resolution*/
	if(f->fmt.pix.width > MAX_WIDTH || f->fmt.pix.height > MAX_HEIGHT) {
		csi_err("size is too large,automatically set to maximum!\n");
		f->fmt.pix.width = MAX_WIDTH;
		f->fmt.pix.height = MAX_HEIGHT;
	}

	csi_fmt = get_format(dev,f);
	if (!csi_fmt) {
		csi_err("Fourcc format (0x%08x) invalid.\n",f->fmt.pix.pixelformat);
		return -EINVAL;
	}
	
	csi_dbg(0,"pix->width=%d\n",f->fmt.pix.width);
	csi_dbg(0,"pix->height=%d\n",f->fmt.pix.height);

	return 0;
}

static int vidioc_s_fmt_vid_cap(struct file *file, void *priv,
					struct v4l2_format *f)
{
	struct csi_dev *dev = video_drvdata(file);
	struct videobuf_queue *q = &dev->vb_vidq;
	int ret,width_buf,height_buf,width_len;
	struct v4l2_mbus_framefmt ccm_fmt;//linux-3.0
	struct csi_fmt *csi_fmt;
	
	csi_dbg(0,"vidioc_s_fmt_vid_cap\n");	
	
	if (csi_is_generating(dev)) {
		csi_err("%s device busy\n", __func__);
		return -EBUSY;
	}
	
	mutex_lock(&q->vb_lock);
	
	ret = vidioc_try_fmt_vid_cap(file, priv, f);
	if (ret < 0) {
		csi_err("try format failed!\n");
		goto out;
	}
		
	csi_fmt = get_format(dev,f);
	if (!csi_fmt) {
		csi_err("Fourcc format (0x%08x) invalid.\n",f->fmt.pix.pixelformat);
		ret	= -EINVAL;
		goto out;
	}
	
	ccm_fmt.code = csi_fmt->ccm_fmt;//linux-3.0
	ccm_fmt.width = f->fmt.pix.width;//linux-3.0
	ccm_fmt.height = f->fmt.pix.height;//linux-3.0
	ccm_fmt.field = f->fmt.pix.field;//linux-3.0
	
	ret = v4l2_subdev_call(dev->sd,video,s_mbus_fmt,&ccm_fmt);//linux-3.0
	if (ret < 0) {
		csi_err("v4l2 sub device s_fmt error!\n");
		goto out;
	}
	
	//save the current format info
	dev->fmt = csi_fmt;
	dev->vb_vidq.field = f->fmt.pix.field;
	dev->width  = f->fmt.pix.width;
	dev->height = f->fmt.pix.height;
	
	//set format
	dev->csi_mode.output_fmt = dev->fmt->output_fmt;
	dev->csi_mode.input_fmt = dev->fmt->input_fmt;
	dev->csi_mode.field_sel = dev->fmt->csi_field;
	
	switch(dev->fmt->ccm_fmt) {
	case V4L2_MBUS_FMT_YUYV8_2X8://linux-3.0
		if ((dev->fmt->fourcc == V4L2_PIX_FMT_NV61) || (dev->fmt->fourcc == V4L2_PIX_FMT_NV21) || (dev->fmt->fourcc == V4L2_PIX_FMT_YVU420))
			dev->csi_mode.seq = CSI_YVYU;
		else
			dev->csi_mode.seq = CSI_YUYV;
		break;
	case V4L2_MBUS_FMT_YVYU8_2X8://linux-3.0
		dev->csi_mode.seq = CSI_YVYU;
		break;
	case V4L2_MBUS_FMT_UYVY8_2X8://linux-3.0
		dev->csi_mode.seq = CSI_UYVY;
		break;
	case V4L2_MBUS_FMT_VYUY8_2X8://linux-3.0
		dev->csi_mode.seq = CSI_VYUY;
		break;
	default:
		dev->csi_mode.seq = CSI_YUYV;
		break;
	}
	
	switch(dev->fmt->input_fmt){
	case CSI_RAW:
		if ( (dev->fmt->fourcc == V4L2_PIX_FMT_YUYV) || (dev->fmt->fourcc == V4L2_PIX_FMT_YVYU) || \
				 (dev->fmt->fourcc == V4L2_PIX_FMT_UYVY) || (dev->fmt->fourcc == V4L2_PIX_FMT_VYUY)) {
			
			width_len  = dev->width*2;
			width_buf = dev->width*2;
			height_buf = dev->height;

		} else {
			width_len  = dev->width;
			width_buf = dev->width;
			height_buf = dev->height;
		}
		break;
	case CSI_BAYER:
		width_len  = dev->width;
		width_buf = dev->width;
		height_buf = dev->height;
		break;
	case CSI_CCIR656://TODO
		width_len  = dev->width;
		width_buf = dev->width*2;
		height_buf = dev->height/2;
		break;
	case CSI_YUV422:
		width_len  = dev->width;
		width_buf = dev->width*2;
		height_buf = dev->height;
		break;
	case CSI_YUV422_16://TODO
		width_len  = dev->width;
		width_buf = dev->width;
		height_buf = dev->height;
		break;
	default:
		width_len  = dev->width;
		width_buf = dev->width*2;
		height_buf = dev->height;
		break;
	}			

	bsp_csi_configure(dev,&dev->csi_mode);
	//horizontal and vertical offset are constant zero
	bsp_csi_set_size(dev,width_buf,height_buf,width_len);

	ret = 0;
out:
	mutex_unlock(&q->vb_lock);
	return ret;
}

static int vidioc_reqbufs(struct file *file, void *priv,
			  struct v4l2_requestbuffers *p)
{
	struct csi_dev *dev = video_drvdata(file);
	
	csi_dbg(0,"vidioc_reqbufs\n");
	
	return videobuf_reqbufs(&dev->vb_vidq, p);
}

static int vidioc_querybuf(struct file *file, void *priv, struct v4l2_buffer *p)
{
	struct csi_dev *dev = video_drvdata(file);
	
	return videobuf_querybuf(&dev->vb_vidq, p);
}

static int vidioc_qbuf(struct file *file, void *priv, struct v4l2_buffer *p)
{
	struct csi_dev *dev = video_drvdata(file);
	
	return videobuf_qbuf(&dev->vb_vidq, p);
}

static int vidioc_dqbuf(struct file *file, void *priv, struct v4l2_buffer *p)
{
	struct csi_dev *dev = video_drvdata(file);

	return videobuf_dqbuf(&dev->vb_vidq, p, file->f_flags & O_NONBLOCK);
}

#ifdef CONFIG_VIDEO_V4L1_COMPAT
static int vidiocgmbuf(struct file *file, void *priv, struct video_mbuf *mbuf)
{
	struct csi_dev *dev = video_drvdata(file);

	return videobuf_cgmbuf(&dev->vb_vidq, mbuf, 8);
}
#endif


static int vidioc_streamon(struct file *file, void *priv, enum v4l2_buf_type i)
{
	struct csi_dev *dev = video_drvdata(file);
	struct csi_dmaqueue *dma_q = &dev->vidq;
	struct csi_buffer *buf;
	
	int ret;

	csi_dbg(0,"video stream on\n");
	if (i != V4L2_BUF_TYPE_VIDEO_CAPTURE) {
		return -EINVAL;
	}	
	
	if (csi_is_generating(dev)) {
		csi_err("stream has been already on\n");
		return 0;
	}
	
	/* Resets frame counters */
	dev->ms = 0;
	dev->jiffies = jiffies;

	dma_q->frame = 0;
	dma_q->ini_jiffies = jiffies;
	
	ret = videobuf_streamon(&dev->vb_vidq);
	if (ret) {
		return ret;
	}	
	
	buf = list_entry(dma_q->active.next,struct csi_buffer, vb.queue);
	csi_set_addr(dev,buf);
	
	if (dev->capture_mode == V4L2_MODE_IMAGE) {
		bsp_csi_int_clear_status(dev,CSI_INT_VSYNC_TRIG);
		bsp_csi_int_clear_status(dev,CSI_INT_CAPTURE_DONE);
		bsp_csi_int_enable(dev,CSI_INT_VSYNC_TRIG);
		bsp_csi_int_enable(dev,CSI_INT_CAPTURE_DONE);
		bsp_csi_capture_video_start(dev);
	} else {
		bsp_csi_int_clear_status(dev,CSI_INT_FRAME_DONE);
		bsp_csi_int_enable(dev, CSI_INT_FRAME_DONE);
		bsp_csi_capture_video_start(dev);
	}	
	
	csi_start_generating(dev);
	return 0;
}


static int vidioc_streamoff(struct file *file, void *priv, enum v4l2_buf_type i)
{
	struct csi_dev *dev = video_drvdata(file);
	struct csi_dmaqueue *dma_q = &dev->vidq;
	int ret;

	csi_dbg(0,"video stream off\n");
	
	if (!csi_is_generating(dev)) {
		csi_err("stream has been already off\n");
		return 0;
	}
	
	csi_stop_generating(dev);

	/* Resets frame counters */
	dev->ms = 0;
	dev->jiffies = jiffies;

	dma_q->frame = 0;
	dma_q->ini_jiffies = jiffies;
	
	bsp_csi_int_disable(dev,CSI_INT_VSYNC_TRIG);
	bsp_csi_int_disable(dev,CSI_INT_CAPTURE_DONE);
	bsp_csi_int_disable(dev,CSI_INT_FRAME_DONE);
	bsp_csi_int_clear_status(dev, CSI_INT_VSYNC_TRIG);
	bsp_csi_int_clear_status(dev,CSI_INT_CAPTURE_DONE);
	bsp_csi_int_clear_status(dev, CSI_INT_FRAME_DONE);
	
	bsp_csi_capture_video_stop(dev);
	
	
	if (i != V4L2_BUF_TYPE_VIDEO_CAPTURE) {
		return -EINVAL;
	}

	ret = videobuf_streamoff(&dev->vb_vidq);
	if (ret!=0) {
		csi_err("videobu_streamoff error!\n");
		return ret;
	}
	
	if (ret!=0) {
		csi_err("videobuf_mmap_free error!\n");
		return ret;
	}
	
	return 0;
}


static int vidioc_enum_input(struct file *file, void *priv,
				struct v4l2_input *inp)
{
	struct csi_dev *dev = video_drvdata(file);
	
	if (inp->index > dev->dev_qty-1) {
		csi_err("input index invalid!\n");
		return -EINVAL;
	}

	inp->type = V4L2_INPUT_TYPE_CAMERA;
	
	return 0;
}

static int vidioc_g_input(struct file *file, void *priv, unsigned int *i)
{
	struct csi_dev *dev = video_drvdata(file);

	*i = dev->input;
	return 0;
}

static int internal_s_input(struct csi_dev *dev, unsigned int i)
{
	struct v4l2_control ctrl;
	int ret;
	
	if (i > dev->dev_qty-1) {
		csi_err("set input error!\n");
		return -EINVAL;
	}
	
	if (i == dev->input)
		return 0;
	
	csi_dbg(0,"input_num = %d\n",i);

//	spin_lock(&dev->slock);
	if(dev->input != -1) {
		/*Power down current device*/
		ret = v4l2_subdev_call(dev->sd,core, s_power, CSI_SUBDEV_STBY_ON);
		if(ret < 0)
			goto altend;
	}
	/* Alternate the device info and select target device*/
        ret = update_ccm_info(dev, dev->ccm_cfg[i]);
        if (ret < 0)
	{
		csi_err("Error when set ccm info when selecting input!,input_num = %d\n",i);
		goto recover;
	}
	
	/* change the csi setting */
	csi_dbg(0,"dev->ccm_info->vref = %d\n",dev->ccm_info->vref);
	csi_dbg(0,"dev->ccm_info->href = %d\n",dev->ccm_info->href);
	csi_dbg(0,"dev->ccm_info->clock = %d\n",dev->ccm_info->clock);
	csi_dbg(0,"dev->ccm_info->mclk = %d\n",dev->ccm_info->mclk);
	
	dev->csi_mode.vref       = dev->ccm_info->vref;
        dev->csi_mode.href       = dev->ccm_info->href;
        dev->csi_mode.clock      = dev->ccm_info->clock;
  
//  bsp_csi_configure(dev,&dev->csi_mode);
	csi_clk_out_set(dev);
	
	/* Initial target device */
	ret = v4l2_subdev_call(dev->sd,core, s_power, CSI_SUBDEV_STBY_OFF);
	if (ret!=0) {
	  csi_err("sensor standby off error when selecting target device!\n");
	  goto recover;
	}
	
	ret = v4l2_subdev_call(dev->sd,core, init, 0);
	if (ret!=0) {
		csi_err("sensor initial error when selecting target device!\n");
		goto recover;
	}
	
	/* Set the initial flip */
	ctrl.id = V4L2_CID_VFLIP;
	ctrl.value = dev->vflip;
	ret = v4l2_subdev_call(dev->sd,core, s_ctrl, &ctrl);
	if (ret!=0) {
		csi_err("sensor sensor_s_ctrl V4L2_CID_VFLIP error when vidioc_s_input!input_num = %d\n",i);
	}
	
	ctrl.id = V4L2_CID_HFLIP;
	ctrl.value = dev->hflip;
	ret = v4l2_subdev_call(dev->sd,core, s_ctrl, &ctrl);
	if (ret!=0) {
		csi_err("sensor sensor_s_ctrl V4L2_CID_HFLIP error when vidioc_s_input!input_num = %d\n",i);
	}
	
	dev->input = i;
  ret = 0;
  
altend:
//  spin_unlock(&dev->slock);
	return ret;
	
recover:
	/*Power down target device*/
	if (dev->input == -1) 
        {
                if(dev->dev_qty == 1)
		        goto altend;
		else
			dev->input = (i ==0 ? 1 : 0 );
	}
	ret = v4l2_subdev_call(dev->sd,core, s_power, CSI_SUBDEV_STBY_ON);
	if(ret < 0)
	{	
		csi_err("sensor standby on error at current device!\n");
		goto altend;
	}
	/* Alternate the device info and select the current device*/
        ret = update_ccm_info(dev, dev->ccm_cfg[dev->input]);
        if (ret < 0)
	{
		csi_err("Error when set ccm info when selecting input!\n");
		goto altend;
	}
	
	
	/*Re Initial current device*/
	ret = v4l2_subdev_call(dev->sd,core, s_power, CSI_SUBDEV_STBY_OFF);
	if (ret!=0)
        {
	        csi_err("sensor standby off error when selecting back current device!\n");
	        goto recover;
	}
	
	ret = v4l2_subdev_call(dev->sd,core, init, 0);
	if (ret!=0) 
        {
		csi_err("sensor recovering error when selecting back current device!\n");
	}
	ret = 0;
	goto altend;
}

static int vidioc_s_input(struct file *file, void *priv, unsigned int i)
{
	struct csi_dev *dev = video_drvdata(file);
	
	return internal_s_input(dev , i);
}


static int vidioc_queryctrl(struct file *file, void *priv,
			    struct v4l2_queryctrl *qc)
{
	struct csi_dev *dev = video_drvdata(file);
	int ret;
		
	ret = v4l2_subdev_call(dev->sd,core,queryctrl,qc);
	if (ret < 0)
		csi_err("v4l2 sub device queryctrl error!\n");
	
	return ret;
}

static int vidioc_g_ctrl(struct file *file, void *priv,
			 struct v4l2_control *ctrl)
{
	struct csi_dev *dev = video_drvdata(file);
	int ret;
	
	ret = v4l2_subdev_call(dev->sd,core,g_ctrl,ctrl);
	if (ret < 0)
		csi_err("v4l2 sub device g_ctrl error!\n");
	
	return ret;
}


static int vidioc_s_ctrl(struct file *file, void *priv,
				struct v4l2_control *ctrl)
{
	struct csi_dev *dev = video_drvdata(file);
	struct v4l2_queryctrl qc;
	int ret;
	
	qc.id = ctrl->id;
	ret = vidioc_queryctrl(file, priv, &qc);
	if (ret < 0) {
		return ret;
	}

	if (ctrl->value < qc.minimum || ctrl->value > qc.maximum) {
		return -ERANGE;
	}

	ret = v4l2_subdev_call(dev->sd,core,s_ctrl,ctrl);
	if (ret < 0)
		csi_err("v4l2 sub device s_ctrl error!\n");

	return ret;
}

static int vidioc_g_parm(struct file *file, void *priv,
			 struct v4l2_streamparm *parms) 
{
	struct csi_dev *dev = video_drvdata(file);
	int ret;
	
	ret = v4l2_subdev_call(dev->sd,video,g_parm,parms);
	if (ret < 0)
		csi_err("v4l2 sub device g_parm error!\n");

	return ret;
}

static int vidioc_s_parm(struct file *file, void *priv,
			 struct v4l2_streamparm *parms)
{
	struct csi_dev *dev = video_drvdata(file);
	int ret;
	
	if(parms->parm.capture.capturemode != V4L2_MODE_VIDEO && \
		parms->parm.capture.capturemode != V4L2_MODE_IMAGE) {
		
		parms->parm.capture.capturemode = V4L2_MODE_VIDEO;
	}
	
	dev->capture_mode = parms->parm.capture.capturemode;
	
	ret = v4l2_subdev_call(dev->sd,video,s_parm,parms);
	if (ret < 0)
		csi_err("v4l2 sub device s_parm error!\n");

	return ret;
}


static ssize_t csi_read(struct file *file, char __user *data, size_t count, loff_t *ppos)
{
	struct csi_dev *dev = video_drvdata(file);

//	csi_start_generating(dev);
	if(csi_is_generating(dev)) {
		return videobuf_read_stream(&dev->vb_vidq, data, count, ppos, 0,
					file->f_flags & O_NONBLOCK);
	} else {
		csi_err("csi is not generating!\n");
		return -EINVAL;
	}
}

static unsigned int csi_poll(struct file *file, struct poll_table_struct *wait)
{
	struct csi_dev *dev = video_drvdata(file);
	struct videobuf_queue *q = &dev->vb_vidq;

//	csi_start_generating(dev);
	if(csi_is_generating(dev)) {
		return videobuf_poll_stream(file, q, wait);
	} else {
		csi_err("csi is not generating!\n");
		return -EINVAL;
	}
}

static int csi_open(struct file *file)
{
	struct csi_dev *dev = video_drvdata(file);
	int ret,input_num;
	
	csi_dbg(0,"csi_open\n");

	if (dev->opened == 1) {
		csi_err("device open busy\n");
		return -EBUSY;
	}
	
	csi_clk_enable(dev);
	csi_reset_disable(dev);
	
	if(dev->stby_mode == 1)	{
		//open all the device power and set it to standby on
		for (input_num=dev->dev_qty-1; input_num>=0; input_num--) {
			/* update target device info and select it*/
			ret = update_ccm_info(dev, dev->ccm_cfg[input_num]);
			if (ret < 0)
			{
				csi_err("Error when set ccm info when csi open!\n");
			}
			
			dev->csi_mode.vref       = dev->ccm_info->vref;
		  dev->csi_mode.href       = dev->ccm_info->href;
		  dev->csi_mode.clock      = dev->ccm_info->clock;
			csi_clk_out_set(dev);
			
			ret = v4l2_subdev_call(dev->sd,core, s_power, CSI_SUBDEV_PWR_ON);
		  if (ret!=0) {
		  	csi_err("sensor CSI_SUBDEV_PWR_ON error at device number %d when csi open!\n",input_num);
		  }
	
			ret = v4l2_subdev_call(dev->sd,core, s_power, CSI_SUBDEV_STBY_ON);
			if (ret!=0) {
		  	csi_err("sensor CSI_SUBDEV_STBY_ON error at device number %d when csi open!\n",input_num);
		  } 
		}
	}
	
	dev->input=-1;//default input null

	bsp_csi_open(dev);
	bsp_csi_set_offset(dev,0,0);//h and v offset is initialed to zero
	dev->opened = 1;
	dev->fmt = &formats[5]; //default format
	return 0;		
}

static int csi_close(struct file *file)
{
	struct csi_dev *dev = video_drvdata(file);
	int ret,input_num;
	
	csi_dbg(0,"csi_close\n");

	bsp_csi_int_disable(dev,CSI_INT_FRAME_DONE);
	bsp_csi_int_disable(dev,CSI_INT_VSYNC_TRIG);
	bsp_csi_int_disable(dev,CSI_INT_CAPTURE_DONE);
	
	//bsp_csi_int_clear_status(dev,CSI_INT_FRAME_DONE);

	bsp_csi_capture_video_stop(dev);
	bsp_csi_close(dev);

	csi_clk_disable(dev);
	csi_reset_enable(dev);


	videobuf_stop(&dev->vb_vidq);
	videobuf_mmap_free(&dev->vb_vidq);

	dev->opened=0;
	csi_stop_generating(dev);
	
	if(dev->stby_mode == 0) {
		return v4l2_subdev_call(dev->sd,core, s_power, CSI_SUBDEV_STBY_ON);
	} else {
		//close all the device power	
		for (input_num=0; input_num<dev->dev_qty; input_num++) {
      /* update target device info and select it */
      ret = update_ccm_info(dev, dev->ccm_cfg[input_num]);
			if (ret < 0)
			{
				csi_err("Error when set ccm info when csi_close!\n");
			}
			
			ret = v4l2_subdev_call(dev->sd,core, s_power, CSI_SUBDEV_PWR_OFF);
		  if (ret!=0) {
		  	csi_err("sensor power off error at device number %d when csi open!\n",input_num);
		  	return ret;
		  }
		}
	}
	
	return 0;
}

static int csi_mmap(struct file *file, struct vm_area_struct *vma)
{
	struct csi_dev *dev = video_drvdata(file);
	int ret;

	csi_dbg(0,"mmap called, vma=0x%08lx\n", (unsigned long)vma);

	ret = videobuf_mmap_mapper(&dev->vb_vidq, vma);

	csi_dbg(0,"vma start=0x%08lx, size=%ld, ret=%d\n",
		(unsigned long)vma->vm_start,
		(unsigned long)vma->vm_end - (unsigned long)vma->vm_start,
		ret);
	return ret;
}

static const struct v4l2_file_operations csi_fops = {
	.owner	  = THIS_MODULE,
	.open	  = csi_open,
	.release  = csi_close,
	.read     = csi_read,
	.poll	  = csi_poll,
	.ioctl    = video_ioctl2,
	.mmap     = csi_mmap,
};

static const struct v4l2_ioctl_ops csi_ioctl_ops = {
	.vidioc_querycap          = vidioc_querycap,
	.vidioc_enum_fmt_vid_cap  = vidioc_enum_fmt_vid_cap,
	.vidioc_g_fmt_vid_cap     = vidioc_g_fmt_vid_cap,
	.vidioc_try_fmt_vid_cap   = vidioc_try_fmt_vid_cap,
	.vidioc_s_fmt_vid_cap     = vidioc_s_fmt_vid_cap,
	.vidioc_reqbufs           = vidioc_reqbufs,
	.vidioc_querybuf          = vidioc_querybuf,
	.vidioc_qbuf              = vidioc_qbuf,
	.vidioc_dqbuf             = vidioc_dqbuf,
	.vidioc_enum_input        = vidioc_enum_input,
	.vidioc_g_input           = vidioc_g_input,
	.vidioc_s_input           = vidioc_s_input,
	.vidioc_streamon          = vidioc_streamon,
	.vidioc_streamoff         = vidioc_streamoff,
	.vidioc_queryctrl         = vidioc_queryctrl,
	.vidioc_g_ctrl            = vidioc_g_ctrl,
	.vidioc_s_ctrl            = vidioc_s_ctrl,
	.vidioc_g_parm		 			  = vidioc_g_parm,
	.vidioc_s_parm		  			= vidioc_s_parm,

#ifdef CONFIG_VIDEO_V4L1_COMPAT
	.vidiocgmbuf              = vidiocgmbuf,
#endif
};

static struct video_device csi_template = {
	.name		= "csi",
	.fops       = &csi_fops,
	.ioctl_ops 	= &csi_ioctl_ops,
	.release	= video_device_release,
};

//modified by heyihang.Jan 15, 2013
#if 1
static int fetch_config(struct csi_dev *dev)
{
//#ifndef CSI_VER_FOR_FPGA
	int input_num,ret;
	script_item_u   val;
  script_item_value_type_e  type;
  //int req_status;
  //int pio_idx;

	/* fetch device quatity issue */
	type = script_get_item("csi0_para","csi_dev_qty", &val);
	if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	  dev->dev_qty=1;
		csi_dbg(0,"fetch csi_dev_qty from sys_config failed, default =1\n");
	} else {
	  dev->dev_qty=val.val;
	}

	/* fetch standby mode */
	type = script_get_item("csi0_para","csi_stby_mode", &val);
	if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	  dev->stby_mode=0;
		csi_dbg(0,"fetch csi_stby_mode from sys_config failed, default =0\n");
	} else {
	  dev->stby_mode=val.val;
	}

	for(input_num=0; input_num<dev->dev_qty; input_num++)
	{
		dev->ccm_cfg[input_num] = &ccm_cfg[input_num]; 
		csi_dbg(0,"dev->ccm_cfg[%d] = %p\n",input_num,dev->ccm_cfg[input_num]);
	}
	
	if(dev->dev_qty > 0)
	{
		dev->ccm_cfg[0]->i2c_addr = i2c_addr;
		strcpy(dev->ccm_cfg[0]->ccm,ccm);
		
		/* fetch i2c and module name*/
		type = script_get_item("csi0_para","csi_twi_id", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	    dev->ccm_cfg[0]->twi_id=0;
		  csi_dbg(0,"fetch csi_twi_id from sys_config failed, default =0\n");
		} else {
	    dev->ccm_cfg[0]->twi_id=val.val;
	  }
		
		ret = strcmp(dev->ccm_cfg[0]->ccm,"");
		if((dev->ccm_cfg[0]->i2c_addr == 0xff) && (ret == 0))	//when insmod without parm
		{
			type = script_get_item("csi0_para","csi_twi_addr", &val);
			if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	      dev->ccm_cfg[0]->i2c_addr=0x78;
				csi_dbg(0,"fetch csi_twi_addr from sys_config failed, default =0x78\n");
			} else {
	      dev->ccm_cfg[0]->i2c_addr=val.val;
	    }

			type = script_get_item("csi0_para","csi_mname", &val);
			if (SCIRPT_ITEM_VALUE_TYPE_STR != type) {
			  char tmp_str[]="ov5640";
			  strcpy(dev->ccm_cfg[0]->ccm,tmp_str);
				csi_dbg(0,"fetch csi_mname from sys_config failed, default ov5640\n");
			} else {
			  strcpy(dev->ccm_cfg[0]->ccm,val.str);
	    }
		}
		
		/* fetch interface issue*/
		type = script_get_item("csi0_para","csi_if", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	    dev->ccm_cfg[0]->interface=0;
			csi_dbg(0,"fetch csi_if from sys_config failed, default =0\n");
		} else {
	    dev->ccm_cfg[0]->interface=val.val;
	  }
	
		/* fetch power issue*/	
		type = script_get_item("csi0_para","csi_iovdd", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_STR != type) {
		  char null_str[]="";
		  strcpy(dev->ccm_cfg[0]->iovdd_str,null_str);
			csi_dbg(0,"fetch csi_iovdd from sys_config failed\n");
		} else {
		  strcpy(dev->ccm_cfg[0]->iovdd_str,val.str);
		}
		
		type = script_get_item("csi0_para","csi_avdd", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_STR != type) {
		  char null_str[]="";
		  strcpy(dev->ccm_cfg[0]->avdd_str,null_str);
			csi_dbg(0,"fetch csi_avdd from sys_config failed\n");
		} else {
		  strcpy(dev->ccm_cfg[0]->avdd_str,val.str);
		}
		
		type = script_get_item("csi0_para","csi_dvdd", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_STR != type) {
		  char null_str[]="";
		  strcpy(dev->ccm_cfg[0]->dvdd_str,null_str);
			csi_dbg(0,"fetch csi_dvdd from sys_config failed\n");
		} else {
		  strcpy(dev->ccm_cfg[0]->dvdd_str,val.str);
		}
		
		type = script_get_item("csi0_para","csi_vol_iovdd", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	    dev->ccm_cfg[0]->vol_iovdd=0;
			csi_dbg(0,"fetch csi_vol_iovdd from sys_config failed, default =0\n");
		} else {
	    dev->ccm_cfg[0]->vol_iovdd=val.val;
	  }
		type = script_get_item("csi0_para","csi_vol_dvdd", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	    dev->ccm_cfg[0]->vol_dvdd=0;
			csi_dbg(0,"fetch csi_vol_dvdd from sys_config failed, default =0\n");
		} else {
	    dev->ccm_cfg[0]->vol_dvdd=val.val;
	  }
		type = script_get_item("csi0_para","csi_vol_avdd", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	    dev->ccm_cfg[0]->vol_avdd=0;
			csi_dbg(0,"fetch csi_vol_avdd from sys_config failed, default =0\n");
		} else {
	    dev->ccm_cfg[0]->vol_avdd=val.val;
	  }
	  /* fetch power issue*/
	  
//		/* fetch standby mode */
//		type = script_get_item("csi0_para","csi_stby_mode", &dev->ccm_cfg[0]->stby_mode , sizeof(int));
//		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
//			csi_dbg(0,"fetch csi_stby_mode from sys_config failed\n");
//		} else {
//		  
//		}
		
		/* fetch flip issue */
		type = script_get_item("csi0_para","csi_vflip", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
		  dev->ccm_cfg[0]->vflip=0;
			csi_dbg(0,"fetch vflip from sys_config failed, default=0\n");
		} else {
		  dev->ccm_cfg[0]->vflip=val.val;
		}
		
		type = script_get_item("csi0_para","csi_hflip", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
		  dev->ccm_cfg[0]->hflip=0;
			csi_dbg(0,"fetch hflip from sys_config failed, default=0\n");
		} else {
		  dev->ccm_cfg[0]->hflip=val.val;
		}
		
		/* fetch flash light issue */
		type = script_get_item("csi0_para","csi_flash_pol", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
		  dev->ccm_cfg[0]->flash_pol=0;
			csi_dbg(0,"fetch csi_flash_pol from sys_config failed, default=0\n");
		} else {
		  dev->ccm_cfg[0]->flash_pol=val.val;
		}
		
		/* fetch reset/power/standby/flash/af io issue */
		type = script_get_item("csi0_para","csi_reset", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_PIO != type) {
		  dev->ccm_cfg[0]->reset_io.gpio=GPIO_INDEX_INVALID;
			csi_dbg(0,"not using csi_reset in sys_config\n");
		} else {
		  dev->ccm_cfg[0]->reset_io.gpio=val.gpio.gpio;
		  dev->ccm_cfg[0]->reset_io.mul_sel=val.gpio.mul_sel;
		}
		
		type = script_get_item("csi0_para","csi_stby", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_PIO != type) {
		  dev->ccm_cfg[0]->standby_io.gpio=GPIO_INDEX_INVALID;
			csi_dbg(0,"not using csi_stby in sys_config\n");
		} else {
		  dev->ccm_cfg[0]->standby_io.gpio=val.gpio.gpio;
		  dev->ccm_cfg[0]->standby_io.mul_sel=val.gpio.mul_sel;
		}
		
		type = script_get_item("csi0_para","csi_power_en", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_PIO != type) {
		  dev->ccm_cfg[0]->power_io.gpio=GPIO_INDEX_INVALID;
			csi_dbg(0,"not using csi_power_en in sys_config\n");
		} else {
		  dev->ccm_cfg[0]->power_io.gpio=val.gpio.gpio;
		  dev->ccm_cfg[0]->power_io.mul_sel=val.gpio.mul_sel;
		}
		
		type = script_get_item("csi0_para","csi_flash", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_PIO != type) {
			csi_dbg(0,"not using csi_flash in sys_config\n");
			dev->ccm_cfg[0]->flash_io.gpio=GPIO_INDEX_INVALID;
		} else {
		  dev->ccm_cfg[0]->flash_io.gpio=val.gpio.gpio;
		  dev->ccm_cfg[0]->flash_io.mul_sel=val.gpio.mul_sel;
		}
		
		type = script_get_item("csi0_para","csi_af_en", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_PIO != type) {
			dev->ccm_cfg[0]->af_power_io.gpio=GPIO_INDEX_INVALID;
			csi_dbg(0,"not using csi_af_en in sys_config\n");
		} else {
		  dev->ccm_cfg[0]->af_power_io.gpio=val.gpio.gpio;
		  dev->ccm_cfg[0]->af_power_io.mul_sel=val.gpio.mul_sel;
		}
	} 
	
	if(dev->dev_qty > 1)
	{
		dev->ccm_cfg[1]->i2c_addr = i2c_addr_b;
		strcpy(dev->ccm_cfg[1]->ccm,ccm_b);
	
		/* fetch i2c and module name*/
		type = script_get_item("csi0_para","csi_twi_id_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	    dev->ccm_cfg[1]->twi_id=0;
		  csi_dbg(0,"fetch csi_twi_id_b from sys_config failed, default =0\n");
		} else {
	    dev->ccm_cfg[1]->twi_id=val.val;
	  }
		
		ret = strcmp(dev->ccm_cfg[1]->ccm,"");
		if((dev->ccm_cfg[1]->i2c_addr == 0xff) && (ret == 0))	//when insmod without parm
		{
			type = script_get_item("csi0_para","csi_twi_addr_b", &val);
			if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	      dev->ccm_cfg[1]->i2c_addr=0x78;
				csi_dbg(0,"fetch csi_twi_addr_b from sys_config failed, default =0x78\n");
			} else {
	      dev->ccm_cfg[1]->i2c_addr=val.val;
	    }

			type = script_get_item("csi0_para","csi_mname_b", &val);
			if (SCIRPT_ITEM_VALUE_TYPE_STR != type) {
			  char tmp_str[]="ov5640";
			  strcpy(dev->ccm_cfg[1]->ccm,tmp_str);
				csi_dbg(0,"fetch csi_mname_b from sys_config failed, default ov5640\n");
			} else {
			  strcpy(dev->ccm_cfg[1]->ccm,val.str);
	    }
		}
		
		/* fetch interface issue*/
		type = script_get_item("csi0_para","csi_if_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	    dev->ccm_cfg[1]->interface=0;
			csi_dbg(0,"fetch csi_if_b from sys_config failed, default =0\n");
		} else {
	    dev->ccm_cfg[1]->interface=val.val;
	  }
	
		/* fetch power issue*/	
		type = script_get_item("csi0_para","csi_iovdd_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_STR != type) {
		  char null_str[]="";
		  strcpy(dev->ccm_cfg[1]->iovdd_str,null_str);
			csi_dbg(0,"fetch csi_iovdd_b from sys_config failed\n");
		} else {
		  strcpy(dev->ccm_cfg[1]->iovdd_str,val.str);
		}
		
		type = script_get_item("csi0_para","csi_avdd_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_STR != type) {
		  char null_str[]="";
		  strcpy(dev->ccm_cfg[1]->avdd_str,null_str);
			csi_dbg(0,"fetch csi_avdd_b from sys_config failed\n");
		} else {
		  strcpy(dev->ccm_cfg[1]->avdd_str,val.str);
		}
		
		type = script_get_item("csi0_para","csi_dvdd_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_STR != type) {
		  char null_str[]="";
		  strcpy(dev->ccm_cfg[1]->dvdd_str,null_str);
			csi_dbg(0,"fetch csi_dvdd_b from sys_config failed\n");
		} else {
		  strcpy(dev->ccm_cfg[1]->dvdd_str,val.str);
		}
		
		type = script_get_item("csi0_para","csi_vol_iovdd_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	    dev->ccm_cfg[1]->vol_iovdd=0;
			csi_dbg(0,"fetch csi_vol_iovdd_b from sys_config failed, default =0\n");
		} else {
	    dev->ccm_cfg[1]->vol_iovdd=val.val;
	  }
		type = script_get_item("csi0_para","csi_vol_dvdd_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	    dev->ccm_cfg[1]->vol_dvdd=0;
			csi_dbg(0,"fetch csi_vol_dvdd_b from sys_config failed, default =0\n");
		} else {
	    dev->ccm_cfg[1]->vol_dvdd=val.val;
	  }
		type = script_get_item("csi0_para","csi_vol_avdd_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
	    dev->ccm_cfg[1]->vol_avdd=0;
			csi_dbg(0,"fetch csi_vol_avdd_b from sys_config failed, default =0\n");
		} else {
	    dev->ccm_cfg[1]->vol_avdd=val.val;
	  }
//		/* fetch standby mode */
//		type = script_get_item("csi0_para","csi_stby_mode", &dev->ccm_cfg[0]->stby_mode , sizeof(int));
//		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
//			csi_dbg(0,"fetch csi_stby_mode from sys_config failed\n");
//		} else {
//		  
//		}
		
		/* fetch flip issue */
		type = script_get_item("csi0_para","csi_vflip_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
		  dev->ccm_cfg[1]->vflip=0;
			csi_dbg(0,"fetch vflip_b from sys_config failed, default=0\n");
		} else {
		  dev->ccm_cfg[1]->vflip=val.val;
		}
		
		type = script_get_item("csi0_para","csi_hflip_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
		  dev->ccm_cfg[1]->hflip=0;
			csi_dbg(0,"fetch hflip_b from sys_config failed, default=0\n");
		} else {
		  dev->ccm_cfg[1]->hflip=val.val;
		}
		
		/* fetch flash light issue */
		type = script_get_item("csi0_para","csi_flash_pol_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
		  dev->ccm_cfg[1]->flash_pol=0;
			csi_dbg(0,"fetch csi_flash_pol_b from sys_config failed, default=0\n");
		} else {
		  dev->ccm_cfg[1]->flash_pol=val.val;
		}
		
		/* fetch reset/power/standby/flash/af io issue */
		type = script_get_item("csi0_para","csi_reset_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_PIO != type) {
		  dev->ccm_cfg[1]->reset_io.gpio=GPIO_INDEX_INVALID;
			csi_dbg(0,"not using csi_reset_b in sys_config\n");
		} else {
		  dev->ccm_cfg[1]->reset_io.gpio=val.gpio.gpio;
		  dev->ccm_cfg[1]->reset_io.mul_sel=val.gpio.mul_sel;
		}
		
		type = script_get_item("csi0_para","csi_stby_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_PIO != type) {
		  dev->ccm_cfg[1]->standby_io.gpio=GPIO_INDEX_INVALID;
			csi_dbg(0,"not using csi_stby_b in sys_config\n");
		} else {
		  dev->ccm_cfg[1]->standby_io.gpio=val.gpio.gpio;
		  dev->ccm_cfg[1]->standby_io.mul_sel=val.gpio.mul_sel;
		}
		
		type = script_get_item("csi0_para","csi_power_en_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_PIO != type) {
		  dev->ccm_cfg[1]->power_io.gpio=GPIO_INDEX_INVALID;
			csi_dbg(0,"not using csi_power_en_b in sys_config\n");
		} else {
		  dev->ccm_cfg[1]->power_io.gpio=val.gpio.gpio;
		  dev->ccm_cfg[1]->power_io.mul_sel=val.gpio.mul_sel;
		}
		
		type = script_get_item("csi0_para","csi_flash_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_PIO != type) {
		  dev->ccm_cfg[1]->flash_io.gpio=GPIO_INDEX_INVALID;
			csi_dbg(0,"not using csi_flash_b in sys_config\n");
		} else {
		  dev->ccm_cfg[1]->flash_io.gpio=val.gpio.gpio;
		  dev->ccm_cfg[1]->flash_io.mul_sel=val.gpio.mul_sel;
		}
		
		type = script_get_item("csi0_para","csi_af_en_b", &val);
		if (SCIRPT_ITEM_VALUE_TYPE_PIO != type) {
		  dev->ccm_cfg[1]->af_power_io.gpio=GPIO_INDEX_INVALID;
			csi_dbg(0,"not using csi_af_en_b in sys_config\n");
		} else {
		  dev->ccm_cfg[1]->af_power_io.gpio=val.gpio.gpio;
		  dev->ccm_cfg[1]->af_power_io.mul_sel=val.gpio.mul_sel;
		}
	} 
	
	for(input_num=0; input_num<dev->dev_qty; input_num++)
	{
		csi_dbg(0,"dev->ccm_cfg[%d]->ccm = %s\n",input_num,dev->ccm_cfg[input_num]->ccm);
		csi_dbg(0,"dev->ccm_cfg[%d]->twi_id = %x\n",input_num,dev->ccm_cfg[input_num]->twi_id);
		csi_dbg(0,"dev->ccm_cfg[%d]->i2c_addr = %x\n",input_num,dev->ccm_cfg[input_num]->i2c_addr);
		csi_dbg(0,"dev->ccm_cfg[%d]->interface = %x\n",input_num,dev->ccm_cfg[input_num]->interface);
		csi_dbg(0,"dev->ccm_cfg[%d]->vflip = %x\n",input_num,dev->ccm_cfg[input_num]->vflip);
		csi_dbg(0,"dev->ccm_cfg[%d]->hflip = %x\n",input_num,dev->ccm_cfg[input_num]->hflip);
		csi_dbg(0,"dev->ccm_cfg[%d]->iovdd_str = %s\n",input_num,dev->ccm_cfg[input_num]->iovdd_str);
		csi_dbg(0,"dev->ccm_cfg[%d]->avdd_str = %s\n",input_num,dev->ccm_cfg[input_num]->avdd_str);
		csi_dbg(0,"dev->ccm_cfg[%d]->dvdd_str = %s\n",input_num,dev->ccm_cfg[input_num]->dvdd_str);
		csi_dbg(0,"dev->ccm_cfg[%d]->flash_pol = %d\n",input_num,dev->ccm_cfg[input_num]->flash_pol);
//		csi_dbg(0,"dev->ccm_cfg[%d]->reset_io.gpio = %d\n",input_num,dev->ccm_cfg[input_num]->reset_io.gpio);
//		csi_dbg(0,"dev->ccm_cfg[%d]->standby_io.gpio = %d\n",input_num,dev->ccm_cfg[input_num]->standby_io.gpio);
//		csi_dbg(0,"dev->ccm_cfg[%d]->power_io.gpio = %d\n",input_num,dev->ccm_cfg[input_num]->power_io.gpio);
	}
//#else
//	dev->dev_qty = 1;
//  dev->stby_mode = 0;
//	dev->ccm_cfg[0] = &ccm_cfg[0]; 
//	dev->ccm_cfg[0]->twi_id = 1;
//	dev->ccm_cfg[0]->i2c_addr = 0x78;
//  strcpy(dev->ccm_cfg[0]->ccm,"ov5640");
////	dev->ccm_cfg[0]->i2c_addr = 0x5a;
////  strcpy(dev->ccm_cfg[0]->ccm,"s5k4ec");
////	dev->ccm_cfg[0]->i2c_addr = 0x78;
////  strcpy(dev->ccm_cfg[0]->ccm,"gc2035");
//	dev->ccm_cfg[0]->interface = 0x00;
//	dev->ccm_cfg[0]->stby_mode = 0;
//	dev->ccm_cfg[0]->vflip = 0;
//	dev->ccm_cfg[0]->hflip = 1;
//	
////	dev->dev_qty = 2;
////  dev->stby_mode = 0;
////	dev->ccm_cfg[1] = &ccm_cfg[1]; 
////	dev->ccm_cfg[1]->twi_id = 1;
////	dev->ccm_cfg[1]->i2c_addr = 0x42;
////  strcpy(dev->ccm_cfg[1]->ccm,"gc0308");
//////	dev->ccm_cfg[1]->i2c_addr = 0x5a;
//////  strcpy(dev->ccm_cfg[1]->ccm,"s5k4ec");
//////	dev->ccm_cfg[1]->i2c_addr = 0x42;
//////  strcpy(dev->ccm_cfg[1]->ccm,"gc0308");
////	dev->ccm_cfg[1]->interface = 0x0;
////	dev->ccm_cfg[1]->stby_mode = 0;
////	dev->ccm_cfg[1]->vflip = 0;
////	dev->ccm_cfg[1]->hflip = 1;
//#endif	
//	dev->ccm_cfg[0]->twi_id = 1;
	
	return 0;
}
#else
static int fetch_config(struct csi_dev *dev)
{
#ifndef FPGA	
	int input_num,ret;
	
	/* fetch device quatity issue */
	ret = script_parser_fetch("csi0_para","csi_dev_qty", &dev->dev_qty , sizeof(int));
	if (ret) {
		csi_err("fetch csi_dev_qty from sys_config failed\n");
	}
	
	/* fetch standby mode */
	ret = script_parser_fetch("csi0_para","csi_stby_mode", &dev->stby_mode , sizeof(int));
	if (ret) {
		csi_err("fetch csi_stby_mode from sys_config failed\n");
	}

	for(input_num=0; input_num<dev->dev_qty; input_num++)
	{
		dev->ccm_cfg[input_num] = &ccm_cfg[input_num]; 
		csi_dbg(0,"dev->ccm_cfg[%d] = %p\n",input_num,dev->ccm_cfg[input_num]);
	}
	
	if(dev->dev_qty > 0)
	{
		dev->ccm_cfg[0]->i2c_addr = i2c_addr;
		strcpy(dev->ccm_cfg[0]->ccm,ccm);
		
		/* fetch i2c and module name*/
		ret = script_parser_fetch("csi0_para","csi_twi_id", &dev->ccm_cfg[0]->twi_id , sizeof(int));
		if (ret) {
		}
		
		ret = strcmp(dev->ccm_cfg[0]->ccm,"");
		if((dev->ccm_cfg[0]->i2c_addr == 0xff) && (ret == 0))	//when insmod without parm
		{
			ret = script_parser_fetch("csi0_para","csi_twi_addr", &dev->ccm_cfg[0]->i2c_addr , sizeof(int));
			if (ret) {
				csi_err("fetch csi_twi_addr from sys_config failed\n");
			}
			
			ret = script_parser_fetch("csi0_para","csi_mname", (int *)&dev->ccm_cfg[0]->ccm , I2C_NAME_SIZE*sizeof(char));
			if (ret) {
				csi_err("fetch csi_mname from sys_config failed\n");
			}
		}
		
		/* fetch interface issue*/
		ret = script_parser_fetch("csi0_para","csi_if", &dev->ccm_cfg[0]->interface , sizeof(int));
		if (ret) {
			csi_err("fetch csi_if from sys_config failed\n");
		}
		
		/* fetch power issue*/
	
		ret = script_parser_fetch("csi0_para","csi_iovdd", (int *)&dev->ccm_cfg[0]->iovdd_str , 32*sizeof(char));
		if (ret) {
			csi_err("fetch csi_iovdd from sys_config failed\n");
		} 
		
		ret = script_parser_fetch("csi0_para","csi_avdd", (int *)&dev->ccm_cfg[0]->avdd_str , 32*sizeof(char));
		if (ret) {
			csi_err("fetch csi_avdd from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_dvdd", (int *)&dev->ccm_cfg[0]->dvdd_str , 32*sizeof(char));
		if (ret) {
			csi_err("fetch csi_dvdd from sys_config failed\n");
		}
		
		/* fetch standby mode */
		ret = script_parser_fetch("csi0_para","csi_stby_mode", &dev->ccm_cfg[0]->stby_mode , sizeof(int));
		if (ret) {
			csi_err("fetch csi_stby_mode from sys_config failed\n");
		}
		/* fetch flip issue */
		ret = script_parser_fetch("csi0_para","csi_vflip", &dev->ccm_cfg[0]->vflip , sizeof(int));
		if (ret) {
			csi_err("fetch vflip from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_hflip", &dev->ccm_cfg[0]->hflip , sizeof(int));
		if (ret) {
			csi_err("fetch hflip from sys_config failed\n");
		}
		
		/* fetch flash light issue */
		ret = script_parser_fetch("csi0_para","csi_flash_pol", &dev->ccm_cfg[0]->flash_pol , sizeof(int));
		if (ret) {
			csi_err("fetch csi_flash_pol from sys_config failed\n");
		}
		
		/* fetch reset/power/standby/flash/af io issue */
		ret = script_parser_fetch("csi0_para","csi_reset", (int *)&dev->ccm_cfg[0]->reset_io , sizeof(user_gpio_set_t)/sizeof(int));
		if (ret) {
			csi_err("fetch csi_reset from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_stby", (int *)&dev->ccm_cfg[0]->standby_io , sizeof(user_gpio_set_t)/sizeof(int));
		if (ret) {
			csi_err("fetch csi_stby from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_power_en", (int *)&dev->ccm_cfg[0]->power_io , sizeof(user_gpio_set_t)/sizeof(int));
		if (ret) {
			csi_err("fetch csi_power_en from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_flash", (int *)&dev->ccm_cfg[0]->flash_io , sizeof(user_gpio_set_t)/sizeof(int));
		if (ret) {
			csi_err("fetch csi_flash from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_af_en", (int *)&dev->ccm_cfg[0]->af_power_io , sizeof(user_gpio_set_t)/sizeof(int));
		if (ret) {
			csi_err("fetch csi_af_en from sys_config failed\n");
		}
	} 
	
	if(dev->dev_qty > 1)
	{
		dev->ccm_cfg[1]->i2c_addr = i2c_addr_b;
		strcpy(dev->ccm_cfg[1]->ccm,ccm_b);
	
		/* fetch i2c and module name*/
		ret = script_parser_fetch("csi0_para","csi_twi_id_b", &dev->ccm_cfg[1]->twi_id , sizeof(int));
		if (ret) {
			csi_err("fetch csi_twi_id_b from sys_config failed\n");
		}
		
		ret = strcmp(dev->ccm_cfg[1]->ccm,"");
		if((dev->ccm_cfg[1]->i2c_addr == 0xff) && (ret == 0))	//when insmod without parm
		{
			ret = script_parser_fetch("csi0_para","csi_twi_addr_b", &dev->ccm_cfg[1]->i2c_addr , sizeof(int));
			if (ret) {
				csi_err("fetch csi_twi_addr_b from sys_config failed\n");
			}
			
			ret = script_parser_fetch("csi0_para","csi_mname_b", (int *)&dev->ccm_cfg[1]->ccm , I2C_NAME_SIZE*sizeof(char));
			if (ret) {
				csi_err("fetch csi_mname_b from sys_config failed\n");;
			}
		}
		
		/* fetch interface issue*/
		ret = script_parser_fetch("csi0_para","csi_if_b", &dev->ccm_cfg[1]->interface , sizeof(int));
		if (ret) {
			csi_err("fetch csi_if_b from sys_config failed\n");
		}
		
		/* fetch power issue*/
		ret = script_parser_fetch("csi0_para","csi_iovdd_b", (int *)&dev->ccm_cfg[1]->iovdd_str , 32*sizeof(char));
		if (ret) {
			csi_err("fetch csi_iovdd_b from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_avdd_b", (int *)&dev->ccm_cfg[1]->avdd_str , 32*sizeof(char));
		if (ret) {
			csi_err("fetch csi_avdd_b from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_dvdd_b", (int *)&dev->ccm_cfg[1]->dvdd_str , 32*sizeof(char));
		if (ret) {
			csi_err("fetch csi_dvdd_b from sys_config failed\n");
		}

		/* fetch standby mode */
		ret = script_parser_fetch("csi0_para","csi_stby_mode", &dev->ccm_cfg[1]->stby_mode , sizeof(int));
		if (ret) {
			csi_err("fetch csi_stby_mode from sys_config failed\n");
		}
		
		/* fetch flip issue */
		ret = script_parser_fetch("csi0_para","csi_vflip_b", &dev->ccm_cfg[1]->vflip , sizeof(int));
		if (ret) {
			csi_err("fetch vflip_b from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_hflip_b", &dev->ccm_cfg[1]->hflip , sizeof(int));
		if (ret) {
			csi_err("fetch hflip_b from sys_config failed\n");
		}
		
		/* fetch flash light issue */
		ret = script_parser_fetch("csi0_para","csi_flash_pol_b", &dev->ccm_cfg[1]->flash_pol , sizeof(int));
		if (ret) {
			csi_err("fetch csi_flash_pol_b from sys_config failed\n");
		}
		
		/* fetch reset/power/standby/flash/af io issue */
		ret = script_parser_fetch("csi0_para","csi_reset_b", (int *)&dev->ccm_cfg[1]->reset_io , sizeof(user_gpio_set_t)/sizeof(int));
		if (ret) {
			csi_err("fetch csi_reset_b from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_stby_b", (int *)&dev->ccm_cfg[1]->standby_io , sizeof(user_gpio_set_t)/sizeof(int));
		if (ret) {
			csi_err("fetch csi_stby_b from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_power_en_b", (int *)&dev->ccm_cfg[1]->power_io , sizeof(user_gpio_set_t)/sizeof(int));
		if (ret) {
			csi_err("fetch csi_power_en_b from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_flash_b", (int *)&dev->ccm_cfg[1]->flash_io , sizeof(user_gpio_set_t)/sizeof(int));
		if (ret) {
			csi_err("fetch csi_flash_b from sys_config failed\n");
		}
		
		ret = script_parser_fetch("csi0_para","csi_af_en_b", (int *)&dev->ccm_cfg[1]->af_power_io , sizeof(user_gpio_set_t)/sizeof(int));
		if (ret) {
			csi_err("fetch csi_af_en_b from sys_config failed\n");
		}
	}

	
	for(input_num=0; input_num<dev->dev_qty; input_num++)
	{
		csi_dbg(0,"dev->ccm_cfg[%d]->ccm = %s\n",input_num,dev->ccm_cfg[input_num]->ccm);
		csi_dbg(0,"dev->ccm_cfg[%d]->twi_id = %x\n",input_num,dev->ccm_cfg[input_num]->twi_id);
		csi_dbg(0,"dev->ccm_cfg[%d]->i2c_addr = %x\n",input_num,dev->ccm_cfg[input_num]->i2c_addr);
		csi_dbg(0,"dev->ccm_cfg[%d]->interface = %x\n",input_num,dev->ccm_cfg[input_num]->interface);
		csi_dbg(0,"dev->ccm_cfg[%d]->vflip = %x\n",input_num,dev->ccm_cfg[input_num]->vflip);
		csi_dbg(0,"dev->ccm_cfg[%d]->hflip = %x\n",input_num,dev->ccm_cfg[input_num]->hflip);
		csi_dbg(0,"dev->ccm_cfg[%d]->iovdd_str = %s\n",input_num,dev->ccm_cfg[input_num]->iovdd_str);
		csi_dbg(0,"dev->ccm_cfg[%d]->avdd_str = %s\n",input_num,dev->ccm_cfg[input_num]->avdd_str);
		csi_dbg(0,"dev->ccm_cfg[%d]->dvdd_str = %s\n",input_num,dev->ccm_cfg[input_num]->dvdd_str);
		csi_dbg(0,"dev->ccm_cfg[%d]->flash_pol = %x\n",input_num,dev->ccm_cfg[input_num]->flash_pol);
	}
#else
	dev->dev_qty = 1;
  dev->stby_mode = 0;
	dev->ccm_cfg[0] = &ccm_cfg[0]; 
	dev->ccm_cfg[0]->twi_id = 0;
	dev->ccm_cfg[0]->i2c_addr = 0x78;
  strcpy(dev->ccm_cfg[0]->ccm,"ov5640");
	dev->ccm_cfg[0]->interface = 0x0;
	dev->ccm_cfg[0]->stby_mode = 0;
	dev->ccm_cfg[0]->vflip = 0;
	dev->ccm_cfg[0]->hflip = 1;
#endif	
	return 0;
}
#endif

//add by heyihang.Jan 17, 2013
static int csi_gpio_release(int cnt)
{
  /* release gpio */
	script_item_u   *gpio_list=NULL;
	//int cnt;
	int m;
	
  csi_print("csi free gpio cnt=%d\n",cnt);
  m=script_get_pio_list("csi0_para",&gpio_list);
  if(m>cnt)
  {
    //printk("csi release gpio less than sys_config\n");
    csi_dbg(0,"csi release gpio less than sys_config\n");
    m=cnt;
  }
  
	while(m--)
	{
		gpio_free(gpio_list[m].gpio.gpio);
	}
  
  return 0;
}


static int csi_probe(struct platform_device *pdev)
{
	struct csi_dev *dev;
	struct resource *res;
	struct video_device *vfd;
	struct i2c_adapter *i2c_adap;
	int ret = 0;
	int input_num;
    script_item_u   *gpio_list=NULL;
	int cnt, i;

    //add by heyihang.Jan 24, 2013
    char *camera_list_para      = "camera_list_para";
    char *camera_list_para_used = "camera_list_para_used";
    script_item_value_type_e script_item_type;
    script_item_u value;
    
    //del by heyihang.Jan 15, 2013
    #if 0
	writel(0x33333333, 0xf1c20800+0x90);
	writel(0x33333333, 0xf1c20800+0x94);
    #endif
	
	csi_dbg(0,"csi_probe\n");
	/*request mem for dev*/	
	dev = kzalloc(sizeof(struct csi_dev), GFP_KERNEL);
	if (!dev) {
		csi_err("request dev mem failed!\n");
		return -ENOMEM;
	}
	dev->id = pdev->id;
	dev->pdev = pdev;

	spin_lock_init(&dev->slock);

	res = platform_get_resource(pdev, IORESOURCE_MEM, 0);
	if (!res) {
		csi_err("failed to find the registers\n");
		ret = -ENOENT;
		goto err_info;
	}

	dev->regs_res = request_mem_region(res->start, resource_size(res),
			dev_name(&pdev->dev));
	if (!dev->regs_res) {
		csi_err("failed to obtain register region\n");
		ret = -ENOENT;
		goto err_info;
	}

	dev->regs = ioremap(res->start, resource_size(res));
	if (!dev->regs) {
		csi_err("failed to map registers\n");
		ret = -ENXIO;
		goto err_req_region;
	}

  
  /*get irq resource*/

	res = platform_get_resource(pdev, IORESOURCE_IRQ, 0);
	if (!res) {
		csi_err("failed to get IRQ resource\n");
		ret = -ENXIO;
		goto err_regs_unmap;
	}

	dev->irq = res->start;

	ret = request_irq(dev->irq, csi_isr, 0, pdev->name, dev);
	if (ret) {
		csi_err("failed to install irq (%d)\n", ret);
		goto err_clk;
	}

    /*pin resource*/
	dev->csi_pin_hd = gpio_request_ex("csi0_para",NULL);
	if (dev->csi_pin_hd==-1) {
		csi_err("csi0 pin request error!\n");
		ret = -ENXIO;
		goto err_irq;
	}

    /* v4l2 device register */
	ret = v4l2_device_register(&pdev->dev, &dev->v4l2_dev);	
	if (ret) {
		csi_err("Error registering v4l2 device\n");
		goto err_irq;
		
	}

	dev_set_drvdata(&(pdev)->dev, (dev));
    
    //add by heyihang.Jan 17, 2013
    cnt = script_get_pio_list("csi0_para",&gpio_list);
	if (cnt==0) {
		csi_err("csi0 get pin list error!\n");
		ret = -ENXIO;
		goto err_irq;
	} else {
        /* request gpio */
        csi_dbg(0,"csi0 pin request...\n");
      	for(i = 0; i < cnt; i++)
      	{
            //printk("request gpio cnt=%d, i=%d\n", cnt, i);
      	    if(0 != gpio_request(gpio_list[i].gpio.gpio, NULL))
      	        csi_print("csi0 pin request error at %d\n",i);
      	}
  	    //dev->csi_pin_hd
      	dev->csi_pin_list=gpio_list;
  	    dev->csi_pin_cnt=i;//record pin request
      	/*config gpio*/
  	    if(0 != sw_gpio_setall_range(&gpio_list[0].gpio, cnt))
  	    {
  	        csi_err("sw_gpio_setall_range failed\n");
  	        //goto err_gpio;
  	    }
	}    
    
    
	/* fetch sys_config1 */

	ret = fetch_config(dev);
	if (ret) {
		csi_err("Error at fetch_config\n");
		goto err_irq;
	}

  /* v4l2 subdev register	*/
	dev->module_flag = 0;
	for(input_num=0; input_num<dev->dev_qty; input_num++)
	{	
//		if(dev->module_flag)
//			break;

		if(!strcmp(dev->ccm_cfg[input_num]->ccm,""))
			break;

		if(dev->module_flag) {
			dev->ccm_cfg[input_num]->sd = dev->ccm_cfg[input_num-1]->sd;
			csi_dbg(0,"num = %d , sd_0 = %p,sd_1 = %p\n",input_num,dev->ccm_cfg[input_num]->sd,dev->ccm_cfg[input_num-1]->sd);
			goto reg_sd;
		}

		if((dev->dev_qty > 1) && (input_num+1<dev->dev_qty))
		{
			if( (!strcmp(dev->ccm_cfg[input_num]->ccm,dev->ccm_cfg[input_num+1]->ccm)))
				dev->module_flag = 1;
		}

		i2c_adap = i2c_get_adapter(dev->ccm_cfg[input_num]->twi_id);

		if (i2c_adap == NULL) {
			csi_err("request i2c adapter failed,input_num = %d\n",input_num);
			ret = -EINVAL;
			goto free_dev;//linux-3.0
		}

		dev->ccm_cfg[input_num]->sd = kmalloc(sizeof(struct v4l2_subdev *),GFP_KERNEL);
		if (dev->ccm_cfg[input_num]->sd == NULL) {
			csi_err("unable to allocate memory for subdevice pointers,input_num = %d\n",input_num);
			ret = -ENOMEM;
			goto free_dev;//linux-3.0
		}

        //add by heyihang.Jan 24, 2013
        script_item_type = script_get_item(camera_list_para, camera_list_para_used, &value);
        if ((SCIRPT_ITEM_VALUE_TYPE_INT == script_item_type) && (value.val == 1)) {
                camera_export_info(dev->ccm_cfg[input_num]->ccm, &dev->ccm_cfg[input_num]->i2c_addr,input_num);
        }

		dev_sensor[input_num].addr = (unsigned short)(dev->ccm_cfg[input_num]->i2c_addr>>1);
		strcpy(dev_sensor[input_num].type,dev->ccm_cfg[input_num]->ccm);

		dev->ccm_cfg[input_num]->sd = v4l2_i2c_new_subdev_board(&dev->v4l2_dev,
											i2c_adap,
											//dev_sensor[input_num].type,//linux-3.0
											&dev_sensor[input_num],
											NULL);
reg_sd:					
		if (!dev->ccm_cfg[input_num]->sd) {
			csi_err("Error registering v4l2 subdevice,input_num = %d\n",input_num);
			goto free_dev;	
		} else{
			csi_print("registered sub device,input_num = %d\n",input_num);
		}
 
		dev->ccm_cfg[input_num]->ccm_info.mclk = CSI_OUT_RATE;
		dev->ccm_cfg[input_num]->ccm_info.vref = CSI_LOW;
		dev->ccm_cfg[input_num]->ccm_info.href = CSI_LOW;
		dev->ccm_cfg[input_num]->ccm_info.clock = CSI_FALLING;
		dev->ccm_cfg[input_num]->ccm_info.stby_mode = dev->stby_mode;
		
		ret = v4l2_subdev_call(dev->ccm_cfg[input_num]->sd,core,ioctl,CSI_SUBDEV_CMD_GET_INFO,&dev->ccm_cfg[input_num]->ccm_info);
		if (ret < 0)
		{
			csi_err("Error when get ccm info,input_num = %d,use default!\n",input_num);
		}

		dev->ccm_cfg[input_num]->ccm_info.iocfg = input_num;

		ret = v4l2_subdev_call(dev->ccm_cfg[input_num]->sd,core,ioctl,CSI_SUBDEV_CMD_SET_INFO,&dev->ccm_cfg[input_num]->ccm_info);
		if (ret < 0)
		{
			csi_err("Error when set ccm info,input_num = %d,use default!\n",input_num);
		}

		/*power issue*/
		dev->ccm_cfg[input_num]->iovdd = NULL;
		dev->ccm_cfg[input_num]->avdd = NULL;
		dev->ccm_cfg[input_num]->dvdd = NULL;
	
		if(strcmp(dev->ccm_cfg[input_num]->iovdd_str,"")) {
			dev->ccm_cfg[input_num]->iovdd = regulator_get(NULL, dev->ccm_cfg[input_num]->iovdd_str);
			if (IS_ERR(dev->ccm_cfg[input_num]->iovdd)) {
				csi_err("get regulator csi_iovdd error!input_num = %d\n",input_num);
				goto free_dev;
			}
		}

		if(strcmp(dev->ccm_cfg[input_num]->avdd_str,"")) {
			dev->ccm_cfg[input_num]->avdd = regulator_get(NULL, dev->ccm_cfg[input_num]->avdd_str);
			if (IS_ERR(dev->ccm_cfg[input_num]->avdd)) {
				csi_err("get regulator csi_avdd error!input_num = %d\n",input_num);
				goto free_dev;
			}
		}
	
		if(strcmp(dev->ccm_cfg[input_num]->dvdd_str,"")) {
			dev->ccm_cfg[input_num]->dvdd = regulator_get(NULL, dev->ccm_cfg[input_num]->dvdd_str);
			if (IS_ERR(dev->ccm_cfg[input_num]->dvdd)) {
				csi_err("get regulator csi_dvdd error!input_num = %d\n",input_num);
				goto free_dev;
			}
		}	
	}

	for(input_num=0; input_num<dev->dev_qty; input_num++)
	{
		csi_dbg(0,"dev->ccm_cfg[%d]->sd = %p\n",input_num,dev->ccm_cfg[input_num]->sd);
		csi_dbg(0,"dev->ccm_cfg[%d]->ccm_info = %p\n",input_num,&dev->ccm_cfg[input_num]->ccm_info);
		csi_dbg(0,"dev->ccm_cfg[%d]->ccm_info.iocfg = %d\n",input_num,dev->ccm_cfg[input_num]->ccm_info.iocfg);
		csi_dbg(0,"dev->ccm_cfg[%d]->ccm_info.vref = %d\n",input_num,dev->ccm_cfg[input_num]->ccm_info.vref);
		csi_dbg(0,"dev->ccm_cfg[%d]->ccm_info.href = %d\n",input_num,dev->ccm_cfg[input_num]->ccm_info.href);
		csi_dbg(0,"dev->ccm_cfg[%d]->ccm_info.clock = %d\n",input_num,dev->ccm_cfg[input_num]->ccm_info.clock);
		csi_dbg(0,"dev->ccm_cfg[%d]->ccm_info.mclk = %d\n",input_num,dev->ccm_cfg[input_num]->ccm_info.mclk);
		csi_dbg(0,"dev->ccm_cfg[%d]->iovdd = %p\n",input_num,dev->ccm_cfg[input_num]->iovdd);
		csi_dbg(0,"dev->ccm_cfg[%d]->avdd = %p\n",input_num,dev->ccm_cfg[input_num]->avdd);
		csi_dbg(0,"dev->ccm_cfg[%d]->dvdd = %p\n",input_num,dev->ccm_cfg[input_num]->dvdd);
	}

	update_ccm_info(dev, dev->ccm_cfg[0]);

	/*clock resource*/
	if (csi_clk_get(dev)) {
		csi_err("csi clock get failed!\n");
		ret = -ENXIO;
		goto unreg_dev;
	}
	
	/* power on and power off device */
	for(input_num=0; input_num<dev->dev_qty; input_num++)
	{
      ret = update_ccm_info(dev, dev->ccm_cfg[input_num]);
      if(ret<0)
      	csi_err("Error when set ccm info when probe!\n");
      
		if(dev->stby_mode == 1) {
			csi_print("power on and power off camera %d!\n",input_num);
			v4l2_subdev_call(dev->ccm_cfg[input_num]->sd,core, s_power, CSI_SUBDEV_PWR_ON);
			v4l2_subdev_call(dev->ccm_cfg[input_num]->sd,core, s_power, CSI_SUBDEV_PWR_OFF);
		} else {
			csi_print("power on and standy on camera %d!\n",input_num);
			csi_clk_out_set(dev);
            
            csi_dbg(0, "L%d \n", __LINE__);
            
			v4l2_subdev_call(dev->ccm_cfg[input_num]->sd,core, s_power, CSI_SUBDEV_PWR_ON);
            
            csi_dbg(0, "L%d \n", __LINE__);
            
			v4l2_subdev_call(dev->ccm_cfg[input_num]->sd,core, s_power, CSI_SUBDEV_STBY_OFF);

            csi_dbg(0, "L%d \n", __LINE__);
            
			v4l2_subdev_call(dev->ccm_cfg[input_num]->sd,core, s_power, CSI_SUBDEV_STBY_ON);
		}
	}
        
	//csi_dbg("%s(): csi-%d registered successfully\n",__FUNCTION__, dev->id);

	/*video device register	*/
	ret = -ENOMEM;
	vfd = video_device_alloc();
	if (!vfd) {
        csi_err("[L%d]call func video_device_alloc fail!!\n", __LINE__);
		goto err_clk;
	}	

	*vfd = csi_template;
	vfd->v4l2_dev = &dev->v4l2_dev;

	dev_set_name(&vfd->dev, "csi-0");
	ret = video_register_device(vfd, VFL_TYPE_GRABBER, video_nr);
	if (ret < 0) {
        csi_err("[L%d]call func video_register_device fail!!\n", __LINE__);
		goto rel_vdev;
	}	
    
	video_set_drvdata(vfd, dev);

	/*add device list*/
	/* Now that everything is fine, let's add it to device list */
	list_add_tail(&dev->csi_devlist, &csi_devlist);

	if (video_nr != -1) {
		video_nr++;
	}
	dev->vfd = vfd;

	csi_print("V4L2 device registered as %s\n",video_device_node_name(vfd));

	/*initial video buffer queue*/
	videobuf_queue_dma_contig_init(&dev->vb_vidq, &csi_video_qops,
			NULL, &dev->slock, V4L2_BUF_TYPE_VIDEO_CAPTURE,
			V4L2_FIELD_NONE,//default format, can be changed by s_fmt
			sizeof(struct csi_buffer), dev,NULL);//linux-3.0
	
	/* init video dma queues */
	INIT_LIST_HEAD(&dev->vidq.active);
	//init_waitqueue_head(&dev->vidq.wq);
	
	/* initial state */
	dev->capture_mode = V4L2_MODE_VIDEO;
	
#if 0
{
        void __iomem *regs_dram;
        volatile unsigned int tmpval = 0;
        volatile unsigned int tmpval_csi = 0;
        
        printk("Warning: we write the DRAM priority directely here, it will be fixed in the next version.");
        regs_dram = ioremap(SDRAM_REGS_pBASE, 0x2E0);
        
        tmpval = readl(regs_dram + 0x250 + 4 * 16);
        printk("cpu host port: %x\n", tmpval);
        tmpval |= 3 << 2;
        printk("cpu priority: %d\n", tmpval);
        
        tmpval_csi = readl(regs_dram + 0x250 + 4 * 20);         // csi0
        printk("csi0 host port: %x\n", tmpval_csi);
        tmpval_csi |= (!(3<<2));
        tmpval_csi |= tmpval;
        writel(tmpval_csi, regs_dram + 0x250 + 4 * 20);
        printk("csi0 host port(after): %x\n", tmpval_csi);
        
        tmpval_csi = readl(regs_dram + 0x250 + 4 * 27);         // csi1
        printk("csi1 host port: %x\n", tmpval_csi);
        tmpval_csi |= (!(3<<2));
        tmpval_csi |= tmpval;
        writel(tmpval_csi, regs_dram + 0x250 + 4 * 27);
        printk("csi1 host port(after): %x\n", tmpval_csi);
        
        iounmap(regs_dram);
}
#endif // AJUST_DRAM_PRIORITY

	return 0;

rel_vdev:
	video_device_release(vfd);
err_clk:
	csi_clk_release(dev);	
unreg_dev:
	v4l2_device_unregister(&dev->v4l2_dev);	
free_dev:
//	kfree(dev);
err_irq:
	free_irq(dev->irq, dev);
err_regs_unmap:
	iounmap(dev->regs);
err_req_region:
	release_resource(dev->regs_res);
	kfree(dev->regs_res);
err_info:
	kfree(dev);
	csi_err("failed to install\n");

	return ret;
}

void csi_dev_release(struct device *dev)
{
}

static int csi_release(void)
{
	struct csi_dev *dev;
	struct list_head *list;

	csi_dbg(0,"csi_release\n");
	while (!list_empty(&csi_devlist)) 
	{
		list = csi_devlist.next;
		list_del(list);
		dev = list_entry(list, struct csi_dev, csi_devlist);

		v4l2_info(&dev->v4l2_dev, "unregistering %s\n", video_device_node_name(dev->vfd));
		video_unregister_device(dev->vfd);
		csi_clk_release(dev);	
		v4l2_device_unregister(&dev->v4l2_dev);
		free_irq(dev->irq, dev);
		iounmap(dev->regs);
		release_resource(dev->regs_res);
		kfree(dev->regs_res);
		kfree(dev);

        //add by heyihang.Jan 17, 2013
        csi_gpio_release(dev->csi_pin_cnt);
	}
	
	csi_print("csi_release ok!\n");
	return 0;
}

static int __devexit csi_remove(struct platform_device *pdev)
{
	struct csi_dev *dev;
	dev=(struct csi_dev *)dev_get_drvdata(&(pdev)->dev);
	
	csi_dbg(0,"csi_remove\n");
	
//	video_device_release(vfd);
//	csi_clk_release(dev);
//	free_irq(dev->irq, dev);
//	
//	iounmap(dev->regs);
//	release_resource(dev->regs_res);
//	kfree(dev->regs_res);
//	kfree(dev);
	csi_print("csi_remove ok!\n");
	return 0;
}

static int csi_suspend(struct platform_device *pdev, pm_message_t state)
{
	struct csi_dev *dev=(struct csi_dev *)dev_get_drvdata(&(pdev)->dev);
	int ret,input_num;
	
	csi_print("csi_suspend\n");
	
	if (dev->opened==1) {		
		csi_clk_disable(dev);

		if(dev->stby_mode == 0) {
			csi_print("set camera to standby!\n");	
			return v4l2_subdev_call(dev->sd,core, s_power, CSI_SUBDEV_STBY_ON);
		} else {
			csi_print("set camera to power off!\n");
			//close all the device power	
			for (input_num=0; input_num<dev->dev_qty; input_num++) {
        /* update target device info and select it */
        ret = update_ccm_info(dev, dev->ccm_cfg[input_num]);
        if (ret < 0)
				{
					csi_err("Error when set ccm info when csi_suspend!\n");
				}
		
				ret = v4l2_subdev_call(dev->sd,core, s_power, CSI_SUBDEV_PWR_OFF);
			  if (ret!=0) {
			  	csi_err("sensor power off error at device number %d when csi_suspend!\n",input_num);
			  	return ret;
			  }
			}
		}
	}
	return 0;
}

static int csi_resume(struct platform_device *pdev)
{
	int ret,input_num;
	struct csi_dev *dev=(struct csi_dev *)dev_get_drvdata(&(pdev)->dev);
	
	csi_print("csi_resume\n");

	if (dev->opened==1) {
		csi_clk_out_set(dev);
//		csi_clk_enable(dev);
		if(dev->stby_mode == 0) {
			ret = v4l2_subdev_call(dev->sd,core, s_power,CSI_SUBDEV_STBY_OFF);
			if(ret < 0)
				return ret;
			ret = v4l2_subdev_call(dev->sd,core, init, 0);
			if (ret!=0) {
				csi_err("sensor initial error when resume from suspend!\n");
				return ret;
			} else {
				csi_print("sensor initial success when resume from suspend!\n");
			}
		} else {
			//open all the device power
			for (input_num=0; input_num<dev->dev_qty; input_num++) {
        /* update target device info and select it */
        ret = update_ccm_info(dev, dev->ccm_cfg[input_num]);
        if (ret < 0)
				{
					csi_err("Error when set ccm info when csi_resume!\n");
				}
					
				ret = v4l2_subdev_call(dev->sd,core, s_power, CSI_SUBDEV_PWR_ON);
			  if (ret!=0) {
			  	csi_err("sensor power on error at device number %d when csi_resume!\n",input_num);
			  }
			}
			
			/* update target device info and select it */
			ret = update_ccm_info(dev, dev->ccm_cfg[0]);
			if (ret < 0)
			{
				csi_err("Error when set ccm info when csi_resume!\n");
			}
		
			ret = v4l2_subdev_call(dev->sd,core, init,0);
			if (ret!=0) {
				csi_err("sensor full initial error when resume from suspend!\n");
				return ret;
			} else {
				csi_print("sensor full initial success when resume from suspend!\n");
			}
		}
	} 		
	// add by lmm 
	for (input_num=0; input_num<dev->dev_qty; input_num++)
		{
			if(strcmp(dev->ccm_cfg[input_num]->iovdd_str,"") && dev->ccm_cfg[input_num]->vol_iovdd >0)
			{
				dev->iovdd=regulator_get(NULL,dev->ccm_cfg[input_num]->iovdd_str);
				if(IS_ERR(dev->iovdd))
					csi_err("get iovdd fail\n");
				
				ret=regulator_set_voltage(dev->iovdd,dev->ccm_cfg[input_num]->vol_iovdd*1000,dev->ccm_cfg[input_num]->vol_iovdd*1000);
				if(ret)
				{
					csi_err("unable to set voltage for iovdd \n");
				}
				
				ret=regulator_enable(dev->iovdd);
				
				if(ret)
				{
					csi_err("unable to enable iovdd \n");	

				}
					
			}

			if(strcmp(dev->ccm_cfg[input_num]->avdd_str,"") && dev->ccm_cfg[input_num]->vol_avdd >0)
			{
				dev->avdd=regulator_get(NULL,dev->ccm_cfg[input_num]->avdd_str);
				if(IS_ERR(dev->avdd))
					csi_err("get avdd fail\n");
				
				ret=regulator_set_voltage(dev->avdd,dev->ccm_cfg[input_num]->vol_avdd*1000,dev->ccm_cfg[input_num]->vol_avdd*1000);
				if(ret)
				{
					csi_err(" unable to set voltage for avdd\n");
				}
				
				ret=regulator_enable(dev->avdd);
				if(ret)
				{
					csi_err("unable to enable avdd \n");	
				}
			}
			
			if(strcmp(dev->ccm_cfg[input_num]->dvdd_str,"") && dev->ccm_cfg[input_num]->vol_dvdd >0)
			{
				dev->dvdd=regulator_get(NULL,dev->ccm_cfg[input_num]->dvdd_str);
				if(IS_ERR(dev->dvdd))
					csi_err("get dvdd fail\n");
				
				ret=regulator_set_voltage(dev->dvdd,dev->ccm_cfg[input_num]->vol_dvdd*1000,dev->ccm_cfg[input_num]->vol_dvdd*1000);
				if(ret)
				{
					csi_err("unable to set voltage for dvdd \n");
				}
				
				ret=regulator_enable(dev->dvdd);
				
				if(ret)
				{
					csi_err("unable to enable dvdd \n");	

				}
					
			}

		}
	//add end
	return 0;
}


static struct platform_driver csi_driver = {
	.probe		= csi_probe,
	.remove		= __devexit_p(csi_remove),
	.suspend	= csi_suspend,
	.resume		= csi_resume,
	//.id_table	= csi_driver_ids,
	.driver = {
		.name	= "sunxi_csi0",
		.owner	= THIS_MODULE,
	}
};

static struct resource csi0_resource[] = {
	[0] = {
		.start	= CSI0_REGS_BASE,
		.end	= CSI0_REGS_BASE + CSI0_REG_SIZE - 1,
		.flags	= IORESOURCE_MEM,
	},
	[1] = {
		.start	= SW_INTC_IRQNO_CSI0,
		.end	= SW_INTC_IRQNO_CSI0,
		.flags	= IORESOURCE_IRQ,
	},
};

static struct platform_device csi_device[] = {
	[0] = {
	.name           	= "sunxi_csi0",
        .id             	= 0,
	.num_resources		= ARRAY_SIZE(csi0_resource),
        .resource       	= csi0_resource,
	.dev.release            = csi_dev_release,
	}
};

static int __init csi_init(void)
{
        u32 ret;
	int csi_used;
	script_item_u   val;
        script_item_value_type_e  type;
	csi_print("Welcome to CSI driver\n");
	csi_print("csi_init\n");
    
	type = script_get_item("csi0_para","csi_used", &val);
	if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
		csi_err("fetch csi_used from sys_config failed\n");
		return -1;
	} else {
        csi_used=val.val;
	}

	if(!csi_used)
	{
		csi_err("csi_used=0,csi driver is not enabled!\n");
		return 0;
	}
	
	ret = platform_driver_register(&csi_driver);

	if (ret) {
		csi_err("platform driver register failed\n");
		return -1;
	}

	ret = platform_device_register(&csi_device[0]);
	if (ret) {
		csi_err("platform device register failed\n");
		return -1;
	}
	return 0;
}

static void __exit csi_exit(void)
{
	int csi_used;
	script_item_u   val;
        script_item_value_type_e  type;
	csi_print("csi_exit\n");

	type = script_get_item("csi0_para","csi_used", &val);
	if (SCIRPT_ITEM_VALUE_TYPE_INT != type) {
		csi_err("fetch csi_used from sys_config failed\n");
		return;
	} else {
                csi_used=val.val;
	}
	
	if(csi_used)
	{
		csi_release();
		platform_device_unregister(&csi_device[0]);
		platform_driver_unregister(&csi_driver);
	}
}

module_init(csi_init);
module_exit(csi_exit);

MODULE_AUTHOR("raymonxiu");
MODULE_LICENSE("Dual BSD/GPL");
MODULE_DESCRIPTION("CSI driver for sunxi");
