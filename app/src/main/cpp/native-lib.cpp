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

#define IMG_C 3
#define aloge(...) __android_log_print(ANDROID_LOG_ERROR, "AIoTCaffe2", __VA_ARGS__);
#define alogd(...) __android_log_print(ANDROID_LOG_DEBUG, "AIoTCaffe2", __VA_ARGS__);

class PredictorWrapper {
public:
    PredictorWrapper(
            caffe2::Predictor *predictor,
            bool needToBgr,
            float norm_mean[], int norm_mean_cnt,
            float norm_std_dev[], int norm_std_dev_cnt,
            int imgW, int imgH) {
        this->predictor = predictor;
        this->needToBgr = needToBgr;
        this->norm_mean_cnt = norm_mean_cnt;
        if (norm_mean_cnt <= 0) {
            this->norm_mean = new float[1];
        } else {
            this->norm_mean = new float[norm_mean_cnt];
            for (int i = 0; i < norm_mean_cnt; ++i) {
                this->norm_mean[i] = norm_mean[i];
            }
        }
        this->norm_std_dev_cnt = norm_std_dev_cnt;
        if (norm_std_dev_cnt <= 0) {
            this->norm_std_dev = new float[1];
        } else {
            this->norm_std_dev = new float[norm_std_dev_cnt];
            for (int i = 0; i < norm_std_dev_cnt; ++i) {
                this->norm_std_dev[i] = norm_std_dev[i];
            }
        }
        this->imgW = imgW;
        this->imgH = imgH;
        this->maxDataSize = this->imgW * this->imgH * IMG_C;
    }

    virtual ~PredictorWrapper() {
        delete norm_std_dev;
        delete norm_mean;
        delete predictor;
    }

    caffe2::Predictor *predictor;

    bool needToBgr;

    float *norm_mean;
    int norm_mean_cnt;
    float *norm_std_dev;
    int norm_std_dev_cnt;

    int imgW;
    int imgH;
    int maxDataSize;
};

extern "C"
jlong
Java_cn_ac_ict_acs_iot_aiot_android_caffe2_PredictorWrapper_loadNetByFile(
        JNIEnv *env,
        jobject /* this */,
        jstring filePath) {

    const char *filePathCharP = env->GetStringUTFChars(filePath, nullptr);

    auto *net = new caffe2::NetDef();

    alogd("Attempting to load protobuf netdefs...");
    std::vector<char> buffer;
    std::ifstream file(filePathCharP, std::ios::binary | std::ios::ate);
    if (!file) {
        return false;
    }
    std::streamsize size = file.tellg();
    file.seekg(0, std::ios::beg);
    buffer.resize(size);
    file.read(buffer.data(), size);

    const void* data = buffer.data();
    int bufferSize = buffer.size();
    alogd("size, file=%d, buffer=%d", size, bufferSize);
    if (!net->ParseFromArray(data, bufferSize)) {  // crash when load a big net file(like vgg) by protobuf;
        aloge("Couldn't parse net from data.\n");
    }
    file.close();

    env->ReleaseStringUTFChars(filePath, filePathCharP);
    alogd("load net done.");
    jlong p = (jlong)(net);
    return p;
}
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
        jlong pInitNet, jlong pPredictNet,
        jboolean needToBgr,
        jfloatArray normMean, jint normMeanCnt,
        jfloatArray normStdDev, jint normStdDevCnt,
        jint inputImageWidth, jint inputImageHeight) {
    alogd("new predictor.");
    caffe2::NetDef *initNet = (caffe2::NetDef *)(pInitNet);
    caffe2::NetDef *predictNet = (caffe2::NetDef *)(pPredictNet);
    alogd("Instantiating predictor...");
    caffe2::Predictor *predictor = new caffe2::Predictor(*initNet, *predictNet);
    bool toBgr = (needToBgr != JNI_FALSE);

    int norm_mean_cnt = normMeanCnt;
    float *norm_mean;
    if (norm_mean_cnt <= 0) {
        norm_mean = NULL;
    } else {
        norm_mean = new float[norm_mean_cnt];
        jfloat *mean_data = env->GetFloatArrayElements(normMean, 0);
        for (int i=0; i<norm_mean_cnt; ++i) {
            norm_mean[i] = mean_data[i];
        }
    }

    int norm_std_dev_cnt = normStdDevCnt;
    float *norm_std_dev;
    if (norm_std_dev_cnt <= 0) {
        norm_std_dev = NULL;
    } else {
        norm_std_dev = new float[norm_std_dev_cnt];
        jfloat *std_dev_data = env->GetFloatArrayElements(normStdDev, 0);
        for (int i=0; i<norm_std_dev_cnt; ++i) {
            norm_std_dev[i] = std_dev_data[i];
        }
    }
    int imgW = inputImageWidth;
    int imgH = inputImageHeight;

    PredictorWrapper *wrapper = new PredictorWrapper(predictor, toBgr, norm_mean, norm_mean_cnt, norm_std_dev, norm_std_dev_cnt, imgW, imgH);
    alogd("new predictor done.");
    if (norm_mean != NULL) {
        delete norm_mean;
    }
    if (norm_std_dev != NULL) {
        delete norm_std_dev;
    }
    jlong p = (jlong)(wrapper);
    return p;
}

/**
 * 可以考虑在native-lib中使用静态数组保存c对象，java对象需要申请该数组中保存
 * 的某个c对象，类似于享元模式或者文件打开表。
 * 这样就不用传递指针，以避免指针类型的问题。
 */
extern "C"
void
Java_cn_ac_ict_acs_iot_aiot_android_caffe2_PredictorWrapper_classificationFromCaffe2(
        JNIEnv *env,
        jobject /* this */,
        jlong pWrapper,
        jbyteArray R, jbyteArray G, jbyteArray B,
        jfloatArray result, jintArray resultCnt) {
    PredictorWrapper *wrapper = (PredictorWrapper *) pWrapper;
    caffe2::Predictor *predictor = wrapper->predictor;
    if (!predictor) {
        int resultCntI = -1;
        env->SetIntArrayRegion(resultCnt, 0, 1, &resultCntI);
        return;
    }
    float *input_data = new float[wrapper->maxDataSize];
    jsize r_len = env->GetArrayLength(R);
    jbyte *r_data = env->GetByteArrayElements(R, 0);
    assert(r_len <= wrapper->maxDataSize);
    jsize g_len = env->GetArrayLength(G);
    jbyte *g_data = env->GetByteArrayElements(G, 0);
    assert(g_len <= wrapper->maxDataSize);
    jsize b_len = env->GetArrayLength(B);
    jbyte *b_data = env->GetByteArrayElements(B, 0);
    assert(b_len <= wrapper->maxDataSize);
    int oneChannel = wrapper->imgH * wrapper->imgW;
    int oneRow = wrapper->imgW;
    for (auto i = 0; i < wrapper->imgH; ++i) {
        jbyte *r_row = &r_data[i * wrapper->imgW];
        jbyte *g_row = &g_data[i * wrapper->imgW];
        jbyte *b_row = &b_data[i * wrapper->imgW];
        for (auto j = 0; j < wrapper->imgW; ++j) {
            // Tested on Pixel and S7.
            char r = r_row[j];
            char g = g_row[j];
            char b = b_row[j];

            auto b_i = 0;
            auto g_i = 0;
            auto r_i = 0;
            bool toBgr = wrapper->needToBgr;
            if (toBgr) {
            //if (1) {
                b_i = 0 * oneChannel + j * oneRow + i;
                g_i = 1 * oneChannel + j * oneRow + i;
                r_i = 2 * oneChannel + j * oneRow + i;
            } else {
                r_i = 0 * oneChannel + j * oneRow + i;
                g_i = 1 * oneChannel + j * oneRow + i;
                b_i = 2 * oneChannel + j * oneRow + i;
            }

            if (wrapper->norm_mean_cnt > 0
                    && wrapper->norm_std_dev_cnt > 0
                    && wrapper->norm_mean_cnt == wrapper->norm_std_dev_cnt) {
                if (wrapper->norm_mean_cnt < 3) {
                    // 1;
                    float mean = wrapper->norm_mean[0];
                    float std_dev = wrapper->norm_std_dev[0];
                    input_data[r_i] = (r - mean) / std_dev;
                    input_data[g_i] = (g - mean) / std_dev;
                    input_data[b_i] = (b - mean) / std_dev;
                } else {
                    // 3;
                    float *mean = wrapper->norm_mean;
                    float *std_dev = wrapper->norm_std_dev;
                    input_data[r_i] = (r - mean[0]) / std_dev[0];
                    input_data[g_i] = (g - mean[1]) / std_dev[1];
                    input_data[b_i] = (b - mean[2]) / std_dev[2];
                }
            } else {
                input_data[r_i] = (float) (r / 255.0);
                input_data[g_i] = (float) (g / 255.0);
                input_data[b_i] = (float) (b / 255.0);
            }

        }
    }
    caffe2::TensorCPU input = caffe2::Tensor(std::vector<int>({1, IMG_C, wrapper->imgH, wrapper->imgW}), caffe2::CPU);
    memcpy(input.mutable_data<float>(), input_data, oneChannel * IMG_C * sizeof(float));
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
    delete input_data;
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
