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

import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;

import java.io.IOException;

import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/** This TensorFlow Lite classifier works with the quantized MobileNet model. */
public class ClassifierWithNorm extends Classifier {

  /** Quantized MobileNet requires additional dequantization to the output probability. */
  private static final float PROBABILITY_MEAN = 0.0f;

  private static final float PROBABILITY_STD = 1.0f;

  /** Float MobileNet requires additional normalization of the used input. */
  private final float[] mean;
  private final float[] std_dev;

  /**
   * Initializes a {@code ClassifierQuantizedMobileNet}.
   */
  public ClassifierWithNorm(String net_tflite_filepath, Device device, int numThreads, String labelsFilePath, boolean needToBgr, float[] mean, float[] std_dev, LogUtil.Log log)
          throws IOException {
    super(net_tflite_filepath, device, numThreads, labelsFilePath, needToBgr, log);
    this.mean = mean;
    this.std_dev = std_dev;
  }

  @Override
  protected String getModelPath() {
    return null;
  }

  @Override
  protected String getLabelPath() {
    return null;
  }

  @Override
  protected TensorOperator getPreprocessNormalizeOp() {
    return new NormalizeOp(mean, std_dev);
  }

  @Override
  protected TensorOperator getPostprocessNormalizeOp() {
    return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
  }
}
