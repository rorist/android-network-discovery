// Taken from: http://code.google.com/p/android-wifi-tether/source/browse/trunk/native/libnativetask/android_tether_system_NativeTask.c

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

JNIEXPORT jint JNICALL Java_info_lamatricexiste_network_Utils_NativeTask_runCommand(JNIEnv *env, jclass class, jstring command) {
  const char *commandString;
  commandString = (*env)->GetStringUTFChars(env, command, 0);
  int exitcode = system(commandString); 
  (*env)->ReleaseStringUTFChars(env, command, commandString);  
  return (jint)exitcode;
}
