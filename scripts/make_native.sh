#!/bin/bash

PATH_TO_APP='/home/rorist/workspace/SmbExploit'
PATH_TO_SRC='/opt/android-src'
SCAND_PATH='external/scand'
LIBCOMMAND_PATH='external/libcommand'

cd $PATH_TO_SRC
mkdir -p development/tools/layoutopt/app/src/resources
rm -rf out/target/product/generic/system/bin
rm -rf out/target/product/generic/system/lib

# SCAND
rm -rf $SCAND_PATH; mkdir $SCAND_PATH
cp $PATH_TO_APP/native/scand.c $SCAND_PATH/.
cp $PATH_TO_APP/native/libscan.* $SCAND_PATH/.
cp $PATH_TO_APP/native/Android-scand.mk $SCAND_PATH/Android.mk

# LIBCOMMAND.SO
rm -rf $LIBCOMMAND_PATH
mkdir $LIBCOMMAND_PATH
cp $PATH_TO_APP/native/command.c $LIBCOMMAND_PATH/.
cp $PATH_TO_APP/native/Android-command.mk $LIBCOMMAND_PATH/Android.mk
#echo 'libcommand.so           0x9A000000' > $PATH_TO_SRC/build/core/prelink-linux-arm.map

# Build
make scand libcommand && {
  cp out/target/product/generic/system/bin/scand $PATH_TO_APP/res/raw/scand
  cp out/target/product/generic/system/lib/libcommand.so $PATH_TO_APP/libs/armeabi/libcommand.so
}
