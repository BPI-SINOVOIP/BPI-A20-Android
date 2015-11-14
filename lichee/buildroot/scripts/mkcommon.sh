#!/bin/bash
#
# scripts/mkcommon.sh
# (c) Copyright 2013
# Allwinner Technology Co., Ltd. <www.allwinnertech.com>
# James Deng <csjamesdeng@allwinnertech.com>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.

BR_SCRIPTS_DIR=`dirname $0`

if [ "$1" = "pack" ] ; then
    ${BR_SCRIPTS_DIR}/build_pack.sh $1
    exit 0
elif [ "$1" = "pack_dump" ]; then
	${BR_SCRIPTS_DIR}/build_dump.sh $1
	exit 0
elif [ "$1" = "pack_prvt" ]; then
	${BR_SCRIPTS_DIR}/build_prvt.sh $1
	exit 0
fi

# source shflags
. ${BR_SCRIPTS_DIR}/shflags/shflags

. ${BR_SCRIPTS_DIR}/mkcmd.sh

# define option, format:
#   'long option' 'default value' 'help message' 'short option'
DEFINE_string 'platform' 'sun7i' 'platform to build, e.g. sun7i' 'p'
DEFINE_string 'kernel' '3.4' 'kernel to build, e.g. 3.3' 'k'
DEFINE_string 'board' '' 'board to build, e.g. evb' 'b'
DEFINE_string 'module' '' 'module to build, e.g. buildroot, kernel, uboot, clean' 'm'
DEFINE_string 'boot' 'boot_v2.0' 'boot to build. e.g. boot_v1.0' 'v'
DEFINE_boolean 'independent' false 'output build to independent directory' 'i'

# parse the command-line
FLAGS "$@" || exit $?
eval set -- "${FLAGS_ARGV}"

chip=${FLAGS_platform%%_*}
platform=${FLAGS_platform##*_}
kernel=${FLAGS_kernel}
board=${FLAGS_board}
module=${FLAGS_module}
boot_version=${FLAGS_boot}

if [ "${platform}" = "${chip}" ] ; then
    mk_error "not support this command, please write the command refer to the help"
    #platform="linux"
    exit 1
fi

if [ -z "${module}" ] ; then
    module="all"
fi

if [ "${boot_version}" = "boot_v1.0" ] ; then
	LICHEE_UBOOT_DIR=${LICHEE_TOP_DIR}/boot-v1.0/u-boot
	if [ ! -d ${LICHEE_UBOOT_DIR} ] ; then
		mk_error "invalid boot_version '${boot_version}'"
		exit 1
	fi
	init_boot "1.0"
elif [ "${boot_version}" = "boot_v2.0" ] ; then
	LICHEE_UBOOT_DIR=${LICHEE_TOP_DIR}/brandy
	if [ ! -d ${LICHEE_UBOOT_DIR} ] ; then
		mk_error "invalid boot_version '${boot_version}'"
		exit 1
	fi
	init_boot "2.0"
else
	mk_error "invalid boot_version '${boot_version}'"
	exit 1
fi

if ! init_chips ${chip} || \
   ! init_platforms ${chip} ${platform} ; then
    mk_error "invalid platform '${FLAGS_platform}'"
    exit 1
fi

if [ ${FLAGS_board} ] && \
   ! init_boards ${chip} ${platform} ${board} ; then
    mk_error "invalid board '${FLAGS_board}'"
    exit 1
fi

if [ "${kernel}" = "3.3" ] ; then
    LICHEE_KERN_DIR=${LICHEE_TOP_DIR}/linux-3.3
    if [ ! -d ${LICHEE_KERN_DIR} ] ; then
        mk_error "invalid kernel '${kernel}'"
        exit 1
    fi
fi

## init output directory
init_outdir

if [ ${module} = "all" ]; then
    mklichee
elif [ ${module} = "boot" ] ; then
    mkboot
elif [ ${module} = "buildroot" ] ; then
    mkbr
elif [ ${module} = "kernel" ] ; then
    mkkernel
elif [ ${module} = "uboot" ] ; then
    mkuboot
elif [ ${module} = "clean" ] ; then
    mkclean
elif [ ${module} = "mrproer" ] ; then
    mkmrproer
elif [ ${module} = "distclean" ] ; then
    mkdistclean
else
    mk_error "invalid module '${module}'"
    exit 1
fi

exit $?

