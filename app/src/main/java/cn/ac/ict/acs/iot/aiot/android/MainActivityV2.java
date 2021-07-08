package cn.ac.ict.acs.iot.aiot.android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.log.Log;

import cn.ac.ict.acs.iot.aiot.android.dataset.Dataset;
import cn.ac.ict.acs.iot.aiot.android.log.ALog;
import cn.ac.ict.acs.iot.aiot.android.model.Model;

/**
 * Created by alanubu on 20-11-5.
 */
public class MainActivityV2 extends AppCompatActivity {
    /*
     * 四个按钮
     * 测试说明
     * 资源下载
     * 开始测试
     * 测试结果
     * 入口页边角增加调试按钮，进入旧版单个负载执行的页面
     */

    public static final int PERMISSION_RW_CAMERA = 2;
    public static final String[] PERMISSIONS_RW_CAMERA = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };

    private Model modelI;
    private Dataset datasetI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {//todo:上传手机名称
        super.onCreate(savedInstanceState);
        mkdirs();
        Log.setInstance(new ALog());
        modelI = Model.getInstance();
        datasetI = Dataset.getInstance(this);
        setContentView(R.layout.activity_main_v2);
        findViewById(R.id.btn_note).setOnClickListener(view -> onNote());
        findViewById(R.id.btn_download).setOnClickListener(view -> onDownload());
        findViewById(R.id.btn_image_classify).setOnClickListener(view -> onImageClassify());
        findViewById(R.id.btn_quant_classify).setOnClickListener(view -> onQuantClassify());
        findViewById(R.id.btn_result_list).setOnClickListener(view -> onResultList());
//        findViewById(R.id.btn_block_go).setOnClickListener(view -> onBlockGo());
        findViewById(R.id.btn_old_debug).setOnClickListener(view -> onOldDebug());
        findViewById(R.id.btn_object_detection).setOnClickListener(view -> onObjectDetection());
        askPermission();
    }

    private void mkdirs() {
        String appDataDir = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + "aiot";
        JIoUtil.mkdir(appDataDir);
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
     * 权限申请结果回调
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
                }
                break;
            }
        }
    }

    private void onBtn(Class clz) {
        Intent intent = new Intent(MainActivityV2.this, clz);
        Bundle bundle = new Bundle();
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void onNote() {
        onBtn(NoteActivity.class);
    }

    private void onDownload() {
        onBtn(DownloadActivity.class);
    }

    private void onImageClassify() {
        onBtn(ImageClassifyActivityV2.class);
    }

    private void onResultList() {
        onBtn(ResultListActivity.class);
    }
    private void onBlockGo() {
        onBtn(blockTestActivity.class);
    }

    private void onQuantClassify() {
        onBtn(QuantClassifyActivity.class);
    }

    private void onOldDebug() {
        onBtn(MainActivity.class);
    }
    private void onObjectDetection() {
        onBtn(ObjectDetectionActivity.class);
    }
}

