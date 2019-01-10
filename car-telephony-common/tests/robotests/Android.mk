LOCAL_PATH := $(call my-dir)


############################################################
# CarArchCommon app just for Robolectric test target.     #
############################################################
include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME := CarTelephonyCommonAppForTesting
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_MODULE_TAGS := optional

LOCAL_USE_AAPT2 := true

LOCAL_PRIVILEGED_MODULE := true

LOCAL_STATIC_ANDROID_LIBRARIES := \
    androidx.car_car \
    car-arch-common \
    car-telephony-common

include $(BUILD_PACKAGE)

#############################################################
# car-telephony-common Robolectric test target.             #
#############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := CarTelephonyCommonRoboTests

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_RESOURCE_DIRS := config

# Include the testing libraries
LOCAL_JAVA_LIBRARIES := \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt

LOCAL_INSTRUMENTATION_FOR := CarTelephonyCommonAppForTesting

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_JAVA_LIBRARY)

##################################################################
# car-telephony-common runner target to run the previous target. #
##################################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunCarTelephonyCommonRoboTests

LOCAL_JAVA_LIBRARIES := \
    CarTelephonyCommonRoboTests \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt


LOCAL_TEST_PACKAGE := CarTelephonyCommonAppForTesting

LOCAL_INSTRUMENT_SOURCE_DIRS := $(dir $(LOCAL_PATH))../src

include external/robolectric-shadows/run_robotests.mk
