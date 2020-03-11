package cn.ac.ict.acs.iot.aiot.android.pytorch;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.github.labowenzi.commonj.log.Log;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.IOException;

import cn.ac.ict.acs.iot.aiot.android.StatisticsScore;
import cn.ac.ict.acs.iot.aiot.android.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.model.AbstractModel;
import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.model.ModelDesc;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 19-12-26.
 */
public class PyTorchModels {

    public static PyTorchModel newModel(LogUtil.Log log, Model.ModelDir dir, ModelDesc.Pytorch desc) {
        String filePath = desc.getNet_pt_filepath(dir);
        return new PyTorchModelFromFile(desc, log, filePath);
    }

    public abstract static class PyTorchModel extends AbstractModel {

        public static final int INPUT_TENSOR_WIDTH = 224;
        public static final int INPUT_TENSOR_HEIGHT = 224;

        @Nullable
        protected final ModelDesc.Pytorch modelDesc;

        protected Module module;

        public PyTorchModel(@Nullable ModelDesc.Pytorch modelDesc, LogUtil.Log log) {
            super(modelDesc, log);
            this.modelDesc = modelDesc;
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
        public int getInputImageWidth() {
            return INPUT_TENSOR_WIDTH;
        }
        @Override
        public int getInputImageHeight() {
            return INPUT_TENSOR_HEIGHT;
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
            log.loglnA("ic", "pytorch", "unknown bitmap convert method: " + modelDesc.getBitmap_convert_method());
            return super.convertBitmap(bitmapOri);
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

    public static class PyTorchModelFromFile extends PyTorchModel {
        public PyTorchModelFromFile(ModelDesc.Pytorch desc, LogUtil.Log log, String filePath) {
            super(desc, log);
            timeRecord.loadModel.setStart();
            module = Module.load(filePath);
            timeRecord.loadModel.setEnd();
            log.logln("load model: " + timeRecord.loadModel);
        }
    }

}
