package cn.ac.ict.acs.iot.aiot.android.dataset;

import android.text.TextUtils;

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

import androidx.annotation.NonNull;

public class Coco {//coco folder    {coco2017_val (datasets)and catagory map(classesinfo)}
    public static final String COCO_DIR = "coco";

    private final ImageClasses classesInfo;//catagory map
    private final List<Dataset> datasets;

    public Coco(String dirPath, DatasetDesc.Coco cocoDesc) {
        String CocoDirPath = dirPath + "/" + COCO_DIR;

        try {
            classesInfo = new ImageClasses(CocoDirPath, cocoDesc.getClasses_info());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("No class info file: " + cocoDesc.getClasses_info(), e);
        }
        datasets = new ArrayList<>(cocoDesc.getDatasets().size());
        for (DatasetDesc.Coco.Dataset dataset : cocoDesc.getDatasets()) {
            datasets.add(new Dataset(CocoDirPath, dataset, classesInfo));
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

    public static class ImageClasses implements IImageClasses {//the file conclude catagory number and name(description)
        public final String[] classes;//catagory number
        public final String[] classesDesc;//description

        public ImageClasses(String imageDirPath, String rootDir) throws FileNotFoundException {
            this(readData(imageDirPath + "/" + rootDir));
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

    private static class Dataset implements IDataset {//20201116/coco/coco2017/validation
        protected final String name;
        protected final String rootDir;
        protected final String[] classes;
//        protected final String[][] classesFiles;
        protected final String[] Files;//all images in rootDir
        protected final int size;

        protected final ImageClasses classesInfo;

        public Dataset(String CocoDirPath, DatasetDesc.Desc.Dataset dataset, ImageClasses classesInfo) {
            this.name = dataset.getName();//coco2017
            this.rootDir = CocoDirPath + "/" + dataset.getDir();///storage/emulated/0/aiot/download/20201203_model_6_dataset_100x5/aiot/datasets/20201116/coco/coco2017/validation
            this.classesInfo = classesInfo;//coco2017_val(catagory label map)
            this.classes = classesInfo.classes;//catagories
//            this.classesFiles = new String[classes.length][];//all images in every catagories

            if (TextUtils.isEmpty(rootDir)) {
                throw new RuntimeException("No Coco dir " + rootDir);
            }
            File root = new File(rootDir);
            if (!root.exists() || !root.isDirectory()) {
                throw new RuntimeException("Wrong Coco dir " + rootDir);
            }

            this.Files = root.list();

            this.size = Files.length;
        }

        @Override
        public boolean isStatusOk() {
            return classes != null && classes.length > 0
                    && Files != null && Files.length > 0
                    && size > 0
                    && classesInfo != null
                    && classesInfo.classes != null && classesInfo.classes.length > 0;
        }

        @Override
        public String get(int index) {
            if (index < 0) {
                return null;
            }
            return rootDir + '/' + Files[index];
        }
        @Override
        public int getClassIndex(int index) {
            //todo:discarded
//            if (index < 0) {
//                return -1;
//            }
//            for (int i=0; i<classesFiles.length; ++i) {
//                if (index < classesFiles[i].length) {
//                    return i;
//                }
//                index -= classesFiles[i].length;
//            }
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
