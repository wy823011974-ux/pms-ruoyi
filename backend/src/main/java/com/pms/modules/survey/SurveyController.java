package com.pms.modules.survey;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 调查数据控制器 — Excel上传、数据管理、导出、上传历史
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;
    private final PmsSurveyDataMapper surveyDataMapper;
    private final PmsUploadHistoryMapper historyMapper;

    /**
     * 上传调查数据 — Excel校验 → 脱敏 → 入库
     */
    @PostMapping("/projects/{projectId}/upload-data")
    public Result<Map<String, Object>> upload(@PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("file_type_config_id") Long ftcId,
            @RequestParam(defaultValue = "0") int skipRows,
            @RequestParam(defaultValue = "true") boolean overwrite) {
        try {
            Map<String, Object> result = surveyService.uploadData(projectId, ftcId, skipRows, overwrite, file);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询调查数据
     */
    @GetMapping("/projects/{projectId}/survey-data")
    public Result<Map<String, Object>> list(@PathVariable Long projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) Long fileTypeConfigId) {
        Map<String, Object> data = surveyService.listData(projectId, page, pageSize, fileTypeConfigId);
        return Result.ok(data);
    }

    /**
     * 删除单条调查数据
     */
    @DeleteMapping("/projects/{projectId}/survey-data/{dataId}")
    public Result<Void> deleteOne(@PathVariable Long dataId) {
        surveyDataMapper.deleteById(dataId);
        return Result.ok();
    }

    /**
     * 清空指定文件类型的所有调查数据
     */
    @DeleteMapping("/projects/{projectId}/survey-data/clear")
    public Result<Void> clear(@PathVariable Long projectId, @RequestParam Long fileTypeConfigId) {
        int n = surveyDataMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PmsSurveyData>()
                .eq(PmsSurveyData::getProjectId, projectId)
                .eq(PmsSurveyData::getFileTypeConfigId, fileTypeConfigId));
        return Result.ok();
    }

    /**
     * 导出调查数据为 Excel 文件下载
     */
    @GetMapping("/projects/{projectId}/survey-data/export")
    public ResponseEntity<byte[]> export(@PathVariable Long projectId,
            @RequestParam(required = false) Long fileTypeConfigId) {
        byte[] data = surveyService.exportData(projectId, fileTypeConfigId);
        String filename = "survey_data_" + projectId + "_" + java.time.LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /**
     * 查询上传历史
     */
    @GetMapping("/projects/{projectId}/upload-history")
    public Result<List<Map<String, Object>>> history(@PathVariable Long projectId,
            @RequestParam(required = false) Long fileTypeConfigId) {
        return Result.ok(surveyService.uploadHistory(projectId, fileTypeConfigId));
    }
}
