# Install and run this app

To install this app on an Android device and run.  

## Step 1: Install Android Studio  

Android Studio version is in [**README.md**](./README.md)#compile env.  

## Step 2: Git clone this project    

## Step 3: Download libs for compiling.
  
Download [**libcaffe2.a**](http://125.39.136.212:8484/aiotbench/libcaffe2.a), and move it to app/src/main/jniLibs/armeabi-v7a/, see [**README.md**](./README.md)#Caffe2.  
Now you can compile this project and run the app on an Android device.  
Or just download our apk [**AIoTBench.apk**](http://125.39.136.212:8484/aiotbench/AIoTBench.apk.1.1) and install.  
But the app need models and datasets to test.   

## Step 4: Download models and datasets for testing.
  
Download [**aiot.zip**](http://125.39.136.212:8484/aiotbench/aiot.zip).  
Extract it. The directories and files are like:  
<details>
  <summary><mark><font color=darkred>tree</font></mark></summary>
  <pre><code>  aiot/
               ├── datasets
               │   └── 20200109-1
               │       ├── datasets.json
               │       └── imagenet
               │           ├── ILSVRC2012_img_val_classes_with_name
               │           ├── ILSVRC2012_img_val_sample
               │           │   └── validation
               │           │       ├── n01440764
               │           │       │   ├── ILSVRC2012_val_00000293.JPEG
               │           │       │   └── ILSVRC2012_val_00002138.JPEG
               │           │       └── n01443537
               │           │           ├── ILSVRC2012_val_00000236.JPEG
               │           │           └── ILSVRC2012_val_00000262.JPEG
               │           └── ILSVRC2012_img_val_sample_3x2
               │               └── validation
               │                   ├── n01440764
               │                   │   ├── ILSVRC2012_val_00000293.JPEG
               │                   │   ├── ILSVRC2012_val_00002138.JPEG
               │                   ├── n01443537
               │                   │   ├── ILSVRC2012_val_00000236.JPEG
               │                   │   ├── ILSVRC2012_val_00000262.JPEG
               │                   └── n01484850
               │                       ├── ILSVRC2012_val_00002338.JPEG
               │                       └── ILSVRC2012_val_00049090.JPEG
               └── models
                   └── 20200120
                       ├── models.json
                       ├── caffe2
                       │   └── inception_v3
                       │       ├── init_net.pb
                       │       ├── predict_net.pb
                       │       ├── predict_net.pbtxt
                       │       └── synset2015.txt
                       ├── pytorch
                       │   └── inception_v3
                       │       └── inception_v3.pt
                       └── tflite
                           └── inception_v3
                               ├── inception_v3.pb
                               ├── inception_v3.tflite
                               └── labels.txt
  </code></pre>
</details>  
  
Now you can use these models to do the tests on these datasets. Have fun.  
