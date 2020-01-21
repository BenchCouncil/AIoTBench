package com.github.labowenzi.commonj;

import com.github.labowenzi.commonj.algorithm.CuSearch;
import com.github.labowenzi.commonj.algorithm.MD5;
import com.github.labowenzi.commonj.annotation.NotNull;
import com.github.labowenzi.commonj.annotation.Nullable;
import com.github.labowenzi.commonj.filetype.FileTypeUtil;
import com.github.labowenzi.commonj.log.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alanubu on 18-7-29.
 */
public class JIoUtil {
    private static final String TAG = "IOUtil";

    private JIoUtil() {
    }

    /**
     * mkdir;
     * @param strFolder dir path;
     * @return ok;
     */
    public static boolean mkdir(String strFolder) {
        File file = new File(strFolder);
        if (!file.exists()) {
            // file not exist
            if (file.mkdirs()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public static void logEFileOperation() {
        Log.e("file operation", "error");
    }

    public static void logEFileOperation(String method) {
        Log.e("file operation", "error when do \"" + method + "\"");
    }

    public static void logEFileOperation(String method, String filepath) {
        Log.e("file operation", "error when do \"" + method + "\" on file \"" + filepath + "\"");
    }

    /**
     * Close a {@link Closeable} stream without throwing any exception
     *
     * @param c closeable;
     */
    public static void closeSilently( final Closeable c ) {
        if ( c == null ) return;
        try {
            c.close();
        } catch ( final Throwable t ) {
            logEFileOperation("close");
        }
    }

    public static boolean isInDir(@NotNull File file, @NotNull File dir) {
        try {
            return isInDir(file.getCanonicalPath(), dir.getCanonicalPath());
        } catch (IOException e) {
            Log.e(TAG, "io", e);
            return false;
        }
    }
    public static boolean isInDir(@NotNull String file, @NotNull String dir) {
        return file.startsWith(dir + File.separator);
    }
    public static boolean isInDir(@NotNull String file, @NotNull File dir) {
        try {
            if (isInDir(file, dir.getCanonicalPath())) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "io", e);
        }
        try {
            if (isInDir(new File(file), dir)) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "io", e);
        }
        return false;
    }

    private static class JFile {
        public String name;
        public JFile(File f) {
            name = f.getName();
        }
    }
    private static class JDir extends JFile {
        public List<JFile> files = new ArrayList<>();
        public JDir(File f) {
            super(f);
        }
    }
    public static String lsRJ(File f_d) {
        if (f_d.exists()) {
            if (f_d.isDirectory()) {
                JDir d = lsRDirExistJ(f_d);
                return jsonFormat(JJsonUtils.simpleToJson(d));
            } else {
                JFile f = new JFile(f_d);
                return jsonFormat(JJsonUtils.simpleToJson(f));
            }
        } else {
            return JJsonUtils.EMPTY_JSON;
        }
    }
    private static JDir lsRDirExistJ(File dir) {
        JDir d = new JDir(dir);
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                d.files.add(lsRDirExistJ(f));
            } else {
                d.files.add(new JFile(f));
            }
        }
        return d;
    }
    private static String jsonFormat(String json) {
        StringBuilder sb = new StringBuilder();
        int tabs = 0;
        for (int i = 0; i < json.length(); ++i) {
            char c = json.charAt(i);
            switch (c) {
                case '[':
                case '{': {
                    if (i < json.length()-1) {
                        char c1 = json.charAt(i+1);
                        if ( (c == '[' && c1 == ']') || (c == '{' && c1 == '}')) {
                            ++i;
                            sb.append(c).append(c1).append('\n');
                            for (int j = 0; j < tabs; ++j) sb.append(' ');
                        } else {
                            sb.append(c).append('\n');
                            tabs += 2;
                            for (int j = 0; j < tabs; ++j) sb.append(' ');
                        }
                    } else {
                        sb.append(c).append('\n');
                        tabs += 2;
                        for (int j = 0; j < tabs; ++j) sb.append(' ');
                    }
                    break;
                }
                case ']':
                case '}': {
                    sb.append('\n');
                    tabs -= 2;
                    for (int j=0; j < tabs; ++j) sb.append(' ');
                    sb.append(c);
                    if (i < json.length()-1) {
                        char c1 = json.charAt(i+1);
                        if (c1 == ',') {
                            sb.append(c1);
                            ++ i;
                        }
                        if (i < json.length()-1) {
                            char c2 = json.charAt(i+1);
                            if (c2 != ']' && c2 != '}') {
                                sb.append('\n');
                                for (int j = 0; j < tabs; ++j) sb.append(' ');
                            }
                        }
                    }
                    break;
                }
                default: {
                    sb.append(c);
                    break;
                }
            }
        }
        return sb.toString();
    }

    public static String lsR(File f_d) {
        if (f_d.exists()) {
            if (f_d.isDirectory()) {
                return lsRDirExist(f_d, 0);
            } else {
                return "{" + f_d.getName() + "\n" + "}";
            }
        } else {
            return "{}";
        }
    }

    private static String lsRDirExist(File dir, int blanks) {
        StringBuilder res = new StringBuilder();
        res.append(dir.getName()).append(":");
        res.append('{');
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                res.append(lsRDirExist(f, blanks + 2));
                res.append(',');
            } else {
//                for (int i=0; i<blanks; ++i) {
//                    res.append(' ');
//                }
                res.append(f.getName());
//                res.append("\n");
                res.append(',');
            }
        }
        res.append('}');
        return res.toString();
    }

    private static long[] getExistDirSizeInfo(File dir, long blockSize) {
        long[] res = new long[3];  // count, size, capacity;
        res[0] = res[1] = res[2] = 0;

        // dir itself;
        res[0] += 1;
        res[1] += dir.length();
        res[2] += dir.length();  // dir: length == capacity;

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return res;
        for (File f : files) {
            if (f.isFile()) {
                res[0] += 1;
                res[1] += f.length();
                res[2] += (f.length() / blockSize + 1) * blockSize;
            }
            else {
                long[] childRes = getExistDirSizeInfo(f, blockSize);
                res[0] += childRes[0];
                res[1] += childRes[1];
                res[2] += childRes[2];
            }
        }
        return res;
    }

    public static boolean touchFile(String filename) {
        boolean res = true;
        File file = new File(filename);
        if (file.exists()) {
            long newModifiedTime = System.currentTimeMillis();
            if (!file.setLastModified(newModifiedTime)) {
                logEFileOperation("setLastModified");
                res = false;
            }
        } else {
            try {
                new FileOutputStream(filename).close();
            } catch (IOException e) {
                e.printStackTrace();
                res = false;
            }
        }
        return res;
    }

    public static void writeToFile(File file, String data) {
        String fname = file.getAbsolutePath();
        if (!file.exists()) touchFile(fname);
        File f = new File(fname);
        try {
            FileOutputStream fos = new FileOutputStream(f, true);
            fos.write(data.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteFileSilently(@Nullable String filePath) {
        if (JUtil.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return deleteFileSilently(file);
    }
    public static boolean deleteFileSilently(@Nullable File file) {
        return file != null && file.exists() && file.delete();
    }
    public static boolean deleteFilesInDirSilently(@Nullable String tempDir) {
        if (JUtil.isEmpty(tempDir)) {
            return false;
        }
        File dir = new File(tempDir);
        return deleteFilesInDirSilently(dir);
    }
    public static boolean deleteFilesInDirSilently(@Nullable File tempDir) {
        if (tempDir == null || !tempDir.exists() || !tempDir.isDirectory()) {
            return false;
        }
        for (File file : tempDir.listFiles()) {
            deleteFileSilently(file);
        }
        return true;
    }
    public static boolean deleteFileOrInDirSilently(@Nullable String fileOrDirPath) {
        if (JUtil.isEmpty(fileOrDirPath)) {
            return false;
        }
        File file = new File(fileOrDirPath);
        return deleteFileOrInDirSilently(file);
    }
    public static boolean deleteFileOrInDirSilently(@Nullable File tempFileOrDir) {
        if (tempFileOrDir == null || !tempFileOrDir.exists()) {
            return false;
        }
        if (tempFileOrDir.isFile()) {
            deleteFileSilently(tempFileOrDir);
        } else if (tempFileOrDir.isDirectory()) {
            deleteFilesInDirSilently(tempFileOrDir);
        }
        return true;
    }

    public static String readJson(@NotNull String filePath) {
        try {
            InputStream inputStream = new FileInputStream(new File(filePath));
            String res = readJson(inputStream);
            closeSilently(inputStream);
            return res;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "cannot open " + filePath, e);
            return null;
        }
    }
    public static String readJson(@NotNull InputStream in) {
        String res;
        try{
            int length = in.available();
            byte [] buffer = new byte[length];
            in.read(buffer);
            res = new String(buffer, StandardCharsets.UTF_8);//用new String可以运行在任意API Level
            return res;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static final String F_TYPE_UNKNOWN = "unknown";

    public static boolean isPdfFile(String filename) {
        String suffix = getFileSuffix(filename);
        return !JUtil.isEmpty(suffix) && FileTypeUtil.FileType.E_PDF.matchExtension(suffix);
    }
    public static boolean isDocFile(String filename) {
        String suffix = getFileSuffix(filename);
        return !JUtil.isEmpty(suffix) && FileTypeUtil.getByExtension(suffix, FileTypeUtil.FileType.GROUP_DOC) != null;
    }
    public static boolean isXlsFile(String filename) {
        String suffix = getFileSuffix(filename);
        return !JUtil.isEmpty(suffix) && FileTypeUtil.getByExtension(suffix, FileTypeUtil.FileType.GROUP_XLS) != null;
    }
    public static boolean isPptFile(String filename) {
        String suffix = getFileSuffix(filename);
        return !JUtil.isEmpty(suffix) && FileTypeUtil.getByExtension(suffix, FileTypeUtil.FileType.GROUP_PPT) != null;
    }
    public static boolean isOfficeFile(String filename) {
        String suffix = getFileSuffix(filename);
        return !JUtil.isEmpty(suffix) && FileTypeUtil.getByExtension(suffix, FileTypeUtil.FileType.GROUP_OFFICE) != null;
    }
    public static boolean isTextFile(String filename) {
        String suffix = getFileSuffix(filename);
        return !JUtil.isEmpty(suffix) && FileTypeUtil.FileType.E_TXT.matchExtension(suffix);
    }
    public static boolean isPicFile(String filename) {
        String suffix = getFileSuffix(filename);
        return !JUtil.isEmpty(suffix) && FileTypeUtil.getByExtension(suffix, FileTypeUtil.FileType.GROUP_PIC) != null;
    }
    public static boolean isPossibleAnimatedPicByFileName(String filename) {
        String suffix = getFileSuffix(filename);
        return !JUtil.isEmpty(suffix) && FileTypeUtil.getByExtension(suffix, FileTypeUtil.FileType.GROUP_POSSIBLE_ANIMATED_PIC) != null;
    }
    public static boolean isPossibleAnimatedPicByFileSignature(String filepath) {
        FileTypeUtil.FileType fileType = FileTypeUtil.getBySignature(filepath);
        return fileType != null && JUtil.inArray(fileType, FileTypeUtil.FileType.GROUP_POSSIBLE_ANIMATED_PIC);
    }
    public static boolean isPossibleAnimatedPicByFile(String filepath) {
        return isPossibleAnimatedPicByFileName(filepath) || isPossibleAnimatedPicByFileSignature(filepath);
    }
    public static boolean isAudioFile(String filename) {
        String suffix = getFileSuffix(filename);
        return !JUtil.isEmpty(suffix) && FileTypeUtil.getByExtension(suffix, FileTypeUtil.FileType.GROUP_AUDIO) != null;
    }
    public static boolean isVideoFile(String filename) {
        String suffix = getFileSuffix(filename);
        return !JUtil.isEmpty(suffix) && FileTypeUtil.getByExtension(suffix, FileTypeUtil.FileType.GROUP_VIDEO) != null;
    }


    public static String getFileSuffix(String filename) {
        if (filename == null) return null;
        int loc = filename.lastIndexOf('.');
        if (loc < 0 || loc == filename.length()-1) return null;
        return filename.substring(loc+1);
    }

    public static String genFileName(String oriStr, String extension) {
        String str;
        try {
            int index;
            index = oriStr.indexOf("?");
            if (index >= 0) {
                oriStr = oriStr.substring(0, index);
            }
            index = oriStr.lastIndexOf("%");
            if (index >= 0) {
                oriStr = oriStr.substring(index + 1);
            }
            index = oriStr.lastIndexOf("/");
            if (index >= 0) {
                oriStr = oriStr.substring(index + 1);
            }
            index = oriStr.lastIndexOf(".");
            if (index >= 0) {
                oriStr = oriStr.substring(0, index);
            }
            str = oriStr;
        } catch (Exception e) {
            str = null;
        }
        if (JUtil.isEmpty(str)) {
            str = "default";
        }
        return str + "." + extension;
    }

    public static final int GEN_FILE_NAME_TRY_MAX = 10;
    @Nullable
    public static String findPossibleFilenameInDir(@NotNull String dir, @NotNull String filename, @NotNull IFindPossibleFileInDir find) {
        File file = new File(dir, filename);
        if (find.shouldReturn(file)) {
            return filename;
        }
        String fname = getFileNameWithoutExtension(filename);
        String extension = getFileExtension(filename);
        if (fname == null) fname = "";
        if (extension == null) extension = "";
        for (int i=1; i<=GEN_FILE_NAME_TRY_MAX; ++i) {
            String newName = fname + "_" + i + "." + extension;
            file = new File(dir, newName);
            if (find.shouldReturn(file)) {
                return newName;
            }
        }
        return null;
    }

    public static String genFileNameInDir(String dir, String filename) {
        if (JUtil.isEmpty(dir)) {
            return null;
        }
        filename = JUtil.isEmpty(filename) ? "" : filename;
        return findPossibleFilenameInDir(dir, filename, new IFindPossibleFileInDir() {
            @Override
            public boolean shouldReturn(@NotNull File file) {
                return !file.exists();
            }
        });
    }

    @Nullable
    public static String checkMd5OnPossibleFileInDir(String dir, String filename, final String md5) {
        if (JUtil.isEmpty(dir) || !MD5.isLegal(md5)) {
            return null;
        }
        filename = JUtil.isEmpty(filename) ? "" : filename;
        return findPossibleFilenameInDir(dir, filename, new IFindPossibleFileInDir() {
            @Override
            public boolean shouldReturn(@NotNull File file) {
                return checkFileMd5(file.getAbsolutePath(), md5);
            }
        });
    }

    public interface IFindPossibleFileInDir {
        boolean shouldReturn(@NotNull File file);
    }

    public static boolean checkFileMd5(@Nullable String filePath, @Nullable String md5) {
        if (JUtil.isEmpty(md5) || JUtil.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }
        String thisMd5 = MD5.getFileMd5(filePath);
        return MD5.isMd5Equal(md5, thisMd5);
    }

    @Nullable
    public static String getFileNameWithoutExtension(@NotNull String filename) {
        int index = filename.lastIndexOf('.');
        if (index >= 0) filename = filename.substring(0, index);
        index = filename.lastIndexOf(File.separator);
        if (index >= 0) return filename.substring(index + 1);
        else return filename;
    }
    @Nullable
    public static String getFileExtension(@NotNull String filename) {
        int index = filename.lastIndexOf('.');
        if (index >= 0) return filename.substring(index + 1);
        return null;
    }

    @NotNull
    public static String getFileNameFromPath(@NotNull String path) {
        int index = path.lastIndexOf('/');
        if (index >= 0) return path.substring(index + 1);
        return path;
    }

    public static boolean canRead(@Nullable String filePath) {
        return !JUtil.isEmpty(filePath) && canRead(new File(filePath));
    }
    public static boolean canRead(@Nullable File file) {
        return file != null && file.exists() && file.canRead();
    }

    public static boolean canReadNormalFile(@Nullable String filePath) {
        return !JUtil.isEmpty(filePath) && canReadNormalFile(new File(filePath));
    }
    public static boolean canReadNormalFile(@Nullable File file) {
        return canRead(file) && file.isFile();
    }

    public static long searchByteInFile(String filepath, byte[] pattern) {
        if (JUtil.isEmpty(filepath) || JUtil.isEmpty(pattern)) {
            return -1;
        }
        File file = new File(filepath);
        if (!JIoUtil.canReadNormalFile(file)) {
            return -1;
        }
        if (file.length() <= 0) {
            return -1;
        }
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (is == null) {
            return -1;
        }
        long find = searchByte(is, pattern);
        JIoUtil.closeSilently(is);
        return find;
    }
    public static long searchByte(@NotNull InputStream is, byte[] pattern) {
        if (JUtil.isEmpty(pattern)) {
            return -1;
        }
        final int readLen = pattern.length >= 2048 ? pattern.length+4096 : 4096;
        // final int readLen = pattern.length >= 5 ? pattern.length+10 : 10;
        final int bytesAboveLen = pattern.length - 1;
        byte[] bytes = new byte[readLen + bytesAboveLen];  // [bytes above] + [curr bytes];
        int bytesStart = bytesAboveLen;
        int bytesEnd;
        int total = 0;
        try {
            int len;
            while ((len = is.read(bytes, bytesAboveLen, readLen)) > 0) {
                bytesEnd = bytesAboveLen + len;
                int find = CuSearch.search(bytes, bytesStart, bytesEnd, pattern);
                if (find >= 0) {
                    return total + find - bytesAboveLen;
                }
                bytesStart = len >= bytesAboveLen ? 0 : bytesAboveLen - len;
                int curLen = bytesAboveLen-bytesStart;
                int desStart = bytesStart;
                int srcStart = bytesEnd - curLen;
                for (int i=0; i<curLen; ++i) {
                    bytes[desStart+i] = bytes[srcStart+i];
                }
                total += len;
            }
        } catch (IOException e) {
            Log.e(TAG, "io", e);
        }
        return -1;
    }
}
