package com.pms.modules.projectType;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.Result;
import com.pms.modules.fileType.PmsFileTypeConfig;
import com.pms.modules.fileType.PmsFileTypeConfigMapper;
import com.pms.modules.fileType.PmsFieldDefinition;
import com.pms.modules.fileType.PmsFieldDefinitionMapper;
import lombok.RequiredArgsConstructor;
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
