package cn.ac.ict.acs.iot.aiot.android.model;

import com.github.labowenzi.commonj.JUtil;

import java.util.List;

/**
 * json model;
 * need to be in proguard-rules.pro;
 * Created by alanubu on 20-1-19.
 */
public class ModelDesc {

    private List<Caffe2> caffe2;
    private List<Pytorch> pytorch;
    private List<Tflite> tflite;

    public List<Caffe2> getCaffe2() {
        return caffe2;
    }

    public List<Pytorch> getPytorch() {
        return pytorch;
    }

    public List<Tflite> getTflite() {
        return tflite;
    }

    public static  <T extends BaseModelDesc> String[] getNames(List<T> models) {
        if (JUtil.isEmpty(models)) {
            return null;
        }
        String[] names = new String[models.size()];
        for (int i=0; i<names.length; ++i) {
            names[i] = models.get(i).getName();
        }
        return names;
    }
    public static  <T extends BaseModelDesc> T getModel(List<T> models, String name) {
        if (JUtil.isEmpty(models) || JUtil.isEmpty(name)) {
            return null;
        }
        for (T m : models) {
            if (name.equals(m.getName())) {
                return m;
            }
        }
        return null;
    }

    public static class BaseModelDesc {
        private String name;

        public String getName() {
            return name;
        }
    }

    public static class Caffe2 extends BaseModelDesc {
        public static final String FRAMEWORK = Model.FRAMEWORK_CAFFE2;
        private String dir;
        private String init_net_pb;
        private String predict_net_pb;

        public String getDir() {
            return dir;
        }

        public String getInit_net_pb() {
            return init_net_pb;
        }
        public String getInit_net_pb_filepath(Model.ModelDir dir) {
            return dir.getDirPath() + "/" + "caffe2" + "/" + this.dir + "/" + init_net_pb;
        }

        public String getPredict_net_pb() {
            return predict_net_pb;
        }
        public String getPredict_net_pb_filepath(Model.ModelDir dir) {
            return dir.getDirPath() + "/" + "caffe2" + "/" + this.dir + "/" + predict_net_pb;
        }
    }

    public static class Pytorch extends BaseModelDesc {
        public static final String FRAMEWORK = Model.FRAMEWORK_PYTORCH;
        private String dir;
        private String net_pt;

        public String getDir() {
            return dir;
        }

        public String getNet_pt() {
            return net_pt;
        }
        public String getNet_pt_filepath(Model.ModelDir dir) {
            return dir.getDirPath() + "/" + "pytorch" + "/" + this.dir + "/" + net_pt;
        }
    }

    public static class Tflite extends BaseModelDesc {
        public static final String FRAMEWORK = Model.FRAMEWORK_TFLITE;
        private String dir;
        private String net_tflite;
        private String quantization;
        private String labels;

        public String getDir() {
            return dir;
        }

        public String getNet_tflite() {
            return net_tflite;
        }
        public String getNet_tflite_filepath(Model.ModelDir dir) {
            return dir.getDirPath() + "/" + "tflite" + "/" + this.dir + "/" + net_tflite;
        }

        public String getQuantization() {
            return quantization;
        }

        public String getLabels() {
            return labels;
        }
        public String getLabels_filepath(Model.ModelDir dir) {
            return dir.getDirPath() + "/" + "tflite" + "/" + this.dir + "/" + labels;
        }
    }
}
