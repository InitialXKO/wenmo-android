#include <jni.h>
#include <string>
#include <vector>
#include "sime.h"

static sime::Sime* g_sime = nullptr;
static std::string g_composition;
static bool g_traditional = false;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_ink_wenmo_ime_engine_SimeEngine_nativeInit(JNIEnv* env, jclass clazz, jstring dict_path, jstring cnt_path) {
    const char* dict_c = env->GetStringUTFChars(dict_path, nullptr);
    const char* cnt_c = env->GetStringUTFChars(cnt_path, nullptr);

    if (g_sime) {
        delete g_sime;
    }
    g_sime = new sime::Sime(dict_c, cnt_c);

    env->ReleaseStringUTFChars(dict_path, dict_c);
    env->ReleaseStringUTFChars(cnt_path, cnt_c);
    return (g_sime && g_sime->Ready()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_ink_wenmo_ime_engine_SimeEngine_nativeType(JNIEnv* env, jclass clazz, jchar ch) {
    if (ch >= 'a' && ch <= 'z') {
        g_composition.push_back(static_cast<char>(ch));
    }
}

JNIEXPORT void JNICALL
Java_ink_wenmo_ime_engine_SimeEngine_nativeBackspace(JNIEnv* env, jclass clazz) {
    if (!g_composition.empty()) {
        g_composition.pop_back();
    }
}

JNIEXPORT void JNICALL
Java_ink_wenmo_ime_engine_SimeEngine_nativeClear(JNIEnv* env, jclass clazz) {
    g_composition.clear();
}

JNIEXPORT jstring JNICALL
Java_ink_wenmo_ime_engine_SimeEngine_nativeComposition(JNIEnv* env, jclass clazz) {
    return env->NewStringUTF(g_composition.c_str());
}

JNIEXPORT jobjectArray JNICALL
Java_ink_wenmo_ime_engine_SimeEngine_nativeCandidates(JNIEnv* env, jclass clazz) {
    jclass stringClass = env->FindClass("java/lang/String");
    if (g_composition.empty() || !g_sime || !g_sime->Ready()) {
        return env->NewObjectArray(0, stringClass, nullptr);
    }

    auto results = g_sime->DecodeStr(g_composition, 20);
    jobjectArray array = env->NewObjectArray(static_cast<jsize>(results.size()), stringClass, nullptr);
    for (size_t i = 0; i < results.size(); ++i) {
        jstring str = env->NewStringUTF(results[i].text.c_str());
        env->SetObjectArrayElement(array, static_cast<jsize>(i), str);
        env->DeleteLocalRef(str);
    }
    return array;
}

JNIEXPORT void JNICALL
Java_ink_wenmo_ime_engine_SimeEngine_nativeSetTraditional(JNIEnv* env, jclass clazz, jboolean trad) {
    g_traditional = (trad == JNI_TRUE);
}

JNIEXPORT jboolean JNICALL
Java_ink_wenmo_ime_engine_SimeEngine_nativeIsTraditional(JNIEnv* env, jclass clazz) {
    return g_traditional ? JNI_TRUE : JNI_FALSE;
}

}
