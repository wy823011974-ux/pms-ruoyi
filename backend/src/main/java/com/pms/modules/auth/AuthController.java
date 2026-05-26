package com.pms.modules.auth;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.Result;
import com.pms.framework.security.JwtUtils;
import com.pms.modules.user.SysUser;
import com.pms.modules.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String email = body.get("email");
        String password = body.get("password");
        String displayName = body.getOrDefault("displayName", "");
        String department = body.getOrDefault("department", "");

        if (StrUtil.isBlank(phone) || !phone.matches("^1[3-9]\\d{9}$")) return Result.fail("请输入正确的11位手机号");
        if (StrUtil.isBlank(displayName)) return Result.fail("姓名不能为空");
        if (StrUtil.isBlank(department)) return Result.fail("部门/单位不能为空");
        if (StrUtil.isBlank(email)) return Result.fail("邮箱不能为空");
        if (StrUtil.isBlank(password) || password.length() < 8) return Result.fail("密码至少8位");

        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)) > 0)
            return Result.fail("该手机号已注册");

        SysUser user = new SysUser();
        user.setUsername(phone);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setDisplayName(displayName);
        user.setDepartment(department);
        user.setRole("member");
        user.setStatus(0); // 注册后待审批
        userMapper.insert(user);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String account = body.get("account");
        String password = body.get("password");

        if (StrUtil.isBlank(account) || StrUtil.isBlank(password)) return Result.fail("账号和密码不能为空");

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, account).or().eq(SysUser::getUsername, account));

        if (user == null || !passwordEncoder.matches(password, user.getPassword()))
            return Result.fail("账号或密码错误");

        if (user.getStatus() == 0) return Result.fail("账号已被禁用或尚未通过审批");

        // 检查锁定
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            return Result.fail("账号已锁定，请稍后再试");
        }

        String token = jwtUtils.createToken(user.getUsername(), user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("access_token", token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("displayName", user.getDisplayName());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("role", user.getRole());
        userInfo.put("status", user.getStatus());
        result.put("user", userInfo);

        return Result.ok(result);
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Result.fail(401, "未登录");
        Long userId = (Long) auth.getPrincipal();
        SysUser user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("displayName", user.getDisplayName());
        info.put("email", user.getEmail());
        info.put("phone", user.getPhone());
        info.put("role", user.getRole());
        info.put("status", user.getStatus());
        info.put("department", user.getDepartment());
        return Result.ok(info);
    }
}
