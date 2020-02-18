/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package cn.ac.ict.acs.iot.aiot.android.tflite;

import android.app.Activity;

import java.io.IOException;

import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;

import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/** This TensorFlow Lite classifier works with the quantized MobileNet model. */
public class ClassifierQuantizedMobileNet extends Classifier {

  /**
   * The quantized model does not require normalization, thus set mean as 0.0f, and std as 1.0f to
   * bypass the normalization.
   */
  private static final float IMAGE_MEAN = 0.0f;

  private static final float IMAGE_STD = 1.0f;

  /** Quantized MobileNet requires additional dequantization to the output probability. */
  private static final float PROBABILITY_MEAN = 0.0f;

  private static final float PROBABILITY_STD = 255.0f;

  /**
   * Initializes a {@code ClassifierQuantizedMobileNet}.
   */
  public ClassifierQuantizedMobileNet(Activity activity, Device device, int numThreads, LogUtil.Log log)
      throws IOException {
    super(activity, device, numThreads, log);
  }
  public ClassifierQuantizedMobileNet(String net_tflite_filepath, Device device, int numThreads, String labelsFilePath, LogUtil.Log log)
      throws IOException {
    super(net_tflite_filepath, device, numThreads, labelsFilePath, log);
  }

  @Override
  protected String getModelPath() {
    // you can download this file from
    // see build.gradle for where to obtain this file. It should be auto
    // downloaded into assets.
    return "tflite/mobilenet_v1_1.0_224_quant.tflite";
  }

  @Override
  protected String getLabelPath() {
    return "tflite/labels.txt";
  }

  @Override
  protected TensorOperator getPreprocessNormalizeOp() {
    return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
  }

  @Override
  protected TensorOperator getPostprocessNormalizeOp() {
    return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
  }
}
