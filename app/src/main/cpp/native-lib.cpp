#include <jni.h>
#include <string>
#include "llama.h"

static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_gemmaapp_LlamaBridge_loadModel(
    JNIEnv* env,
    jclass clazz,
    jstring model_path,
    jint gpu_layers
) {
    const char* path = env->GetStringUTFChars(model_path, nullptr);

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = gpu_layers;

    g_model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (!g_model) {
        return JNI_FALSE;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;

    g_ctx = llama_init_from_model(g_model, ctx_params);
    if (!g_ctx) {
        llama_model_free(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_gemmaapp_LlamaBridge_unloadModel(
    JNIEnv* env,
    jclass clazz
) {
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    llama_backend_free();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_gemmaapp_LlamaBridge_generate(
    JNIEnv* env,
    jclass clazz,
    jstring prompt,
    jobject on_token
) {
    if (!g_ctx || !g_model) return;

    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);

    jclass lambda_class = env->GetObjectClass(on_token);
    jmethodID invoke_id = env->GetMethodID(lambda_class, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;");

    jstring token_str = env->NewStringUTF(" [Модель загружена успешно!]");
    if (invoke_id != nullptr) {
        env->CallObjectMethod(on_token, invoke_id, token_str);
    }

    env->ReleaseStringUTFChars(prompt, prompt_str);
}
