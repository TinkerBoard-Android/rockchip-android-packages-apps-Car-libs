LOCAL_PATH := $(call my-dir)

############################################################
# CarAssistClient app just for Robolectric test target.     #
############################################################
include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME := CarAssistClient
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_MODULE_TAGS := optional

LOCAL_USE_AAPT2 := true

LOCAL_PRIVILEGED_MODULE := true

LOCAL_JAVA_LIBRARIES := android.car

LOCAL_STATIC_ANDROID_LIBRARIES := \
    car-assist-client-lib

include $(BUILD_PACKAGE)

#############################################
# Car-Assist-Client Robolectric test target. #
#############################################
include $(CLEAR_VARS)

LOCAL_MODULE := CarAssistClientRoboTests
LOCAL_MODULE_CLASS := JAVA_LIBRARIES

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_RESOURCE_DIRS := config

# Include the testing libraries
LOCAL_JAVA_LIBRARIES := \
    android.car \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt

LOCAL_INSTRUMENTATION_FOR := CarAssistClient

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_JAVA_LIBRARY)

#############################################################
# CarAssistClient runner target to run the previous target. #
#############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunCarAssistClientRoboTests

LOCAL_SDK_VERSION := current

LOCAL_STATIC_JAVA_LIBRARIES := \
    CarAssistClientRoboTests

LOCAL_JAVA_LIBRARIES := \
    CarAssistClientRoboTests \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt \
    android.car

LOCAL_TEST_PACKAGE := CarAssistClient

LOCAL_ROBOTEST_FILES := \
    $(call find-files-in-subdirs,$(LOCAL_PATH)/src,*Test.java,.))

LOCAL_INSTRUMENT_SOURCE_DIRS := $(dir $(LOCAL_PATH))../src

include external/robolectric-shadows/run_robotests.mk
