package com.common.storage.constant;

public enum QualityVideoRank {
    P240(400000, 240),
    P360(600000, 360),
    P480(800000, 480),
    P720(4000000, 720),
    P1080(10000000, 1080);

    public int bitRate;
    public int min;

    QualityVideoRank(int bitRate, int min) {
        this.bitRate = bitRate;
        this.min = min;
    }
}
