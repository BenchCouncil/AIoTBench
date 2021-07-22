package cn.ac.ict.acs.iot.aiot.android.dataset;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.JJsonUtils;
import com.github.labowenzi.commonj.JUtil;
import com.github.labowenzi.commonj.log.Log;

import java.io.File;
import java.util.List;

/**
 * Created by alanubu on 20-1-19.
 */
public class Dataset {
    private static final String TAG = "dataset";

    private static Dataset instance = null;

    @NonNull
    public static Dataset getInstance(Context context) {
        if (instance == null) {
            instance = getInstance(context, DIR_PATH);
        }
        return instance;
    }

    @NonNull
    public static Dataset getInstance(Context context, String dirPath) {
        return new Dataset(context, dirPath);
    }

    public static Dataset resetInstance(Context context) {
        instance = null;
        return getInstance(context);
    }

    public static final String DIR_FILE = "aiot/datasets";
    public static final String DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + DIR_FILE;

    private DemoImage demoImage;

    private final String dirPath;

    private String[] dirs;
    private DatasetDir datasetDir;

    public Dataset(Context context, String dirPath) {
        this.dirPath = dirPath;
        demoImage = new DemoImage(context);
        File datasetDir = new File(dirPath);
        if (datasetDir.exists() && datasetDir.isDirectory()) {
            dirs = datasetDir.list();
        } else {
            dirs = null;
        }
        this.datasetDir = null;
    }

    public DemoImage getDemoImage() {
        return demoImage;
    }

    public String[] getDirs() {
        return dirs;
    }

    public DatasetDir getDatasetDir() {
        return datasetDir;
    }

    public void setDatasetDir(String dir) {
        if (JUtil.isEmpty(dir) || JUtil.isEmpty(dirs)) {
            Log.e(TAG, "no dir or dirs");
            return;
        }
        if (JUtil.inArray(dir, dirs)) {
            String dirPath = this.dirPath + "/" + dir;
            this.datasetDir = new DatasetDir(dirPath);
        } else {
            Log.e(TAG, "no dir in dirs");
        }
    }

    public String getDefaultDatasetName() {
        return DemoImage.NAME;
    }

    public IDataset getDataset(String datasetName) {
        if (JUtil.isEmpty(datasetName)) {
            return null;
        }
        if (datasetName.equals(DemoImage.NAME)) {
            return demoImage;
        }
        if (datasetName.equals("coco2017")) {
            return datasetDir.coco.getDataset(datasetName);
        }
        if (datasetName.equals("div2k")) {
            return datasetDir.div2k.getDataset(datasetName);
        }
        return datasetDir.imagenet.getDataset(datasetName);
    }

    public static class DatasetDir {
        public static final String DATASETS_FILE = "datasets.json";

        private final String dirPath;
        private final DatasetDesc datasetDesc;

        /**
         * like {@link DatasetDesc};
         */
        private final ImageNet imagenet;
        private final Coco coco;
        private final Div2k div2k;

        private String[] names;

        public DatasetDir(String dirPath) {
            this.dirPath = dirPath;//....aiot/datasets/20201116
            this.datasetDesc = JJsonUtils.fromJson(JIoUtil.readJson(dirPath + "/" + DATASETS_FILE), DatasetDesc.class);
            this.imagenet = new ImageNet(dirPath, datasetDesc.getImagenet());
            this.coco = new Coco(dirPath, datasetDesc.getCoco());
            this.div2k = new Div2k(dirPath, datasetDesc.getDiv2k());
            init();
        }

        private void init() {
            List<DatasetDesc.ImageNet.Dataset> imageNetDatasets = datasetDesc.getImagenet().getDatasets();
            List<DatasetDesc.Coco.Dataset> cocoDatasets = datasetDesc.getCoco().getDatasets();
            List<DatasetDesc.Div2k.Dataset> div2kDatasets = datasetDesc.getDiv2k().getDatasets();

            this.names = new String[1 + imageNetDatasets.size() + cocoDatasets.size()];
            names[0] = DemoImage.NAME;
            int i;
            for (i = 1; i <= imageNetDatasets.size(); ++i) {
                names[i] = imageNetDatasets.get(i - 1).getName();
            }
            for (; i <= imageNetDatasets.size()+cocoDatasets.size(); i++) {
                names[i] = cocoDatasets.get(i - 1 - imageNetDatasets.size()).getName();
            }
            for (; i<names.length;i++){
                names[i]=div2kDatasets.get(i-1-imageNetDatasets.size()-cocoDatasets.size()).getName();
            }
        }

        public DatasetDesc getDatasetDesc() {
            return datasetDesc;
        }

        public ImageNet getImagenet() {
            return imagenet;
        }

        public Coco getCoco() {
            return coco;
        }

        public Div2k getDiv2k() {
            return div2k;
        }

        public String[] getNames() {
            return names;
        }
    }
}
