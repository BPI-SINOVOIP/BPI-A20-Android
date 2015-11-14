#!/bin/sh
###############################################################################
# \version     1.0.0
# \date        2012年09月26日
# \author      Martin <zhengjiewen@allwinnertech.com>
# \Descriptions:
#                       create the inital version
###############################################################################
source send_cmd_pipe.sh

#while true; do
#    sata_tmp=`fdisk -l | grep "Disk /dev/sda" | awk '{print $2}'`
#    sata_dev=`echo $sata_tmp | awk -F: '{print $1}'`
#    echo "sata_dev=$sata_dev"
#
#        if [ -n "$sata_dev" ];  then
#                SEND_CMD_PIPE_OK_EX $3 "sata_dev=$sata_dev"
#                #SEND_CMD_PIPE_OK
#        fi
#        sleep 1
#done

while true; do
	for nr in a b c d e f g h i j k l m n o p q r s t u v w x y z; do
    	sata="sd$nr"
    	sata_tmp=`find /sys/devices/platform/sw_ahci.0/ -name $sata`

    	if [ `echo $sata_tmp | grep $sata` ]; then
    		SEND_CMD_PIPE_OK_EX $3 "sata_dev=$sata"
    	fi
    	usleep 200000
    done
done
