package cn.ac.ict.acs.iot.aiot.android.dataset;

/**
 * Created by alanubu on 20-1-19.
 */
public interface IImageClasses {
    /**
     * @param index class id, index;
     * @return class's name;
     */
    String getName(int index);
    /**
     * @param index class id, index;
     * @return class's desc;
     */
    String getDesc(int index);
    /**
     * @return size of all classes;
     */
    int getSize();
}
