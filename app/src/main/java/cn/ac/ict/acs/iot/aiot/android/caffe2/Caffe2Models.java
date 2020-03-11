package cn.ac.ict.acs.iot.aiot.android.caffe2;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import cn.ac.ict.acs.iot.aiot.android.StatisticsScore;
import cn.ac.ict.acs.iot.aiot.android.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.model.AbstractModel;
import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.model.ModelDesc;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/**
 * Created by alanubu on 19-12-31.
 */
public class Caffe2Models {

    public static Caffe2Model newModel(LogUtil.Log log, Model.ModelDir dir, ModelDesc.Caffe2 desc) {
        String init = desc.getInit_net_pb_filepath(dir);
        String predict = desc.getPredict_net_pb_filepath(dir);
        return new Caffe2ModelFromFile(desc, log, init, predict);
    }

    public abstract static class Caffe2Model extends AbstractModel {

        @Nullable
        protected final ModelDesc.Caffe2 modelDesc;

        protected PredictorWrapper predictor;

        public Caffe2Model(@Nullable ModelDesc.Caffe2 modelDesc, LogUtil.Log log) {
            super(modelDesc, log);
            this.modelDesc = modelDesc;
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
        }

        @Override
        public int getInputImageWidth() {
            return 224;
        }

        @Override
        public int getInputImageHeight() {
            return 224;
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
            float[] scores = predictor.doImageClassification(bitmap);
            log.loglnA("ic", "bitmap", bitmap, "ic", "end", StatisticsTime.TimeRecord.time());

            log.loglnA("ic", "bitmap", bitmap, "statistics", "start", StatisticsTime.TimeRecord.time());
            StatisticsScore statistics = new StatisticsScore(scores);
            statistics.calc();
            statistics.updateHit(target);
            return statistics;
        }
    }
    public static class Caffe2ModelFromFile extends Caffe2Model {
        public Caffe2ModelFromFile(ModelDesc.Caffe2 desc, LogUtil.Log log, String initNetFilePath, String predictNetFilePath) {
            super(desc, log);
            timeRecord.loadModel.setStart();
            predictor = new PredictorWrapper(initNetFilePath, predictNetFilePath, desc, log);
            timeRecord.loadModel.setEnd();
            log.logln("load model: " + timeRecord.loadModel);
        }
    }


}
