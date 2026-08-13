package com.yirancrazy.minimall.id.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Snowflake，提供ID生成相关能力
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class Snowflake {
    private final long epoch = 1700000000000L;                          // 起始时间戳
    private final long datacenterId;                                    // 数据中心ID，一般占5位（0~31），由配置文件或环境变量指定
    private final long workerId;                                        // 工作机器ID，一般占5位（0~31），由配置文件或环境变量指定
    private final long sequenceBits = 12L;                              // 序列号占用的位数，12位可支持每毫秒每节点生成4096个ID
    private final long workerIdShift = sequenceBits;                    // 工作机器ID左移位数 = 序列号位数，即低位留给序列号
    private final long datacenterIdShift = sequenceBits + 5L;           // 数据中心ID左移位数 = 序列号位数 + 工作机器ID位数（5位）
    private final long timestampShift = sequenceBits + 5L + 5L;         // 时间戳左移位数 = 序列号位数 + 工作机器ID位数 + 数据中心ID位数
    private final long sequenceMask = ~(-1L << sequenceBits);           // 序列号掩码，用于将序列号限制在 [0, 2^sequenceBits - 1] 范围内
    private long lastTs = -1L;                                          // 上次生成ID的时间戳（毫秒），用于判断时钟回拨和计算序列号重置
    private long sequence = 0L;                                         // 当前毫秒内的序列号，从0开始递增，达到最大值后阻塞或重置

    /**
     * 构造 Snowflake ID 生成器实例
     * @param datacenterId 数据中心id
     * @param workerId 工作机器id
     */
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
     * @return 生成的分布式唯一 ID
     */
    public synchronized long nextId() {
        long ts = System.currentTimeMillis();

        // 判断时钟是否回拨
        if (ts < lastTs) {
            throw new IllegalStateException("clock moved backwards");
        }

        // 判断是否为同一毫秒
        if (ts == lastTs) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                ts = waitNextMillis(lastTs);
            }
        }

        // 如果是下一毫秒，则重置序列号
        else {
            sequence = 0L;
        }
        lastTs = ts;
        return ((ts - epoch) << timestampShift)
            | (datacenterId << datacenterIdShift)
            | (workerId << workerIdShift)
            | sequence;
    }

    /**
     * 自旋等待下一毫秒
     * @param last 上次生成ID的时间戳
     * @return 下一毫秒的时间戳
     */
    private long waitNextMillis(long last) {
        long ts = System.currentTimeMillis();
        while (ts <= last) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }
}