package com.yirancrazy.minimall.id.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.id.dto.IdNextDTO;
import com.yirancrazy.minimall.id.service.IdService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ID 内部接口控制器，提供基于业务标签的号段下发能力，仅供服务间内部调用。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@RestController
@RequestMapping("/internal/id")
public class IdControllerV1 {

    private final IdService idService;

    public IdControllerV1(IdService idService) {
        this.idService = idService;
    }

    /**
     * 根据业务标签获取下一个全局唯一 ID，封装为统一 Result 返回。
     *
     * @param bizTag 业务标签，用于区分不同业务域的号段（如 ORDER、USER 等）
     * @return 包含业务标签与新生成 ID 的统一返回结果
     */
    @GetMapping("/next")
    public Result<IdNextDTO> next(@RequestParam("bizTag") String bizTag) {
        long id = idService.nextId(bizTag);
        return Result.success(new IdNextDTO(bizTag, id));
    }
}