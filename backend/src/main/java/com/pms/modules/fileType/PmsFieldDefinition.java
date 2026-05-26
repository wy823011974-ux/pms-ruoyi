package com.pms.modules.fileType;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("pms_field_definition")
public class PmsFieldDefinition {
    @TableId(type = IdType.AUTO) private Long id;
    private Long fileTypeConfigId;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private String options;
    private Integer isRequired;
    private Integer sortOrder;
    private Integer isActive;
    private String extraAttrs;
    private LocalDateTime createTime;
}