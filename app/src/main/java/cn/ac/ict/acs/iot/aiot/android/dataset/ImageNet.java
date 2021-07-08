package cn.ac.ict.acs.iot.aiot.android.dataset;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.JUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * like {@link DatasetDesc.ImageNet}
 * Created by alanubu on 20-1-19.
 */
public class ImageNet {
    public static final String IMAGENET_DIR = "imagenet";

    private final ImageClasses classesInfo;
    private final List<Dataset> datasets;

    public ImageNet(String dirPath, DatasetDesc.ImageNet imageNetDesc) {
        String imageNetDirPath = dirPath + "/" + IMAGENET_DIR;

        try {
            classesInfo = new ImageClasses(imageNetDirPath, imageNetDesc.getClasses_info());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("No class info file: " + imageNetDesc.getClasses_info(), e);
        }
        datasets = new ArrayList<>(imageNetDesc.getDatasets().size());
        for (DatasetDesc.ImageNet.Dataset dataset : imageNetDesc.getDatasets()) {
            datasets.add(new Dataset(imageNetDirPath, dataset, classesInfo));
        }
    }

    public ImageClasses getClassesInfo() {
        return classesInfo;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public Dataset getDataset(String datasetName) {
        if (JUtil.isEmpty(datasetName) || JUtil.isEmpty(datasets)) {
            return null;
        }
        for (Dataset d : datasets) {
            if (datasetName.equals(d.name)) {
                return d;
            }
        }
        return null;
    }

    public static class ImageClasses implements IImageClasses {
        public final String[] classes;
        public final String[] classesDesc;

        public ImageClasses(String imageNetDirPath, String rootDir) throws FileNotFoundException {
            this(readData(imageNetDirPath + "/" + rootDir));
        }
        public ImageClasses(InputStream inputStream) {
            this(readData(inputStream));
        }
        public ImageClasses(List<String> data) {
            int length = data.size();
            this.classes = new String[length];
            this.classesDesc = new String[this.classes.length];
            for (int i = 0; i < length; i++) {
                String s = data.get(i);
                String[] ss = s.split("\t");
                this.classes[i] = ss[0];
                this.classesDesc[i] = ss[1];
            }
        }

        private static List<String> readData(String filePath) throws FileNotFoundException {
            InputStream in = new FileInputStream(new File(filePath));
            List<String> data = readData(in);
            JIoUtil.closeSilently(in);
            return data;
        }
        private static List<String> readData(InputStream inputStream) {
            ArrayList<String> arrayList = new ArrayList<>(1000);
            try {
                InputStreamReader inputReader = new InputStreamReader(inputStream);
                BufferedReader bf = new BufferedReader(inputReader);
                // 按行读取字符串
                String str;
                while ((str = bf.readLine()) != null) {
                    arrayList.add(str);
                }
                bf.close();
                inputReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return arrayList;
        }

        @Override
        public String getName(int index) {
            return classes[index];
        }

        @Override
        public String getDesc(int index) {
            return classesDesc[index];
        }

        @Override
        public int getSize() {
            return classes.length;
        }
    }

    private static class Dataset implements IDataset {
        protected final String name;
        protected final String rootDir;
        protected final String[] classes;
        protected final String[][] classesFiles;
        protected final int size;

        protected final ImageClasses classesInfo;

        public Dataset(String imageNetDirPath, DatasetDesc.ImageNet.Dataset dataset, ImageClasses classesInfo) {
            this.name = dataset.getName();
            this.rootDir = imageNetDirPath + "/" + dataset.getDir();//storage/emulated/0/aiot/download/20201203_model_6_dataset_100x5/aiot/datasets/20201116/coco/coco2017/validation
            this.classesInfo = classesInfo;//coco2017_val(catagory label map)
            this.classes = classesInfo.classes;//catagories
            this.classesFiles = new String[classes.length][];//all images in every catagories
            if (TextUtils.isEmpty(rootDir)) {
                throw new RuntimeException("No imageNet dir " + rootDir);
            }
            File root = new File(rootDir);
            if (!root.exists() || !root.isDirectory()) {
                throw new RuntimeException("Wrong imageNet dir " + rootDir);
            }
            for (int i=0; i<classes.length; ++i) {
                String classDirName = classes[i];
                String classPath = rootDir + "/" + classDirName;
                File classDir = new File(classPath);
                if (classDir.exists() && classDir.isDirectory() && !TextUtils.isEmpty(classDirName) && classDirName.startsWith("n")) {
                    String[] files = classDir.list();
                    classesFiles[i] = files == null || files.length <= 0 ? new String[0] : files;
                } else {
                    classesFiles[i] = new String[0];
                }
            }
            int size = 0;
            for (String[] files : this.classesFiles) {
                size += files.length;
            }
            this.size = size;
        }

        @Override
        public boolean isStatusOk() {
            return classes != null && classes.length > 0
                    && classesFiles != null && classesFiles.length > 0
                    && size > 0
                    && classesInfo != null
                    && classesInfo.classes != null && classesInfo.classes.length > 0;
        }

        @Override
        public String get(int index) {
            if (index < 0) {
                return null;
            }
            for (int i=0; i<classesFiles.length; ++i) {
                if (index < classesFiles[i].length) {
                    return rootDir + '/' + classes[i] + '/' + classesFiles[i][index];
                }
                index -= classesFiles[i].length;
            }
            return null;
        }
        @Override
        public int getClassIndex(int index) {
            if (index < 0) {
                return -1;
            }
            for (int i=0; i<classesFiles.length; ++i) {
                if (index < classesFiles[i].length) {
                    return i;
                }
                index -= classesFiles[i].length;
            }
            return -1;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public ImageClasses getClassesInfo() {
            return classesInfo;
        }

        @NonNull
        @Override
        public String toString() {
            return "root=" + rootDir
                    + ",classes[" + classes.length + "]"
                    + ",files[" + size + "]";
        }
    }
}
