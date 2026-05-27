package com.pms.modules.user;

/**
 * @author ROY
 * @date 2026/05/27
 */

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String role) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(role)) qw.eq(SysUser::getRole, role);
        qw.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, pageSize), qw);
        return Result.page(p.getTotal(), p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        String phone = body.get("phone"), email = body.get("email"), password = body.get("password");
        String displayName = body.get("displayName"), department = body.get("department");
        String role = body.getOrDefault("role", "member");

        if (!phone.matches("^1[3-9]\\d{9}$")) return Result.fail("请输入正确的11位手机号");
        if (StrUtil.isBlank(displayName)) return Result.fail("姓名不能为空");
        if (StrUtil.isBlank(department)) return Result.fail("部门/单位不能为空");

        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)) > 0)
            return Result.fail("该手机号已注册");

        SysUser u = new SysUser();
        u.setUsername(phone); u.setPhone(phone); u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setDisplayName(displayName); u.setDepartment(department);
        u.setRole(role); u.setStatus(1); // 管理员创建直接启用
        userMapper.insert(u);
        return Result.ok(toVO(u));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        SysUser u = userMapper.selectById(id);
        if (u == null) return Result.fail("用户不存在");

        // 超级管理员不可编辑
        if ("super_admin".equals(u.getRole())) return Result.fail("超级管理员不可修改");

        if (body.containsKey("displayName")) u.setDisplayName(body.get("displayName"));
        if (body.containsKey("phone")) u.setPhone(body.get("phone"));
        if (body.containsKey("department")) u.setDepartment(body.get("department"));
        if (body.containsKey("role")) u.setRole(body.get("role"));
        if (body.containsKey("status") && !"super_admin".equals(u.getRole()))
            u.setStatus(Integer.parseInt(body.get("status")));
        userMapper.updateById(u);
        return Result.ok(toVO(u));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) return Result.fail("用户不存在");
        if ("super_admin".equals(u.getRole())) return Result.fail("不能删除超级管理员");
        userMapper.deleteById(id);
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        SysUser u = userMapper.selectById(id);
        if (u == null) return Result.fail("用户不存在");
        if ("super_admin".equals(u.getRole())) return Result.fail("不能重置超级管理员密码");
        u.setPassword(passwordEncoder.encode(body.get("newPassword")));
        userMapper.updateById(u);
        return Result.ok();
    }

    private Map<String, Object> toVO(SysUser u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId()); m.put("username", u.getUsername()); m.put("email", u.getEmail());
        m.put("displayName", u.getDisplayName()); m.put("phone", u.getPhone());
        m.put("department", u.getDepartment()); m.put("role", u.getRole());
        m.put("status", u.getStatus()); m.put("loginFailCount", u.getLoginFailCount());
        m.put("createTime", u.getCreateTime());
        return m;
    }
}
