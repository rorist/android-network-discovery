LOCAL_PATH := $(call my-dir)

# Library
include $(CLEAR_VARS)

LOCAL_MODULE    := libscan
LOCAL_SRC_FILE  := libscan.c
#LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)

# Scan daemon
#include $(CLEAR_VARS)

#LOCAL_SRC_FILES         := scand.c
#LOCAL_SHARED_LIBRARIES  := libscan
#LOCAL_LDLIBS            := -llog
#LOCAL_MODULE            := scand

#include $(BUILD_EXECUTABLE)
