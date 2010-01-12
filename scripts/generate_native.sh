#!/bin/sh
PATH_TO_APP='/home/rorist/workspace/SmbExploit'
PATH_TO_NDK='/opt/android-ndk'
APPS_DIR='apps/net'
cd $PATH_TO_NDK

if [ -d "$APPS_DIR" ]; then
  echo 'exist'
else
  mkdir $APPS_DIR
  ln -s $PATH_TO_APP/jni/Application.mk $APPS_DIR/.
fi
