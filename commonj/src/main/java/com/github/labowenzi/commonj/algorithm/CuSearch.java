package com.github.labowenzi.commonj.algorithm;

import com.github.labowenzi.commonj.JUtil;
import com.github.labowenzi.commonj.annotation.NotNull;

/**
 * search;
 * Created by hexiwen on 18-2-1.
 */

public class CuSearch {
    private static final String TAG = "search";
    private CuSearch() {
    }

    public static int search(byte[] src, byte[] pattern) {
        if (JUtil.isEmpty(src) || JUtil.isEmpty(pattern)) {
            return -1;
        }
        return search(src, 0, src.length, pattern);
    }
    /**
     * 0 <= start < end <= src.len;
     * end - start >= pattern.len;
     */
    public static int search(byte[] src, int srcStart, int srcEnd, byte[] pattern) {
        if (JUtil.isEmpty(src) || JUtil.isEmpty(pattern)) {
            return -1;
        }
        if (!(0 <= srcStart && srcStart < srcEnd && srcEnd <= src.length)) {
            return -1;
        }
        if (pattern.length > (srcEnd - srcStart)) {
            return -1;
        }
        return searchRaw(src, srcStart, srcEnd, pattern);
    }

    /**
     * src and pattern must not be empty;
     * 0 <= start < end <= src.len;
     * end - start >= pattern.len;
     */
    private static int searchRaw(@NotNull byte[] src, int srcStart, int srcEnd, @NotNull byte[] pattern) {
        int end = srcEnd - pattern.length;
        for (int i=srcStart; i<=end; ++i) {
            if (searchMatchRaw(src, i, pattern, 0, pattern.length)) {
                return i;
            }
        }
        return -1;
    }
    private static boolean searchMatchRaw(byte[] src, int srcStart, byte[] pattern, int patternStart, int matchCount) {
        for (int i=0; i<matchCount; ++i) {
            if (src[srcStart+i] != pattern[patternStart+i]) {
                return false;
            }
        }
        return true;
    }
}
