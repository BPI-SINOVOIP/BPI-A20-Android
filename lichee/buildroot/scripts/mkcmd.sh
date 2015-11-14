# scripts/mkcmd.sh
#
# (c) Copyright 2013
# Allwinner Technology Co., Ltd. <www.allwinnertech.com>
# James Deng <csjamesdeng@allwinnertech.com>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.

# Notice:
#   1. This script muse source at the top directory of lichee.

function mk_error()
{
    echo -e "\033[47;31mERROR: $*\033[0m"
    help
}

function mk_warn()
{
    echo -e "\033[47;34mWARN: $*\033[0m"
}

function mk_info()
{
    echo -e "\033[47;30mINFO: $*\033[0m"
}

# define importance variable
LICHEE_TOP_DIR=`pwd`
LICHEE_BOOT_DIR=${LICHEE_TOP_DIR}/boot-v1.0/boot
LICHEE_BR_DIR=${LICHEE_TOP_DIR}/buildroot
LICHEE_KERN_DIR=${LICHEE_TOP_DIR}/linux-3.4
LICHEE_TOOLS_DIR=${LICHEE_TOP_DIR}/tools
LICHEE_UBOOT_DIR=${LICHEE_TOP_DIR}/brandy
LICHEE_OUT_DIR=${LICHEE_TOP_DIR}/out

# make surce at the top directory of lichee
if [ ! -d ${LICHEE_BOOT_DIR} -o \
     ! -d ${LICHEE_BR_DIR} -o \
     ! -d ${LICHEE_KERN_DIR} -o \
     ! -d ${LICHEE_TOOLS_DIR} -o \
     ! -d ${LICHEE_UBOOT_DIR} ] ; then
    mk_error "You are not at the top directory of lichee."
    mk_error "Please changes to that directory."
    exit 1
fi

# export importance variable
export LICHEE_TOP_DIR
export LICHEE_BOOT_DIR
export LICHEE_BR_DIR
export LICHEE_KERN_DIR
export LICHEE_TOOLS_DIR
export LICHEE_UBOOT_DIR
export LICHEE_OUT_DIR

# return true if used default config
function check_br_defconf()
{
    local defconf
    local outdir
    local ret=1
    
    if [ ${LICHEE_PLATFORM} = "linux" ] ; then
        defconf="${LICHEE_CHIP}_${LICHEE_PLATFORM}_${LICHEE_BOARD}_defconfig"
        if [ ! -f ${LICHEE_BR_DIR}/configs/${defconf} ] ; then
            ret=0
            defconf="${LICHEE_CHIP}_defconfig"
        fi
    else
        ret=0
        unset defconf
    fi
    export LICHEE_BR_DEFCONF=${defconf}

    return ${ret}
}

# return true if used default config
function check_kern_defconf()
{
    local defconf
    local ret=1
    if [ ${LICHEE_PLATFORM} = "linux" ] ; then
        defconf="${LICHEE_CHIP}smp_${LICHEE_PLATFORM}_${LICHEE_BOARD}_defconfig"
        if [ ! -f ${LICHEE_KERN_DIR}/arch/arm/configs/${defconf} ] ; then
            ret=0
            defconf="${LICHEE_CHIP}smp_defconfig"
        fi
    elif [ ${LICHEE_PLATFORM} = "dragonboard" ] ; then
         defconf="${LICHEE_CHIP}smp_${LICHEE_PLATFORM}_defconfig"
         if [ ! -f ${LICHEE_KERN_DIR}/arch/arm/configs/${defconf} ] ; then
             ret=0
             defconf="${LICHEE_CHIP}smp_android_defconfig"
             mk_info "use ${LICHEE_CHIP}smp_android_defconfig"
         fi
    else
        defconf="${LICHEE_CHIP}smp_${LICHEE_PLATFORM}_${LICHEE_BOARD}_defconfig"
        if [ ! -f ${LICHEE_KERN_DIR}/arch/arm/configs/${defconf} ] ; then
            ret=0
            defconf="${LICHEE_CHIP}smp_${LICHEE_PLATFORM}_defconfig"
        fi
    fi
    export LICHEE_KERN_DEFCONF=${defconf}

    return ${ret}
}

# return true if used default config
function check_uboot_defconf()
{
    local defconf
    local ret=1

    defconf="${LICHEE_CHIP}_${LICHEE_PLATFORM}_${LICHEE_BOARD}"
    if [ ! -f ${LICHEE_UBOOT_DIR}/include/configs/${defconf} ] ; then
        ret=0
        defconf="${LICHEE_CHIP}"
    fi
    export LICHEE_UBOOT_DEFCONF=${defconf}

    return ${ret}
}

function init_chips()
{
    local chip=$1
    local cnt=0
    local ret=1
    for chipdir in ${LICHEE_TOOLS_DIR}/$PACK_DIR/chips/* ; do
        chips[$cnt]=`basename $chipdir`
        if [ ${chips[$cnt]} = ${chip} ] ; then
            ret=0
            export LICHEE_CHIP=${chip}
        fi
        ((cnt+=1))
    done

    return ${ret}
}

function init_platforms()
{
    local chip=$1
    local platform=$2
    local cnt=0
    local ret=1
    for platdir in ${LICHEE_TOOLS_DIR}/$PACK_DIR/chips/${chip}/configs/* ; do
        platforms[$cnt]=`basename $platdir`
        if [ ${platforms[$cnt]} = ${platform} ] ; then
            ret=0
            export LICHEE_PLATFORM=${platform}
        fi
        ((cnt+=1))
    done

    return ${ret}
}

function init_boards()
{
    local chip=$1
    local platform=$2
    local board=$3
    local cnt=0
    local ret=1
    for boarddir in ${LICHEE_TOOLS_DIR}/$PACK_DIR/chips/${chip}/configs/${platform}/* ; do
        boards[$cnt]=`basename $boarddir`
        if [ ${boards[$cnt]} = ${board} ] ; then
            ret=0
            export LICHEE_BOARD=${board}
        fi
        ((cnt+=1))
    done

    return ${ret}
}

function init_boot()
{
	export BOOT_VERSION="$1"

	if [ ! -d ${LICHEE_OUT_DIR} ]; then
		mkdir ${LICHEE_OUT_DIR}
	fi

	if [ ! -e ${LICHEE_OUT_DIR}/pack_root.cfg ];then
		echo "PACK_ROOT=" > ${LICHEE_OUT_DIR}/pack_root.cfg
	fi

	if [ "${BOOT_VERSION}" = "1.0" ]; then
		sed -i 's/\(PACK_ROOT=\).*/\1tools\/pack_v1\.0/' ${LICHEE_OUT_DIR}/pack_root.cfg
		export PACK_DIR="pack_v1.0"
	elif [ "${BOOT_VERSION}" = "2.0" ]; then
		sed -i 's/\(PACK_ROOT=\).*/\1tools\/pack_brandy/' ${LICHEE_OUT_DIR}/pack_root.cfg
		export PACK_DIR="pack_brandy"	
	else
		echo -e "\033[0;31;1m#########################################\033[0m"
		echo -e "\033[0;31;1m#   make sure boot version is correct   #\033[0m"
		echo -e "\033[0;31;1m#########################################\033[0m"
		mkhelp
		exit 1
	fi

}

function selct_boot()
{
	if [ ! -e ${LICHEE_OUT_DIR}/pack_root.cfg ];then
		echo -e "\033[0;31;1m#   make sure boot version before pack  #\033[0m"
		echo -e "\033[0;31;1m#   please build the project again!     #\033[0m"
		exit 0
	fi

	PACK_ROOT_DIR=`sed -n '/PACK_ROOT=/'p ${LICHEE_OUT_DIR}/pack_root.cfg | sed 's/PACK_ROOT=//'`

	if [ "${PACK_ROOT_DIR}" = "tools/pack_v1.0" ]; then
		echo -e "\033[0;31;1m###################\033[0m"
		echo -e "\033[0;31;1m#   use boot1.0   #\033[0m"
		echo -e "\033[0;31;1m###################\033[0m"
	elif [ "${PACK_ROOT_DIR}" = "tools/pack_brandy" ]; then
		echo -e "\033[0;31;1m###################\033[0m"
		echo -e "\033[0;31;1m#   use boot2.0   #\033[0m"
		echo -e "\033[0;31;1m###################\033[0m"
	else
		echo -e "\033[0;31;1m################################################\033[0m"
		echo -e "\033[0;31;1m#        make sure boot version is correct     #\033[0m"
		echo -e "\033[0;31;1m#  your data is incorrect,build project again  #\033[0m"
		echo -e "\033[0;31;1m################################################\033[0m"
		exit 1
	fi
}

function select_chip()
{
    local cnt=0
    local choice

    printf "All available chips:\n"
    for chipdir in ${LICHEE_TOOLS_DIR}/$PACK_DIR/chips/* ; do
        chips[$cnt]=`basename $chipdir`
        printf "%4d. %s\n" $cnt ${chips[$cnt]}
        ((cnt+=1))
    done

    while true ; do
        read -p "Choice: " choice
        if [ -z "${choice}" ] ; then
            continue
        fi

        if [ -z "${choice//[0-9]/}" ] ; then
            if [ $choice -ge 0 -a $choice -lt $cnt ] ; then
                export LICHEE_CHIP="${chips[$choice]}"
                break
            fi
        fi
        printf "Invalid input ...\n"
    done
}

function select_platform()
{
    local cnt=0
    local choice
    
    select_chip
    
    printf "All available platforms:\n"
    for platdir in ${LICHEE_TOOLS_DIR}/$PACK_DIR/chips/${LICHEE_CHIP}/configs/* ; do
        platforms[$cnt]=`basename $platdir`
        printf "%4d. %s\n" $cnt ${platforms[$cnt]}
        ((cnt+=1))
    done
    
    while true ; do
        read -p "Choice: " choice
        if [ -z "${choice}" ] ; then
            continue
        fi

        if [ -z "${choice//[0-9]/}" ] ; then
            if [ $choice -ge 0 -a $choice -lt $cnt ] ; then
                export LICHEE_PLATFORM="${platforms[$choice]}"
                break
            fi
        fi
        printf "Invalid input ...\n"
    done
}

function select_board()
{
    local cnt=0
    local choice

    select_platform
    
    printf "All available boards:\n"
    for boarddir in ${LICHEE_TOOLS_DIR}/$PACK_DIR/chips/${LICHEE_CHIP}/configs/${LICHEE_PLATFORM}/* ; do
        boards[$cnt]=`basename $boarddir`
        if [ ${boards[$cnt]} = "default" ] ; then
            continue
        fi
        printf "%4d. %s\n" $cnt ${boards[$cnt]}
        ((cnt+=1))
    done
    
    while true ; do
        read -p "Choice: " choice
        if [ -z "${choice}" ] ; then
            continue
        fi

        if [ -z "${choice//[0-9]/}" ] ; then
            if [ $choice -ge 0 -a $choice -lt $cnt ] ; then
                export LICHEE_BOARD="${boards[$choice]}"
                break
            fi
        fi
        printf "Invalid input ...\n"
    done
}

# output to out/<chip>/<platform>/common directory only when
# both buildroot and kernel use default config file.
function init_outdir()
{
    if check_br_defconf && check_kern_defconf ; then
        export LICHEE_PLAT_OUT="${LICHEE_OUT_DIR}/${LICHEE_PLATFORM}/common"
        export LICHEE_BR_OUT="${LICHEE_PLAT_OUT}/buildroot"
    else
        export LICHEE_PLAT_OUT="${LICHEE_OUT_DIR}/${LICHEE_PLATFORM}/${LICHEE_BOARD}"
        export LICHEE_BR_OUT="${LICHEE_PLAT_OUT}/buildroot"
    fi

    mkdir -p ${LICHEE_BR_OUT}
}

function mksetting()
{
    printf "\n"
    printf "mkscript current setting:\n"
    printf "boot version: ${BOOT_VERSION}\n"
    printf "        Chip: ${LICHEE_CHIP}\n"
    printf "    Platform: ${LICHEE_PLATFORM}\n"
    printf "       Board: ${LICHEE_BOARD}\n"
    printf "  Output Dir: ${LICHEE_PLAT_OUT}\n"
    printf "\n"
}

function mkbr()
{
    mk_info "build buildroot ..."

    local build_script

    if check_br_defconf ; then
        if [ ${LICHEE_PLATFORM} = "linux" ] ; then
            build_script="scripts/build_${LICHEE_CHIP}.sh"
        else
            build_script="scripts/build_${LICHEE_CHIP}_${LICHEE_PLATFORM}.sh"
        fi
    else
        build_script="scripts/build_${LICHEE_CHIP}_${LICHEE_PLATFORM}_${LICHEE_BOARD}.sh"
    fi

    (cd ${LICHEE_BR_DIR} && [ -x ${build_script} ] && ./${build_script})
    [ $? -ne 0 ] && mk_error "build buildroot Failed" && return 1

    mk_info "build buildroot OK."
}

function clbr()
{
    mk_info "build buildroot ..."

    local build_script

    if check_br_defconf ; then
        if [ ${LICHEE_PLATFORM} = "linux" ] ; then
            build_script="scripts/build_${LICHEE_CHIP}.sh"
        else
            build_script="scripts/build_${LICHEE_CHIP}_${LICHEE_PLATFORM}.sh"
        fi
    else
        build_script="scripts/build_${LICHEE_CHIP}_${LICHEE_PLATFORM}_${LICHEE_BOARD}.sh"
    fi

    (cd ${LICHEE_BR_DIR} && [ -x ${build_script} ] && ./${build_script} "clean")

    mk_info "clean buildroot OK."
}

function prepare_toolchain()
{
    mk_info "prepare toolchain ..."
    tooldir=${LICHEE_BR_OUT}/external-toolchain
    if [ ! -d ${tooldir} ] ; then
        mkbr
    fi

    if ! echo $PATH | grep -q "${tooldir}" ; then
        export PATH=${tooldir}/bin:$PATH
    fi
}

function mkkernel()
{
    mk_info "build kernel ..."
    local build_script
    if check_kern_defconf ; then
        if [ ${LICHEE_PLATFORM} = "linux" ] ; then
            build_script="scripts/build_${LICHEE_CHIP}.sh"
        else
            build_script="scripts/build_${LICHEE_CHIP}_${LICHEE_PLATFORM}.sh"
        fi
    else
        build_script="scripts/build_${LICHEE_CHIP}_${LICHEE_PLATFORM}_${LICHEE_BOARD}.sh"
    fi

    prepare_toolchain

    # mark kernel .config belong to which platform
    local config_mark="${LICHEE_KERN_DIR}/.config.mark"
    if [ -f ${config_mark} ] ; then
        if ! grep -q "${LICHEE_PLATFORM}" ${config_mark} ; then
            mk_info "clean last time build for different platform"
            (cd ${LICHEE_KERN_DIR} && [ -x ${build_script} ] && ./${build_script} "clean")
            echo "${LICHEE_PLATFORM}" > ${config_mark}
        fi
    else
        echo "${LICHEE_PLATFORM}" > ${config_mark}
    fi

    (cd ${LICHEE_KERN_DIR} && [ -x ${build_script} ] && ./${build_script})
    [ $? -ne 0 ] && mk_error "build kernel Failed" && return 1

    mk_info "build kernel OK."
}

function clkernel()
{
    mk_info "clean kernel ..."

    local build_script

    if check_kern_defconf ; then
        if [ ${LICHEE_PLATFORM} = "linux" ] ; then
            build_script="scripts/build_${LICHEE_CHIP}.sh"
        else
            build_script="scripts/build_${LICHEE_CHIP}_${LICHEE_PLATFORM}.sh"
        fi
    else
        build_script="scripts/build_${LICHEE_CHIP}_${LICHEE_PLATFORM}_${LICHEE_BOARD}.sh"
    fi

    prepare_toolchain

    (cd ${LICHEE_KERN_DIR} && [ -x ${build_script} ] && ./${build_script} "clean")

    mk_info "clean kernel OK."
}

function mkuboot()
{
    mk_info "build u-boot ..."

    local build_script

    if check_uboot_defconf ; then
        build_script="build.sh"
    else
        build_script="build_${LICHEE_CHIP}_${LICHEE_PLATFORM}_${LICHEE_BOARD}.sh"
    fi

    prepare_toolchain

    (cd ${LICHEE_UBOOT_DIR} && [ -x ${build_script} ] && ./${build_script})
    [ $? -ne 0 ] && mk_error "build u-boot Failed" && return 1

    mk_info "build u-boot OK."
}

function cluboot()
{
    mk_info "clean u-boot ..."

    local build_script

    if check_uboot_defconf ; then
        build_script="build.sh"
    else
        build_script="build_${LICHEE_CHIP}_${LICHEE_PLATFORM}_${LICHEE_BOARD}.sh"
    fi

    prepare_toolchain

    (cd ${LICHEE_UBOOT_DIR} && [ -x ${build_script} ] && ./${build_script} "clean")

    mk_info "clean u-boot OK."
}

function mkboot()
{
    mk_info "build boot ..."
    mk_info "build boot OK."
}

function mkrootfs()
{
    mk_info "build rootfs ..."
    
    if [ ${LICHEE_PLATFORM} = "linux" ] ; then
        make O=${LICHEE_BR_OUT} -C ${LICHEE_BR_DIR} target-generic-getty-busybox
        [ $? -ne 0 ] && mk_error "build rootfs Failed" && return 1
        make O=${LICHEE_BR_OUT} -C ${LICHEE_BR_DIR} target-finalize
        [ $? -ne 0 ] && mk_error "build rootfs Failed" && return 1
        make O=${LICHEE_BR_OUT} -C ${LICHEE_BR_DIR} LICHEE_GEN_ROOTFS=y rootfs-ext4
        [ $? -ne 0 ] && mk_error "build rootfs Failed" && return 1
        cp ${LICHEE_BR_OUT}/images/rootfs.ext4 ${LICHEE_PLAT_OUT}
    elif [ ${LICHEE_PLATFORM} = "dragonboard" ] ; then
		echo "Regenerating dragonboard Rootfs..."
        (cd ${LICHEE_BR_DIR}/target/dragonboard; \
        	if [ ! -d "./rootfs" ]; then \
        	echo "extract dragonboard rootfs.tar.gz"; \
        	tar zxf rootfs.tar.gz; \
        	fi \
        )
		mkdir -p ${LICHEE_BR_DIR}/target/dragonboard/rootfs/lib/modules
        rm -rf ${LICHEE_BR_DIR}/target/dragonboard/rootfs/lib/modules/*
        cp -rf ${LICHEE_KERN_DIR}/output/lib/modules/* ${LICHEE_BR_DIR}/target/dragonboard/rootfs/lib/modules/
        (cd ${LICHEE_BR_DIR}/target/dragonboard; ./build.sh)
        cp ${LICHEE_BR_DIR}/target/dragonboard/rootfs.ext4 ${LICHEE_PLAT_OUT}
    else
        mk_info "skip make rootfs for ${LICHEE_PLATFORM}"
    fi

    mk_info "build rootfs OK."
}

function mklichee()
{
    mksetting

    mk_info "build lichee ..."

    # bpi, skip build uboot
    # mkbr && mkkernel && mkuboot && mkrootfs
    mkbr && mkkernel && mkrootfs

    [ $? -ne 0 ] && return 1

    mk_info "build lichee OK."
}

function mkclean()
{
    clkernel
    cluboot
    clbr

    mk_info "clean product output dir ..."
    rm -rf ${LICHEE_PLAT_OUT}
}

function mkdistclean()
{
    clkernel
    cluboot

    mk_info "clean entires output dir ..."
    rm -rf ${LICHEE_OUT_DIR}
}

function mkpack()
{
    mk_info "packing firmware ..."

    if [ -z "${LICHEE_CHIP}" -o \
         -z "${LICHEE_PLATFORM}" -o \
         -z "${LICHEE_BOARD}" ] ; then
        mk_error "invalid chip, platform or board."
        printf "\n"
        select_board
    fi

    (cd ${LICHEE_TOOLS_DIR}/$PACK_DIR && \
    ./pack -c ${LICHEE_CHIP} -p ${LICHEE_PLATFORM} -b ${LICHEE_BOARD})
}

function mkhelp()
{
    printf "
                mkscript - lichee build script

<version>: 0.9.3
<author >: james

<command>:
    mkboot      build boot
    mkbr        build buildroot
    mkkernel    build kernel
    mkuboot     build u-boot
    mkrootfs    build rootfs for linux, dragonboard
    mklichee    build total lichee
    
    mkclean     clean current board output
    mkdistclean clean entires output

    mkpack      pack firmware for lichee

    mksetting   show current setting
    mkhelp      show this message

"
}

function help()
{
	printf "
	NAME
		build - The top level build script for Lichee Linux BSP

	SYNOPSIS
		build [-h] | [-p platform] [-k kern_version] [-m module] [-v boot_version] | pack

	OPTIONS
		-h             Display help message
		-p [platform]  platform, e.g. sun7i_android, sun7i_dragonboard
					sun7i_android: android kernel for a20 platform
					sun7i_dragonboard: board test packages

		-k [kern_ver]  3.4(default)                          [OPTIONAL]

		-m [module]    Use this option when you dont want to build all. [OPTIONAL]
			e.g. kernel, buildroot, uboot, all(default)...

		-v [boot_version]       boot_version, e.g. boot_v1.0, boot_v2.0(default)...
			boot_v1.0
			boot_v2.0
		pack           To start pack program

	Examples:
		boot_v2.0:
			./build.sh -p sun7i_android
			./build.sh -p sun7i_dragonboard
			./build.sh pack
			./build.sh pack_dump
			./build.sh pack_prvt
		boot_v1.0:
			./build.sh -p sun7i_android -v boot_v1.0
			./build.sh -p sun7i_dragonboard -v boot_v1.0
			./build.sh pack
"
}
