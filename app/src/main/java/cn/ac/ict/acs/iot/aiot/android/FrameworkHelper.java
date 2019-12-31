package cn.ac.ict.acs.iot.aiot.android;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.labowenzi.commonj.JEnumUtil;

import cn.ac.ict.acs.iot.aiot.android.pytorch.PyTorchModels;
import cn.ac.ict.acs.iot.aiot.android.tflite.TfLiteModels;
import cn.ac.ict.acs.iot.aiot.android.util.LogUtil;

/**
 * Created by alanubu on 19-12-30.
 */
public class FrameworkHelper {
    public enum Type implements JEnumUtil.EnumString {
        E_PY_TORCH("PyTorch",
                new ModelHelper.Type[]{
                        ModelHelper.Type.E_MOBILE_NET,
                        ModelHelper.Type.E_RES_NET,
                }, null,
                new PyTorchModelGenerator()),
        E_CAFFE_2("Caffe2",
                new ModelHelper.Type[]{
                        ModelHelper.Type.E_MOBILE_NET,
                        ModelHelper.Type.E_RES_NET,
                }, null,
                null),
        E_TENSORFLOW_LITE("tensorflow_lite",
                new ModelHelper.Type[]{
                        ModelHelper.Type.E_MOBILE_NET_FLOAT,
                        ModelHelper.Type.E_MOBILE_NET_QUANTIZED,
                }, null,
                new TfLiteModelGenerator()),
        ;

        @NonNull
        private final String value;
        private final ModelHelper.Type[] availableModels;
        private final String[] availableModelStrings;
        private final DatasetHelper.Type[] availableDatasets;
        private final String[] availableDatasetStrings;
        private final IModelGenerator modelGenerator;

        Type(@NonNull String value, ModelHelper.Type[] availableModels, DatasetHelper.Type[] availableDatasets, IModelGenerator modelGenerator) {
            this.value = value;
            if (availableModels == null) {
                availableModels = new ModelHelper.Type[]{
                        ModelHelper.Type.E_MOBILE_NET,
                        ModelHelper.Type.E_RES_NET,
                };
            }
            this.availableModels = availableModels;
            this.availableModelStrings = new String[this.availableModels.length];
            for (int i=0; i<this.availableModelStrings.length; ++i) {
                this.availableModelStrings[i] = this.availableModels[i].getValue();
            }
            if (availableDatasets == null) {
                availableDatasets = new DatasetHelper.Type[]{
                        DatasetHelper.Type.E_DEMO,
                        DatasetHelper.Type.E_IMAGENET_2_2,
                        DatasetHelper.Type.E_IMAGENET_10_50,
                };
            }
            this.availableDatasets = availableDatasets;
            this.availableDatasetStrings = new String[this.availableDatasets.length];
            for (int i=0; i<this.availableDatasetStrings.length; ++i) {
                this.availableDatasetStrings[i] = this.availableDatasets[i].getValue();
            }
            this.modelGenerator = modelGenerator;
        }

        public ModelHelper.Type[] getAvailableModels() {
            return availableModels;
        }
        public String[] getAvailableModelStrings() {
            return availableModelStrings;
        }
        public DatasetHelper.Type[] getAvailableDatasets() {
            return availableDatasets;
        }
        public String[] getAvailableDatasetStrings() {
            return availableDatasetStrings;
        }

        public IModelGenerator getModelGenerator() {
            return modelGenerator;
        }

        @NonNull
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return super.toString() + "(" + value + ")";
        }

        public static final Type[] values;
        public static final Type[] availableFrameworks;
        public static final String[] availableFrameworkStrings;
        static {
            values = values();
            availableFrameworks = new Type[] {
                    E_PY_TORCH,
                    E_CAFFE_2,
                    E_TENSORFLOW_LITE,
            };
            availableFrameworkStrings = new String[availableFrameworks.length];
            for (int i = 0; i< availableFrameworkStrings.length; ++i) {
                availableFrameworkStrings[i] = availableFrameworks[i].getValue();
            }
        }

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

    public interface IModelGenerator {
        ModelHelper.AbstractModel genModel(Activity activity, LogUtil.Log log, ModelHelper.Type model);
    }
    public static class PyTorchModelGenerator implements IModelGenerator {
        @Override
        public ModelHelper.AbstractModel genModel(Activity activity, LogUtil.Log log, ModelHelper.Type model) {
            if (model == ModelHelper.Type.E_MOBILE_NET) {
                return new PyTorchModels.MobileNet_925(activity, log);
            } else if (model == ModelHelper.Type.E_RES_NET){
                return new PyTorchModels.ResNet18(activity, log);
            }
            return null;
        }
    }
    public static class TfLiteModelGenerator implements IModelGenerator {
        @Override
        public ModelHelper.AbstractModel genModel(Activity activity, LogUtil.Log log, ModelHelper.Type model) {
            if (model == ModelHelper.Type.E_MOBILE_NET_FLOAT) {
                return new TfLiteModels.MobileNetFloat(activity, log);
            } else if (model == ModelHelper.Type.E_MOBILE_NET_QUANTIZED){
                return new TfLiteModels.MobileNetQuantized(activity, log);
            }
            return null;
        }
    }

}
