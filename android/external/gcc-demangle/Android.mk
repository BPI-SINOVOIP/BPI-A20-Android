# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := cp-demangle.c
LOCAL_CFLAGS += -DHAVE_STRING_H -DHAVE_STDLIB_H -DIN_GLIBCPP_V3
LOCAL_MODULE := libgccdemangle
LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

##########################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := cp-demangle.c
LOCAL_CFLAGS += -DHAVE_STRING_H -DHAVE_STDLIB_H -DIN_GLIBCPP_V3
LOCAL_MODULE := libgccdemangle
LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false

include $(BUILD_HOST_SHARED_LIBRARY)

##########################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := test.c
LOCAL_SHARED_LIBRARIES := libgccdemangle
LOCAL_MODULE := gccdemangle_test
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)

##########################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := test.c
LOCAL_SHARED_LIBRARIES := libgccdemangle
LOCAL_MODULE := gccdemangle_test
LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_EXECUTABLE)
