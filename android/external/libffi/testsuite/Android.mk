# Copyright 2007 The Android Open Source Project
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


# Single test file to use when doing a default build.
FFI_SINGLE_TEST_FILE := libffi.call/struct5.c

# We only build ffi at all for non-arm, non-x86 targets.
ifneq ($(TARGET_ARCH),arm)
    ifneq ($(TARGET_ARCH),x86)

        include $(CLEAR_VARS)

        LOCAL_SRC_FILES := $(FFI_SINGLE_TEST_FILE)
        LOCAL_C_INCLUDES := external/libffi/$(TARGET_OS)-$(TARGET_ARCH)
        LOCAL_SHARED_LIBRARIES := libffi

        LOCAL_MODULE := ffi-test
        LOCAL_MODULE_TAGS := tests

        include $(BUILD_EXECUTABLE)

    endif
endif
