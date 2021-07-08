package cn.ac.ict.acs.iot.aiot.android.dataset;

import java.util.AbstractList;
import java.util.List;

/**
 * json model;
 * need to be in proguard-rules.pro;
 * Created by alanubu on 20-1-19.
 */
public class DatasetDesc {//coordinate with the json file

    private ImageNet imagenet;
    private Coco coco;

    public Coco getCoco() {
        return coco;
    }

    public ImageNet getImagenet() {
        return imagenet;
    }

    public abstract static class Desc{
        private String classes_info;
        private String task;

        private List<Dataset> datasets;

        public String getTask() {
            return task;
        }
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
    public static class ImageNet extends Desc {}//usage:DatasetDesc.ImageNet imageNetDesc

    public static class Coco extends Desc{}//usage:DatasetDesc.Coco cocoDesc
}
