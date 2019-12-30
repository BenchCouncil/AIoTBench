package cn.ac.ict.acs.iot.aiot.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.ac.ict.acs.iot.aiot.android.util.EnumUtil;

/**
 * Created by alanubu on 19-12-30.
 */
public class FrameworkHelper {
    public enum Type implements EnumUtil.EnumString {
        E_PY_TORCH("PyTorch",
                new ModelHelper.Type[]{
                        ModelHelper.Type.E_MOBILE_NET,
                        ModelHelper.Type.E_RES_NET,
                }, null),
        E_CAFFE_2("Caffe2",
                new ModelHelper.Type[]{
                        ModelHelper.Type.E_MOBILE_NET,
                        ModelHelper.Type.E_RES_NET,
                }, null),
        E_TENSORFLOW_LITE("tensorflow_lite",
                new ModelHelper.Type[]{
                        ModelHelper.Type.E_MOBILE_NET_FLOAT,
                        ModelHelper.Type.E_MOBILE_NET_QUANTIZED,
                }, null),
        ;

        @NonNull
        private final String value;
        private final ModelHelper.Type[] availableModels;
        private final String[] availableModelStrings;
        private final DatasetHelper.Type[] availableDatasets;
        private final String[] availableDatasetStrings;

        Type(@NonNull String value, ModelHelper.Type[] availableModels, DatasetHelper.Type[] availableDatasets) {
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

}
