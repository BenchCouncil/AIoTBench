package com.github.labowenzi.commonj;

import com.github.labowenzi.commonj.annotation.NotNull;
import com.github.labowenzi.commonj.annotation.Nullable;

/**
 * Created by alanubu on 19-12-31.
 */
public class JEnumUtil {
    public interface EnumString {
        @NotNull
        String getValue();
    }

    public interface EnumInt {
        int getValue();
    }


    @Nullable
    public static <T extends Enum<T>> T getByLowerCase(@Nullable String value, T[] values) {
        if (value == null) {
            return null;
        }
        for (T item : values) {
            if (item.name().toLowerCase().equals(value)) {
                return item;
            }
        }
        return null;
    }
    @NotNull
    public static <T extends Enum<T>> T getByLowerCase(@Nullable String value, T[] values, @NotNull T defaultValue) {
        T v = getByLowerCase(value, values);
        return v == null ? defaultValue : v;
    }
}
