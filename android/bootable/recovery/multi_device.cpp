#include <stdlib.h>
#include <stdio.h>
#include <sys/stat.h>
#include <linux/kdev_t.h>
#include <fcntl.h>
#include <unistd.h>
#include <dirent.h>
#include <string.h>
#include <sys/mount.h>
#include <errno.h>
#include <limits.h>
#include <sys/wait.h>
#include "android_runtime/AndroidRuntime.h"
#include "md5.h"
#include "multi_device.h"

#define DEV_BLOCK_DIR          "/dev/block"
#define SYS_BLOCK_DIR          "/sys/dev/block"
#define MOUNT_POINT            "/ex_update"
#define RECOVERY_COMMAND_FILE  "/cache/recovery/command"
#define EX_INSTALL_PACKAGE_CMD "%s %s %s"
#define CACHE_LINK_PATH        "/cache/update.zip"
#define MOUNT_EXFAT            "/sbin/mount.exfat"

#define SYSLNK_MAX_TEST_DEPTH (strlen("../../devices/platform/sw-ehci.x/usbx/x-x/x-x.x"))

#ifndef MD5_DIGEST_LENGTH
#define MD5_DIGEST_LENGTH 16
#endif

static int do_md5(const char *path, char *outMD5)
{
    unsigned int i;
    int fd;
    MD5_CTX md5_ctx;
    unsigned char md5[MD5_DIGEST_LENGTH];

    fd = open(path, O_RDONLY);
    if (fd < 0) {
        fprintf(stderr,"could not open %s, %s\n", path, strerror(errno));
        return -1;
    }

    /* Note that bionic's MD5_* functions return void. */
    MD5_Init(&md5_ctx);

    while (1) {
        char buf[4096];
        ssize_t rlen;
        rlen = read(fd, buf, sizeof(buf));
        if (rlen == 0)
            break;
        else if (rlen < 0) {
            (void)close(fd);
            fprintf(stderr,"could not read %s, %s\n", path, strerror(errno));
            return -1;
        }
        MD5_Update(&md5_ctx, buf, rlen);
    }
    if (close(fd)) {
        fprintf(stderr,"could not close %s, %s\n", path, strerror(errno));
        return -1;
    }

    MD5_Final(md5, &md5_ctx);

    memset(outMD5, 0, MD5_DIGEST_LENGTH*2 + 1);

    for(int i=0;i<MD5_DIGEST_LENGTH;i++){
        char tmp[3];
        sprintf(tmp, "%02x", md5[i]);
        strcat(outMD5, tmp);
    }

    return 0;
}

static int ensure_dev_mounted(const char * devPath, const char * mountedPoint) {
    int ret;
    if (devPath == NULL || mountedPoint == NULL) {
        return 0;
    }
    mkdir(mountedPoint, 0755); //in case it doesn't already exist
    ret = mount(devPath, mountedPoint, "vfat",
            MS_NOATIME | MS_NODEV | MS_NODIRATIME, "utf8");
    if (ret == 0) {
        fprintf(stderr,"mount %s with fs 'vfat' success\n", devPath);
        return 1;
    } else {
        ret = mount(devPath, mountedPoint, "ntfs",
                MS_NOATIME | MS_NODEV | MS_NODIRATIME, "utf8");
        if (ret == 0) {
            fprintf(stderr,"mount %s with fs 'ntfs' success\n", devPath);
            return 1;
        } else {
            ret = mount(devPath, mountedPoint, "ext4",
                    MS_NOATIME | MS_NODEV | MS_NODIRATIME, "utf8");
            if (ret == 0) {
                fprintf(stderr,"mount %s with fs 'ext4' success\n", devPath);
                return 1;
            } else {
                int status;
                pid_t pid = fork();
                if (pid > 0) {
                    /* Parent, wait for the child to return */
                    waitpid(pid, &status, 0);
                    if (WIFEXITED(status)) {
                        if (WEXITSTATUS(status) != 0) {
                            fprintf(stderr, "%s terminated by exit(%d) \n", MOUNT_EXFAT,
                                WEXITSTATUS(status));
                        }
                        else
                        {
                            fprintf(stderr,"mount %s with fs 'exfat' success\n", devPath);
                            return 1;
                        }
                    } else if (WIFSIGNALED(status))
                        fprintf(stderr, "%s terminated by signal %d \n", MOUNT_EXFAT,
                            WTERMSIG(status));
                    else if (WIFSTOPPED(status))
                        fprintf(stderr,  "%s stopped by signal %d \n", MOUNT_EXFAT,
                            WSTOPSIG(status));
                } else if (pid == 0) {
                    /* child, run checker */
                    fprintf(stderr,"try run %s\n", MOUNT_EXFAT);
                    if (execl(MOUNT_EXFAT, MOUNT_EXFAT, devPath,mountedPoint,"-o","noatime,nodiratime",(char *)NULL) < 0)
                    {
                        int err=errno;
                        fprintf(stderr,"Cannot run %s error %s \n", MOUNT_EXFAT, strerror(err));
                        exit(-1);
                    }
                } else {
                    /* No need to check for error in fork, we can't really handle it now */
                    fprintf(stderr,"Fork failed trying to run %s\n", "/sbin/mount.exfat");
                }
            }
        }
        fprintf(stderr,"failed to mount %s (%s)\n", devPath, strerror(errno));
        return 0;
    }
}

static int parseUpdateExCmd(const char *opt, char *relative_path, char *md5, char* syslink) {
    char tmp[2000];
    char *pstart, *pend;
    strcpy(tmp, opt);
    int i = 0;

    pstart = tmp;
    pend = strstr(pstart, ":");
    if(pend == NULL) return 0;
    strncpy(relative_path, pstart, pend - pstart);
    pstart = pend + 1;

    pend = strstr(pstart, ":");
    if(pend == NULL) return 0;
    strncpy(md5, pstart, pend - pstart);

    pstart = pend + 1;
    strcpy(syslink, pstart);

    return 1;
}

static int getNodeBySysLnk(char *syslink, char *outNode) {
    dirent *dirp;
    DIR *dp;
    char file[PATH_MAX] = {0};
    struct stat file_stat;

    //根据设备符号连接查找设备号
    FILE *fp;
    unsigned long long major = 0, minor = 0;
    int count = 0;

    if ((dp = opendir(SYS_BLOCK_DIR)) == NULL)
        return 0;
    while ((dirp = readdir(dp)) != NULL) {
        if (strcmp(dirp->d_name, ".") == 0 || strcmp(dirp->d_name, "..") == 0)
            continue;

        sprintf(file, SYS_BLOCK_DIR"/%s", dirp->d_name);

        lstat(file, &file_stat);
        if (S_ISLNK(file_stat.st_mode)) {
            char tmplink[PATH_MAX];
            size_t size = 0;
            memset(tmplink, 0, PATH_MAX);
            size = readlink(file, tmplink, PATH_MAX);
            fprintf(stdout,"getNodeBySysLnk:%s %s\n", file, tmplink);
            //比较其设备路径和设备号
            if(strncmp(syslink, tmplink, SYSLNK_MAX_TEST_DEPTH) == 0
                    && strcmp(syslink+strlen(syslink)-1, tmplink+strlen(tmplink)-1) == 0){
                count = sscanf(dirp->d_name, "%lld:%lld", &major, &minor);
                break;
            }
        }
    }
    closedir(dp);
    fprintf(stdout,"getNodeBySysLnk:The device number is %lld:%lld\n", major, minor);
    if(count != 2) return 0;
    dev_t devNum = MKDEV(major, minor);

    //根据设备号查找设备节点
    if ((dp = opendir(DEV_BLOCK_DIR)) == NULL)
        return 0;
    while ((dirp = readdir(dp)) != NULL) {
        if (strcmp(dirp->d_name, ".") == 0 || strcmp(dirp->d_name, "..") == 0)
            continue;

        sprintf(file, DEV_BLOCK_DIR"/%s", dirp->d_name);

        lstat(file, &file_stat);
        fprintf(stdout,"getNodeBySysLnk:%s %lld %lld\n", file, MAJOR(file_stat.st_rdev), MINOR(file_stat.st_rdev));
        if (S_ISBLK(file_stat.st_mode) && file_stat.st_rdev == devNum) {
            strcpy(outNode, file);
            closedir(dp);
            return 1;
        }
    }
    closedir(dp);
    return 0;
}

static int mountNode(char *node) {
    return ensure_dev_mounted(node, MOUNT_POINT);
}

static int testFile(char *relative_path, char *md5) {
    char full_path[PATH_MAX];
    sprintf(full_path, MOUNT_POINT"%s", relative_path);

    char file_md5[MD5_DIGEST_LENGTH * 2 + 1];
    if(do_md5(full_path, file_md5) == 0){
        return strcmp(file_md5, md5) == 0;
    }

    return 0;
}

int updateFromMutliDevice(const char *opt , char *outpath) {

    char relative_path[PATH_MAX] = {0};
    char md5[MD5_DIGEST_LENGTH * 2 + 1] = {0};
    char syslink[PATH_MAX] = {0};
    // 解析命令
    if (parseUpdateExCmd(opt, relative_path, md5, syslink) == 0) {
        fprintf(stderr,"Parse %s failed", opt);
        return 0;
    }
    fprintf(stdout,"Parse result:%s %s %s\n", relative_path, md5, syslink);

    // 根据设备符号链接获得设备节点
    char node[PATH_MAX];
    int times = 10;
    while (times--) {
        if (getNodeBySysLnk(syslink, node) == 0) {
            fprintf(stderr, "Get node by syslink %s failed, try again!Left %d\n", syslink, times);
            sleep(1);
            continue;
        }
        fprintf(stdout, "Get Node: %s\n", node);
        break;
    }
    if(times < 0) return 0;

    // 挂载设备到指定目录
    if (mountNode(node) == 0) {
        fprintf(stderr,"Mount node %s falied\n", node);
        return 0;
    }

    // 根据md5测试文件的正确性
    if (testFile(relative_path, md5) == 0) {
        fprintf(stderr,"There is not the correct file.\n");
        return 0;
    }

    // 创建升级包的符号链接
    char tmp[PATH_MAX]={0};
    sprintf(tmp, MOUNT_POINT"%s", relative_path);
    unlink(CACHE_LINK_PATH);
    if(symlink(tmp, CACHE_LINK_PATH)){
        fprintf(stderr, "Link %s to %s error, %s", tmp, CACHE_LINK_PATH, strerror(errno));
        return 0;
    }

    strcpy(outpath, CACHE_LINK_PATH);
    fprintf(stdout,"OK!Ready to start update.\n");

    return 1;
}
