package cn.ac.ict.acs.iot.aiot.android;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import cn.ac.ict.acs.iot.aiot.android.util.MathUtil;

/**
 * Created by alanubu on 19-12-31.
 */
public class StatisticsTime {
    public static class TimeRecord {
        public StartEndTime loadModel = new StartEndTime();
        public StartEndTime loadDataset = new StartEndTime();

        public StartEndTime[] images = null;

        public Statistics statistics;

        public void calc() {
            if (images == null) {
                statistics = null;
            } else {
                statistics = new Statistics(images);
                statistics.calc();
            }
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append("load model=").append(loadModel.diff());
            s.append(", load dataset=").append(loadDataset.diff());
            if (statistics != null) {
                s.append(", images.len=").append(statistics.length());
                s.append("\n" + "maxIndex=").append(statistics.maxIndex[0]);
                s.append(' ' + "max=").append(statistics.max[0]);
                s.append(", " + "minIndex=").append(statistics.minIndex[0]);
                s.append(' ' + "min=").append(statistics.min[0]);
                s.append(", avg=").append(statistics.avg)
                        .append(", sd=").append(statistics.sd);
                s.append("\nfirstTime=").append(statistics.firstTime)
                        .append(", lastTime=").append(statistics.lastTime);
                s.append(", avgWithoutFirstTime=").append(statistics.avgWithoutFirstTime);
            }
            return s.toString();
        }

        public static class StartEndTime {
            public long start = -1;
            public long end = -1;

            public void setStart() {
                start = time();
            }
            public void setEnd() {
                end = time();
            }
            public long diff() {
                long d = end - start;
                if (d < 0) {
                    return -1;
                } else {
                    return d;
                }
            }

            @NonNull
            @Override
            public String toString() {
                return "s=" + start + ",e=" + end + ",d=" + diff();
            }
        }

        public static long time() {
            return SystemClock.elapsedRealtime();
        }
    }

    public static class Statistics extends MathUtil.StatisticsLong{
        public double firstTime;
        public double lastTime;
        public double avgWithoutFirstTime;

        public Statistics(TimeRecord.StartEndTime[] data) {
            super(toLong(data));
        }

        @Override
        public void calc() {
            super.calc();
            int len = length();
            if (len > 0) {
                firstTime = get(0);
                lastTime = get(len - 1);
                if (len > 1) {
                    double sum = 0;
                    for (int i = 1; i < len; ++i) {
                        sum += get(i);
                    }
                    avgWithoutFirstTime = sum / (len - 1);
                } else {
                    avgWithoutFirstTime = 0;
                }
            } else {
                firstTime = -1;
                lastTime = -1;
                avgWithoutFirstTime = -1;
            }
        }

        public static long[] toLong(TimeRecord.StartEndTime[] data) {
            if (data == null) {
                return null;
            }
            int i;
            for (i=0; i<data.length; ++i) {
                TimeRecord.StartEndTime set = data[i];
                if (set == null) {
                    break;
                }
            }
            if (i <= 0) {
                return new long[0];
            }
            long[] ret = new long[i];
            for (int j=0; j<i; ++j) {
                ret[j] = data[j].diff();
            }
            return ret;
        }
    }
}
