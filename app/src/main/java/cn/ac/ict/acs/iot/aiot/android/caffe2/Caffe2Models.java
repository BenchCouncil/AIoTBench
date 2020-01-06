package cn.ac.ict.acs.iot.aiot.android.caffe2;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import androidx.annotation.WorkerThread;

import cn.ac.ict.acs.iot.aiot.android.ModelHelper;
import cn.ac.ict.acs.iot.aiot.android.StatisticsScore;
import cn.ac.ict.acs.iot.aiot.android.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/**
 * Created by alanubu on 19-12-31.
 */
public class Caffe2Models {

    public abstract static class Caffe2Model extends ModelHelper.AbstractModel {

        protected PredictorWrapper predictor;

        public Caffe2Model(Activity activity, LogUtil.Log log, String initNetFileName, String predictNetFileName) {
            super(log);
            timeRecord.loadModel.setStart();
            AssetManager assetManager = activity.getAssets();
            predictor = new PredictorWrapper(assetManager, initNetFileName, predictNetFileName);
            timeRecord.loadModel.setEnd();
            log.logln("load model: " + timeRecord.loadModel);
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
    public abstract static class DefaultNet extends Caffe2Model {
        public DefaultNet(Activity activity, LogUtil.Log log, String initNetFileName, String predictNetFileName) {
            super(activity, log, initNetFileName, predictNetFileName);
        }

        @Override
        public int getInputImageWidth() {
            return 227;
        }

        @Override
        public int getInputImageHeight() {
            return 227;
        }
    }

    public static class MobileNet extends DefaultNet {
        public MobileNet(Activity activity, LogUtil.Log log) {
            super(activity, log, "caffe2/mobilenet_init_net_v2.pb", "caffe2/mobilenet_predict_net_v2.pb");
        }
    }
    public static class ResNet18 extends DefaultNet {
        public ResNet18(Activity activity, LogUtil.Log log) {
            super(activity, log, "caffe2/resnet18_init_net_v1.pb", "caffe2/resnet18_predict_net_v1.pb");
        }
    }
    public static class SqueezeNet extends DefaultNet {
        public SqueezeNet(Activity activity, LogUtil.Log log) {
            super(activity, log, "caffe2/squeeze_init_net_v1.pb", "caffe2/squeeze_predict_net_v1.pb");
        }
    }
    public static class UnknowNet extends DefaultNet {
        public UnknowNet(Activity activity, LogUtil.Log log) {
            super(activity, log, "caffe2/unknow_init_net.pb", "caffe2/unknow_predict_net.pb");
        }
    }
}
