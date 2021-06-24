package com.github.labowenzi.commonj;

/**
 * emoji util;
 * Created by hexiwen on 16-7-14.
 */
public class JEmojiUtil {
    // 实际上，unicode定义的表情远非这点，参考：
    //   官网：http://www.unicode.org/charts/
    //   下载：http://users.teilar.gr/~g1951d/
    //   介绍网站：http://apps.timwhitlock.info/emoji/tables/unicode
    // 下面的这些主要参考的是Emoji的维基百科词条。
    //   wiki：https://en.wikipedia.org/wiki/Emoji
    public static final int[][] EMOJI_RANGE = {
            {0x1f1e6, 0x1f1ff},
            {0x1f300, 0x1f5ff},
            {0x1f900, 0x1f9ff},
            {0x1f600, 0x1f64f},
            {0x1f680, 0x1f6ff},
            {0x2600, 0x26ff},
            {0x2700, 0x27bf},
    };

    public static boolean isEmoji(int codePoint) {
        for (int[] range : EMOJI_RANGE) {
            if (range[0] <= codePoint && codePoint <= range[1]) return true;
        }
        return false;
    }

    public static boolean containsEmoji(String str) {
        for (int offset = 0; offset < str.length(); ) {
            final int codepoint = str.codePointAt(offset);
            if (isEmoji(codepoint)) return true;
            offset += Character.charCount(codepoint);
        }
        return false;
    }
}
