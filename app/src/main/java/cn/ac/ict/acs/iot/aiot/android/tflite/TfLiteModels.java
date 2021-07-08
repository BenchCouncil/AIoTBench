package cn.ac.ict.acs.iot.aiot.android.tflite;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.github.labowenzi.commonj.JUtil;
import com.github.labowenzi.commonj.log.Log;

import java.io.IOException;
import java.util.List;

import cn.ac.ict.acs.iot.aiot.android.statistics.StatisticsScore;
import cn.ac.ict.acs.iot.aiot.android.statistics.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.model.AbstractModel;
import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.model.ModelDesc;
import cn.ac.ict.acs.iot.aiot.android.tflite_detector.Detector;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 19-12-30.
 */
public class TfLiteModels {

    public static TfLiteModel newModel(LogUtil.Log log, Model.ModelDir dir, String quantName, ModelDesc.Tflite desc, Model.Device device) {
        String filePath = desc.getNet_tflite_filepath(dir, quantName);
        String labelsFilePath = desc.getLabels_filepath(dir);

        if (JUtil.isEmpty(filePath) || JUtil.isEmpty(labelsFilePath) ) {
            return null;
        }
        return new TfLiteModelFromFile(desc, log,  filePath, labelsFilePath, device);
    }

    public abstract static class TfLiteModel extends AbstractModel {

        @Nullable
        protected final ModelDesc.Tflite modelDesc;

        protected Classifier classifier;
        protected Detector detector;

        public TfLiteModel(@Nullable ModelDesc.Tflite modelDesc, LogUtil.Log log) {
            super(modelDesc, log);
            this.modelDesc = modelDesc;
        }

        @Override
        public boolean isStatusOk() {
            return true;//note:ok???
        }

        @Override
        public int getInputImageWidth() {
            int wFromModel = classifier.getImageSizeX();
            int wFromDesc = super.getInputImageWidth();
            if (wFromModel != wFromDesc) {
                Object[] msg = {
                        "ic", "error", "width from model and desc are not equal", "width from model=" + wFromModel, "width from desc=" + wFromDesc
                };
                log.loglnA(msg);
                Log.e("ic", Util.arrToString(msg, 1));
            }
            return wFromDesc;
        }

        @Override
        public int getInputImageHeight() {
            int hFromModel = classifier.getImageSizeY();
            int hFromDesc = super.getInputImageHeight();
            if (hFromModel != hFromDesc) {
                Object[] msg = {
                        "ic", "error", "height from model and desc are not equal", "height from model=" + hFromModel, "height from desc=" + hFromDesc
                };
                log.loglnA(msg);
                Log.e("ic", Util.arrToString(msg, 1));
            }
            return hFromDesc;
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
            final List<Recognition> results = classifier.recognizeImage(bitmap);
            log.loglnA("ic", "bitmap", bitmap, "ic", "end", StatisticsTime.TimeRecord.time());

            log.loglnA("ic", "bitmap", bitmap, "statistics", "start", StatisticsTime.TimeRecord.time());
            final float[] scores = getDataAsFloatArray(results);
            StatisticsScore statistics = new StatisticsScore(scoreTopK, scores);
            statistics.calc();
            statistics.updateHit(target);
            return statistics;
        }
        @Override
        @WorkerThread
        protected StatisticsScore doObjectDetectionContinue(Bitmap bitmap, int target) {
            final List<Recognition> results = detector.recognizeImage(bitmap);
            log.loglnA("ic", "bitmap", bitmap, "ic", "end", StatisticsTime.TimeRecord.time());

            log.loglnA("ic", "bitmap", bitmap, "statistics", "start", StatisticsTime.TimeRecord.time());
            final float[] scores = getDataAsFloatArray(results);
            StatisticsScore statistics = new StatisticsScore(scoreTopK, scores);
            statistics.calc();
            statistics.updateHit(target);
            return statistics;
        }
        private float[] getDataAsFloatArray(List<Recognition> results) {
            float[] ret = new float[dataset.getClassesInfo().getSize()];
            int resultsLen = results.size();
            boolean resultHasDummy = resultsLen != ret.length;
            for (int i=0; i<ret.length; ++i) {
                ret[i] = 0;
            }
            for (Recognition result : results) {
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
            if (classifier != null) {
                classifier.close();
            }
            System.gc();
        }
    }
    public static class TfLiteModelFromFile extends TfLiteModel {
        public TfLiteModelFromFile(ModelDesc.Tflite desc, LogUtil.Log log, String net_tflite_filepath, String labelsFilePath, Model.Device device) {
            super(desc, log);
            try {
                if (desc.getTask().equals("image_classification")){
                    timeRecord.loadModel.setStart();
                    classifier = Classifier.create(net_tflite_filepath, device, 1, labelsFilePath, desc, log);
                    timeRecord.loadModel.setEnd();
                }else if (desc.getTask().equals("object_detection")){
                    timeRecord.loadModel.setStart();
                    detector = Detector.create(net_tflite_filepath, device, 1, labelsFilePath, desc, log);
                    timeRecord.loadModel.setEnd();
                }

                log.logln("load model: " + timeRecord.loadModel);
            } catch (IOException e) {
                classifier = null;
                e.printStackTrace();
            }
        }
    }


}
