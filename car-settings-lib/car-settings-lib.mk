#
# Copyright (C) 2018 The Android Open Source Project
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
#

#
# Include this file to utilize the car-settings-lib's resources and files.
#
# Make sure to include it after you've set all your desired LOCAL variables.
# Note that you must explicitly set your LOCAL_RESOURCE_DIR before including this file.
#
# For example:
#
#   LOCAL_RESOURCE_DIR := \
#        $(LOCAL_PATH)/res
#
#   include packages/apps/Car/libs/car-settings-lib/car-settings-lib.mk
#

# Check that LOCAL_RESOURCE_DIR is defined
ifeq (,$(LOCAL_RESOURCE_DIR))
$(error LOCAL_RESOURCE_DIR must be defined)
endif

# Include car-settings-lib
ifeq (,$(findstring car-settings-lib, $(LOCAL_STATIC_ANDROID_LIBRARIES)))
LOCAL_STATIC_ANDROID_LIBRARIES += car-settings-lib
endif