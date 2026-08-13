package com.yirancrazy.minimall.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 文件上传业务错误码枚举，供用户端/商家端上传接口统一使用。
 * @Version: 1.0
 * @DateTime: 2026/08/12
 **/
@Getter
@AllArgsConstructor
public enum UploadCodeEnum implements BaseEnum {

    CHUNK_PARAM_INVALID("20030", "分片参数非法", "分片序号或总分片数非法"),
    CHUNK_UPLOAD_FAIL("20031", "分片保存失败", "分片保存到临时目录失败"),
    CHUNK_MERGE_FAIL("20032", "分片合并失败", "分片合并或上传MinIO失败"),
    UPLOAD_NOT_FOUND("20033", "上传任务不存在", "uploadId 不存在或已过期");

    private final String code;
    private final String alias;
    private final String message;
}
