package cn.ac.ict.acs.iot.aiot.android;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Trace;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

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
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.model.ModelDesc;
import cn.ac.ict.acs.iot.aiot.android.tflite.Recognition;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

import static java.lang.Math.min;

public class SuperResolutioner {
    private static final String TAG = "SuperResolution";
    private static final int LR_IMAGE_HEIGHT = 50;
    private static final int LR_IMAGE_WIDTH = 50;
    private static final int UPSCALE_FACTOR = 4;
    private static final int SR_IMAGE_HEIGHT = LR_IMAGE_HEIGHT * UPSCALE_FACTOR;
    private static final int SR_IMAGE_WIDTH = LR_IMAGE_WIDTH * UPSCALE_FACTOR;
    private MappedByteBuffer model;
    private long superResolutionNativeHandle = 0;
    private Bitmap selectedLRBitmap = null;
    private boolean useGPU = false;
    private Switch gpuSwitch;
    private int[] lowResRGB = null;

    private TensorImage inputImageBuffer;
    private TensorBuffer outputImageBuffer;

    private GpuDelegate gpuDelegate = null;
    private MappedByteBuffer tfLiteModel;
    private static Interpreter.Options tfliteOptions = new Interpreter.Options();
    protected Interpreter tfLite;
    private ObjectDetector objectDetector;
    protected ObjectDetector.ObjectDetectorOptions.Builder optionsBuilder;

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

    protected SuperResolutioner(String net_tflite_filepath, Model.Device device, int numThreads, String labelsFilePath, boolean needToBgr, boolean isQuantized, LogUtil.Log log) throws IOException {
        DownloadInfo.ResourceOne resource = DownloadInfo.Resource.getInstance().getOne();
        String resourceName = resource.name;
        this.tfLiteModel = loadModelFile(net_tflite_filepath);

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
        lowResRGB = new int[LR_IMAGE_HEIGHT * LR_IMAGE_WIDTH];
        DataType imageDataType = tfLite.getInputTensor(0).dataType();
        int probabilityTensorIndex = 0;
        int[] outputShape = tfLite.getOutputTensor(0).shape();
        DataType outputDataType = tfLite.getOutputTensor(probabilityTensorIndex).dataType();

        // Creates the input tensor.
        inputImageBuffer = new TensorImage(imageDataType);

        // Creates the output tensor and its processor.
        outputImageBuffer = TensorBuffer.createFixedSize(outputShape,outputDataType);

    }

    public static SuperResolutioner create(
            final String net_tflite_filepath,
            Model.Device device,
            int numThreads,
            final String labelFilename,
            ModelDesc.Tflite modelDesc,
            LogUtil.Log log)
            throws IOException {
        boolean needToBgr = modelDesc != null && modelDesc.needToBgr();
        return new SuperResolutioner(net_tflite_filepath, device, numThreads, labelFilename, needToBgr, true, log);
    }

    private static Bitmap loadImage(String fileName) throws Exception {
        return BitmapFactory.decodeFile(fileName);
    }

    public void processImage(Bitmap bitmap) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("processImage");

        Trace.endSection(); // "recognizeImage"

        int h = bitmap.getHeight() / LR_IMAGE_HEIGHT;
        int w = bitmap.getWidth() / LR_IMAGE_WIDTH;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                Bitmap newbitmap= Bitmap.createBitmap(bitmap,
                        j*LR_IMAGE_WIDTH,
                        i*LR_IMAGE_HEIGHT,
                        LR_IMAGE_WIDTH,
                        LR_IMAGE_HEIGHT);
                doSuperResolution(newbitmap);
            }
        }

    }

    public synchronized void doSuperResolution(Bitmap bitmap) {
        inputImageBuffer.load(bitmap);
        tfLite.run(inputImageBuffer.getBuffer(), outputImageBuffer.getBuffer().rewind());

    }

    public void close() {
        if (tfLite != null) {
            tfLite.close();
            tfLite = null;
        }
    }

}
