package cn.ac.ict.acs.iot.aiot.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.labowenzi.commonj.JUtil;
import com.github.labowenzi.commonj.log.Log;

import java.lang.ref.WeakReference;

import cn.ac.ict.acs.iot.aiot.android.dataset.Dataset;
import cn.ac.ict.acs.iot.aiot.android.dataset.IDataset;
import cn.ac.ict.acs.iot.aiot.android.model.AbstractModel;
import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.statistics.StatisticsScore;
import cn.ac.ict.acs.iot.aiot.android.statistics.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 19-12-25.
 */
public class ImageClassifyActivity extends AppCompatActivity {
    public static final String TAG = "imageClassify";

    public static final String EXTRA_RESOURCE_NAME = "extra_resource_name";
    public static final String EXTRA_DIR_MODEL = "extra_dir_model";
    public static final String EXTRA_DIR_DATASET = "extra_dir_dataset";
    public static final String EXTRA_FRAMEWORK_NAME = "extra_framework_name";
    public static final String EXTRA_QUANT_NAME = "extra_quant_name";
    public static final String EXTRA_MODEL_NAME = "extra_model_name";
    public static final String EXTRA_DATASET_NAME = "extra_dataset_name";
    public static final String EXTRA_DEVICE_NAME = "extra_device_name";

    public static final String RESOURCE_NAME_DEFAULT = "app_resource_name_default";

    public static final int TIME_RECORD_TOP_K = 3;
    public static final int SCORE_TOP_K = 5;

    private Model modelI;
    private Dataset datasetI;

    private String resourceName;
    private String dirModel;
    private String dirDataset;
    private String frameworkName;
    private String quantName;
    private String modelName;
    private String datasetName;
    private String deviceName;

    private AbstractModel model = null;
    private StatisticsTime.TimeRecord timeRecord = null;

    private IDataset dataset = null;

    private LogUtil.Log log = null;

    private TextView mImageInfo;
    private TextView mResult;
    private TextView mTimeRecord;
    private LinearLayout mImages;

    private MHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new MHandler(this);
        Intent intent = getIntent();
        resourceName = intent.getStringExtra(EXTRA_RESOURCE_NAME);
        dirModel = intent.getStringExtra(EXTRA_DIR_MODEL);
        dirDataset = intent.getStringExtra(EXTRA_DIR_DATASET);
        frameworkName = intent.getStringExtra(EXTRA_FRAMEWORK_NAME);
        quantName = intent.getStringExtra(EXTRA_QUANT_NAME);
        modelName = intent.getStringExtra(EXTRA_MODEL_NAME);
        datasetName = intent.getStringExtra(EXTRA_DATASET_NAME);
        deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        if (JUtil.isEmpty(frameworkName)
                || JUtil.isEmpty(resourceName)
                || JUtil.isEmpty(dirModel)
                || JUtil.isEmpty(dirDataset)
                || JUtil.isEmpty(modelName)
                || JUtil.isEmpty(datasetName)
                || JUtil.isEmpty(deviceName)) {
            Util.showToast("wrong model, dataset or device", this);
            finish();
            return;
        }
        modelI = MainActivity.getAiotModel(resourceName);
        modelI.setModelDir(dirModel);
        datasetI = MainActivity.getAiotDataset(this, resourceName);
        datasetI.setDatasetDir(dirDataset);
        String fmd = frameworkName
                + '_' + quantName
                + '_' + modelName
                + '_' + datasetName
                + '_' + deviceName;
        log = LogUtil.Log.inLogDir("ic_" + fmd + ".log");
        log.logln("resource=" + resourceName);
        log.logln("dirModel=" + dirModel);
        log.logln("dirDataset=" + dirDataset);
        log.logln("framework=" + frameworkName);
        log.logln("quant=" + quantName);
        log.logln("model=" + modelName);
        log.logln("dataset=" + datasetName);
        log.logln("device=" + deviceName);
        if (!loadModel()) {
            finish();
            return;
        }
        if (!loadDataset()) {
            finish();
            return;
        }
        initViewById();
        setViewsByData();
        doImageClassification();
    }

    private boolean loadModel() {
        if (JUtil.isEmpty(frameworkName)) {
            Util.showToast("no framework", this);
            return false;
        }
        if (!JUtil.inArray(modelName, modelI.getModelDir().getInfo(frameworkName).names)) {
            Util.showToast(R.string.not_implemented, this);
            return false;
        }
        Model.Device device = Model.Device.get(deviceName);
        if (device == null) {
            Util.showToast("wrong device", this);
            return false;
        }
        model = modelI.getModel(this, log, frameworkName, quantName, modelName, device);
        if (model == null || !model.isStatusOk()) {
            Util.showToast("load model err", this);
            return false;
        }
        timeRecord = model.timeRecord;
        log.logln("load model: " + timeRecord.loadModel);
        return true;
    }

    private boolean loadDataset() {
        if (JUtil.inArray(datasetName, datasetI.getDatasetDir().getNames())) {
            timeRecord.loadDataset.setStart();
            dataset = datasetI.getDataset(datasetName);
            timeRecord.loadDataset.setEnd();
            log.logln("load dataset: " + timeRecord.loadDataset);
            if (!dataset.isStatusOk()) {
                Util.showToast("load dataset err", this);
                finish();
                return false;
            }
        } else {
            Util.showToast(R.string.not_implemented, this);
            finish();
            return false;
        }
        return true;
    }

    private void initViewById() {
        setContentView(R.layout.activity_image_classify);
        mImageInfo = findViewById(R.id.tv_image_info);
        mResult = findViewById(R.id.tv_result);
        mTimeRecord = findViewById(R.id.tv_time_record);
        mImages = findViewById(R.id.ll_images);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void setViewsByData() {
        StringBuilder infoSb = new StringBuilder();
        infoSb.append("framework: ").append(frameworkName);
        infoSb.append("\nquant:").append(quantName);
        infoSb.append("\nmodel: ").append(modelName);
        infoSb.append("\ndataset: ").append(datasetName);
        if (dataset != null) {
                infoSb.append(",  classes[").append(dataset.getClassesInfo().getSize()).append("]")
                    .append(", files[").append(dataset.size()).append("]");
        }
        infoSb.append("\ndevice: ").append(deviceName);
        mImageInfo.setText(infoSb.toString());
        mResult.setText(null);
        mTimeRecord.setText(null);
    }

    private void doImageClassification() {
        if (model != null) {
            model.handler = mHandler;
            model.what = AbstractModel.HANDLER_DO_IMAGE_CLASSIFICATION;
            model.scoreTopK = SCORE_TOP_K;
            if (dataset != null) {
                model.setDataset(dataset);
            } else {
                Util.showToast("no dataset", this);
                return;
            }
            model.doImageClassification();
        } else {
            Util.showToast("no model", this);
        }
    }
    private void onImageClassified(int process, Object obj) {
        AbstractModel.Status status = null;
        if (obj!= null && !(obj instanceof AbstractModel.Status)) {
            Log.e("image classified data", "null or wrong type");
        } else {
            status = (AbstractModel.Status) obj;
        }
        onImageClassified(process, status);
    }
    private void onImageClassified(int process, AbstractModel.Status status) {
        int processOri = process;
        boolean allDone = false;
        int total = dataset.size();
        if (process < 0) {
            process = 0;
        } else if (process >= total){
            allDone = true;
            process = total;
        } else {
            process += 1;
        }
        StringBuilder result = new StringBuilder("result:\n" + process + " / " + dataset.size());
        if (allDone) {
            result.append('\n').append("all done");
        }
        StatisticsScore.HitStatistic statistics = status == null ? null : status.statistics;
        if (statistics == null) {
            result.append("\nnull");
        } else {
            result.append("\ncount=").append(statistics.count);
            for (int i=0; i<statistics.topKMaxHitA.length; ++i) {
                result.append("\ntop[").append(i + 1).append("]=").append(statistics.topKMaxHitA[i]).append(';');
            }
        }
        String resultText = result.toString();
        timeRecord.calc(TIME_RECORD_TOP_K);
        String time = "time " + timeRecord;

        mResult.setText(resultText);
        mTimeRecord.setText(time);
        if (status != null) {
            if (0 <= processOri && processOri < dataset.size()) {
                Bitmap bm = status.bitmapOri;
                if (bm != null) {
                    addImages(bm);
                }
                bm = status.bitmapCroped;
                if (bm != null) {
                    addImages(bm);
                }
            }
        }
        if (allDone) {
            log.logln("all done");
            log.logln(resultText);
            log.logln(time);
        }
    }

    private void addImages(Bitmap bm) {
        int ivsCnt = mImages.getChildCount();
        if (ivsCnt >= 20) {
            return;
        }
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(400, 400);
        ImageView iv = new ImageView(this);
        iv.setPadding(16, 16, 16, 16);
        iv.setImageBitmap(bm);
        mImages.addView(iv, params);
    }

    @Override
    protected void onDestroy() {
        log.close();
        if (model != null) {
            model.destroy();
            model = null;
        }
        super.onDestroy();
    }

    private static class MHandler extends Handler {
        protected final WeakReference<ImageClassifyActivity> mTarget;

        public MHandler(ImageClassifyActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case AbstractModel.HANDLER_DO_IMAGE_CLASSIFICATION: {
                    onImageClassified(msg);
                    break;
                }
            }
        }

        private void onImageClassified(Message msg) {
            ImageClassifyActivity target = mTarget.get();
            if (target != null) {
                target.onImageClassified(msg.arg2, msg.obj);
            }
        }
    }
}
