package com.github.labowenzi.commonj.filetype;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.JUtil;
import com.github.labowenzi.commonj.algorithm.MD5;
import com.github.labowenzi.commonj.annotation.Nullable;
import com.github.labowenzi.commonj.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * file type;
 * Created by hexiwen on 17-12-19.
 */

public class FileTypeUtil {
    private static final String TAG = "FileTypeUtil";

    private FileTypeUtil() {
    }

    // java enum String "FileType{pdf, doc, docx, xls, xlsx, ppt, pptx, jpg, jpeg, png, bmp, webp, gif, mp3, m4a, wav, aac, amr, wmv, rmvb, avi, mp4, rar, zip, apk, psd,}"
    public enum FileType {
        E_PDF("25504446",  // %PDF
                new String[] {"pdf",}),

        E_DOC_XLS_PPT("D0CF11E0A1B11AE1",  // ........
                new String[] {"doc", "xls", "ppt",}),
        E_DOCX_XLSX_PPTX("504B030414000600",  // PK......
                new String[] {"docx", "xlsx", "pptx",}),
        E_DOC(null,
                new String[] {"doc",}, E_DOC_XLS_PPT),
        E_DOCX(null,
                new String[] {"docx",}, E_DOCX_XLSX_PPTX),
        E_XLS(null,
                new String[] {"xls",}, E_DOC_XLS_PPT),
        E_XLSX(null,
                new String[] {"xlsx",}, E_DOCX_XLSX_PPTX),
        E_PPT(null,
                new String[] {"ppt",}, E_DOC_XLS_PPT),
        E_PPTX(null,
                new String[] {"pptx",}, E_DOCX_XLSX_PPTX),

        E_JPG("ffd8ff",
                new String[] {"jpg", "jpeg",}),
        E_PNG("89504e470d0a",  // .PNG..
                new String[] {"png",}),
        E_BMP("424d",  // BM
                new String[] {"bmp",}),
        E_PSD("38425053",  // 8BPS
                new String[] {"psd",}),
        E_WEBP("52494646........57454250",  // RIFF....WEBP
                new String[] {"webp",}),
        E_GIF("47494638",  // GIF8
                new String[] {"gif",}),

        E_MP3("494433",  // ID3
                new String[] {"mp3",}),
        E_M4A("........667479704D344120",  // "....ftypM4A "
                new String[] {"m4a",}),
        E_WAV("52494646........57415645666D7420",  // "RIFF....WAVEfmt "
                new String[] {"wav",}),
        E_AAC(null,
                new String[] {"aac",}),
        E_AAC_MPEG4("fff1",  // ..
                new String[] {"aac",}, E_AAC),
        E_AAC_MPEG2("fff9",  // ..
                new String[] {"aac",}, E_AAC),
        E_AMR("2321414D52",  // #!AMR
                new String[] {"amr",}),

        E_WMV("3026B2758E66CF11A6D900AA0062CE6C",  // ................
                new String[] {"wmv", "wma",}),
        E_RMVB("2E524D46",  // .RMF
                new String[] {"rmvb", "rm",}),
        E_AVI("52494646........415649204C495354",  // "RIFF....AVI LIST"
                new String[] {"avi",}),
        E_MP4("........66747970",  // ....ftyp
                new String[] {"mp4",}),

        E_RAR("52617221",  // Rar!
                new String[] {"rar",}),
        E_ZIP("504b0304",  // PK..
                new String[] {"zip",}),

        E_APK(null,
                new String[] {"apk",}, E_ZIP),

        E_TXT(null,
                F_T_GROUP_PLAIN_TEXT),
        ;

        @Nullable
        private final Byte[] signature;
        @Nullable
        private final String[] extensions;
        @Nullable
        private final FileType parent;  // 如：apk文件就是在zip文件上改的。

        FileType(@Nullable String signature, @Nullable String[] extensions) {
            this(signature, extensions, null);
        }
        FileType(@Nullable String signature, @Nullable String[] extensions, @Nullable FileType parent) {
            this.signature = MD5.hexToBytesNullable(signature);
            this.extensions = extensions;
            this.parent = parent;
        }

        @Nullable
        public Byte[] getSignature() {
            return signature;
        }

        @Nullable
        public String[] getExtensions() {
            return extensions;
        }

        @Nullable
        public FileType getParent() {
            return parent;
        }

        @Nullable
        public String getOneExtensions() {
            return JUtil.isEmpty(extensions) ? null : extensions[0];
        }

        public boolean matchExtension(@Nullable String extension) {
            if (JUtil.isEmpty(extension)) {
                return false;
            }
            if (JUtil.isEmpty(extensions)) {
                return false;
            }
            for (String e : extensions) {
                if (extension.equalsIgnoreCase(e)) {
                    return true;
                }
            }
            return false;
        }

        public static final int MATCH_SIGNATURE_FAILED = 0;
        public static final int MATCH_SIGNATURE_MATCH_ALL = 1;
        public static final int MATCH_SIGNATURE_MATCH_PREFIX = 2;
        public int matchSignature(@Nullable byte[] bytes) {
            if (JUtil.isEmpty(bytes) || JUtil.isEmpty(signature)) {
                return MATCH_SIGNATURE_FAILED;
            }
            if (signature.length > bytes.length) {
                return MATCH_SIGNATURE_FAILED;
            }
            for (int i=0; i<signature.length; ++i) {
                Byte s = signature[i];
                if (s == null) {
                    continue;
                }
                byte b = bytes[i];
                if (!s.equals(b)) {
                    return MATCH_SIGNATURE_FAILED;
                }
            }
            if (signature.length == bytes.length) {
                return MATCH_SIGNATURE_MATCH_ALL;
            } else {
                return MATCH_SIGNATURE_MATCH_PREFIX;
            }
        }

        @Override
        public String toString() {
            return super.toString();
        }

        public static final FileType[] values = values();

        public static final int SIGNATURE_MAX_LENGTH;
        static {
            int s = 0;
            for (FileType f : values()) {
                Byte[] sign = f.getSignature();
                s = Math.max(s, sign == null ? 0 : sign.length);
            }
            SIGNATURE_MAX_LENGTH = s;
        }

        public static final FileType[] GROUP_DOC = {
                E_DOC, E_DOCX,
        };
        public static final FileType[] GROUP_XLS = {
                E_XLS, E_XLSX,
        };
        public static final FileType[] GROUP_PPT = {
                E_PPT, E_PPTX,
        };
        public static final FileType[] GROUP_OFFICE = {
                E_DOC, E_DOCX, E_XLS, E_XLSX, E_PPT, E_PPTX,
        };
        public static final FileType[] GROUP_PIC = {
                E_JPG, E_PNG, E_BMP, E_WEBP, E_GIF,
        };
        public static final FileType[] GROUP_POSSIBLE_ANIMATED_PIC = {
                E_WEBP, E_GIF,
        };
        public static final FileType[] GROUP_AUDIO = {
                E_MP3, E_M4A, E_WAV, E_AAC, E_AAC_MPEG2, E_AAC_MPEG4, E_AMR,
        };
        public static final FileType[] GROUP_VIDEO = {
                E_WMV, E_RMVB, E_AVI, E_MP4,
        };
    }

    public static final FileType[] ALL_EXTENSIONS = {
            FileType.E_PDF,

            FileType.E_DOC,
            FileType.E_DOCX,
            FileType.E_XLS,
            FileType.E_XLSX,
            FileType.E_PPT,
            FileType.E_PPTX,

            FileType.E_JPG,
            FileType.E_PNG,
            FileType.E_BMP,
            FileType.E_PSD,
            FileType.E_WEBP,
            FileType.E_GIF,

            FileType.E_MP3,
            FileType.E_M4A,
            FileType.E_WAV,
            FileType.E_AAC,
            FileType.E_AMR,

            FileType.E_WMV,
            FileType.E_RMVB,
            FileType.E_AVI,
            FileType.E_MP4,

            FileType.E_RAR,
            FileType.E_ZIP,
            FileType.E_APK,

            FileType.E_TXT,
    };
    @Nullable
    public static FileType getByExtension(@Nullable String extension) {
        return getByExtension(extension, ALL_EXTENSIONS);
    }
    @Nullable
    public static FileType getByExtension(@Nullable String extension, @Nullable FileType[] fileTypes) {
        if (JUtil.isEmpty(extension) || JUtil.isEmpty(fileTypes)) {
            return null;
        }
        for (FileType item : fileTypes) {
            if (item == null) {
                continue;
            }
            if (item.matchExtension(extension)) {
                return item;
            }
        }
        return null;
    }

    public static final int FILE_TYPE_SIGNATURE_MATCH_MIN_LENGTH = 16;
    /**
     * @param signature at least {@link #FILE_TYPE_SIGNATURE_MATCH_MIN_LENGTH} bytes;
     *                  maybe {@link FileType#SIGNATURE_MAX_LENGTH}
     */
    @Nullable
    public static FileType getBySignature(@Nullable byte[] signature) {
        if (JUtil.isEmpty(signature) || signature.length < FILE_TYPE_SIGNATURE_MATCH_MIN_LENGTH) {
            return null;
        }
        for (FileType item : FileType.values) {
            int match = item.matchSignature(signature);
            if (match == FileType.MATCH_SIGNATURE_MATCH_ALL) {
                return item;
            } else if (match == FileType.MATCH_SIGNATURE_MATCH_PREFIX) {
                return item;
            }
        }
        return null;
    }
    @Nullable
    public static FileType getBySignature(@Nullable String filePath) {
        if (JUtil.isEmpty(filePath)) {
            return null;
        }
        File file = new File(filePath);
        if (!JIoUtil.canReadNormalFile(file)) {
            return null;
        }
        if (file.length() < FILE_TYPE_SIGNATURE_MATCH_MIN_LENGTH) {
            return null;
        }
        byte[] bytes = new byte[FILE_TYPE_SIGNATURE_MATCH_MIN_LENGTH];
        try {
            InputStream is = new FileInputStream(file);
            int len = is.read(bytes);
            JIoUtil.closeSilently(is);
            if (len < bytes.length) {
                return null;
            } else {
                return getBySignature(bytes);
            }
        } catch (NullPointerException | IOException e) {
            Log.e(TAG, "io", e);
        }
        return null;
    }

    @Nullable
    public static FileType guessFileType(@Nullable String filePath) {
        if (JUtil.isEmpty(filePath)) {
            return null;
        }
        String extension = JIoUtil.getFileExtension(filePath);
        FileType fileType = JUtil.isEmpty(extension) ? null : getByExtension(extension);
        if (fileType == null) {
            fileType =getBySignature(filePath);
        }
        return fileType;
    }

    public static final String[] F_T_GROUP_PLAIN_TEXT = {
            "c",
            "cpp",
            "cs",
            "csv",
            "go",
            "h",
            "hs",
            "java",
            "js",
            "json",
            "log",
            "lsp",
            "m",
            "md",
            "me",
            "pas",
            "php",
            "pl",
            "py",
            "r",
            "rb",
            "rtf",
            "scala",
            "sh",
            "sql",
            "swift",
            "txt",
            "xml",
    };
}
