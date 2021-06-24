package cn.ac.ict.acs.iot.aiot.android;

import androidx.appcompat.app.AppCompatActivity;
import cn.ac.ict.acs.iot.aiot.android.dataset.Dataset;
import cn.ac.ict.acs.iot.aiot.android.dataset.IDataset;
import cn.ac.ict.acs.iot.aiot.android.model.AbstractModel;
import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.statistics.StatisticsScore;
import cn.ac.ict.acs.iot.aiot.android.statistics.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.util.DialogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.labowenzi.commonj.JJsonUtils;
import com.github.labowenzi.commonj.JTimeUtil;
import com.github.labowenzi.commonj.JUtil;
import com.github.labowenzi.commonj.log.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class QuantClassifyActivity extends AppCompatActivity {
    public static final String TAG = "quantClassify";

    public static final int TIME_RECORD_TOP_K = ImageClassifyActivity.TIME_RECORD_TOP_K;
    public static final int SCORE_TOP_K = ImageClassifyActivity.SCORE_TOP_K;

    private Button mBtnGo;

    private TvInfoGlobal mTvInfoGlobal;
    private TvInfoCurr mTvInfoCurr;
    private TvLog mTvLog;


    private String resourceName;
    private Model modelI;
    private Dataset datasetI;
    private IDataset dataset = null;
    private WorkLoadStatus[] statuses;
    private StatisticsTime.TimeRecord.StartEndTime timeRecordLoadDataset;
    private LogUtil.Log log = null;

    private boolean stopRunning = false;
    //note:new
    private String log_path;
    private String result_filepath;
    private String procedure_filepath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quant);
//        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                .detectDiskReads()
//                .detectDiskWrites()
//                .detectNetwork()   // or .detectAll() for all detectable problems
//                .penaltyLog()
//                .build());
//        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                .detectLeakedSqlLiteObjects()
//                .penaltyLog()
//                .penaltyDeath()
//                .build());
        log_path = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + "aiot/log";
        mTvInfoGlobal = new TvInfoGlobal(findViewById(R.id.tv_quant_info_global));
        mTvInfoCurr = new TvInfoCurr(findViewById(R.id.tv_quant_info_curr));
        mTvLog = new TvLog(findViewById(R.id.tv_quant_log));
        mBtnGo = findViewById(R.id.btn_quant_go);
        findViewById(R.id.btn_quant_back).setOnClickListener(view -> onBackPressed());
        mBtnGo.setClickable(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTvInfoGlobal.init();
        initProgramLog();

        mTvInfoGlobal.composeText();
        mTvInfoGlobal.refreshTextView();
        mTvInfoCurr.composeText();
        mTvInfoCurr.refreshTextView();
        mTvLog.composeText();
        mTvLog.refreshTextView();


        DownloadInfo.ResourceOne resource = DownloadInfo.Resource.getInstance().getOne();
        if (resource == null) {
            resourceName = null;
        } else {
            resourceName = resource.name;
        }
        loadDataset();
        loadWorkloads();
        mBtnGo.setOnClickListener(view -> onGo());
        mBtnGo.setClickable(true);
    }

    private void loadDataset() {
        if (JUtil.isEmpty(resourceName)) {
            dataset = null;
            return;
        }
        datasetI = Dataset.getInstance(this, DownloadInfo.getDirPathDataset(resourceName));//note:dataset暂时不用变
        if (!JUtil.isEmpty(datasetI.getDirs())) {
            datasetI.setDatasetDir(datasetI.getDirs()[0]);
        }
        String datasetName;
        String[] datasetNames = datasetI.getDatasetDir().getNames();
        if (JUtil.isEmpty(datasetNames)) {
            datasetName = null;
        } else if (datasetNames.length == 1) {
            datasetName = datasetNames[0];
        } else {
            datasetName = datasetNames[1];  // not demo;
        }
        if (JUtil.isEmpty(datasetName)) {
            dataset = null;
            return;
        }
        timeRecordLoadDataset = new StatisticsTime.TimeRecord.StartEndTime();
        timeRecordLoadDataset.setStart();
        dataset = datasetI.getDataset(datasetName);
        timeRecordLoadDataset.setEnd();
        log.logln("load dataset: " + timeRecordLoadDataset);
        if (!dataset.isStatusOk()) {
            Util.showToast("load dataset err", this);
            dataset = null;
            return;
        }
    }

    private boolean isDatasetOk() {
        return dataset != null;
    }

    private void loadWorkloads() {
        if (!isDatasetOk()) {
            return;
        }
        if (JUtil.isEmpty(resourceName)) {
            return;
        }
        modelI = Model.getInstance(DownloadInfo.getDirPathModels(resourceName));
        if (!JUtil.isEmpty(modelI.getDirs())) {
            modelI.setModelDir(modelI.getDirs()[0]);
        }
        LinkedList<WorkLoadStatus> ll = new LinkedList<>();
        String quants[] = Model.getSupportedQuant("tflite");
        for (int i = 1; i < quants.length; i++) {
            String quantName = quants[i];
            for (String modelName : modelI.getModelDir().getInfo("tflite").names) {
                for (String deviceName : Model.Device.getNames(Model.getSupportedDevices("tflite"))) {
                    // todo: maybe from config file;
                    WorkLoadStatus status = new WorkLoadStatus();
                    status.framework = "tflite";
                    status.quant = quantName;
                    status.model = modelName;
                    status.device = deviceName;
                    status.name = "tflite" + '_' + quantName + "_" + deviceName + '_' + modelName;  // todo: maybe from config file;
                    status.imgTotal = dataset.size();
                    ll.add(status);
                }
            }
        }
//        for (int i = 1; i < quants.length - 2; i++) {
//            String quantName = quants[i];
//            String modelName = modelI.getModelDir().getInfo("tflite").names[0];
//            String deviceName = Model.Device.getNames(Model.getSupportedDevices("tflite"))[0];
//            // todo: maybe from config file;
//            WorkLoadStatus status = new WorkLoadStatus();
//            status.framework = "tflite";
//            status.quant = quantName;
//            status.model = modelName;
//            status.device = deviceName;
//            status.name = "tflite" + '_' + quantName + "_" + deviceName + '_' + modelName;  // todo: maybe from config file;
//            status.imgTotal = dataset.size();
//            ll.add(status);
//        }
        statuses = new WorkLoadStatus[ll.size()];
        statuses = ll.toArray(statuses);
        mTvInfoGlobal.workloadsTotal = statuses.length;
    }

    private boolean isWorkloadsOk() {
        return !JUtil.isEmpty(statuses);
    }

    @Override
    public void onBackPressed() {
        if (isRunning()) {
            String title = "是否退出？";
            String msg = "是否结束测试并退出？";
            String posStr = "结束测试并退出";
            DialogInterface.OnClickListener posL = (dialog, which) -> {
                stopRunning();
                super.onBackPressed();
                dialog.dismiss();
            };
            String negStr = "取消";
            DialogInterface.OnClickListener negL = (dialog, which) -> dialog.dismiss();
            DialogUtil.dlg2(QuantClassifyActivity.this, title, msg, posStr, posL, negStr, negL).show();
        } else {
            super.onBackPressed();
        }
    }

    private void onGo() {
        if (isDatasetOk() && isWorkloadsOk()) {
            startRunning();
            mBtnGo.setClickable(false);
        } else {
            String title = "数据未下载";
            String msg = "所需数据未下载或下载中，请从首页进入下载页等待下载完成";
            String btnStr = "确定";
            DialogInterface.OnClickListener btnL = (dialog, which) -> dialog.dismiss();
            DialogUtil.dlg1(QuantClassifyActivity.this, title, msg, btnStr, btnL).show();
        }
    }

    private boolean isRunning() {
        if (JUtil.isEmpty(statuses)) {
            return false;
        }
        for (WorkLoadStatus status : statuses) {
            if (status.isRunning()) {
                return true;
            }
        }
        return false;
    }

    private void startRunning() {
        if (stopRunning) {
            return;
        }
        mTvInfoGlobal.startTime = System.currentTimeMillis();
        runOne(0);
    }

    private void runOne(int statusIndex) {
        if (statusIndex < 0 || statusIndex >= statuses.length) {
            mTvInfoGlobal.endTime = System.currentTimeMillis();
            if (statusIndex == statuses.length) {
                mTvInfoGlobal.workloadsCurr = statusIndex;
                mTvInfoGlobal.scoreTextTillNow = scoreText(0, statusIndex);
                mTvInfoGlobal.composeText();
                mTvInfoGlobal.refreshTextView();
            }
            outputResultLog();
            Util.showToast("all done", this);
            final AlertDialog alertDialog = new AlertDialog.Builder(QuantClassifyActivity.this).create();
            alertDialog.show();
            /* 添加对话框自定义布局 */
            alertDialog.setContentView(R.layout.activity_popup_upload_windows);
            /* 获取对话框窗口 */
            Window window = alertDialog.getWindow();
            /* 设置显示窗口的宽高 */
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            /* 设置窗口显示位置 */
            window.setGravity(Gravity.CENTER);
            /* 通过window找布局里的控件 */
            window.findViewById(R.id.upload_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 隐藏对话框
                    alertDialog.dismiss();

                }
            });
            window.findViewById(R.id.upload_ok).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 隐藏对话框
                    Button ok = window.findViewById(R.id.upload_ok);
                    ok.setClickable(false);
                    ok.setTextColor(0xff828382);
//                    TextView tv = window.findViewById(R.id.upload_description);
//                    tv.setText("please wait until the window close automatically");
                    Button cancel = window.findViewById(R.id.upload_cancel);
                    cancel.setClickable(false);
                    cancel.setTextColor(0xff828382);
                    Thread st = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Socket socket = new Socket("192.168.122.72", 3456);
                                DataInputStream in = new DataInputStream(socket.getInputStream());
                                File file1 = new File(result_filepath);
                                System.out.println("文件大小：" + file1.length() + "B");

                                DataInputStream dis = new DataInputStream(new FileInputStream(result_filepath));
                                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                                dos.writeUTF(file1.getName());
                                dos.writeLong(file1.length());
                                byte[] buf = new byte[1024 * 9];
                                int len = 0;
                                while ((len = dis.read(buf)) != -1) {
                                    dos.write(buf, 0, len);
                                    System.out.println(len);
                                    dos.flush();
                                }
                                System.out.println("文件1上传结束，，，，");
                                String flag = in.readUTF();
                                System.out.println(flag);
                                File file2 = new File(procedure_filepath);
                                System.out.println("文件大小：" + file2.length() + "B");

                                dis = new DataInputStream(new FileInputStream(procedure_filepath));
                                dos = new DataOutputStream(socket.getOutputStream());
                                dos.writeUTF(file2.getName());
                                dos.writeLong(file2.length());
                                len = 0;
                                while ((len = dis.read(buf)) != -1) {
                                    dos.write(buf, 0, len);
                                    dos.flush();
                                }
                                System.out.println("文件2上传结束，，，，");
                                dis.close();
                                dos.close();
                                alertDialog.dismiss();
//                                Looper.prepare();
//                                Toast.makeText(QuantClassifyActivity.this, "upload done, thanks for your waiting!", Toast.LENGTH_LONG).show();
//                                Looper.loop();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                alertDialog.dismiss();
                            } catch (IOException e) {
                                e.printStackTrace();
                                alertDialog.dismiss();
                            }
                        }
                    });
//                    sendThread st = new sendThread();
                    st.start();
                    try {
                        st.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

//                    alertDialog.dismiss();
                    Toast.makeText(QuantClassifyActivity.this, "upload done, thanks for your waiting!", Toast.LENGTH_LONG+Toast.LENGTH_LONG).show();
//                    //自己进行其他的处理
//                    // 1.加载JDBC驱动
//                    try {
//                        Class.forName("com.mysql.jdbc.Driver");
//                        Log.v(TAG, "加载JDBC驱动成功");
//                    } catch (ClassNotFoundException e) {
//                        Log.e(TAG, "加载JDBC驱动失败");
//                        return;
//                    }
//                    // 2.设置好IP/端口/数据库名/用户名/密码等必要的连接信息
////                    String ip = "218.194.38.231";
////                    int port = 3306;
////                    String dbName = "aiot";
////                    String url = "jdbc:mysql://" + ip + ":" + port
////                            + "/" + dbName; // 构建连接mysql的字符串
////                    String user = "root";
////                    String password = "199942439z";
//                    String driver = "com.mysql.jdbc.Driver";//MySQL 驱动
//                    String url = "jdbc:mysql://121.48.163.88:3306/db2";//MYSQL数据库连接Url
//                    String user = "root";//用户名
//                    String password = "123456";//密码
//                    try {
//                        Class.forName(driver);//获取MYSQL驱动
//                        Connection conn = (Connection) DriverManager.getConnection(url, user, password);//获取连接
//                        Log.i(TAG,"chenggong");
//                    } catch (ClassNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    }
                }
            });
            return;
        }
        if (stopRunning) {
            return;
        }
        mTvInfoGlobal.workloadsCurr = statusIndex;
        mTvInfoGlobal.scoreTextTillNow = scoreText(0, statusIndex);
        mTvInfoGlobal.composeText();
        mTvInfoGlobal.refreshTextView();
        WorkLoadStatus status = statuses[statusIndex];
        mTvInfoCurr.status = status;
        String frameworkName = status.framework;
        if (JUtil.isEmpty(frameworkName)) {
            Util.showToast("no framework", this);
            return;
        }
        String modelName = status.model;
        if (!JUtil.inArray(modelName, modelI.getModelDir().getInfo(frameworkName).names)) {
            Util.showToast(R.string.not_implemented, this);
            return;
        }
        String deviceName = status.device;
        Model.Device device = Model.Device.get(deviceName);
        if (device == null) {
            Util.showToast("wrong device", this);
            return;
        }
        AbstractModel model = modelI.getModel(this, log, frameworkName, status.quant, modelName, device);
        if (model == null || !model.isStatusOk()) {
            Util.showToast("load model err", this);
            return;
        }
        StatisticsTime.TimeRecord timeRecord = model.timeRecord;
        log.logln("load model: " + timeRecord.loadModel);
        timeRecord.loadDataset = timeRecordLoadDataset;
        status.timeRecord = timeRecord;
        status.startTime = System.currentTimeMillis();
        runOneModel(statusIndex, status, model);
    }

    private void runOneModel(int statusIndex, WorkLoadStatus status, AbstractModel model) {
        model.handler = new MHandler(this, model, statusIndex, status);
        model.what = AbstractModel.HANDLER_DO_IMAGE_CLASSIFICATION;
        model.scoreTopK = SCORE_TOP_K;
        if (dataset != null) {
            model.setDataset(dataset);
        } else {
            Util.showToast("no dataset", this);
            return;
        }
        log.logln("start workload " + statusIndex + " name=" + status.name);
        model.doImageClassification();
    }

    private void onImageClassified(AbstractModel model, int process, Object obj, int statusIndex, WorkLoadStatus workLoadStatus) {
        AbstractModel.Status status = null;
        if (obj != null && !(obj instanceof AbstractModel.Status)) {
            Log.e("image classified data", "null or wrong type");
        } else {
            status = (AbstractModel.Status) obj;
        }
        onImageClassified(model, process, status, statusIndex, workLoadStatus);
    }

    private void onImageClassified(AbstractModel model, int process, AbstractModel.Status status, int statusIndex, WorkLoadStatus workLoadStatus) {
        int processOri = process;
        boolean allDone = false;
        int total = dataset.size();
        if (process < 0) {
            process = 0;
        } else if (process >= total) {
            allDone = true;
            process = total;
        } else {
            process += 1;
        }
        workLoadStatus.imgCurr = process;
        StatisticsScore.HitStatistic statistics = status == null ? null : status.statistics;
        if (statistics != null) {
            workLoadStatus.imgCorrect = statistics.topKMaxHit[0];  // top1 correct;
        }
        StatisticsTime.TimeRecord timeRecord = workLoadStatus.timeRecord;
        timeRecord.calc(TIME_RECORD_TOP_K);
        workLoadStatus.timeLoad = timeRecord.loadModel.diff();
        workLoadStatus.timeFirstImg = (long) timeRecord.statistics.firstTime;
        mTvInfoCurr.composeText();
        mTvInfoCurr.refreshTextView();
        if (allDone) {
            log.logln("all done for workload " + statusIndex + " name=" + workLoadStatus.name);
            if (model != null) {
                model.destroy();
            }
            workLoadStatus.endTime = System.currentTimeMillis();
            runOne(statusIndex + 1);

//            Util.showToast("upload done", this);


        }
        if (stopRunning) {
            if (model != null) {
                model.destroy();
            }
        }
    }

//    private void sendFile(String result_filepath, String procedure_filepath) {
//        try {
//            Socket socket = new Socket("192.168.0.105", 3456);
//            File file1 = new File(result_filepath);
//            System.out.println("文件大小：" + file1.length() + "kb");
//
//            DataInputStream dis = new DataInputStream(new FileInputStream(result_filepath));
//            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//            dos.writeUTF(file1.getName());
//            byte[] buf = new byte[1024 * 9];
//            int len = 0;
//            while ((len = dis.read(buf)) != -1) {
//                dos.write(buf, 0, len);
//                dos.flush();
//            }
//            System.out.println("文件1上传结束，，，，");
//            dis.close();
//            dos.close();
//
//            File file2 = new File(procedure_filepath);
//            System.out.println("文件大小：" + file2.length() + "kb");
//
//            dis = new DataInputStream(new FileInputStream(procedure_filepath));
//            dos = new DataOutputStream(socket.getOutputStream());
//            dos.writeUTF(file2.getName());
//            len = 0;
//            while ((len = dis.read(buf)) != -1) {
//                dos.write(buf, 0, len);
//                dos.flush();
//            }
//            System.out.println("文件2上传结束，，，，");
//            dis.close();
//            dos.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }

    public static String logName(Context ctx) {
        String deviceName = Build.MODEL;
        long deviceRamTotal = Util.getTotalRamSize(ctx);
        SimpleDateFormat simDateF = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String dateText = simDateF.format(new Date());
        String ramGB = String.valueOf(deviceRamTotal / (1000000000));
        return deviceName + '_' + ramGB + '_' + dateText;
    }

    private int RamInteger(long ramsize) {
        int res = (int) (ramsize / (long) 1000000000);
        if (res % 2 == 1) {
            res = res + 1;
        }
        return res;
    }

    private void outputResultLog() {
        String fileName = logName(this);
        result_filepath = log_path + '/' + fileName;//note:new
        LogUtil.Log log = LogUtil.Log.inLogDir(fileName);
        log.logln("device:" + mTvInfoGlobal.deviceName);
        log.logln("ram size:" + RamInteger(mTvInfoGlobal.deviceRamTotal));
        log.logln("ram available size:" + mTvInfoGlobal.deviceRamAvailable);
        log.logln("device fingerprint:" + mTvInfoGlobal.deviceFingerprint);
        log.logln("device hardware:" + mTvInfoGlobal.hardware);
        log.logln("device sdk int:" + mTvInfoGlobal.sdkInt);
        log.logln("resource name:" + resourceName);
        log.logln("image count:" + dataset.size());
        log.logln("time total:" + (mTvInfoGlobal.endTime - mTvInfoGlobal.startTime) + "ms");
        log.logln("vips:" + vips(statuses, this.log));
        log.logln("vops:" + vops(statuses, this.log));
        for (WorkLoadStatus status : statuses) {
            log.logln("\n");
            log.logln("workload");
            log.logln("name:" + status.name);
            log.logln("framework:" + status.framework);
            log.logln("framework:" + status.quant);
            log.logln("device:" + status.device);
            log.logln("model:" + status.model);
            log.logln("time_load:" + status.timeLoad + "ms");
            log.logln("time_first_img:" + status.timeFirstImg + "ms");
            log.logln("time_total:" + (status.endTime - status.startTime) + "ms");
            log.logln("time_avg:" + status.avgTime() + "ms");
            log.logln("time_sd:" + status.timeSd() + "ms");
            log.logln("accuracy:" + status.accuracy());
        }
    }

    private void initProgramLog() {
        String fileName = "program_log_" + logName(this) + ".log";
        procedure_filepath = log_path + '/' + fileName;//note:new
        log = LogUtil.Log.inLogDir(fileName);
    }

    private void stopRunning() {
        if (stopRunning) {
            return;
        }
        stopRunning = true;
    }

    private String scoreText(int start, int end) {
        int cnt = end - start;
        if (cnt <= 0) {
            return "";
        }
        WorkLoadStatus[] statuses = new WorkLoadStatus[cnt];
        System.arraycopy(this.statuses, start, statuses, 0, cnt);
        return "vips=" + vips(statuses, log) + ", vops=" + vops(statuses, log);
    }

    public static double vips(WorkLoadStatus[] statuses, LogUtil.Log log) {
        if (JUtil.isEmpty(statuses)) {
            return 0;
        }
        double[] accuracies = new double[statuses.length];
        double[] times = new double[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            WorkLoadStatus status = statuses[i];
            accuracies[i] = status.accuracy();
            times[i] = status.avgTime() / 1000.0;
        }
        return vips(accuracies, times, log);
    }

    public static double vips(double[] accuracies, double[] times, LogUtil.Log log) {
        double sum = 0;
        for (int i = 0; i < accuracies.length; i++) {
            sum += accuracies[i] / times[i];
        }
//        double res = sum / accuracies.length;
        double res = sum;
        if (log != null) {
            String text = "vips=" + res;
            text += "  accuracy=" + JJsonUtils.toJson(accuracies);
            text += "  time=" + JJsonUtils.toJson(times);
            log.logln(text);
        }
        return res;
    }

    public static double vops(WorkLoadStatus[] statuses, LogUtil.Log log) {
        if (JUtil.isEmpty(statuses)) {
            return 0;
        }
        double[] accuracies = new double[statuses.length];
        long[] flops = new long[statuses.length];
        double[] times = new double[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            WorkLoadStatus status = statuses[i];
            accuracies[i] = status.accuracy();
            times[i] = status.avgTime() / 1000.0;
            flops[i] = flopsFromModel(status.model);
            if (log != null) {
                log.logln("flops=" + flops[i] + "  model=" + status.model);
            }
        }
        return vops(accuracies, flops, times, log);
    }

    public static double vops(double[] accuracies, long[] flops, double[] times, LogUtil.Log log) {
        double sum = 0;
        for (int i = 0; i < accuracies.length; i++) {
            sum += accuracies[i] * flops[i] / times[i];
        }
//        double res = sum / accuracies.length;
        double res = sum;
        if (log != null) {
            String text = "vops=" + res;
            text += "  accuracy=" + JJsonUtils.toJson(accuracies);
            text += "  flops=" + JJsonUtils.toJson(flops);
            text += "  time=" + JJsonUtils.toJson(times);
            log.logln(text);
        }
        return res;
    }

    public static final String[] FLOPS_MODEL_NAME = {
            // todo: names from model name;
            "resnet50",
            "inception_v3",
            "densenet121",
            "squeezenet",
            "mobilenet_v2",
            "mnasnet",
    };
    public static final long[] FLOPS_FLOPS = {
            3800,
            5000,
            2800,
            833,
            300,
            315,
    };

    public static long flopsFromModel(String modelName) {
        if (JUtil.isEmpty(modelName)) {
            return 1;
        }
        int index = -1;
        for (int i = 0; i < FLOPS_MODEL_NAME.length; i++) {
            if (modelName.equalsIgnoreCase(FLOPS_MODEL_NAME[i])) {
                index = i;
                break;
            }
        }
        return index < 0 ? 1 : FLOPS_FLOPS[index];
    }

    private class TvData {
        public TextView mTv;

        protected String textCached;

        public TvData(TextView mTv) {
            this.mTv = mTv;
        }

        public void composeText() {
            textCached = "";
        }

        public void refreshTextView() {
            mTv.setText(textCached);
        }
    }

    private class TvInfoGlobal extends TvData {
        public String deviceFingerprint;
        public String hardware;
        public long sdkInt;
        public String deviceName;  // eg: xiaomi10
        public long deviceRamTotal;  // eg: 64GB
        public long deviceRamAvailable;  // eg: 64GB

        public String resourceName;  // eg: 20201110_model_8_dataset_2x2

        public int workloadsCurr;
        public int workloadsTotal;

        public long startTime;
        public long endTime;

        public String scoreTextTillNow;

        public TvInfoGlobal(TextView mTv) {
            super(mTv);
            startTime = -1;
            endTime = -1;
        }

        public void init() {
            QuantClassifyActivity ctx = QuantClassifyActivity.this;
            deviceFingerprint = Build.FINGERPRINT;
            hardware = Build.HARDWARE;
            sdkInt = Build.VERSION.SDK_INT;
            deviceName = Build.MODEL;
            deviceRamTotal = Util.getTotalRamSize(ctx);
            deviceRamAvailable = Util.getAvailableMemory(ctx);
            resourceName = ctx.resourceName;
        }

        @Override
        public void composeText() {
            StringBuilder s = new StringBuilder();
            s.append("device name: ").append(deviceName);
            s.append("\ndevice ram total: ").append(deviceRamTotal);
            s.append("\ndevice ram available: ").append(deviceRamAvailable);
            s.append("\ndevice fingerprint:").append(mTvInfoGlobal.deviceFingerprint);
            s.append("\ndevice hardware:").append(mTvInfoGlobal.hardware);
            s.append("\ndevice sdk int:").append(mTvInfoGlobal.sdkInt);
            s.append("\nresource name: ").append(resourceName);
            s.append("\nworkload done: ").append(workloadsCurr).append('/').append(workloadsTotal);
            long t = (endTime < 0 ? System.currentTimeMillis() : endTime) - startTime;
            String text = JTimeUtil.timeElapsedHours(t);
            s.append("\ntime used for ").append(workloadsCurr).append(" workload(s): ").append(text);
            s.append("\nscore for ").append(workloadsCurr).append(" workload(s): ").append(scoreTextTillNow);
            textCached = s.toString();
        }
    }

    private class TvInfoCurr extends TvData {
        public WorkLoadStatus status;

        public TvInfoCurr(TextView mTv) {
            super(mTv);
        }

        @Override
        public void composeText() {
            if (status == null) {
                textCached = "";
                return;
            }
            StringBuilder s = new StringBuilder();
            s.append("workload name: ").append(status.name);
            s.append("\nimg done: ").append(status.imgCurr).append('/').append(status.imgTotal);
            long t = status.timeUsed();
            String text = JTimeUtil.timeElapsedHours(t);
            s.append("\ntime: ").append(text);
            s.append("\navg time for ").append(status.imgCurr).append(" img(s): ").append(status.avgTimeText());//timeRecord.statistics
            s.append("\nsd time for ").append(status.imgCurr).append(" img(s): ").append(status.timeSdText());
            s.append("\naccuracy for ").append(status.imgCurr).append(" img(s): ").append(status.accuracyText());
//            s.append("\n").append(status.timeRecord);
            textCached = s.toString();
        }
    }

    private class TvLog extends TvData {
        public TvLog(TextView mTv) {
            super(mTv);
            textCached = "";
        }

        @Override
        public void composeText() {
        }

        public void addStatus(WorkLoadStatus status) {
            StringBuilder s = new StringBuilder();
            if (!JUtil.isEmpty(textCached)) {
                s.append(textCached).append("\n\n");
            }
            s.append("workload name: ").append(status.name);
            s.append("\ntime: ").append(status.timeUsedMs());
            s.append("\navg time: ").append(status.avgTimeText());
            s.append("\nsd time: ").append(status.timeSdText());
            s.append("\naccuracy: ").append(status.accuracyText());
            textCached = s.toString();
        }
    }

    private static class WorkLoadStatus {
        /*
         * 测试负载
         * tflite-cpu-res50
         * tflite-cpu-inception_v3
         * tflite-cpu-densenet121
         * tflite-cpu-squeezenet
         * tflite-cpu-mobilenet_v2
         * tflite-cpu-mnasnet
         * tflite-nnapi-res50
         * tflite-nnapi-inception_v3
         * tflite-nnapi-densenet121
         * tflite-nnapi-squeezenet
         * tflite-nnapi-mobilenet_v2
         * tflite-nnapi-mnasnet
         */
        public String name;  // eg: tflite-cpu-res50
        public String framework;  // eg: tflite
        public String quant; //eg: dr/ f16/ int
        public String device;  // eg: cpu
        public String model;  // eg: res50
        public long timeLoad;
        public long timeFirstImg;

        public int imgCurr;
        public int imgTotal;
        public int imgCorrect;

        public long startTime;
        public long endTime;

        public StatisticsTime.TimeRecord timeRecord;

        public WorkLoadStatus() {
            startTime = -1;
            endTime = -1;
        }

        public boolean isRunning() {
            return startTime > 0 && endTime < 0;
        }

        public long timeUsed() {
            return (endTime < 0 ? System.currentTimeMillis() : endTime) - startTime;
        }

        public String timeUsedMs() {
            return timeUsed() + "ms";
        }

        protected String toString(double d) {
            return String.format(Locale.getDefault(), "%.2f", d);
        }

        public double avgTime() {
            if (imgCurr <= 0) {
                return 0;
            }
            return (timeUsed() * 1.0) / imgCurr;
        }

        public String avgTimeText() {
            return toString(avgTime()) + "ms";
        }

        public double timeSd() {
            return timeRecord.statistics.sd;
        }

        public String timeSdText() {
            return toString(timeSd());
        }

        public double accuracy() {
            if (imgCurr <= 0) {
                return 0;
            }
            return (imgCorrect * 1.0) / imgCurr;
        }

        public String accuracyText() {
            return toString(accuracy());
        }
    }

//    public class sendThread extends Thread {
//        @Override
//        public void run() {
//            super.run();
//            try {
//                Socket socket = new Socket("121.48.163.88", 3456);
//                DataInputStream in = new DataInputStream(socket.getInputStream());
//                File file1 = new File(result_filepath);
//                System.out.println("文件大小：" + file1.length() + "B");
//
//                DataInputStream dis = new DataInputStream(new FileInputStream(result_filepath));
//                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//                dos.writeUTF(file1.getName());
//                dos.writeLong(file1.length());
//                byte[] buf = new byte[1024 * 9];
//                int len = 0;
//                while ((len = dis.read(buf)) != -1) {
//                    dos.write(buf, 0, len);
//                    System.out.println(len);
//                    dos.flush();
//                }
//                System.out.println("文件1上传结束，，，，");
//                String flag = in.readUTF();
//                System.out.println(flag);
//                File file2 = new File(procedure_filepath);
//                System.out.println("文件大小：" + file2.length() + "B");
//
//                dis = new DataInputStream(new FileInputStream(procedure_filepath));
//                dos = new DataOutputStream(socket.getOutputStream());
//                dos.writeUTF(file2.getName());
//                dos.writeLong(file2.length());
//                len = 0;
//                while ((len = dis.read(buf)) != -1) {
//                    dos.write(buf, 0, len);
//                    dos.flush();
//                }
//                System.out.println("文件2上传结束，，，，");
//                dis.close();
//                dos.close();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
//    }

    private static class MHandler extends Handler {
        protected final WeakReference<QuantClassifyActivity> mTarget;
        protected final WeakReference<AbstractModel> mModel;
        protected final int statusIndex;
        protected final WorkLoadStatus status;

        public MHandler(QuantClassifyActivity target, AbstractModel model, int statusIndex, WorkLoadStatus status) {
            this.mTarget = new WeakReference<>(target);
            this.mModel = new WeakReference<>(model);
            this.statusIndex = statusIndex;
            this.status = status;
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
            QuantClassifyActivity target = mTarget.get();
            AbstractModel model = mModel.get();
            if (target != null) {
                target.onImageClassified(model, msg.arg2, msg.obj, statusIndex, status);
            } else {
                if (model != null) {
                    model.destroy();
                }
            }
        }
    }
}