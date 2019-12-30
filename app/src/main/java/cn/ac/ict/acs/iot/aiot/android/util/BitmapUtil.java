package cn.ac.ict.acs.iot.aiot.android.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Created by alanubu on 19-12-13.
 */
public class BitmapUtil {

    public static Bitmap resizeImage(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) w) / width;
        float scaleHeight = ((float) h) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // if you want to rotate the Bitmap
        // matrix.postRotate(45);
        return Bitmap.createBitmap(bitmap, 0, 0, width,
                height, matrix, true);
    }

    public static Bitmap centerCropResize(Bitmap bm, int newWidth, int newHeight) {
        Bitmap bitmap = centerCrop(bm, newWidth, newHeight);
        return resizeImage(bitmap, newWidth, newHeight);
    }
    public static Bitmap centerCrop(Bitmap bm, int newWidth, int newHeight) {
        int w = bm.getWidth(); // 得到图片的宽，高
        int h = bm.getHeight();
        int retX;
        int retY;
        double wh = (double) w / (double) h;
        double nwh = (double) newWidth / (double) newHeight;
        if (wh > nwh) {
            retX = h * newWidth / newHeight;
            retY = h;
        } else {
            retX = w;
            retY = w * newHeight / newWidth;
        }
        int startX = w > retX ? (w - retX) / 2 : 0;//基于原图，取正方形左上角x坐标
        int startY = h > retY ? (h - retY) / 2 : 0;
        return Bitmap.createBitmap(bm, startX, startY, retX, retY, null, false);
    }
}
