package cn.ac.ict.acs.iot.aiot.android.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.github.labowenzi.commonj.JJsonUtils;
import com.github.labowenzi.commonj.ThreadPoolUtil;
import com.github.labowenzi.commonj.log.Log;

import cn.ac.ict.acs.iot.aiot.android.statistics.StatisticsScore;
import cn.ac.ict.acs.iot.aiot.android.statistics.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.dataset.IDataset;
import cn.ac.ict.acs.iot.aiot.android.util.BitmapUtil;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.MathUtil;

/**
 * Created by alanubu on 20-1-20.
 */
public abstract class AbstractModel implements IModel {

    public static final int INPUT_TENSOR_WIDTH = 224;
    public static final int INPUT_TENSOR_HEIGHT = 224;

    public static final int HANDLER_DO_IMAGE_CLASSIFICATION = 59250;
    public static final int HANDLER_DO_OBJECT_DETECTION= 69250;


    public final StatisticsTime.TimeRecord timeRecord;

    public Handler handler;
    public int what;

    protected IDataset dataset;

    public final LogUtil.Log log;
    public int scoreTopK = MathUtil.Statistics.DEFAULT_TOP_K;

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

    protected int[] getBitmapConvertSizeFromDesc() {
        if (modelDesc == null || modelDesc.getBitmap_convert_size() == null || modelDesc.getBitmap_convert_size().length < 2) {
            return null;
        }
        return modelDesc.getBitmap_convert_size();
    }
    @Override
    public int getInputImageWidth() {
        int[] imgSize = getBitmapConvertSizeFromDesc();
        return imgSize == null ? INPUT_TENSOR_WIDTH : imgSize[0];
    }
    @Override
    public int getInputImageHeight() {
        int[] imgSize = getBitmapConvertSizeFromDesc();
        return imgSize == null ? INPUT_TENSOR_HEIGHT : imgSize[1];
    }

    public void setDataset(IDataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public void doImageClassification() {
        ThreadPoolUtil.run(this::doImageClassificationContinue);
    }
    @Override
    public void doObjectDetection() {
        ThreadPoolUtil.run(this::doObjectDetectionContinue);
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
        Status status = new Status(scoreTopK);
        sendMsg(-1, status);
        timeRecord.taskTotal.setStart();
        for (int i=0; i<dataset.size(); i++) {
            if (isDestroyed() || isPreDestroyed()) {
                break;
            }
            doImageClassificationContinue(i, status);//一张图片做一次分类
            sendMsg(i, status);
        }
        timeRecord.taskTotal.setEnd();
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
        int target = dataset.getClassIndex(imageIndex);//note:Ground Truth idx

        time.setStart();
        log.loglnA("ic", "index", imageIndex, "all", "start", time.start);

        log.loglnA("ic", "index", imageIndex, "bitmap", "start", StatisticsTime.TimeRecord.time());
        status.bitmapOri = BitmapFactory.decodeFile(imageFilePath);
        status.bitmapCroped = convertBitmap(status.bitmapOri);
        log.loglnA("ic", "index", imageIndex, "bitmap", "end", StatisticsTime.TimeRecord.time());

        try {
            int[] imageSize = new int[] {status.bitmapCroped.getWidth(), status.bitmapCroped.getHeight()};
            log.loglnA("ic", "index", imageIndex, "bitmap.size", JJsonUtils.toJson(imageSize), "ic", "start", StatisticsTime.TimeRecord.time());
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
    protected void doObjectDetectionContinue() {
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
        Status status = new Status(scoreTopK);
        sendMsg(-1, status);
        timeRecord.taskTotal.setStart();
        for (int i=0; i<dataset.size(); i++) {
            if (isDestroyed() || isPreDestroyed()) {
                break;
            }
            doObjectDetectionContinue(i, status);//一张图片做一次检测
            sendMsg(i, status);
        }
        timeRecord.taskTotal.setEnd();
        sendMsg(dataset.size(), status);
        workThreadRunning = false;
        if (isPreDestroyed() && !isDestroyed()) {
            this.destroyed = true;
            doDestroy();
        }
    }
    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;
//        android.util.Log.i("hello-transpose",String.valueOf(transpose));
        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;
//        android.util.Log.i("hello-wid-hei",String.valueOf(inWidth)+" "+inHeight+" "+dstWidth+" " +dstHeight);
        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }
    @WorkerThread
    protected void doObjectDetectionContinue(int imageIndex, Status status) {
        StatisticsTime.TimeRecord.StartEndTime time = new StatisticsTime.TimeRecord.StartEndTime();
        timeRecord.images[imageIndex] = time;
        String imageFilePath = dataset.get(imageIndex);
        int target = dataset.getClassIndex(imageIndex);

        time.setStart();
        log.loglnA("ic", "index", imageIndex, "all", "start", time.start);

        log.loglnA("ic", "index", imageIndex, "bitmap", "start", StatisticsTime.TimeRecord.time());
        status.bitmapOri = BitmapFactory.decodeFile(imageFilePath);
        int previewWidth=status.bitmapOri.getWidth();
        int previewHeight=status.bitmapOri.getHeight();
        status.bitmapCroped=Bitmap.createBitmap(300,300, Bitmap.Config.ARGB_8888);
//        status.bitmapCroped = convertBitmap(status.bitmapOri);
        Matrix frameToCropTransform = getTransformationMatrix(
                previewWidth, previewHeight,
                300, 300,
                90, false);

        Matrix cropToFrameTransform = new Matrix();
        android.util.Log.i("hello-croptoframe",cropToFrameTransform.toShortString());
        frameToCropTransform.invert(cropToFrameTransform);
        Canvas canvas = new Canvas(status.bitmapCroped);
        canvas.drawBitmap(status.bitmapOri, frameToCropTransform, null);
        //todo:change the image-processing
        log.loglnA("ic", "index", imageIndex, "bitmap", "end", StatisticsTime.TimeRecord.time());

        try {
            int[] imageSize = new int[] {status.bitmapCroped.getWidth(), status.bitmapCroped.getHeight()};
            log.loglnA("ic", "index", imageIndex, "bitmap.size", JJsonUtils.toJson(imageSize), "ic", "start", StatisticsTime.TimeRecord.time());
            StatisticsScore statistics = doObjectDetectionContinue(status.bitmapCroped, target);

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

    @WorkerThread
    protected abstract StatisticsScore doObjectDetectionContinue(Bitmap bitmap, int target);
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
        public StatisticsScore.HitStatistic statistics;
        public Bitmap bitmapOri = null;
        public Bitmap bitmapCroped = null;

        public Status(int scoreTopK) {
            statistics = new StatisticsScore.HitStatistic(scoreTopK);
        }
    }
}
