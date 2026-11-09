package com.fangu.middle.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.fangu.middle.common.R;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Data
    public static class LoginReq {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody @Validated LoginReq req) {
        String token = authService.login(req.getUsername(), req.getPassword());
        return R.ok(Map.of("token", token));
    }

    @GetMapping("/current")
    public R<CurrentAccount> current() {
        return R.ok(authService.current());
    }

    /** vben 权限码：角色码 + 数据范围 */
    @GetMapping("/codes")
    public R<java.util.List<String>> codes() {
        CurrentAccount acc = authService.current();
        return R.ok(java.util.List.of(acc.getRoleCode(), "SCOPE_" + acc.getDataScope()));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok(null);
    }
}
