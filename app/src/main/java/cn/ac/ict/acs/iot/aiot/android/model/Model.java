package cn.ac.ict.acs.iot.aiot.android.model;

import android.app.Activity;
import android.os.Environment;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.labowenzi.commonj.JEnumUtil;
import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.JJsonUtils;
import com.github.labowenzi.commonj.JUtil;
import com.github.labowenzi.commonj.log.Log;

import java.io.File;
import java.util.List;

import cn.ac.ict.acs.iot.aiot.android.caffe2.Caffe2Models;
import cn.ac.ict.acs.iot.aiot.android.pytorch.PyTorchModels;
import cn.ac.ict.acs.iot.aiot.android.tflite.TfLiteModels;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;
import cn.ac.ict.acs.iot.aiot.android.util.Util;

/**
 * Created by alanubu on 20-1-20.
 */
public class Model {
    private static final String TAG = "model";

    private static Model instance = null;
    @NonNull
    public static Model getInstance() {
        if (instance == null) {
            instance = getInstance(DIR_PATH);
        }
        return instance;
    }
    @NonNull
    public static Model getInstance(String dirPath) {
        return new Model(dirPath);
    }
    public static Model resetInstance() {
        instance = null;
        return getInstance();
    }
    public static final String FRAMEWORK_CAFFE2 = "caffe2";
    public static final String FRAMEWORK_PYTORCH = "pytorch";
    public static final String FRAMEWORK_TFLITE = "tflite";
    public static final String FRAMEWORK_DEFAULT = FRAMEWORK_PYTORCH;
    public static final String Quant_f16 = "f16";
    public static final String Quant_int = "full_int";
    public static final String Quant_DR = "DR";
    public static final String Quant_DEFAULT = Quant_DR;

    public static final String[] FRAMEWORKS = {//todo:change temporarily
            FRAMEWORK_TFLITE,
            FRAMEWORK_PYTORCH,
            FRAMEWORK_CAFFE2,
    };
    public static final Device[][] FRAMEWORKS_SUPPORTED_DEVICES = {
            {Device.CPU, Device.NNAPI, Device.GPU},  // tflite  // todo: can gpu run or not;
//            {Device.CPU, Device.NNAPI},  // tflite
            {Device.CPU},  // pytorch
            {Device.CPU}   // caffe2
    };
    public static final String[][] FRAMEWORKS_SUPPORTED_Quants = {
            {"no", Quant_DR, Quant_int, Quant_f16},  // tflite
            {"no"},  // pytorch
            {"no"},  // caffe2
    };
    public static final String[] Quants = {
            "no",
            Quant_f16,
            Quant_int,
            Quant_DR
    };

    public static String[] getSupportedQuant(String framework) {
        int index = JUtil.indexInArray(framework, FRAMEWORKS);
        if (index < 0) {
            return null;
        }
        return FRAMEWORKS_SUPPORTED_Quants[index];
    }

    public static Device[] getSupportedDevices(String framework) {
        int index = JUtil.indexInArray(framework, FRAMEWORKS);
        if (index < 0) {
            return null;
        }
        return FRAMEWORKS_SUPPORTED_DEVICES[index];
    }
    @NonNull
    public static Device getDefaultSupportedDevice(String framework) {
        Device[] devices = getSupportedDevices(framework);
        return JUtil.isEmpty(devices) ? Device.CPU : devices[0];
    }

    /** The runtime device type used for executing classification. */
    public enum Device {
        CPU,
        NNAPI,
        GPU,
        ;

        @Nullable
        public static Device get(@Nullable String value) {
            return JEnumUtil.getByLowerCase(value, values());
        }
        @Nullable
        public static String getName(@Nullable Device value) {
            return value == null ? null : value.name().toLowerCase();
        }
        @Nullable
        public static String[] getNames(@Nullable Device[] values) {
            if (values == null) {
                return null;
            }
            String[] res = new String[values.length];
            for (int i=0; i<values.length; ++i) {
                res[i] = getName(values[i]);
            }
            return res;
        }
    }

    public static final String DIR_FILE = "aiot/models";
    public static final String DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + DIR_FILE;

    private final String dirPath;

    private String[] dirs;
    private ModelDir modelDir;

    public Model(String dirPath) {
        this.dirPath = dirPath;
        File modelDir = new File(dirPath);
        if (modelDir.exists() && modelDir.isDirectory()) {
            dirs = modelDir.list();
        } else {
            dirs = null;
        }
        this.modelDir = null;
    }

    public String[] getDirs() {
        return dirs;
    }

    public ModelDir getModelDir() {
        return modelDir;
    }

    public void setModelDir(String dir) {
        if (JUtil.isEmpty(dir) || JUtil.isEmpty(dirs)) {
            Log.e(TAG, "no dir or dirs");
            return;
        }
        if (JUtil.inArray(dir, dirs)) {
            String dirPath = this.dirPath + "/" + dir;
            this.modelDir = new ModelDir(dirPath);
        } else {
            Log.e(TAG, "no dir in dirs");
        }
    }

    public String getDefaultModelName(String framework) {
        return modelDir == null ? null : modelDir.getInfo(framework).defaultModelName;
    }

    public AbstractModel getModel(Activity activity, LogUtil.Log log, String framework, String quantName, String model, Device device) {
        return modelDir.getInfo(framework).generator.genModel(activity, log, modelDir, quantName, model, device);
    }

    public static class ModelDir {
        public static final String MODELS_FILE = "models.json";

        private final String dirPath;// /storage/emulated/0/aiot/download/20201203_model_6_dataset_100x5/aiot/models/20201105_novgg
        private final ModelDesc modelDesc;

        private Info caffe2;
        private Info pytorch;
        private Info tflite;

        public ModelDir(String dirPath) {
            this.dirPath = dirPath;
            this.modelDesc = JJsonUtils.fromJson(JIoUtil.readJson(dirPath + "/" + MODELS_FILE), ModelDesc.class);
            init();
        }

        private void init() {
            caffe2 = new Info();
            pytorch = new Info();
            tflite = new Info();
            List<ModelDesc.Caffe2> caffe2L = modelDesc.getCaffe2();
            if (!JUtil.isEmpty(caffe2L)) {
                caffe2.defaultModelName = caffe2L.get(0).getName();
                caffe2.names = ModelDesc.getNames(caffe2L);
                caffe2.generator = new Caffe2ModelGenerator();
            }
            List<ModelDesc.Pytorch> pytorchL = modelDesc.getPytorch();
            if (!JUtil.isEmpty(pytorchL)) {
                pytorch.defaultModelName = pytorchL.get(0).getName();
                pytorch.names = ModelDesc.getNames(pytorchL);
                pytorch.generator = new PyTorchModelGenerator();
            }
            List<ModelDesc.Tflite> tfliteL = modelDesc.getTflite();
            if (!JUtil.isEmpty(tfliteL)) {
                tflite.defaultModelName = tfliteL.get(0).getName();
                tflite.names = ModelDesc.getNames(tfliteL);
                tflite.object_detection_names=ModelDesc.getTaskNames(tfliteL,"object_detection");
                tflite.image_classifiction_names=ModelDesc.getTaskNames(tfliteL,"image_classification");
                tflite.super_resolution_names=ModelDesc.getTaskNames(tfliteL,"super_resolution");

                tflite.generator = new TfLiteModelGenerator();
            }
        }

        public String getDirPath() {
            return dirPath;
        }

        public ModelDesc getModelDesc() {
            return modelDesc;
        }

        public Info getCaffe2() {
            return caffe2;
        }

        public Info getPytorch() {
            return pytorch;
        }

        public Info getTflite() {
            return tflite;
        }

        public Info getInfo(String framework) {
            if (JUtil.isEmpty(framework)) {
                return null;
            }
            switch (framework) {
                case FRAMEWORK_CAFFE2: {
                    return caffe2;
                }
                case FRAMEWORK_PYTORCH: {
                    return pytorch;
                }
                case FRAMEWORK_TFLITE: {
                    return tflite;
                }
                default: {
                    return null;
                }
            }
        }

        public static class Info {
            public String defaultModelName = null;
            public String[] names = null;//all model names in model.json
            public String[] object_detection_names = null;
            public String[] image_classifiction_names = null;
            public String[] super_resolution_names = null;
            public IModelGenerator generator = null;
        }

        public interface IModelGenerator {
            AbstractModel genModel(Activity activity, LogUtil.Log log, ModelDir dir, String quantName, String modelName, Device device);
        }
        public static class PyTorchModelGenerator implements IModelGenerator {
            @Override
            public AbstractModel genModel(Activity activity, LogUtil.Log log, ModelDir dir, String quantName, String modelName, Device device) {
                ModelDesc.Pytorch modelD = ModelDesc.getModel(dir.getModelDesc().getPytorch(), modelName);
                if (modelD == null) {
                    return null;
                }
                if (!JUtil.inArray(device, getSupportedDevices(FRAMEWORK_PYTORCH))) {
                    Util.showToast("device not support", activity);
                    return null;
                }
                return PyTorchModels.newModel(log, dir, modelD);
            }
        }
        public static class Caffe2ModelGenerator implements IModelGenerator {
            @Override
            public AbstractModel genModel(Activity activity, LogUtil.Log log, ModelDir dir, String quantName, String modelName, Device device) {
                ModelDesc.Caffe2 modelD = ModelDesc.getModel(dir.getModelDesc().getCaffe2(), modelName);
                if (modelD == null) {
                    return null;
                }
                if (!JUtil.inArray(device, getSupportedDevices(FRAMEWORK_CAFFE2))) {
                    Util.showToast("device not support", activity);
                    return null;
                }
                return Caffe2Models.newModel(log, dir, modelD);
            }
        }
        public static class TfLiteModelGenerator implements IModelGenerator {
            @Override
            public AbstractModel genModel(Activity activity, LogUtil.Log log, ModelDir dir, String quantName, String modelName, Device device) {
                ModelDesc.Tflite modelD = ModelDesc.getModel(dir.getModelDesc().getTflite(), modelName);
                if (modelD == null) {
                    return null;
                }
                if (!JUtil.inArray(device, getSupportedDevices(FRAMEWORK_TFLITE))) {
                    Util.showToast("device not support", activity);
                    return null;
                }
                return TfLiteModels.newModel(log, dir, quantName, modelD, device);
            }
        }
    }
}
