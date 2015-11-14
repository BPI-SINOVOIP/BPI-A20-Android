#!/bin/bash
function cdevice()
{	
	cd $DEVICE
}

function cout()
{
	cd $OUT	
}

function extract-bsp()
{
	LICHEE_DIR=$ANDROID_BUILD_TOP/../lichee
	LINUXOUT_DIR=$LICHEE_DIR/out/android/common
	LINUXOUT_MODULE_DIR=$LICHEE_DIR/out/android/common/lib/modules/*/*
	CURDIR=$PWD

	cd $DEVICE

	#extract kernel
	if [ -f kernel ]; then
		rm kernel
	fi
	cp $LINUXOUT_DIR/bImage kernel
	echo "$DEVICE/bImage copied!"

	#extract linux modules
	if [ -d modules ]; then
		rm -rf modules
	fi
	mkdir -p modules/modules
	cp -rf $LINUXOUT_MODULE_DIR modules/modules
	echo "$DEVICE/modules copied!"
	chmod 0755 modules/modules/*

# create modules.mk
(cat << EOF) > ./modules/modules.mk 
# modules.mk generate by extract-files.sh , do not edit it !!!!
PRODUCT_COPY_FILES += \\
	\$(call find-copy-subdir-files,*,\$(LOCAL_PATH)/modules,system/vendor/modules)

EOF

	cd $CURDIR
}

function make-all()
{
	# check lichee dir
	LICHEE_DIR=$ANDROID_BUILD_TOP/../lichee
	if [ ! -d $LICHEE_DIR ] ; then
		echo "$LICHEE_DIR not exists!"
		return
	fi

	extract-bsp
	m -j8

}

function pack()
{
	T=$(gettop)

	if [ ! -e ${T}/../lichee/out/pack_root.cfg ]; then
		echo -e "\033[0;31;1m#   make sure boot version before pack  #\033[0m"
		echo -e "\033[0;31;1m#   please build the project again!     #\033[0m"
		return
	fi

	PACK_DIR=`sed -n '/PACK_ROOT=/'p ${T}/../lichee/out/pack_root.cfg | sed 's/PACK_ROOT=//'`

	if [ "${PACK_DIR}" = "tools/pack_v1.0" ]; then
		echo -e "\033[0;31;1m###################\033[0m"
		echo -e "\033[0;31;1m#   use boot1.0   #\033[0m"
		echo -e "\033[0;31;1m###################\033[0m"
	elif [ "${PACK_DIR}" = "tools/pack_brandy" ]; then
		echo -e "\033[0;31;1m###################\033[0m"
		echo -e "\033[0;31;1m#   use boot2.0   #\033[0m"
		echo -e "\033[0;31;1m###################\033[0m"
	else
		echo -e "\033[0;31;1m################################################\033[0m"
		echo -e "\033[0;31;1m#        make sure boot version is correct     #\033[0m"
		echo -e "\033[0;31;1m#  your data is incorrect,build lichee again  #\033[0m"
		echo -e "\033[0;31;1m################################################\033[0m"
		return
	fi

	export ANDROID_IMAGE_OUT=$OUT
	export PACKAGE=$T/../lichee/$PACK_DIR

	sh $DEVICE/package.sh $@
}

function exdroid_diff()
{
	echo "please check v1, v2 in build/tools/exdroid_diff.sh (^C to break)"
	read
	repo forall -c '$ANDROID_BUILD_TOP/build/tools/exdroid_diff.sh'	
}

function exdroid_patch()
{
	echo "please confirm this is v1 (^C to break)"
	read
	repo forall -c '$ANDROID_BUILD_TOP/build/tools/exdroid_patch.sh'	
}

function get_uboot()
{
   pack
   rm $OUT/boot_info
   echo "-------------------------------------"
   if [ ! -e ${T}/../lichee/out/pack_root.cfg ];then
     echo -e "\033[0;31;1m#     should make sure boot version before pack        #\033[0m"
     echo -e "\033[0;31;1m#   please build the project again or lunch again!     #\033[0m"
     return 0
   fi

   PACK_DIR=`sed -n '/PACK_ROOT=/'p ${T}/../lichee/out/pack_root.cfg | sed 's/PACK_ROOT=//'`

   if [ "${PACK_DIR}" = "tools/pack_v1.0" ]; then
     echo -e "\033[0;31;1m###################\033[0m"
     echo -e "\033[0;31;1m#   use boot1.0   #\033[0m"
     echo -e "\033[0;31;1m###################\033[0m"
     if [ ! -e $OUT/bootloader ];then
         mkdir $OUT/bootloader
     fi
     rm -rf $OUT/bootloader/*
     cp $PACKAGE/out/bootloader.fex $OUT
     cp -r $PACKAGE/out/bootfs/* $OUT/bootloader/
     echo "cp $PACKAGE/out/bootloader.fex to $OUT"
     echo "cp -r $PACKAGE/out/bootfs/* $OUT/bootloader"
     cp $PACKAGE/out/env.fex $OUT
     cp $PACKAGE/out/boot0_nand.bin $OUT/boot0_nand.fex
     cp $PACKAGE/out/boot0_sdcard.fex $OUT/boot0_sdcard.fex
     cp $PACKAGE/out/boot1_nand.fex $OUT/uboot_nand.fex
     cp $PACKAGE/out/boot1_sdcard.fex $OUT/uboot_sdcard.fex
     echo "boot_version=1.0" > $OUT/boot_info
     echo "cp $PACKAGE/out/env.fex $OUT"

   elif [ "${PACK_DIR}" = "tools/pack_brandy" ]; then
     echo -e "\033[0;31;1m###################\033[0m"
     echo -e "\033[0;31;1m#   use boot2.0   #\033[0m"
     echo -e "\033[0;31;1m###################\033[0m"
     if [ ! -e $OUT/bootloader ];then
         mkdir $OUT/bootloader
     fi
     rm -rf $OUT/bootloader/*
     cp $PACKAGE/out/bootloader.fex $OUT
     cp -r $PACKAGE/out/boot-resource/* $OUT/bootloader/
     echo "cp $PACKAGE/out/bootloader.fex to $OUT"
     echo "cp -r $PACKAGE/out/boot-resource/* $OUT/bootloader"
     cp $PACKAGE/out/env.fex $OUT
     cp $PACKAGE/out/boot0_nand.fex $OUT/boot0_nand.fex
     cp $PACKAGE/out/boot0_sdcard.fex $OUT/boot0_sdcard.fex
     cp $PACKAGE/out/u-boot.fex $OUT/uboot_nand.fex
     cp $PACKAGE/out/u-boot.fex $OUT/uboot_sdcard.fex
     echo "boot_version=2.0" > $OUT/boot_info
     echo "cp $PACKAGE/out/env.fex $OUT/"
   else
     echo -e "\033[0;31;1m#  get the uboot error maybe your data is incorrect     #\033[0m"
     echo -e "\033[0;31;1m#   please build the project again or lunch again!      #\033[0m"
   fi
}

function make_ota_target_file()
{
    get_uboot
    echo "rm $OUT/obj/PACKAGING/target_files_intermediates/"
    rm -rf $OUT/obj/PACKAGING/target_files_intermediates/
    echo "---make target-files-package---"
    make target-files-package

}

function make_ota_package()
{
    get_uboot
    echo "rm $OUT/obj/PACKAGING/target_files_intermediates/"
    rm -rf $OUT/obj/PACKAGING/target_files_intermediates/
    echo "----make otapackage ----"
    make otapackage
}

function make_ota_package_inc()
{
    mv *.zip old_target_files.zip
    get_uboot
    echo "rm $OUT/obj/PACKAGING/target_files_intermediates/"
    rm -rf $OUT/obj/PACKAGING/target_files_intermediates/
    echo "----make otapackage_inc----"
    make otapackage_inc
}
