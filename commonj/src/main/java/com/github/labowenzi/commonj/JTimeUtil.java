package com.github.labowenzi.commonj;

/**
 * Created by alanubu on 18-8-22.
 */
public class JTimeUtil {

    private JTimeUtil() {
    }

    /**
     * 20180822-23:00:00 大概是 1534978800000，二进制表示为 0x16563DDB980，二进制为41位，
     * 不是32位的 int 或 float 精度能接受的。
     */
    public static double msToSDouble(long timeMs) {
        return timeMs / 1000.0d;
    }

    public static String timeElapsedHours(long timeMs) {
        long ori = timeMs;
        long ms = ori % 1000;
        ori /= 1000;
        long s = ori % 60;
        ori /= 60;
        long m = ori % 60;
        ori /= 60;
        long h = ori;
        return h + "H" + m + "m" + s + "s" + ms;
    }
}
