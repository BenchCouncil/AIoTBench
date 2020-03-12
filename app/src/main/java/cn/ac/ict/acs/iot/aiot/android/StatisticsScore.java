package cn.ac.ict.acs.iot.aiot.android;

import androidx.annotation.NonNull;

import cn.ac.ict.acs.iot.aiot.android.util.MathUtil;

/**
 * Created by alanubu on 19-12-13.
 */
public class StatisticsScore {

    public final MathUtil.StatisticsFloat scores;
    public final boolean[] maxHit;

    public int target;
    public float firstHitRecognitionDepth;

    public StatisticsScore(int topK, float[] scores) {
        this.scores = new MathUtil.StatisticsFloat(topK, scores);
        this.maxHit = new boolean[this.scores.max.length];

        this.target = -1;
        this.firstHitRecognitionDepth = -1;
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
        if (maxHit[0]) {
            // first hit;
            // 可辨识度
            double maxDiff = (scores.max[0] - scores.max[1]) / scores.max[0];
            firstHitRecognitionDepth = (float) maxDiff;
        } else {
            firstHitRecognitionDepth = -1;
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
        s.append(' ').append("firstHitRecognitionDepth=").append(firstHitRecognitionDepth);
        s.append(' ').append("target=").append(target);
        return s.toString();
    }

    public static class HitStatistic {
        public final int topK;
        public int count;
        public final int[] topKMaxHit;
        public final float[] topKMaxHitA;

        public HitStatistic(int topK) {
            this.topK = topK;
            topKMaxHit = new int[topK];
            topKMaxHitA = new float[topK];
            count = 0;
            for (int i=0; i<topKMaxHit.length; ++i) {
                topKMaxHit[i] = 0;
                topKMaxHitA[i] = 0;
            }
        }

        public void updateBy(StatisticsScore s) {
            count += 1;
            for (int i=0; i<topKMaxHit.length; ++i) {
                topKMaxHit[i] += s.maxHit[i] ? 1 : 0;
                topKMaxHitA[i] = ((float) topKMaxHit[i]) / count;
            }
        }
    }
}
