//
// Created by chenkaiyi on 12/19/24.
//

#include "testso3.h"
#include "testso2.h"
#include <jni.h>

int test3() {
    return test2() + 3;
}

JNIEXPORT jint JNICALL
Java_com_muye_testso_NativeLib_test3(JNIEnv *env, jobject thiz) {
    return test3();
}