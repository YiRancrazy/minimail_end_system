package com.yirancrazy.minimall.id.service;

/**
* 雪花 ID 算法工具类，根据 datacenterId 与 workerId 生成趋势递增的分布式唯一 ID，线程安全。
 */
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

    /**
     * 生成下一个雪花 ID，同一毫秒内通过自增序列区分；如时钟回拨则抛出异常，否则自旋至下一毫秒。
     *
     * @return 生成的分布式唯一 ID
     */
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