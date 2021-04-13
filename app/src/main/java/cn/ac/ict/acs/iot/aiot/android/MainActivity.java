package cn.ac.ict.acs.iot.aiot.android;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.JUtil;
import com.github.labowenzi.commonj.log.Log;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cn.ac.ict.acs.iot.aiot.android.dataset.Dataset;
import cn.ac.ict.acs.iot.aiot.android.log.ALog;
import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.util.DialogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSION_RW_CAMERA = 2;
    public static final String[] PERMISSIONS_RW_CAMERA = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private TextView mResource;
    private TextView mDirModel;
    private TextView mDirDataset;
    private TextView mFramework;
    private TextView mQuant;
    private TextView mModel;
    private TextView mDataset;
    private TextView mDevice;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mkdirs();
        Log.setInstance(new ALog());
        resourceName = ImageClassifyActivity.RESOURCE_NAME_DEFAULT;
        modelI = getAiotModel(resourceName);
        datasetI = getAiotDataset(this, resourceName);
        setContentView(R.layout.activity_main);
        mResource = findViewById(R.id.tv_resource);
        mDirModel = findViewById(R.id.tv_dir_model);
        mDirDataset = findViewById(R.id.tv_dir_dataset);
        mFramework = findViewById(R.id.tv_framework);
        mQuant = findViewById(R.id.tv_quant);
        mModel = findViewById(R.id.tv_model);
        mDataset = findViewById(R.id.tv_dataset);
        mDevice = findViewById(R.id.tv_device);
        findViewById(R.id.btn_resource).setOnClickListener(view -> onSelectResource());
        findViewById(R.id.btn_dir_model).setOnClickListener(view -> onSelectDirModel());
        findViewById(R.id.btn_dir_dataset).setOnClickListener(view -> onSelectDirDataset());
        findViewById(R.id.btn_framework).setOnClickListener(view -> onSelectFramework());
        findViewById(R.id.btn_quant).setOnClickListener(View -> onSelectQuant());
        findViewById(R.id.btn_model).setOnClickListener(view -> onSelectModel());
        findViewById(R.id.btn_dataset).setOnClickListener(view -> onSelectDataset());
        findViewById(R.id.btn_device).setOnClickListener(view -> onSelectDevice());
        findViewById(R.id.btn_go).setOnClickListener(view -> onGo());
        dirModel = null;
        dirDataset = null;
        frameworkName = null;
        quantName = null;
        modelName = null;
        datasetName = null;
        deviceName = null;
        askPermission();
        initDirAndViews();
    }

    private void mkdirs() {
        String appDataDir = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + "aiot";
        JIoUtil.mkdir(appDataDir);
    }

    private void initDir() {
        if (!JUtil.isEmpty(modelI.getDirs())) {
            dirModel = modelI.getDirs()[0];
            modelI.setModelDir(dirModel);
        }
        if (!JUtil.isEmpty(datasetI.getDirs())) {
            dirDataset = datasetI.getDirs()[0];
            datasetI.setDatasetDir(dirDataset);
        }
        frameworkName = Model.FRAMEWORK_DEFAULT;
        quantName = "no";
        modelName = modelI.getDefaultModelName(frameworkName);
        datasetName = datasetI.getDefaultDatasetName();
        deviceName = Model.Device.getName(Model.Device.CPU);
    }
    private void initDirAndViews() {
        initDir();
        refreshResourceViews();
        refreshDirModelViews();
        refreshDirDatasetViews();
        refreshFrameworkViews();
        refreshModelViews();
        refreshDatasetViews();
        refreshDeviceViews();
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
                } else {
                    // reset models;
                    modelI = Model.resetInstance();
                    datasetI = Dataset.resetInstance(this);
                    initDirAndViews();
                }
                break;
            }
        }
    }

    public static String[] getAiotResources() {
        List<String> l = new LinkedList<>();
        l.add(ImageClassifyActivity.RESOURCE_NAME_DEFAULT);
        File downloadDir = new File(DownloadInfo.DIR_PATH);
        if (JIoUtil.canRead(downloadDir) && downloadDir.isDirectory()) {
            File[] resources = downloadDir.listFiles();
            if (!JUtil.isEmpty(resources)) {
                for (File resource : resources) {
                    if (JIoUtil.canRead(resource) && resource.isDirectory()) {
                        l.add(resource.getName());
                    }
                }
            }
        }
        String[] arr = new String[l.size()];
        arr = l.toArray(arr);
        return arr;
    }
    public static Dataset getAiotDataset(Context ctx, String resourceName) {
        if (JUtil.isEmpty(resourceName)) {
            return null;
        } else if (resourceName.equals(ImageClassifyActivity.RESOURCE_NAME_DEFAULT)) {
            return Dataset.getInstance(ctx);
        } else {
            return Dataset.getInstance(ctx, DownloadInfo.getDirPathDataset(resourceName));
        }
    }
    public static Model getAiotModel(String resourceName) {
        if (JUtil.isEmpty(resourceName)) {
            return null;
        } else if (resourceName.equals(ImageClassifyActivity.RESOURCE_NAME_DEFAULT)) {
            return Model.getInstance();
        } else {
            return Model.getInstance(DownloadInfo.getDirPathModels(resourceName));
        }
    }

    private void onSelect(String title, String[] items, DialogInterface.OnClickListener itemsL) {
        String negStr = "cancel";
        DialogInterface.OnClickListener negL = (dialog, which) -> dialog.dismiss();
        DialogUtil.dlgList(this, title, null, items, itemsL, null, null, negStr, negL).show();
    }
    private void onSelectResource() {
        String title = "resource";
        String[] items = getAiotResources();
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectResource(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }
    private void onSelectResource(int index) {
        resourceName = getAiotResources()[index];
        modelI = getAiotModel(resourceName);
        datasetI = getAiotDataset(this, resourceName);
        initDirAndViews();
    }
    private void onSelectDirModel() {
        String title = "model dirs";
        String[] items = modelI.getDirs();
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectDirModel(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }
    private void onSelectDirModel(int index) {
        dirModel = modelI.getDirs()[index];
        modelI.setModelDir(dirModel);
        modelName = modelI.getDefaultModelName(frameworkName);
        deviceName = Model.Device.getName(Model.getDefaultSupportedDevice(frameworkName));
        refreshDirModelViews();
        refreshModelViews();
        refreshDeviceViews();
    }
    private void onSelectDirDataset() {
        String title = "dataset dirs";
        String[] items = datasetI.getDirs();
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectDirDataset(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }
    private void onSelectDirDataset(int index) {
        dirDataset = datasetI.getDirs()[index];
        datasetI.setDatasetDir(dirDataset);
        datasetName = datasetI.getDefaultDatasetName();
        refreshDirDatasetViews();
        refreshDatasetViews();
    }
    private void onSelectFramework() {
        String title = "frameworks";
        String[] items = Model.FRAMEWORKS;
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectFramework(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }
    private void onSelectFramework(int index) {
        frameworkName = Model.FRAMEWORKS[index];
        modelName = modelI.getDefaultModelName(frameworkName);
        deviceName = Model.Device.getName(Model.getDefaultSupportedDevice(frameworkName));
        refreshFrameworkViews();
        refreshQuantViews();
        refreshModelViews();
        refreshDeviceViews();
    }

    private void onSelectQuant() {
        String title = "Quant";
        String[] items = Model.getSupportedQuant(frameworkName);
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectQuant(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }

    private void onSelectQuant(int index) {
        quantName = Model.getSupportedQuant(frameworkName)[index];
        modelName = modelI.getDefaultModelName(frameworkName);
        refreshQuantViews();
        refreshModelViews();
        refreshDeviceViews();
    }

    private void onSelectModel() {
        if (JUtil.isEmpty(frameworkName) || modelI.getModelDir() == null) {
            Util.showToast("no framework", this);
            return;
        }
        String title = "models";
        String[] items = modelI.getModelDir().getInfo(frameworkName).names;
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectModel(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }
    private void onSelectModel(int index) {
        modelName = modelI.getModelDir().getInfo(frameworkName).names[index];
        if (JUtil.isEmpty(modelName)) {
            Util.showToast("wrong model", this);
        }
        refreshModelViews();
    }
    private void onSelectDataset() {
        if (datasetI.getDatasetDir() == null) {
            Util.showToast("no framework", this);
            return;
        }
        String title = "datasets";
        String[] items = datasetI.getDatasetDir().getNames();
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectDataset(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }
    private void onSelectDataset(int index) {
        datasetName = datasetI.getDatasetDir().getNames()[index];
        if (JUtil.isEmpty(datasetName)) {
            Util.showToast("wrong dataset", this);
        }
        refreshDatasetViews();
    }
    private void onSelectDevice() {
        if (JUtil.isEmpty(frameworkName) || JUtil.isEmpty(modelName)) {
            Util.showToast("no framework or model", this);
            return;
        }
        String title = "devices";
        String[] items = Model.Device.getNames(Model.getSupportedDevices(frameworkName));
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectDevice(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }
    private void onSelectDevice(int index) {
        deviceName = Model.Device.getNames(Model.getSupportedDevices(frameworkName))[index];
        if (JUtil.isEmpty(deviceName)) {
            Util.showToast("wrong device", this);
        }
        refreshDeviceViews();
    }

    private void refreshResourceViews() {
        refreshTextViews(mResource, resourceName);
    }
    private void refreshDirModelViews() {
        refreshTextViews(mDirModel, dirModel);
    }
    private void refreshDirDatasetViews() {
        refreshTextViews(mDirDataset, dirDataset);
    }
    private void refreshFrameworkViews() {
        refreshTextViews(mFramework, frameworkName);
    }

    private void refreshQuantViews() {
        refreshTextViews(mQuant, quantName);
    }

    private void refreshModelViews() {
        refreshTextViews(mModel, modelName);
    }
    private void refreshDatasetViews() {
        refreshTextViews(mDataset, datasetName);
    }
    private void refreshDeviceViews() {
        refreshTextViews(mDevice, deviceName);
    }
    private void refreshTextViews(TextView view, String text) {
        view.setText(text);
    }

    private void onGo() {
        if (!JUtil.isEmpty(frameworkName)) {
            Intent intent = new Intent(MainActivity.this, ImageClassifyActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(ImageClassifyActivity.EXTRA_RESOURCE_NAME, resourceName);
            bundle.putString(ImageClassifyActivity.EXTRA_DIR_MODEL, dirModel);
            bundle.putString(ImageClassifyActivity.EXTRA_DIR_DATASET, dirDataset);
            bundle.putString(ImageClassifyActivity.EXTRA_FRAMEWORK_NAME, frameworkName);
            bundle.putString(ImageClassifyActivity.EXTRA_QUANT_NAME, quantName);
            bundle.putString(ImageClassifyActivity.EXTRA_MODEL_NAME, modelName);
            bundle.putString(ImageClassifyActivity.EXTRA_DATASET_NAME, datasetName);
            bundle.putString(ImageClassifyActivity.EXTRA_DEVICE_NAME, deviceName);
            intent.putExtras(bundle);
            startActivity(intent);
        } else {
            Util.showToast("no framework", this);
        }
    }
}
