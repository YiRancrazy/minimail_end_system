package com.yirancrazy.minimall.id.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.id.service.IdService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 内部 Snowflake 发号 HTTP 入口。控制器将 GET /internal/id/next 请求委托给
 *     IdService，并用 Result 封装结果；本身不负责生成算法或持久化。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/id")
public class IdControllerV1 {

    private final IdService idService;

    /**
     * 创建内部 ID 控制器。
     *
     * @param idService 用于获取下一个 ID 的发号服务
     */
    public IdControllerV1(IdService idService) {
        this.idService = idService;
    }

    /**
     * 处理 GET /internal/id/next 请求并返回下一个生成的 ID。
     *
     * <p>业务标签会原样传给 IdService，但当前实现不会使用该值；每次调用都会推进共享生成器状态。
     *
     * @param bizTag 调用方传入的业务标签，例如 ORDER 或 PAY
     * @return 包含生成的 long 型 ID 的成功响应
     * @throws IllegalStateException 生成器检测到时钟回拨时抛出
     */
    @GetMapping("/next")
    public Result<Long> next(@RequestParam("bizTag") String bizTag) {
        long id = idService.nextId(bizTag);
        return Result.success(id);
    }
}
