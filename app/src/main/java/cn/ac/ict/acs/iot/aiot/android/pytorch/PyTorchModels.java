package cn.ac.ict.acs.iot.aiot.android.pytorch;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.WorkerThread;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.IOException;

import cn.ac.ict.acs.iot.aiot.android.ModelHelper;
import cn.ac.ict.acs.iot.aiot.android.StatisticsScore;
import cn.ac.ict.acs.iot.aiot.android.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 19-12-26.
 */
public class PyTorchModels {

    public abstract static class PyTorchModel extends ModelHelper.AbstractModel {

        protected Module module;

        public PyTorchModel(LogUtil.Log log) {
            super(log);
        }

        protected static Module loadFromAssetFile(Context context, String filename) {
            try {
                return Module.load(Util.assetFilePath(context, filename));
            } catch (IOException e) {
                Log.e("load module", "Error reading assets", e);
                return null;
            }
        }

        @Override
        public boolean isStatusOk() {
            return module != null;
        }

        @Override
        protected void doDestroy() {
            super.doDestroy();
            if (module != null) {
                module.destroy();
            }
        }

        @Override
        @WorkerThread
        protected StatisticsScore doImageClassificationContinue(Bitmap bitmap, int target) {
            final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                    TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

            final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
            log.loglnA("ic", "bitmap", bitmap, "ic", "end", StatisticsTime.TimeRecord.time());

            log.loglnA("ic", "bitmap", bitmap, "statistics", "start", StatisticsTime.TimeRecord.time());
            final float[] scores = outputTensor.getDataAsFloatArray();
            StatisticsScore statistics = new StatisticsScore(scores);
            statistics.calc();
            statistics.updateHit(target);
            return statistics;
        }
    }

    public static class MobileNet_925 extends PyTorchModel {
        public static final String assetFile = "pytorch/mobilenet_quantized_scripted_925.pt";

        public static final int INPUT_TENSOR_WIDTH = 224;
        public static final int INPUT_TENSOR_HEIGHT = 224;

        public MobileNet_925(Context context, LogUtil.Log log) {
            super(log);
            timeRecord.loadModel.setStart();
            module = loadFromAssetFile(context, assetFile);
            timeRecord.loadModel.setEnd();
            log.logln("load model: " + timeRecord.loadModel);
        }

        @Override
        public int getInputImageWidth() {
            return INPUT_TENSOR_WIDTH;
        }
        @Override
        public int getInputImageHeight() {
            return INPUT_TENSOR_HEIGHT;
        }
    }

    public static class ResNet18 extends PyTorchModel {
        public static final String assetFile = "pytorch/resnet18.pt";

        public ResNet18(Context context, LogUtil.Log log) {
            super(log);
            timeRecord.loadModel.setStart();
            module = loadFromAssetFile(context, assetFile);
            timeRecord.loadModel.setEnd();
            log.logln("load model: " + timeRecord.loadModel);
        }

        @Override
        public int getInputImageWidth() {
            return 224;
        }
        @Override
        public int getInputImageHeight() {
            return 224;
        }
    }

}
