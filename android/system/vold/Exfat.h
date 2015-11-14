#ifndef __EXFAT_H__
#define __EXFAT_H__

#include <unistd.h>

class Exfat {
public:
    static int check(const char *fsPath);
    static int doMount(const char *fsPath, const char *mountPoint,
                       bool ro, bool remount, bool executable,
                       int ownerUid, int ownerGid, int permMask,
                       bool createLost);
    static int format(const char *fsPath, unsigned int numSectors);
};

#endif
