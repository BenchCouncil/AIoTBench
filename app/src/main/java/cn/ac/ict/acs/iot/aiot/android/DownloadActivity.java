package cn.ac.ict.acs.iot.aiot.android;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.JNetUtil;
import com.github.labowenzi.commonj.JTimeUtil;
import com.github.labowenzi.commonj.JUtil;
import com.github.labowenzi.commonj.JZip;
import com.github.labowenzi.commonj.ThreadPoolUtil;
import com.github.labowenzi.commonj.log.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Locale;

import cn.ac.ict.acs.iot.aiot.android.util.DialogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 20-11-5.
 */
public class DownloadActivity extends AppCompatActivity {
    /*
     * 资源下载
     * 如果资源已下载，则提示已下载
     * 点击后，状态显示下载进度（已下载/总大小）
     * 完成后可返回入口页
     */
    private static final String TAG = "DownloadActivity";

    private TextView mName;
    private TextView mDesc;
    private TextView mTvProgress;
    private Button mBtnDownload;

    private String name;
    private String desc;
    private long sizeTotal;
    private String urlFile;
    private long sizeDownload;
    private long downloadTimeUsed;
    private long downloadStartTime;

    private DownloadInfo.DownloadSp downloadSp;
    private DownloadL dl;
    private MHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        mName = findViewById(R.id.tv_download_name);
        mDesc = findViewById(R.id.tv_download_desc);
        mTvProgress = findViewById(R.id.tv_download_progress);
        mBtnDownload = findViewById(R.id.btn_download_download);
        findViewById(R.id.btn_back).setOnClickListener(view -> onBackPressed());
        mBtnDownload.setOnClickListener(view -> onDownload());

        mHandler = new MHandler(this);
        DownloadInfo.ResourceOne resource = DownloadInfo.Resource.getInstance().getOne();
        if (resource == null) {
            name = null;
            desc = null;
            urlFile = null;
            sizeTotal = -1;
            Util.showToast("no download info file", this);
            finish();
            return;
        } else {
            name = resource.name;
            desc = resource.desc;
            urlFile = resource.url_file;
            sizeTotal = resource.size_total;
        }
        sizeDownload = 0;
        downloadStartTime = -1;
        downloadTimeUsed = 0;

        downloadSp = new DownloadInfo.DownloadSp(this);
        loadDownloadedData();
        dl = null;


        mName.setText(name);
        mDesc.setText(desc);
        refreshProgressStatus();
    }

    private void refreshProgressStatus() {
        double progress;
        if (sizeTotal <= 0) {
            progress = 0;
        } else {
            progress = (sizeDownload*100.0) / (sizeTotal*1.0);
        }
        String text;
        boolean clickable;
        if (sizeDownload <= 0) {
            text = "未下载";
            clickable = true;
        } else if (sizeDownload >= sizeTotal) {
            text = "已下载";
            clickable = false;
        } else {
            text = "下载中";
            clickable = false;
        }
        text = text + "：" + String.format(Locale.getDefault(), "%.2f", progress) + "%" + "：" + sizeDownload + " Byte / " + sizeTotal + " Byte";
        long timeUsed = downloadTimeUsed;
        if (downloadStartTime > 0) {
            timeUsed += System.currentTimeMillis()-downloadStartTime;
        }
        if (timeUsed > 0) {
            text = text + "：已下载耗时 " + JTimeUtil.timeElapsedHours(timeUsed);
        }
        mTvProgress.setText(text);
        mBtnDownload.setClickable(clickable);
    }

    @Override
    public void onBackPressed() {
        if (isDownloading()) {
            String title = "是否退出？";
            String msg = "是否取消下载并退出？";
            String posStr = "取消下载并退出";
            DialogInterface.OnClickListener posL = (dialog, which) -> {
                cancelDownload();
                super.onBackPressed();
                dialog.dismiss();
            };
            String negStr = "取消";
            DialogInterface.OnClickListener negL = (dialog, which) -> {
                dialog.dismiss();
            };
            DialogUtil.dlg2(DownloadActivity.this, title, msg, posStr, posL, negStr, negL).show();
        } else {
            super.onBackPressed();
        }
    }

    private void loadDownloadedData() {
        sizeDownload = downloadSp.getSizeDownload();
        downloadTimeUsed = downloadSp.getDownloadTimeUsed();

        File f = new File(DownloadL.getFilePath(name));
        if (JIoUtil.canReadNormalFile(f)) {
            long size = f.length();
            if (size == sizeTotal) {
                sizeDownload = sizeTotal;
            } else {
                sizeDownload = 0;
                clearData();
            }
        } else {
            sizeDownload = 0;
            clearData();
        }
    }
    private void saveToSp() {
        downloadSp.setSizeDownload(sizeDownload);
        downloadSp.setDownloadTimeUsed(downloadTimeUsed);
    }

    private void clearData() {
        JIoUtil.deleteFileSilently(DownloadL.getTempFilePath(name));
        JIoUtil.deleteFileSilently(DownloadL.getFilePath(name));
        JIoUtil.deleteFileSilently(DownloadL.getFileDir(name));
    }

    private void onDownload() {
        if (sizeDownload >= sizeTotal) {
            Util.showToast("已下载完成", this);
            return;
        }
        dl = new DownloadL(this, urlFile, name);
        dl.getFile();
    }

    private void onDownloaded() {
        if (dl != null) {
            dl.cancel(false);
            dl = null;
        }
        unzip();
    }
    private void unzip() {
        Util.showToast("unzip start", this);
        String zipFile = DownloadL.getFilePath(name);
        String destDir = DownloadL.getFileDir(name);
        unzip(mHandler, zipFile, destDir);
    }
    private static void unzip(Handler handler, String zipFile, String destDir) {
        ThreadPoolUtil.run(() -> {
            try {
                JZip.unzipFile(zipFile, destDir);
                unzipSendMsg(handler, true);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage(), e);
                unzipSendMsg(handler, false);
            }
        });
    }
    private static void unzipSendMsg(Handler handler, boolean success) {
        Message message = handler.obtainMessage();
        message.what = MHandler.HANDLER_UNZIP_DONE;
        message.arg1 = !success ? 0 : 1;
        message.obj = null;
        message.arg2 = 0;
        handler.sendMessage(message);
    }
    private void unzipDone(boolean success) {
        String text = success ? "unzip done" : "unzip failed";
        Util.showToast(text, this);
    }

    private boolean isDownloading() {
        return dl != null;
    }

    private void cancelDownload() {
        if (dl != null) {
            dl.cancel(false);
            dl = null;
        }
    }

    public static class DownloadL extends AsyncTask<String, Long, Long> {
        private final WeakReference<DownloadActivity> mTarget;
        private final String url;
        private final String name;
        private final String tempPath;
        private final String path;

        public DownloadL(DownloadActivity target, String url, String name) {
            this.mTarget = new WeakReference<>(target);
            this.url = url;
            this.name = name;
            tempPath = getTempFilePath(name);
            path = getFilePath(name);
        }

        public static String getFileDir(String name) {
            return DownloadInfo.DIR_PATH + '/' + name;
        }
        public static String getTempFilePath(String name) {
            return getFileDir(name) + '/' + ".download_tmp";
        }
        public static String getFilePath(String name) {
            return getFileDir(name) + '/' + name + ".zip";
        }

        private int getType() {
            synchronized (this) {
                int type;
                if ((new File(getTempFilePath(name))).exists()) {
                    type = 1; // wait;
                } else if ((new File(getFilePath(name))).exists()) {
                    type = 2; // get;
                } else {
                    type = 3; // download;
                    JIoUtil.mkdir(getFileDir(name));
                    File tempFile = new File(getTempFilePath(name));
                    try {
                        if (!tempFile.createNewFile()) {
                            Log.e(TAG,"createNewFile failed");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "cannot create file: " + tempFile.getAbsolutePath());
                        failed();
                    }
                }
                return type;
            }
        }

        public void getFile() {
            if (JUtil.isEmpty(url)) {
                failed();
                return;
            }
            int type = getType();
            switch (type) {
                case 1: {
                    // on another task
                    Log.e(TAG, "onAnotherTask");
                    break;
                }
                case 2: {
                    onAlreadyGet();
                    break;
                }
                case 3: {
                    toDownload();
                    break;
                }
                default: {
                    Log.d(TAG, "what?");
                    failed();
                    break;
                }
            }
        }

        private void failed() {
            onDone(null, -1L);
        }
        private void onAlreadyGet() {
            // file exist;
            Log.d(TAG, "get from disk: " + url);
            String path = getFilePath(name);
            onDone(path, new File(path).length());
        }
        private void toDownload() {
            // to download the file;
            Log.d(TAG, "downloading by url: " + url);
            execute();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DownloadActivity target = mTarget.get();
            if (target != null) {
                target.sizeDownload = 1;
                target.refreshProgressStatus();
            }
        }

        @Override
        protected Long doInBackground(String... params) {
            if (url == null || tempPath == null || path == null) {
                Log.e(TAG, "wrong params");
                return -1L;
            }
            DownloadActivity target = mTarget.get();
            if (target != null) {
                boolean flag = downloadToFile();
                if (flag) {
                    boolean flag1 = (new File(tempPath)).renameTo(new File(path));
                    if (!flag1) Log.e(TAG, "renameTo");
                    Log.d(TAG, "download url to file: " + url + "  " + path);
                    return new File(path).length();
                } else {
                    File file = new File(tempPath);
                    if (!file.delete()) Log.e(TAG, "delete");
                    Log.e(TAG, "download url: failed: " + url);
                    return -1L;
                }
            } else {
                Log.e(TAG, "no cache");
            }
            return -1L;
        }

        protected boolean downloadToFile() {
            URL finalUrl = JNetUtil.resoveUrl(url);
            if (null == finalUrl) {
                Log.e(TAG, "wrong final url");
                return false;
            }
            long total = JNetUtil.getLength(finalUrl);
            if (total == 0) total = -1;
            InputStream stream = JNetUtil.openRemoteInputStream(finalUrl);
            if (null == stream) {
                Log.e(TAG, "failed to open input stream");
                return false;
            }

            InputStream is = new BufferedInputStream(stream, JNetUtil.BUFFER_SIZE);
            FileOutputStream out;
            try {
                out = new FileOutputStream(tempPath);
                byte[] data = new byte[JNetUtil.BUFFER_SIZE];
//                byte[] data = new byte[1024];
                long cur = 0;
                publishProgress(cur, total);
                int count;
                while((count = is.read(data)) != -1) {
                    cur += count;
                    publishProgress(cur, total);
                    out.write(data, 0, count);
                }
                Log.d("download", "get data total: " + cur + " total=" + total);
                out.flush();
                out.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            if (values == null || values.length < 2) return;
            Long cur = values[0];
            Long total = values[1];
            DownloadActivity target = mTarget.get();
            if (target != null) {
                target.sizeDownload = cur;
                target.refreshProgressStatus();
            }
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);
            onDone(path, aLong);
        }

        public void onDone(String filepath, Long result) {
            if (filepath == null) {
                Log.e(TAG, "error: downloading");
                return;
            }
            File f = new File(filepath);
            if (!JIoUtil.canRead(f)) {
                Log.e(TAG, "error: downloading file");
                return;
            }
            DownloadActivity target = mTarget.get();
            if (target != null) {
                target.sizeDownload = target.sizeTotal;
                target.refreshProgressStatus();
                target.onDownloaded();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            JIoUtil.deleteFileSilently(tempPath);
        }
    }

    private static class MHandler extends Handler{
        public static final int HANDLER_UNZIP_DONE = 1;

        protected final WeakReference<DownloadActivity> mTarget;

        public MHandler(DownloadActivity target) {
            this.mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case HANDLER_UNZIP_DONE: {
                    unzipDone(msg);
                    break;
                }
            }
        }

        private void unzipDone(Message msg) {
            boolean success = msg.arg1 != 0;
            DownloadActivity target = mTarget.get();
            if (target != null) {
                target.unzipDone(success);
            }
        }
    }
}
