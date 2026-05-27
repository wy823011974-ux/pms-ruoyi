package com.pms.modules.projectType;

/**
 * @author ROY
 * @date 2026/05/27
 */

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("pms_project_type")
public class PmsProjectType {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private String code;
    private String description;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
