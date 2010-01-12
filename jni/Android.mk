LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := socket-test
LOCAL_SRC_FILES := socket-test.c

include $(BUILD_SHARED_LIBRARY)
