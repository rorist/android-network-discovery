LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libcommand
LOCAL_SRC_FILE  := command.c
LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)

include $(BUILD_SHARED_LIBRARY)
