#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_gemmaapp_LlamaBridge_stringFromJNI(JNIEnv* env, jobject /* this */) {
    std::string hello = "C++ ENGINE IS ALIVE!";
    return env->NewStringUTF(hello.c_str());
}
