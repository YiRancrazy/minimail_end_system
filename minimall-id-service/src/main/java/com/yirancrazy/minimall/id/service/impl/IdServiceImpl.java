package com.yirancrazy.minimall.id.service.impl;

import com.yirancrazy.minimall.id.service.IdService;
import com.yirancrazy.minimall.id.service.Snowflake;
import org.springframework.stereotype.Service;

@Service
public class IdServiceImpl implements IdService {

    private final Snowflake snowflake = new Snowflake(1L, 1L);

    @Override
    public long nextId(String bizTag) {
        return snowflake.nextId();
    }
}