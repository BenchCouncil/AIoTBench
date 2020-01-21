package cn.ac.ict.acs.iot.aiot.android.model;

import android.app.Activity;
import android.os.Environment;

import androidx.annotation.NonNull;

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

/**
 * Created by alanubu on 20-1-20.
 */
public class Model {
    private static final String TAG = "model";

    private static Model instance = null;
    @NonNull
    public static Model getInstance() {
        if (instance == null) {
            instance = new Model();
        }
        return instance;
    }
    public static final String FRAMEWORK_CAFFE2 = "caffe2";
    public static final String FRAMEWORK_PYTORCH = "pytorch";
    public static final String FRAMEWORK_TFLITE = "tflite";
    public static final String FRAMEWORK_DEFAULT = FRAMEWORK_PYTORCH;

    public static final String[] FRAMEWORKS = {
            FRAMEWORK_PYTORCH,
            FRAMEWORK_CAFFE2,
            FRAMEWORK_TFLITE,
    };

    public static final String DIR_FILE = "aiot/models";
    public static final String DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + DIR_FILE;

    private String[] dirs;
    private ModelDir modelDir;

    public Model() {
        File modelDir = new File(DIR_PATH);
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
            return;
        } else {
            Log.e(TAG, "no dir or dirs");
        }
        if (JUtil.inArray(dir, dirs)) {
            String dirPath = DIR_PATH + "/" + dir;
            this.modelDir = new ModelDir(dirPath);
        } else {
            Log.e(TAG, "no dir in dirs");
        }
    }

    public String getDefaultModelName(String framework) {
        return modelDir == null ? null : modelDir.getInfo(framework).defaultModelName;
    }

    public AbstractModel getModel(Activity activity, LogUtil.Log log, String framework, String model) {
        return modelDir.getInfo(framework).generator.genModel(activity, log, modelDir, model);
    }

    public static class ModelDir {
        public static final String MODELS_FILE = "models.json";

        private final String dirPath;
        private final ModelDesc modelDesc;

        private Info caffe2;
        private Info pytorch;
        private Info tflite;

        public ModelDir(String dirPath) {
            this.dirPath = dirPath;
            String filePath = dirPath + "/" + MODELS_FILE;
            String json = JIoUtil.readJson(filePath);
            ModelDesc desc = JJsonUtils.fromJson(json, ModelDesc.class);
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
            public String[] names = null;
            public IModelGenerator generator = null;
        }

        public interface IModelGenerator {
            AbstractModel genModel(Activity activity, LogUtil.Log log, ModelDir dir, String modelName);
        }
        public static class PyTorchModelGenerator implements IModelGenerator {
            @Override
            public AbstractModel genModel(Activity activity, LogUtil.Log log, ModelDir dir, String modelName) {
                ModelDesc.Pytorch modelD = ModelDesc.getModel(dir.getModelDesc().getPytorch(), modelName);
                if (modelD == null) {
                    return null;
                }
                return PyTorchModels.newModel(log, dir, modelD);
            }
        }
        public static class Caffe2ModelGenerator implements IModelGenerator {
            @Override
            public AbstractModel genModel(Activity activity, LogUtil.Log log, ModelDir dir, String modelName) {
                ModelDesc.Caffe2 modelD = ModelDesc.getModel(dir.getModelDesc().getCaffe2(), modelName);
                if (modelD == null) {
                    return null;
                }
                return Caffe2Models.newModel(log, dir, modelD);
            }
        }
        public static class TfLiteModelGenerator implements IModelGenerator {
            @Override
            public AbstractModel genModel(Activity activity, LogUtil.Log log, ModelDir dir, String modelName) {
                ModelDesc.Tflite modelD = ModelDesc.getModel(dir.getModelDesc().getTflite(), modelName);
                if (modelD == null) {
                    return null;
                }
                return TfLiteModels.newModel(log, dir, modelD);
            }
        }
    }
}
