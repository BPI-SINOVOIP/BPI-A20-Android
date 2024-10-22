# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)

# bpi, prebuild lib used if have simcom modem
ifneq ($(TARGET_SIMCOM_USED),true)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	rild.c


LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libril \
	libdl

LOCAL_CFLAGS := -DRIL_SHLIB

LOCAL_MODULE:= rild
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)

# For radiooptions binary
# =======================
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	radiooptions.c

LOCAL_SHARED_LIBRARIES := \
	libcutils \

LOCAL_CFLAGS := \

LOCAL_MODULE:= radiooptions
LOCAL_MODULE_TAGS := debug

include $(BUILD_EXECUTABLE)

endif
