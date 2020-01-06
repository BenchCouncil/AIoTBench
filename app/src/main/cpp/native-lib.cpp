#include <jni.h>
#include <string>
#include <algorithm>

#define PROTOBUF_USE_DLLS 1
#define CAFFE2_USE_LITE_PROTO 1

#include <caffe2/predictor/predictor.h>
#include <caffe2/core/operator.h>
#include <caffe2/core/timer.h>

#include "caffe2/core/init.h"
#include <caffe2/core/tensor.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <ATen/ATen.h>
#include "classes.h"

#define IMG_H 227
#define IMG_W 227
#define IMG_C 3
#define MAX_DATA_SIZE IMG_H * IMG_W * IMG_C
#define aloge(...) __android_log_print(ANDROID_LOG_ERROR, "AIoTCaffe2", __VA_ARGS__);
#define alogd(...) __android_log_print(ANDROID_LOG_DEBUG, "AIoTCaffe2", __VA_ARGS__);

extern "C"
jlong
Java_cn_ac_ict_acs_iot_aiot_android_caffe2_PredictorWrapper_loadNet(
                JNIEnv *env,
                jobject /* this */,
                jobject assetManager, jstring fileName) {

    const char *fileNameCharP = env->GetStringUTFChars(fileName, nullptr);

    auto *net = new caffe2::NetDef();
    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
    alogd("Attempting to load protobuf netdefs...");
    AAsset *asset = AAssetManager_open(mgr, fileNameCharP, AASSET_MODE_BUFFER);
    assert(asset != nullptr);
    const void *data = AAsset_getBuffer(asset);
    assert(data != nullptr);
    off_t len = AAsset_getLength(asset);
    assert(len != 0);
    if (!net->ParseFromArray(data, len)) {
        aloge("Couldn't parse net from data.\n");
    }
    AAsset_close(asset);

    env->ReleaseStringUTFChars(fileName, fileNameCharP);
    alogd("load net done.");
    jlong p = (jlong)(net);
    return p;
}

extern "C"
jlong
Java_cn_ac_ict_acs_iot_aiot_android_caffe2_PredictorWrapper_initCaffe2(
        JNIEnv *env,
        jobject /* this */,
        jlong pInitNet, jlong pPredictNet) {
    alogd("new predictor.");
    caffe2::NetDef *initNet = (caffe2::NetDef *)(pInitNet);
    caffe2::NetDef *predictNet = (caffe2::NetDef *)(pPredictNet);
    alogd("Instantiating predictor...");
    caffe2::Predictor *predictor = new caffe2::Predictor(*initNet, *predictNet);
    alogd("new predictor done.");
    jlong p = (jlong)(predictor);
    return p;
}

extern "C"
void
Java_cn_ac_ict_acs_iot_aiot_android_caffe2_PredictorWrapper_classificationFromCaffe2(
        JNIEnv *env,
        jobject /* this */,
        jlong pPredictor,
        jbyteArray R, jbyteArray G, jbyteArray B,
        jfloatArray result, jintArray resultCnt) {
    caffe2::Predictor *predictor = (caffe2::Predictor *) pPredictor;
    if (!predictor) {
        int resultCntI = -1;
        env->SetIntArrayRegion(resultCnt, 0, 1, &resultCntI);
        return;
    }
    float input_data[MAX_DATA_SIZE];
    jsize r_len = env->GetArrayLength(R);
    jbyte *r_data = env->GetByteArrayElements(R, 0);
    assert(r_len <= MAX_DATA_SIZE);
    jsize g_len = env->GetArrayLength(G);
    jbyte *g_data = env->GetByteArrayElements(G, 0);
    assert(g_len <= MAX_DATA_SIZE);
    jsize b_len = env->GetArrayLength(B);
    jbyte *b_data = env->GetByteArrayElements(B, 0);
    assert(b_len <= MAX_DATA_SIZE);
    for (auto i = 0; i < IMG_H; ++i) {
        jbyte *r_row = &r_data[i * IMG_W];
        jbyte *g_row = &g_data[i * IMG_W];
        jbyte *b_row = &b_data[i * IMG_W];
        for (auto j = 0; j < IMG_W; ++j) {
            // Tested on Pixel and S7.
            char r = r_row[j];
            char g = g_row[j];
            char b = b_row[j];

            auto b_i = 0 * IMG_H * IMG_W + j * IMG_W + i;
            auto g_i = 1 * IMG_H * IMG_W + j * IMG_W + i;
            auto r_i = 2 * IMG_H * IMG_W + j * IMG_W + i;

            input_data[r_i] = (float) (r / 255.0);
            input_data[g_i] = (float) (g / 255.0);
            input_data[b_i] = (float) (b / 255.0);

        }
    }
    caffe2::TensorCPU input = caffe2::Tensor(std::vector<int>({1, IMG_C, IMG_H, IMG_W}), caffe2::CPU);
    memcpy(input.mutable_data<float>(), input_data, IMG_H * IMG_W * IMG_C * sizeof(float));
    std::vector<caffe2::TensorCPU> input_vec({input});
    std::vector<caffe2::TensorCPU> output_vec(1);
    (*predictor)(input_vec, &output_vec);
    const auto& output = output_vec[0];
    auto data = output.data<float>();
    for (auto i = 0; i < output.size(); ++i) {
        env->SetFloatArrayRegion(result , i, 1, data+i);
    }
    int resultCntI = (int) (output.size());
    env->SetIntArrayRegion(resultCnt, 0, 1, &resultCntI);
}

extern "C"
void
Java_cn_ac_ict_acs_iot_aiot_android_caffe2_PredictorWrapper_deletePtr(
        JNIEnv *env,
        jobject /* this */,
        jlong ptr) {
    void *p = (void *)(ptr);
    delete p;
}
