package com.fangu.middle.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 401/403 映射为真实 HTTP 状态码，便于前端拦截器统一处理认证失效 */
    private static ResponseEntity<R<Void>> respond(int code, String msg) {
        R<Void> body = R.fail(code, msg);
        if (code == 401) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        if (code == 403) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> biz(BizException e) {
        return respond(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<R<Void>> valid(Exception e) {
        return respond(40001, "参数校验失败");
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<Void>> notLogin(Exception e) {
        return respond(401, "未登录或令牌失效");
    }

    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public ResponseEntity<R<Void>> forbidden(Exception e) {
        return respond(403, "无权限或越权");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> error(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(500, "服务端异常"));
    }
}
