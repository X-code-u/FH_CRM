package com.fangu.middle.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fangu.middle.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    public String login(String username, String password) {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (user == null || !"ENABLED".equals(user.getStatus())
                || !BCrypt.checkpw(password, user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", user.getUsername());
        return StpUtil.getTokenValue();
    }

    /** 解析当前账号上下文（数据权限拦截器与业务层共用） */
    public CurrentAccount current() {
        long userId = StpUtil.getLoginIdAsLong();
        CurrentAccount cached = (CurrentAccount) StpUtil.getSession().get("account");
        if (cached != null) return cached;

        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BizException(401, "账号不存在");
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role == null) throw new BizException(401, "角色未配置");

        CurrentAccount acc = new CurrentAccount();
        acc.setUserId(user.getId());
        acc.setUsername(user.getUsername());
        acc.setRoleCode(role.getRoleCode());
        acc.setDataScope(role.getDataScope());
        acc.setFranchiseeId(user.getFranchiseeId());
        acc.setStoreId(user.getStoreId());
        StpUtil.getSession().set("account", acc);
        return acc;
    }
}
