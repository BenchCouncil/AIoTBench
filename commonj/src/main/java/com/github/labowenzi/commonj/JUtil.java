package com.github.labowenzi.commonj;

import com.github.labowenzi.commonj.annotation.Nullable;

import java.util.Collection;

/**
 * Created by alanubu on 18-8-1.
 */
public class JUtil {

    private JUtil() {
    }

    public static boolean isEqual(String a, String b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }
    public static boolean isEqualIgnoreCase(String a, String b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else {
            return a.equalsIgnoreCase(b);
        }
    }

    public static boolean isEmpty(@Nullable Object[] arr) {
        return arr == null || arr.length == 0;
    }
    public static boolean isEmpty(@Nullable byte[] arr) {
        return arr == null || arr.length == 0;
    }
    public static boolean isEmpty(@Nullable char[] arr) {
        return arr == null || arr.length == 0;
    }
    public static boolean isEmpty(@Nullable short[] arr) {
        return arr == null || arr.length == 0;
    }
    public static boolean isEmpty(@Nullable int[] arr) {
        return arr == null || arr.length == 0;
    }
    public static boolean isEmpty(@Nullable long[] arr) {
        return arr == null || arr.length == 0;
    }
    public static boolean isEmpty(@Nullable float[] arr) {
        return arr == null || arr.length == 0;
    }
    public static boolean isEmpty(@Nullable double[] arr) {
        return arr == null || arr.length == 0;
    }
    public static boolean isEmpty(@Nullable boolean[] arr) {
        return arr == null || arr.length == 0;
    }
    public static boolean isEmpty(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    public static boolean isEmpty(@Nullable String str) {
        return str == null || str.length() <= 0;
    }

    public static boolean inArray(int item, @Nullable int[] items) {
        return indexInArray(item, items) >= 0;
    }
    public static int indexInArray(int item, @Nullable int[] items) {
        if (items == null) {
            return -1;
        }
        for (int i = 0; i < items.length; i++) {
            if (item == items[i]) {
                return i;
            }
        }
        return -1;
    }
    public static boolean inArray(long item, @Nullable long[] items) {
        return indexInArray(item, items) >= 0;
    }
    public static int indexInArray(long item, @Nullable long[] items) {
        if (items == null) {
            return -1;
        }
        for (int i = 0; i < items.length; i++) {
            if (item == items[i]) {
                return i;
            }
        }
        return -1;
    }
    public static boolean inArray(Object obj, Object[] arr) {
        return indexInArray(obj, arr) >= 0;
    }
    public static int indexInArray(Object obj, Object[] arr) {
        if (arr == null) {
            return -1;
        }
        if (obj == null) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == null) return i;
            }
        } else {
            for (int i = 0; i < arr.length; i++) {
                if (obj.equals(arr[i])) return i;
            }
        }
        return -1;
    }
    public static boolean inArrayNaive(Object obj, Object[] arr) {
        return indexInArrayNaive(obj, arr) >= 0;
    }
    public static int indexInArrayNaive(Object obj, Object[] arr) {
        if (arr == null) {
            return -1;
        }
        if (obj == null) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == null) return i;
            }
        } else {
            for (int i = 0; i < arr.length; i++) {
                if (obj == arr[i]) return i;
            }
        }
        return -1;
    }

    public static class Test {
        public static void main(String[] args) {
        }
    }
}
