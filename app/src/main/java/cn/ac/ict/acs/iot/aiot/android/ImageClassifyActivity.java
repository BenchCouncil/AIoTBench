package cn.ac.ict.acs.iot.aiot.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
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
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 19-12-25.
 */
public class ImageClassifyActivity extends AppCompatActivity {
    public static final String TAG = "imageClassify";

    public static final String EXTRA_FRAMEWORK_NAME = "extra_framework_name";
    public static final String EXTRA_MODEL_NAME = "extra_model_name";
    public static final String EXTRA_DATASET_NAME = "extra_dataset_name";

    private Model modelI;
    private Dataset datasetI;

    private String frameworkName;
    private String modelName;
    private String datasetName;

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
        modelI = Model.getInstance();
        datasetI = Dataset.getInstance(this);
        Intent intent = getIntent();
        frameworkName = intent.getStringExtra(EXTRA_FRAMEWORK_NAME);
        modelName = intent.getStringExtra(EXTRA_MODEL_NAME);
        datasetName = intent.getStringExtra(EXTRA_DATASET_NAME);
        if (JUtil.isEmpty(frameworkName)
                || JUtil.isEmpty(modelName)
                || JUtil.isEmpty(datasetName)) {
            Util.showToast("wrong model or dataset", this);
            finish();
            return;
        }
        String fmd = frameworkName
                + '_' + modelName
                + '_' + datasetName;
        log = LogUtil.Log.inLogDir("ic_" + fmd + ".log");
        log.logln("framework=" + frameworkName);
        log.logln("model=" + modelName);
        log.logln("dataset=" + datasetName);
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
        model = modelI.getModel(this, log, frameworkName, modelName);
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
    }

    private void setViewsByData() {
        if (dataset != null) {
            mImageInfo.setText(dataset.toString());
        }
        mResult.setText(null);
        mTimeRecord.setText(null);
    }

    private void doImageClassification() {
        if (model != null) {
            model.handler = mHandler;
            model.what = AbstractModel.HANDLER_DO_IMAGE_CLASSIFICATION;
            if (dataset != null) {
                model.dataset = dataset;
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
        timeRecord.calc();
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
        model.destroy();
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
