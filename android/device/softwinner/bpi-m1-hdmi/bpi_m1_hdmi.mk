# wing fpga product config

$(call inherit-product, device/softwinner/wing-common/ProductCommon.mk)

DEVICE_PACKAGE_OVERLAYS := device/softwinner/bpi-m1-hdmi/overlay

PRODUCT_COPY_FILES += \
	device/softwinner/bpi-m1-hdmi/modules/modules/nand.ko:root/nand.ko \
	device/softwinner/bpi-m1-hdmi/modules/modules/sun7i-ir.ko:root/sun7i-ir.ko 

PRODUCT_COPY_FILES += \
	device/softwinner/bpi-m1-hdmi/kernel:kernel \
	device/softwinner/bpi-m1-hdmi/recovery.fstab:recovery.fstab \
	frameworks/native/data/etc/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
	frameworks/base/data/etc/android.hardware.wifi.direct.xml:system/etc/permissions/android.hardware.wifi.direct.xml 

PRODUCT_COPY_FILES += \
	device/softwinner/bpi-m1-hdmi/ueventd.sun7i.rc:root/ueventd.sun7i.rc \
	device/softwinner/bpi-m1-hdmi/init.sun7i.rc:root/init.sun7i.rc \
	device/softwinner/bpi-m1-hdmi/init.sun7i.usb.rc:root/init.sun7i.usb.rc \
	device/softwinner/bpi-m1-hdmi/init.recovery.sun7i.rc:root/init.recovery.sun7i.rc \
	device/softwinner/bpi-m1-hdmi/camera.cfg:system/etc/camera.cfg \
	device/softwinner/bpi-m1-hdmi/media_profiles.xml:system/etc/media_profiles.xml \
	frameworks/native/data/etc/android.hardware.camera.xml:system/etc/permissions/android.hardware.camera.xml \
        frameworks/native/data/etc/tablet_core_hardware.xml:system/etc/permissions/tablet_core_hardware.xml

#input device config
PRODUCT_COPY_FILES += \
	device/softwinner/bpi-m1-hdmi/sw-keyboard.kl:system/usr/keylayout/sw-keyboard.kl \
	device/softwinner/bpi-m1-hdmi/sun7i-ir.kl:system/usr/keylayout/sun7i-ir.kl \
	device/softwinner/bpi-m1-hdmi/tp.idc:system/usr/idc/tp.idc 

#PRODUCT_COPY_FILES += \
	device/softwinner/bpi-m1-hdmi/initlogo.rle:root/initlogo.rle

PRODUCT_COPY_FILES += \
	device/softwinner/bpi-m1-hdmi/vold.fstab:system/etc/vold.fstab
	
#PRODUCT_PACKAGES += \
	TvdLauncher \
	TSCalibration2

# wifi & bt config file
#PRODUCT_COPY_FILES += \
	frameworks/native/data/etc/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
    	frameworks/native/data/etc/android.hardware.wifi.direct.xml:system/etc/permissions/android.hardware.wifi.direct.xml \
    	frameworks/native/data/etc/android.hardware.bluetooth.xml:system/etc/permissions/android.hardware.bluetooth.xml \
    	system/bluetooth/data/main.nonsmartphone.conf:system/etc/bluetooth/main.conf

# bpi, simcom prebuild libs
#PRODUCT_PACKAGES += \
	librilutils \
	radiooptions \
	init.gprs-pppd \
	3gdata_call.conf 

# rtl8723as bt fw and config
#PRODUCT_COPY_FILES += \
	device/softwinner/wing-common/hardware/realtek/bluetooth/rtl8723as/rlt8723a_chip_b_cut_bt40_fw.bin:system/etc/rlt8723a_chip_b_cut_bt40_fw.bin \
	device/softwinner/wing-common/hardware/realtek/bluetooth/rtl8723as/rtk8723_bt_config:system/etc/rtk8723_bt_config

# bcm40181 sdio wifi fw and nvram
#PRODUCT_COPY_FILES += \
	hardware/broadcom/wlan/firmware/bcm40181/fw_bcm40181a2_p2p.bin:system/vendor/modules/fw_bcm40181a2_p2p.bin \
	hardware/broadcom/wlan/firmware/bcm40181/fw_bcm40181a2_apsta.bin:system/vendor/modules/fw_bcm40181a2_apsta.bin \
	hardware/broadcom/wlan/firmware/bcm40181/fw_bcm40181a2.bin:system/vendor/modules/fw_bcm40181a2.bin \
	hardware/broadcom/wlan/firmware/bcm40181/bcm40181_nvram.txt:system/vendor/modules/bcm40181_nvram.txt

# bcm40183 sdio wifi fw and nvram
#PRODUCT_COPY_FILES += \
	hardware/broadcom/wlan/firmware/bcm40183/fw_bcm40183b2_p2p.bin:system/vendor/modules/fw_bcm40183b2_p2p.bin \
	hardware/broadcom/wlan/firmware/bcm40183/fw_bcm40183b2_apsta.bin:system/vendor/modules/fw_bcm40183b2_apsta.bin \
	hardware/broadcom/wlan/firmware/bcm40183/fw_bcm40183b2.bin:system/vendor/modules/fw_bcm40183b2.bin \
	hardware/broadcom/wlan/firmware/bcm40183/bcm40183_nvram.txt:system/vendor/modules/bcm40183_nvram.txt

# ap6210 sdio wifi fw and nvram
#PRODUCT_COPY_FILES += \
	hardware/broadcom/wlan/firmware/ap6210/fw_bcm40181a2.bin:system/vendor/modules/fw_bcm40181a2.bin \
	hardware/broadcom/wlan/firmware/ap6210/fw_bcm40181a2_apsta.bin:system/vendor/modules/fw_bcm40181a2_apsta.bin \
	hardware/broadcom/wlan/firmware/ap6210/fw_bcm40181a2_p2p.bin:system/vendor/modules/fw_bcm40181a2_p2p.bin \
	hardware/broadcom/wlan/firmware/ap6210/nvram_ap6210.txt:system/vendor/modules/nvram_ap6210.txt \
	hardware/broadcom/wlan/firmware/ap6210/bcm20710a1.hcd:system/vendor/modules/bcm20710a1.hcd
	
#PRODUCT_COPY_FILES += \
	hardware/broadcom/wlan/firmware/ap6181/fw_bcm40181a2.bin:system/vendor/modules/fw_bcm40181a2.bin \
	hardware/broadcom/wlan/firmware/ap6181/fw_bcm40181a2_apsta.bin:system/vendor/modules/fw_bcm40181a2_apsta.bin \
	hardware/broadcom/wlan/firmware/ap6181/fw_bcm40181a2_p2p.bin:system/vendor/modules/fw_bcm40181a2_p2p.bin \
	hardware/broadcom/wlan/firmware/ap6181/nvram_ap6181.txt:system/vendor/modules/nvram_ap6181.txt 
#	hardware/broadcom/wlan/firmware/ap6181/bcm20710a1.hcd:system/vendor/modules/bcm20710a1.hcd
	
PRODUCT_PROPERTY_OVERRIDES += \
	dalvik.vm.heapsize=256m \
	dalvik.vm.heapstartsize=8m \
	dalvik.vm.heapgrowthlimit=96m \
	dalvik.vm.heaptargetutilization=0.75 \
	dalvik.vm.heapminfree=2m \
	dalvik.vm.heapmaxfree=8m \
	persist.sys.usb.config=mass_storage,adb \
	ro.property.tabletUI=true \
	ro.udisk.lable=BPI_M1 \
	ro.product.firmware=v2.1 \
	ro.sw.defaultlauncherpackage=com.softwinner.launcher \
	ro.sw.defaultlauncherclass=com.softwinner.launcher.Launcher \
	audio.output.active=AUDIO_CODEC \
	audio.input.active=AUDIO_CODEC \
	ro.audio.multi.output=true \
	ro.sw.directlypoweroff=true \
	ro.softmouse.left.code=6 \
 	ro.softmouse.right.code=14 \
    	ro.softmouse.top.code=67 \
    	ro.softmouse.bottom.code=10 \
    	ro.softmouse.leftbtn.code=2 \
    	ro.softmouse.midbtn.code=-1 \
    	ro.softmouse.rightbtn.code=-1 \
    	ro.sw.shortpressleadshut=false \
    	ro.sw.videotrimming=1 \
    	persist.sys.device_name = MiniMax 
	

$(call inherit-product-if-exists, device/softwinner/bpi-m1-hdmi/modules/modules.mk)

PRODUCT_CHARACTERISTICS := tablet

# Overrides
PRODUCT_BRAND  := BPI
PRODUCT_NAME   := bpi_m1_hdmi
PRODUCT_DEVICE := bpi-m1-hdmi
PRODUCT_MODEL  := BPI-M1

