package cn.ac.ict.acs.iot.aiot.android.util;

/**
 * Created by alanubu on 19-12-27.
 */
public class MathUtil {

    public abstract static class Statistics {
        public static final int DEFAULT_TOP_K = 3;

        public final int topK;
        public final double[] max;
        public final int[] maxIndex;
        public final double[] min;
        public final int[] minIndex;
        public double avg;
        public double sd;  // standard deviation

        public Statistics(int topK) {
            this.topK = topK;
            max = new double[topK];
            maxIndex = new int[max.length];
            min = new double[topK];
            minIndex = new int[min.length];
        }

        public abstract int length();
        public abstract double get(int index);

        public void calc() {
            int length = length();
            if (length <= 0) {
                for (int i=0; i<max.length; ++i) {
                    max[i] = 0;
                    maxIndex[i] = -1;
                    min[i] = 0;
                    minIndex[i] = -1;
                }
                avg = 0;
                sd = 0;
                return;
            }
            for (int i=0; i<max.length; ++i) {
                max[i] = -Float.MAX_VALUE;
                maxIndex[i] = -1;
                min[i] = Float.MAX_VALUE;
                minIndex[i] = -1;
            }
            double sum = 0;
            for (int i = 0; i < length; i++) {
                updateTopKMax(i);
                updateTopKMin(i);
                sum += get(i);
            }
            avg = sum / length;
            sum = 0;
            for (int i = 0; i < length; i++) {
                sum += (get(i)-avg) * (get(i)-avg);
            }
            sd = Math.sqrt(sum/length);
        }
        private void updateTopKMax(int index) {
            double v = get(index);
            int i;
            for (i=max.length-1; i>=0; --i) {
                if (v < max[i]) {
                    break;
                }
            }
            ++i;
            for (int j=max.length-2; j>=0 && j>=i; --j) {
                max[j+1] = max[j];
                maxIndex[j+1] = maxIndex[j];
            }
            if (i < max.length) {
                max[i] = v;
                maxIndex[i] = index;
            }
        }
        private void updateTopKMin(int index) {
            double v = get(index);
            int i;
            for (i=min.length-1; i>=0; --i) {
                if (v > min[i]) {
                    break;
                }
            }
            ++i;
            for (int j=min.length-2; j>=0 && j>=i; --j) {
                min[j+1] = min[j];
                minIndex[j+1] = minIndex[j];
            }
            if (i < min.length) {
                min[i] = v;
                minIndex[i] = index;
            }
        }
    }
    public static class StatisticsFloat extends Statistics {
        public final float[] data;

        public StatisticsFloat(int topK, float[] data) {
            super(topK);
            this.data = data;
        }

        @Override
        public int length() {
            return data == null ? -1 : data.length;
        }

        @Override
        public double get(int index) {
            return data[index];
        }
    }
    public static class StatisticsInt extends Statistics {
        public final int[] data;

        public StatisticsInt(int topK, int[] data) {
            super(topK);
            this.data = data;
        }

        @Override
        public int length() {
            return data == null ? -1 : data.length;
        }

        @Override
        public double get(int index) {
            return data[index];
        }
    }
    public static class StatisticsLong extends Statistics {
        public final long[] data;

        public StatisticsLong(int topK, long[] data) {
            super(topK);
            this.data = data;
        }

        @Override
        public int length() {
            return data == null ? -1 : data.length;
        }

        @Override
        public double get(int index) {
            return data[index];
        }
    }
}
