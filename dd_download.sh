#!/bin/sh

# make partition table by fdisk command
# reserve part for fex binaries download 0~204799
# partition1 /dev/sdc1 vfat 204800~327679
# partition2 /dev/sdc2 ext4 327680~end

set -e

O=$1
P=lichee/tools/pack_brandy/out

sudo dd if=$P/boot0_sdcard.fex 	of=$O bs=1k seek=8
sudo dd if=$P/u-boot.fex 	of=$O bs=1k seek=19096
sudo dd if=$P/sunxi_mbr.fex 	of=$O bs=1k seek=20480
sudo dd if=$P/bootloader.fex	of=$O bs=1k seek=36864
sudo dd if=$P/env.fex 		of=$O bs=1k seek=69632
sudo dd if=$P/boot.fex 		of=$O bs=1k seek=86016
