package com.pms.services;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import java.io.ByteArrayInputStream;
import java.util.*;

public class ExcelValidator {

    /**
     * Parse Excel file content into list of row maps.
     * @param content File bytes
     * @param sheetName Sheet name (null = first sheet)
     * @param skipRows Rows to skip before header row
     * @return List of rows, each as Map<column_name, cell_value>
     */
    public static List<Map<String, String>> parseExcel(byte[] content, String sheetName, int skipRows) {
        List<Map<String, String>> rows = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        EasyExcel.read(new ByteArrayInputStream(content), new ReadListener<Map<Integer, String>>() {
            int rowIdx = 0;

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                if (rowIdx < skipRows) { rowIdx++; return; }
                if (rowIdx == skipRows) {
                    // Header row
                    for (Map.Entry<Integer, String> e : data.entrySet()) {
                        String val = e.getValue(); headers.add(StrUtil.isNotBlank(val) ? val.trim() : "col" + e.getKey());
                    }
                } else {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        String val = data.getOrDefault(i, "");
                        row.put(headers.get(i), val != null ? val.trim() : "");
                    }
                    // Skip completely empty rows
                    if (row.values().stream().anyMatch(StrUtil::isNotBlank)) rows.add(row);
                }
                rowIdx++;
            }

            @Override public void doAfterAllAnalysed(AnalysisContext context) {}
            @Override public void onException(Exception e, AnalysisContext context) { throw new RuntimeException(e); }
        }).sheet(sheetName)
          .headRowNumber(0)  // 禁用EasyExcel自动表头处理，由我们手动管理表头/数据行
          .doRead();

        return rows;
    }
}
