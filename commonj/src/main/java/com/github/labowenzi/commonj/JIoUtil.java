package com.github.labowenzi.commonj;

import java.io.File;

/**
 * Created by alanubu on 18-7-29.
 */
public class JIoUtil {

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
}
