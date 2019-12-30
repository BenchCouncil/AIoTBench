package cn.ac.ict.acs.iot.aiot.android.pytorch;

import androidx.annotation.NonNull;

import cn.ac.ict.acs.iot.aiot.android.util.MathUtil;

/**
 * Created by alanubu on 19-12-13.
 */
public class PyTorchScoreStatistics {

    public final MathUtil.StatisticsFloat scores;
    public final boolean[] maxHit;

    public int target;

    public PyTorchScoreStatistics(float[] scores) {
        this.scores = new MathUtil.StatisticsFloat(scores);
        this.maxHit = new boolean[this.scores.max.length];
    }

    public void calc() {
        for (int i=0; i<maxHit.length; ++i) {
            maxHit[i] = false;
        }
        scores.calc();
    }

    public void updateHit(int target) {
        this.target = target;
        int[] maxScoreIndex = scores.maxIndex;
        int i;
        for (i=0; i<maxScoreIndex.length; ++i) {
            if (maxScoreIndex[i] == target) {
                break;
            }
        }
        for (int j=0; j<i && j<maxScoreIndex.length; ++j) {
            maxHit[j] = false;
        }
        for (int j=i; j<maxScoreIndex.length; ++j) {
            maxHit[j] = true;
        }
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("score.len=" + scores.length());
        s.append(", " + "maxIndex=");
        for (int i=0; i<scores.maxIndex.length; ++i) {
            s.append(scores.maxIndex[i]).append(',');
        }
        s.append(' ' + "maxScore=");
        for (int i=0; i<scores.max.length; ++i) {
            s.append(scores.max[i]).append(',');
        }
        s.append(' ').append("target=").append(target);
        return s.toString();
    }

    public static class HitStatistic {
        public int count;
        public int[] topKMaxHit = new int[MathUtil.Statistics.TOP_K_MAX];
        public float[] topKMaxHitA = new float[MathUtil.Statistics.TOP_K_MAX];

        public HitStatistic() {
            count = 0;
            for (int i=0; i<topKMaxHit.length; ++i) {
                topKMaxHit[i] = 0;
                topKMaxHitA[i] = 0;
            }
        }

        public void updateBy(PyTorchScoreStatistics s) {
            count += 1;
            for (int i=0; i<topKMaxHit.length; ++i) {
                topKMaxHit[i] += s.maxHit[i] ? 1 : 0;
                topKMaxHitA[i] = ((float) topKMaxHit[i]) / count;
            }
        }
    }
}
