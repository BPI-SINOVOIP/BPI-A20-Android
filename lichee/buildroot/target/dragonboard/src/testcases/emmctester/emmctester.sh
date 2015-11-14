#!/bin/sh

source send_cmd_pipe.sh
source script_parser.sh

flashdev="/dev/mmcblk"
mountpoint="/tmp/flash"

if [ ! -d "/sys/devices/platform/sunxi-mmc.2" ]; then
	SEND_CMD_PIPE_FAIL $3
    exit 1
fi
cd "/sys/devices/platform/sunxi-mmc.2/mmc_host/"; a=`ls`;cd -
devindex=${a:3:1}
flashdev=$flashdev$devindex
echo "flashdev=$flashdev"

mkfs.vfat $flashdev
if [ $? -ne 0 ]; then
    SEND_CMD_PIPE_FAIL $3
    exit 1
fi
echo "create vfat file system for /dev/nanda done"

if [ ! -d $mountpoint ]; then
    mkdir $mountpoint
fi

mount $flashdev $mountpoint
if [ $? -ne 0 ]; then
    SEND_CMD_PIPE_FAIL $3
    exit 1
fi
echo "mount $flashdev to $mountpoint OK"

capacity=`df -h | grep $flashdev | awk '{printf $2}'`
echo "flash capacity: $capacity"
SEND_CMD_PIPE_MSG $3 $capacity

total_size=`busybox df -m | grep $flashdev | awk '{printf $2}'`
echo "total_size=$total_size"
test_size=`script_fetch "emmc" "test_size"`
if [ -z "$test_size" -o $test_size -le 0 -o $test_size -gt $total_size ]; then
    test_size=64
fi

echo "test_size=$test_size"
echo "emmc test read and write"
emmcrw "$mountpoint/test.bin" "$test_size"
if [ $? -ne 0 ]; then
    SEND_CMD_PIPE_FAIL $3
else
    SEND_CMD_PIPE_OK_EX $3 $capacity
fi
