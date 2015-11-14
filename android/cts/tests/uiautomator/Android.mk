# Copyright (C) 2012 The Android Open Source Project
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

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA)/local/tmp

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE := CtsUiAutomatorTests
LOCAL_JAVA_LIBRARIES := uiautomator_sdk_v17
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_CTS_TEST_APK := CtsUiAutomatorApp
LOCAL_CTS_TEST_APP_PACKAGE := com.android.cts.uiautomator
LOCAL_CTS_TEST_PACKAGE := android.uiautomator

include $(BUILD_CTS_UI_JAVA_LIBRARY)

# Build the test APK using its own makefile, and any other CTS-related packages
include $(call all-makefiles-under,$(LOCAL_PATH))
