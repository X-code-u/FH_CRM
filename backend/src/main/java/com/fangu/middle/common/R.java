package com.fangu.middle.common;

import lombok.Data;

/** 统一响应结构 {code, msg, data, requestId} */
@Data
public class R<T> {
    private int code;
    private String msg;
    private T data;
    private String requestId;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.msg = "ok";
        r.data = data;
        r.requestId = RequestIdHolder.get();
        return r;
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        r.requestId = RequestIdHolder.get();
        return r;
    }
}
