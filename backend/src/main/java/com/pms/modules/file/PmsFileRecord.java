package com.pms.modules.file;

/**
 * @author ROY
 * @date 2026/05/27
 */

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("pms_file_record")
public class PmsFileRecord {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long fileTypeConfigId;
    private String filename;
    private String originalName;
    private String fileType;
    private Integer version;
    private Long fileSize;
    private Integer skipRows;
    private Integer isOverwrite;
    private String storagePath;
    private Long uploadBy;
    private LocalDateTime createTime;
}