package cn.ac.ict.acs.iot.aiot.android;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import cn.ac.ict.acs.iot.aiot.android.util.EnumUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 19-12-25.
 */
public class DatasetHelper {
    private static final String TAG = "dataset";

    // java enum String 'Type{demo, imageNet, user_selected_file,}'
    public enum Type implements EnumUtil.EnumString {
        E_DEMO("demo") {
            @Override
            public IDataset getDataset(Context context) {
                return new DatasetHelper.DemoAssetImage(
                        context,
                        DatasetHelper.ImageNet.IMAGE_NET_CLASS_FILE_PATH);
            }
        },
        E_IMAGENET_2_2("imageNet_2x2") {
            @Override
            public IDataset getDataset(Context context) {
                return new DatasetHelper.ImageNet.ImageNetLoader(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + ImageNet.IMAGE_NET_DATA_DIR_2_2,
                        DatasetHelper.ImageNet.IMAGE_NET_CLASS_FILE_PATH);
            }
        },
        E_IMAGENET_10_50("imageNet_10x50") {
            @Override
            public IDataset getDataset(Context context) {
                return new DatasetHelper.ImageNet.ImageNetLoader(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + ImageNet.IMAGE_NET_DATA_DIR_10_50,
                        DatasetHelper.ImageNet.IMAGE_NET_CLASS_FILE_PATH);
            }
        },
        E_IMAGENET_1000_50("imageNet_1000x50") {
            @Override
            public IDataset getDataset(Context context) {
                throw new IllegalArgumentException("not implement");
            }
        },
        E_USER_SELECTED_FILE("其他可选文件") {
            @Override
            public IDataset getDataset(Context context) {
                throw new IllegalArgumentException("use getDataset(Context context, String dirOrFile)");
            }
            public IDataset getDataset(Context context, String dirOrFile) {
                throw new IllegalArgumentException("not implement");
            }
        },
        ;

        @NonNull
        private final String value;

        Type(@NonNull String value) {
            this.value = value;
        }

        @NonNull
        public String getValue() {
            return value;
        }

        public abstract IDataset getDataset(Context context);

        @Override
        @NonNull
        public String toString() {
            return super.toString() + "(" + value + ")";
        }

        public static final Type[] values = values();

        @Nullable
        public static Type get(@Nullable String value) {
            if (value == null) {
                return null;
            }
            for (Type item : values) {
                if (item.getValue().equals(value)) {
                    return item;
                }
            }
            return null;
        }
    }


    public static class ImageNet {
        public static final String IMAGE_NET_DATA_DIR_2_2 = "aiot/imagenet/ILSVRC2012_img_val_sample/validation";
        public static final String IMAGE_NET_DATA_DIR_10_50 = "aiot/imagenet/ILSVRC2012_img_val_sample_10x50/validation";

        public static final String IMAGE_NET_CLASS_FILE = "aiot/imagenet/ILSVRC2012_img_val_classes_with_name";
        public static final String IMAGE_NET_CLASS_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + IMAGE_NET_CLASS_FILE;

        public static class ImageNetLoader implements Iterable<String>, IDataset {
            protected final String rootDir;
            protected final String[] classes;
            protected final String[][] classesFiles;
            protected final int size;

            protected final ImageNetClasses classesInfo;

            public ImageNetLoader(String rootDir, String classInfoFilePath) {
                this.rootDir = rootDir;
                if (TextUtils.isEmpty(rootDir)) {
                    throw new RuntimeException("No imageNet dir " + rootDir);
                }
                File root = new File(rootDir);
                if (!root.exists() || !root.isDirectory()) {
                    throw new RuntimeException("Wrong imageNet dir " + rootDir);
                }
                File[] classesDirs = root.listFiles();
                ArrayList<String> classesDirList = new ArrayList<>(classesDirs.length);
                ArrayList<String[]> classesDirFileLists = new ArrayList<>(classesDirs.length);
                for (File classDir : classesDirs) {
                    String classDirName = classDir.getName();
                    if (classDir.exists() && classDir.isDirectory() && !TextUtils.isEmpty(classDirName) && classDirName.startsWith("n")) {
                        classesDirList.add(classDirName);
                        String[] files = classDir.list();
                        classesDirFileLists.add(files == null || files.length <= 0 ? new String[0] : files);
                    }
                }
                this.classes = new String[classesDirList.size()];
                for (int i=0; i<this.classes.length; ++i) {
                    this.classes[i] = classesDirList.get(i);
                }
                this.classesFiles = new String[classesDirFileLists.size()][];
                for (int i=0; i<this.classesFiles.length; ++i) {
                    this.classesFiles[i] = classesDirFileLists.get(i);
                }
                int size = 0;
                for (String[] files : this.classesFiles) {
                    size += files.length;
                }
                this.size = size;

                try {
                    classesInfo = new ImageNetClasses(classInfoFilePath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
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
            public ImageNetClasses getClassesInfo() {
                return classesInfo;
            }

            @NonNull
            @Override
            public String toString() {
                return "root=" + rootDir
                        + ",classes[" + classes.length + "]"
                        + ",files[" + size + "]";
            }

            @NonNull
            @Override
            public Iterator<String> iterator() {
                return new Itr();
            }

            private class Itr implements Iterator<String> {

                private int classesIndex = -1;
                private int classesFileIndex = -1;

                @Override
                public boolean hasNext() {
                    if (classesIndex >= classes.length) {
                        return false;
                    } else if (classesIndex < classes.length-1) {
                        return true;
                    } else {
                        // classesIndex == classes.length-1
                        int limit = classesFiles[classesIndex].length;
                        return classesFileIndex < limit-1;
                    }
                }

                @Override
                public String next() {
                    if (classesIndex >= classes.length) {
                        throw new NoSuchElementException();
                    }
                    classesIndex = classesIndex < 0 ? 0 : classesIndex;
                    int limit = classesFiles[classesIndex].length;
                    if (classesFileIndex >= limit-1) {
                        classesIndex += 1;
                        classesFileIndex = -1;
                        return next();
                    }
                    classesFileIndex += 1;
                    classesFileIndex = classesFileIndex < 0 ? 0 : classesFileIndex;
                    return rootDir + '/' + classes[classesIndex] + '/' + classesFiles[classesIndex][classesFileIndex];
                }
            }
        }

        public static class ImageNetClasses implements IImageClasses {
            public final String[] classes;
            public final String[] classesDesc;

            public ImageNetClasses(String rootDir) throws FileNotFoundException {
                this(new FileInputStream(new File(rootDir)));
            }
            public ImageNetClasses(InputStream inputStream) {
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
                int length = arrayList.size();
                this.classes = new String[length];
                this.classesDesc = new String[this.classes.length];
                for (int i = 0; i < length; i++) {
                    String s = arrayList.get(i);
                    String[] ss = s.split("\t");
                    this.classes[i] = ss[0];
                    this.classesDesc[i] = ss[1];
                }
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
    }

    public static class DemoAssetImage implements IDataset {
        public static final String ASSET_FILE_PATH = "img/image-400x400-rgb.jpg";
        public static final int CLASS_IN_IMAGENET = 269;  // gray wolf;

        protected final String assetFilePathCopied;
        protected final ImageNet.ImageNetClasses classesInfo;

        public DemoAssetImage(Context context, String classInfoFilePath) {
            try {
                assetFilePathCopied = Util.assetFilePath(context, ASSET_FILE_PATH);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            try {
                classesInfo = new ImageNet.ImageNetClasses(classInfoFilePath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public String get(int index) {
            return assetFilePathCopied;
        }

        @Override
        public int getClassIndex(int index) {
            return CLASS_IN_IMAGENET;
        }

        @Override
        public IImageClasses getClassesInfo() {
            return classesInfo;
        }

        @Override
        public boolean isStatusOk() {
            return !TextUtils.isEmpty(assetFilePathCopied)
                    && classesInfo != null;
        }

        @NonNull
        @Override
        public String toString() {
            return "asset file=" + ASSET_FILE_PATH
                    + ",classe=" + CLASS_IN_IMAGENET;
        }
    }

    public interface IDataset {
        /**
         * @return how many images to test;
         */
        int size();

        /**
         * @param index image index, see {@link #size()};
         * @return image file path;
         */
        String get(int index);

        /**
         * @param index image index, see {@link #size()};
         * @return the class's id(in this database, id is index) of the image;
         */
        int getClassIndex(int index);

        /**
         * @return info of the image's class;
         */
        IImageClasses getClassesInfo();

        boolean isStatusOk();
    }

    public interface IImageClasses {
        /**
         * @param index class id, index;
         * @return class's name;
         */
        String getName(int index);
        /**
         * @param index class id, index;
         * @return class's desc;
         */
        String getDesc(int index);
        /**
         * @return size of all classes;
         */
        int getSize();
    }

}
