package com.yirancrazy.minimall.id.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeTest {

    @Test
    void generated_ids_are_monotonic_and_unique() {
        Snowflake sf = new Snowflake(1L, 1L);
        long prev = 0L;
        for (int i = 0; i < 1000; i++) {
            long id = sf.nextId();
            assertTrue(id > prev, "id should increase: " + id + " after " + prev);
            prev = id;
        }
    }

    @Test
    void id_keeps_top_bits_consistent() {
        Snowflake sf = new Snowflake(7L, 7L);
        long id = sf.nextId();
        assertTrue(id > 0);
    }
}