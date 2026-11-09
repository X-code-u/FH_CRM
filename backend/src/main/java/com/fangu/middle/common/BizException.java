package com.fangu.middle.common;

import lombok.Getter;

/** 业务异常，code 对应错误码表（40001 参数 / 40002 门店不可用 / 40003 价格未维护 / 40004 状态不允许 / 40005 并发冲突） */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
