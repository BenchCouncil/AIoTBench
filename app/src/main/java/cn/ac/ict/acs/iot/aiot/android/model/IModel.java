package cn.ac.ict.acs.iot.aiot.android.model;

/**
 * Created by alanubu on 20-1-20.
 */
public interface IModel {

    /**
     * @return image width;
     */
    int getInputImageWidth();
    /**
     * @return image height;
     */
    int getInputImageHeight();

    boolean isStatusOk();

    /**
     * do image classification, background, set send statistics data by handler;
     */
    void doImageClassification();
    void doObjectDetection();
    void doSuperResolution();
}
