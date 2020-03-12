package cn.ac.ict.acs.iot.aiot.android.caffe2;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;

import cn.ac.ict.acs.iot.aiot.android.model.ModelDesc;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/**
 * Created by alanubu on 20-1-2.
 */
public class PredictorWrapper {
    static {
        System.loadLibrary("native-lib");
    }
    private static final String TAG = "predictor";
    private static final long NULL = 0;

    private final LogUtil.Log log;

    protected long pInitNet;
    protected long pPredictNet;
    protected long pWrapper;

    protected final boolean needToBgr;
    protected final float[] normMean;
    protected final float[] normStdDev;

    // todo: load net from file instead of asset;
    // test in other project;
    // https://github.com/facebookarchive/caffe2/issues/567#issuecomment-301969664
    protected native long loadNetByFile(String filePath);
    protected native long loadNet(AssetManager mgr, String fileName);
    protected native long initCaffe2(long pInitNet, long pPredictNet, boolean needToBgr, float[] normMean, int normMeanCnt, float[] normStdDev, int normStdDevCnt);

    protected native void classificationFromCaffe2(long pWrapper, byte[] R, byte[] G, byte[] B, float[] result, int[] resultCnt);

    protected native void deletePtr(long ptr);

    public PredictorWrapper(String initNetFilePath, String predictNetFilePath, ModelDesc.Caffe2 desc, LogUtil.Log log) {
        this.log = log;
        //this.needToBgr = (desc != null) && (desc.needToBgr());
        this.needToBgr = desc.needToBgr();
        this.normMean = desc == null ? null : desc.getNorm_mean();
        int normMeanCnt = this.normMean == null ? -1 : this.normMean.length;
        this.normStdDev = desc == null ? null : desc.getNorm_std_dev();
        int normStdDevCnt = this.normStdDev == null ? -1 : this.normStdDev.length;
        this.pInitNet = loadNetByFile(initNetFilePath);
        this.pPredictNet = loadNetByFile(predictNetFilePath);
        this.pWrapper = initCaffe2(pInitNet, pPredictNet, needToBgr, normMean, normMeanCnt, normStdDev, normStdDevCnt);
    }

    public boolean isStatusOk() {
        return pInitNet != NULL
                && pPredictNet != NULL
                && pWrapper != NULL;
    }

    public void destroy() {
        destroy(pWrapper);
        pWrapper = NULL;
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
        byte[] G = new byte[h * w];
        byte[] B = new byte[h * w];
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
        classificationFromCaffe2(pWrapper, R, G, B, result, resultCnt);
        return result;
    }
}
