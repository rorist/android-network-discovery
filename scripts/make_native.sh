#!/bin/sh
PATH_TO_APP='/home/rorist/workspace/SmbExploit'
PATH_TO_NDK='/opt/android-ndk'
APP_NAME='net'

cd $PATH_TO_NDK
if [ -d "apps/$APP_NAME"! ]; then
  mkdir apps/$APP_NAME
  ln -s $PATH_TO_APP/jni/Application.mk apps/$APP_NAME/.
fi
make APP=$APP_NAME -B
cd -
