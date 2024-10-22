/*
*********************************************************************************************************
* File    : dram_init.c
* By      : Berg.Xing
* Date    : 2011-12-07
* Descript: dram for AW1625 chipset
* Update  : date                auther      ver     notes
*           2011-12-07          Berg        1.0     create file from A10
*           2012-01-11          Berg        1.1     kill bug for 1/2 rank decision
*           2012-01-31          Berg        1.2     kill bug for clock frequency > 600MHz
*           2013-03-06          CPL         1.3     modify for A20
*						2013-06-24			YeShaozhen			1.15		add spread spectrum funciton, use dram_tpr4[2] configure
*						2013-06-28			YeShaozhen			1.16		DRAM frequency jump, use dram_tpr4[3] configure
						2014-05-20			liuke				1.17		modify code for read ODT support,para->dram_trp5[1:0] to indicate DQS/DQ odt disable 
*********************************************************************************************************
*/
#include "dram_i.h"

/******************************************************************************/
/*                              file head of Boot                             */
/******************************************************************************/
typedef struct _Boot_file_head
{
        __u32  jump_instruction;   // one intruction jumping to real code
        __u8   magic[8];           // ="eGON.BT0" or "eGON.BT1",  not C-style string.
        __u32  check_sum;          // generated by PC
        __u32  length;             // generated by PC
        __u32  pub_head_size;      // the size of boot_file_head_t
        __u8   pub_head_vsn[4];    // the version of boot_file_head_t
        __u8   file_head_vsn[4];   // the version of boot0_file_head_t or boot1_file_head_t
        __u8   Boot_vsn[4];        // Boot version
        __u8   eGON_vsn[4];        // eGON version
        __u8   platform[8];        // platform information
}boot_file_head_t;


typedef struct _boot_para_info_t
{
    __u8   blkmagic[16];          // "ePDK-Magic-Block", not C-style string.
    __u8   magic[8];
    __u8   eGON_vsn[4];           // eGON version
        __u8   Boot_vsn[4];           // Boot version
    __u32  reserved[20];
}boot_para_info_t;

//������?��?��?o��GPIO?��1?��?��y?Y?��11
typedef struct _normal_gpio_cfg
{
    __u8      port;                       //???��o?
    __u8      port_num;                   //???��?������o?
    __s8      mul_sel;                    //1|?������o?
    __s8      pull;                       //��?�������䨬?
    __s8      drv_level;                  //?y?��?y?��?����|
    __s8      data;                       //��?3?��???
    __u8      reserved[2];                //���ꨢ???��?����?��????
}
normal_gpio_cfg;

/******************************************************************************/
/*                              file head of Boot0                            */
/******************************************************************************/
typedef struct _boot0_private_head_t
{
        __u32                       prvt_head_size;
        char                        prvt_head_vsn[4];       // the version of boot0_private_head_t
        standy_dram_para_t          dram_para;              // DRAM patameters for initialising dram. Original values is arbitrary,
        __s32                                           uart_port;              // UART?????�¡���o?
        normal_gpio_cfg             uart_ctrl[2];           // UART?????��(�̡¨�?�䨰��??��)��y?YD??��
        __s32                       enable_jtag;            // 1 : enable,  0 : disable
    normal_gpio_cfg                 jtag_gpio[5];           // �����?JTAG��?��?2?GPIOD??��
    normal_gpio_cfg             storage_gpio[32];       // ��?��騦����? GPIOD??��
    char                        storage_data[512 - sizeof(normal_gpio_cfg) * 32];      // ��??����ꨢ?��y?YD??��
    //boot_nand_connect_info_t    nand_connect_info;
}boot0_private_head_t;


typedef struct _boot0_file_head_t
{
        boot_file_head_t      boot_head;
        boot0_private_head_t  prvt_head;
}boot0_file_head_t;




/*
*********************************************************************************************************
*                                   DRAM INIT
*
* Description: dram init function
*
* Arguments  : para     dram config parameter
*
*
* Returns    : result
*
* Note       :
*********************************************************************************************************
*/
void mctl_ddr3_reset(void)
{
    __u32 reg_val;

    reg_val = mctl_read_w(SDR_CR);
    reg_val &= ~(0x1<<12);
    mctl_write_w(SDR_CR, reg_val);
    mem_delay(0x100);
    reg_val = mctl_read_w(SDR_CR);
    reg_val |= (0x1<<12);
    mctl_write_w(SDR_CR, reg_val);
}

void mctl_set_drive(void)
{
    __u32 reg_val;

    reg_val = mctl_read_w(SDR_CR);
    reg_val |= (0x6<<12);
    reg_val |= 0xFFC;
    reg_val &= ~0x3;
    reg_val &= ~(0x3<<28);
    //  reg_val |= 0x7<<20;
    mctl_write_w(SDR_CR, reg_val);
}

void mctl_itm_disable(void)
{
    __u32 reg_val = 0x0;

    reg_val = mctl_read_w(SDR_CCR);
    reg_val |= 0x1<<28;
    reg_val &= ~(0x1U<<31);          //danielwang, 2012-05-18
    mctl_write_w(SDR_CCR, reg_val);
}

void mctl_itm_enable(void)
{
    __u32 reg_val = 0x0;

    reg_val = mctl_read_w(SDR_CCR);
    reg_val &= ~(0x1<<28);
    mctl_write_w(SDR_CCR, reg_val);
}

void mctl_enable_dll0(__u32 phase)
{
    mctl_write_w(SDR_DLLCR0, (mctl_read_w(SDR_DLLCR0) & ~(0x3f<<6)) | (((phase>>16)&0x3f)<<6));
    mctl_write_w(SDR_DLLCR0, (mctl_read_w(SDR_DLLCR0) & ~0x40000000) | 0x80000000);

    //mctl_delay(0x100);
    delay_us(10);

    mctl_write_w(SDR_DLLCR0, mctl_read_w(SDR_DLLCR0) & ~0xC0000000);

    //mctl_delay(0x1000);
    delay_us(10);

    mctl_write_w(SDR_DLLCR0, (mctl_read_w(SDR_DLLCR0) & ~0x80000000) | 0x40000000);
    //mctl_delay(0x1000);
    delay_us(100);
}

void mctl_enable_dllx(__u32 phase)
{
    __u32 i = 0;
    __u32 reg_val;
    __u32 dll_num;
    __u32   dqs_phase = phase;

    reg_val = mctl_read_w(SDR_DCR);
    reg_val >>=6;
    reg_val &= 0x7;
    if(reg_val == 3)
        dll_num = 5;
    else
        dll_num = 3;

    for(i=1; i<dll_num; i++)
    {
        mctl_write_w(SDR_DLLCR0+(i<<2), (mctl_read_w(SDR_DLLCR0+(i<<2)) & ~(0xf<<14)) | ((dqs_phase&0xf)<<14));
        mctl_write_w(SDR_DLLCR0+(i<<2), (mctl_read_w(SDR_DLLCR0+(i<<2)) & ~0x40000000) | 0x80000000);
        dqs_phase = dqs_phase>>4;
    }

    //mctl_delay(0x100);
    delay_us(10);

    for(i=1; i<dll_num; i++)
    {
        mctl_write_w(SDR_DLLCR0+(i<<2), mctl_read_w(SDR_DLLCR0+(i<<2)) & ~0xC0000000);
    }

    //mctl_delay(0x1000);
    delay_us(10);

    for(i=1; i<dll_num; i++)
    {
        mctl_write_w(SDR_DLLCR0+(i<<2), (mctl_read_w(SDR_DLLCR0+(i<<2)) & ~0x80000000) | 0x40000000);
    }
    //mctl_delay(0x1000);
    delay_us(100);
}

void mctl_disable_dll(void)
{
    __u32 reg_val;

    reg_val = mctl_read_w(SDR_DLLCR0);
    reg_val &= ~(0x1<<30);
    reg_val |= 0x1U<<31;
    mctl_write_w(SDR_DLLCR0, reg_val);

    reg_val = mctl_read_w(SDR_DLLCR1);
    reg_val &= ~(0x1<<30);
    reg_val |= 0x1U<<31;
    mctl_write_w(SDR_DLLCR1, reg_val);

    reg_val = mctl_read_w(SDR_DLLCR2);
    reg_val &= ~(0x1<<30);
    reg_val |= 0x1U<<31;
    mctl_write_w(SDR_DLLCR2, reg_val);

    reg_val = mctl_read_w(SDR_DLLCR3);
    reg_val &= ~(0x1<<30);
    reg_val |= 0x1U<<31;
    mctl_write_w(SDR_DLLCR3, reg_val);

    reg_val = mctl_read_w(SDR_DLLCR4);
    reg_val &= ~(0x1<<30);
    reg_val |= 0x1U<<31;
    mctl_write_w(SDR_DLLCR4, reg_val);
}

__u32 hpcr_value[32] = {
    0x00000301,0x00000301,0x00000301,0x00000301,
    0x00000301,0x00000301,0x00000301,0x00000301,
    0x0,       0x0,       0x0,       0x0,
    0x0,       0x0,       0x0,       0x0,
    0x00001031,0x00001031,0x00000735,0x00001035,
    0x00001035,0x00000731,0x00001031,0x00000735,
    0x00001035,0x00001031,0x00000731,0x00001035,
    0x00001031,0x00000301,0x00000301,0x00000731,
};
void mctl_configure_hostport(void)
{
    __u32 i;

    for(i=0; i<8; i++)
    {
        mctl_write_w(SDR_HPCR + (i<<2), hpcr_value[i]);
    }
    
    for(i=16; i<28; i++)
    {
        mctl_write_w(SDR_HPCR + (i<<2), hpcr_value[i]);
    }   
    
    mctl_write_w(SDR_HPCR + (29<<2), hpcr_value[i]);
    mctl_write_w(SDR_HPCR + (31<<2), hpcr_value[i]);
}


void mctl_setup_dram_clock(__u32 clk, __u32 pll_tun_en)
{
    __u32 reg_val;
    
    if(pll_tun_en)
    {
	      //spread spectrum
		    reg_val	= 0x00;
		    reg_val	|= (0x0<<17);	//spectrum freq
		    reg_val	|= 0x3333;	//BOT
		    reg_val	|= (0x113<<20);	//STEP
		    reg_val	|= (0x2<<29);	//MODE Triangular
		    mctl_write_w(DRAM_CCM_SDRAM_PLL_TUN2, reg_val);
		    reg_val	|= (0x1<<31);	//enable spread spectrum
		    mctl_write_w(DRAM_CCM_SDRAM_PLL_TUN2, reg_val);
  	}
  	else
  	{
  	    mctl_write_w(DRAM_CCM_SDRAM_PLL_TUN2, 0x00000000);
  	}

    //setup DRAM PLL
    reg_val = mctl_read_w(DRAM_CCM_SDRAM_PLL_REG);
    
    //DISABLE PLL before configuration by yeshaozhen at 20130711
    reg_val &= ~(0x1<<31);  	//PLL disable before configure
    mctl_write_w(DRAM_CCM_SDRAM_PLL_REG, reg_val);
    delay_us(20);
    //20130711 end
    
    reg_val &= ~0x3;
    reg_val |= 0x1;                                             //m factor
    reg_val &= ~(0x3<<4);
    reg_val |= 0x1<<4;                                          //k factor
    reg_val &= ~(0x1f<<8);
    reg_val |= ((clk/24)&0x1f)<<8;                              //n factor
    reg_val &= ~(0x3<<16);
    reg_val |= 0x1<<16;                                         //p factor
    reg_val &= ~(0x1<<29);                                      //clock output disable
    reg_val |= (__u32)0x1<<31;                                  //PLL En
    mctl_write_w(DRAM_CCM_SDRAM_PLL_REG, reg_val);
    //mctl_delay(0x100000);
    delay_us(10000);
    reg_val = mctl_read_w(DRAM_CCM_SDRAM_PLL_REG);
    reg_val |= 0x1<<29;
    mctl_write_w(DRAM_CCM_SDRAM_PLL_REG, reg_val);

    //setup MBUS clock
    reg_val = (0x1U<<31) | (0x1<<24) | (0x1<<0) | (0x1<<16);
    mctl_write_w(DRAM_CCM_MUS_CLK_REG, reg_val);

    //open DRAMC AHB & DLL register clock
    //close it first
    reg_val = mctl_read_w(DRAM_CCM_AHB_GATE_REG);
    reg_val &= ~(0x3<<14);
    mctl_write_w(DRAM_CCM_AHB_GATE_REG, reg_val);
    delay_us(10);
    //then open it
    reg_val |= 0x3<<14;
    mctl_write_w(DRAM_CCM_AHB_GATE_REG, reg_val);
    delay_us(10);
    
}

__s32 DRAMC_init(__dram_para_t *para)
{
    __u32 reg_val;
	__u32 hold_flag = 0;
    __u8  reg_value;
    __s32 ret_val;  

    //check input dram parameter structure
    if(!para)
    {
        //dram parameter is invalid
        return 0;
    }
    
    //close AHB clock for DRAMC,--added by yeshaozhen at 20130708
    reg_val = mctl_read_w(DRAM_CCM_AHB_GATE_REG);
    reg_val &= ~(0x3<<14);
    mctl_write_w(DRAM_CCM_AHB_GATE_REG, reg_val);
		delay_us(10);
		//20130708 AHB close end
    
    //setup DRAM relative clock
		if(para->dram_tpr4 & 0x4)
			mctl_setup_dram_clock(para->dram_clk,1);	//PLL tunning enable
		else
			mctl_setup_dram_clock(para->dram_clk,0);	


    mctl_set_drive();

    //dram clock off
    DRAMC_clock_output_en(0);


    mctl_itm_disable();
    
    if(para->dram_clk > 300)	//enable DLL when feq is more than 300M
    	mctl_enable_dll0(para->dram_tpr3);
    else
			mctl_disable_dll();

    //configure external DRAM
    reg_val = 0;
    if(para->dram_type == 3)
        reg_val |= 0x1;
    reg_val |= (para->dram_io_width>>3) <<1;

    if(para->dram_chip_density == 256)
        reg_val |= 0x0<<3;
    else if(para->dram_chip_density == 512)
        reg_val |= 0x1<<3;
    else if(para->dram_chip_density == 1024)
        reg_val |= 0x2<<3;
    else if(para->dram_chip_density == 2048)
        reg_val |= 0x3<<3;
    else if(para->dram_chip_density == 4096)
        reg_val |= 0x4<<3;
    else if(para->dram_chip_density == 8192)
        reg_val |= 0x5<<3;
    else
        reg_val |= 0x0<<3;
    reg_val |= ((para->dram_bus_width>>3) - 1)<<6;
    reg_val |= (para->dram_rank_num -1)<<10;
    reg_val |= 0x1<<12;
    reg_val |= ((0x1)&0x3)<<13;
    mctl_write_w(SDR_DCR, reg_val);

    //SDR_ZQCR1 set bit24 to 1
    reg_val  = mctl_read_w(SDR_ZQCR1);
    reg_val |= (0x1<<24) | (0x1<<1);
    if(para->dram_tpr4 & 0x2)
    {
        reg_val &= ~((0x1<<24) | (0x1<<1));
    }    
    mctl_write_w(SDR_ZQCR1, reg_val);
    //read odt enable;  liuke edit for read odt support,20140518
    reg_val = mctl_read_w(SDR_ODTCR);
    reg_val &=~(0xff);
    if(para->dram_odt_en)						//for odt enable
    {
    	reg_val |= (1<<0);						//1 rank config
    	if (para->dram_rank_num == 2)	//2 ranks config
    		reg_val |= (1<<4);
    }
    mctl_write_w(SDR_ODTCR, reg_val);

    //dram clock on
    DRAMC_clock_output_en(1);
    
    hold_flag = mctl_read_w(SDR_DPCR);
    if(hold_flag == 0) //normal branch
    {
        //set odt impendance divide ratio
        reg_val=((para->dram_zq)>>8)&0xfffff;
        reg_val |= ((para->dram_zq)&0xff)<<20;
        reg_val |= (para->dram_zq)&0xf0000000;
        reg_val |= (0x1u<<31);
        mctl_write_w(SDR_ZQCR0, reg_val);
        
        while( !((mctl_read_w(SDR_ZQSR)&(0x1u<<31))) );

	}            
        //Set CKE Delay to about 1ms
        reg_val = mctl_read_w(SDR_IDCR);
        reg_val |= 0x1ffff;
        mctl_write_w(SDR_IDCR, reg_val);
	
//      //dram clock on
//      DRAMC_clock_output_en(1);
    //reset external DRAM when CKE is Low
    //reg_val = mctl_read_w(SDR_DPCR);
    if(hold_flag == 0) //normal branch
    {
        //reset ddr3
        mctl_ddr3_reset();
    }
    else
    {
        //setup the DDR3 reset pin to high level
        reg_val = mctl_read_w(SDR_CR);
        reg_val |= (0x1<<12);
        mctl_write_w(SDR_CR, reg_val);
    }
    
    mem_delay(0x10);
    while(mctl_read_w(SDR_CCR) & (0x1U<<31)) {};

		if(para->dram_clk > 300)	//enable DLL when feq is more than 300M
    	mctl_enable_dllx(para->dram_tpr3);
    //set I/O configure register
//    reg_val = 0x00cc0000;
//   reg_val |= (para->dram_odt_en)&0x3;
//  reg_val |= ((para->dram_odt_en)&0x3)<<30;
//    mctl_write_w(SDR_IOCR, reg_val);

    //liuke edit for read odt support,20140518
    if(para->dram_odt_en)
    {
    reg_val = (0x3u<<30) | (0xcc<<16) | (0x3<<0);			//DQ and DQS DQ pins odt on for default
    reg_val &= ~((para->dram_tpr5 & 0x3)<<30 | (para->dram_tpr5 & 0x3));	//dram_tpr5 bit1:0 for DQS/DQ odt disable
    mctl_write_w(SDR_IOCR, reg_val);
    }
    //set refresh period
    DRAMC_set_autorefresh_cycle(para->dram_clk);

    //set timing parameters
    mctl_write_w(SDR_TPR0, para->dram_tpr0);
    mctl_write_w(SDR_TPR1, para->dram_tpr1);
    mctl_write_w(SDR_TPR2, para->dram_tpr2);

    //set mode register
    if(para->dram_type==3)                          //ddr3
    {
        reg_val = 0x1<<12;
        reg_val |= (para->dram_cas - 4)<<4;
        reg_val |= 0x5<<9;
    }
    else if(para->dram_type==2)                 //ddr2
    {
        reg_val = 0x2;
        reg_val |= para->dram_cas<<4;
        reg_val |= 0x5<<9;
    }
    mctl_write_w(SDR_MR, reg_val);

    mctl_write_w(SDR_EMR, para->dram_emr1);
    mctl_write_w(SDR_EMR2, para->dram_emr2);
    mctl_write_w(SDR_EMR3, para->dram_emr3);

    //set DQS window mode
    reg_val = mctl_read_w(SDR_CCR);
    reg_val |= 0x1U<<14;
    reg_val &= ~(0x1U<<17);
        //2T & 1T mode 
    if(para->dram_tpr4 & 0x1)
    {
        reg_val |= 0x1<<5;
    }
    mctl_write_w(SDR_CCR, reg_val);

    //initial external DRAM
    reg_val = mctl_read_w(SDR_CCR);
    reg_val |= 0x1U<<31;
    mctl_write_w(SDR_CCR, reg_val);

    while(mctl_read_w(SDR_CCR) & (0x1U<<31)) {};
    
//  while(1);

    //setup zq calibration manual
    //reg_val = mctl_read_w(SDR_DPCR);
    if(hold_flag == 1)
    {

//      super_standby_flag = 1;

    	//disable auto-fresh			//by cpl 2013-5-6
    	reg_val = mctl_read_w(SDR_DRR);
    	reg_val |= 0x1U<<31;
    	mctl_write_w(SDR_DRR, reg_val);


        reg_val = mctl_read_w(SDR_GP_REG0);
        reg_val &= 0x000fffff;
        mctl_write_w(SDR_GP_REG0, reg_val);
        //reg_val |= 0x17b00000;
        reg_val |= (0x1<<28) | (para->dram_zq<<20);
        mctl_write_w(SDR_ZQCR0, reg_val);
        
        //03-08
        reg_val = mctl_read_w(SDR_DCR);
        reg_val &= ~(0x1fU<<27);
        reg_val |= 0x12U<<27;
        mctl_write_w(SDR_DCR, reg_val);
        while( mctl_read_w(SDR_DCR)& (0x1U<<31) );
        
        mem_delay(0x100);

        //dram pad hold off
        mctl_write_w(SDR_DPCR, 0x16510000);
        
        while(mctl_read_w(SDR_DPCR) & 0x1){}        
                
        //exit self-refresh state
        reg_val = mctl_read_w(SDR_DCR);
        reg_val &= ~(0x1fU<<27);
        reg_val |= 0x17U<<27;
        mctl_write_w(SDR_DCR, reg_val);
    
        //check whether command has been executed
        while( mctl_read_w(SDR_DCR)& (0x1U<<31) );
    	//disable auto-fresh			//by cpl 2013-5-6
    	reg_val = mctl_read_w(SDR_DRR);
    	reg_val &= ~(0x1U<<31);
    	mctl_write_w(SDR_DRR, reg_val);

        mem_delay(0x100);;
    
//        //issue a refresh command
//        reg_val = mctl_read_w(SDR_DCR);
//        reg_val &= ~(0x1fU<<27);
//        reg_val |= 0x13U<<27;
//        mctl_write_w(SDR_DCR, reg_val);
//        
//        while( mctl_read_w(SDR_DCR)& (0x1U<<31) );
//
//        mem_delay(0x100);
    }

    //scan read pipe value
    mctl_itm_enable();
    
    if(hold_flag == 0)//normal branch
    {
    	ret_val = DRAMC_scan_readpipe();
    	
    	if(ret_val < 0)
	    {
	        return 0;
	    }
    
    }else	//super standby branch
    {
    	//write back dqs gating value
        reg_val = mctl_read_w(SDR_GP_REG1);
        mctl_write_w(SDR_RSLR0, reg_val);

        reg_val = mctl_read_w(SDR_GP_REG2);
        mctl_write_w(SDR_RDQSGR, reg_val);

      //mctl_write_w(SDR_RSLR0, dqs_value_save[0]);
	  //mctl_write_w(SDR_RDQSGR, dqs_value_save[1]);
    }
    
    //configure all host port
    mctl_configure_hostport();

    return DRAMC_get_dram_size();
}



__s32 DRAMC_scan_readpipe(void)
{
    __u32 reg_val;

    //Clear Error Flag
    reg_val = mctl_read_w(SDR_CSR);
    reg_val &= ~(0x1<<20);
    mctl_write_w(SDR_CSR, reg_val);
    
    //data training trigger
    reg_val = mctl_read_w(SDR_CCR);
    reg_val |= 0x1<<30;
    mctl_write_w(SDR_CCR, reg_val);

    //check whether data training process is end
    while(mctl_read_w(SDR_CCR) & (0x1<<30)) {};

    //check data training result
    reg_val = mctl_read_w(SDR_CSR);
    if(reg_val & (0x1<<20))
    {
        return -1;
    }

    return (0);
}

/*
*********************************************************************************************************
*                                   DRAM SCAN READ PIPE
*
* Description: dram scan read pipe
*
* Arguments  : none
*
* Returns    : result, 0:fail, 1:success;
*
* Note       :
*********************************************************************************************************
*/


/*
*********************************************************************************************************
*                                   DRAM CLOCK CONTROL
*
* Description: dram get clock
*
* Arguments  : on   dram clock output (0: disable, 1: enable)
*
* Returns    : none
*
* Note       :
*********************************************************************************************************
*/
void DRAMC_clock_output_en(__u32 on)
{
    __u32 reg_val;

    reg_val = mctl_read_w(SDR_CR);

    if(on)
        reg_val |= 0x1<<16;
    else
        reg_val &= ~(0x1<<16);

    mctl_write_w(SDR_CR, reg_val);
}
/*
*********************************************************************************************************
* Description: Set autorefresh cycle
*
* Arguments  : clock value in MHz unit
*
* Returns    : none
*
* Note       :
*********************************************************************************************************
*/
void DRAMC_set_autorefresh_cycle(__u32 clk)
{
    __u32 reg_val;
//  __u32 dram_size;
    __u32 tmp_val;

//  dram_size = mctl_read_w(SDR_DCR);
//  dram_size >>=3;
//  dram_size &= 0x7;

//  if(clk < 600)
    {
//      if(dram_size<=0x2)
//          tmp_val = (131*clk)>>10;
//      else
//          tmp_val = (336*clk)>>10;
        reg_val = 0x83;
        tmp_val = (7987*clk)>>10;
        tmp_val = tmp_val*9 - 200;
        reg_val |= tmp_val<<8;
        reg_val |= 0x8<<24;
        mctl_write_w(SDR_DRR, reg_val);
    }
//  else
//   {
//      mctl_write_w(SDR_DRR, 0x0);
//   }
}


/*
**********************************************************************************************************************
*                                               GET DRAM SIZE
*
* Description: Get DRAM Size in MB unit;
*
* Arguments  : None
*
* Returns    : 32/64/128
*
* Notes      :
*
**********************************************************************************************************************
*/
__u32 DRAMC_get_dram_size(void)
{
    __u32 reg_val;
    __u32 dram_size;
    __u32 chip_den;

    reg_val = mctl_read_w(SDR_DCR);
    chip_den = (reg_val>>3)&0x7;
    if(chip_den == 0)
        dram_size = 32;
    else if(chip_den == 1)
        dram_size = 64;
    else if(chip_den == 2)
        dram_size = 128;
    else if(chip_den == 3)
        dram_size = 256;
    else if(chip_den == 4)
        dram_size = 512;
    else
        dram_size = 1024;

    if( ((reg_val>>1)&0x3) == 0x1)
        dram_size<<=1;
    if( ((reg_val>>6)&0x7) == 0x3)
        dram_size<<=1;
    if( ((reg_val>>10)&0x3) == 0x1)
        dram_size<<=1;

    return dram_size;
}


__s32 dram_init(void)
{
    /* do nothing for dram init */
    return 0;
}


void save_mem_status(volatile __u32 val)
{
    //*(volatile __u32 *)(STATUS_REG  + 0x04) = val;
    return;
}


//*****************************************************************************
//	void mctl_self_refresh_entry()
//  Description:	Enter into self refresh state
//
//	Arguments:		None
//
//	Return Value:	None
//*****************************************************************************
void mctl_self_refresh_entry(void)
{
	__u32 reg_val;

	//disable auto refresh
	reg_val = mctl_read_w(SDR_DRR);
	reg_val |= 0x1U<<31;
	mctl_write_w(SDR_DRR, reg_val);

	reg_val = mctl_read_w(SDR_DCR);
	reg_val &= ~(0x1fU<<27);
	reg_val |= 0x12U<<27;
	mctl_write_w(SDR_DCR, reg_val);

	//check whether command has been executed
	while( mctl_read_w(SDR_DCR)& (0x1U<<31) );
	delay_us(10);//mem_delay(10);
}

//**************************************************
void setup_dram_pll(__u32 clk)
{
    __u32 reg_val;

    //setup DRAM PLL
    reg_val = mctl_read_w(DRAM_CCM_SDRAM_PLL_REG);
    reg_val &= ~(1U<<29);
    mctl_write_w(DRAM_CCM_SDRAM_PLL_REG, reg_val);		//close DDR-CLK
    delay_us(10);
    reg_val &= ~(1U<<31);
    mctl_write_w(DRAM_CCM_SDRAM_PLL_REG, reg_val);		//disable PLL
    delay_us(10);//mem_delay(100);

    reg_val &= ~0x3;
    reg_val |= 0x1;                                             //m factor
    reg_val &= ~(0x3<<4);
    reg_val |= 0x1<<4;                                          //k factor
    reg_val &= ~(0x1f<<8);
    reg_val |= ((clk/24)&0x1f)<<8;                              //n factor
    reg_val &= ~(0x3<<16);
    reg_val |= 0x1<<16;                                         //p factor
    reg_val &= ~(0x1<<29);                                      //clock output disable
    reg_val |= (__u32)0x1<<31;                                  //PLL En
    mctl_write_w(DRAM_CCM_SDRAM_PLL_REG, reg_val);
    //mctl_delay(0x100000);
    delay_us(1200);//mem_delay(1000);
    reg_val = mctl_read_w(DRAM_CCM_SDRAM_PLL_REG);
    reg_val |= 0x1<<29;
    mctl_write_w(DRAM_CCM_SDRAM_PLL_REG, reg_val);
    delay_us(100);//mem_delay(100);

}
//******************************************************
//	dram_freq_jum(unsigned char);
//	used for frequency jump, add by yeshaozhen at 20130628
//******************************************************
__s32	dram_freq_jum(unsigned char freq_p,__dram_para_t *dram_para)
{
	__u32 reg_val = 0;
	
	//__s32 return_val = 0;
	__u32 i;
	//__u32 dram_access[32];
	
	if((dram_para->dram_tpr4 & 0x8) == 0)	//freq jump not allow
		return 0;
		
		//__asm__ __volatile__ ("cmp r0,r0");
		//__asm__ __volatile__ ("beq .");
		
		
//		//enable bandwidth couter   //disable access to dram
//		for(i=0;i<32;i++)
//		{
//			reg_val = mctl_read_w( SDR_HPCR + (i<<2) );
//			//dram_access[i]= reg_val;
//			reg_val |= (0x3U<<30);
//			//reg_val &= ~(0x1);
//			mctl_write_w( (SDR_HPCR + (i<<2)) , reg_val);
//		}
//		
//		//reset counter
//		reg_val = mctl_read_w(SDR_LTR);
//		reg_val &= ~(1U<<17);
//		mctl_write_w(SDR_LTR, reg_val);
//		reg_val |= (1U<<17);
//		mctl_write_w(SDR_LTR, reg_val);
		
		
	if(freq_p == 0)	//the first calibration frequency point
	{
			//__asm__ __volatile__ ("cmp r0,r0");
			//__asm__ __volatile__ ("beq .");
			
			mctl_self_refresh_entry();	//DRAMC_enter_selfrefresh();
			//turn off DLL
			mctl_disable_dll();
			DRAMC_clock_output_en(0);	//dram clock off

			setup_dram_pll(120);
			delay_us(10);//
			mctl_itm_disable();
			mctl_enable_dll0(dram_para->dram_tpr3);										//disable DLL when frequency is 120
			delay_us(10);//
			
			DRAMC_clock_output_en(1);	//dram clock on
			delay_us(1000);//mem_delay(0x1000);

			//set refresh period
			DRAMC_set_autorefresh_cycle(120);
			
			mctl_enable_dllx(dram_para->dram_tpr3);
			
			delay_us(100);//
			
			//disable auto-fresh
			reg_val = mctl_read_w(SDR_DRR);
			reg_val |= 0x1U<<31;
			mctl_write_w(SDR_DRR, reg_val);

			//03-08
			reg_val = mctl_read_w(SDR_DCR);
			reg_val &= ~(0x1fU<<27);
			reg_val |= 0x12U<<27;
			mctl_write_w(SDR_DCR, reg_val);
			while( mctl_read_w(SDR_DCR)& (0x1U<<31) );

			delay_us(10);//mem_delay(0x10);

			//exit self-refresh state
			reg_val = mctl_read_w(SDR_DCR);
			reg_val &= ~(0x1fU<<27);
			reg_val |= 0x17U<<27;
			mctl_write_w(SDR_DCR, reg_val);

			//check whether command has been executed
			while( mctl_read_w(SDR_DCR)& (0x1U<<31) );
			
//			//reset DLL in DRAM
//			reg_val = mctl_read_w(SDR_MR);
//			reg_val |= (0x1U<<8);
//			mctl_write_w(SDR_MR, reg_val);
//			delay_us(100);//mem_delay(0x1000);			

			//enable auto-fresh			//by cpl 2013-5-6
			reg_val = mctl_read_w(SDR_DRR);
			reg_val &= ~(0x1U<<31);
			mctl_write_w(SDR_DRR, reg_val);

			mctl_itm_enable();
			//write back dqs gating value
			mctl_write_w(SDR_RSLR0, (dram_para->dram_tpr5) & 0xfff);	//dqs_value_save_120[0]
			mctl_write_w(SDR_RDQSGR,((dram_para->dram_tpr5)>>16) & 0xff);	// dqs_value_save_120[1]);		
			
		  delay_us(10);//
		  //__asm__ __volatile__ ("cmp r0,r0");
			//__asm__ __volatile__ ("beq .");
	}
	else if(freq_p == 1)
	{
			mctl_self_refresh_entry();//DRAMC_enter_selfrefresh();

			mctl_disable_dll();
			DRAMC_clock_output_en(0);	//dram clock off

			setup_dram_pll(dram_para->dram_clk);
			delay_us(10);//
			mctl_itm_disable();
			mctl_enable_dll0(dram_para->dram_tpr3);
			delay_us(10);//

//			//dram clock on
			DRAMC_clock_output_en(1);
			delay_us(1000);//mem_delay(0x1000);

			//set refresh period
			DRAMC_set_autorefresh_cycle(dram_para->dram_clk);
			
			mctl_enable_dllx(dram_para->dram_tpr3);
			
			delay_us(100);//

		  	//disable auto-fresh			//by cpl 2013-5-6
			reg_val = mctl_read_w(SDR_DRR);
			reg_val |= 0x1U<<31;
			mctl_write_w(SDR_DRR, reg_val);

			//03-08
			reg_val = mctl_read_w(SDR_DCR);
			reg_val &= ~(0x1fU<<27);
			reg_val |= 0x12U<<27;
			mctl_write_w(SDR_DCR, reg_val);
			while( mctl_read_w(SDR_DCR)& (0x1U<<31) );

			delay_us(10);//mem_delay(0x10);

			//exit self-refresh state
			reg_val = mctl_read_w(SDR_DCR);
			reg_val &= ~(0x1fU<<27);
			reg_val |= 0x17U<<27;
			mctl_write_w(SDR_DCR, reg_val);

			//check whether command has been executed
			while( mctl_read_w(SDR_DCR)& (0x1U<<31) );
			
//			//reset DLL in DRAM
//			reg_val = mctl_read_w(SDR_MR);
//			reg_val |= (0x1U<<8);
//			mctl_write_w(SDR_MR, reg_val);
//			delay_us(2000);//mem_delay(0x1000);

			//enable auto-fresh			//by cpl 2013-5-6
			reg_val = mctl_read_w(SDR_DRR);
			reg_val &= ~(0x1U<<31);
			mctl_write_w(SDR_DRR, reg_val);

			mctl_itm_enable();
			//write back dqs gating value
      reg_val = mctl_read_w(SDR_GP_REG1);
      mctl_write_w(SDR_RSLR0, reg_val);

      reg_val = mctl_read_w(SDR_GP_REG2);
      mctl_write_w(SDR_RDQSGR, reg_val);
      
      delay_us(10);//
	}
	
//	//stop bandwidth counter
//		reg_val = mctl_read_w(SDR_LTR);
//		reg_val &= ~(1U<<17);
//		mctl_write_w(SDR_LTR, reg_val);
//	//read the bandwidth value
//		return_val = mctl_read_w(SDR_BANDWIDTH);
		
//		//enable dram access
//		for(i=0;i<32;i++)
//		{
//			mctl_write_w( SDR_HPCR + (i<<2) , dram_access[i]);
//		}
		
		//return return_val;
}
//*****************************************************************************end of freq jump at 20120628 by YeShaozhen

__s32 init_DRAM(standy_dram_para_t *boot0_para)
{
    __u32 i;
    __s32 ret_val;

    if(boot0_para->dram_clk > 2000)
    {
//          boot0_para->dram_clk = mem_uldiv(boot0_para->dram_clk, 1000000);
            //boot0_para.dram_clk /= 1000000;
    }

    ret_val = 0;
    i = 0;
    while( (ret_val == 0) && (i<4) )
    {
            ret_val = DRAMC_init(boot0_para);
            i++;
    }
    return ret_val;
}


__s32 dram_exit(void)
{
    return 0;
}

__s32 dram_get_size(void)
{
    return DRAMC_get_dram_size();
}

void dram_set_clock(int clk)
{
    return mctl_setup_dram_clock(clk,0);	//no PLL tunning
}

void dram_set_drive(void)
{
    mctl_set_drive();
}

void dram_set_autorefresh_cycle(__u32 clk)
{
    DRAMC_set_autorefresh_cycle(clk);
}

__s32 dram_scan_readpipe(void)
{
    return DRAMC_scan_readpipe();
}

