package cn.ac.ict.acs.iot.aiot.android.tflite;

import android.app.Activity;
import android.graphics.Bitmap;

import androidx.annotation.WorkerThread;

import com.github.labowenzi.commonj.JUtil;

import java.io.IOException;
import java.util.List;

import cn.ac.ict.acs.iot.aiot.android.StatisticsScore;
import cn.ac.ict.acs.iot.aiot.android.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.model.AbstractModel;
import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.model.ModelDesc;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/**
 * Created by alanubu on 19-12-30.
 */
public class TfLiteModels {

    public static TfLiteModel newModel(LogUtil.Log log, Model.ModelDir dir, ModelDesc.Tflite desc) {
        String filePath = desc.getNet_tflite_filepath(dir);
        String labelsFilePath = desc.getLabels_filepath(dir);
        Classifier.Model m = getModel(desc);
        if (JUtil.isEmpty(filePath) || JUtil.isEmpty(labelsFilePath) || m == null) {
            return null;
        }
        return new TfLiteModelFromFile(log, m, filePath, labelsFilePath);
    }
    public static Classifier.Model getModel(ModelDesc.Tflite desc) {
        String q = desc.getQuantization();
        Classifier.Model[] models = Classifier.Model.values();
        for (Classifier.Model m : models) {
            if (m.name().toLowerCase().equals(q)) {
                return m;
            }
        }
        return Classifier.Model.FLOAT;
    }

    public abstract static class TfLiteModel extends AbstractModel {

        protected Classifier classifier;

        public TfLiteModel(LogUtil.Log log) {
            super(log);
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

        @Override
        @WorkerThread
        protected StatisticsScore doImageClassificationContinue(Bitmap bitmap, int target) {
            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
            log.loglnA("ic", "bitmap", bitmap, "ic", "end", StatisticsTime.TimeRecord.time());

            log.loglnA("ic", "bitmap", bitmap, "statistics", "start", StatisticsTime.TimeRecord.time());
            final float[] scores = getDataAsFloatArray(results);
            StatisticsScore statistics = new StatisticsScore(scores);
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
        protected void doDestroy() {
            super.doDestroy();
            classifier.close();
        }
    }
    public static class TfLiteModelFromFile extends TfLiteModel {
        public TfLiteModelFromFile(LogUtil.Log log, Classifier.Model model, String net_tflite_filepath, String labelsFilePath) {
            super(log);
            try {
                timeRecord.loadModel.setStart();
                classifier = Classifier.create(net_tflite_filepath, model, Classifier.Device.CPU, 1, labelsFilePath, log);
                timeRecord.loadModel.setEnd();
                log.logln("load model: " + timeRecord.loadModel);
            } catch (IOException e) {
                classifier = null;
                e.printStackTrace();
            }
        }
    }
    public abstract static class DefaultNet extends TfLiteModel {
        public DefaultNet(Activity activity, Classifier.Model model, LogUtil.Log log) {
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
    }

    public static class MobileNetFloat extends DefaultNet {
        public MobileNetFloat(Activity activity, LogUtil.Log log) {
            super(activity, Classifier.Model.FLOAT, log);
        }
    }
    public static class MobileNetQuantized extends DefaultNet {
        public MobileNetQuantized(Activity activity, LogUtil.Log log) {
            super(activity, Classifier.Model.QUANTIZED, log);
        }
    }
}
