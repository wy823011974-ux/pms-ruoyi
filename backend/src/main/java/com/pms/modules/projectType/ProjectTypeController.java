package com.pms.modules.projectType;

/**
 * @author ROY
 * @date 2026/05/27
 */

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.Result;
import com.pms.modules.fileType.PmsFileTypeConfig;
import com.pms.modules.fileType.PmsFileTypeConfigMapper;
import com.pms.modules.fileType.PmsFieldDefinition;
import com.pms.modules.fileType.PmsFieldDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/project-types")
@RequiredArgsConstructor
public class ProjectTypeController {

    private final PmsProjectTypeMapper ptMapper;
    private final PmsFileTypeConfigMapper ftcMapper;
    private final PmsFieldDefinitionMapper fdMapper;

    // ========== 项目类型 CRUD ==========
    @GetMapping
    public Result<List<Map<String,Object>>> list() {
        List<PmsProjectType> pts = ptMapper.selectList(new LambdaQueryWrapper<PmsProjectType>().orderByDesc(PmsProjectType::getCreateTime));
        return Result.ok(pts.stream().map(this::ptVO).collect(Collectors.toList()));
    }

    @PostMapping
    public Result<Map<String,Object>> create(@RequestBody PmsProjectType pt) {
        if (ptMapper.selectCount(new LambdaQueryWrapper<PmsProjectType>().eq(PmsProjectType::getCode, pt.getCode())) > 0)
            return Result.fail("编码已存在");
        ptMapper.insert(pt);
        return Result.ok(ptVO(pt));
    }

    @PutMapping("/{id}")
    public Result<Map<String,Object>> update(@PathVariable Long id, @RequestBody PmsProjectType pt) {
        pt.setId(id); ptMapper.updateById(pt);
        return Result.ok(ptVO(ptMapper.selectById(id)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        // cascade: delete FTCs -> fields
        List<PmsFileTypeConfig> ftcs = ftcMapper.selectList(new LambdaQueryWrapper<PmsFileTypeConfig>().eq(PmsFileTypeConfig::getProjectTypeId, id));
        for (PmsFileTypeConfig ftc : ftcs) {
            fdMapper.delete(new LambdaQueryWrapper<PmsFieldDefinition>().eq(PmsFieldDefinition::getFileTypeConfigId, ftc.getId()));
            ftcMapper.deleteById(ftc.getId());
        }
        ptMapper.deleteById(id);
        return Result.ok();
    }

    // ========== 文件类型配置 CRUD ==========
    @GetMapping("/{typeId}/file-types")
    public Result<List<Map<String,Object>>> listFileTypes(@PathVariable Long typeId) {
        List<PmsFileTypeConfig> ftcs = ftcMapper.selectList(
                new LambdaQueryWrapper<PmsFileTypeConfig>().eq(PmsFileTypeConfig::getProjectTypeId, typeId).orderByAsc(PmsFileTypeConfig::getSortOrder));
        return Result.ok(ftcs.stream().map(this::ftcVO).collect(Collectors.toList()));
    }

    @PostMapping("/{typeId}/file-types")
    public Result<Map<String,Object>> createFileType(@PathVariable Long typeId, @RequestBody PmsFileTypeConfig ftc) {
        ftc.setProjectTypeId(typeId);
        if (StrUtil.isBlank(ftc.getCode())) ftc.setCode(cn.hutool.extra.pinyin.PinyinUtil.getPinyin(ftc.getName(), "").toLowerCase().replace(" ", "_"));
        ftcMapper.insert(ftc);
        return Result.ok(ftcVO(ftc));
    }

    @PutMapping("/{typeId}/file-types/{cfgId}")
    public Result<Map<String,Object>> updateFileType(@PathVariable Long cfgId, @RequestBody PmsFileTypeConfig ftc) {
        ftc.setId(cfgId); ftcMapper.updateById(ftc);
        return Result.ok(ftcVO(ftcMapper.selectById(cfgId)));
    }

    @DeleteMapping("/{typeId}/file-types/{cfgId}")
    public Result<Void> deleteFileType(@PathVariable Long cfgId) {
        fdMapper.delete(new LambdaQueryWrapper<PmsFieldDefinition>().eq(PmsFieldDefinition::getFileTypeConfigId, cfgId));
        ftcMapper.deleteById(cfgId);
        return Result.ok();
    }

    // ========== 字段定义 CRUD ==========
    @GetMapping("/{typeId}/file-types/{cfgId}/fields")
    public Result<List<Map<String,Object>>> listFields(@PathVariable Long cfgId) {
        List<PmsFieldDefinition> fds = fdMapper.selectList(
                new LambdaQueryWrapper<PmsFieldDefinition>().eq(PmsFieldDefinition::getFileTypeConfigId, cfgId).orderByAsc(PmsFieldDefinition::getSortOrder));
        return Result.ok(fds.stream().map(this::fdVO).collect(Collectors.toList()));
    }

    @PostMapping("/{typeId}/file-types/{cfgId}/fields")
    public Result<Map<String,Object>> createField(@PathVariable Long cfgId, @RequestBody PmsFieldDefinition fd) {
        fd.setFileTypeConfigId(cfgId);
        fdMapper.insert(fd);
        return Result.ok(fdVO(fd));
    }

    @PutMapping("/{typeId}/file-types/{cfgId}/fields/{fieldId}")
    public Result<Map<String,Object>> updateField(@PathVariable Long fieldId, @RequestBody PmsFieldDefinition fd) {
        fd.setId(fieldId); fdMapper.updateById(fd);
        return Result.ok(fdVO(fdMapper.selectById(fieldId)));
    }

    @DeleteMapping("/{typeId}/file-types/{cfgId}/fields/{fieldId}")
    public Result<Void> deleteField(@PathVariable Long fieldId) { fdMapper.deleteById(fieldId); return Result.ok(); }

    // ========== Schema 导入导出 ==========

    /**
     * 导出完整Schema — 项目类型 + 所有文件类型 + 所有字段定义
     */
    @GetMapping("/{typeId}/schema/export")
    public Result<Map<String, Object>> exportSchema(@PathVariable Long typeId) {
        PmsProjectType pt = ptMapper.selectById(typeId);
        if (pt == null) return Result.fail("项目类型不存在");

        List<PmsFileTypeConfig> ftcs = ftcMapper.selectList(
                new LambdaQueryWrapper<PmsFileTypeConfig>().eq(PmsFileTypeConfig::getProjectTypeId, typeId));

        List<Map<String, Object>> schemas = new ArrayList<>();
        for (PmsFileTypeConfig ftc : ftcs) {
            List<PmsFieldDefinition> fields = fdMapper.selectList(
                    new LambdaQueryWrapper<PmsFieldDefinition>()
                            .eq(PmsFieldDefinition::getFileTypeConfigId, ftc.getId())
                            .eq(PmsFieldDefinition::getIsActive, 1)
                            .orderByAsc(PmsFieldDefinition::getSortOrder));

            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("project_type", Map.of("name", pt.getName(), "code", pt.getCode(), "description", pt.getDescription() != null ? pt.getDescription() : ""));

            Map<String, Object> ftcMap = new LinkedHashMap<>();
            ftcMap.put("name", ftc.getName()); ftcMap.put("code", ftc.getCode());
            ftcMap.put("skip_rows", ftc.getSkipRows()); ftcMap.put("sheet_name", ftc.getSheetName());
            ftcMap.put("has_fields", ftc.getHasFields()); ftcMap.put("sort_order", ftc.getSortOrder());
            schema.put("file_type_config", ftcMap);

            List<Map<String, Object>> columns = new ArrayList<>();
            for (PmsFieldDefinition fd : fields) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("key", fd.getFieldKey()); col.put("name", fd.getFieldLabel());
                col.put("type", fd.getFieldType()); col.put("required", fd.getIsRequired() == 1);
                if (fd.getOptions() != null) col.put("options", fd.getOptions());
                if (fd.getExtraAttrs() != null) {
                    try {
                        Map<String, Object> attrs = JSONUtil.parseObj(fd.getExtraAttrs());
                        col.putAll(attrs);
                    } catch (Exception ignored) {
                        // extra_attrs JSON解析失败时跳过该属性
                    }
                }
                columns.add(col);
            }
            schema.put("columns", columns);
            schemas.add(schema);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project_type_name", pt.getName());
        result.put("file_type_count", ftcs.size());
        result.put("schemas", schemas);
        return Result.ok(result);
    }

    /**
     * 导入Schema JSON — 自动创建文件类型配置和字段定义
     */
    @PostMapping("/{typeId}/schema/import")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin', 'ROLE_admin')")
    public Result<Map<String, Object>> importSchema(@PathVariable Long typeId, @RequestBody Map<String, Object> body) {
        PmsProjectType pt = ptMapper.selectById(typeId);
        if (pt == null) return Result.fail("项目类型不存在");

        int fileTypesCreated = 0, fileTypesUpdated = 0, fieldsCreated = 0;
        Object schemasObj = body.get("schemas");
        if (!(schemasObj instanceof List)) return Result.fail("schemas 必须是数组");

        List<?> schemas = (List<?>) schemasObj;
        for (Object obj : schemas) {
            @SuppressWarnings("unchecked")
            Map<String, Object> schema = (Map<String, Object>) obj;

            // 解析文件类型配置
            @SuppressWarnings("unchecked")
            Map<String, Object> ftcMap = (Map<String, Object>) schema.get("file_type_config");
            if (ftcMap == null) continue;

            String code = (String) ftcMap.getOrDefault("code",
                    cn.hutool.extra.pinyin.PinyinUtil.getPinyin((String) ftcMap.get("name"), "").toLowerCase().replace(" ", "_"));

            // 检查是否已存在同编码的文件类型配置
            PmsFileTypeConfig existingFtc = ftcMapper.selectOne(
                    new LambdaQueryWrapper<PmsFileTypeConfig>()
                            .eq(PmsFileTypeConfig::getProjectTypeId, typeId)
                            .eq(PmsFileTypeConfig::getCode, code));

            PmsFileTypeConfig ftc;
            if (existingFtc != null) {
                // 已存在：更新 + 删除旧字段后重建
                ftc = existingFtc;
                fileTypesUpdated++;
                ftc.setName((String) ftcMap.get("name"));
                ftc.setSkipRows((Integer) ftcMap.getOrDefault("skip_rows", 0));
                ftc.setSheetName((String) ftcMap.get("sheet_name"));
                ftc.setHasFields(Boolean.TRUE.equals(ftcMap.get("has_fields")) || Integer.valueOf(1).equals(ftcMap.get("has_fields")));
                ftc.setSortOrder((Integer) ftcMap.getOrDefault("sort_order", 0));
                ftcMapper.updateById(ftc);
                // 删除旧字段
                fdMapper.delete(new LambdaQueryWrapper<PmsFieldDefinition>()
                        .eq(PmsFieldDefinition::getFileTypeConfigId, ftc.getId()));
            } else {
                // 新创建
                ftc = new PmsFileTypeConfig();
                ftc.setProjectTypeId(typeId);
                ftc.setName((String) ftcMap.get("name"));
                ftc.setCode(code);
                ftc.setSkipRows((Integer) ftcMap.getOrDefault("skip_rows", 0));
                ftc.setSheetName((String) ftcMap.get("sheet_name"));
                ftc.setHasFields(Boolean.TRUE.equals(ftcMap.get("has_fields")) || Integer.valueOf(1).equals(ftcMap.get("has_fields")));
                ftc.setSortOrder((Integer) ftcMap.getOrDefault("sort_order", 0));
                ftcMapper.insert(ftc);
                fileTypesCreated++;
            }

            // 解析字段定义
            Object columnsObj = schema.get("columns");
            if (columnsObj instanceof List) {
                List<?> columns = (List<?>) columnsObj;
                for (int i = 0; i < columns.size(); i++) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> col = (Map<String, Object>) columns.get(i);

                    PmsFieldDefinition fd = new PmsFieldDefinition();
                    fd.setFileTypeConfigId(ftc.getId());
                    fd.setFieldKey((String) col.get("key"));
                    fd.setFieldLabel((String) col.get("name"));
                    fd.setFieldType((String) col.getOrDefault("type", "text"));
                    fd.setIsRequired(Boolean.TRUE.equals(col.get("required")) ? 1 : 0);
                    fd.setSortOrder(i + 1);
                    fd.setIsActive(1);

                    // 处理options
                    if (col.get("options") != null) {
                        fd.setOptions(JSONUtil.toJsonStr(col.get("options")));
                    }

                    // 处理extra_attrs（敏感信息、校验规则等）
                    Map<String, Object> extraAttrs = new HashMap<>();
                    if (col.get("sensitive") != null) extraAttrs.put("sensitive", col.get("sensitive"));
                    if (col.get("anonymize_rule") != null) extraAttrs.put("anonymize_rule", col.get("anonymize_rule"));
                    if (col.get("max_length") != null) extraAttrs.put("max_length", col.get("max_length"));
                    if (col.get("pattern") != null) extraAttrs.put("pattern", col.get("pattern"));
                    if (col.get("min") != null) extraAttrs.put("min", col.get("min"));
                    if (col.get("max") != null) extraAttrs.put("max", col.get("max"));
                    if (!extraAttrs.isEmpty()) fd.setExtraAttrs(JSONUtil.toJsonStr(extraAttrs));

                    fdMapper.insert(fd);
                    fieldsCreated++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "导入成功");
        result.put("fileTypesCreated", fileTypesCreated);
        result.put("fileTypesUpdated", fileTypesUpdated);
        result.put("fieldsCreated", fieldsCreated);
        return Result.ok(result);
    }

    // ========== VO helpers ==========
    private Map<String,Object> ptVO(PmsProjectType pt) {
        Map<String,Object> m = new HashMap<>(); m.put("id",pt.getId()); m.put("name",pt.getName());
        m.put("code",pt.getCode()); m.put("description",pt.getDescription()); m.put("createdAt",pt.getCreateTime()); return m;
    }
    private Map<String,Object> ftcVO(PmsFileTypeConfig f) {
        Map<String,Object> m = new HashMap<>(); m.put("id",f.getId()); m.put("projectTypeId",f.getProjectTypeId());
        m.put("name",f.getName()); m.put("code",f.getCode()); m.put("skipRows",f.getSkipRows());
        m.put("sheetName",f.getSheetName()); m.put("hasFields",f.getHasFields()); m.put("sortOrder",f.getSortOrder());
        m.put("createdAt",f.getCreateTime()); return m;
    }
    private Map<String,Object> fdVO(PmsFieldDefinition fd) {
        Map<String,Object> m = new HashMap<>(); m.put("id",fd.getId()); m.put("fileTypeConfigId",fd.getFileTypeConfigId());
        m.put("fieldKey",fd.getFieldKey()); m.put("fieldLabel",fd.getFieldLabel());
        m.put("fieldType",fd.getFieldType()); m.put("options",fd.getOptions());
        m.put("isRequired",fd.getIsRequired()); m.put("sortOrder",fd.getSortOrder());
        m.put("isActive",fd.getIsActive()); m.put("extraAttrs",fd.getExtraAttrs()); return m;
    }
}
