#define bb_debug(fmt, args...) printf("burnboot:"fmt, ## args)

typedef struct {
    unsigned char* buffer;
    long len;
} BufferExtractCookie;

typedef int (*DeviceBurn)(BufferExtractCookie *cookie, char* path);

int getFlashType();

int getBufferExtractCookieOfFile(const char* path, BufferExtractCookie* cookie);

int getDeviceInfo(int boot_num, char* dev_node, char* boot_bin, DeviceBurn* burnFunc);

int checkBoot0Sum(BufferExtractCookie* cookie);

int checkUbootSum(BufferExtractCookie* cookie);

int getDramPara(void *newBoot0, void *innerBoot0);

int genBoot0CheckSum(void *cookie);

int checkBoot1Sum(BufferExtractCookie* cookie);
int checkEmmcUbootType(char* path);
