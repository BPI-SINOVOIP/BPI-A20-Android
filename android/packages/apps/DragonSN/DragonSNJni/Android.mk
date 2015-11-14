LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
#OPENCV_LIB_TYPE := STATIC
APP_ABI := all
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := liballwinnertech_read_private

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_SRC_FILES := native.c \
			fetch_env.c
			
LOCAL_SHARED_LIBRARIES := liblog
include $(BUILD_SHARED_LIBRARY)
