package cn.ac.ict.acs.iot.aiot.android.tflite;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;
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

        if (JUtil.isEmpty(filePath) || JUtil.isEmpty(labelsFilePath) ) {
            return null;
        }
        return new TfLiteModelFromFile(desc, log,  filePath, labelsFilePath);
    }

    public abstract static class TfLiteModel extends AbstractModel {

        @Nullable
        protected final ModelDesc.Tflite modelDesc;

        protected Classifier classifier;

        public TfLiteModel(@Nullable ModelDesc.Tflite modelDesc, LogUtil.Log log) {
            super(modelDesc, log);
            this.modelDesc = modelDesc;
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
        protected Bitmap convertBitmap(Bitmap bitmapOri) {
            if (modelDesc == null) {
                return super.convertBitmap(bitmapOri);
            }
            ModelDesc.BaseModelDesc.BitmapConvertMethod m = modelDesc.getBitmapConvertMethod();
            if (m != null) {
                return super.convertBitmap(bitmapOri);
            }
            // model defined method;
            log.loglnA("ic", "tflite", "unknown bitmap convert method: " + modelDesc.getBitmap_convert_method());
            return super.convertBitmap(bitmapOri);
        }

        @Override
        @WorkerThread
        protected StatisticsScore doImageClassificationContinue(Bitmap bitmap, int target) {
            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
            log.loglnA("ic", "bitmap", bitmap, "ic", "end", StatisticsTime.TimeRecord.time());

            log.loglnA("ic", "bitmap", bitmap, "statistics", "start", StatisticsTime.TimeRecord.time());
            final float[] scores = getDataAsFloatArray(results);
            StatisticsScore statistics = new StatisticsScore(scoreTopK, scores);
            statistics.calc();
            statistics.updateHit(target);
            return statistics;
        }

        private float[] getDataAsFloatArray(List<Classifier.Recognition> results) {
            float[] ret = new float[dataset.getClassesInfo().getSize()];
            int resultsLen = results.size();
            boolean resultHasDummy = resultsLen != ret.length;
            for (int i=0; i<ret.length; ++i) {
                ret[i] = 0;
            }
            for (Classifier.Recognition result : results) {
                if (result == null) {
                    continue;
                }
                int idInt = result.getIdInt();
                if (resultHasDummy) idInt -= 1;  // tensorflow 的输出的第 0 个是 background，所以其 id 比普通 imageNet 的 id 多 1；
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
        public TfLiteModelFromFile(ModelDesc.Tflite desc, LogUtil.Log log, String net_tflite_filepath, String labelsFilePath) {
            super(desc, log);
            try {
                timeRecord.loadModel.setStart();
                classifier = Classifier.create(net_tflite_filepath, desc.getDevice(), 1, labelsFilePath, desc, log);
                timeRecord.loadModel.setEnd();
                log.logln("load model: " + timeRecord.loadModel);
            } catch (IOException e) {
                classifier = null;
                e.printStackTrace();
            }
        }
    }


}
