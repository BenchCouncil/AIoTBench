package cn.ac.ict.acs.iot.aiot.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.JJsonUtils;
import com.github.labowenzi.commonj.JUtil;
import com.github.labowenzi.commonj.log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alanubu on 20-11-10.
 */
public class DownloadInfo {
    private static final String TAG = "downloadinfo";

    public static final String DIR_FILE = "aiot/download";
    public static final String DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + DIR_FILE;

    public static String getDirPath(String resourceName) {
        return DIR_PATH + '/' + resourceName;
    }
    public static String getDirPathDataset(String resourceName) {
        return getDirPath(resourceName) + "/aiot/datasets";
    }
    public static String getDirPathModels(String resourceName) {
        return getDirPath(resourceName) + "/aiot/models";
    }

    public static class Resource {

        private static Resource instance = null;
        @NonNull
        public static Resource getInstance() {
            if (instance == null) {
                instance = new Resource();
            }
            return instance;
        }

        public final ResourceDesc desc;

        public Resource() {
            ResourceDesc desc = null;
            if (false) {
                try {
                    String downloadInfoFilePath = DIR_PATH + '/' + "download_info.json";
                    String json = JIoUtil.readJson(downloadInfoFilePath);
                    desc = JJsonUtils.fromJson(json, ResourceDesc.class);
                } catch (Exception e) {
                    Log.e(TAG, "wrong download_info file");
                }
            }
            desc = new ResourceDesc();  // todo: use download info in java code instead of json file;
            desc.resources = new ArrayList<>();
            ResourceOne one = new ResourceOne();
            one.name = "20201203_model_6_dataset_100x5";
            one.desc = "描述2-model:20201105_novgg:6-dataset:100x5";
            one.url_file = "http://125.39.136.212:8484/20201203_model_6_dataset_100x5.zip";
            one.size_total = 855169109;
            desc.resources.add(one);
            this.desc = desc;
        }

        @Nullable
        public ResourceOne getOne() {
            if (desc != null && !JUtil.isEmpty(desc.resources)) {
                return desc.resources.get(0);
            }
            return null;
        }
    }
    public static class ResourceDesc {
        public List<ResourceOne> resources;
    }
    public static class ResourceOne {
        public String name;
        public String desc;
        public String url_file;
        public long size_total;
    }



    public static class AbsSp {

        @NonNull
        protected final SharedPreferences sp;

        public AbsSp(@NonNull SharedPreferences sp) {
            this.sp = sp;
        }
        public AbsSp(@NonNull Context ctx) {
            String settingFileName = "app_setting";
            int fileMode = Context.MODE_PRIVATE;
            this.sp = ctx.getSharedPreferences(settingFileName, fileMode);
        }

        protected void setString(@NonNull String key, @Nullable String value) {
            sp.edit().putString(key, value==null?"":null).apply();
        }

    }
    public static class DownloadSp extends AbsSp {
        public DownloadSp(@NonNull Context ctx) {
            super(ctx.getSharedPreferences("download_info", Context.MODE_PRIVATE));
        }

        public long getSizeDownload() {
            return sp.getLong("SIZE_DOWNLOAD", 0);
        }
        public void setSizeDownload(long sizeDownload) {
            sp.edit().putLong("SIZE_DOWNLOAD", sizeDownload).apply();
        }

        public long getDownloadTimeUsed() {
            return sp.getLong("DOWNLOAD_TIME_USED", 0);
        }
        public void setDownloadTimeUsed(long downloadTimeUsed) {
            sp.edit().putLong("DOWNLOAD_TIME_USED", downloadTimeUsed).apply();
        }
    }
}
