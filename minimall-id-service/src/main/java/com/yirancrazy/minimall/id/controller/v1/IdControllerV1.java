package com.yirancrazy.minimall.id.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.id.dto.IdNextDTO;
import com.yirancrazy.minimall.id.service.IdService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/id")
public class IdControllerV1 {

    private final IdService idService;

    public IdControllerV1(IdService idService) {
        this.idService = idService;
    }

    @GetMapping("/next")
    public Result<IdNextDTO> next(@RequestParam("bizTag") String bizTag) {
        long id = idService.nextId(bizTag);
        return Result.success(new IdNextDTO(bizTag, id));
    }
}