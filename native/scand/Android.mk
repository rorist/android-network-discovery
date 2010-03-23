LOCAL_PATH := $(call my-dir)

# Scan daemon
include $(CLEAR_VARS)

LOCAL_MODULE            := scand
LOCAL_SRC_FILES         := scand.c discover.c

LOCAL_C_INCLUDES := $(call include-path-for, system-core)/cutils
LOCAL_SHARED_LIBRARIES := libcutils

include $(BUILD_EXECUTABLE)
