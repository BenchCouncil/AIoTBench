package cn.ac.ict.acs.iot.aiot.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.ac.ict.acs.iot.aiot.android.util.EnumUtil;

/**
 * Created by alanubu on 19-12-25.
 */
public class ModelHelper {

    // java enum String 'Type{mobile_net, res_net, }'
    public enum Type implements EnumUtil.EnumString {
        E_MOBILE_NET("mobile_net"),
        E_RES_NET("res_net"),

        E_MOBILE_NET_FLOAT("mobile_net_float"),
        E_MOBILE_NET_QUANTIZED("mobile_net_quantized"),
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

        @Override
        public String toString() {
            return super.toString() + "(" + value + ")";
        }

        public static final Type[] values;

        static {
            values = values();
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
