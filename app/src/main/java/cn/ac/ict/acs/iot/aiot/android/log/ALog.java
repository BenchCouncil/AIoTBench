package cn.ac.ict.acs.iot.aiot.android.log;

import com.github.labowenzi.commonj.log.Log;

/**
 * Created by alanubu on 20-1-19.
 */
public class ALog implements Log.ILog {
    @Override
    public int v(String tag, String msg) {
        return android.util.Log.v(tag, msg);
    }

    @Override
    public int v(String tag, String msg, Throwable tr) {
        return android.util.Log.v(tag, msg, tr);
    }

    @Override
    public int d(String tag, String msg) {
        return android.util.Log.d(tag, msg);
    }

    @Override
    public int d(String tag, String msg, Throwable tr) {
        return android.util.Log.d(tag, msg, tr);
    }

    @Override
    public int i(String tag, String msg) {
        return android.util.Log.i(tag, msg);
    }

    @Override
    public int i(String tag, String msg, Throwable tr) {
        return android.util.Log.i(tag, msg, tr);
    }

    @Override
    public int w(String tag, String msg) {
        return android.util.Log.w(tag, msg);
    }

    @Override
    public int w(String tag, String msg, Throwable tr) {
        return android.util.Log.w(tag, msg, tr);
    }

    @Override
    public int e(String tag, String msg) {
        return android.util.Log.e(tag, msg);
    }

    @Override
    public int e(String tag, String msg, Throwable tr) {
        return android.util.Log.e(tag, msg, tr);
    }
}
