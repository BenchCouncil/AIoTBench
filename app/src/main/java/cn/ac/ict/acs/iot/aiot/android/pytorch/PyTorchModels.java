package cn.ac.ict.acs.iot.aiot.android.pytorch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.IOException;

import cn.ac.ict.acs.iot.aiot.android.DatasetHelper;
import cn.ac.ict.acs.iot.aiot.android.util.BitmapUtil;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.MathUtil;
import cn.ac.ict.acs.iot.aiot.android.util.ThreadPoolUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 19-12-26.
 */
public class PyTorchModels {

    public static final int HANDLER_DO_IMAGE_CLASSIFICATION = 59250;

    public interface IModel {

        /**
         * @return image width;
         */
        int getInputImageWidth();
        /**
         * @return image height;
         */
        int getInputImageHeight();

        boolean isStatusOk();

        /**
         * do image classification, background, set send statistics data by handler;
         */
        void doImageClassification();
    }

    public abstract static class AbstractModel implements IModel {
        public final TimeRecord timeRecord;

        public Handler handler;
        public int what;

        public DatasetHelper.IDataset dataset;

        public final LogUtil.Log log;

        public AbstractModel(LogUtil.Log log) {
            timeRecord = new TimeRecord();
            this.log = log;
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
        public void doImageClassification() {
            ThreadPoolUtil.run(this::doImageClassificationContinue);
        }
        @WorkerThread
        protected void doImageClassificationContinue() {
            if (dataset == null || dataset.size() <= 0) {
                sendMsg(dataset == null ? -1 : dataset.size(), null);
                return;
            }
            timeRecord.images = new TimeRecord.StartEndTime[dataset.size()];
            Status status = new Status();
            sendMsg(-1, status);
            for (int i=0; i<dataset.size(); i++) {
                doImageClassificationContinue(i, status);
                sendMsg(i, status);
            }
            sendMsg(dataset.size(), status);
        }

        @WorkerThread
        protected void doImageClassificationContinue(int imageIndex, PyTorchModels.Status status) {
            TimeRecord.StartEndTime time = new TimeRecord.StartEndTime();
            timeRecord.images[imageIndex] = time;
            String imageFilePath = dataset.get(imageIndex);
            int target = dataset.getClassIndex(imageIndex);

            time.setStart();
            log.loglnA("ic", "index", imageIndex, "all", "start", time.start);

            log.loglnA("ic", "index", imageIndex, "bitmap", "start", TimeRecord.time());
            status.bitmapOri = BitmapFactory.decodeFile(imageFilePath);
            status.bitmapCroped = BitmapUtil.centerCropResize(status.bitmapOri, getInputImageWidth(), getInputImageHeight());
            log.loglnA("ic", "index", imageIndex, "bitmap", "end", TimeRecord.time());

            log.loglnA("ic", "index", imageIndex, "bitmap", status.bitmapCroped, "ic", "start", TimeRecord.time());
            PyTorchScoreStatistics statistics = doImageClassificationContinue(status.bitmapCroped, target);

            log.loglnA("ic", "index", imageIndex, "statistics", "data", statistics);
            status.statistics.updateBy(statistics);
            log.loglnA("ic", "index", imageIndex, "statistics", "end", TimeRecord.time());
            time.setEnd();
            log.loglnA("ic", "index", imageIndex, "all", "end", time.end);
            log.loglnA("ic", "index", imageIndex, "all", "end", time);
        }

        @WorkerThread
        protected abstract PyTorchScoreStatistics doImageClassificationContinue(Bitmap bitmap, int target);

        private void sendMsg(int process, PyTorchModels.Status status) {
            Message message = handler.obtainMessage();
            message.what = what;
            message.arg1 = 0;
            message.obj = status;
            message.arg2 = process;
            handler.sendMessage(message);
        }

        public void destroy() {
        }
    }

    public abstract static class PyTorchModel extends AbstractModel {

        protected Module module;

        public PyTorchModel(LogUtil.Log log) {
            super(log);
        }

        @Override
        public boolean isStatusOk() {
            return module != null;
        }

        @Override
        public void destroy() {
            super.destroy();
            if (module != null) {
                module.destroy();
            }
        }

        @Override
        @WorkerThread
        protected PyTorchScoreStatistics doImageClassificationContinue(Bitmap bitmap, int target) {
            final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                    TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

            final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
            log.loglnA("ic", "bitmap", bitmap, "ic", "end", TimeRecord.time());

            log.loglnA("ic", "bitmap", bitmap, "statistics", "start", TimeRecord.time());
            final float[] scores = outputTensor.getDataAsFloatArray();
            PyTorchScoreStatistics statistics = new PyTorchScoreStatistics(scores);
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

    public static class TimeRecord {
        public StartEndTime loadModel = new StartEndTime();
        public StartEndTime loadDataset = new StartEndTime();

        public StartEndTime[] images = null;

        public Statistics statistics;

        public void calc() {
            if (images == null) {
                statistics = null;
            } else {
                statistics = new Statistics(images);
                statistics.calc();
            }
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append("load model=").append(loadModel.diff());
            s.append(", load dataset=").append(loadDataset.diff());
            if (statistics != null) {
                s.append(", images.len=").append(statistics.length());
                s.append("\n" + "maxIndex=").append(statistics.maxIndex[0]);
                s.append(' ' + "max=").append(statistics.max[0]);
                s.append(", " + "minIndex=").append(statistics.minIndex[0]);
                s.append(' ' + "min=").append(statistics.min[0]);
                s.append(", avg=").append(statistics.avg)
                        .append(", sd=").append(statistics.sd);
                s.append("\nfirstTime=").append(statistics.firstTime)
                        .append(", lastTime=").append(statistics.lastTime);
                s.append(", avgWithoutFirstTime=").append(statistics.avgWithoutFirstTime);
            }
            return s.toString();
        }

        public static class StartEndTime {
            public long start = -1;
            public long end = -1;

            public void setStart() {
                start = time();
            }
            public void setEnd() {
                end = time();
            }
            public long diff() {
                long d = end - start;
                if (d < 0) {
                    return -1;
                } else {
                    return d;
                }
            }

            @NonNull
            @Override
            public String toString() {
                return "s=" + start + ",e=" + end + ",d=" + diff();
            }
        }

        public static class Statistics extends MathUtil.StatisticsLong{
            public double firstTime;
            public double lastTime;
            public double avgWithoutFirstTime;

            public Statistics(StartEndTime[] data) {
                super(toLong(data));
            }

            @Override
            public void calc() {
                super.calc();
                int len = length();
                if (len > 0) {
                    firstTime = get(0);
                    lastTime = get(len - 1);
                    if (len > 1) {
                        double sum = 0;
                        for (int i = 1; i < len; ++i) {
                            sum += get(i);
                        }
                        avgWithoutFirstTime = sum / (len - 1);
                    } else {
                        avgWithoutFirstTime = 0;
                    }
                } else {
                    firstTime = -1;
                    lastTime = -1;
                    avgWithoutFirstTime = -1;
                }
            }

            public static long[] toLong(StartEndTime[] data) {
                if (data == null) {
                    return null;
                }
                int i;
                for (i=0; i<data.length; ++i) {
                    StartEndTime set = data[i];
                    if (set == null) {
                        break;
                    }
                }
                if (i <= 0) {
                    return new long[0];
                }
                long[] ret = new long[i];
                for (int j=0; j<i; ++j) {
                    ret[j] = data[j].diff();
                }
                return ret;
            }
        }

        public static long time() {
            return SystemClock.elapsedRealtime();
        }
    }

    public static class Status {
        public PyTorchScoreStatistics.HitStatistic statistics = new PyTorchScoreStatistics.HitStatistic();
        public Bitmap bitmapOri = null;
        public Bitmap bitmapCroped = null;

    }
}
