#
# Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
# Licensed under GNU's GPL 2, see README
#

LOCAL_PATH := $(call my-dir)

# Scan daemon
include $(CLEAR_VARS)

LOCAL_MODULE            := scand
LOCAL_SRC_FILES         := scand.c discover.c

LOCAL_C_INCLUDES := $(call include-path-for, system-core)/cutils
LOCAL_SHARED_LIBRARIES := libcutils

include $(BUILD_EXECUTABLE)
