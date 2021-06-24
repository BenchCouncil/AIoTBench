package cn.ac.ict.acs.iot.aiot.android.util;

import android.app.ActivityManager;
import android.content.Context;
import android.widget.Toast;

import com.github.labowenzi.commonj.JIoUtil;
import com.github.labowenzi.commonj.log.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by alanubu on 19-12-13.
 */
public class Util {

    public static void debugLogE(String tag, String msg) {
        Log.e(String.valueOf(tag), String.valueOf(msg));
    }
    public static void debugLogEArray(Object ... msg) {
        String tag;
        String msgStr;
        if (msg == null || msg.length <= 0) {
            tag = "debugLogE";
            msgStr = "Empty";
        } else if (msg.length == 1) {
            tag = String.valueOf(msg[0]);
            msgStr = "Empty";
        } else {
            tag = String.valueOf(msg[0]);
            msgStr = arrToString(msg, 1);
        }
        debugLogE(tag, msgStr);
    }
    public static String arrToString(Object[] msg, int start) {
        int len = (msg == null ? -1 : msg.length) - start;
        String msgStr;
        if (msg == null || start < 0 || len <= 0) {
            msgStr = "Empty";
        } else if (len == 1) {
            msgStr = String.valueOf(msg[start]);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(msg[start]);
            for (int i = start+1; i < msg.length; ++i) {
                sb.append("  -  ").append(msg[i]);
            }
            msgStr = sb.toString();
        }
        return msgStr;
    }

    public static void showToast(String s, Context context) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }
    public static void showToast(int resId, Context context) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }
    public static void showToast(String s, Context context, final int duration) {
        Toast.makeText(context, s, duration).show();
    }
    public static void showToast(int resId, Context context, final int duration) {
        Toast.makeText(context, resId, duration).show();
    }

    /**
     * Copies specified asset to the file in /files app directory and returns this file absolute path.
     *
     * @return absolute file path
     */
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        file.getParentFile().mkdirs();

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
                JIoUtil.closeSilently(os);
            }
            JIoUtil.closeSilently(is);
            return file.getAbsolutePath();
        }
    }

    /**
     * 总的运存（内存）大小;
     */
    public static long getTotalRamSize(Context context) {
        String dir = "/proc/meminfo";
        try {
            FileReader fr = new FileReader(dir);
            BufferedReader br = new BufferedReader(fr, 2048);
            String memoryLine = br.readLine();
            String subMemoryLine = memoryLine.substring(memoryLine.indexOf("MemTotal:"));
            br.close();
            return Integer.parseInt(subMemoryLine.replaceAll("\\D+", "")) * 1024L;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取当前可用运存（内存），返回数据以字节为单位。
     */
    public static long getAvailableMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }
}
