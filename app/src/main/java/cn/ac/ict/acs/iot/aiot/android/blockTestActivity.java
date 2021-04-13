package cn.ac.ict.acs.iot.aiot.android;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import cn.ac.ict.acs.iot.aiot.android.dataset.IDataset;
import cn.ac.ict.acs.iot.aiot.android.model.Model;
import cn.ac.ict.acs.iot.aiot.android.statistics.StatisticsTime;
import cn.ac.ict.acs.iot.aiot.android.util.DialogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;

import com.github.labowenzi.commonj.JEnumUtil;
import com.github.labowenzi.commonj.JJsonUtils;
import com.github.labowenzi.commonj.JUtil;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;

public class blockTestActivity extends AppCompatActivity {
    private TextView mblock;
    private TextView para_example;
    private TextView para1;
    private TextView para2;
    private TextView para3;
    private TextView para4;
    public int TIME_RECORD_TOP_K = 3;

    private TvInfoGlobal mTvInfoGlobal;
    private TvLog mTvLog;
    private TextView tv_result;

    private Button mBtnGo;
    private Button mBtnRemove;
    private String resourceName;
    private Model modelI;
    private IDataset dataset = null;
    private WorkLoadStatus[] statuses;
    private LogUtil.Log log = null;

    private boolean stopRunning = false;
    //note:new
    private String log_path;
    private String result_filepath;
    private String procedure_filepath;
    private final int run_times = 100;
    public String DIR_FILE;
    public String DIR_PATH;
    public String model_dir;
    public String net_tflite_filepath;
    private String blockName;
    private String datasetName;
    private String datasetFileName;
    private String modelFileName;
    private String[] blocks = {
            "conv",
            "avgpool",
            "maxpool",
            "DepthwiseConv2D",
            "resnet-conv-block",
            "resnet-identity-block",
            "mobilenet-stride1",
            "mobilenet-stride2",
            "squeezenet",
            "densenet"
    };
    private String[] block_para_examples = {
            "299,299,3  2   2  32\n" + "input_shape    kernel_size   strides   filters",
            "35,35,192    3    1    1\n28,28,256    2   2\n" + "input_shape    kernel_size    strides   same_padding\nsame_padding不填则为valid，1则为Same模式",
            "147,147,64    3    2\n" + "input_shape    kernel_size    strides",
            "112,112,96    3    2\n" + "input_shape    kernel_size    strides",
            "14,14,1024     512     7,7,2048\n" + "input_shape    filters    output_shape",
            "56,56,256    512\n" + "input_shape   filters\n" + "input和output的shape一样",
            "56,56,24\n" + "input_shape\n" + "input和output的shape一样",
            "112,112,16      56,56,24\n" + "input_shape    output_shape\n" + "filters不需要指定，默认乘以6",
            "55,55,96    16      64 \n" + "最终output_shape会是55,55,128(即64*2)\n" + "input_shape    1st_filters   2nd_filters",
            "56,56,64    6     32\n" + "input_shape   total_layers  stage(densenet121里面是32)"
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_test);
        para1 = findViewById(R.id.para1);
        para2 = findViewById(R.id.para2);
        para3 = findViewById(R.id.para3);
        para4 = findViewById(R.id.para4);
        para_example = findViewById(R.id.block_para_example);
        mTvInfoGlobal = new TvInfoGlobal(findViewById(R.id.tv_block_globalInfo));
        mTvInfoGlobal.init();
        mTvInfoGlobal.composeText();
        mTvInfoGlobal.refreshTextView();
        tv_result = findViewById(R.id.tv_block_result);
        mblock = findViewById(R.id.tv_blocks);
        mBtnGo = findViewById(R.id.btn_block_go);
        mBtnRemove = findViewById(R.id.btn_remove_parameter);
        findViewById(R.id.btn_block).setOnClickListener(view -> onSelectBlock());
        mBtnRemove.setOnClickListener(view -> onRemovePara());
        resourceName = "block";
        log_path = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + "aiot/log";
        DIR_FILE = "aiot/block";
        DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + DIR_FILE;
        model_dir = DIR_PATH + "/model/";

        mBtnGo.setOnClickListener(view -> {
            try {
                onGo();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void onRemovePara() {
        para1.setText("");
        para2.setText("");
        para3.setText("");
        para4.setText("");
    }

    private String changeSpaceToComma(String str) {
        return str.replace(' ', ',');
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void onGo() throws IOException {
        String[] p = new String[4];
        String msg = "";
        msg += mblock.getText();
        p[0] = changeSpaceToComma(para1.getText().toString());
        p[1] = changeSpaceToComma(para2.getText().toString());
        p[2] = changeSpaceToComma(para3.getText().toString());
        p[3] = changeSpaceToComma(para4.getText().toString());
        for (int i = 0; i < 4; i++) {
            if (p[i] != null) {
                msg += " ";
                msg += p[i];
            }
        }
        msg = msg.trim();
        goInit(msg);
        datasetName = p[0];
        loadDataset();
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                loadModel();
            }
        });
        th.start();
        try {
            th.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        startRunning();


    }

    private void goInit(String msg) {
        //参数获取
        modelFileName = msg.replace(" ", "_");
    }

    private void onSelect(String title, String[] items, DialogInterface.OnClickListener itemsL) {
        String negStr = "cancel";
        DialogInterface.OnClickListener negL = (dialog, which) -> dialog.dismiss();
        DialogUtil.dlgList(this, title, null, items, itemsL, null, null, negStr, negL).show();
    }

    private void onSelectBlock() {
        String title = "block";
        String[] items = blocks;
        DialogInterface.OnClickListener itemsL = (dialog, which) -> {
            onSelectBlock(which);
            dialog.dismiss();
        };
        onSelect(title, items, itemsL);
    }

    private void onSelectBlock(int index) {
        blockName = blocks[index];
        refreshBlockViews();
        para_example.setText("参数示例：" + block_para_examples[index]);
        para1.setText("");
        para2.setText("");
        para3.setText("");
        para4.setText("");
    }

    private void refreshBlockViews() {
        refreshTextViews(mblock, blockName);
    }

    private void refreshTextViews(TextView view, String text) {
        view.setText(text);
    }

    private void loadModel() {
        net_tflite_filepath = model_dir + "tflite/" + modelFileName + ".tflite";
        File file = new File(net_tflite_filepath);
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            //note:生成模型文件
            try {
                File model = new File(net_tflite_filepath);
                Socket socket = new Socket("192.168.1.18", 2345);
                DataInputStream in = new DataInputStream(socket.getInputStream());

                DataOutputStream fileRecvDos = new DataOutputStream(new FileOutputStream(model));
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("start" + modelFileName);
                byte[] buf = new byte[1024 * 9];
                int len = 0;
                while ((len = in.read(buf)) != -1) {
                    fileRecvDos.write(buf, 0, len);
                    fileRecvDos.flush();
                }
                dos.close();
                fileRecvDos.close();
                in.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            Thread st = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        File model = new File(net_tflite_filepath);
//                        Socket socket = new Socket("10.0.2.2", 3456);
//                        DataInputStream in = new DataInputStream(socket.getInputStream());
//
//                        DataOutputStream fileRecvDos = new DataOutputStream(new FileOutputStream(model));
//                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//                        dos.writeUTF("start"+modelFileName);
//                        byte[] buf = new byte[1024 * 9];
//                        int len = 0;
//                        while ((len = in.read(buf)) != -1) {
//                            fileRecvDos.write(buf, 0, len);
//                            System.out.println(len);
//                            fileRecvDos.flush();
//                        }
//                        dos.close();
//                        fileRecvDos.close();
//                        in.close();
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//            st.start();
//            try {
//                st.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    private void loadDataset() throws IOException {//dataset是否存在，存在即返回，不存在即生成
        if (JUtil.isEmpty(resourceName)) {
            dataset = null;
            return;
        }
        datasetFileName = DIR_PATH + "/datasets/";

        File file = new File(datasetFileName + datasetName);
        file.getParentFile().mkdirs();

        if (!file.exists()) {
            boolean b = file.createNewFile();
            //note：生成文件,随机写入文件即可，怎么读，读成什么应该是看接口的，或者自己可动的
            int[] inputsize = new int[3];
            String[] inputsize_str = datasetName.split(",");
            inputsize[0] = Integer.parseInt(inputsize_str[0]);
            inputsize[1] = Integer.parseInt(inputsize_str[1]);
            inputsize[2] = Integer.parseInt(inputsize_str[2]);
            Random random = new Random();
//            byte[][][] input = new byte[inputsize[2]][inputsize[0]][inputsize[1]];
//            FileOutputStream fos = new FileOutputStream(file);
//            for (int i=0;i<inputsize[2];i++){
//                for (int j=0;j<inputsize[0];j++){
//                    for (int k =0;k<inputsize[1];k++){
//                        input[i][j][k]=random.nextFloat();
//                    }
//                    random.nextBytes(input[i][j]);
//                    fos.write(input[i][j]);
//                    fos.write(input[i][j]);
//                    fos.write(input[i][j]);
//                    fos.write(input[i][j]);
//
//                }
//            }
            float[][][] input = new float[inputsize[2]][inputsize[0]][inputsize[1]];
            FileOutputStream fos = new FileOutputStream(file);
            DataOutputStream dos = new DataOutputStream(fos);
            for (int i = 0; i < inputsize[2]; i++) {
                for (int j = 0; j < inputsize[0]; j++) {
                    for (int k = 0; k < inputsize[1]; k++) {
                        input[i][j][k] = random.nextFloat();
                        dos.writeFloat(input[i][j][k]);
                    }


                }
            }
            fos.close();
            dos.close();

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
        File file = new File(DIR_PATH + "/models/tflite/" + modelFileName + ".tflite");
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            //note:生成模型文件
        }
        modelI = Model.getInstance(DIR_PATH + "/models");
        if (!JUtil.isEmpty(modelI.getDirs())) {
            modelI.setModelDir(modelI.getDirs()[0]);
        }
        LinkedList<WorkLoadStatus> ll = new LinkedList<>();

        for (String deviceName : Model.Device.getNames(Model.getSupportedDevices("tflite"))) {
            // todo: maybe from config file;
            WorkLoadStatus status = new WorkLoadStatus();
            status.framework = "tflite";
            status.quant = "no";
            status.model = modelFileName;
            status.device = deviceName;
            status.name = "tflite" + '_' + status.quant + "_" + deviceName + '_' + status.model;  // todo: maybe from config file;
            status.imgTotal = dataset.size();
            ll.add(status);
        }

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
            DialogUtil.dlg2(this, title, msg, posStr, posL, negStr, negL).show();
        } else {
            super.onBackPressed();
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startRunning() throws IOException {
        if (stopRunning) {
            return;
        }
        mBtnGo.setClickable(false);
//        mTvInfoGlobal.startTime = System.currentTimeMillis();
//        runOne(0);
        startRunning(0);
        mBtnGo.setClickable(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startRunning(int zz) throws IOException {
        if (stopRunning) {
            return;
        }
        MappedByteBuffer tfliteModel = loadModelFile(net_tflite_filepath);
        String fileName = modelFileName + logName(this);
        LogUtil.Log log = LogUtil.Log.inLogDir(fileName);
        log.logln("device:" + mTvInfoGlobal.deviceName);
        log.logln("ram size:" + RamInteger(mTvInfoGlobal.deviceRamTotal));
        log.logln("ram available size:" + mTvInfoGlobal.deviceRamAvailable);
        log.logln("device fingerprint:" + mTvInfoGlobal.deviceFingerprint);
        log.logln("device hardware:" + mTvInfoGlobal.hardware);
        log.logln("device sdk int:" + mTvInfoGlobal.sdkInt);
        String str = "平均时间(ms)\n";
        for (String deviceName : Model.Device.getNames(Model.getSupportedDevices("tflite"))) {

            Interpreter tflite;
            /** Options for configuring the Interpreter. */
            Interpreter.Options tfliteOptions = new Interpreter.Options();

            switch (deviceName) {
                case "nnapi":
                    tfliteOptions.setUseNNAPI(true);
                    break;
                case "gpu":
                    GpuDelegate gpuDelegate = new GpuDelegate();
                    tfliteOptions.addDelegate(gpuDelegate);
                    break;
                case "cpu":
                    break;
            }
            tfliteOptions.setNumThreads(1);
            tflite = new Interpreter(tfliteModel, tfliteOptions);

            String[] strs = modelFileName.split("_");
            int[] inputsize = new int[4];
            inputsize[0] = 1;
            String[] inputsize_str = strs[1].split(",");
            inputsize[1] = Integer.parseInt(inputsize_str[0]);
            inputsize[2] = Integer.parseInt(inputsize_str[1]);
            inputsize[3] = Integer.parseInt(inputsize_str[2]);

            File file = new File(datasetFileName + datasetName);
            byte[] bytes = Files.readAllBytes(Paths.get(datasetFileName + datasetName));
            ByteBuffer bbf = ByteBuffer.wrap(bytes);
            int[] probabilityShape = tflite.getOutputTensor(0).shape();
            DataType probabilityDataType = tflite.getOutputTensor(0).dataType();
            long start = System.currentTimeMillis();
            for (int i = 0; i < 20; i++) {
                TensorBuffer inputTensorBuffer = TensorBuffer.createFixedSize(inputsize, DataType.FLOAT32);
//            inputTensorBuffer.loadArray();
                TensorBuffer outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
                inputTensorBuffer.loadBuffer(bbf);
                tflite.run(inputTensorBuffer.getBuffer(), outputProbabilityBuffer.getBuffer());
            }
//            float[] floats
            long end = System.currentTimeMillis();
            str = str + deviceName + ": " + (float) (end - start) / 20 + "\n";
        }

        tv_result.setText(str);
        log.logln(str);
    }

    private MappedByteBuffer loadModelFile(String net_tflite_filepath) throws IOException {
        File file = new File(net_tflite_filepath);
        FileInputStream inputStream = new FileInputStream(file);
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = 0;
        long declaredLength = file.length();
        MappedByteBuffer ret = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        fileChannel.close();
        inputStream.close();
        return ret;
    }




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
            blockTestActivity ctx = blockTestActivity.this;
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
            long t = (endTime < 0 ? System.currentTimeMillis() : endTime) - startTime;

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

    public enum Device {
        CPU,
        NNAPI,
        GPU,
        ;

        @Nullable
        public static Device get(@Nullable String value) {
            return JEnumUtil.getByLowerCase(value, values());
        }

        @Nullable
        public static String getName(@Nullable Model.Device value) {
            return value == null ? null : value.name().toLowerCase();
        }

        @Nullable
        public static String[] getNames(@Nullable Model.Device[] values) {
            if (values == null) {
                return null;
            }
            String[] res = new String[values.length];
            for (int i = 0; i < values.length; ++i) {
                res[i] = getName(values[i]);
            }
            return res;
        }
    }

}