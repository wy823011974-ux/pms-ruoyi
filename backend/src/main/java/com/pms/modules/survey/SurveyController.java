package com.pms.modules.survey;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.Result;
import com.pms.modules.fileType.PmsFileTypeConfig;
import com.pms.modules.fileType.PmsFileTypeConfigMapper;
import com.pms.modules.project.PmsProject;
import com.pms.modules.project.PmsProjectMapper;
import com.pms.modules.projectType.PmsProjectType;
import com.pms.modules.projectType.PmsProjectTypeMapper;
import com.pms.services.ExcelValidator;
import com.pms.services.Anonymizer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SurveyController {

    private final PmsSurveyDataMapper surveyDataMapper;
    private final PmsProjectMapper projectMapper;
    private final PmsFileTypeConfigMapper ftcMapper;
    private final PmsProjectTypeMapper ptMapper;
    private final PmsUploadHistoryMapper historyMapper;

    @PostMapping("/projects/{projectId}/upload-data")
    public Result<Map<String,Object>> upload(@PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("file_type_config_id") Long ftcId,
            @RequestParam(defaultValue = "0") int skipRows,
            @RequestParam(defaultValue = "true") boolean overwrite) {
        try {
            PmsProject proj = projectMapper.selectById(projectId);
            if (proj == null) return Result.fail("项目不存在");

            PmsFileTypeConfig ftc = ftcMapper.selectById(ftcId);
            if (ftc == null) return Result.fail("文件类型配置不存在");

            // Read file content
            byte[] content = file.getBytes();
            if (content.length > 52428800) return Result.fail("文件过大(最大50MB)");

            // Validate magic bytes
            if (content.length > 4) {
                byte[] zipSig = {0x50, 0x4B, 0x03, 0x04};
                byte[] oleSig = {(byte)0xD0, (byte)0xCF, 0x11, (byte)0xE0};
                boolean isZip = true, isOle = true;
                for (int i = 0; i < 4; i++) { if (content[i] != zipSig[i]) isZip = false; if (content[i] != oleSig[i]) isOle = false; }
                if (!isZip && !isOle) return Result.fail("文件格式不匹配：扩展名与文件内容不符");
            }

            // Parse Excel header
            int actualSkip = skipRows > 0 ? skipRows : (ftc.getSkipRows() != null ? ftc.getSkipRows() : 0);
            List<List<String>> data = ExcelValidator.parseExcel(content, ftc.getSheetName(), actualSkip);
            if (data.isEmpty()) return Result.fail("文件为空或无法解析");

            // Overwrite: delete old data
            if (overwrite) {
                surveyDataMapper.delete(new LambdaQueryWrapper<PmsSurveyData>()
                        .eq(PmsSurveyData::getProjectId, projectId)
                        .eq(PmsSurveyData::getFileTypeConfigId, ftcId));
            }

            // Anonymize and insert
            PmsProjectType pt = ptMapper.selectById(proj.getProjectTypeId());
            int inserted = 0;
            for (Map<String, String> row : data) {
                Map<String, String> masked = Anonymizer.anonymize(row);
                PmsSurveyData sd = new PmsSurveyData();
                sd.setProjectId(projectId); sd.setFileTypeConfigId(ftcId);
                sd.setRowData(JSONUtil.toJsonStr(masked));
                sd.setProjectCode(proj.getCode()); sd.setProjectName(proj.getName());
                sd.setProjectYear(proj.getYear()); sd.setProjectLocation(proj.getLocation());
                sd.setProjectTypeName(pt != null ? pt.getName() : null);
                sd.setFileTypeName(ftc.getName());
                sd.setOriginalFilename(file.getOriginalFilename());
                surveyDataMapper.insert(sd);
                inserted++;
            }

            // Record history
            PmsUploadHistory h = new PmsUploadHistory();
            h.setProjectId(projectId); h.setFileTypeConfigId(ftcId);
            h.setOriginalFilename(file.getOriginalFilename());
            h.setTotalRows(inserted); h.setSuccessRows(inserted); h.setStatus("success");
            historyMapper.insert(h);

            Map<String,Object> result = new HashMap<>();
            result.put("message", "上传成功"); result.put("totalRows", inserted);
            result.put("successRows", inserted); result.put("uploadHistoryId", h.getId());
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/projects/{projectId}/survey-data")
    public Result<Map<String,Object>> list(@PathVariable Long projectId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) Long fileTypeConfigId) {
        LambdaQueryWrapper<PmsSurveyData> qw = new LambdaQueryWrapper<>();
        qw.eq(PmsSurveyData::getProjectId, projectId);
        if (fileTypeConfigId != null) qw.eq(PmsSurveyData::getFileTypeConfigId, fileTypeConfigId);
        qw.orderByAsc(PmsSurveyData::getId);
        Page<PmsSurveyData> p = surveyDataMapper.selectPage(new Page<>(page, pageSize), qw);
        return Result.page(p.getTotal(), p.getRecords().stream().map(this::sdVO).collect(Collectors.toList()));
    }

    @DeleteMapping("/projects/{projectId}/survey-data/{dataId}")
    public Result<Void> deleteOne(@PathVariable Long dataId) { surveyDataMapper.deleteById(dataId); return Result.ok(); }

    @DeleteMapping("/projects/{projectId}/survey-data/clear")
    public Result<Void> clear(@PathVariable Long projectId, @RequestParam Long fileTypeConfigId) {
        int n = surveyDataMapper.delete(new LambdaQueryWrapper<PmsSurveyData>()
                .eq(PmsSurveyData::getProjectId, projectId)
                .eq(PmsSurveyData::getFileTypeConfigId, fileTypeConfigId));
        return Result.ok();
    }

    @GetMapping("/projects/{projectId}/upload-history")
    public Result<List<Map<String,Object>>> history(@PathVariable Long projectId,
            @RequestParam(required = false) Long fileTypeConfigId) {
        LambdaQueryWrapper<PmsUploadHistory> qw = new LambdaQueryWrapper<>();
        qw.eq(PmsUploadHistory::getProjectId, projectId);
        if (fileTypeConfigId != null) qw.eq(PmsUploadHistory::getFileTypeConfigId, fileTypeConfigId);
        qw.orderByDesc(PmsUploadHistory::getCreateTime);
        return Result.ok(historyMapper.selectList(qw).stream().map(h -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id",h.getId()); m.put("originalFilename",h.getOriginalFilename());
            m.put("totalRows",h.getTotalRows()); m.put("successRows",h.getSuccessRows());
            m.put("status",h.getStatus()); m.put("createdAt",h.getCreateTime()); return m;
        }).collect(Collectors.toList()));
    }

    // Export
    @GetMapping("/projects/{projectId}/survey-data/export")
    public Result<Void> export(@PathVariable Long projectId) { return Result.ok(); } // TODO: file download

    private Map<String,Object> sdVO(PmsSurveyData sd) {
        Map<String,Object> m = new HashMap<>();
        m.put("id",sd.getId()); m.put("projectId",sd.getProjectId());
        m.put("rowData", JSONUtil.parse(sd.getRowData()));
        m.put("projectCode",sd.getProjectCode()); m.put("projectName",sd.getProjectName());
        m.put("projectYear",sd.getProjectYear()); m.put("projectLocation",sd.getProjectLocation());
        m.put("projectTypeName",sd.getProjectTypeName()); m.put("fileTypeName",sd.getFileTypeName());
        m.put("uploadedBy",sd.getUploadBy());
        m.put("createdAt",sd.getCreateTime()); return m;
    }
}
