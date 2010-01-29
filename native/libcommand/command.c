#include <stdlib.h>
#include <jni.h>

JNIEXPORT jint JNICALL Java_info_lamatricexiste_network_Utils_Command_runCommand (JNIEnv *env, jclass class, jstring command)
{
  const char *commandString;
  commandString = (*env)->GetStringUTFChars(env, command, 0);
  int exitcode = system(commandString); 
  (*env)->ReleaseStringUTFChars(env, command, commandString);  
  return (jint)exitcode;
}
