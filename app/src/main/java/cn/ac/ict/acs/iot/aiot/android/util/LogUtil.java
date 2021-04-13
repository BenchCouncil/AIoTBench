package cn.ac.ict.acs.iot.aiot.android.util;

import android.os.Environment;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by alanubu on 19-12-27.
 */
public class LogUtil {

    public static class Log implements Closeable {
        public static final String DEFAULT_DIR = "aiot/log";
        public static final String DEFAULT_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + DEFAULT_DIR;

        private OutputStream out;

        public Log() {
            this(DEFAULT_FILE_PATH + '/' + "log_default.log");
        }

        public Log(String filePath) {
            try {
                File file = new File(filePath);
                file.getParentFile().mkdirs();
                out = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                out = null;
                e.printStackTrace();
            }
        }

        public static Log inLogDir(String fileName) {
            return new Log(DEFAULT_FILE_PATH + '/' + fileName);
        }

        public void loglnA(Object ... objs) {
            logln(Util.arrToString(objs, 0));
        }
        public void logln(String str) {
            log(str + '\n');
            flush();
        }
        public void logA(Object ... objs) {
            log(Util.arrToString(objs, 0));
        }
        public void log(String str) {
            if (out != null) {
                try {
                    out.write(str.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void flush() {
            if (out != null) {
                try {
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void close() {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out = null;
            }
        }
    }
}
