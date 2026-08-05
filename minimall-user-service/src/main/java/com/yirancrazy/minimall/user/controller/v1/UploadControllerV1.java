package com.yirancrazy.minimall.user.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端文件上传控制器，提供 MinIO 预签名 URL 生成（头像等）。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@RestController
@RequestMapping("/api/v1/user/upload")
public class UploadControllerV1 {

    private final MinioUtil minioUtil;

    public UploadControllerV1(MinioUtil minioUtil) {
        this.minioUtil = minioUtil;
    }

    /**
     * 生成上传用预签名 PUT URL，有效期 15 分钟。
     * @param objectKey 目标对象 key，可选；为空时自动生成 UUID
     * @return 预签名 URL
     */
    @GetMapping("/presigned-url")
    public Result<String> presignedUrl(@RequestParam(required = false) String objectKey) {
        return Result.success(minioUtil.presignedPutUrl(objectKey));
    }
}
