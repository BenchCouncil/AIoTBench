package com.github.labowenzi.commonj.log;

/**
 * Created by alanubu on 20-1-19.
 */
public class Log {
    private static final String TAG = "log";

    private static ILog instance = null;

    public static ILog getInstance() {
        if (instance == null) {
            instance = new LogDefault();
        }
        return instance;
    }
    public static void setInstance(ILog instance) {
        Log.instance = instance;
    }

    public static int v(String tag, String msg) {
        return getInstance().v(tag, msg);
    }
    public static int v(String tag, String msg, Throwable tr) {
        return getInstance().v(tag, msg, tr);
    }

    public static int d(String tag, String msg) {
        return getInstance().d(tag, msg);
    }
    public static int d(String tag, String msg, Throwable tr) {
        return getInstance().d(tag, msg, tr);
    }

    public static int i(String tag, String msg) {
        return getInstance().i(tag, msg);
    }
    public static int i(String tag, String msg, Throwable tr) {
        return getInstance().i(tag, msg, tr);
    }

    public static int w(String tag, String msg) {
        return getInstance().w(tag, msg);
    }
    public static int w(String tag, String msg, Throwable tr) {
        return getInstance().w(tag, msg, tr);
    }

    public static int e(String tag, String msg) {
        return getInstance().e(tag, msg);
    }
    public static int e(String tag, String msg, Throwable tr) {
        return getInstance().e(tag, msg, tr);
    }

    public interface ILog {
        int v(String tag, String msg);
        int v(String tag, String msg, Throwable tr);

        int d(String tag, String msg);
        int d(String tag, String msg, Throwable tr);

        int i(String tag, String msg);
        int i(String tag, String msg, Throwable tr);

        int w(String tag, String msg);
        int w(String tag, String msg, Throwable tr);

        int e(String tag, String msg);
        int e(String tag, String msg, Throwable tr);
    }

    public static class LogDefault implements ILog {
        @Override
        public int v(String tag, String msg) {
            System.out.println(tag + ": " + msg);
            return 0;
        }

        @Override
        public int v(String tag, String msg, Throwable tr) {
            System.out.println(tag + ": " + msg + "  tr=" + tr);
            return 0;
        }

        @Override
        public int d(String tag, String msg) {
            System.out.println(tag + ": " + msg);
            return 0;
        }

        @Override
        public int d(String tag, String msg, Throwable tr) {
            System.out.println(tag + ": " + msg + "  tr=" + tr);
            return 0;
        }

        @Override
        public int i(String tag, String msg) {
            System.out.println(tag + ": " + msg);
            return 0;
        }

        @Override
        public int i(String tag, String msg, Throwable tr) {
            System.out.println(tag + ": " + msg + "  tr=" + tr);
            return 0;
        }

        @Override
        public int w(String tag, String msg) {
            System.err.println(tag + ": " + msg);
            return 0;
        }

        @Override
        public int w(String tag, String msg, Throwable tr) {
            System.err.println(tag + ": " + msg + "  tr=" + tr);
            return 0;
        }

        @Override
        public int e(String tag, String msg) {
            System.err.println(tag + ": " + msg);
            return 0;
        }

        @Override
        public int e(String tag, String msg, Throwable tr) {
            System.err.println(tag + ": " + msg + "  tr=" + tr);
            return 0;
        }
    }
}
