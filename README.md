# AIoT Bench on Android

AIoT benchmark program on Android;
For different frameworks, models and data sets;

## Framework

PyTorch
Caffe2
Tensorflow lite

## Model

MobileNet
ResNet

# Data set

demo image
ImageNet



## compile env

android studio: 3.5.3
ndk: r20b(most recent);

## PyTorch

code from: [**PyTorch android demo**](https://github.com/pytorch/android-demo-app.git)

## Caffe2

code from: [**pytorch android**](https://github.com/cedrickchee/pytorch-android.git)
other code: [**AICamera**](https://github.com/bwasti/AICamera.git), only for old ndk(like r12b).

as [**pytorch android**](https://github.com/cedrickchee/pytorch-android.git) /README.md mentioned, this program need libcaffe2.a;
generate by: [**PyTorch**](https://github.com/pytorch/pytorch.git)
in dir: app/src/main/jniLibs/armeabi-v7a/, see app/.gitignore;

## Tensorflow lite

code from: [**Tensorflow lite android**](https://github.com/tensorflow/examples/blob/master/lite/examples/image_classification/android/README.md)

