package cn.ac.ict.acs.iot.aiot.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.github.labowenzi.commonj.JEnumUtil;

import cn.ac.ict.acs.iot.aiot.android.util.BitmapUtil;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.ThreadPoolUtil;

/**
 * Created by alanubu on 19-12-25.
 */
public class ModelHelper {

    public static final int HANDLER_DO_IMAGE_CLASSIFICATION = 59250;

    // java enum String 'Type{mobile_net, res_net, }'
    public enum Type implements JEnumUtil.EnumString {
        E_MOBILE_NET("mobile_net"),
        E_RES_NET("res_net"),

        E_MOBILE_NET_FLOAT("mobile_net_float"),
        E_MOBILE_NET_QUANTIZED("mobile_net_quantized"),
        ;

        @NonNull
        private final String value;

        Type(@NonNull String value) {
            this.value = value;
        }

        @NonNull
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return super.toString() + "(" + value + ")";
        }

        public static final Type[] values;

        static {
            values = values();
        }

        @Nullable
        public static Type get(@Nullable String value) {
            if (value == null) {
                return null;
            }
            for (Type item : values) {
                if (item.getValue().equals(value)) {
                    return item;
                }
            }
            return null;
        }
    }

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
        public final StatisticsTime.TimeRecord timeRecord;

        public Handler handler;
        public int what;

        public DatasetHelper.IDataset dataset;

        public final LogUtil.Log log;

        public AbstractModel(LogUtil.Log log) {
            timeRecord = new StatisticsTime.TimeRecord();
            this.log = log;
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
            timeRecord.images = new StatisticsTime.TimeRecord.StartEndTime[dataset.size()];
            Status status = new Status();
            sendMsg(-1, status);
            for (int i=0; i<dataset.size(); i++) {
                doImageClassificationContinue(i, status);
                sendMsg(i, status);
            }
            sendMsg(dataset.size(), status);
        }

        @WorkerThread
        protected void doImageClassificationContinue(int imageIndex, Status status) {
            StatisticsTime.TimeRecord.StartEndTime time = new StatisticsTime.TimeRecord.StartEndTime();
            timeRecord.images[imageIndex] = time;
            String imageFilePath = dataset.get(imageIndex);
            int target = dataset.getClassIndex(imageIndex);

            time.setStart();
            log.loglnA("ic", "index", imageIndex, "all", "start", time.start);

            log.loglnA("ic", "index", imageIndex, "bitmap", "start", StatisticsTime.TimeRecord.time());
            status.bitmapOri = BitmapFactory.decodeFile(imageFilePath);
            status.bitmapCroped = BitmapUtil.centerCropResize(status.bitmapOri, getInputImageWidth(), getInputImageHeight());
            log.loglnA("ic", "index", imageIndex, "bitmap", "end", StatisticsTime.TimeRecord.time());

            log.loglnA("ic", "index", imageIndex, "bitmap", status.bitmapCroped, "ic", "start", StatisticsTime.TimeRecord.time());
            StatisticsScore statistics = doImageClassificationContinue(status.bitmapCroped, target);

            log.loglnA("ic", "index", imageIndex, "statistics", "data", statistics);
            status.statistics.updateBy(statistics);
            log.loglnA("ic", "index", imageIndex, "statistics", "end", StatisticsTime.TimeRecord.time());
            time.setEnd();
            log.loglnA("ic", "index", imageIndex, "all", "end", time.end);
            log.loglnA("ic", "index", imageIndex, "all", "end", time);
        }

        @WorkerThread
        protected abstract StatisticsScore doImageClassificationContinue(Bitmap bitmap, int target);

        private void sendMsg(int process, Status status) {
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

    public static class Status {
        public StatisticsScore.HitStatistic statistics = new StatisticsScore.HitStatistic();
        public Bitmap bitmapOri = null;
        public Bitmap bitmapCroped = null;

    }
}
