package com.yirancrazy.minimall.stock.service;

public interface StockService {
    boolean reserve(Long skuId, Integer quantity);

    boolean release(Long skuId, Integer quantity);

    long query(Long skuId);
}