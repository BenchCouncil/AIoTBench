package cn.ac.ict.acs.iot.aiot.android.dataset;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.IOException;

import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 20-1-19.
 */
public class DemoImage implements IDataset {
    public static final String NAME = "demo";
    public static final String[] ASSET_FILE_PATHS = {
            "img/image-400x400-rgb.jpg",
            "img/dog.jpg",
    };
    public static final int[] CLASS_IN_IMAGENETS = {
            269,  // gray wolf;
            258,  // Samoyed;
    };
    public static final String[][] CLASS_INFO_IN_IMAGENETS = {
            {
                "n02114367",
                "timber wolf, grey wolf, gray wolf, Canis lupus",
            },
            {
                "n02111889",
                "Samoyed, Samoyede",
            },
    };

    protected final int assetIndex = 0;

    protected final String assetFilePathCopied;
    protected final ImageClasses classesInfo;

    public DemoImage(Context context) {
        try {
            assetFilePathCopied = Util.assetFilePath(context, ASSET_FILE_PATHS[assetIndex]);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        classesInfo = new ImageClasses(assetIndex);
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
        return CLASS_IN_IMAGENETS[assetIndex];
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
        return "asset file=" + ASSET_FILE_PATHS[assetIndex]
                + ",classe=" + CLASS_IN_IMAGENETS[assetIndex];
    }

    public static class ImageClasses implements IImageClasses {
        private final int assetIndex;

        public ImageClasses(int assetIndex) {
            this.assetIndex = assetIndex;
        }

        @Override
        public String getName(int index) {
            return index == assetIndex ? CLASS_INFO_IN_IMAGENETS[assetIndex][0] : null;
        }

        @Override
        public String getDesc(int index) {
            return index == assetIndex ? CLASS_INFO_IN_IMAGENETS[assetIndex][1] : null;
        }

        @Override
        public int getSize() {
            return 1000;  // imageNet size;
        }
    }
}
