package com.pms.modules.survey;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.modules.fileType.PmsFieldDefinition;
import com.pms.modules.fileType.PmsFieldDefinitionMapper;
import com.pms.modules.fileType.PmsFileTypeConfig;
import com.pms.modules.fileType.PmsFileTypeConfigMapper;
import com.pms.modules.project.PmsProject;
import com.pms.modules.project.PmsProjectMapper;
import com.pms.modules.projectType.PmsProjectType;
import com.pms.modules.projectType.PmsProjectTypeMapper;
import com.pms.services.Anonymizer;
import com.pms.services.ExcelValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 调查数据服务 — Excel 上传、校验、脱敏、入库、导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final PmsSurveyDataMapper surveyDataMapper;
    private final PmsProjectMapper projectMapper;
    private final PmsFileTypeConfigMapper ftcMapper;
    private final PmsProjectTypeMapper ptMapper;
    private final PmsFieldDefinitionMapper fdMapper;
    private final PmsUploadHistoryMapper historyMapper;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB上限

    /**
     * 上传并处理调查数据 — 校验 → 脱敏 → 入库
     */
    @Transactional
    public Map<String, Object> uploadData(Long projectId, Long ftcId, int skipRows,
                                           boolean overwrite, MultipartFile file) {
        PmsProject proj = projectMapper.selectById(projectId);
        if (proj == null) throw new RuntimeException("项目不存在");

        PmsFileTypeConfig ftc = ftcMapper.selectById(ftcId);
        if (ftc == null) throw new RuntimeException("文件类型配置不存在");

        byte[] content = readFileBytes(file);
        if (content.length > MAX_FILE_SIZE) throw new RuntimeException("文件过大(最大50MB)");

        validateFileMagicBytes(content);

        // 解析Excel
        int actualSkip = skipRows > 0 ? skipRows : (ftc.getSkipRows() != null ? ftc.getSkipRows() : 0);
        List<Map<String, String>> rows = ExcelValidator.parseExcel(content, ftc.getSheetName(), actualSkip);
        if (rows.isEmpty()) throw new RuntimeException("文件为空或无法解析");

        // 获取字段定义，用于校验和脱敏
        List<PmsFieldDefinition> fields = fdMapper.selectList(
                new LambdaQueryWrapper<PmsFieldDefinition>()
                        .eq(PmsFieldDefinition::getFileTypeConfigId, ftcId)
                        .eq(PmsFieldDefinition::getIsActive, 1)
                        .orderByAsc(PmsFieldDefinition::getSortOrder));

        // 覆盖模式：先删除旧数据
        if (overwrite) {
            int deleted = surveyDataMapper.delete(new LambdaQueryWrapper<PmsSurveyData>()
                    .eq(PmsSurveyData::getProjectId, projectId)
                    .eq(PmsSurveyData::getFileTypeConfigId, ftcId));
            log.info("覆盖模式：删除了 {} 条旧数据", deleted);
        }

        PmsProjectType pt = ptMapper.selectById(proj.getProjectTypeId());
        int inserted = 0;

        for (Map<String, String> row : rows) {
            // 校验行数据
            validateRow(row, fields);
            // 脱敏处理
            Map<String, String> masked = Anonymizer.anonymize(row);
            // 入库
            PmsSurveyData sd = new PmsSurveyData();
            sd.setProjectId(projectId);
            sd.setFileTypeConfigId(ftcId);
            sd.setRowData(JSONUtil.toJsonStr(masked));
            sd.setProjectCode(proj.getCode());
            sd.setProjectName(proj.getName());
            sd.setProjectYear(proj.getYear());
            sd.setProjectLocation(proj.getLocation());
            sd.setProjectTypeName(pt != null ? pt.getName() : null);
            sd.setFileTypeName(ftc.getName());
            sd.setOriginalFilename(file.getOriginalFilename());
            surveyDataMapper.insert(sd);
            inserted++;
        }

        // 记录上传历史
        PmsUploadHistory h = new PmsUploadHistory();
        h.setProjectId(projectId);
        h.setFileTypeConfigId(ftcId);
        h.setOriginalFilename(file.getOriginalFilename());
        h.setTotalRows(inserted);
        h.setSuccessRows(inserted);
        h.setStatus("success");
        historyMapper.insert(h);

        log.info("数据上传完成：项目={}, 类型={}, 行数={}", proj.getCode(), ftc.getCode(), inserted);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "上传成功");
        result.put("totalRows", inserted);
        result.put("successRows", inserted);
        result.put("uploadHistoryId", h.getId());
        return result;
    }

    /**
     * 分页查询调查数据
     */
    public Map<String, Object> listData(Long projectId, int page, int pageSize, Long fileTypeConfigId) {
        LambdaQueryWrapper<PmsSurveyData> qw = new LambdaQueryWrapper<>();
        qw.eq(PmsSurveyData::getProjectId, projectId);
        if (fileTypeConfigId != null) qw.eq(PmsSurveyData::getFileTypeConfigId, fileTypeConfigId);
        qw.orderByAsc(PmsSurveyData::getId);
        Page<PmsSurveyData> p = surveyDataMapper.selectPage(new Page<>(page, pageSize), qw);
        return Map.of("total", p.getTotal(), "rows", p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    /**
     * 导出调查数据为 Excel 字节流
     */
    public byte[] exportData(Long projectId, Long fileTypeConfigId) {
        LambdaQueryWrapper<PmsSurveyData> qw = new LambdaQueryWrapper<>();
        qw.eq(PmsSurveyData::getProjectId, projectId);
        if (fileTypeConfigId != null) qw.eq(PmsSurveyData::getFileTypeConfigId, fileTypeConfigId);
        qw.orderByAsc(PmsSurveyData::getId);
        List<PmsSurveyData> list = surveyDataMapper.selectList(qw);

        if (list.isEmpty()) throw new RuntimeException("没有数据可导出");

        // 从第一条数据推断表头
        Map<String, Object> firstRow = JSONUtil.parseObj(list.get(0).getRowData());
        List<String> headers = new ArrayList<>(firstRow.keySet());

        // 构建导出数据
        List<List<String>> exportRows = new ArrayList<>();
        for (PmsSurveyData sd : list) {
            Map<String, Object> rowMap = JSONUtil.parseObj(sd.getRowData());
            List<String> row = new ArrayList<>();
            for (String header : headers) {
                Object val = rowMap.getOrDefault(header, "");
                row.add(val != null ? val.toString() : "");
            }
            exportRows.add(row);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        EasyExcel.write(bos).sheet("调查数据").head(buildHead(headers)).doWrite(exportRows);
        return bos.toByteArray();
    }

    /**
     * 查询上传历史
     */
    public List<Map<String, Object>> uploadHistory(Long projectId, Long fileTypeConfigId) {
        LambdaQueryWrapper<PmsUploadHistory> qw = new LambdaQueryWrapper<>();
        qw.eq(PmsUploadHistory::getProjectId, projectId);
        if (fileTypeConfigId != null) qw.eq(PmsUploadHistory::getFileTypeConfigId, fileTypeConfigId);
        qw.orderByDesc(PmsUploadHistory::getCreateTime);
        return historyMapper.selectList(qw).stream().map(h -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", h.getId());
            m.put("originalFilename", h.getOriginalFilename());
            m.put("totalRows", h.getTotalRows());
            m.put("successRows", h.getSuccessRows());
            m.put("status", h.getStatus());
            m.put("createdAt", h.getCreateTime());
            return m;
        }).collect(Collectors.toList());
    }

    // === 私有辅助方法 ===

    private byte[] readFileBytes(MultipartFile file) {
        try { return file.getBytes(); }
        catch (Exception e) { throw new RuntimeException("读取文件失败: " + e.getMessage()); }
    }

    /**
     * 文件魔数校验 — 防止伪造扩展名
     */
    private void validateFileMagicBytes(byte[] content) {
        if (content.length < 4) return;
        byte[] zipSig = {0x50, 0x4B, 0x03, 0x04};       // .xlsx (ZIP)
        byte[] oleSig = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}; // .xls (OLE2)
        boolean isZip = true, isOle = true;
        for (int i = 0; i < 4; i++) {
            if (content[i] != zipSig[i]) isZip = false;
            if (content[i] != oleSig[i]) isOle = false;
        }
        if (!isZip && !isOle) throw new RuntimeException("文件格式不匹配：扩展名与文件内容不符");
    }

    /**
     * 校验单行数据 — 根据字段定义的 type 和 required 规则
     */
    private void validateRow(Map<String, String> row, List<PmsFieldDefinition> fields) {
        for (PmsFieldDefinition fd : fields) {
            String value = row.get(fd.getFieldLabel());
            if (value == null) value = row.get(fd.getFieldKey());

            // 必填校验
            if (fd.getIsRequired() != null && fd.getIsRequired() == 1
                    && (value == null || value.trim().isEmpty())) {
                throw new RuntimeException("字段 '" + fd.getFieldLabel() + "' 是必填的");
            }

            if (value == null || value.trim().isEmpty()) continue;

            // 类型校验
            switch (fd.getFieldType()) {
                case "number":
                    try { Double.parseDouble(value.trim()); }
                    catch (NumberFormatException e) {
                        throw new RuntimeException("字段 '" + fd.getFieldLabel() + "' 应为数字，实际值: " + value);
                    }
                    break;
                case "select":
                    if (fd.getOptions() != null && !fd.getOptions().toString().contains(value.trim())) {
                        // select类型仅警告，不阻断
                        log.warn("字段 '{}' 的值 '{}' 不在预设选项中", fd.getFieldLabel(), value);
                    }
                    break;
            }
        }
    }

    private List<List<String>> buildHead(List<String> headers) {
        List<List<String>> head = new ArrayList<>();
        for (String h : headers) head.add(List.of(h));
        return head;
    }

    private Map<String, Object> toVO(PmsSurveyData sd) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", sd.getId());
        m.put("projectId", sd.getProjectId());
        m.put("rowData", JSONUtil.parse(sd.getRowData()));
        m.put("projectCode", sd.getProjectCode());
        m.put("projectName", sd.getProjectName());
        m.put("projectYear", sd.getProjectYear());
        m.put("projectLocation", sd.getProjectLocation());
        m.put("projectTypeName", sd.getProjectTypeName());
        m.put("fileTypeName", sd.getFileTypeName());
        m.put("uploadedBy", sd.getUploadBy());
        m.put("createdAt", sd.getCreateTime());
        return m;
    }
}
