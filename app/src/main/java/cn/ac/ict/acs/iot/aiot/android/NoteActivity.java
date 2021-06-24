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
        String note = "这是说明";
        mNote.setText(note);
    }
}
