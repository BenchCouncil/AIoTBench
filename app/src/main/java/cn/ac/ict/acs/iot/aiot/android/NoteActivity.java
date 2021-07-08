package cn.ac.ict.acs.iot.aiot.android;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by alanubu on 20-11-5.
 */
public class NoteActivity extends AppCompatActivity {
    /*
     * 测试说明
     * 点击后显示测试说明
     * 可返回入口页
     */
    private TextView mNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        mNote = findViewById(R.id.tv_note);
        findViewById(R.id.btn_back).setOnClickListener(view -> finish());
        String note = "AIoTBench focuses on the evaluation of the inference ability of mobile and embedded devices. Considering the representative and diversity of both model and framework, AIoTBench covers three typical heavy-weight networks: ResNet50, InceptionV3, DenseNet121, as well as three light-weight networks: SqueezeNet, MobileNetV2, MnasNet. Each model is implemented by three popular frameworks: Tensorflow Lite, Caffe2, Pytorch Mobile. For each model in Tensorflow Lite, we also provide three quantization versions: dynamic range quantization, full integer quantization, float16 quantization. Moreover, the models in Tensorflow Lite can be run with CPU and NNAPI delegate, respectively. There are 60 workload instances in total.\n" +
                "\n" +
                "To run the test, you should first download the models and dataset on the device. You can click the Non-quantizing Test or Quantizing Test button to automatically runs all workloads. Also, you can configure and run Single Workload Test when you want to evaluate a particular workload.\n";
        mNote.setText(note);
    }
}
