LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := command.c 

LOCAL_SHARED_LIBRARIES := libcutils

LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)

LOCAL_MODULE := libcommand

include $(BUILD_SHARED_LIBRARY)
