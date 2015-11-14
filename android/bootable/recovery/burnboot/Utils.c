#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <string.h>

#include "BurnBoot.h"
#include "BootHead.h"

#define DEVNODE_PATH_NAND   "/dev/block/bootloader"
#define DEVNODE_PATH_SD "/dev/block/mmcblk0"

#define CMDLINE_FILE_PATH "/proc/cmdline"

#define UBOOT_MAGIC				"uboot"
#define BOOT1_MAGIC             "eGON.BT1"
#define BOOT0_MAGIC             "eGON.BT0"

static int spliteKeyAndValue(char *str, char **key, char **value){
	int elocation = strcspn(str,"=");
	if (elocation < 0){
		return -1;
	}
	str[elocation] = '\0';
	*key = str;
	*value = str + elocation + 1;
	return 0;
}

static int getInfoFromCmdline(char *key, char *value){
	FILE *fp;
    char cmdline[1024];
	//read partition info from /proc/cmdline
    if ((fp = fopen(CMDLINE_FILE_PATH,"r")) == NULL) {
        bb_debug("can't open /proc/cmdline \n");
        // goto error;
        return -1;
    }
    fgets(cmdline,1024,fp);
    fclose(fp);
    // bb_debug("%s\n", cmdline); 
    //splite the cmdline by space
    char *p = NULL;
    char *lkey = NULL;
    char *lvalue = NULL;
    p = strtok(cmdline, " ");
    if (!spliteKeyAndValue(p, &lkey, &lvalue)){
    	if (!strcmp(lkey,key)){
    		goto done;
    	}
    }
    // bb_debug("the first k-v is %s\n", p);

    while ((p = strtok(NULL, " "))){
    	// bb_debug("the other k-v is %s\n", p);
    	if (!spliteKeyAndValue(p, &lkey, &lvalue)){
	    	if (!strcmp(lkey,key)){
	    		goto done;
	    	}
	    }
    }

    bb_debug("no key named %s in cmdline.\n", key);
    strcpy(value, "-1");
    return -1;

    done:
    strcpy(value, lvalue);
    return 0;
}

int getFlashType(){
	char ctype[8];
	getInfoFromCmdline("boot_type", ctype);
	bb_debug("flash type = %s\n", ctype);

	int flash_type = atoi(ctype);
	//atoi出错时会返回0,当ctype字符串为0时也会返回0，所以这里要判断是否出错.
	if (flash_type == 0 && ctype[0] != '0'){
		return FLASH_TYPE_UNKNOW;
	}

	return flash_type;
}

int getBufferExtractCookieOfFile(const char *path, BufferExtractCookie *cookie){

	if (cookie == NULL){
		// printf("get file stat failed!\n");
		return -1;
	}

	struct stat statbuff;
	if(stat(path, &statbuff) < 0){
		bb_debug("get file stat failed!!\n");
		return -1;
	}
	cookie->len = statbuff.st_size;
	// bb_debug("file size is %d\n",(int)cookie->len);
	
	unsigned char *buffer = malloc(cookie->len);

	FILE *fp = fopen(path,"r");
	if (fp == NULL){
		bb_debug("open file failed!\n");
		return -1;
	}
	
	if (!fread(buffer, cookie->len, 1, fp)){
		bb_debug("read file failed!\n");
		return -1;
	}

	cookie->buffer = buffer;

	return 0;

}

//获取flash类型，根据flash类型选择烧写哪个boot0和确定设备文件地址
int getDeviceInfo(int boot_num, char *dev_node, char *boot_bin, DeviceBurn *burnFunc){
	
	int flash_type = getFlashType();

	switch(flash_type){
		case FLASH_TYPE_NAND:
			strcpy(dev_node, DEVNODE_PATH_NAND);
			bb_debug("use nand flash!!\n");
			break;
		case FLASH_TYPE_SD1:
        case FLASH_TYPE_SD2:
        	strcpy(dev_node, DEVNODE_PATH_SD);
        	bb_debug("use sd flash\n");
        	break;
        default:
        	goto error;
	}

    switch(boot_num){
        case BOOT0:
            switch(flash_type){
                case FLASH_TYPE_NAND:
                    strcpy(boot_bin, "boot0_nand.fex");
                    *burnFunc = burnNandBoot0;
                    break;
                case FLASH_TYPE_SD1:
                case FLASH_TYPE_SD2:
                    strcpy(boot_bin, "boot0_sdcard.fex");
                    *burnFunc = burnSdBoot0;
                    break;
            }
            break;
        case UBOOT:
            switch(flash_type){
                case FLASH_TYPE_NAND:
                    strcpy(boot_bin, "uboot_nand.fex");
                    *burnFunc = burnNandUboot;
                    break;
                case FLASH_TYPE_SD1:
                case FLASH_TYPE_SD2:
                    strcpy(boot_bin, "uboot_sdcard.fex");
                    *burnFunc = burnSdUboot;
                    break;
            }
            break;
                   
            
    }

    return flash_type;
    error:
    return FLASH_TYPE_UNKNOW;
}

int checkBoot0Sum(BufferExtractCookie *cookie){

	standard_boot_file_head_t  *head_p;
    unsigned int length;
    unsigned int *buf;
    unsigned int loop;
    unsigned int i;
    unsigned int sum;
    unsigned int csum;

    head_p = (standard_boot_file_head_t *)cookie->buffer;

    length = head_p->length;
    if( ( length & 0x3 ) != 0 )                   // must 4-byte-aligned
            return -1;
    if((length > 24 * 1024) != 0 ) {
        bb_debug("boot0 file length over size!!\n");
    }
    if ((length & ( 512 - 1 )) != 0){
        bb_debug("boot0 file did not aliged!!\n");
    }
    buf = (unsigned int *)cookie->buffer;
    csum = head_p->check_sum;
    head_p->check_sum = STAMP_VALUE;              // fill stamp
    loop = length >> 2;
    
    for( i = 0, sum = 0;  i < loop;  i++ )
        sum += buf[i];

    head_p->check_sum = csum;
    bb_debug("Boot0 -> File length is %u,original sum is %u,new sum is %u\n", length, head_p->check_sum, sum);
    return !(csum == sum);
}


int checkUbootSum(BufferExtractCookie* cookie){
    uboot_file_head  *head_p;
    unsigned int length;
    unsigned int *buf;
    unsigned int loop;
    unsigned int i;
    unsigned int sum;
    unsigned int csum;

    head_p = (uboot_file_head *)cookie->buffer;
    length = head_p->length;
    if((length & 0x3) != 0)                   // must 4-byte-aligned
            return -1;

    buf = (unsigned int *)cookie->buffer;
    csum = head_p->check_sum;

    head_p->check_sum = STAMP_VALUE;              // fill stamp
    loop = length >> 2;
    
    for( i = 0, sum = 0; i < loop; i++)
        sum += buf[i];

    head_p->check_sum = csum;
    bb_debug("Uboot -> File length is %u,original sum is %u,new sum is %u\n", length, head_p->check_sum, sum);
    return !(csum == sum);
}

int checkUbootMagic(void *buffer)
{
	uboot_file_head  *head_p;

	head_p = (uboot_file_head *)buffer;
	if(strncmp(head_p->magic, UBOOT_MAGIC, 5))
	{
		bb_debug("Uboot Magic check failed:the new is %s\n", head_p->magic);
		return -1;
	}
    return 0;
}
int checkBoot1Magic(void *buffer)
{
	boot1_file_head  *head_p;
	head_p = (boot1_file_head *)buffer;
	if(strncmp(head_p->magic, BOOT1_MAGIC, 8))
    {
		bb_debug("Uboot Magic check failed:the new is %s\n", head_p->magic);
		return -1;
    }
    return 0;
}
int getDramPara(void *newBoot0, void *innerBoot0){
    boot0_file_head_t *srcHead;
    boot0_file_head_t *dstHead;

    srcHead = (boot0_file_head_t *)innerBoot0;
    dstHead = (boot0_file_head_t *)newBoot0;

    memcpy(&dstHead->prvt_head, &srcHead->prvt_head, 40 * 4);
    return 0;
}

int genBoot0CheckSum(void *cookie)
{
    standard_boot_file_head_t  *head_p;
    unsigned int length;
    unsigned int *buf;
    unsigned int loop;
    unsigned int i;
    unsigned int sum;

    head_p = (standard_boot_file_head_t *)cookie;
    length = head_p->length;
    if( ( length & 0x3 ) != 0 )                   // must 4-byte-aligned
        return -1;
    buf = (unsigned int *)cookie;
    head_p->check_sum = STAMP_VALUE;              // fill stamp
    loop = length >> 2;

    for( i = 0, sum = 0;  i < loop;  i++ )
        sum += buf[i];

    /* write back check sum */
    head_p->check_sum = sum;
    return 0;
}

void clearPageCache(){
    FILE *fp = fopen("/proc/sys/vm/drop_caches", "w+");
    char *num = "1";
    fwrite(num, sizeof(char), 1, fp);
    fclose(fp);
}

int checkBoot1Sum(BufferExtractCookie* cookie){
    boot1_file_head  *head_p;
    unsigned int length;
    unsigned int *buf;
    unsigned int loop;
    unsigned int i;
    unsigned int sum;
    unsigned int csum;

    head_p = (boot1_file_head *)cookie->buffer;
    length = head_p->length;
    if( ( length & 0x3 ) != 0 )                   // must 4-byte-aligned
            return -1;

    buf = (unsigned int *)cookie->buffer;
    csum = head_p->check_sum;

    head_p->check_sum = STAMP_VALUE;              // fill stamp
    loop = length >> 2;
    
    for( i = 0, sum = 0;  i < loop;  i++ )
        sum += buf[i];

    head_p->check_sum = csum;
    bb_debug("boot1:File length is %u,old sum is %u,new sum is %u\n", length, head_p->check_sum, sum);
    return !(csum == sum);
}

//function: check the boot type: boot1.0 or boot2.0
//return: 1:boot1.0, 2:boot2.0, -1:error
int checkUbootType(char* buffer)
{
	if(!checkUbootMagic(buffer))  //boot2.0-uboot
	{
		bb_debug("the system used boot type is boot2.0\n");
		return 2;
	}

	if(!checkBoot1Magic(buffer)) //boot1.0-boot1
	{
		bb_debug("the system used boot type is boot1.0\n");
		return 1;
	}

	bb_debug("check emmc boot type failed\n");
	return -1;
}

//function: check boot type used by emmc
//return: 1:boot1.0, 2:boot2.0, -1:error
int checkEmmcUbootType(char* path)
{
	char buffer[640 * 1024];
	memset(buffer, 0, sizeof(buffer));

	if(readSdUboot(path, buffer))
	{
		bb_debug("readSdUboot failed");
		return -1;
	}

	return checkUbootType(buffer);
}
