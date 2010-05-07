#!/bin/bash
#
# Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
# Licensed under GNU's GPL 2, see README
#

BASE=$(dirname `cd ${0%/*} && echo $PWD}`)
PATH_TO_NATIVE="$BASE/native"
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
  mkdir -p $BASE/libs/armeabi
  cp out/target/product/generic/system/bin/scand $BASE/res/raw/scand
  #cp out/target/product/generic/system/lib/libcommand.so $BASE/libs/armeabi/libcommand.so
}
