package cn.ac.ict.acs.iot.aiot.android.dataset;

/**
 * Created by alanubu on 20-1-19.
 */
public interface IDataset {
    /**
     * @return how many images to test;
     */
    int size();

    /**
     * @param index image index, see {@link #size()};
     * @return image file path;
     */
    String get(int index);

    /**
     * @param index image index, see {@link #size()};
     * @return the class's id(in this database, id is index) of the image;
     */
    int getClassIndex(int index);

    /**
     * @return info of the image's class;
     */
    IImageClasses getClassesInfo();

    boolean isStatusOk();
}
