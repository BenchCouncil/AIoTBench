package com.github.labowenzi.commonj.algorithm;

import com.github.labowenzi.commonj.annotation.Nullable;
import com.github.labowenzi.commonj.log.Log;

/**
 * math;
 * Created by hexiwen on 16-11-11.
 */

public final class CuMath {
    private static final String TAG = "CuMath";
    private CuMath() {
    }

    public static final double PI = 3.14159265358979324;
    public static final double EPSILON = 1e-5;

    public static boolean equal(double x, double y) {
        double abs = Math.abs(x - y);
        return abs < EPSILON;
    }
    public static boolean equal(float x, float y) {
        float abs = Math.abs(x - y);
        return  abs < EPSILON;
    }
    public static boolean equal(float x, double y) {
        return equal((double) x, y);
    }
    public static boolean equal(double x, float y) {
        return equal(x, (double) y);
    }

    private static void thrMinMaxArgs() {
        throw new IllegalArgumentException("wrong args: get min or max in null or empty array");
    }
    public static double min(double... args) {
        if (args == null || args.length == 0) thrMinMaxArgs();
        double min = args[0];
        for (int i=1; i<args.length; ++i) {
            min = Math.min(min, args[i]);
        }
        return min;
    }
    public static float min(float... args) {
        if (args == null || args.length == 0) thrMinMaxArgs();
        float min = args[0];
        for (int i=1; i<args.length; ++i) {
            min = Math.min(min, args[i]);
        }
        return min;
    }
    public static int min(int... args) {
        if (args == null || args.length == 0) thrMinMaxArgs();
        int min = args[0];
        for (int i=1; i<args.length; ++i) {
            min = Math.min(min, args[i]);
        }
        return min;
    }
    public static long min(long... args) {
        if (args == null || args.length == 0) thrMinMaxArgs();
        long min = args[0];
        for (int i=1; i<args.length; ++i) {
            min = Math.min(min, args[i]);
        }
        return min;
    }
    public static double max(double... args) {
        if (args == null || args.length == 0) thrMinMaxArgs();
        double max = args[0];
        for (int i=1; i<args.length; ++i) {
            max = Math.max(max, args[i]);
        }
        return max;
    }
    public static float max(float... args) {
        if (args == null || args.length == 0) thrMinMaxArgs();
        float max = args[0];
        for (int i=1; i<args.length; ++i) {
            max = Math.max(max, args[i]);
        }
        return max;
    }
    public static int max(int... args) {
        if (args == null || args.length == 0) thrMinMaxArgs();
        int max = args[0];
        for (int i=1; i<args.length; ++i) {
            max = Math.max(max, args[i]);
        }
        return max;
    }
    public static long max(long... args) {
        if (args == null || args.length == 0) thrMinMaxArgs();
        long max = args[0];
        for (int i=1; i<args.length; ++i) {
            max = Math.max(max, args[i]);
        }
        return max;
    }

    public static boolean equal(boolean l, boolean r) {
        return (l && r) || (!l && !r);
    }

    public static boolean equal(int v1, int v2) {
        return v1 == v2;
    }
    public static boolean equal(int v1, Integer v2) {
        return v2 != null && v2 == v1;
    }
    public static boolean equal(int v1, long v2) {
        return v1 == v2;
    }
    public static boolean equal(int v1, Long v2) {
        return v2 != null && v2 == v1;
    }
    public static boolean equal(Integer v1, int v2) {
        return v1 != null && v1 == v2;
    }
    public static boolean equal(Integer v1, Integer v2) {
        if (v1 == null && v2 == null) return true;
        if (v1 == null || v2 == null) return false;
        return v1.intValue() == v2.intValue();
    }
    public static boolean equal(Integer v1, long v2) {
        return v1 != null && v1 == v2;
    }
    public static boolean equal(Integer v1, Long v2) {
        if (v1 == null && v2 == null) return true;
        if (v1 == null || v2 == null) return false;
        return v1.intValue() == v2.longValue();
    }
    public static boolean equal(long v1, int v2) {
        return v1 == v2;
    }
    public static boolean equal(long v1, Integer v2) {
        return v2 != null && v2 == v1;
    }
    public static boolean equal(long v1, long v2) {
        return v1 == v2;
    }
    public static boolean equal(long v1, Long v2) {
        return v2 != null && v1 == v2;
    }
    public static boolean equal(Long v1, int v2) {
        return v1 != null && v1 == v2;
    }
    public static boolean equal(Long v1, Integer v2) {
        if (v1 == null && v2 == null) return true;
        if (v1 == null || v2 == null) return false;
        return v1.longValue() == v2.intValue();
    }
    public static boolean equal(Long v1, long v2) {
        return v1 != null && v1 == v2;
    }
    public static boolean equal(Long v1, Long v2) {
        if (v1 == null && v2 == null) return true;
        if (v1 == null || v2 == null) return false;
        return v1.longValue() == v2.longValue();
    }

    public static int compare(int vl, int vr) {
        int v1 = vl;
        int v2 = vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(int vl, Integer vr) {
        int v1 = vl;
        int v2 = vr == null ? 0 : vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(int vl, long vr) {
        int v1 = vl;
        long v2 = vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(int vl, Long vr) {
        int v1 = vl;
        long v2 = vr == null ? 0 : vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(Integer vl, int vr) {
        int v1 = vl == null ? 0 : vl;
        int v2 = vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(Integer vl, Integer vr) {
        int v1 = vl == null ? 0 : vl;
        int v2 = vr == null ? 0 : vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(Integer vl, long vr) {
        int v1 = vl == null ? 0 : vl;
        long v2 = vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(Integer vl, Long vr) {
        int v1 = vl == null ? 0 : vl;
        long v2 = vr == null ? 0 : vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(long vl, int vr) {
        long v1 = vl;
        int v2 = vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(long vl, Integer vr) {
        long v1 = vl;
        int v2 = vr == null ? 0 : vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(long vl, long vr) {
        long v1 = vl;
        long v2 = vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(long vl, Long vr) {
        long v1 = vl;
        long v2 = vr == null ? 0 : vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(Long vl, int vr) {
        long v1 = vl == null ? 0 : vl;
        int v2 = vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(Long vl, Integer vr) {
        long v1 = vl == null ? 0 : vl;
        int v2 = vr == null ? 0 : vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(Long vl, long vr) {
        long v1 = vl == null ? 0 : vl;
        long v2 = vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }
    public static int compare(Long vl, Long vr) {
        long v1 = vl == null ? 0 : vl;
        long v2 = vr == null ? 0 : vr;
        return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
    }

    @Nullable
    public static Integer parseInt(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            Log.e(TAG, "math", e);
            return null;
        }
    }

    @Nullable
    public static Long parseLong(String string) {
        try {
            return Long.parseLong(string);
        } catch (NumberFormatException e) {
            Log.e(TAG, "math", e);
            return null;
        }
    }

    @Nullable
    public static Float parseFloat(String string) {
        try {
            return Float.parseFloat(string);
        } catch (NumberFormatException e) {
            Log.e(TAG, "math", e);
            return null;
        }
    }

    @Nullable
    public static Double parseDouble(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            Log.e(TAG, "math", e);
            return null;
        }
    }
}
