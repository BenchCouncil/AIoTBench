package cn.ac.ict.acs.iot.aiot.android.tflite;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

import cn.ac.ict.acs.iot.aiot.android.DatasetHelper;
import cn.ac.ict.acs.iot.aiot.android.ModelHelper;
import cn.ac.ict.acs.iot.aiot.android.R;
import cn.ac.ict.acs.iot.aiot.android.pytorch.PyTorchModels;
import cn.ac.ict.acs.iot.aiot.android.pytorch.PyTorchScoreStatistics;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 19-12-25.
 */
public class TfLiteImageClassify extends AppCompatActivity {
    public static final String TAG = "tfliteIc";

    public static final String EXTRA_MODEL_NAME = "extra_model_name";
    public static final String EXTRA_DATASET_NAME = "extra_dataset_name";

    private ModelHelper.Type modelType;
    private DatasetHelper.Type datasetType;

    private TfLiteModels.TfLiteModel model = null;
    private PyTorchModels.TimeRecord timeRecord = null;

    private DatasetHelper.IDataset dataset = null;

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
        modelType = ModelHelper.Type.get(intent.getStringExtra(EXTRA_MODEL_NAME));
        datasetType = DatasetHelper.Type.get(intent.getStringExtra(EXTRA_DATASET_NAME));
        if (modelType == null || datasetType == null) {
            Util.showToast("wrong model or dataset", this);
            finish();
            return;
        }
        log = LogUtil.Log.inLogDir("tflite.log");
        log.logln("model=" + modelType);
        log.logln("dataset=" + datasetType);
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
        if (modelType == ModelHelper.Type.E_MOBILE_NET_FLOAT
                || modelType == ModelHelper.Type.E_MOBILE_NET_QUANTIZED) {
            if (modelType == ModelHelper.Type.E_MOBILE_NET_FLOAT) {
                model = new TfLiteModels.MobileNetFloat(this, log);
            } else {
                model = new TfLiteModels.MobileNetQuantized(this, log);
            }
            if (!model.isStatusOk()) {
                Util.showToast("load model err", this);
                finish();
                return false;
            }
            timeRecord = model.timeRecord;
            log.logln("load model: " + timeRecord.loadModel);
        } else {
            Util.showToast(R.string.not_implemented, this);
            finish();
            return false;
        }
        return true;
    }

    private boolean loadDataset() {
        if (datasetType == DatasetHelper.Type.E_DEMO
                || datasetType == DatasetHelper.Type.E_IMAGENET_2_2
                || datasetType == DatasetHelper.Type.E_IMAGENET_10_50) {
            timeRecord.loadDataset.setStart();
            dataset = datasetType.getDataset(this);
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
        setContentView(R.layout.activity_tflite_image_classify);
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
            model.what = TfLiteModels.HANDLER_DO_IMAGE_CLASSIFICATION;
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
        PyTorchModels.Status status = null;
        if (obj!= null && !(obj instanceof PyTorchModels.Status)) {
            Log.e("image classified data", "null or wrong type");
        } else {
            status = (PyTorchModels.Status) obj;
        }
        onImageClassified(process, status);
    }
    private void onImageClassified(int process, PyTorchModels.Status status) {
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
        PyTorchScoreStatistics.HitStatistic statistics = status == null ? null : status.statistics;
        if (statistics == null) {
            result.append("\nnull");
        } else {
            result.append("\ncount=").append(statistics.count);
            for (int i=0; i<statistics.topKMaxHitA.length; ++i) {
                result.append("\ntop[").append(i + 1).append("]=").append(statistics.topKMaxHitA[i]).append(';');
            }
        }
        timeRecord.calc();
        String time = "time " + timeRecord;

        mResult.setText(result.toString());
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
        protected final WeakReference<TfLiteImageClassify> mTarget;

        public MHandler(TfLiteImageClassify target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case PyTorchModels.HANDLER_DO_IMAGE_CLASSIFICATION: {
                    onImageClassified(msg);
                    break;
                }
            }
        }

        private void onImageClassified(Message msg) {
            TfLiteImageClassify target = mTarget.get();
            if (target != null) {
                target.onImageClassified(msg.arg2, msg.obj);
            }
        }
    }
}
