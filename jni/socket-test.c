#include <string.h>
#include <jni.h>

jstring
Java_info_lamatricexiste_network_DiscoverActivity_stringFromJNI( JNIEnv* env,
                                                  jobject thiz )
{
    return (*env)->NewStringUTF(env, "Hello from JNI !");
}
