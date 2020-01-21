package cn.ac.ict.acs.iot.aiot.android.dataset;

import java.util.List;

/**
 * json model;
 * need to be in proguard-rules.pro;
 * Created by alanubu on 20-1-19.
 */
public class DatasetDesc {

    private ImageNet imagenet;

    public ImageNet getImagenet() {
        return imagenet;
    }

    public static class ImageNet {
        private String classes_info;
        private List<Dataset> datasets;

        public String getClasses_info() {
            return classes_info;
        }

        public List<Dataset> getDatasets() {
            return datasets;
        }

        public static class Dataset {
            private String name;
            private String dir;

            public String getName() {
                return name;
            }

            public String getDir() {
                return dir;
            }
        }
    }
}
