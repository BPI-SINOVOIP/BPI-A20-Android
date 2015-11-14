#define LOG_TAG "RecoverySystemEx"

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"
#include "md5.h"
#include <stdlib.h>
#include <stdio.h>
#include <sys/stat.h>
#include <linux/kdev_t.h>
#include <fcntl.h>
#include <unistd.h>
#include <dirent.h>

#define SYS_BLOCK_DIR          "/sys/dev/block"
#define RECOVERY_DIR           "/cache/recovery"
#define RECOVERY_COMMAND_FILE  "/cache/recovery/command"
#define EX_INSTALL_PACKAGE_CMD "--update_package_ex=%s:%s:%s"

#ifndef MD5_DIGEST_LENGTH
#define MD5_DIGEST_LENGTH 16
#endif

#ifndef MAX_PATH
#define MAX_PATH 256
#endif

namespace softwinner
{

static int get_relative_path(const char* fullpath, char* out_rel_path){
    char mount_points[MAX_PATH][MAX_PATH] = {0};
    char mount_point[MAX_PATH] = {0};

    // 获得当前系统的所有挂载点，将挂载点和文件路径做比较，以抽取相对路径
    FILE *f;
    if((f = fopen("/proc/mounts", "r")) == NULL){
        ALOGW("Could not open /proc/mounts: %s\n", strerror(errno));
        return 0;
    }

    char device[256] = {0};
    char mount_path[256] = {0};
    char rest[256] = {0};

    int mount_points_size = 0;

    char s[2000] = {0};
    while (fgets(s, 2000, f)) {
        char *c, *e = s;

        for (c = s; *c; c++) {
            if (*c == ' ') {
                e = c + 1;
                break;
            }
        }

        for (c = e; *c; c++) {
            if (*c == ' ') {
                *c = '\0';
                break;
            }
        }

        strcpy(mount_points[mount_points_size], e);
        mount_points_size++;
    }
    fclose(f);

    //分割完整路径，用于和挂载点做比较，抽取相对路径
    for (int i = 0; i < mount_points_size; i++) {
        int len = strlen(mount_points[i]);
        if (strncmp(fullpath, mount_points[i], len) == 0 &&
                strncmp("/", mount_points[i], len)) {
            strcpy(out_rel_path, fullpath + len);
            return 1;
        }
    }
    return 0;
}

static int do_md5(const char *path, char *outMD5)
{
    unsigned int i;
    int fd;
    MD5_CTX md5_ctx;
    unsigned char md5[MD5_DIGEST_LENGTH] = {0};

    fd = open(path, O_RDONLY);
    if (fd < 0) {
        ALOGW("could not open %s, %s\n", path, strerror(errno));
        return -1;
    }

    /* Note that bionic's MD5_* functions return void. */
    MD5_Init(&md5_ctx);

    while (1) {
        char buf[4096] = {0};
        ssize_t rlen;
        rlen = read(fd, buf, sizeof(buf));
        if (rlen == 0)
            break;
        else if (rlen < 0) {
            (void)close(fd);
            ALOGW("could not read %s, %s\n", path, strerror(errno));
            return -1;
        }
        MD5_Update(&md5_ctx, buf, rlen);
    }
    if (close(fd)) {
        ALOGW("could not close %s, %s\n", path, strerror(errno));
        return -1;
    }

    MD5_Final(md5, &md5_ctx);

    memset(outMD5, 0, MD5_DIGEST_LENGTH*2 + 1);

    for(int i=0;i<MD5_DIGEST_LENGTH;i++){
        char tmp[3] = {0};
        sprintf(tmp, "%02x", md5[i]);
        strcat(outMD5, tmp);
    }

    return 0;
}


static int getLinkOnSysBlock(dev_t devNum, char* outlink){
    struct stat   file_stat;
    DIR           *dp;
    char          ptr[MAX_PATH] = {0};

    sprintf(ptr, SYS_BLOCK_DIR"/%d:%d", MAJOR(devNum), MINOR(devNum));

    if (lstat(ptr, &file_stat) < 0) {
        ALOGW("Can not stat %s", ptr);
        return 0;
    }

    if (S_ISLNK(file_stat.st_mode)) {
        size_t size = 0;
        size = readlink(ptr, outlink, MAX_PATH);
        return size;
    }

    return 0;
}

static int writeCmd(const char* path, const char* md5, const char* syslink){
    if(access(RECOVERY_DIR, F_OK) && mkdir(RECOVERY_DIR, 0777) < 0){
        return 0;
    }

    FILE *fp;
    if((fp = fopen(RECOVERY_COMMAND_FILE, "w")) == NULL){
        return 0;
    }

    char command[2048] = {0};
    sprintf(command, EX_INSTALL_PACKAGE_CMD, path, md5, syslink);


    fwrite(command, 1, strlen(command), fp);
    fclose(fp);

    return 1;
}

static bool nativeInstallPackageEx(JNIEnv *env, jobject thiz, jobject context,
        jstring path) {
    const char* c_path = env->GetStringUTFChars(path, NULL);
    // 测试文件是否存在
    if (access(c_path, F_OK)) {
        ALOGW("Install package failed!Can not access %s", c_path);
        return false;
    }

    // 获得该文件所在设备的设备号
    struct stat file_stat;
    if(lstat(c_path, &file_stat) < 0){
        ALOGW("Install package failed!Can not stat %s", c_path);
        return false;
    }

    // 根据设备号查找设备的符号链接
    char link[MAX_PATH] = {0};
    if(getLinkOnSysBlock(file_stat.st_dev, link) == 0){
        ALOGW("Install package failed!Get link of device failed");
        return false;
    }

    // 计算文件的MD5值
    char md5[MD5_DIGEST_LENGTH*2+1] = {0};
    if(do_md5(c_path, md5) < 0){
        ALOGW("Install package failed!Can not get the MD5 of the file.");
        return false;
    }

    // 获得文件的相对路径
    char relative_path[MAX_PATH] = {0};
    if(get_relative_path(c_path, relative_path) == 0){
        ALOGW("Install package failed!Get relative path error.");
        return false;
    }

    // 写入命令到/cache/recovery/command
    if(writeCmd(relative_path, md5, link) == 0){
        ALOGW("Install package failed!Write command to failed.");
        return false;
    }

    return true;
}

static JNINativeMethod method_table[] = {
    { "nativeInstallPackageEx", "(Landroid/content/Context;Ljava/lang/String;)Z", (void*)nativeInstallPackageEx },
};

int register_softwinner_os_RecoverySystemEx(JNIEnv *env)
{
    return jniRegisterNativeMethods(env, "softwinner/os/RecoverySystemEx",
            method_table, NELEM(method_table));
}

};
