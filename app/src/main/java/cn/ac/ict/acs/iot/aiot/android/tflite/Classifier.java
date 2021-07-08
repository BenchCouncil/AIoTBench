/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.SystemClock;
import android.os.Trace;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.JJsonUtils;
import com.github.labowenzi.commonj.JUtil;
import com.github.labowenzi.commonj.log.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.model.ModelDesc;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/** A classifier specialized to label images using TensorFlow Lite. */
public abstract class Classifier {
  private static final String TAG = "tflite classifier";

  protected final LogUtil.Log log;

  /** The loaded TensorFlow Lite model. */
  private MappedByteBuffer tfliteModel;

  /** Image size along the x axis. */
  private final int imageSizeX;

  /** Image size along the y axis. */
  private final int imageSizeY;

  /** Optional GPU delegate for accleration. */
  private GpuDelegate gpuDelegate = null;

  /** An instance of the driver class to run model inference with Tensorflow Lite. */
  protected Interpreter tflite;

  /** Options for configuring the Interpreter. */
  private final Interpreter.Options tfliteOptions = new Interpreter.Options();

  /** Labels corresponding to the output of the vision model. */
  private List<String> labels;//no use:只是获取标签id对应的字符串而已，只在tostring里面用到了

  protected final boolean needToBgr;
  /** Input image TensorBuffer. */
  private TensorImage inputImageBuffer;

  /** Output probability TensorBuffer. */
  private final TensorBuffer outputProbabilityBuffer;

  /** Processer to apply post processing of the output probability. */
  private final TensorProcessor probabilityProcessor;

  public static Classifier create(String net_tflite_filepath, Model.Device device, int numThreads, String labelsFilePath, ModelDesc.Tflite modelDesc, LogUtil.Log log)
          throws IOException {
    boolean needToBgr = modelDesc != null && modelDesc.needToBgr();
    //if (modelDesc != null) {
      float[] mean = modelDesc.getNorm_mean();
      float[] std_dev = modelDesc.getNorm_std_dev();
      //if (!JUtil.isEmpty(mean) && !JUtil.isEmpty(std_dev) && mean.length == std_dev.length) {
        return new ClassifierWithNorm(net_tflite_filepath, device, numThreads, labelsFilePath, needToBgr, mean, std_dev, log);
      //}
    //}
  }



  /** Initializes a {@code Classifier}. */
  protected Classifier(String net_tflite_filepath, Model.Device device, int numThreads, String labelsFilePath, boolean needToBgr, LogUtil.Log log) throws IOException {
    this(null, net_tflite_filepath, labelsFilePath, device, numThreads, needToBgr, log);
  }
  protected Classifier(Activity activity, String net_tflite_filepath, String labelsFilePath, Model.Device device, int numThreads, boolean needToBgr, LogUtil.Log log) throws IOException {
    this.needToBgr = needToBgr;
    this.log = log;
    if (JUtil.isEmpty(net_tflite_filepath)) {
      tfliteModel = loadModelFile(activity);
    } else {
      tfliteModel = loadModelFile(net_tflite_filepath);
    }
    switch (device) {
      case NNAPI:
        tfliteOptions.setUseNNAPI(true);
        break;
      case GPU:
        gpuDelegate = new GpuDelegate();
        tfliteOptions.addDelegate(gpuDelegate);
        break;
      case CPU:
        break;
    }
    tfliteOptions.setNumThreads(numThreads);
    tflite = new Interpreter(tfliteModel, tfliteOptions);

    // Loads labels out from the label file.
    int[] oShape = tflite.getOutputTensor(0).shape();  // like: [1, 1001]
    int oLength = oShape[1];
    if (JUtil.isEmpty(labelsFilePath)) {
      labels = loadLabelList(activity);
    } else {
      labels = loadLabelList(labelsFilePath);
    }
    if (labels.size() != oLength) {
      int lSize = labels.size();
      if (lSize == oLength - 1) {
        // add background;
        labels.add(0, "dummy");
      } else if (lSize == oLength + 1) {
        // remove background;
        labels.remove(0);
      } else {
        String msg = String.format("wrong labels size(%d) with model output size(%d)", lSize, oLength);
        Log.e(TAG, msg);
        log.loglnA(TAG, msg);
      }
    }

    // Reads type and shape of input and output tensors, respectively.
    int imageTensorIndex = 0;
    int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}//note:from model
    imageSizeY = imageShape[1];
    imageSizeX = imageShape[2];
    DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();
    int probabilityTensorIndex = 0;
    int[] probabilityShape =
            tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
    DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

    // Creates the input tensor.
    inputImageBuffer = new TensorImage(imageDataType);

    // Creates the output tensor and its processor.
    outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

    // Creates the post processor for the output probability.
    probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

    log.loglnA(TAG, "Created a Tensorflow Lite Image Classifier.");
    String tfliteMsgFormat = "tflite, iShape=%s, oShape=%s";
    String tfliteMsg = String.format(tfliteMsgFormat, JJsonUtils.toJson(imageShape), JJsonUtils.toJson(probabilityShape));
    log.loglnA(TAG, tfliteMsg);
  }

  /** Reads label list from Assets. */
  private List<String> loadLabelList(Activity activity) throws IOException {
    InputStream in = activity.getAssets().open(getLabelPath());
    List<String> ret = FileUtil.loadLabels(in);
    JIoUtil.closeSilently(in);
    return ret;
  }
  private List<String> loadLabelList(String labelsFilePath) throws IOException {
    InputStream in = new FileInputStream(new File(labelsFilePath));
    List<String> ret = FileUtil.loadLabels(in);
    JIoUtil.closeSilently(in);
    return ret;
  }

  /** Memory-map the model file in Assets. */
  private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
    return FileUtil.loadMappedFile(activity, getModelPath());
  }
  private MappedByteBuffer loadModelFile(String net_tflite_filepath) throws IOException {
    File file = new File(net_tflite_filepath);
    FileInputStream inputStream = new FileInputStream(file);
    FileChannel fileChannel = inputStream.getChannel();
    long startOffset = 0;
    long declaredLength = file.length();
    MappedByteBuffer ret = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    fileChannel.close();
    inputStream.close();
    return ret;
  }

  /** Runs inference and returns the classification results. */
  public List<Recognition> recognizeImage(final Bitmap bitmap) {
    // Logs this method so that it can be analyzed with systrace.
    Trace.beginSection("recognizeImage");

    Trace.beginSection("loadImage");
    long startTimeForLoadImage = SystemClock.uptimeMillis();
    inputImageBuffer = loadImage(bitmap);
    long endTimeForLoadImage = SystemClock.uptimeMillis();
    Trace.endSection();
//    Log.v(TAG, "Timecost to load the image: " + (endTimeForLoadImage - startTimeForLoadImage));

    // Runs the inference call.
    Trace.beginSection("runInference");
    long startTimeForReference = SystemClock.uptimeMillis();
    tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());
    long endTimeForReference = SystemClock.uptimeMillis();
    Trace.endSection();
//    Log.v(TAG, "Timecost to run model inference: " + (endTimeForReference - startTimeForReference));

    // Gets the map of label and probability.
    Map<String, Float> labeledProbability =
        new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
            .getMapWithFloatValue();
    Trace.endSection();

    // Gets top-k results.
    return getAllProbability(labeledProbability);
  }

  /** Closes the interpreter and model to release resources. */
  public void close() {
    if (tflite != null) {
      tflite.close();
      tflite = null;
    }
    if (gpuDelegate != null) {
      gpuDelegate.close();
      gpuDelegate = null;
    }
    tfliteModel = null;
  }

  /** Get the image size along the x axis. */
  public int getImageSizeX() {
    return imageSizeX;
  }

  /** Get the image size along the y axis. */
  public int getImageSizeY() {
    return imageSizeY;
  }

  /** Loads input image, and applies preprocessing. */
  private TensorImage loadImage(final Bitmap bitmap) {

    if (needToBgr) {
    //if (true) {
      for (int i = 0; i < bitmap.getWidth(); ++i) {
        for (int j = 0; j < bitmap.getHeight(); ++j) {
          int px = bitmap.getPixel(i, j);
          int a = Color.alpha(px);
          int r = Color.red(px);
          int g = Color.green(px);
          int b = Color.blue(px);
          px = Color.argb(a, b, g, r);
          bitmap.setPixel(i, j, px);
        }
      }
    }
    // Loads bitmap into a TensorImage.
    inputImageBuffer.load(bitmap);

    // Creates processor for the TensorImage.
    ImageProcessor.Builder builder = new ImageProcessor.Builder();
   // if (needToBgr) builder.add(new RgbToBgrOp());
    int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
    builder.add(new ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(new ResizeOp(imageSizeX, imageSizeY, ResizeMethod.NEAREST_NEIGHBOR))
            .add(getPreprocessNormalizeOp());
    //int th = bitmap.getWidth() > bitmap.getHeight() ? 256 : 256*bitmap.getHeight()/bitmap.getWidth();
    //int tw = bitmap.getWidth() > bitmap.getHeight() ? 256*bitmap.getWidth()/bitmap.getHeight() : 256 ;
    //builder.add(new ResizeOp(th, tw, ResizeMethod.NEAREST_NEIGHBOR))
    //        .add(new ResizeWithCropOrPadOp(imageSizeX, imageSizeY))
    //        .add(getPreprocessNormalizeOp());
    ImageProcessor imageProcessor = builder.build();
    return imageProcessor.process(inputImageBuffer);
  }

  /** Gets the top-k results. */
  private List<Recognition> getAllProbability(Map<String, Float> labelProb) {
    // Find the best classifications.
    PriorityQueue<Recognition> pq =
        new PriorityQueue<>(
            labelProb.size(),
            new Comparator<Recognition>() {
              @Override
              public int compare(Recognition lhs, Recognition rhs) {
                // Intentionally reversed to put high confidence at the head of the queue.
                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
              }
            });

    for (int i = 0; i < labels.size(); ++i) {
      String label = labels.get(i);
      Float prob = labelProb.get(label);
      Recognition r = new Recognition(
              "" + i, i,
              label,
              prob,
              null);
      pq.add(r);
    }

    final ArrayList<Recognition> recognitions = new ArrayList<>();
//    int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
    int recognitionsSize = pq.size();
    for (int i = 0; i < recognitionsSize; ++i) {
      recognitions.add(pq.poll());
    }
    return recognitions;
  }

  /** Gets the name of the model file stored in Assets. */
  protected abstract String getModelPath();

  /** Gets the name of the label file stored in Assets. */
  protected abstract String getLabelPath();

  /** Gets the TensorOperator to nomalize the input image in preprocessing. */
  protected abstract TensorOperator getPreprocessNormalizeOp();

  /**
   * Gets the TensorOperator to dequantize the output probability in post processing.
   *
   * <p>For quantized model, we need de-quantize the prediction with NormalizeOp (as they are all
   * essentially linear transformation). For float model, de-quantize is not required. But to
   * uniform the API, de-quantize is added to float model too. Mean and std are set to 0.0f and
   * 1.0f, respectively.
   */
  protected abstract TensorOperator getPostprocessNormalizeOp();
}
