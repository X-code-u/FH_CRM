package com.fangu.middle.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fangu.middle.common.BizException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

/**
 * 门店登录账号维护：总部在门店档案中为门店设置店长账号与密码。
 * 账号固定绑定 STORE_OP 角色，数据范围由门店归属决定。
 */
@Service
@RequiredArgsConstructor
public class StoreAccountService {

    private static final Pattern USERNAME_REGEX = Pattern.compile("^[A-Za-z0-9_]{3,32}$");

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    public AccountVO findByStoreId(Long storeId) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getStoreId, storeId)
                .eq(SysUser::getRoleId, storeOpRole().getId())
                .last("LIMIT 1"));
        if (user == null) {
            return null;
        }
        AccountVO vo = new AccountVO();
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setStatus(user.getStatus());
        vo.setStoreId(user.getStoreId());
        return vo;
    }

    /**
     * 新建或更新门店账号。
     *
     * @param password 为空表示保持原密码（仅更新已有账号时允许）
     */
    public void save(Long storeId, Long franchiseeId, String username, String password, String realName) {
        if (username == null || !USERNAME_REGEX.matcher(username).matches()) {
            throw new BizException(40001, "账号需为 3-32 位字母、数字或下划线");
        }
        if (password != null && !password.isEmpty() && password.length() < 6) {
            throw new BizException(40001, "密码不少于 6 位");
        }
        SysRole role = storeOpRole();
        SysUser existing = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getStoreId, storeId)
                .eq(SysUser::getRoleId, role.getId())
                .last("LIMIT 1"));
        if (existing == null) {
            if (password == null || password.isEmpty()) {
                throw new BizException(40001, "新建账号必须设置密码");
            }
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(10)));
            user.setRealName(realName == null || realName.isBlank() ? username : realName);
            user.setRoleId(role.getId());
            user.setFranchiseeId(franchiseeId);
            user.setStoreId(storeId);
            user.setStatus("ENABLED");
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            try {
                userMapper.insert(user);
            } catch (DuplicateKeyException e) {
                throw new BizException(40001, "登录账号已存在");
            }
            return;
        }
        Long sameName = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .ne(SysUser::getId, existing.getId()));
        if (sameName > 0) {
            throw new BizException(40001, "登录账号已存在");
        }
        existing.setUsername(username);
        existing.setRealName(realName == null || realName.isBlank() ? existing.getRealName() : realName);
        existing.setFranchiseeId(franchiseeId);
        existing.setStatus("ENABLED");
        existing.setUpdatedAt(OffsetDateTime.now());
        if (password != null && !password.isEmpty()) {
            existing.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(10)));
        }
        try {
            userMapper.updateById(existing);
        } catch (DuplicateKeyException e) {
            throw new BizException(40001, "登录账号已存在");
        }
    }

    private SysRole storeOpRole() {
        SysRole role = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, "STORE_OP"));
        if (role == null) {
            throw new BizException(500, "门店角色未配置");
        }
        return role;
    }

    @Data
    public static class AccountVO {
        private String username;
        private String realName;
        private String status;
        private Long storeId;
    }
}
