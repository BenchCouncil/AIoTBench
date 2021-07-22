package cn.ac.ict.acs.iot.aiot.android.tflite_detector;

import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Trace;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
//import org.tensorflow.lite.support.metadata.MetadataExtractor;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;
import org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions;

import cn.ac.ict.acs.iot.aiot.android.DownloadInfo;
import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.model.ModelDesc;
import cn.ac.ict.acs.iot.aiot.android.tflite.Recognition;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API: -
 * https://github.com/tensorflow/models/tree/master/research/object_detection where you can find the
 * training code.
 *
 * <p>To use pretrained models in the API or convert to TF Lite models, please see docs for details:
 * -
 * https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/tf1_detection_zoo.md
 * -
 * https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/tf2_detection_zoo.md
 * -
 * https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/running_on_mobile_tensorflowlite.md#running-our-model-on-android
 */
public class Detector {
    private static final String TAG = "ObjectDetection";

    // Only return this many results.
    private static int NUM_DETECTIONS = 10;
    // Float model
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;
    // Number of threads in the java app
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;
    // Config values.
    protected boolean needToBgr;
    private int inputSize;
    // Pre-allocated buffers.
    private final List<String> labels = new ArrayList<>();
    private int[] intValues;
    /**
     * Image size along the x axis.
     */
    private final int imageSizeX;

    /**
     * Image size along the y axis.
     */
    private final int imageSizeY;
    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private float[][][] outputLocations;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] outputClasses;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] outputScores;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] numDetections;

    private ByteBuffer imgData;
    private GpuDelegate gpuDelegate = null;
    private MappedByteBuffer tfLiteModel;
    private static Interpreter.Options tfliteOptions = new Interpreter.Options();
    protected Interpreter tfLite;
    private ObjectDetector objectDetector;
    protected ObjectDetectorOptions.Builder optionsBuilder;
    /**
     * Memory-map the model file in Assets.
     */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private static MappedByteBuffer loadModelFile(String net_tflite_filepath) throws IOException {
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

    protected Detector(String net_tflite_filepath, Model.Device device, int numThreads, String labelsFilePath, boolean needToBgr, boolean isQuantized, LogUtil.Log log) throws IOException {
        DownloadInfo.ResourceOne resource = DownloadInfo.Resource.getInstance().getOne();
        String resourceName = resource.name;
        this.needToBgr = needToBgr;
        this.tfLiteModel = loadModelFile(net_tflite_filepath);
        optionsBuilder = ObjectDetectorOptions.builder().setMaxResults(NUM_DETECTIONS);//note:2nd

        objectDetector = ObjectDetector.createFromBufferAndOptions(this.tfLiteModel, optionsBuilder.build());//note:2nd

        //todo:read lable.json文件
//        MetadataExtractor metadata = new MetadataExtractor(modelFile);//lable
//        try (BufferedReader br =
//                     new BufferedReader(
//                             new InputStreamReader(
//                                     metadata.getAssociatedFile(labelFilename), Charset.defaultCharset()))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                Log.w(TAG, line);
//                d.labels.add(line);
//            }
//        }
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

//        tfliteOptions.setUseXNNPACK(true);//what????10x faster
        tfLite = new Interpreter(tfLiteModel, tfliteOptions);

        int[] imageShape = tfLite.getInputTensor(0).shape(); // {1, height, width, 3}//note:from model
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];


        isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isModelQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        imgData = ByteBuffer.allocateDirect(imageSizeY * imageSizeX * 3 * numBytesPerChannel);
        imgData.order(ByteOrder.nativeOrder());
        intValues = new int[imageSizeY * imageSizeX];//300*300

//        outputLocations = new float[1][NUM_DETECTIONS][4];
//        outputClasses = new float[1][NUM_DETECTIONS];
//        outputScores = new float[1][NUM_DETECTIONS];
//        numDetections = new float[1];
    }

    public static Detector create(
            final String net_tflite_filepath,
            Model.Device device,
            int numThreads,
            final String labelFilename,
            ModelDesc.Tflite modelDesc,
            LogUtil.Log log)
            throws IOException {
        boolean needToBgr = modelDesc != null && modelDesc.needToBgr();
        NUM_DETECTIONS=modelDesc.getNum_detection();
        return new Detector(net_tflite_filepath, device, numThreads,labelFilename, needToBgr, true, log);
    }

    private Bitmap loadImage(final Bitmap bitmap) {
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
        DataType imageDataType = tfLite.getInputTensor(0).dataType();
        TensorImage inputImageBuffer= new TensorImage(imageDataType);
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        ImageProcessor.Builder builder = new ImageProcessor.Builder();
        // if (needToBgr) builder.add(new RgbToBgrOp());
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        builder.add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(getPreprocessNormalizeOp());
        //int th = bitmap.getWidth() > bitmap.getHeight() ? 256 : 256*bitmap.getHeight()/bitmap.getWidth();
        //int tw = bitmap.getWidth() > bitmap.getHeight() ? 256*bitmap.getWidth()/bitmap.getHeight() : 256 ;
        //builder.add(new ResizeOp(th, tw, ResizeMethod.NEAREST_NEIGHBOR))
        //        .add(new ResizeWithCropOrPadOp(imageSizeX, imageSizeY))
        //        .add(getPreprocessNormalizeOp());
        ImageProcessor imageProcessor = builder.build();
        return imageProcessor.process(inputImageBuffer).getBitmap();
    }
    protected TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }
    private static Bitmap loadImage(String fileName) throws Exception {
        return BitmapFactory.decodeFile(fileName);
    }
    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }
    public List<Recognition> recognizeImage(final Bitmap bitmap) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage");
        System.out.println(TensorImage.fromBitmap(bitmap));
        List<Detection> results = objectDetector.detect(TensorImage.fromBitmap(bitmap));

        // Converts a list of {@link Detection} objects into a list of {@link Recognition} objects
        // to match the interface of other inference method, such as using the <a
        // href="https://github.com/tensorflow/examples/tree/master/lite/examples/object_detection/android/lib_interpreter">TFLite
        // Java API.</a>.
        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int cnt = 0;
        for (Detection detection : results) {
//            System.out.println(detection.getCategories().get(0).getLabel());
//            System.out.println(detection.getCategories().get(0).getScore());
//            System.out.println(detection.getBoundingBox().bottom+" "
//                    +detection.getBoundingBox().top+" "
//                    +detection.getBoundingBox().left+" "
//                    +detection.getBoundingBox().right);
            recognitions.add(
                    new Recognition(
                            "" + cnt,
                            cnt++,
                            detection.getCategories().get(0).getLabel(),
                            detection.getCategories().get(0).getScore(),
                            detection.getBoundingBox()));
        }
        Trace.endSection(); // "recognizeImage"
        return recognitions;
    }
    public List<Recognition> recognizeImage_old(Bitmap bitmap) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
//        bitmap=loadImage(bitmap);//640*426__>300*300

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        inputSize=imageSizeX;
        imgData.rewind();

        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }

        Trace.endSection(); // preprocessBitmap

        // Copy the input data into TensorFlow.
        Trace.beginSection("feed");
//        System.out.println(NUM_DETECTIONS);
        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];
        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);
        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");
        long s=System.currentTimeMillis();
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        long e=System.currentTimeMillis();
        System.out.println(e-s);
        Trace.endSection();

        // Show the best detections.
        // after scaling them back to the input size.
        // You need to use the number of detections from the output and not the NUM_DETECTONS variable
        // declared on top
        // because on some models, they don't always output the same total number of detections
        // For example, your model's NUM_DETECTIONS = 20, but sometimes it only outputs 16 predictions
        // If you don't use the output's numDetections, you'll get nonsensical data
//        System.out.println(numDetections[0]);
        int numDetectionsOutput =
                min(NUM_DETECTIONS, (int) numDetections[0]); // cast from float to integer, use min for safety
//        System.out.println(numDetectionsOutput);
        final ArrayList<Recognition> recognitions = new ArrayList<>(numDetectionsOutput);
        for (int i = 0; i < numDetectionsOutput; ++i) {
//            System.out.println(outputLocations[0][i][0]*inputSize+" "
//                    +outputLocations[0][i][0]*inputSize+" "
//                    +outputLocations[0][i][3]*inputSize+" "
//                    +outputLocations[0][i][2]*inputSize);
//            System.out.println(outputClasses[0][i]);
//            System.out.println(outputScores[0][i]);

            final RectF detection =
                    new RectF(
                            outputLocations[0][i][1] * inputSize,
                            outputLocations[0][i][0] * inputSize,
                            outputLocations[0][i][3] * inputSize,
                            outputLocations[0][i][2] * inputSize);
            recognitions.add(
                    new Recognition("" + i, i, null, outputScores[0][i], detection));

        }
        Trace.endSection(); // "recognizeImage"

        return recognitions;
    }

    public void enableStatLogging(final boolean logStats) {
    }

    public String getStatString() {
        return "";
    }

    public void close() {
        if (tfLite != null) {
            tfLite.close();
            tfLite = null;
        }
    }

//    public void setNumThreads(int numThreads) {
//        if (tfLite != null) {
//            tfLiteOptions.setNumThreads(numThreads);
//            recreateInterpreter();
//        }
//    }

//    public void setUseNNAPI(boolean isChecked) {
//        if (tfLite != null) {
//            tfLiteOptions.setUseNNAPI(isChecked);
//            recreateInterpreter();
//        }
//    }

//    private void recreateInterpreter() {
//        tfLite.close();
//        tfLite = new Interpreter(tfLiteModel, tfliteOptions);
//    }
}
