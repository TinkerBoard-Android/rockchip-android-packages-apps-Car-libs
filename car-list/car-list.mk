#
# Copyright (C) 2017 The Android Open Source Project
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
# Include this file to utilize the car-list's resources and files.
#
# Make sure to include it after you've set all your desired LOCAL variables.
# Note that you must explicitly set your LOCAL_RESOURCE_DIR before including this file.
#
# For example:
#
#   LOCAL_RESOURCE_DIR := \
#        $(LOCAL_PATH)/res
#
#   include packages/apps/Car/libs/car-list/car-list.mk
#

ifeq (,$(LOCAL_RESOURCE_DIR))
$(error LOCAL_RESOURCE_DIR must be defined)
endif

# Include support-v7-cardview, if not already included
ifeq (,$(findstring android-support-v7-cardview,$(LOCAL_STATIC_ANDROID_LIBRARIES)))
LOCAL_STATIC_ANDROID_LIBRARIES += android-support-v7-cardview
endif

# Include support-v7-appcompat, if not already included
ifeq (,$(findstring android-support-v7-appcompat,$(LOCAL_STATIC_ANDROID_LIBRARIES)))
LOCAL_STATIC_ANDROID_LIBRARIES += android-support-v7-appcompat
endif

# Include support-v7-recyclerview, if not already included
ifeq (,$(findstring android-support-v7-recyclerview,$(LOCAL_STATIC_ANDROID_LIBRARIES)))
LOCAL_STATIC_ANDROID_LIBRARIES += android-support-v7-recyclerview
endif

# Include car-list
ifeq (,$(findstring car-list, $(LOCAL_STATIC_ANDROID_LIBRARIES)))
LOCAL_STATIC_ANDROID_LIBRARIES += car-list
endif