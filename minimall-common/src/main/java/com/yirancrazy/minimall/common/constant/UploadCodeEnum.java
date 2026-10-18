package com.yirancrazy.minimall.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UploadCodeEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Getter
@AllArgsConstructor
public enum UploadCodeEnum implements BaseEnum {

    CHUNK_PARAM_INVALID("20030", "分片参数非法", "分片序号或总分片数非法"),
    CHUNK_UPLOAD_FAIL("20031", "分片保存失败", "分片保存到临时目录失败"),
    CHUNK_MERGE_FAIL("20032", "分片合并失败", "分片合并或上传MinIO失败"),
    UPLOAD_NOT_FOUND("20033", "上传任务不存在", "uploadId 不存在或已过期"),
    UPLOAD_FORBIDDEN("20034", "无权访问上传任务", "uploadId 不属于当前用户"),
    CHUNK_COUNT_LIMIT_EXCEEDED("20035", "分片数量超限", "分片总数超过系统限制"),
    CHUNK_SIZE_LIMIT_EXCEEDED("20036", "分片大小超限", "单个分片超过系统限制"),
    TASK_SIZE_LIMIT_EXCEEDED("20037", "上传任务大小超限", "单个上传任务总大小超过系统限制"),
    USER_QUOTA_EXCEEDED("20038", "上传配额超限", "用户上传临时空间超过系统限制");

    private final String code;
    private final String alias;
    private final String message;
}
