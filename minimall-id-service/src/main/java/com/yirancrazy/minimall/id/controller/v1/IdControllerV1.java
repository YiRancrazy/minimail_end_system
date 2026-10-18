package com.yirancrazy.minimall.id.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.id.service.IdService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/internal/id")
public class IdControllerV1 {

    private final IdService idService;

    /**
     * 构造 IdControllerV1 实例。
     * @param idService idService 参数
     */
    public IdControllerV1(IdService idService) {
        this.idService = idService;
    }


    /**
     * 执行 next 方法。
     * @param bizTag bizTag 参数
     * @return 统一响应结果
     */
    @GetMapping("/next")
    public Result<Long> next(@RequestParam("bizTag") String bizTag) {
        long id = idService.nextId(bizTag);
        return Result.success(id);
    }
}