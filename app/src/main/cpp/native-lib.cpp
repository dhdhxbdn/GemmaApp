#include <jni.h>
#include <string>
#include "llama.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_gemmaapp_LlamaBridge_stringFromJNI(JNIEnv* env, jobject /* this */) {
    // Инициализируем бэкенд нейросети
    llama_backend_init();
    
    // Запрашиваем информацию о железе (инструкции ARM NEON и тд)
    std::string sys_info = llama_print_system_info();
    
    // Освобождаем ресурсы
    llama_backend_free();
    
    std::string result = "Llama.cpp Engine Active!\n\nSystem Info:\n" + sys_info;
    return env->NewStringUTF(result.c_str());
}
