package cn.ac.ict.acs.iot.aiot.android.util;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * thread pool;
 * Created by hexiwen on 16-12-29.
 */

public final class ThreadPoolUtil {
    private static final String TAG = "ThreadPoolUtil";

    private ThreadPoolUtil() {
    }

    private static boolean runOnPool = true;
    private static final Object executorLock = new Object();
    private static ExecutorService executor = null;

    public static final int CORE_POOL_SIZE = 0;
    public static final int MAX_POOL_SIZE = 16;
    public static final long KEEP_ALIVE_TIME = 60L;

    @NonNull
    public static ExecutorService getExecutor() {
        if (executor == null) {
            synchronized (executorLock) {
                if (executor == null) {
                    executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
                            KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                            new SynchronousQueue<Runnable>());
                }
            }
        }
        return executor;
    }

    public static boolean isRunOnPool() {
        return runOnPool;
    }
    public static void setRunOnPool(boolean runOnPool) {
        ThreadPoolUtil.runOnPool = runOnPool;
    }

    public static void threadPoolRun(Runnable runnable) {
        try {
            getExecutor().execute(runnable);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
    public static void threadPoolRunThrows(Runnable runnable) {
        getExecutor().execute(runnable);
    }
    public static Thread newThreadRun(Runnable runnable) {
        try {
            Thread t = new Thread(runnable);
            t.start();
            return t;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }
    public static Thread newThreadRunThrows(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.start();
        return t;
    }
    public static void run(Runnable runnable) {
        if (runOnPool) threadPoolRun(runnable);
        else newThreadRun(runnable);
    }
    public static void runThrows(Runnable runnable) {
        if (runOnPool) threadPoolRunThrows(runnable);
        else newThreadRunThrows(runnable);
    }
}
