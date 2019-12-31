package com.github.labowenzi.commonj;

import com.github.labowenzi.commonj.annotation.NotNull;

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
}
