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
etc.  

# Data set

demo image  
ImageNet  



## compile env

android studio: 3.6.1(most recent);  
ndk: r21(most recent);  

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


## aiot dir

    aiot
      models
        20200109-1
          models.json desc
        20200109-2
          models.json desc
        20200110-1
          models.json desc
      datasets
        20200109-1
          datasets.json desc

### models.json

    {
      "caffe2": [
        {
          "name": "inception_v3",
          "dir": "inception_v3",
          "init_net_pb": "init_net.pb",
          "predict_net_pb": "predict_net.pb",
          "norm_mean": [0,0,0],
          "norm_std_dev": [255.0,255.0,255.0],
          "labels": "synset2015.txt",
          "bitmap_rgb_type": "bgr",
          "bitmap_convert_size": [299,299]
        }
      ],
      "pytorch": [
        {
          "name": "inception_v3",
          "dir": "inception_v3",
          "net_pt": "inception_v3.pt",
          "bitmap_convert_size": [299,299]
        }
      ],
      "tflite": [
        {
          "name": "inception_v3",
          "dir": "inception_v3",
          "net_tflite": "inception_v3.tflite",
          "quantization": "float",
          "norm_mean": [127.5],
          "norm_std_dev": [127.5],
          "labels": "labels.txt",
          "bitmap_convert_method": "copy",
          "bitmap_rgb_type": "default",
          "bitmap_convert_size": [299,299]
        }
      ]
    }

### datasets.json

    {
      "imagenet": {
        "classes_info": "ILSVRC2012_img_val_classes_with_name",
        "datasets": [
          {
            "name": "imagenet_2x2",
            "dir": "ILSVRC2012_img_val_sample/validation"
          }
        ]
      }
    }

## data set

### ImageNet

#### image files

    ILSVRC2012_img_val/validation
      eg:
        n01440764/
          eg
            ILSVRC2012_val_00000293.JPEG
            ILSVRC2012_val_00002138.JPEG
            ILSVRC2012_val_00003014.JPEG
          50 ILSVRC2012_val_xxxxxxxx.JPEG files;
        n01443537/
        n01484850/
      1000 nxxxxxxxx/ dirs;

#### class info

    ILSVRC2012_img_val_classes_with_name
      eg:
        n01440764	tench, Tinca tinca
        n01443537	goldfish, Carassius auratus
        n01484850	great white shark, white shark, man-eater, man-eating
      1000 nxxxxxxxx classes;
      index of each class in this array is the classification output data of model;

