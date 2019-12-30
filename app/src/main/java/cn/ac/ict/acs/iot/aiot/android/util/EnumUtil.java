package cn.ac.ict.acs.iot.aiot.android.util;

import androidx.annotation.NonNull;

/**
 * enum util;
 * Created by hexiwen on 17-3-30.
 */

public class EnumUtil {
    public interface EnumString {
        @NonNull
        String getValue();
    }

    public interface EnumInt {
        int getValue();
    }
}
