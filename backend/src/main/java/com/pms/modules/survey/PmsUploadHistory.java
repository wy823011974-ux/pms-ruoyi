package com.pms.modules.survey;

/**
 * @author ROY
 * @date 2026/05/27
 */

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("pms_upload_history")
public class PmsUploadHistory {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long fileTypeConfigId;
    private Long uploadBy;
    private String originalFilename;
    private Integer totalRows;
    private Integer successRows;
    private String status;
    private String errorDetail;
    private LocalDateTime createTime;
}