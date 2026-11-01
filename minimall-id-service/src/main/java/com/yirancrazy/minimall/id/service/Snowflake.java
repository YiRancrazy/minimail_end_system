package com.yirancrazy.minimall.id.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于系统毫秒时钟的同步 64 位 Snowflake 生成器。组合 41 位时间戳偏移、
 *     5 位数据中心 ID、5 位工作节点 ID 和 12 位毫秒内序列；检测到时钟回拨时立即失败。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class Snowflake {
    private final long epoch = 1700000000000L; // 起始时间戳
    private final long datacenterId; // 数据中心ID，一般占5位（0~31），由配置文件或环境变量指定
    private final long workerId; // 工作机器ID，一般占5位（0~31），由配置文件或环境变量指定
    private final long sequenceBits = 12L; // 序列号占用的位数，12位可支持每毫秒每节点生成4096个ID
    private final long workerIdShift = sequenceBits; // 工作机器ID左移位数 = 序列号位数，即低位留给序列号
    private final long datacenterIdShift = sequenceBits + 5L; // 数据中心ID左移位数 = 序列号位数 + 工作机器ID位数（5位）
    private final long timestampShift =
            sequenceBits + 5L + 5L; // 时间戳左移位数 = 序列号位数 + 工作机器ID位数 + 数据中心ID位数
    private final long sequenceMask =
            ~(-1L << sequenceBits); // 序列号掩码，用于将序列号限制在 [0, 2^sequenceBits - 1] 范围内
    private long lastTs = -1L; // 上次生成ID的时间戳（毫秒），用于判断时钟回拨和计算序列号重置
    private long sequence = 0L; // 当前毫秒内的序列号，从0开始递增，达到最大值后阻塞或重置

    /**
     * 为指定数据中心和工作节点创建生成器。
     *
     * <p>两个节点编号都必须位于 [0, 31]；运行实例之间必须使用唯一组合，避免生成重复 ID。
     *
     * @param datacenterId 位于 [0, 31] 范围内的数据中心编号
     * @param workerId 位于 [0, 31] 范围内的工作节点编号
     * @throws IllegalArgumentException 任一编号超出 [0, 31] 范围时抛出
     */
    public Snowflake(long datacenterId, long workerId) {
        if (datacenterId < 0 || datacenterId > 31) {
            throw new IllegalArgumentException("datacenterId 超出范围 [0,31]");
        }
        if (workerId < 0 || workerId > 31) {
            throw new IllegalArgumentException("workerId 超出范围 [0,31]");
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    /**
     * 为当前节点生成下一个标识符。
     *
     * <p>方法采用同步访问并更新 lastTs 和 sequence；在当前毫秒内递增 12 位序列，序列耗尽后
     * 等待下一毫秒，并拒绝早于上次调用的时间戳。
     *
     * @return 由时间戳、节点编号和序列组合而成的 64 位标识符
     * @throws IllegalStateException 系统时钟发生回拨时抛出
     */
    public synchronized long nextId() {
        long ts = System.currentTimeMillis();

        // 判断时钟是否回拨
        if (ts < lastTs) {
            throw new IllegalStateException("时钟回拨");
        }

        // 判断是否为同一毫秒
        if (ts == lastTs) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                ts = waitNextMillis(lastTs);
            }
        } else {
            // 如果是下一毫秒，则重置序列号
            sequence = 0L;
        }
        lastTs = ts;
        return ((ts - epoch) << timestampShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    /**
     * 自旋等待系统时钟超过指定毫秒。
     *
     * <p>当前毫秒的序列空间耗尽后调用该辅助方法。等待期间线程保持活跃，因此会持续消耗 CPU。
     *
     * @param last 上一次用于生成 ID 的时间戳
     * @return 首次观察到的、晚于 last 的时间戳
     */
    private long waitNextMillis(long last) {
        long ts = System.currentTimeMillis();

        // 自旋等待下一毫秒,解决轻微时钟回拨,严重时钟回拨只能被动等待。
        while (ts <= last) {
            ts = System.currentTimeMillis();
        }

        return ts;
    }
}
