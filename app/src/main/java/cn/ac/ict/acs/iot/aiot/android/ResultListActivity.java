package cn.ac.ict.acs.iot.aiot.android;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.JUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cn.ac.ict.acs.iot.aiot.android.util.DialogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 20-11-5.
 */
public class ResultListActivity extends AppCompatActivity {
    /*
     * 测试结果
     * 点击查询历史结果
     * 先列出结果列表（按时间保存的文件名）
     * 点击一个结果，显示具体详情
     */

    private ListView mList;
    private ResultAdapter mAdapter;

    private final List<Result> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_list);
        mList = findViewById(R.id.lv_result);
        findViewById(R.id.btn_back).setOnClickListener(view -> onBackPressed());

        initData();
        mAdapter = new ResultAdapter();
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showResult(position);
            }
        });
    }

    private void initData() {
        String deviceName = Build.MODEL;
        File logDir = new File(LogUtil.Log.DEFAULT_FILE_PATH);
        File[] logDirFiles = logDir.listFiles();
        List<File> resultFiles = new LinkedList<>();
        if (!JUtil.isEmpty(logDirFiles)) {
            for (File logDirFile : logDirFiles) {
                if (!JIoUtil.canReadNormalFile(logDirFile)) {
                    continue;
                }
                String name = logDirFile.getName();
                if (!name.startsWith(deviceName)) {
                    continue;
                }
                Result item = new Result();
                item.name = name;
                item.path = logDirFile.getPath();
                data.add(item);
            }
        }
    }

    private void showResult(int index) {
        if (index < 0 || index >= data.size()) {
            Util.showToast("no data", this);
            return;
        }
        Result item = data.get(index);
        String title = "Log";
        String msg = readLogFrom(item);
        String btnStr = "确定";
        DialogInterface.OnClickListener btnL = (dialog, which) -> dialog.dismiss();
        DialogUtil.dlg1(ResultListActivity.this, title, msg, btnStr, btnL).show();
    }

    private String readLogFrom(Result item) {
        List<String> lines = JIoUtil.readLines(item.path);
        StringBuilder s = new StringBuilder();
        if (!JUtil.isEmpty(lines)) {
            for (String line : lines) {
                s.append(line).append('\n');
            }
        }
        return s.toString();
    }

    private static class Result {
        public String name;
        public String path;
    }

    private class ViewHolder {
        private View mRoot;
        private TextView mName;

        public ViewHolder(View root) {
            mRoot = root;
            mName = root.findViewById(R.id.tv_name);
        }

        public void setData(Result data) {
            if (data == null) {
                mName.setText("");
                return;
            }
            mName.setText(data.name);
        }
    }

    private class ResultAdapter extends ArrayAdapter<Result> {
        public ResultAdapter() {
            super(ResultListActivity.this, R.layout.item_result, data);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Result item = getItem(position);
            ViewHolder vh;
            if (convertView == null) {
                View view = getLayoutInflater().inflate(R.layout.item_result, parent, false);
                vh = new ViewHolder(view);
                view.setTag(vh);
                convertView = view;
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            vh.setData(item);
            return convertView;
        }
    }
}
