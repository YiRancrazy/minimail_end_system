package com.yirancrazy.minimall.id.service;

public class Snowflake {
    private final long epoch = 1700000000000L;
    private final long datacenterId;
    private final long workerId;
    private final long sequenceBits = 12L;
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + 5L;
    private final long timestampShift = sequenceBits + 5L + 5L;
    private final long sequenceMask = ~(-1L << sequenceBits);
    private long lastTs = -1L;
    private long sequence = 0L;

    public Snowflake(long datacenterId, long workerId) {
        if (datacenterId < 0 || datacenterId > 31) {
            throw new IllegalArgumentException("datacenterId out of range [0,31]");
        }
        if (workerId < 0 || workerId > 31) {
            throw new IllegalArgumentException("workerId out of range [0,31]");
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTs) {
            throw new IllegalStateException("clock moved backwards");
        }
        if (ts == lastTs) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                ts = waitNextMillis(lastTs);
            }
        } else {
            sequence = 0L;
        }
        lastTs = ts;
        return ((ts - epoch) << timestampShift)
            | (datacenterId << datacenterIdShift)
            | (workerId << workerIdShift)
            | sequence;
    }

    private long waitNextMillis(long last) {
        long ts = System.currentTimeMillis();
        while (ts <= last) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }
}