package com.pms.modules.fileType;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 字段定义 Excel 导入导出行
 */
@Data
public class FieldExcelRow {
    @ExcelProperty("字段标识")
    private String fieldKey;

    @ExcelProperty("字段名称")
    private String fieldLabel;

    @ExcelProperty("字段类型(text/number/date/select)")
    private String fieldType;

    @ExcelProperty("是否必填(是/否)")
    private String isRequired;

    @ExcelProperty("是否启用(是/否)")
    private String isActive;

    @ExcelProperty("选项(select类型，逗号分隔)")
    private String options;

    @ExcelProperty("排序")
    private Integer sortOrder;
}
