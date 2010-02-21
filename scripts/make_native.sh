#!/bin/bash

BASE=$(dirname `cd ${0%/*} && echo $PWD/${0##*/}`)
PATH_TO_APP='/home/rorist/workspace/SmbExploit'
PATH_TO_NATIVE="$PATH_TO_APP/native"
PATH_TO_SRC='/opt/android-src'
SCAND_PATH='external/scand'
#LIBCOMMAND_PATH='external/libcommand'

cd $PATH_TO_SRC
mkdir -p development/tools/layoutopt/app/src/resources
rm -rf out/target/product/generic/system/bin
rm -rf out/target/product/generic/system/lib

# SCAND
rm -rf $SCAND_PATH; mkdir $SCAND_PATH
cp $PATH_TO_NATIVE/scand/* $SCAND_PATH/.

# LIBCOMMAND.SO
#rm -rf $LIBCOMMAND_PATH; mkdir $LIBCOMMAND_PATH
#cp $PATH_TO_NATIVE/libcommand/* $LIBCOMMAND_PATH/.
#if [ "`cat $PATH_TO_SRC/build/core/prelink-linux-arm.map | grep 0x9A000000 | wc -l`" -eq 0 ]; then
#  echo 'libcommand.so           0x9A000000' >> $PATH_TO_SRC/build/core/prelink-linux-arm.map
#fi

# Build
make scand && {
  echo "\nBuild Successfull!"
  mkdir -p $PATH_TO_APP/libs/armeabi
  cp out/target/product/generic/system/bin/scand $PATH_TO_APP/res/raw/scand
  #cp out/target/product/generic/system/lib/libcommand.so $PATH_TO_APP/libs/armeabi/libcommand.so
}
