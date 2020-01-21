package com.github.labowenzi.commonj.algorithm;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.JTextUtil;
import com.github.labowenzi.commonj.annotation.NotNull;
import com.github.labowenzi.commonj.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * md5;
 * Created by hexiwen on 16-4-26.
 */
public final class MD5 {
    private MD5() {
    }

    public static final int BIT_LENGTH = 128;
    public static final int HEX_LENGTH = 32;
    public static final int BYTE_LENGTH = 16;

    final protected static char[] hexArray = "0123456789abcdef".toCharArray();

    public static String getMessageDigest(@NotNull byte[] bytes) {
        byte[] md5 = getMd5(bytes);
        if (md5 == null) return null;
        else return bytesToHex(bytes);
    }

    public static byte[] getMd5(@NotNull byte[] bytes) {
        try {
            MessageDigest md5;
            md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes);
            return md5.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String bytesToString(@NotNull byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < bytes.length; i ++){
            sb.append(bytes[i]);
        }
        return sb.toString();
    }
    public static String bytesToHex(@NotNull byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    public static String bytesToHex(@NotNull Byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            char c1 = '.';
            char c2 = '.';
            Byte b = bytes[j];
            if (b != null) {
                int v = b & 0xFF;
                c1 = hexArray[v >>> 4];
                c2 = hexArray[v & 0x0F];
            }
            hexChars[j * 2] = c1;
            hexChars[j * 2 + 1] = c2;
        }
        return new String(hexChars);
    }
    public static String bytesToHexInInteger(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
            bytes[i] = bytes[i];
        }
        return sb.toString();
    }
    public static String bytesToHexInBigInt(@NotNull byte[] bytes) {
        BigInteger bigInt = new BigInteger(1, bytes);
        String hex = bigInt.toString(0x10);
        hex = JTextUtil.getNotNullText(hex);
        StringBuilder sb = new StringBuilder();
        // 当 bytes 以 0 开头，则转换为 BigInt 时会略去前导的 0，因此需要按照 bytes 的位数补全 0。
        int length = bytes.length + bytes.length;
        for (int i=hex.length(); i<length; ++i) {
            sb.append('0');
        }
        sb.append(hex);
        return sb.toString();
    }

    public static Byte hexToByte(char c) {
        if ('0' <= c && c <= '9') {
            return (byte) (c - '0');
        } else if ('a' <= c && c <= 'f') {
            return (byte) (c - 'a' + 10);
        } else if ('A' <= c && c <= 'F') {
            return (byte) (c - 'A' + 10);
        } else {
            return null;
        }
    }
    public static Byte hexToByte(char c1, char c2) {
        Byte b1 = hexToByte(c1);
        if (b1 == null) {
            return null;
        }
        Byte b2 = hexToByte(c2);
        if (b2 == null) {
            return null;
        }
        return (byte) ((b1 << 4) | b2);
    }
    public static byte[] hexToBytes(@Nullable String hex) {
        if (hex == null) {
            return null;
        } else if (hex.length() == 0) {
            return new byte[0];
        }
        char[] chars = hex.toCharArray();
        if (chars.length == 0 || chars.length % 2 != 0) {
            return null;
        }
        byte[] bytes = new byte[chars.length / 2];
        for (int i=0, cnt=0; i<chars.length; i+=2, cnt+=1) {
            Byte b = hexToByte(chars[i], chars[i+1]);
            if (b == null) {
                return null;
            }
            bytes[cnt] = b;
        }
        return bytes;
    }
    public static Byte[] hexToBytesNullable(@Nullable String hex) {
        if (hex == null) {
            return null;
        } else if (hex.length() == 0) {
            return new Byte[0];
        }
        char[] chars = hex.toCharArray();
        if (chars.length == 0) {
            return new Byte[0];
        } else if (chars.length < 2) {
            return new Byte[] {null,};
        }
        Byte[] bytes = new Byte[(chars.length+1) / 2];
        int cnt = 0;
        for (int i=1; i<chars.length; i+=2, cnt+=1) {
            Byte b = hexToByte(chars[i-1], chars[i]);
            bytes[cnt] = b;
        }
        if (cnt < bytes.length) {
            bytes[bytes.length - 1] = null;
        }
        return bytes;
    }

    @Nullable
    public static byte[] getFileMd5Byte(@Nullable String filePath) {
        if (filePath == null) return null;
        File file = new File(filePath);
        if (!file.exists()) return null;
        if (!file.isFile()) return null;
        MessageDigest digest;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) JIoUtil.closeSilently(in);
        }
        return digest.digest();
    }

    @Nullable
    public static String getFileMd5(@Nullable String filePath) {
        byte[] bytes = getFileMd5Byte(filePath);
        return bytes == null ? null : bytesToHex(bytes);
    }

    public static boolean isLegal(@Nullable String checksum) {
        return checksum != null && checksum.length() == HEX_LENGTH && JTextUtil.isHexString(checksum);
    }
    public static boolean isLegal(char[] checksum) {
        return checksum != null && checksum.length == HEX_LENGTH && JTextUtil.isHex(checksum);
    }
    public static boolean isLegal(byte[] checksum) {
        return checksum != null && checksum.length == BYTE_LENGTH;
    }

    public static boolean isMd5Equal(@Nullable String checksum1, @Nullable String checksum2) {
        return JTextUtil.equalCaseInsensitive(checksum1, checksum2);
    }
}
