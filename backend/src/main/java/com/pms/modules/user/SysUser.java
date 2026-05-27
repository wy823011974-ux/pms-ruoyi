package com.pms.modules.user;

/**
 * @author ROY
 * @date 2026/05/27
 */

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String password;
    private String displayName;
    private String phone;
    private String department;
    private String role;
    private Integer status;
    private Integer loginFailCount;
    private LocalDateTime lockUntil;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
