package com.ghouse354.chronicle;

public class COptions {
    public static enum TimeGranularity {
        Seconds,
        Milliseconds
    }

    public TimeGranularity timeGranularity = TimeGranularity.Seconds;
    public boolean plotTime = true;
}