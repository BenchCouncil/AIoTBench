package cn.ac.ict.acs.iot.aiot.android.statistics;

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

        public StartEndTime taskTotal = new StartEndTime();
        public StartEndTime[] images = null;

        public Statistics statistics;

        public TimeRecord() {
        }

        public void calc(int timeRecordTopK) {
            if (images == null) {
                statistics = null;
            } else {
                statistics = new Statistics(timeRecordTopK, images);
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
                s.append("\navg=").append(floatFormat(statistics.avg))
                        .append(", sd=").append(floatFormat(statistics.sd));
                s.append("\nfirstTime=").append(floatFormat(statistics.firstTime))
                        .append(", lastTime=").append(floatFormat(statistics.lastTime));
                s.append(", avgWithoutFirstTime=").append(floatFormat(statistics.avgWithoutFirstTime));
            }
            s.append("\ntotal time = ").append(taskTotal.diff());
            return s.toString();
        }
        private String floatFormat(double f) {
            return String.format("%.2f", f);
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

        public Statistics(int topK, TimeRecord.StartEndTime[] data) {
            super(topK, toLong(data));
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
