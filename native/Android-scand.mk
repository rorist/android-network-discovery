LOCAL_PATH := $(call my-dir)

# Static library
include $(CLEAR_VARS)

LOCAL_MODULE    := libscan
LOCAL_SRC_FILE  := libscan.c
LOCAL_LDLIBS    := -llog

include $(BUILD_STATIC_LIBRARY)
#include $(BUILD_SHARED_LIBRARY)

# Scan daemon
include $(CLEAR_VARS)

LOCAL_MODULE            := scand
LOCAL_SRC_FILES         := scand.c
LOCAL_STATIC_LIBRARIES  := libscan
#LOCAL_SHARED_LIBRARIES  := libscan

LOCAL_C_INCLUDES := $(call include-path-for, system-core)/cutils
LOCAL_SHARED_LIBRARIES := libcutils

include $(BUILD_EXECUTABLE)
