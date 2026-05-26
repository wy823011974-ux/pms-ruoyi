package com.pms.modules.project;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pms_project")
public class PmsProject {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Integer year;
    private String location;
    private Long projectTypeId;
    private String status;
    private Long leaderId;
    private Long creatorId;
    private String dynamicFields;
    private LocalDate startDate;
    private LocalDate endDate;
    @TableLogic private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
