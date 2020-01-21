package cn.ac.ict.acs.iot.aiot.android.caffe2;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by alanubu on 20-1-2.
 */
public class PredictorWrapper {
    static {
        System.loadLibrary("native-lib");
    }
    private static final String TAG = "predictor";
    private static final long NULL = 0;

    protected long pInitNet;
    protected long pPredictNet;
    protected long pPredictor;

    // todo: load net from file instead of asset;
    // test in other project;
    // https://github.com/facebookarchive/caffe2/issues/567#issuecomment-301969664
    protected native long loadNetByFile(String filePath);
    protected native long loadNet(AssetManager mgr, String fileName);
    protected native long initCaffe2(long pInitNet, long pPredictNet);

    protected native void classificationFromCaffe2(long pPredictor, byte[] R, byte[] G, byte[] B, float[] result, int[] resultCnt);

    protected native void deletePtr(long ptr);

    public PredictorWrapper(String initNetFilePath, String predictNetFilePath) {
        this.pInitNet = loadNetByFile(initNetFilePath);
        this.pPredictNet = loadNetByFile(predictNetFilePath);
        this.pPredictor = initCaffe2(pInitNet, pPredictNet);
    }
    public PredictorWrapper(AssetManager assetManager, String initNetFileName, String predictNetFileName) {
        this.pInitNet = loadNet(assetManager, initNetFileName);
        this.pPredictNet = loadNet(assetManager, predictNetFileName);
        this.pPredictor = initCaffe2(pInitNet, pPredictNet);
    }

    public boolean isStatusOk() {
        return pInitNet != NULL
                && pPredictNet != NULL
                && pPredictor != NULL;
    }

    public void destroy() {
        destroy(pPredictor);
        pPredictor = NULL;
        destroy(pPredictNet);
        pPredictNet = NULL;
        destroy(pInitNet);
        pInitNet = NULL;
    }
    protected void destroy(long ptr) {
        if (ptr != NULL) {
            deletePtr(ptr);
        }
    }

    public float[] doImageClassification(Bitmap bitmap) {
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();

        byte[] R = new byte[h * w];
        byte[] B = new byte[h * w];
        byte[] G = new byte[h * w];
        float[] result = new float[1000];
        int[] resultCnt = new int[1];
        for (int i=0; i<result.length; ++i) {
            result[i] = 0;
        }
        resultCnt[0] = -1;

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int colour = bitmap.getPixel(i, j);
                int red = Color.red(colour);
                int green = Color.green(colour);
                int blue = Color.blue(colour);
                R[i * w + j] = (byte) red;
                G[i * w + j] = (byte) green;
                B[i * w + j] = (byte) blue;
            }
        }
        classificationFromCaffe2(pPredictor, R, G, B, result, resultCnt);
        return result;
    }
}
