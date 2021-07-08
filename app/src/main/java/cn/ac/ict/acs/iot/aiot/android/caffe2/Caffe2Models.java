package cn.ac.ict.acs.iot.aiot.android.caffe2;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.JUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.ac.ict.acs.iot.aiot.android.statistics.StatisticsScore;
import cn.ac.ict.acs.iot.aiot.android.statistics.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.dataset.IDataset;
import cn.ac.ict.acs.iot.aiot.android.dataset.IImageClasses;
import cn.ac.ict.acs.iot.aiot.android.model.AbstractModel;
import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.model.ModelDesc;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/**
 * Created by alanubu on 19-12-31.
 */
public class Caffe2Models {

    public static Caffe2Model newModel(LogUtil.Log log, Model.ModelDir dir, ModelDesc.Caffe2 desc) {
        String labelsFilePath = desc.getLabels_filepath(dir);
        String init = desc.getInit_net_pb_filepath(dir);
        String predict = desc.getPredict_net_pb_filepath(dir);
        return new Caffe2ModelFromFile(desc, log, init, predict, labelsFilePath);
    }

    public abstract static class Caffe2Model extends AbstractModel {

        @Nullable
        protected final ModelDesc.Caffe2 modelDesc;

        protected String labelsFilePath;
        protected int[] labelTrans;

        protected PredictorWrapper predictor;

        public Caffe2Model(@Nullable ModelDesc.Caffe2 modelDesc, LogUtil.Log log, String labelsFilePath) {
            super(modelDesc, log);
            this.modelDesc = modelDesc;
            this.labelsFilePath = labelsFilePath;
            this.labelTrans = null;
        }

        @Override
        public void setDataset(IDataset dataset) {
            super.setDataset(dataset);
            loadLabels();
        }
        private void loadLabels() {
            labelTrans = null;
            if (JUtil.isEmpty(labelsFilePath) || dataset == null || dataset.getClassesInfo() == null) {
                return;
            }
            List<String> lines = JIoUtil.readLines(labelsFilePath);
            if (JUtil.isEmpty(lines)) {
                return;
            }
            int length = lines.size();
            labelTrans = new int[length];
            IImageClasses info =  dataset.getClassesInfo();
            int infoLen = info.getSize();
            Map<String, Integer> classNameToIndex = new HashMap<>(length);
            for (int i=0; i<infoLen; ++i) {
                classNameToIndex.put(info.getName(i), i);
            }
            for (int i = 0; i < length; i++) {
                String s = lines.get(i);
                String[] ss = s.split("\t");
                String classeName = ss[0];
                Integer index = classNameToIndex.get(classeName);
                labelTrans[i] = index == null ? -1 : index;
            }
        }

        @Override
        public boolean isStatusOk() {
            return predictor.isStatusOk();
        }

        @Override
        protected void doDestroy() {
            super.doDestroy();
            if (predictor != null) {
                predictor.destroy();
            }
            System.gc();
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
            log.loglnA("ic", "caffe2", "unknown bitmap convert method: " + modelDesc.getBitmap_convert_method());
            return super.convertBitmap(bitmapOri);
        }

        @Override
        @WorkerThread
        protected StatisticsScore doImageClassificationContinue(Bitmap bitmap, int target) {
            float[] scoresOri = predictor.doImageClassification(bitmap);
            float[] scores;
            if (labelTrans == null) {
                scores = scoresOri;
            } else {
                scores = new float[scoresOri.length];
                for (int i=0; i<scoresOri.length; ++i) {
                    int newI = labelTrans[i];
                    if (newI < 0) {
                        continue;
                    }
                    scores[newI] = scoresOri[i];
                }
            }
            log.loglnA("ic", "bitmap", bitmap, "ic", "end", StatisticsTime.TimeRecord.time());

            log.loglnA("ic", "bitmap", bitmap, "statistics", "start", StatisticsTime.TimeRecord.time());
            StatisticsScore statistics = new StatisticsScore(scoreTopK, scores);
            statistics.calc();
            statistics.updateHit(target);
            return statistics;
        }

        @Override
        protected StatisticsScore doObjectDetectionContinue(Bitmap bitmap, int target) {
            return null;
        }
    }
    public static class Caffe2ModelFromFile extends Caffe2Model {
        public Caffe2ModelFromFile(ModelDesc.Caffe2 desc, LogUtil.Log log, String initNetFilePath, String predictNetFilePath, String labelsFilePath) {
            super(desc, log, labelsFilePath);
            timeRecord.loadModel.setStart();
            predictor = new PredictorWrapper(initNetFilePath, predictNetFilePath, getInputImageWidth(), getInputImageHeight(), desc, log);
            timeRecord.loadModel.setEnd();
            log.logln("load model: " + timeRecord.loadModel);
        }
    }


}
