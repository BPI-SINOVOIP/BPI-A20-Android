#include "nand_blk.h"
#include "nand_dev.h"

/*****************************************************************************/

extern struct nand_blk_ops mytr;
extern struct _nand_info* p_nand_info;
extern void NAND_Interrupt(__u32 nand_index);
extern __u32 NAND_GetCurrentCH(void);
extern int  init_blklayer(void);
extern void   exit_blklayer(void);
extern void set_cache_level(struct _nand_info*nand_info,unsigned short cache_level);
extern void set_capacity_level(struct _nand_info*nand_info,unsigned short capacity_level);
extern int NAND_get_storagetype(void);
extern void NAND_PhysicLock(void); 
extern void NAND_PhysicUnLock(void); 
#define BLK_ERR_MSG_ON
unsigned long long  time_used = 0;
/*****************************************************************************
*Name         :
*Description  :
*Parameter    :
*Return       :
*Note         :
*****************************************************************************/
#ifdef __LINUX_NAND_SUPPORT_INT__

spinlock_t     nand_int_lock;

static irqreturn_t nand_interrupt_ch(int irq, void *dev_id)
{
    unsigned long iflags;
    __u32 nand_index;

    //nand_dbg_err("nand_interrupt_ch0!\n");
    spin_lock_irqsave(&nand_int_lock, iflags);

    nand_index = NAND_GetCurrentCH();
    if(nand_index!=0)
    {
        //nand_dbg_err(" ch %d int in ch0\n", nand_index);
    }
    else
    {
        NAND_Interrupt(nand_index);
    }
    spin_unlock_irqrestore(&nand_int_lock, iflags);

    return IRQ_HANDLED;
}
#endif

/*****************************************************************************
*Name         :
*Description  :
*Parameter    :
*Return       :
*Note         :
*****************************************************************************/
static int nand_suspend(struct platform_device *plat_dev, pm_message_t state)
{
    if(NORMAL_STANDBY== standby_type)
    {
        nand_dbg_err("[NAND] nand_suspend normal\n");
		lock_all_blk();
        NandHwNormalStandby();
    }
    else if(SUPER_STANDBY == standby_type)
    {
        nand_dbg_err("[NAND] nand_suspend super\n");
        lock_all_blk();
		NandHwSuperStandby();
    }

    nand_dbg_err("[NAND] nand_suspend ok \n");
    return 0;
}

/*****************************************************************************
*Name         :
*Description  :
*Parameter    :
*Return       :
*Note         :
*****************************************************************************/
static int nand_resume(struct platform_device *plat_dev)
{

    if(NORMAL_STANDBY== standby_type){
        nand_dbg_err("[NAND] nand_resume normal\n");
        NandHwNormalResume();
		unlock_all_blk();
    }else if(SUPER_STANDBY == standby_type){
        nand_dbg_err("[NAND] nand_resume super\n");
        NandHwSuperResume();
		unlock_all_blk();
    }

    nand_dbg_err("[NAND] nand_resume ok \n");
    return 0;
}

/*****************************************************************************
*Name         :
*Description  :
*Parameter    :
*Return       :
*Note         :
*****************************************************************************/
static int nand_probe(struct platform_device *plat_dev)
{
    nand_dbg_inf("nand_probe\n");
    return 0;
}

/*****************************************************************************
*Name         :
*Description  :
*Parameter    :
*Return       :
*Note         :
*****************************************************************************/
static int nand_remove(struct platform_device *plat_dev)
{
    nand_dbg_inf("nand_remove\n");
    return 0;
}

/*****************************************************************************
*Name         :
*Description  :
*Parameter    :
*Return       :
*Note         :
*****************************************************************************/
uint32 shutdown_flush_write_cache(void)
{
    struct _nftl_blk *nftl_blk;
    struct nand_blk_ops *tr = &mytr;

    nftl_blk = tr->nftl_blk_head.nftl_blk_next;

    while(nftl_blk != NULL)
    {
        nand_dbg_err("shutdown_flush_write_cache\n");
        mutex_lock(nftl_blk->blk_lock);
        nftl_blk->flush_write_cache(nftl_blk,0xffff);
        nftl_blk = nftl_blk->nftl_blk_next;
        //mutex_unlock(nftl_blk->blk_lock);
    }
    return 0;
}

/*****************************************************************************
*Name         :
*Description  :
*Parameter    :
*Return       :
*Note         :
*****************************************************************************/
void nand_shutdown(struct platform_device *plat_dev)
{
    struct nand_blk_dev *dev;
    struct nand_blk_ops *tr = &mytr;

    nand_dbg_err("[NAND]shutdown first\n");
    list_for_each_entry(dev, &tr->devs, list){
        while(blk_fetch_request(dev->rq) != NULL){
            nand_dbg_err("nand_shutdown wait dev %d\n",dev->devnum);
            set_current_state(TASK_INTERRUPTIBLE);
            schedule_timeout(HZ>>3);
        }
    }

    nand_dbg_err("[NAND]shutdown second\n");
    list_for_each_entry(dev, &tr->devs, list){
        while(blk_fetch_request(dev->rq) != NULL){
            nand_dbg_err("nand_shutdown wait dev %d\n",dev->devnum);
            set_current_state(TASK_INTERRUPTIBLE);
            schedule_timeout(HZ>>3);
        }
    }

    shutdown_flush_write_cache();
    NandHwShutDown();
    nand_dbg_err("[NAND]shutdown end\n");
}

static int nand_dram_notify(struct notifier_block *nb, unsigned long event, void *cmd)
{
//    int i = 0;
//    if (event == DRAMFREQ_NOTIFY_PREPARE){
//				printk("[NAND] before  lock\n");
//				lock_all_blk();
//				NAND_PhysicLock();
//				printk("[NAND] after  lock\n");
//    }
//    if (event == DRAMFREQ_NOTIFY_DONE){
//        printk("[NAND] before  unlock\n");
//        NAND_PhysicUnLock();
//		unlock_all_blk();
//			printk("[NAND] after  lock\n");    
//    }
    return 0;
}

/*****************************************************************************
*Name         :
*Description  :
*Parameter    :
*Return       :
*Note         :
*****************************************************************************/
static struct platform_driver nand_driver = {
    .probe = nand_probe,
    .remove = nand_remove,
    .shutdown =  nand_shutdown,
    .suspend = nand_suspend,
    .resume = nand_resume,
    .driver = {
        .name = "sw_nand",
        .owner = THIS_MODULE,
    }
};


static struct resource flash_resource = {
	.start		= 0,
	.end		= 1,
	.flags		= 0x1,
};

static struct platform_device nand_device = {
	.name		= "sw_nand",
	.id		= 33,
	.resource	= &flash_resource,
	.num_resources	= 1,
};

static struct notifier_block nand_notifier = {
       .notifier_call = nand_dram_notify,
};



/*****************************************************************************
*Name         :
*Description  :
*Parameter    :
*Return       :
*Note         :
*****************************************************************************/
extern int test_mbr(uchar* data);

int __init nand_init(void)
{
    int ret;
    script_item_u   nand0_used_flag;
    script_item_u   nand_cache_level;
    script_item_u   nand_capacity_level;
    script_item_value_type_e  type;
    char * dev_name = "nand_dev";
    char * dev_id = "nand_id";

#ifdef __LINUX_NAND_SUPPORT_INT__
    unsigned long irqflags_ch0, irqflags_ch1;
#endif


    /* 获取card_line值 */
    type = script_get_item("nand_para", "nand_used", &nand0_used_flag);
    if(SCIRPT_ITEM_VALUE_TYPE_INT != type)
    {
        nand_dbg_err("nand type err! %d",type);
    }
    nand_dbg_err("[NAND]nand init start, nand_used_flag is %d\n", nand0_used_flag.val);

    nand_cache_level.val = 0;
    type = script_get_item("nand_para", "nand_cache_level", &nand_cache_level);
    if(SCIRPT_ITEM_VALUE_TYPE_INT != type)
    {
        nand_dbg_err("nand_cache_level err! %d",type);
        nand_cache_level.val = 0;
    }

    nand_capacity_level.val = 0;
    type = script_get_item("nand_para", "nand_capacity_level", &nand_capacity_level);
    if(SCIRPT_ITEM_VALUE_TYPE_INT != type)
    {
        nand_dbg_err("nand_capacity_level err! %d",type);
        nand_capacity_level.val = 0;
    }
#ifdef __LINUX_NAND_SUPPORT_INT__
    //nand_dbg_err("[NAND] nand driver version: 0x%x 0x%x, support int! \n", NAND_VERSION_0,NAND_VERSION_1);

    spin_lock_init(&nand_int_lock);
    irqflags_ch0 = IRQF_DISABLED;
    irqflags_ch1 = IRQF_DISABLED;

    if (request_irq(AW_IRQ_NAND, nand_interrupt_ch, IRQF_DISABLED, dev_name, &dev_id))
    {
        nand_dbg_err("nand interrupte ch0 irqno: %d register error\n", AW_IRQ_NAND);
        return -EAGAIN;
    }
    else
    {
        //nand_dbg_err("nand interrupte ch0 irqno: %d register ok\n", AW_IRQ_NAND0);
    }
#endif

    if(nand0_used_flag.val == 0)
    {
        nand_dbg_err("nand driver is disabled \n");
        return 0;
    }

    nand_dbg_err("nand init start\n");

    p_nand_info = NandHwInit();
    if(p_nand_info == NULL)
    {
        return -1;
    }

    set_cache_level(p_nand_info,nand_cache_level.val);
    set_capacity_level(p_nand_info,nand_capacity_level.val);

	int storage_type =0;
    storage_type = NAND_get_storagetype();
    if((1 != storage_type)&&(2 != storage_type))
    {
		ret = nand_info_init(p_nand_info,0,8,NULL);
    }
	else
	{
		nand_dbg_err("storage_type=%d,run nand test for dragonboard\n",storage_type);
				
		uchar *data = kmalloc(0x400,GFP_KERNEL);
		test_mbr(data);
    	ret = nand_info_init(p_nand_info,0,8,data);

	}

    if(ret != 0)
    {
        return ret;
    }

	//platform_device_register(&nand_device);
   	platform_driver_register(&nand_driver);

	//dramfreq_register_notifier(&nand_notifier);
    time_used = 0;
    init_blklayer();

    nand_dbg_err("nand init end \n");

//    //init sysfs
//    nand_dbg_err("init nand sysfs !\n");
//    if((ret = kobject_init_and_add(&kobj,&ktype,NULL,"nand_driver")) != 0 ) {
//      nand_dbg_err("init nand sysfs fail!\n");
//      return ret;
//  }

    return 0;
}
/*****************************************************************************
*Name         :
*Description  :
*Parameter    :
*Return       :
*Note         :
*****************************************************************************/
void __exit nand_exit(void)
{
    script_item_u   nand0_used_flag;
    script_item_value_type_e  type;

    /* 获取card_line值 */
    type = script_get_item("nand0_para", "nand0_used", &nand0_used_flag);
    if(SCIRPT_ITEM_VALUE_TYPE_INT != type)
    nand_dbg_err("nand type err!");
    nand_dbg_err("nand0_used_flag is %d\n", nand0_used_flag.val);

    if(nand0_used_flag.val == 0)
    {
        nand_dbg_err("nand driver is disabled \n");
    }

    platform_driver_unregister(&nand_driver);
	//platform_device_unregister(&nand_device);

    exit_blklayer();

//  kobject_del(&kobj);
//  kobject_put(&kobj);
}

//module_init(nand_init);
//module_exit(nand_exit);
MODULE_LICENSE ("GPL");
MODULE_AUTHOR ("nand flash groups");
MODULE_DESCRIPTION ("Generic NAND flash driver code");