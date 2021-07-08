package cn.ac.ict.acs.iot.aiot.android.dataset;

/**
 * Created by chauncey on 21-6-12.
 */
public interface ICocoClasses {
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
