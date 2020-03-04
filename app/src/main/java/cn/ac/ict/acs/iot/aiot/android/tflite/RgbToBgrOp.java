package cn.ac.ict.acs.iot.aiot.android.tflite;

import android.graphics.Bitmap;
import android.graphics.Color;

import org.tensorflow.lite.support.image.ImageOperator;
import org.tensorflow.lite.support.image.TensorImage;

/**
 * Created by alanubu on 20-2-19.
 */
public class RgbToBgrOp implements ImageOperator {

    public RgbToBgrOp() {
    }

    @Override
    public TensorImage apply(TensorImage image) {
        Bitmap bm = image.getBitmap();
        for (int i=0; i<bm.getWidth(); ++i) {
            for (int j=0; j<bm.getHeight(); ++j) {
                int px = bm.getPixel(i, j);
                int a = Color.alpha(px);
                int r = Color.red(px);
                int g = Color.green(px);
                int b = Color.blue(px);
                px = Color.argb(a, r, g, b);
                bm.setPixel(i, j, px);
            }
        }
        image.load(bm);
        return image;
    }
}
