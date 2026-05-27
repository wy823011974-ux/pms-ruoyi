package com.pms.modules.project;

/**
 * @author ROY
 * @date 2026/05/27
 */

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.Result;
import com.pms.modules.file.PmsFileRecord;
import com.pms.modules.file.PmsFileRecordMapper;
import com.pms.modules.survey.PmsSurveyData;
import com.pms.modules.survey.PmsSurveyDataMapper;
import com.pms.modules.user.SysUser;
import com.pms.modules.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final PmsProjectMapper projectMapper;
    private final SysUserMapper userMapper;
    private final PmsSurveyDataMapper surveyDataMapper;
    private final PmsFileRecordMapper fileRecordMapper;
    private final ProjectService projectService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status, @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean deleted) {
        LambdaQueryWrapper<PmsProject> qw = new LambdaQueryWrapper<>();
        // 默认不显示已删除项目
        if (!deleted) {
            qw.eq(PmsProject::getIsDeleted, 0);
        } else {
            qw.eq(PmsProject::getIsDeleted, 1);
        }
        if (StrUtil.isNotBlank(status)) qw.eq(PmsProject::getStatus, status);
        if (StrUtil.isNotBlank(keyword))
            qw.and(w -> w.like(PmsProject::getName, keyword).or().like(PmsProject::getCode, keyword));
        qw.orderByDesc(PmsProject::getCreateTime);
        Page<PmsProject> p = projectMapper.selectPage(new Page<>(page, pageSize), qw);
        return Result.page(p.getTotal(), p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        PmsProject p = new PmsProject();
        p.setName((String) body.get("name"));
        p.setYear((Integer) body.get("year"));
        p.setLocation((String) body.get("location"));
        p.setProjectTypeId(toLong(body.get("projectTypeId")));
        p.setLeaderId(toLong(body.get("leaderId")));
        // 自动生成项目编码：HBXM-{类型简写}-{年份}-{序号}
        p.setCode(projectService.generateProjectCode(p.getProjectTypeId(), p.getYear()));
        p.setStatus("IN_PROGRESS");
        // 从认证上下文获取当前用户ID
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = (auth != null && auth.getPrincipal() instanceof Long) ? (Long) auth.getPrincipal() : 1L;
        p.setCreatorId(currentUserId);
        projectMapper.insert(p);
        return Result.ok(toVO(p));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        PmsProject p = projectMapper.selectById(id);
        if (p == null) return Result.fail("项目不存在");
        if (body.containsKey("name")) p.setName((String) body.get("name"));
        if (body.containsKey("location")) p.setLocation((String) body.get("location"));
        if (body.containsKey("leaderId")) p.setLeaderId(toLong(body.get("leaderId")));
        projectMapper.updateById(p);
        return Result.ok(toVO(p));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        PmsProject p = projectMapper.selectById(id);
        if (p == null) return Result.fail("项目不存在");

        // 检查是否有已上传的数据或文件
        long dataCount = surveyDataMapper.selectCount(
                new LambdaQueryWrapper<PmsSurveyData>().eq(PmsSurveyData::getProjectId, id));
        long fileCount = fileRecordMapper.selectCount(
                new LambdaQueryWrapper<PmsFileRecord>().eq(PmsFileRecord::getProjectId, id));
        if (dataCount > 0 || fileCount > 0) {
            return Result.fail("该项目已有 " + dataCount + " 条数据和 " + fileCount + " 个文件，请先清空后再删除");
        }

        p.setIsDeleted(1);
        projectMapper.updateById(p);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Map<String,Object>> updateStatus(@PathVariable Long id, @RequestBody Map<String,String> body) {
        PmsProject p = projectMapper.selectById(id);
        if (p == null) return Result.fail("项目不存在");
        String newStatus = body.get("status");
        // 验证状态流转合法性
        if (!projectService.isValidStatusTransition(p.getStatus(), newStatus))
            return Result.fail("不允许从 " + p.getStatus() + " 流转到 " + newStatus);
        p.setStatus(newStatus);
        projectMapper.updateById(p);
        return Result.ok(toVO(p));
    }

    private Map<String,Object> toVO(PmsProject p) {
        Map<String,Object> m = new HashMap<>();
        m.put("id",p.getId()); m.put("code",p.getCode()); m.put("name",p.getName());
        m.put("year",p.getYear()); m.put("location",p.getLocation());
        m.put("projectTypeId",p.getProjectTypeId()); m.put("status",p.getStatus());
        m.put("leaderId",p.getLeaderId()); m.put("creatorId",p.getCreatorId());
        m.put("startDate",p.getStartDate()); m.put("endDate",p.getEndDate());
        m.put("createTime",p.getCreateTime()); m.put("isDeleted",p.getIsDeleted());
        return m;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        return Long.valueOf(v.toString());
    }
}
