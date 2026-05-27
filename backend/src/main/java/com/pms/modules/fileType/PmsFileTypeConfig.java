package com.pms.modules.fileType;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("pms_file_type_config")
public class PmsFileTypeConfig {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectTypeId;
    private String name;
    private String code;
    private Integer skipRows;
    private String sheetName;
    private Boolean hasFields;
    private String uploadSchema;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
