package cn.ac.ict.acs.iot.aiot.android.tflite;

import android.app.Activity;
import android.graphics.Bitmap;

import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.util.List;

import cn.ac.ict.acs.iot.aiot.android.pytorch.PyTorchModels;
import cn.ac.ict.acs.iot.aiot.android.pytorch.PyTorchScoreStatistics;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/**
 * Created by alanubu on 19-12-30.
 */
public class TfLiteModels {

    public static final int HANDLER_DO_IMAGE_CLASSIFICATION = PyTorchModels.HANDLER_DO_IMAGE_CLASSIFICATION;

    public abstract static class TfLiteModel extends PyTorchModels.AbstractModel {

        protected Classifier classifier;

        public TfLiteModel(Activity activity, Classifier.Model model, LogUtil.Log log) {
            super(log);
            try {
                timeRecord.loadModel.setStart();
                classifier = Classifier.create(activity, model, Classifier.Device.CPU, 1, log);
                timeRecord.loadModel.setEnd();
                log.logln("load model: " + timeRecord.loadModel);
            } catch (IOException e) {
                classifier = null;
                e.printStackTrace();
            }
        }

        @Override
        public boolean isStatusOk() {
            return classifier != null;
        }

        @Override
        public int getInputImageWidth() {
            return classifier.getImageSizeX();
        }

        @Override
        public int getInputImageHeight() {
            return classifier.getImageSizeY();
        }

        protected void doImageClassificationContinue2(int imageIndex, PyTorchModels.Status status) {
            //
        }

        @Override
        @WorkerThread
        protected PyTorchScoreStatistics doImageClassificationContinue(Bitmap bitmap, int target) {
            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
            log.loglnA("ic", "bitmap", bitmap, "ic", "end", PyTorchModels.TimeRecord.time());

            log.loglnA("ic", "bitmap", bitmap, "statistics", "start", PyTorchModels.TimeRecord.time());
            final float[] scores = getDataAsFloatArray(results);
            PyTorchScoreStatistics statistics = new PyTorchScoreStatistics(scores);
            statistics.calc();
            statistics.updateHit(target);
            return statistics;
        }

        private float[] getDataAsFloatArray(List<Classifier.Recognition> results) {
            float[] ret = new float[dataset.getClassesInfo().getSize()];
            for (int i=0; i<ret.length; ++i) {
                ret[i] = 0;
            }
            for (Classifier.Recognition result : results) {
                if (result == null) {
                    continue;
                }
                int idInt = result.getIdInt();
                idInt -= 1;  // tensorflow 的输出的第 0 个是 background，所以其 id 比普通 imageNet 的 id 多 1；
                if (idInt < 0 || idInt >=ret.length) {
                    continue;
                }
                ret[idInt] = result.getConfidence();
            }
            return ret;
        }

        @Override
        public void destroy() {
            super.destroy();
            classifier.close();
        }
    }

    public static class MobileNetFloat extends TfLiteModel {
        public MobileNetFloat(Activity activity, LogUtil.Log log) {
            super(activity, Classifier.Model.FLOAT, log);
        }
    }
    public static class MobileNetQuantized extends TfLiteModel {
        public MobileNetQuantized(Activity activity, LogUtil.Log log) {
            super(activity, Classifier.Model.QUANTIZED, log);
        }
    }
}
