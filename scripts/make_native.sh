#!/bin/sh
PATH_TO_APP='/home/rorist/workspace/SmbExploit'
PATH_TO_SRC='/opt/android-src'

cd $PATH_TO_SRC
rm -rf frameworks/base/cmds/scand
cp -R $PATH_TO_APP/native frameworks/base/cmds/scand
make scand
cp out/target/product/generic/system/bin/scand $PATH_TO_APP/assets/scand
cd $PATH_TO_APP
adb push assets/scand /system/bin/scand
