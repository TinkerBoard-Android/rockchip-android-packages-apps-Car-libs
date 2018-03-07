LOCAL_PATH := $(call my-dir)

############################################################
# CarSettingsLib app just for Robolectric test target.     #
############################################################
include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME := CarSettingsLib
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_MODULE_TAGS := optional

LOCAL_USE_AAPT2 := true

LOCAL_PRIVILEGED_MODULE := true

LOCAL_STATIC_ANDROID_LIBRARIES := \
    $(ANDROID_SUPPORT_CAR_TARGETS)

include packages/apps/Car/libs/car-settings-lib/car-settings-lib.mk

include $(BUILD_PACKAGE)

################################################
# Car SettingsLibrary Robolectric test target. #
################################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

# Include the testing libraries (JUnit4 + Robolectric libs).
LOCAL_STATIC_JAVA_LIBRARIES := \
    truth-prebuilt \
    mockito-robolectric-prebuilt

LOCAL_JAR_EXCLUDE_FILES := none

LOCAL_JAVA_LIBRARIES := \
    junit \
    platform-robolectric-3.6.1-prebuilt \
    sdk_vcurrent \
    robolectric_android-all-stub

LOCAL_INSTRUMENTATION_FOR := CarSettingsLib
LOCAL_MODULE := CarSettingsLibRoboTests

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_JAVA_LIBRARY)

##################################################################
# Car Settings Library runner target to run the previous target. #
##################################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunCarSettingsLibRoboTests

LOCAL_SDK_VERSION := current

LOCAL_STATIC_JAVA_LIBRARIES := \
    CarSettingsLibRoboTests

LOCAL_JAVA_LIBRARIES := \
    junit \
    platform-robolectric-3.6.1-prebuilt \
    sdk_vcurrent \
    robolectric_android-all-stub

LOCAL_TEST_PACKAGE := CarSettingsLib

LOCAL_ROBOTEST_FILES := $(filter-out %/BaseRobolectricTest.java,\
    $(call find-files-in-subdirs,$(LOCAL_PATH)/src,*Test.java,.))

LOCAL_INSTRUMENT_SOURCE_DIRS := $(dir $(LOCAL_PATH))../src

include prebuilts/misc/common/robolectric/3.6.1/run_robotests.mk
