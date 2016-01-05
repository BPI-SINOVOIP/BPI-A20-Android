#!/bin/bash
#cd ./lichee
#./build.sh -p sun7i_android
#cd ..
cd ./android
ls
source build/envsetup.sh
lunch bpi_m1plus_lcd-userdebug
#extract-bsp
#make -j8 bootimage
pack
cd ../lichee/tools/pack_brandy
ls -l
