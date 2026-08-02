#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_gemmaapp_LlamaBridge_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Llama.cpp Engine Ready";
    return env->NewStringUTF(hello.c_str());
}
