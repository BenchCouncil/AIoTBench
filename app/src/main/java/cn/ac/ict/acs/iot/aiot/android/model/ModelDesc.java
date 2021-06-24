package cn.ac.ict.acs.iot.aiot.android.model;

import android.util.Log;

import androidx.annotation.Nullable;

import com.github.labowenzi.commonj.JEnumUtil;
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

        /**
         * how to convert a src bitmap to dest bitmap for the model;
         */
        private String bitmap_convert_method;

        /**
         * rgb type of the dest bitmap;
         */
        private String bitmap_rgb_type;

        /**
         * dest bitmap's width and height;
         * [width, height]
         */
        private int[] bitmap_convert_size;

        public String getName() {
            return name;
        }

        public String getBitmap_convert_method() {
            return bitmap_convert_method;
        }
        public BitmapConvertMethod getBitmapConvertMethod() {
            return BitmapConvertMethod.get(bitmap_convert_method);
        }

        //public String getBitmap_rgb_type() {
        //    return bitmap_rgb_type;
        //}
        public BitmapRgbType getBitmapRgbType() {
            return BitmapRgbType.get(bitmap_rgb_type);
        }
        public boolean needToBgr() {
            return getBitmapRgbType() == ModelDesc.BaseModelDesc.BitmapRgbType.BGR;
        }

        public int[] getBitmap_convert_size() {
            return bitmap_convert_size;
        }

        public enum BitmapConvertMethod {
            DEFAULT,  // src --> crop or pad --> resize --> dest;
            COPY,  // src --> copy --> dest;
            ;

            @Nullable
            public static BitmapConvertMethod get(@Nullable String value) {
                return JEnumUtil.getByLowerCase(value, values());
            }
        }
        public enum BitmapRgbType {
            DEFAULT,  // RGB, just copy;
            BGR,  // ARGB --> ABGR
            ;

            @Nullable
            public static BitmapRgbType get(@Nullable String value) {
                return JEnumUtil.getByLowerCase(value, values());
            }
        }
    }

    public static class Caffe2 extends BaseModelDesc {
        public static final String FRAMEWORK = Model.FRAMEWORK_CAFFE2;
        private String dir;
        private String init_net_pb;
        private String predict_net_pb;
        private float[] norm_mean;
        private float[] norm_std_dev;
        private String labels;

        public String getDir() {
            return dir;
        }

        public String getInit_net_pb() {
            return init_net_pb;
        }
        public String getInit_net_pb_filepath(Model.ModelDir dir) {
            return JUtil.isEmpty(init_net_pb) ? null : dir.getDirPath() + "/" + "caffe2" + "/" + this.dir + "/" + init_net_pb;
        }

        public String getPredict_net_pb() {
            return predict_net_pb;
        }
        public String getPredict_net_pb_filepath(Model.ModelDir dir) {
            return JUtil.isEmpty(predict_net_pb) ? null : dir.getDirPath() + "/" + "caffe2" + "/" + this.dir + "/" + predict_net_pb;
        }

        public float[] getNorm_mean() {
            return norm_mean;
        }

        public float[] getNorm_std_dev() {
            return norm_std_dev;
        }

        public String getLabels() {
            return labels;
        }
        public String getLabels_filepath(Model.ModelDir dir) {
            return JUtil.isEmpty(labels) ? null : dir.getDirPath() + "/" + "caffe2" + "/" + this.dir + "/" + labels;
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
            return JUtil.isEmpty(net_pt) ? null : dir.getDirPath() + "/" + "pytorch" + "/" + this.dir + "/" + net_pt;
        }
    }

    public static class Tflite extends BaseModelDesc {
        public static final String FRAMEWORK = Model.FRAMEWORK_TFLITE;
        private String dir;
        private String net_tflite;
        private String quantization;
        private float[] norm_mean;
        private float[] norm_std_dev;
        private String labels;

        public String getDir() {
            return dir;
        }

        public String getNet_tflite() {
            return net_tflite;
        }

        public String getNet_tflite_filepath(Model.ModelDir dir, String quantName) {
            if (quantName.equals("no"))
                return JUtil.isEmpty(net_tflite) ? null : dir.getDirPath() + "/" + "tflite" + "/" + this.dir + "/" + net_tflite;
            else if (quantName.equals("f16"))
                return JUtil.isEmpty(net_tflite) ? null : dir.getDirPath() + "/" + "tflite_quant_f16" + "/" + this.dir + "/" + net_tflite;
            else if (quantName.equals("full_int")){
                return JUtil.isEmpty(net_tflite) ? null : dir.getDirPath() + "/" + "tflite_quant_int" + "/" + this.dir + "/" + net_tflite;}
            else if (quantName.equals("DR")){
                return JUtil.isEmpty(net_tflite) ? null : dir.getDirPath() + "/" + "tflite_quant_dr" + "/" + this.dir + "/" + net_tflite;
            }
            else return null;
        }

        public String getQuantization() {
            return quantization;
        }

        public float[] getNorm_mean() {
            return norm_mean;
        }

        public float[] getNorm_std_dev() {
            return norm_std_dev;
        }

        public String getLabels() {
            return labels;
        }
        public String getLabels_filepath(Model.ModelDir dir) {
            return JUtil.isEmpty(labels) ? null : dir.getDirPath() + "/" + "tflite" + "/" + this.dir + "/" + labels;
        }
    }
}
