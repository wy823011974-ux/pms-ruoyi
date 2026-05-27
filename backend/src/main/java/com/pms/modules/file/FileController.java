package com.pms.modules.file;

/**
 * @author ROY
 * @date 2026/05/27
 */

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.Result;
import com.pms.modules.fileType.PmsFileTypeConfig;
import com.pms.modules.fileType.PmsFileTypeConfigMapper;
import com.pms.modules.project.PmsProject;
import com.pms.modules.project.PmsProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final PmsProjectMapper projectMapper;
    private final PmsFileTypeConfigMapper ftcMapper;
    private final PmsFileRecordMapper fileMapper;

    @Value("${pms.upload-path:./uploads}")
    private String uploadPath;

    @PostMapping("/projects/{projectId}/upload")
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

            // Generate filename
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            PmsFileRecord existing = fileMapper.selectOne(new LambdaQueryWrapper<PmsFileRecord>()
                    .eq(PmsFileRecord::getProjectId, projectId)
                    .eq(PmsFileRecord::getFileTypeConfigId, ftcId)
                    .orderByDesc(PmsFileRecord::getVersion).last("LIMIT 1"));

            int newVersion = (existing != null && !overwrite) ? existing.getVersion() + 1 :
                            (existing != null && overwrite) ? existing.getVersion() : 1;

            String ext = "." + Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf(".") + 1);
            String newFilename = proj.getCode() + "-" + ftc.getCode() + "-" + date + "-v" + newVersion + ext;

            // Save file
            File dir = new File(uploadPath + "/" + proj.getCode() + "/");
            if (!dir.exists()) dir.mkdirs();
            java.nio.file.Files.write(new File(dir, newFilename).toPath(), file.getBytes());

            // If overwrite, delete old physical file
            if (overwrite && existing != null && existing.getStoragePath() != null) {
                new File(existing.getStoragePath()).delete();
            }

            PmsFileRecord record;
            if (overwrite && existing != null) {
                record = existing;
                record.setFilename(newFilename); record.setOriginalName(file.getOriginalFilename());
                record.setFileSize(file.getSize()); record.setSkipRows(skipRows); record.setIsOverwrite(1);
                record.setStoragePath(new File(dir, newFilename).getAbsolutePath());
                fileMapper.updateById(record);
            } else {
                record = new PmsFileRecord();
                record.setProjectId(projectId); record.setFileTypeConfigId(ftcId);
                record.setFilename(newFilename); record.setOriginalName(file.getOriginalFilename());
                record.setFileType(ftc.getCode()); record.setVersion(newVersion);
                record.setFileSize(file.getSize()); record.setSkipRows(skipRows); record.setIsOverwrite(0);
                record.setStoragePath(new File(dir, newFilename).getAbsolutePath());
                fileMapper.insert(record);
            }

            Map<String,Object> r = new HashMap<>(); r.put("message", "上传成功"); r.put("id", record.getId()); return Result.ok(r);
        } catch (Exception e) {
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/projects/{projectId}/files")
    public Result<List<Map<String,Object>>> listFiles(@PathVariable Long projectId) {
        List<PmsFileRecord> files = fileMapper.selectList(new LambdaQueryWrapper<PmsFileRecord>().eq(PmsFileRecord::getProjectId, projectId));
        // Group by file_type_config_id, get latest
        Map<Long, PmsFileRecord> latest = new LinkedHashMap<>();
        for (PmsFileRecord f : files) {
            PmsFileRecord cur = latest.get(f.getFileTypeConfigId());
            if (cur == null || f.getId() > cur.getId()) latest.put(f.getFileTypeConfigId(), f);
        }
        return Result.ok(latest.values().stream().map(this::fVO).collect(Collectors.toList()));
    }

    @GetMapping("/files/{id}/versions")
    public Result<List<Map<String,Object>>> versions(@PathVariable Long id) {
        PmsFileRecord f = fileMapper.selectById(id);
        if (f == null) return Result.fail("文件不存在");
        List<PmsFileRecord> vers = fileMapper.selectList(new LambdaQueryWrapper<PmsFileRecord>()
                .eq(PmsFileRecord::getProjectId, f.getProjectId())
                .eq(PmsFileRecord::getFileTypeConfigId, f.getFileTypeConfigId())
                .orderByDesc(PmsFileRecord::getVersion));
        return Result.ok(vers.stream().map(this::vVO).collect(Collectors.toList()));
    }

    @DeleteMapping("/files/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        PmsFileRecord f = fileMapper.selectById(id);
        if (f == null) return Result.fail("文件不存在");
        // 删除所有版本 + 物理文件
        List<PmsFileRecord> all = fileMapper.selectList(new LambdaQueryWrapper<PmsFileRecord>()
                .eq(PmsFileRecord::getProjectId, f.getProjectId())
                .eq(PmsFileRecord::getFileTypeConfigId, f.getFileTypeConfigId()));
        for (PmsFileRecord rec : all) {
            if (rec.getStoragePath() != null) new File(rec.getStoragePath()).delete();
            fileMapper.deleteById(rec.getId());
        }
        return Result.ok();
    }

    /**
     * 下载文件 — 根据文件记录ID返回文件流
     */
    @GetMapping("/files/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
        PmsFileRecord f = fileMapper.selectById(id);
        if (f == null || f.getStoragePath() == null) return ResponseEntity.notFound().build();

        java.io.File file = new java.io.File(f.getStoragePath());
        if (!file.exists()) return ResponseEntity.notFound().build();

        org.springframework.core.io.FileSystemResource resource = new org.springframework.core.io.FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + java.net.URLEncoder.encode(f.getOriginalName(), java.nio.charset.StandardCharsets.UTF_8) + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private Map<String,Object> fVO(PmsFileRecord f) {
        Map<String,Object> m = new HashMap<>(); m.put("id",f.getId()); m.put("filename",f.getFilename());
        m.put("originalName",f.getOriginalName()); m.put("fileType",f.getFileType());
        m.put("version",f.getVersion()); m.put("fileSize",f.getFileSize()); m.put("skipRows",f.getSkipRows());
        m.put("isOverwrite",f.getIsOverwrite()); m.put("createdAt",f.getCreateTime());
        return m;
    }
    private Map<String,Object> vVO(PmsFileRecord f) {
        Map<String,Object> m = new HashMap<>(); m.put("id",f.getId()); m.put("version",f.getVersion());
        m.put("fileSize",f.getFileSize()); m.put("isOverwrite",f.getIsOverwrite());
        m.put("skipRows",f.getSkipRows()); m.put("createdAt",f.getCreateTime()); return m;
    }
}
