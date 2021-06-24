package com.github.labowenzi.commonj;

import com.github.labowenzi.commonj.annotation.NotNull;
import com.github.labowenzi.commonj.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * text util;
 * Created by hexiwen on 16-7-14.
 */
public class JTextUtil {

    // Pattern for recognizing a URL, based off RFC 3986
    public static final Pattern WEB_URL_PATTERN_1 = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                    + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                    + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    public static final Pattern WEB_URL_PATTERN_2_CIII = Pattern.compile("(https?:\\/\\/|www\\.)[_a-zA-Z-\\d.~!*'();:@&=+$,/?#[\\\\]]+\\.[_a-zA-Z-\\d.~!*'();:@&=+$,/?#[\\\\]]+");

    public static final Pattern WEB_URL_PATTERN = WEB_URL_PATTERN_2_CIII;

    public static boolean containsEmojiChar(String str) {
        return JEmojiUtil.containsEmoji(str);
    }

    public static boolean isBlank(String s) {
        if (s == null) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEng(String str) {
        if (str == null) {
            return false;
        }
        for (int i=0; i<str.length(); ++i) {
            if (!isEngChar(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    public static boolean isEngChar(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
    }

    public static char toLowerCase(char c) {
        if ('a' <= c && c <= 'z') return c;
        else if ('A' <= c && c <= 'Z') return (char) (c -'A' + 'a');
        return c;
    }
    public static char toUpperCase(char c) {
        if ('a' <= c && c <= 'z') return (char) (c -'a' + 'A');
        else if ('A' <= c && c <= 'Z') return c;
        return c;
    }

    public static void toLowerCase(char[] c) {
        if (JUtil.isEmpty(c)) {
            return;
        }
        for (int i=0; i<c.length; ++i) {
            c[i] = toLowerCase(c[i]);
        }
    }
    public static void toUpperCase(char[] c) {
        if (JUtil.isEmpty(c)) {
            return;
        }
        for (int i=0; i<c.length; ++i) {
            c[i] = toUpperCase(c[i]);
        }
    }
    
    private static boolean isSpecialAsciiChar(char c) {
        return (c == 0x0) || (c == 0x9) || (c == 0xA) || (c == 0xD);
    }

    public static boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    public static boolean isHexChar(char c) {
        return     ('0' <= c && c <= '9')
                || ('a' <= c && c <= 'f')
                || ('A' <= c && c <= 'F');
    }

    public static String stringToHex(String str) {
        StringBuilder sb = new StringBuilder(str.length() * 4);
        for (char c : str.toCharArray()) {
            sb.append(Integer.toHexString((int)c));
        }
        return sb.toString();
    }

    public static boolean isHexString(String str) {
        return isHexString(str, 0, str.length());
    }
    public static boolean isHexString(String str, int len) {
        return isHexString(str, 0, len);
    }
    public static boolean isHexString(String str, int start, int end) {
        if (str == null) return false;
        if (start < 0 || start >= str.length()) return false;
        if (end < 0 || end > str.length()) return false;
        if (start > end) return false;
        for (int i=start; i<end; ++i) {
            char c = str.charAt(i);
            if (!isHexChar(c)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isHex(char[] str) {
        if (str == null || str.length <= 0) return false;
        for (char c : str) {
            if (!isHexChar(c)) {
                return false;
            }
        }
        return true;
    }

    public static List<String> split(String resultStr, char c) {
        if (resultStr == null) return null;
        List<String> res = new ArrayList<>();
        if (resultStr.length() == 0) return res;
        String s = "" + c;
        Collections.addAll(res, resultStr.split(s));
        return res;
    }

    public static String toHtmlTextView(String str) {
        if (JUtil.isEmpty(str)) return "";
        StringBuilder sb = new StringBuilder();
        Matcher matcher = WEB_URL_PATTERN.matcher(str);
        int oldMe = 0;
        int ms, me;
        while (matcher.find()) {
            ms = matcher.start(1);
            me = matcher.end();
            if (ms > oldMe) sb.append(htmlEncode(str.substring(oldMe, ms)));
            String url = str.substring(ms, me);
            String url1 = url.startsWith("www") ? ("http://" + url) : url;
            sb.append("<a href=\"").append(url1).append("\">").append(url).append("</a>");
            oldMe = me;
        }
        if (str.length() > oldMe) sb.append(htmlEncode(str.substring(oldMe)));
        return sb.toString();
    }

    @NotNull
    public static String htmlEncode(@NotNull String s) {
        StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;"); //$NON-NLS-1$
                    break;
                case '>':
                    sb.append("&gt;"); //$NON-NLS-1$
                    break;
                case '&':
                    sb.append("&amp;"); //$NON-NLS-1$
                    break;
                case '\'':
                    //http://www.w3.org/TR/xhtml1
                    // The named character reference &apos; (the apostrophe, U+0027) was
                    // introduced in XML 1.0 but does not appear in HTML. Authors should
                    // therefore use &#39; instead of &apos; to work as expected in HTML 4
                    // user agents.
                    sb.append("&#39;"); //$NON-NLS-1$
                    break;
                case '"':
                    sb.append("&quot;"); //$NON-NLS-1$
                    break;
                case ' ':
                    sb.append("&nbsp;");
                    break;
                case '\n':
                    sb.append("<br>");
                    break;
                case '\t':
                    sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String format(String format, Object... args) {
        return String.format(Locale.getDefault(), format, args);
    }
    public static String format(Locale locale, String format, Object... args) {
        return String.format(locale, format, args);
    }

    public static boolean equal(String str, String s) {
        if (str == null && s == null) return true;
        if (str == null || s == null) return false;
        else return str.equals(s);
    }
    public static boolean equalCaseInsensitive(String str, String s) {
        if (str == null && s == null) return true;
        if (str == null || s == null) return false;
        else return str.equalsIgnoreCase(s);
    }

    public static boolean textContain(String textSearch, String... strings) {
        if (textSearch == null) {
            for (String str : strings) {
                if (str == null) return true;
            }
            return false;
        } else {
            textSearch = textSearch.toLowerCase();
            for (String str : strings) {
                if (str == null) continue;
                str = str.toLowerCase();
                if (str.contains(textSearch)) return true;
            }
            return false;
        }
    }

    @NotNull
    public static String getNotNullText(@Nullable String str) {
        return str == null ? "" : str;
    }

    @Nullable
    public static String getNullIfEmpty(@Nullable String str) {
        return JUtil.isEmpty(str) ? null : str;
    }


    public static final Pattern PATTERN_POST_CODE_CHINA = Pattern.compile("[0-9]{6}");
    public static boolean isPostcodeChina(@Nullable String pc) {
        if (pc == null || pc.length() == 0) return false;
        Pattern p = PATTERN_POST_CODE_CHINA;
        Matcher m = p.matcher(pc);
        return m.matches();
    }

    public static final Pattern FIRST_LETTER_PATTERN = Pattern.compile("[a-zA-Z]");
    public static final char FIRST_CHAR_DEFAULT = '#';
    public static char getFirstAsciiChar(@NotNull String str) {
        if (JUtil.isEmpty(str)) {
            return FIRST_CHAR_DEFAULT;
        }
        String firstLetter = str.substring(0, 1).toUpperCase();
        if (!FIRST_LETTER_PATTERN.matcher(String.valueOf(firstLetter)).matches()) {
            return FIRST_CHAR_DEFAULT;
        }
        return firstLetter.charAt(0);
    }

    public static boolean isFirstAsciiChar(char c) {
        if (c == FIRST_CHAR_DEFAULT) {
            return true;
        }
        String s = String.valueOf(c);
        return FIRST_LETTER_PATTERN.matcher(s).matches();
    }

    public static boolean isFirstAsciiChars(String str) {
        if (str == null) {
            return false;
        }
        for (int i=0; i<str.length(); ++i) {
            if (!isFirstAsciiChar(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public static String getFirstLetter(@NotNull String string, @Nullable Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        // maybe use locale to get first letter, not just ascii chat;
        char firstChar = getFirstAsciiChar(string);
        return String.valueOf(firstChar);
    }

    public static void newLine(StringBuilder sb, int indent) {
        sb.append('\n');
        indent(sb, indent);
    }

    public static void indent(StringBuilder sb, int indent) {
        for (int i=0; i<indent; ++i) {
            sb.append(' ');
        }
    }
}
