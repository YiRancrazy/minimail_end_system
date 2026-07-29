package com.yirancrazy.minimall.common.exception;

import lombok.Getter;
import com.yirancrazy.minimall.common.result.ResultCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务异常，承载错误码、别名和提示消息，支持直接传码或由 ResultCode 枚举构造，抛出后由全局异常处理器转换为 Result.fail 响应。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Getter
public class BizException extends RuntimeException
{
    private final String code;
    private final String alias;

    public BizException(String code, String message)
    {
        super(message);
        this.code = code;
        this.alias = null;
    }

    public BizException(String code, String alias, String message)
    {
        super(message);
        this.code = code;
        this.alias = alias;
    }

    public BizException(ResultCode rc)
    {
        super(rc.getMessage());
        this.code = rc.getCode();
        this.alias = rc.getAlias();
    }
}