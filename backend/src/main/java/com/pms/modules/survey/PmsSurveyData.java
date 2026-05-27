package com.pms.modules.survey;

/**
 * @author ROY
 * @date 2026/05/27
 */

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("pms_survey_data")
public class PmsSurveyData {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long fileTypeConfigId;
    private String rowData;
    private String projectCode;
    private String projectName;
    private Integer projectYear;
    private String projectLocation;
    private String projectTypeName;
    private String fileTypeName;
    private String originalFilename;
    private Long uploadBy;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
}
