package cn.ac.ict.acs.iot.aiot.android.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.github.labowenzi.commonj.ThreadPoolUtil;
import com.github.labowenzi.commonj.log.Log;

import cn.ac.ict.acs.iot.aiot.android.StatisticsScore;
import cn.ac.ict.acs.iot.aiot.android.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.dataset.IDataset;
import cn.ac.ict.acs.iot.aiot.android.util.BitmapUtil;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/**
 * Created by alanubu on 20-1-20.
 */
public abstract class AbstractModel implements IModel {

    public static final int HANDLER_DO_IMAGE_CLASSIFICATION = 59250;

    public final StatisticsTime.TimeRecord timeRecord;

    public Handler handler;
    public int what;

    public IDataset dataset;

    public final LogUtil.Log log;

    private boolean destroyed;
    private boolean preDestroyed;
    private boolean workThreadRunning;

    @Nullable
    protected final ModelDesc.BaseModelDesc modelDesc;

    public AbstractModel(@Nullable ModelDesc.BaseModelDesc modelDesc, LogUtil.Log log) {
        timeRecord = new StatisticsTime.TimeRecord();
        this.modelDesc = modelDesc;
        this.log = log;
        this.destroyed = false;
        this.preDestroyed = false;
        this.workThreadRunning = false;
    }

    @Override
    public void doImageClassification() {
        ThreadPoolUtil.run(this::doImageClassificationContinue);
    }
    @WorkerThread
    protected void doImageClassificationContinue() {
        if (isDestroyed() || isPreDestroyed()) {
            return;
        }
        workThreadRunning = true;
        if (dataset == null || dataset.size() <= 0) {
            sendMsg(dataset == null ? -1 : dataset.size(), null);
            workThreadRunning = false;
            return;
        }
        timeRecord.images = new StatisticsTime.TimeRecord.StartEndTime[dataset.size()];
        Status status = new Status();
        sendMsg(-1, status);
        for (int i=0; i<dataset.size(); i++) {
            if (isDestroyed() || isPreDestroyed()) {
                break;
            }
            doImageClassificationContinue(i, status);
            sendMsg(i, status);
        }
        sendMsg(dataset.size(), status);
        workThreadRunning = false;
        if (isPreDestroyed() && !isDestroyed()) {
            this.destroyed = true;
            doDestroy();
        }
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
        status.bitmapCroped = convertBitmap(status.bitmapOri);
        log.loglnA("ic", "index", imageIndex, "bitmap", "end", StatisticsTime.TimeRecord.time());

        try {
            log.loglnA("ic", "index", imageIndex, "bitmap", status.bitmapCroped, "ic", "start", StatisticsTime.TimeRecord.time());
            StatisticsScore statistics = doImageClassificationContinue(status.bitmapCroped, target);

            log.loglnA("ic", "index", imageIndex, "statistics", "data", statistics);
            status.statistics.updateBy(statistics);
            log.loglnA("ic", "index", imageIndex, "statistics", "end", StatisticsTime.TimeRecord.time());
        } catch (Exception e) {
            log.loglnA("ic", "index", imageIndex, "do image classification error", "exception", e);
            Log.e("ic", "do image classification error", e);
        }
        time.setEnd();
        log.loglnA("ic", "index", imageIndex, "all", "end", time.end);
        log.loglnA("ic", "index", imageIndex, "all", "end", time);
    }

    @WorkerThread
    protected Bitmap convertBitmap(Bitmap bitmapOri) {
        if (modelDesc != null) {
            ModelDesc.BaseModelDesc.BitmapConvertMethod m = modelDesc.getBitmapConvertMethod();
            if (m == ModelDesc.BaseModelDesc.BitmapConvertMethod.DEFAULT) {
                return BitmapUtil.centerCropResize(bitmapOri, getInputImageWidth(), getInputImageHeight());
            } else if (m == ModelDesc.BaseModelDesc.BitmapConvertMethod.COPY) {
                return bitmapOri.copy(bitmapOri.getConfig(), true);
            }
        }
        return BitmapUtil.centerCropResize(bitmapOri, getInputImageWidth(), getInputImageHeight());
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
        if (isWorkThreadRunning()) {
            preDestroyed = true;
        } else {
            if (isDestroyed()) {
                return;
            }
            this.destroyed = true;
            doDestroy();
        }
    }

    protected void doDestroy() {
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public boolean isPreDestroyed() {
        return preDestroyed;
    }

    public boolean isWorkThreadRunning() {
        return workThreadRunning;
    }

    public static class Status {
        public StatisticsScore.HitStatistic statistics = new StatisticsScore.HitStatistic();
        public Bitmap bitmapOri = null;
        public Bitmap bitmapCroped = null;

    }
}
