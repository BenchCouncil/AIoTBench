package cn.ac.ict.acs.iot.aiot.android;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import cn.ac.ict.acs.iot.aiot.android.util.DialogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSION_RW_CAMERA = 2;
    public static final String[] PERMISSIONS_RW_CAMERA = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private TextView mFramework;
    private TextView mModel;
    private TextView mDataset;

    private FrameworkHelper.Type frameworkType;
    private ModelHelper.Type modelType;
    private DatasetHelper.Type datasetType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFramework = findViewById(R.id.tv_framework);
        mModel = findViewById(R.id.tv_model);
        mDataset = findViewById(R.id.tv_dataset);
        findViewById(R.id.btn_framework).setOnClickListener(view -> onSelectFramework());
        findViewById(R.id.btn_model).setOnClickListener(view -> onSelectModel());
        findViewById(R.id.btn_dataset).setOnClickListener(view -> onSelectDataset());
        findViewById(R.id.btn_go).setOnClickListener(view -> onGo());
        frameworkType = FrameworkHelper.Type.E_PY_TORCH;
        modelType = ModelHelper.Type.E_MOBILE_NET;
        datasetType = DatasetHelper.Type.E_DEMO;
        refreshFrameworkViews();
        refreshModelViews();
        refreshDatasetViews();
        askPermission();
    }

    private void askPermission() {
        String[] permissions = PERMISSIONS_RW_CAMERA;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                //未授权，申请授权(从相册选择图片需要读取存储卡的权限)
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_RW_CAMERA);
                return;
            }
        }
    }

    /**
     权限申请结果回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_RW_CAMERA: {  //拍照权限申请返回
                boolean allGrant = true;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allGrant = false;
                        break;
                    }
                }
                if (!allGrant) {
                    Toast.makeText(this, "need permissions!", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            }
        }
    }

    private void onSelect(String title, String[] items, DialogInterface.OnClickListener itemsL) {
        String negStr = "cancel";
        DialogInterface.OnClickListener negL = (dialog, which) -> dialog.dismiss();
        DialogUtil.dlgList(this, title, null, items, itemsL, null, null, negStr, negL).show();
    }
    private void onSelectFramework() {
        String title = "frameworks";
        String[] items = FrameworkHelper.Type.availableFrameworkStrings;
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectFramework(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }
    private void onSelectFramework(int index) {
        frameworkType = FrameworkHelper.Type.availableFrameworks[index];
        modelType = frameworkType.getAvailableModels()[0];
        datasetType = frameworkType.getAvailableDatasets()[0];
        refreshFrameworkViews();
        refreshModelViews();
        refreshDatasetViews();
    }
    private void onSelectModel() {
        if (frameworkType == null) {
            Util.showToast("no framework", this);
            return;
        }
        String title = "models";
        String[] items = frameworkType.getAvailableModelStrings();
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectModel(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }
    private void onSelectModel(int index) {
        ModelHelper.Type t = frameworkType.getAvailableModels()[index];
        if (t == null) {
            Util.showToast("wrong model", this);
        } else {
            modelType = t;
            refreshModelViews();
        }
    }
    private void onSelectDataset() {
        if (frameworkType == null) {
            Util.showToast("no framework", this);
            return;
        }
        String title = "datasets";
        String[] items = frameworkType.getAvailableDatasetStrings();
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectDataset(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }
    private void onSelectDataset(int index) {
        DatasetHelper.Type t = frameworkType.getAvailableDatasets()[index];
        if (t == null) {
            Util.showToast("wrong dataset", this);
        } else if (t == DatasetHelper.Type.E_USER_SELECTED_FILE) {
            // to select file from file system;
            Util.showToast(R.string.not_implemented, this);
        } else {
            datasetType = t;
            refreshDatasetViews();
        }
    }

    private void refreshFrameworkViews() {
        refreshTextViews(mFramework, frameworkType.getValue());
    }
    private void refreshModelViews() {
        refreshTextViews(mModel, modelType.getValue());
    }
    private void refreshDatasetViews() {
        refreshTextViews(mDataset, datasetType.getValue());
    }
    private void refreshTextViews(TextView view, String text) {
        view.setText(text);
    }

    private void onGo() {
        if (frameworkType == FrameworkHelper.Type.E_PY_TORCH
                || frameworkType == FrameworkHelper.Type.E_CAFFE_2
                || frameworkType == FrameworkHelper.Type.E_TENSORFLOW_LITE) {
            Intent intent = new Intent(MainActivity.this, ImageClassifyActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(ImageClassifyActivity.EXTRA_FRAMEWORK_NAME, frameworkType.getValue());
            bundle.putString(ImageClassifyActivity.EXTRA_MODEL_NAME, modelType.getValue());
            bundle.putString(ImageClassifyActivity.EXTRA_DATASET_NAME, datasetType.getValue());
            intent.putExtras(bundle);
            startActivity(intent);
        } else {
            Util.showToast("no framework", this);
        }
    }
}
